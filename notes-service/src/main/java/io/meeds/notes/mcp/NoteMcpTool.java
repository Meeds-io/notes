/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.notes.mcp;

import static io.meeds.mcp.server.tool.util.McpToolPluginUtils.getInteger;
import static io.meeds.mcp.server.util.McpToolUtils.formatDate;
import static io.meeds.mcp.server.util.McpToolUtils.markdownToHtml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.UploadToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.notes.mcp.model.NoteBreadcrumbModel;
import io.meeds.notes.mcp.model.NoteModel;
import io.meeds.notes.mcp.model.NoteRootTreeModel;
import io.meeds.notes.mcp.model.NoteTreeModel;
import io.meeds.notes.mcp.model.NoteVersionModel;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.notes.plugin.NotePermanentLinkPlugin;
import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.html.model.HtmlTransformerContext;
import io.meeds.social.html.utils.HtmlUtils;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@Service
@Profile("mcp-server")
public class NoteMcpTool implements McpToolPlugin {

  private static final String     NOTE_EDIT_DENIED = "User isn't allowed to update the Note with id '%s'";

  private static final String     SPACE_WIKI_TYPE  = WikiType.GROUP.toString().toLowerCase();

  private WikiService             wikiService;

  private NoteService             noteService;

  private IdentityManager         identityManager;

  private ActivityManager         activityManager;

  private I18NActivityProcessor   i18NActivityProcessor;

  private SpaceService            spaceService;

  private TranslationService      translationService;

  private ProfilePropertyService  profilePropertyService;

  private UserACL                 userAcl;

  private UserPortalConfigService portalConfigService;

  private PermanentLinkService    permanentLinkService;

  private UploadService           uploadService;

  private AttachmentService       attachmentService;

  private FileService             fileService;

  public NoteMcpTool(WikiService wikiService,
                          NoteService noteService,
                          ActivityManager activityManager,
                          IdentityManager identityManager,
                          I18NActivityProcessor i18NActivityProcessor,
                          SpaceService spaceService,
                          TranslationService translationService,
                          ProfilePropertyService profilePropertyService,
                          UserACL userAcl,
                          UserPortalConfigService portalConfigService,
                          PermanentLinkService permanentLinkService,
                          UploadService uploadService,
                          AttachmentService attachmentService,
                          FileService fileService) {
    this.wikiService = wikiService;
    this.noteService = noteService;
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.i18NActivityProcessor = i18NActivityProcessor;
    this.spaceService = spaceService;
    this.translationService = translationService;
    this.profilePropertyService = profilePropertyService;
    this.userAcl = userAcl;
    this.portalConfigService = portalConfigService;
    this.permanentLinkService = permanentLinkService;
    this.uploadService = uploadService;
    this.attachmentService = attachmentService;
    this.fileService = fileService;
  }

  public NoteRootTreeModel getSpaceNoteTree(long spaceId) throws ObjectNotFoundException, IllegalAccessException {
    Page rootNote = getSpaceParentPage(spaceId);
    return toNoteRootTreeModel(rootNote, spaceId);
  }

  public NoteModel getNote(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId, language);
    return toNoteModel(note);
  }

  /**
   * Lists the language codes a note has translations for (e.g. ["en", "fr"]).
   * The default (untranslated) content is what get_note returns without a
   * language; an empty list means the note has only its default content. Read as
   * the current user, so the note's ACL is enforced. Read-only.
   */
  public List<String> getNoteTranslations(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    getNoteById(noteId); // ACL check: throws if the current user can't view the note
    try {
      return noteService.getPageAvailableTranslationLanguages(noteId, false);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read the note's translations: " + e.getMessage());
    }
  }

  public NoteModel createSpaceNote(Long spaceId,
                                   String title,
                                   String summary,
                                   String htmlContent) throws IllegalAccessException, ObjectNotFoundException {
    if (spaceId == null) {
      throw new IllegalArgumentException("Either choose a target space identified by its id or a parent note to add a child note into.");
    }
    Page parentPage = getSpaceParentPage(spaceId);
    return createNote(parentPage, title, summary, markdownToHtml(htmlContent));
  }

  public NoteModel createChildNote(Long parentNoteId,
                                   String title,
                                   String summary,
                                   String htmlContent) throws IllegalAccessException, ObjectNotFoundException {
    if (parentNoteId == null) {
      throw new IllegalArgumentException("Either choose a target space identified by its id or a parent note to add a child note into.");
    }
    Page parentPage = getNoteById(parentNoteId);
    return createNote(parentPage, title, summary, markdownToHtml(htmlContent));
  }

  @SneakyThrows
  public NoteModel updateNote(long noteId,
                              String title,
                              String htmlContent,
                              String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    if (StringUtils.isBlank(language)) {
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
    } else {
      // Create/update a LANGUAGE translation WITHOUT touching the default note:
      // persist the note first with its default title/content unchanged, then
      // carry the translated title/content only into the language version below.
      // Applying the translated content before updateNote (as before) overwrote
      // the default page, making a new translation replace the original note.
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
      // Base the translation's metadata (summary + cover) on THAT language's own
      // current state, not the default's: covers/summaries are stored per
      // language, so carrying the default's properties into an EXISTING
      // translation would clobber the translation's own distinct cover/summary
      // with the default's. Only a brand-new translation (no per-language
      // properties yet) inherits the default's, matching the native update.
      Page langNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), currentUserAclIdentity, null, language);
      if (langNote != null && langNote.getProperties() != null) {
        note.setProperties(langNote.getProperties());
      } else {
        Page defaultNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), currentUserAclIdentity, null, null);
        if (defaultNote != null && defaultNote.getProperties() != null) {
          note.setProperties(defaultNote.getProperties());
        }
      }
      note.setLang(language);
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
    }
    noteService.createVersionOfNote(note, currentUserAclIdentity.getUserId(), true);
    WikiPageParams noteParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    noteService.removeDraftOfNote(noteParams, language);
    return getNote(noteId, null);
  }

  @SneakyThrows
  public ActivityModel publishNote(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    note.setToBePublished(true);
    noteService.updateNote(note, PageUpdateType.PUBLISH, currentUserAclIdentity);
    note = getNoteById(noteId);
    return toActivityModel(note.getActivityId());
  }

  public void deleteNote(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    noteService.deleteNote(String.valueOf(noteId));
  }

  public void moveNote(long noteId,
                       long targetParentNoteId) throws IllegalAccessException, ObjectNotFoundException {
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    Page note = getNoteById(noteId);
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    Page targetNote = getNoteById(targetParentNoteId);
    if (!noteService.canEditNote(targetNote, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    WikiPageParams currentLocationParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    WikiPageParams newLocationParams = new WikiPageParams(targetNote.getWikiType(),
                                                          targetNote.getWikiOwner(),
                                                          targetNote.getName());
    noteService.moveNote(currentLocationParams, newLocationParams, currentUserAclIdentity);
  }

  @SneakyThrows
  public List<NoteModel> searchNotes(String query,
                                     Long spaceId,
                                     Integer offset,
                                     Integer limit,
                                     Boolean isFavorites) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    WikiSearchData data = new WikiSearchData(StringUtils.lowerCase(query),
                                             currentIdentity.getUserId());
    data.setOffset(getInteger(offset, DEFAULT_OFFSET));
    data.setLimit(getInteger(limit, DEFAULT_LIMIT));
    data.setNotesTreeFilter(false);
    data.setFavorites(isFavorites != null && isFavorites.booleanValue());
    if (spaceId != null && spaceId > 0) {
      data.setSpaceIds(List.of(String.valueOf(spaceId)));
    }
    List<SearchResult> results = noteService.search(data).getAll();
    return results.stream()
                  .map(SearchResult::getId)
                  .map(this::getNoteByIdNoException)
                  .map(this::toNoteModel)
                  .toList();
  }

  /**
   * Lists the version history of a note: each version's number, id, title,
   * content, date and author. Read as the current user, so the note's ACL is
   * enforced. Read-only.
   */
  public List<NoteVersionModel> getNoteVersions(long noteId, String language) throws IllegalAccessException,
                                                                             ObjectNotFoundException {
    Page note = getNoteById(noteId);
    return versionsHistory(note, language).stream().map(this::toNoteVersionModel).toList();
  }

  /**
   * Restores a note to a previous version identified by its version_number (from
   * get_note_versions), creating a new current version from it. Only a user who
   * can edit the note may restore it.
   */
  public NoteModel restoreNoteVersion(long noteId,
                                      long versionNumber,
                                      String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId, language);
    if (!noteService.canEditNote(note, getCurrentUserName())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    PageHistory target =
                       versionsHistory(note, language).stream()
                                                      .filter(version -> version.getVersionNumber() != null
                                                          && version.getVersionNumber() == versionNumber)
                                                      .findFirst()
                                                      .orElseThrow(() -> new ObjectNotFoundException(("Note with id %s has no version number %s. "
                                                          + "Use get_note_versions to list the available versions.").formatted(noteId,
                                                                                                                               versionNumber)));
    try {
      noteService.restoreVersionOfNote(target.getName(), note, getCurrentUserName());
    } catch (Exception e) {
      throw new IllegalStateException("Could not restore the note version: " + e.getMessage());
    }
    return getNote(noteId, null);
  }

  private List<PageHistory> versionsHistory(Page note, String language) {
    try {
      return noteService.getVersionsHistoryOfNoteByLang(note, getCurrentUserName(), StringUtils.trimToNull(language));
    } catch (Exception e) {
      throw new IllegalStateException("Could not read the note version history: " + e.getMessage());
    }
  }

  /**
   * Sets a note's cover ("featured") image from exactly one of a public http(s)
   * URL, base64 bytes, or an ACL-checked reference to an existing platform
   * attachment. Only a user who can edit the note may set it. The image is wired
   * into the note metadata so it actually renders on the note.
   */
  public NoteModel setNoteCover(long noteId,
                                String imageUrl,
                                String imageBase64,
                                String attachmentObjectType,
                                String attachmentObjectId,
                                String altText,
                                String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId, language);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    UploadToolUtils.FetchedContent image = UploadToolUtils.resolveImage(attachmentService,
                                                                      fileService,
                                                                      getCurrentUserAclIdentity(),
                                                                      imageUrl,
                                                                      imageBase64,
                                                                      attachmentObjectType,
                                                                      attachmentObjectId,
                                                                      UploadToolUtils.DEFAULT_MAX_BYTES);
    String uploadId = UploadToolUtils.materialize(uploadService, image.bytes(), image.fileName(), image.mimeType());
    try {
      NotePageProperties properties = resolveBaseProperties(noteId, note, language);
      NoteFeaturedImage featuredImage = new NoteFeaturedImage();
      NoteFeaturedImage existing = properties.getFeaturedImage();
      if (existing != null && existing.getId() != null && existing.getId() > 0) {
        featuredImage.setId(existing.getId()); // update the existing cover file in place
      }
      featuredImage.setUploadId(uploadId);
      featuredImage.setMimeType(image.mimeType());
      featuredImage.setFileName(image.fileName());
      featuredImage.setAltText(altText);
      properties.setNoteId(noteId);
      properties.setDraft(false);
      properties.setFeaturedImage(featuredImage);
      String lang = StringUtils.isBlank(language) ? note.getLang() : language;
      noteService.saveNoteMetadata(properties, lang, currentUserIdentityId(username));
      // propagate the metadata onto a new note version, so the cover is resolved
      // consistently from the published version (as the native update flow does)
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      UploadToolUtils.release(uploadService, uploadId);
      throw new IllegalStateException("Could not set the note cover image: " + e.getMessage());
    }
    return getNote(noteId, null);
  }

  /**
   * Sets (or replaces) a note's summary — the short excerpt shown on note cards.
   * Only a user who can edit the note may set it; the note's cover image is
   * preserved.
   */
  public NoteModel setNoteSummary(long noteId, String summary, String language) throws IllegalAccessException,
                                                                               ObjectNotFoundException {
    Page note = getNoteById(noteId, language);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    try {
      NotePageProperties properties = resolveBaseProperties(noteId, note, language);
      properties.setNoteId(noteId);
      properties.setDraft(false);
      properties.setSummary(summary);
      // leave the existing cover untouched (its id stays in the saved metadata)
      properties.setFeaturedImage(null);
      String lang = StringUtils.isBlank(language) ? note.getLang() : language;
      noteService.saveNoteMetadata(properties, lang, currentUserIdentityId(username));
      // propagate the metadata onto a new note version (as the native update flow does)
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      throw new IllegalStateException("Could not set the note summary: " + e.getMessage());
    }
    return getNote(noteId, null);
  }

  /**
   * Removes a note's cover ("featured") image. Only a user who can edit the note
   * may remove it.
   */
  public NoteModel removeNoteCover(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId, language);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    NotePageProperties properties = resolveBaseProperties(noteId, note, language);
    NoteFeaturedImage existing = properties.getFeaturedImage();
    if (existing == null || existing.getId() == null || existing.getId() <= 0) {
      throw new ObjectNotFoundException("Note with id %s has no cover image to remove.".formatted(noteId));
    }
    try {
      String lang = StringUtils.isBlank(language) ? note.getLang() : language;
      noteService.removeNoteFeaturedImage(noteId, existing.getId(), lang, false, currentUserIdentityId(username));
      // clear the in-memory featured image before resaving the version: removeNoteFeaturedImage
      // already dropped the stored id, and saveNoteMetadata treats a null featuredImage as
      // "leave it alone" — leaving the stale id-only image here would re-add the just-deleted cover
      if (note.getProperties() != null) {
        note.getProperties().setFeaturedImage(null);
      }
      // propagate the removal onto a new note version (as the native update flow does)
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      throw new IllegalStateException("Could not remove the note cover image: " + e.getMessage());
    }
    return getNote(noteId, null);
  }

  // When a language is provided, the base metadata (summary + cover) must come
  // from THAT language's own current state, not the default's, so that editing
  // one field of a translation doesn't clobber the translation's own other
  // fields with the default's. A brand-new translation (no per-language
  // properties yet) still inherits the default's metadata. The default-language
  // path (blank language) keeps using the already-loaded note's properties.
  private NotePageProperties resolveBaseProperties(long noteId, Page note, String language) {
    if (StringUtils.isBlank(language)) {
      return note.getProperties() != null ? note.getProperties() : new NotePageProperties();
    }
    try {
      Identity identity = getCurrentUserAclIdentity();
      Page langNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), identity, null, language);
      if (langNote != null && langNote.getProperties() != null) {
        return langNote.getProperties();
      }
      Page defaultNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), identity, null, null);
      if (defaultNote != null && defaultNote.getProperties() != null) {
        return defaultNote.getProperties();
      }
    } catch (Exception e) {
      // fall back to the note already loaded for the current locale
    }
    return note.getProperties() != null ? note.getProperties() : new NotePageProperties();
  }

  private long currentUserIdentityId(String username) {
    return Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
  }

  private NoteVersionModel toNoteVersionModel(PageHistory version) {
    return new NoteVersionModel(version.getVersionNumber(),
                                version.getId(),
                                version.getTitle(),
                                version.getContent(),
                                formatDate(version.getUpdatedDate() != null ? version.getUpdatedDate() : version.getCreatedDate()),
                                toUserModel(version.getAuthor()));
  }

  private NoteModel createNote(Page parentPage, String title, String summary, String htmlContent) throws IllegalAccessException,
                                                                                                  ObjectNotFoundException {
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    Wiki wiki = wikiService.getWikiByTypeAndOwner(parentPage.getWikiType(), parentPage.getWikiOwner());
    String currentUser = currentUserAclIdentity.getUserId();
    Page note = new Page(UUID.randomUUID().toString());
    note.setAuthor(currentUser);
    note.setOwner(currentUser);
    note.setLastUpdater(currentUser);
    note.setSyntax(wikiService.getDefaultWikiSyntaxId());
    note.setTitle(title);
    note.setContent(htmlContent);
    note.setWikiId(wiki.getId());
    note.setWikiOwner(wiki.getOwner());
    note.setWikiType(wiki.getType());
    NotePageProperties properties = new NotePageProperties();
    properties.setSummary(summary);
    note.setProperties(properties);
    Page createdNote = noteService.createNote(wiki,
                                              parentPage.getName(),
                                              note,
                                              currentUserAclIdentity,
                                              false,
                                              true);
    return getNote(Long.parseLong(createdNote.getId()), null);
  }

  private String sanitizeAndSubstituteMentions(String htmlContent, Locale locale) {
    try {
      htmlContent = HtmlUtils.transform(htmlContent,
                                        new HtmlTransformerContext(ConversationState.getCurrent().getIdentity(),
                                                                   locale));
      String sanitizedBody = HTMLSanitizer.sanitize(htmlContent);
      sanitizedBody = sanitizedBody.replace("&#64;", "@");
      return MentionUtils.substituteUsernames(CommonsUtils.getCurrentPortalOwner(), sanitizedBody, locale);
    } catch (Exception e) {
      return htmlContent;
    }
  }

  @SneakyThrows
  private NoteModel toNoteModel(Page note) {
    Locale currentUserLocale = getCurrentUserLocale();
    String htmlContent = sanitizeAndSubstituteMentions(note.getContent(), currentUserLocale);
    note.setContent(htmlContent);
    String currentUserName = getCurrentUserName();
    boolean canEdit = noteService.canEditNote(note, currentUserName);
    String summary = note.getProperties() != null ? note.getProperties().getSummary() : null;
    return new NoteModel(Long.parseLong(note.getId()),
                         note.getTitle(),
                         summary,
                         htmlContent,
                         getUrl(note),
                         formatDate(note.getCreatedDate()),
                         formatDate(note.getUpdatedDate()),
                         note.isHasChild(),
                         canEdit,
                         toUserModel(note.getAuthor()),
                         toUserModel(note.getLastUpdater()),
                         toNoteBreadcrumb(note));
  }

  private NoteRootTreeModel toNoteRootTreeModel(Page rootNote, long spaceId) {
    NoteRootTreeModel rootTreeModel = new NoteRootTreeModel(Long.parseLong(rootNote.getId()),
                                                            rootNote.getTitle(),
                                                            getUrl(rootNote),
                                                            null,
                                                            spaceId);
    addChildren(rootNote, rootTreeModel);
    return rootTreeModel;
  }

  @SneakyThrows
  private NoteTreeModel toNoteTreeModel(Page note) {
    NoteTreeModel noteTreeModel = new NoteTreeModel(Long.parseLong(note.getId()),
                                                    note.getTitle(),
                                                    getUrl(note),
                                                    null);
    addChildren(note, noteTreeModel);
    return noteTreeModel;
  }

  private List<NoteBreadcrumbModel> toNoteBreadcrumb(Page note) {
    List<NoteBreadcrumbModel> breadcrumb = new ArrayList<>();
    addNoteBreadcrumb(breadcrumb, note);
    return breadcrumb;
  }

  private void addNoteBreadcrumb(List<NoteBreadcrumbModel> breadcrumb, Page note) {
    breadcrumb.add(0,
                   new NoteBreadcrumbModel(Long.parseLong(note.getId()),
                                           note.getTitle(),
                                           getUrl(note)));
    if (StringUtils.isNotBlank(note.getParentPageId())) {
      Page parentNote = noteService.getNoteById(note.getParentPageId());
      addNoteBreadcrumb(breadcrumb, parentNote);
    }
  }

  private void addChildren(Page note, NoteTreeModel noteTreeModel) {
    Collection<Page> childNotes = noteService.getChildrenNoteOf(note, false, false);
    if (CollectionUtils.isNotEmpty(childNotes)) {
      childNotes.stream().map(this::toNoteTreeModel).forEach(noteTreeModel::addChildNote);
    }
  }

  private Page getSpaceParentPage(long spaceId) throws ObjectNotFoundException, IllegalAccessException {
    String currentUsername = getCurrentUserName();
    Space space = spaceService.getSpaceById(spaceId);
    if (space == null) {
      throw new ObjectNotFoundException("Space with id '%s' doesn't exist.".formatted(spaceId));
    } else if (!spaceService.canViewSpace(space, currentUsername)) {
      throw new IllegalAccessException("The current user can't access space with id '%s'.".formatted(spaceId));
    }
    Wiki wiki = wikiService.getWikiByTypeAndOwner(SPACE_WIKI_TYPE, space.getGroupId());
    if (wiki == null) {
      wiki = wikiService.createWiki(SPACE_WIKI_TYPE, space.getGroupId());
      RequestLifeCycle.restartTransaction();
    }
    Page rootNote = noteService.getNoteById(wiki.getWikiHome().getId());
    if (rootNote == null) {
      throw new ObjectNotFoundException("Space with id %s doesn't have notes yet".formatted(spaceId));
    }
    return rootNote;
  }

  @SneakyThrows
  private Page getNoteByIdNoException(long noteId) {
    return getNoteById(noteId);
  }

  private Page getNoteById(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    return getNoteById(noteId, null);
  }

  private Page getNoteById(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    String lang = StringUtils.isBlank(language) ? getCurrentUserLocale().getLanguage() : language;
    Page note = noteService.getNoteByIdAndLang(Long.valueOf(noteId),
                                               getCurrentUserAclIdentity(),
                                               null,
                                               lang);
    if (note == null) {
      throw new ObjectNotFoundException("Note with id %s doesn't exists".formatted(noteId));
    } else if (!noteService.canViewNote(note, getCurrentUserName())) {
      throw new IllegalAccessException("User can't access the note with id %s".formatted(noteId));
    }
    return note;
  }

  private UserModel toUserModel(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    return UserToolUtils.toUserModel(identityManager,
                                     profilePropertyService,
                                     userAcl,
                                     translationService,
                                     portalConfigService,
                                     username,
                                     getCurrentUserName(),
                                     getCurrentUserLocale(),
                                     true);
  }

  @SneakyThrows
  private String getUrl(Page note) {
    return CommonsUtils.getCurrentDomain() +
        permanentLinkService.getLink(new PermanentLinkObject(NotePermanentLinkPlugin.OBJECT_TYPE, note.getId()));
  }

  private ActivityModel toActivityModel(String activityId) {
    return ActivityToolUtils.toActivityModel(activityManager,
                                             spaceService,
                                             identityManager,
                                             userAcl,
                                             permanentLinkService,
                                             profilePropertyService,
                                             translationService,
                                             i18NActivityProcessor,
                                             portalConfigService,
                                             activityId,
                                             getCurrentUserAclIdentity(),
                                             getCurrentUserLocale());
  }

}
