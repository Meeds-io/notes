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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
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

  private static final Log        LOG              = ExoLogger.getLogger(NoteMcpTool.class);

  private static final String     NOTE_EDIT_DENIED = "User isn't allowed to update the Note with id '%s'";

  private static final String     SPACE_WIKI_TYPE  = WikiType.GROUP.toString().toLowerCase();

  private static final String     USER_WIKI_TYPE   = WikiType.USER.toString().toLowerCase();

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
    return availableTranslationLanguages(noteId);
  }

  // Removes ONE language's translation of a note, leaving the note's original
  // content and its other translations intact. Only a user who can edit the note
  // may remove a translation. To delete the whole note (its original content and
  // every translation) use delete_note instead.
  public NoteModel removeNoteTranslation(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    if (StringUtils.isBlank(language)) {
      throw new IllegalArgumentException(("No translation language was provided for the note with id %s. Pass the language code "
          + "of the translation to remove (e.g. 'fr'); use get_note_translations to list the note's existing translations. To "
          + "delete the whole note (its original content and all translations) use delete_note instead.").formatted(noteId));
    }
    Page note = getNoteById(noteId);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    String lang = normalizeLanguage(language);
    // the default content is stored without a language and never listed as a
    // translation, so the default's own code falls through to the check below
    List<String> translations = availableTranslationLanguages(noteId);
    if (translations.stream().noneMatch(l -> StringUtils.equalsIgnoreCase(l, lang))) {
      throw new ObjectNotFoundException(("Note with id %s has no translation for language '%s'. Existing translations: %s. Use "
          + "get_note_translations to list the note's available languages.").formatted(noteId, lang, translations));
    }
    try {
      noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(noteId), lang);
    } catch (Exception e) {
      throw new IllegalStateException("Could not remove the note translation: " + e.getMessage());
    }
    return getNote(noteId, null);
  }

  private List<String> availableTranslationLanguages(long noteId) {
    try {
      return noteService.getPageAvailableTranslationLanguages(noteId, false);
    } catch (Exception e) {
      throw new IllegalStateException("Could not read the note's translations: " + e.getMessage());
    }
  }

  /**
   * Creates a note at the root of a space's notebook, as the current user. The
   * space's notebook is created on first use. The Notes service enforces the
   * space ACL (the user must be allowed to redact or publish in the space).
   *
   * @param spaceId id of the space whose notebook receives the note
   * @param title title of the note
   * @param summary optional plain-text summary shown on note cards
   * @param htmlContent body of the note, HTML or markdown
   * @return the created note, read back as the current user
   * @throws IllegalAccessException when the user can't access the space or add
   *           notes to it
   * @throws ObjectNotFoundException when the space doesn't exist
   */
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

  /**
   * Creates a note in the calling user's own (personal) notebook, at its root.
   * The notebook is resolved from the authenticated user only, never from the
   * tool arguments, so a caller can't target someone else's notebook; it is
   * created on first use, as the Notes REST layer does. The Notes service then
   * re-checks that the user manages that notebook (a personal notebook is only
   * managed by its owner).
   *
   * @param title title of the note
   * @param summary optional plain-text summary shown on note cards
   * @param htmlContent body of the note, HTML or markdown
   * @return the created note, read back as the current user
   * @throws IllegalAccessException when there is no authenticated user, or the
   *           user isn't allowed to add notes to that notebook
   * @throws ObjectNotFoundException when the notebook has no home note
   */
  public NoteModel createPersonalNote(String title,
                                      String summary,
                                      String htmlContent) throws IllegalAccessException, ObjectNotFoundException {
    if (StringUtils.isBlank(title)) {
      throw new IllegalArgumentException("A title is required to create a personal note. Ask the user for one rather than inventing it.");
    }
    Page parentPage = getPersonalParentPage();
    return createNote(parentPage, title, summary, markdownToHtml(htmlContent));
  }

  /**
   * Creates a note as a child of an existing note, in the same notebook (space
   * or personal) as its parent, as the current user.
   *
   * @param parentNoteId id of the note under which the new note is created
   * @param title title of the note
   * @param summary optional plain-text summary shown on note cards
   * @param htmlContent body of the note, HTML or markdown
   * @return the created note, read back as the current user
   * @throws IllegalAccessException when the user can't view the parent note or
   *           add notes to its notebook
   * @throws ObjectNotFoundException when the parent note doesn't exist
   */
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
    String lang = normalizeLanguage(language);
    if (lang == null) {
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
    } else {
      // persist the DEFAULT page first with its own title/content unchanged, as
      // NotesRestService#updateNoteById does, then build the version below
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
      // on THAT language's own state: title, content, cover and summary are all
      // per-language, so starting from the default would revert what a previous
      // call translated. A brand-new translation inherits the default's.
      Page langNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), currentUserAclIdentity, null, lang);
      if (langNote != null && StringUtils.equalsIgnoreCase(langNote.getLang(), lang)) {
        note.setTitle(langNote.getTitle());
        note.setContent(langNote.getContent());
        if (langNote.getProperties() != null) {
          note.setProperties(langNote.getProperties());
        }
      } else {
        Page defaultNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), currentUserAclIdentity, null, null);
        if (defaultNote != null && defaultNote.getProperties() != null) {
          note.setProperties(defaultNote.getProperties());
        }
      }
      note.setLang(lang);
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
    }
    noteService.createVersionOfNote(note, currentUserAclIdentity.getUserId(), true);
    WikiPageParams noteParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    noteService.removeDraftOfNote(noteParams, lang);
    return getNote(noteId, lang); // the version just written, not the default
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
    Page note = getNoteById(noteId, language);
    String lang = normalizeLanguage(language);
    return versionsHistory(note, lang).stream().map(this::toNoteVersionModel).toList();
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
    String lang = normalizeLanguage(language);
    PageHistory target =
                       versionsHistory(note, lang).stream()
                                                  .filter(version -> version.getVersionNumber() != null
                                                      && version.getVersionNumber() == versionNumber)
                                                  .findFirst()
                                                  .orElseThrow(() -> new ObjectNotFoundException(("Note with id %s has no version number %s. "
                                                      + "Use get_note_versions to list the available versions.").formatted(noteId,
                                                                                                                           versionNumber)));
    // the version the restore publishes is built from this object, as in NotePage.vue#restoreVersion
    note.setTitle(target.getTitle());
    note.setContent(target.getContent());
    try {
      noteService.restoreVersionOfNote(String.valueOf(target.getVersionNumber()), note, getCurrentUserName());
    } catch (Exception e) {
      throw new IllegalStateException("Could not restore the note version: " + e.getMessage());
    }
    return getNote(noteId, lang);
  }

  private List<PageHistory> versionsHistory(Page note, String language) {
    try {
      return noteService.getVersionsHistoryOfNoteByLang(note, getCurrentUserName(), normalizeLanguage(language));
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
    UploadToolUtils.ImageSource imageSource = new UploadToolUtils.ImageSource(imageUrl,
                                                                             imageBase64,
                                                                             attachmentObjectType,
                                                                             attachmentObjectId);
    UploadToolUtils.FetchedContent image = UploadToolUtils.resolveImage(attachmentService,
                                                                       fileService,
                                                                       getCurrentUserAclIdentity(),
                                                                       imageSource,
                                                                       UploadToolUtils.DEFAULT_MAX_BYTES);
    String uploadId = UploadToolUtils.materialize(uploadService, image.bytes(), image.fileName(), image.mimeType());
    String lang = normalizeLanguage(language);
    try {
      LanguageProperties resolved = resolveProperties(noteId, note, lang);
      NotePageProperties properties = resolved.properties();
      NoteFeaturedImage featuredImage = new NoteFeaturedImage();
      NoteFeaturedImage existing = properties.getFeaturedImage();
      // Reuse the id only when this language owns the FILE: on a shared id
      // saveNoteFeaturedImage takes updateFile and replaces the default's own
      // binary. Leaving it at 0 writes a new file, as the native draft path does.
      if (resolved.ownsCoverFile() && existing != null && existing.getId() != null && existing.getId() > 0) {
        featuredImage.setId(existing.getId()); // update this language's own cover file in place
      }
      featuredImage.setUploadId(uploadId);
      featuredImage.setMimeType(image.mimeType());
      featuredImage.setFileName(image.fileName());
      featuredImage.setAltText(altText);
      properties.setNoteId(noteId);
      properties.setDraft(false);
      properties.setFeaturedImage(featuredImage);
      noteService.saveNoteMetadata(properties, lang, currentUserIdentityId(username));
      // createVersionOfNote re-saves the note's own properties under its own
      // language, so both must be the ones just written or it reverts them
      note.setProperties(properties);
      note.setLang(lang);
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      UploadToolUtils.release(uploadService, uploadId);
      throw new IllegalStateException("Could not set the note cover image: " + e.getMessage());
    }
    return getNote(noteId, lang);
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
    String lang = normalizeLanguage(language);
    try {
      NotePageProperties properties = resolveProperties(noteId, note, lang).properties();
      properties.setNoteId(noteId);
      properties.setDraft(false);
      properties.setSummary(summary);
      // leave the existing cover untouched (its id stays in the saved metadata)
      properties.setFeaturedImage(null);
      noteService.saveNoteMetadata(properties, lang, currentUserIdentityId(username));
      note.setProperties(properties); // see setNoteCover
      note.setLang(lang);
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      throw new IllegalStateException("Could not set the note summary: " + e.getMessage());
    }
    return getNote(noteId, lang);
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
    String lang = normalizeLanguage(language);
    LanguageProperties resolved = resolveProperties(noteId, note, lang);
    NotePageProperties properties = resolved.properties();
    NoteFeaturedImage existing = properties.getFeaturedImage();
    if (existing == null || existing.getId() == null || existing.getId() <= 0) {
      throw new ObjectNotFoundException("Note with id %s has no cover image to remove.".formatted(noteId));
    }
    // removeNoteFeaturedImage deletes the file unguarded when isDraft=false and
    // cleans only <pageId>-<lang>, so removing a cover this language does not
    // own would take the image off the default note and leave a dangling id.
    if (resolved.coverOwnership() == CoverOwnership.UNDETERMINED) {
      throw new IllegalStateException(("The default version of the note with id %s could not be read, so it is not known "
          + "whether the cover shown in language '%s' is that translation's own or the default note's. Nothing was changed; "
          + "try again.").formatted(noteId, lang));
    }
    if (!resolved.ownsCoverFile()) {
      // not pointing at the no-language removal: it would leave every other
      // translation sharing this id pointing at a deleted file
      throw new IllegalArgumentException(("Note with id %s has no cover image of its own in language '%s': it shows the "
          + "default note's, which other translations may show too. Give '%s' its own cover with set_note_cover to replace "
          + "it, or remove the note's cover for every language from the note itself.").formatted(noteId, lang, lang));
    }
    try {
      noteService.removeNoteFeaturedImage(noteId, existing.getId(), lang, false, currentUserIdentityId(username));
      // saveNoteMetadata treats a null featuredImage as "leave it alone"; a
      // stale id-only image here would re-add the cover just deleted
      properties.setFeaturedImage(null);
      note.setProperties(properties);
      note.setLang(lang);
      noteService.createVersionOfNote(note, username, true);
    } catch (Exception e) {
      throw new IllegalStateException("Could not remove the note cover image: " + e.getMessage());
    }
    return getNote(noteId, lang);
  }

  /**
   * The metadata a language write starts from, plus whether the cover FILE it
   * names is that language's own. SHARED_WITH_DEFAULT: the default note points
   * at the same file. UNDETERMINED: the default's metadata could not be read.
   * Writing or deleting the file must treat both as "not ours".
   */
  private record LanguageProperties(NotePageProperties properties, CoverOwnership coverOwnership) {
    private boolean ownsCoverFile() {
      return coverOwnership == CoverOwnership.OWN;
    }
  }

  private enum CoverOwnership {
    OWN, SHARED_WITH_DEFAULT, UNDETERMINED
  }

  /**
   * Resolves the metadata to write for a language: that language's own state
   * when it has one, the default's otherwise. Takes an already-normalized
   * language — resolving with a raw code while judging ownership with a
   * normalized one would let the two disagree.
   */
  private LanguageProperties resolveProperties(long noteId, Page note, String lang) {
    if (lang == null) {
      // the default version owns whatever cover it names
      return new LanguageProperties(note.getProperties() != null ? note.getProperties() : new NotePageProperties(),
                                    CoverOwnership.OWN); // the default owns what it names
    }
    Identity identity = getCurrentUserAclIdentity();
    NotePageProperties loaded = note.getProperties() != null ? note.getProperties() : new NotePageProperties();
    NotePageProperties defaultProperties = null;
    NotePageProperties resolved = null;
    // caught separately so the verdict can fail CLOSED: without the default's
    // metadata, ownership is unknowable and its callers delete or overwrite the
    // file. A null page or null properties is as blind as a throw -- getNoteById
    // returns null for a missing page rather than throwing -- so neither counts
    // as read.
    boolean defaultRead = false;
    try {
      Page defaultNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), identity, null, null);
      defaultProperties = defaultNote == null ? null : defaultNote.getProperties();
      defaultRead = defaultProperties != null;
    } catch (Exception e) {
      LOG.warn("Could not read the default version of note {} while resolving the metadata of language '{}'."
          + " Treating its cover as shared with the default, so it is neither replaced nor deleted.", noteId, lang, e);
    }
    try {
      Page langNote = noteService.getNoteByIdAndLang(Long.valueOf(noteId), identity, null, lang);
      // the lang comes from the published version found, so this is
      // "that language has a version of its own"
      if (langNote != null && langNote.getProperties() != null && StringUtils.equalsIgnoreCase(langNote.getLang(), lang)) {
        resolved = langNote.getProperties();
      }
    } catch (Exception e) {
      LOG.warn("Could not read note {} in language '{}'; falling back to the metadata already loaded.", noteId, lang, e);
    }
    if (resolved == null) {
      resolved = defaultProperties != null ? defaultProperties : loaded;
    }
    CoverOwnership ownership;
    if (!defaultRead) {
      ownership = CoverOwnership.UNDETERMINED;
    } else {
      ownership = sharesCoverFile(resolved, defaultProperties) ? CoverOwnership.SHARED_WITH_DEFAULT : CoverOwnership.OWN;
    }
    return new LanguageProperties(resolved, ownership);
  }

  /**
   * A translation created by update_note carries the default's cover id into its
   * own metadata item, so "does this language have a version" is NOT the
   * question — the file id is. Mirrors NoteServiceImpl#isOriginalFeaturedImage.
   */
  private boolean sharesCoverFile(NotePageProperties properties, NotePageProperties defaultProperties) {
    Long coverId = coverFileId(properties);
    return coverId != null && coverId.equals(coverFileId(defaultProperties));
  }

  private Long coverFileId(NotePageProperties properties) {
    NoteFeaturedImage image = properties == null ? null : properties.getFeaturedImage();
    return image == null || image.getId() == null || image.getId() <= 0 ? null : image.getId();
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

  /**
   * Creates a note under the given parent, in the parent's notebook, authored
   * and owned by the current user. The Notes service enforces the notebook's
   * ACL with the current user's identity.
   *
   * @param parentPage parent note; its notebook receives the new note
   * @param title title of the note
   * @param summary optional plain-text summary
   * @param htmlContent HTML body of the note
   * @return the created note, read back as the current user
   * @throws IllegalAccessException when the user can't add notes to the
   *           parent's notebook
   * @throws ObjectNotFoundException when the created note can't be read back
   */
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

  /**
   * Resolves the home note of a space's notebook, creating the notebook on
   * first use, after checking that the current user can access the space.
   *
   * @param spaceId id of the space
   * @return the home note of the space's notebook
   * @throws ObjectNotFoundException when the space doesn't exist or its
   *           notebook has no home note
   * @throws IllegalAccessException when the current user can't access the space
   */
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

  /**
   * Resolves the home note of the current user's personal notebook, creating
   * the notebook on first use. A personal notebook is a wiki of type "user"
   * whose owner is the username; the owner is taken from the authenticated user
   * only, never from a caller-supplied value.
   *
   * @return the home note of the current user's personal notebook
   * @throws IllegalAccessException when there is no authenticated user
   * @throws ObjectNotFoundException when the notebook has no home note
   */
  private Page getPersonalParentPage() throws IllegalAccessException, ObjectNotFoundException {
    String currentUsername = getCurrentUserName();
    if (StringUtils.isBlank(currentUsername)) {
      throw new IllegalAccessException("A personal note can only be created by an authenticated user.");
    }
    Wiki wiki = wikiService.getWikiByTypeAndOwner(USER_WIKI_TYPE, currentUsername);
    if (wiki == null) {
      wiki = wikiService.createWiki(USER_WIKI_TYPE, currentUsername);
      RequestLifeCycle.restartTransaction();
    }
    Page rootNote = wiki.getWikiHome() == null ? null : noteService.getNoteById(wiki.getWikiHome().getId());
    if (rootNote == null) {
      throw new ObjectNotFoundException("The personal notebook of user '%s' has no home note yet".formatted(currentUsername));
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

  /**
   * Blank becomes null (the default version), the rest is trimmed and
   * lower-cased so "fr", " fr " and "FR" name one translation: the stored lang
   * is matched with a plain {@code p.lang = :lang}.
   */
  private String normalizeLanguage(String language) {
    String lang = StringUtils.trimToNull(language);
    // ROOT, not the JVM default: on a Turkish locale "FI" lower-cases to "fı"
    return lang == null ? null : lang.toLowerCase(Locale.ROOT);
  }

  /**
   * A blank language means the note's own default version, never the caller's UI
   * locale: resolving it from the profile locale made getNoteByIdAndLang overlay
   * whatever translation that user reads in, so every write started from one.
   */
  private Page getNoteById(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = noteService.getNoteByIdAndLang(Long.valueOf(noteId),
                                               getCurrentUserAclIdentity(),
                                               null,
                                               normalizeLanguage(language));
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

  /**
   * Builds the absolute URL of a note. Only space notes have a resolvable
   * direct-access URL ({@code NotePermanentLinkPlugin} rejects any other
   * notebook type); a personal note has none server-side, so null is returned
   * and the field is omitted from the model instead of failing the call. The
   * test is positive (fail closed): anything not explicitly a space note,
   * including a blank type, gets no URL.
   *
   * @param note the note to link to
   * @return the note's absolute URL, or null when the note isn't a space note
   */
  @SneakyThrows
  private String getUrl(Page note) {
    if (!StringUtils.equalsIgnoreCase(SPACE_WIKI_TYPE, note.getWikiType())) {
      return null;
    }
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
