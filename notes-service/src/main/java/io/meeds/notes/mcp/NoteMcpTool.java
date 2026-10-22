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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
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
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.SiteFilter;
import org.exoplatform.portal.mop.SiteType;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNode;
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
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.UploadToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.notes.mcp.model.NoteBreadcrumbModel;
import io.meeds.notes.mcp.model.NoteModel;
import io.meeds.notes.mcp.model.NoteRootTreeModel;
import io.meeds.notes.mcp.model.NoteSearchCollectionModel;
import io.meeds.notes.mcp.model.NoteSearchHitModel;
import io.meeds.notes.mcp.model.NoteSpaceModel;
import io.meeds.notes.mcp.model.NoteTreeModel;
import io.meeds.notes.mcp.model.NoteVersionCollectionModel;
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

  /** The most items a list tool returns in one call. */
  static final int                MAX_LIMIT            = 100;

  /** The characters {@code get_note} returns in a part when the caller sets no number. */
  static final int                DEFAULT_PART_CHARS   = 40_000;

  /** The most characters {@code get_note} returns in one part. */
  static final int                MAX_PART_CHARS       = 200_000;

  /**
   * The most hits {@code search_notes} pages through: Elasticsearch's default
   * {@code index.max_result_window}, which refuses a search whose from + size
   * goes beyond it.
   */
  static final int                MAX_SEARCH_WINDOW    = 10_000;

  private static final Log        LOG              = ExoLogger.getLogger(NoteMcpTool.class);

  /**
   * The longest HTML character reference, {@code &CounterClockwiseContourIntegral;}:
   * an ampersand further back than this from a cut can't open a reference the
   * cut would split.
   */
  private static final int        MAX_ENTITY_LENGTH    = 33;

  private static final String     SPACES_PREFIX        = "/spaces/";

  private static final String     NOTE_EDIT_DENIED     = "User isn't allowed to update the Note with id '%s'";

  private static final String     TARGET_OUTSIDE_SCOPE = "Note '%s' isn't in the space this call is limited to";

  private static final String     TARGET_UNDER_NOTE    =
                                                   "Note '%s' can't move under itself or one of its own sub-notes";

  private static final String     SCOPE_UNREADABLE     =
                                                   "The space this call is limited to can't be read, so it can't move a note";

  private static final String     SPACE_WIKI_TYPE  = WikiType.GROUP.toString().toLowerCase();

  private static final String     USER_WIKI_TYPE   = WikiType.USER.toString().toLowerCase();

  private static final String     NOTES_NODE_NAME  = "notes";

  private static final String     SORT_BY_DATE     = "date";

  private static final String     SORT_DESC        = "desc";

  private static final String     PORTAL_URI       = "/portal/";

  /** Displayed portal sites, in the order the site switcher shows them. */
  private static final SiteFilter PORTAL_SITES     = new SiteFilter(SiteType.PORTAL, null, null, true, true, true, true, 0, 0);

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

  private LayoutService           layoutService;

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
                          FileService fileService,
                          LayoutService layoutService) {
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
    this.layoutService = layoutService;
  }

  /**
   * @return the space the current call is limited to, null when it isn't
   * @throws IllegalStateException when the call's identity can't be read
   */
  protected Long getScopeSpaceId() {
    ActingIdentity identity = McpToolUtils.getCurrentActingIdentity();
    return identity == null ? null : identity.spaceScope();
  }

  /**
   * Checks a note a call limited to a space names besides its object belongs
   * to that space, by the rule of {@link NoteSpaceScopeResolver}. A call that
   * isn't limited is not checked; one whose identity can't be read is
   * refused. The MCP server refuses such a call before the tool runs, so that
   * branch is defence in depth: it keeps the tool fail-closed should it be
   * reached another way.
   *
   * @param note the note
   * @param noteId its id, for the message
   * @throws IllegalAccessException when the call is limited to a space the
   *           note isn't in, or its identity can't be read
   */
  private void checkInScopeSpace(Page note, long noteId) throws IllegalAccessException {
    Long spaceId;
    try {
      spaceId = getScopeSpaceId();
    } catch (IllegalStateException e) {
      throw new IllegalAccessException(SCOPE_UNREADABLE);
    }
    if (spaceId != null && !spaceId.equals(NoteSpaceScopeResolver.spaceOfNote(note, spaceService))) {
      throw new IllegalAccessException(TARGET_OUTSIDE_SCOPE.formatted(noteId));
    }
  }

  /**
   * Checks a note doesn't move under itself or one of its own sub-notes, by
   * walking the new parent's ancestors up to the root. The note storage
   * re-parents a note without that check, and a move into its own subtree
   * would cut the subtree off its notebook in a cycle. An ancestor seen twice
   * ends the walk, so a tree that already holds a cycle can't loop it.
   *
   * @param note the note to move
   * @param target the note that would become its parent
   * @param noteId the note's id, for the message
   * @throws IllegalArgumentException when the target is the note or one of
   *           its sub-notes
   */
  @SneakyThrows
  private void checkNotUnderItself(Page note, Page target, long noteId) {
    Set<String> visited = new HashSet<>();
    Page ancestor = target;
    while (ancestor != null && visited.add(ancestor.getId())) {
      if (StringUtils.equals(ancestor.getId(), note.getId())) {
        throw new IllegalArgumentException(TARGET_UNDER_NOTE.formatted(noteId));
      }
      String parentId = ancestor.getParentPageId();
      ancestor = StringUtils.isBlank(parentId) ? null : noteService.getNoteById(parentId);
    }
  }

  public NoteRootTreeModel getSpaceNoteTree(long spaceId) throws ObjectNotFoundException, IllegalAccessException {
    Page rootNote = getSpaceParentPage(spaceId);
    return toNoteRootTreeModel(rootNote, spaceId);
  }

  /**
   * Reads a note as the current user: its current content, or one version of
   * its history, whole or part by part. Without offset nor max_chars the whole
   * HTML content is returned. With either, at most max_chars characters of the
   * HTML content are returned from offset, cut where no tag, character
   * reference or surrogate pair is split, with the content's total length and
   * where the next part starts. A part of the current note also carries the
   * number of the published version it was cut from, when that version is the
   * one read: passed back as versionNumber, it pins the next parts to that
   * version even if the note is edited between two calls.
   * <p>
   * A version read returns the version's title, content, update date and last
   * updater, with the current note's summary, url, breadcrumb and can_edit. The version is
   * found in the history of {@code getVersionsHistoryOfNoteByLang}, which, on a
   * note's default language with no history yet, records the current note as
   * its first version.
   *
   * @param noteId the note id
   * @param language the translation to read, null or blank for the default
   * @param offset the first character of the HTML content to read, from 0, as
   *          next_offset gives it; null to read from the start
   * @param maxChars the most characters of the part, {@value #DEFAULT_PART_CHARS}
   *          when null, clamped to 1 and {@value #MAX_PART_CHARS}; null with a
   *          null offset to read the whole content
   * @param versionNumber the number of the version to read, from
   *          get_note_versions; null for the current note
   * @return the note, or the part of it read
   * @throws IllegalAccessException when the user can't view the note
   * @throws ObjectNotFoundException when the note, or the version, doesn't
   *           exist
   */
  public NoteModel getNote(long noteId,
                           String language,
                           Long offset,
                           Integer maxChars,
                           Long versionNumber) throws IllegalAccessException, ObjectNotFoundException {
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
    if (versionNumber != null) {
      PageHistory version = getVersion(note, lang, noteId, versionNumber);
      note.setTitle(version.getTitle());
      note.setContent(version.getContent());
      note.setUpdatedDate(version.getUpdatedDate() != null ? version.getUpdatedDate() : version.getCreatedDate());
      note.setLastUpdater(version.getAuthor());
    }
    if (offset == null && maxChars == null) {
      return toNoteModel(note, versionNumber);
    }
    Long readVersionNumber = versionNumber != null ? versionNumber : publishedVersionNumber(note, lang);
    return toNotePart(toNoteModel(note, readVersionNumber), offset, maxChars);
  }

  /**
   * Finds the number of the published version a current-note read returned:
   * the note's latest version in that language, when it is the one the read
   * overlaid on the note. A note read with a language it has no version in
   * holds the default content, and a version published between the read and
   * this lookup is another one: neither has a number to give.
   *
   * @param note the note as read, with its latest version id
   * @param lang the normalized language read, null for the default
   * @return the version's number, null when none matches the read
   */
  private Long publishedVersionNumber(Page note, String lang) {
    PageVersion published = noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(note.getId()), lang);
    if (published == null || published.getId() == null || !published.getId().equals(note.getLatestVersionId())
        || !StringUtils.isNumeric(published.getName())) {
      return null;
    }
    return Long.valueOf(published.getName());
  }

  /**
   * Reads a note's current content, whole, as the current user.
   *
   * @param noteId the note id
   * @param lang the translation to read, as {@link #resolveLanguage(long, String)}
   *          resolved it, null for the default
   * @return the note
   * @throws IllegalAccessException when the user can't view the note
   * @throws ObjectNotFoundException when the note doesn't exist
   */
  private NoteModel readNote(long noteId, String lang) throws IllegalAccessException, ObjectNotFoundException {
    return toNoteModel(getNoteById(noteId, lang));
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

  /**
   * Removes ONE language's translation of a note, leaving the note's original
   * content and its other translations intact. Only a user who can edit the
   * note may remove a translation. To delete the whole note (its original
   * content and every translation) use delete_note instead.
   *
   * @param noteId the note id
   * @param language the language of the translation to remove
   * @return the note's default version, read back as the current user
   * @throws IllegalAccessException when the user can't edit the note
   * @throws ObjectNotFoundException when the note, or that translation,
   *           doesn't exist
   */
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
    String lang = StringUtils.trim(language);
    // the default content is stored without a language and never listed as a
    // translation, so the default's own code falls through to the check below
    List<String> translations = availableTranslationLanguages(noteId);
    // the code is deleted as stored, exact match first: with both "fr" and "FR"
    // stored, "fr" deletes "fr"
    String storedLang = matchStoredLanguage(lang, translations);
    if (!translations.contains(storedLang)) {
      throw new ObjectNotFoundException(("Note with id %s has no translation for language '%s'. Existing translations: %s. "
          + "Use get_note_translations to list the note's available languages.").formatted(noteId, lang, translations));
    }
    try {
      noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(noteId), storedLang);
    } catch (Exception e) {
      throw new IllegalStateException("Could not remove the note translation: " + e.getMessage());
    }
    return readNote(noteId, null);
  }

  /**
   * Lists the codes a note's translations are stored under, as a distinct list
   * in no order, without the default (stored with no language).
   *
   * @param noteId the note id
   * @return the stored translation codes
   */
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

  /**
   * Updates a note's title and content, or one translation's, as the current
   * user, and publishes the result as a new version.
   *
   * @param noteId the note id
   * @param title the new title, blank to keep it
   * @param htmlContent the new content, HTML or markdown, blank to keep it
   * @param language the translation to write, null or blank for the default
   * @return the version just written, read back as the current user
   * @throws IllegalAccessException when the user can't edit the note
   * @throws ObjectNotFoundException when the note doesn't exist
   */
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
    String lang = resolveLanguage(noteId, language);
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
    return readNote(noteId, lang); // the version just written, not the default
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

  /**
   * Moves a note under another note, both editable by the current user. In a
   * call limited to a space, the new parent must belong to that space: the
   * space scope checks the moved note only, and the new parent would
   * otherwise carry the note to another space's notebook or a personal one.
   *
   * @param noteId the note to move
   * @param targetParentNoteId the note that becomes its parent
   * @throws IllegalAccessException when the current user can't edit either
   *           note, or, in a call limited to a space, the new parent belongs
   *           to another space or none, or the call's identity can't be read
   * @throws ObjectNotFoundException when either note doesn't exist
   * @throws IllegalArgumentException when the new parent is the note itself
   *           or one of its sub-notes
   */
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
    checkInScopeSpace(targetNote, targetParentNoteId);
    checkNotUnderItself(note, targetNote, noteId);
    WikiPageParams currentLocationParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    WikiPageParams newLocationParams = new WikiPageParams(targetNote.getWikiType(),
                                                          targetNote.getWikiOwner(),
                                                          targetNote.getName());
    noteService.moveNote(currentLocationParams, newLocationParams, currentUserAclIdentity);
  }

  /**
   * Full-text search over the notes the current user can read. A blank query
   * lists the most recently updated notes first; a non-blank query is passed
   * as typed and keeps the relevance order. Each hit is compact, without the
   * note's content: get_note reads it. One more hit than the limit is asked
   * for, to tell whether another page follows. Paging stops at the first
   * {@value #MAX_SEARCH_WINDOW} hits, the most the search engine pages
   * through: the offset is clamped so that the page ends within them, and no
   * next page is offered past them.
   *
   * @param query the words to search for, blank to list the latest notes
   * @param spaceId the space whose notebook to search, null to search
   *          everywhere
   * @param offset the hits to skip, 0 when null, clamped to 0 and
   *          {@value #MAX_SEARCH_WINDOW} minus the limit and one
   * @param limit the most hits, {@value McpToolPlugin#DEFAULT_LIMIT} when null
   *          or 0, clamped to 1 and {@value #MAX_LIMIT}
   * @param isFavorites true to search the current user's favorite notes only
   * @return the page of hits, with whether more follow and where
   */
  @SneakyThrows
  public NoteSearchCollectionModel searchNotes(String query,
                                               Long spaceId,
                                               Integer offset,
                                               Integer limit,
                                               Boolean isFavorites) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    // The query is passed as typed: the connector makes each word a wildcard term,
    // which Elasticsearch normalises to lower case at query time.
    WikiSearchData data = new WikiSearchData(query, currentIdentity.getUserId());
    if (StringUtils.isBlank(query)) {
      // No term means no relevance: list the most recently updated notes first.
      data.setSortField(SORT_BY_DATE);
      data.setSortDirection(SORT_DESC);
    }
    int usedLimit = clampLimit(limit);
    // one more hit than the limit is asked for, so the last offset leaves room for it
    int maxOffset = MAX_SEARCH_WINDOW - usedLimit - 1;
    int usedOffset = Math.clamp(getInteger(offset, DEFAULT_OFFSET), 0, maxOffset);
    data.setOffset(usedOffset);
    data.setLimit(usedLimit + 1);
    data.setNotesTreeFilter(false);
    data.setFavorites(isFavorites != null && isFavorites.booleanValue());
    if (spaceId != null && spaceId > 0) {
      data.setSpaceIds(List.of(String.valueOf(spaceId)));
    }
    List<SearchResult> results = noteService.search(data).getAll();
    int nextOffset = usedOffset + usedLimit;
    boolean hasMore = results.size() > usedLimit && nextOffset <= maxOffset;
    NoteLinks links = new NoteLinks();
    Map<String, NoteSpaceModel> spaces = new HashMap<>();
    List<NoteSearchHitModel> notes = results.stream()
                                            .limit(usedLimit)
                                            .map(SearchResult::getId)
                                            .map(this::getNoteByIdNoException)
                                            .map(note -> toNoteSearchHitModel(note, links, spaces))
                                            .toList();
    return new NoteSearchCollectionModel(notes, usedLimit, hasMore, hasMore ? nextOffset : null);
  }

  /**
   * Lists a page of the version history of a note, newest first: each
   * version's number, id, title, date and author, without its content, which
   * get_note reads with the version's number. The history is sorted by version
   * number here, since the service returns it in no stated order, so the pages
   * neither skip nor repeat a version. Read as the current user, so the note's
   * ACL is enforced. Read-only.
   *
   * @param noteId the note id
   * @param language the translation whose history to list, null or blank for
   *          the default
   * @param offset the versions to skip, 0 when null
   * @param limit the most versions, {@value McpToolPlugin#DEFAULT_LIMIT} when
   *          null or 0, clamped to 1 and {@value #MAX_LIMIT}
   * @return the page of versions, with whether more follow and where
   * @throws IllegalAccessException when the user can't view the note
   * @throws ObjectNotFoundException when the note doesn't exist
   */
  public NoteVersionCollectionModel getNoteVersions(long noteId,
                                                    String language,
                                                    Integer offset,
                                                    Integer limit) throws IllegalAccessException, ObjectNotFoundException {
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
    List<PageHistory> history = newestFirst(versionsHistory(note, lang));
    int size = history.size();
    int usedOffset = Math.clamp(getInteger(offset, DEFAULT_OFFSET), 0, size);
    int usedLimit = clampLimit(limit);
    int end = Math.min(size, usedOffset + usedLimit);
    boolean hasMore = end < size;
    List<NoteVersionModel> versions = history.subList(usedOffset, end)
                                             .stream()
                                             .map(this::toNoteVersionModel)
                                             .toList();
    return new NoteVersionCollectionModel(versions, usedLimit, hasMore, hasMore ? end : null);
  }

  /**
   * Sorts a version history by version number, highest first, a version
   * without a number last.
   *
   * @param history the history, in any order, or null
   * @return a sorted copy, empty when the history is null
   */
  private static List<PageHistory> newestFirst(List<PageHistory> history) {
    return history == null ? List.of() :
                           history.stream()
                                  .sorted(Comparator.comparing(PageHistory::getVersionNumber,
                                                               Comparator.nullsLast(Comparator.reverseOrder())))
                                  .toList();
  }

  /**
   * Restores a note to a previous version identified by its version_number (from
   * get_note_versions), creating a new current version from it. Only a user who
   * can edit the note may restore it.
   */
  public NoteModel restoreNoteVersion(long noteId,
                                      long versionNumber,
                                      String language) throws IllegalAccessException, ObjectNotFoundException {
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
    if (!noteService.canEditNote(note, getCurrentUserName())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
    PageHistory target = getVersion(note, lang, noteId, versionNumber);
    // the version the restore publishes is built from this object, as in NotePage.vue#restoreVersion
    note.setTitle(target.getTitle());
    note.setContent(target.getContent());
    try {
      noteService.restoreVersionOfNote(String.valueOf(target.getVersionNumber()), note, getCurrentUserName());
    } catch (Exception e) {
      throw new IllegalStateException("Could not restore the note version: " + e.getMessage());
    }
    return readNote(noteId, lang);
  }

  /**
   * Finds a version of a note's history by its number.
   *
   * @param note the note
   * @param lang the normalized language whose history to search, null for the
   *          default
   * @param noteId the note id, for the message
   * @param versionNumber the version number
   * @return the version
   * @throws ObjectNotFoundException when the history has no such version
   */
  private PageHistory getVersion(Page note, String lang, long noteId, long versionNumber) throws ObjectNotFoundException {
    List<PageHistory> history = versionsHistory(note, lang);
    return (history == null ? List.<PageHistory> of() : history).stream()
                                                                .filter(version -> version.getVersionNumber() != null
                                                                    && version.getVersionNumber() == versionNumber)
                                                                .findFirst()
                                                                .orElseThrow(() -> new ObjectNotFoundException(("Note with id %s has no version number %s. "
                                                                    + "Use get_note_versions to list the available versions.").formatted(noteId,
                                                                                                                                         versionNumber)));
  }

  /**
   * Lists a note's version history in a language, as the current user.
   *
   * @param note the note
   * @param lang the language as {@link #resolveLanguage(long, String)}
   *          resolved it, null for the default
   * @return the history, in the service's order
   */
  private List<PageHistory> versionsHistory(Page note, String lang) {
    try {
      return noteService.getVersionsHistoryOfNoteByLang(note, getCurrentUserName(), lang);
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
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
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
    return readNote(noteId, lang);
  }

  /**
   * Sets (or replaces) a note's summary — the short excerpt shown on note cards.
   * Only a user who can edit the note may set it; the note's cover image is
   * preserved.
   */
  public NoteModel setNoteSummary(long noteId, String summary, String language) throws IllegalAccessException,
                                                                               ObjectNotFoundException {
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
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
    return readNote(noteId, lang);
  }

  /**
   * Removes a note's cover ("featured") image. Only a user who can edit the note
   * may remove it.
   */
  public NoteModel removeNoteCover(long noteId, String language) throws IllegalAccessException, ObjectNotFoundException {
    String lang = resolveLanguage(noteId, language);
    Page note = getNoteById(noteId, lang);
    String username = getCurrentUserName();
    if (!noteService.canEditNote(note, username)) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED.formatted(noteId));
    }
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
    return readNote(noteId, lang);
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

  /**
   * Maps a version of a note to the model get_note_versions lists, without its
   * content.
   *
   * @param version the version
   * @return the version model
   */
  private NoteVersionModel toNoteVersionModel(PageHistory version) {
    return new NoteVersionModel(version.getVersionNumber(),
                                version.getId(),
                                version.getTitle(),
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
    return readNote(Long.parseLong(createdNote.getId()), null);
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

  /**
   * Maps a note to the model the note tools return, its whole content
   * sanitized.
   *
   * @param note the note
   * @return the note model
   */
  private NoteModel toNoteModel(Page note) {
    return toNoteModel(note, null);
  }

  /**
   * Maps a note to the model the note tools return, its whole content
   * sanitized.
   *
   * @param note the note, holding the version's title and content when one is
   *          read
   * @param versionNumber the number of the version read, null for the current
   *          note
   * @return the note model
   */
  @SneakyThrows
  private NoteModel toNoteModel(Page note, Long versionNumber) {
    NoteLinks links = new NoteLinks();
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
                         links.getUrl(note),
                         formatDate(note.getCreatedDate()),
                         formatDate(note.getUpdatedDate()),
                         note.isHasChild(),
                         canEdit,
                         toUserModel(note.getAuthor()),
                         toUserModel(note.getLastUpdater()),
                         toNoteBreadcrumb(note, links),
                         versionNumber,
                         null,
                         null,
                         null,
                         null);
  }

  /**
   * Cuts a part of a note's HTML content. The part starts at the offset, moved
   * back to the start of the tag, character reference or surrogate pair it
   * falls in, and ends at most maxChars characters further, moved back the
   * same way, so that no tag is split and the parts, read one after the other
   * from next_offset, join into the whole content. A part that can't end
   * before its first tag, reference or pair ends there, so a single tag
   * longer than maxChars is returned whole rather than reading nothing.
   *
   * @param model the note, with its whole HTML content
   * @param offset the first character, 0 when null
   * @param maxChars the most characters, {@value #DEFAULT_PART_CHARS} when null,
   *          clamped to 1 and {@value #MAX_PART_CHARS}
   * @return the note holding the part, its bounds, the whole content's length
   *         and where the next part starts
   */
  private NoteModel toNotePart(NoteModel model, Long offset, Integer maxChars) {
    String html = StringUtils.defaultString(model.htmlContent());
    int total = html.length();
    int size = Math.clamp(maxChars == null ? DEFAULT_PART_CHARS : maxChars, 1, MAX_PART_CHARS);
    int start = safeBoundary(html, Math.clamp(offset == null ? 0 : offset, 0, total));
    int end = safeBoundary(html, (int) Math.min((long) start + size, total));
    if (end <= start && start < total) {
      end = boundaryAfter(html, start);
    }
    return new NoteModel(model.noteId(),
                         model.title(),
                         model.summary(),
                         html.substring(start, end),
                         model.url(),
                         model.createdDate(),
                         model.updatedDate(),
                         model.hasChildNotes(),
                         model.canEdit(),
                         model.author(),
                         model.lastUpdater(),
                         model.breadcrumb(),
                         model.versionNumber(),
                         start,
                         end,
                         total,
                         end < total ? end : null);
  }

  /**
   * Moves a cut position of an HTML text back to where it splits nothing: to
   * the start of the tag or comment it falls in, of the character reference
   * it falls in, or of the surrogate pair it falls in. A quoted attribute value
   * holding a {@code >} doesn't end its tag.
   *
   * @param html the HTML text
   * @param position the cut position
   * @return the position, or the start of what it would split, between 0 and
   *         the text's length
   */
  static int safeBoundary(String html, int position) {
    if (position <= 0) {
      return 0;
    } else if (position >= html.length()) {
      return html.length();
    }
    int index = 0;
    while (index < position) {
      if (html.charAt(index) == '<') {
        int tagEnd = tagEnd(html, index);
        if (tagEnd > position) {
          return index;
        }
        index = tagEnd;
      } else {
        index++;
      }
    }
    for (int i = position - 1; i >= Math.max(0, position - MAX_ENTITY_LENGTH); i--) {
      char c = html.charAt(i);
      if (c == '&') {
        return i;
      } else if (c == ';' || c == '>' || Character.isWhitespace(c)) {
        break;
      }
    }
    return Character.isHighSurrogate(html.charAt(position - 1)) ? position - 1 : position;
  }

  /**
   * Finds the end of the tag, comment, character reference or surrogate pair
   * that starts at a position of an HTML text, or the next character.
   *
   * @param html the HTML text
   * @param start the position
   * @return the position after what starts there
   */
  static int boundaryAfter(String html, int start) {
    char c = html.charAt(start);
    if (c == '<') {
      return tagEnd(html, start);
    } else if (c == '&') {
      int semicolon = html.indexOf(';', start);
      return semicolon > start && semicolon - start < MAX_ENTITY_LENGTH ? semicolon + 1 : start + 1;
    } else if (Character.isHighSurrogate(c) && start + 1 < html.length()) {
      return start + 2;
    } else {
      return start + 1;
    }
  }

  /**
   * Finds the end of the tag or comment that starts at a position of an HTML
   * text. A {@code >} inside a quoted attribute value doesn't end the tag.
   *
   * @param html the HTML text
   * @param tagStart the position of the tag's {@code <}
   * @return the position after the tag's {@code >}, or the text's length
   *         when the tag isn't closed
   */
  private static int tagEnd(String html, int tagStart) {
    if (html.startsWith("<!--", tagStart)) {
      int close = html.indexOf("-->", tagStart + 4);
      return close < 0 ? html.length() : close + 3;
    }
    char quote = 0;
    for (int i = tagStart + 1; i < html.length(); i++) {
      char c = html.charAt(i);
      if (quote != 0) {
        if (c == quote) {
          quote = 0;
        }
      } else if (c == '"' || c == '\'') {
        quote = c;
      } else if (c == '>') {
        return i + 1;
      }
    }
    return html.length();
  }

  /**
   * Clamps the limit a list tool is called with.
   *
   * @param limit the requested limit
   * @return {@value McpToolPlugin#DEFAULT_LIMIT} when null or 0, otherwise the
   *         limit between 1 and {@value #MAX_LIMIT}
   */
  private static int clampLimit(Integer limit) {
    return Math.clamp(getInteger(limit, DEFAULT_LIMIT), 1, MAX_LIMIT);
  }

  /**
   * Maps a note found by a search to a compact hit, without its content.
   *
   * @param note the note
   * @param links the links of the notes the call returns
   * @param spaces the spaces already resolved in the call, by group id
   * @return the hit
   */
  private NoteSearchHitModel toNoteSearchHitModel(Page note, NoteLinks links, Map<String, NoteSpaceModel> spaces) {
    String summary = note.getProperties() != null ? note.getProperties().getSummary() : null;
    return new NoteSearchHitModel(Long.parseLong(note.getId()),
                                  note.getTitle(),
                                  summary,
                                  links.getUrl(note),
                                  formatDate(note.getUpdatedDate()),
                                  toNoteSpaceModel(note, spaces));
  }

  /**
   * Resolves the space whose notebook holds a note.
   *
   * @param note the note
   * @param spaces the spaces already resolved in the call, by group id
   * @return the space, null for a note outside a space notebook
   */
  private NoteSpaceModel toNoteSpaceModel(Page note, Map<String, NoteSpaceModel> spaces) {
    String groupId = note.getWikiOwner();
    if (!StringUtils.equalsIgnoreCase(SPACE_WIKI_TYPE, note.getWikiType()) || groupId == null
        || !groupId.startsWith(SPACES_PREFIX)) {
      return null;
    }
    return spaces.computeIfAbsent(groupId, id -> {
      Space space = spaceService.getSpaceByGroupId(id);
      return space == null ? null : new NoteSpaceModel(space.getSpaceId(), space.getDisplayName());
    });
  }

  private NoteRootTreeModel toNoteRootTreeModel(Page rootNote, long spaceId) {
    NoteLinks links = new NoteLinks();
    NoteRootTreeModel rootTreeModel = new NoteRootTreeModel(Long.parseLong(rootNote.getId()),
                                                            rootNote.getTitle(),
                                                            links.getUrl(rootNote),
                                                            null,
                                                            spaceId);
    addChildren(rootNote, rootTreeModel, links);
    return rootTreeModel;
  }

  @SneakyThrows
  private NoteTreeModel toNoteTreeModel(Page note, NoteLinks links) {
    NoteTreeModel noteTreeModel = new NoteTreeModel(Long.parseLong(note.getId()),
                                                    note.getTitle(),
                                                    links.getUrl(note),
                                                    null);
    addChildren(note, noteTreeModel, links);
    return noteTreeModel;
  }

  private List<NoteBreadcrumbModel> toNoteBreadcrumb(Page note, NoteLinks links) {
    List<NoteBreadcrumbModel> breadcrumb = new ArrayList<>();
    addNoteBreadcrumb(breadcrumb, note, links);
    return breadcrumb;
  }

  private void addNoteBreadcrumb(List<NoteBreadcrumbModel> breadcrumb, Page note, NoteLinks links) {
    breadcrumb.add(0,
                   new NoteBreadcrumbModel(Long.parseLong(note.getId()),
                                           note.getTitle(),
                                           links.getUrl(note)));
    if (StringUtils.isNotBlank(note.getParentPageId())) {
      Page parentNote = noteService.getNoteById(note.getParentPageId());
      addNoteBreadcrumb(breadcrumb, parentNote, links);
    }
  }

  private void addChildren(Page note, NoteTreeModel noteTreeModel, NoteLinks links) {
    Collection<Page> childNotes = noteService.getChildrenNoteOf(note, false, false);
    if (CollectionUtils.isNotEmpty(childNotes)) {
      childNotes.stream().map(child -> toNoteTreeModel(child, links)).forEach(noteTreeModel::addChildNote);
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
   * Resolves, once per call, the language a note call reads or writes, to be
   * used for the note read, its history, its metadata and its write alike. A
   * blank language is null, the default version, with no lookup. Otherwise the
   * trimmed code is matched against the codes the note's translations are
   * stored under ({@code Locale.toString()}: "fr", "es_ES"), because the version
   * queries compare {@code p.lang = :lang} exactly, case-sensitive on PostgreSQL
   * and HSQLDB: a wrong-case "FR" would otherwise read the default and write a
   * second translation next to "fr".
   *
   * @param noteId the note id, whose stored translation codes are listed
   * @param language the language as the caller gave it
   * @return the stored code it matches, the trimmed code when none does (a new
   *         translation), null for the default version
   */
  private String resolveLanguage(long noteId, String language) {
    String lang = StringUtils.trimToNull(language);
    return lang == null ? null : matchStoredLanguage(lang, availableTranslationLanguages(noteId));
  }

  /**
   * Matches a trimmed language code to the note's stored translation codes:
   * the code itself when it is stored, else the stored code equal to it
   * ignoring case, else the code as given. The stored codes come as a distinct
   * list in no order, so a case-insensitive match is taken in sorted order to
   * stay the same from one call to the next.
   *
   * @param lang the trimmed, non-blank code
   * @param storedLanguages the note's stored translation codes, or null
   * @return the matched stored code, or {@code lang}
   */
  private static String matchStoredLanguage(String lang, List<String> storedLanguages) {
    if (storedLanguages == null || storedLanguages.contains(lang)) {
      return lang;
    }
    return storedLanguages.stream()
                          .filter(stored -> StringUtils.equalsIgnoreCase(stored, lang))
                          .sorted()
                          .findFirst()
                          .orElse(lang);
  }

  /**
   * Reads a note in a language, as the current user. A null language means
   * the note's own default version, never the caller's UI locale: resolving it
   * from the profile locale made getNoteByIdAndLang overlay whatever
   * translation that user reads in, so every write started from one.
   *
   * @param noteId the note id
   * @param lang the language as {@link #resolveLanguage(long, String)}
   *          resolved it, null for the default
   * @return the note
   * @throws IllegalAccessException when the user can't view the note
   * @throws ObjectNotFoundException when the note doesn't exist
   */
  private Page getNoteById(long noteId, String lang) throws IllegalAccessException, ObjectNotFoundException {
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

  /**
   * Links the notes one tool call returns. A space note is linked through its
   * permanent link. A personal note has no permanent link
   * ({@code NotePermanentLinkPlugin} only resolves space notes), so it is linked
   * the way the Notes application opens it: under the navigation node hosting
   * the Notes application in a portal site of the current user (the personal
   * workspace, e.g. {@code /portal/myworkspace/dashboard/notes/<id>}). That
   * path depends on the caller, not on the note, so an instance resolves it at
   * most once, however many notes and breadcrumb levels the call links: one
   * instance serves one tool call. Any other notebook type, or a personal note
   * when no site of the user hosts a notes node, gets no URL: the field is then
   * omitted from the model instead of failing the call.
   */
  private class NoteLinks {

    private boolean personalNotesPathResolved;

    private String  personalNotesPath;

    /**
     * Builds the absolute URL of a note.
     *
     * @param note the note to link to
     * @return the note's absolute URL, or null when it can't be resolved
     */
    @SneakyThrows
    String getUrl(Page note) {
      if (StringUtils.equalsIgnoreCase(SPACE_WIKI_TYPE, note.getWikiType())) {
        return CommonsUtils.getCurrentDomain() +
            permanentLinkService.getLink(new PermanentLinkObject(NotePermanentLinkPlugin.OBJECT_TYPE, note.getId()));
      } else if (StringUtils.equalsIgnoreCase(USER_WIKI_TYPE, note.getWikiType())) {
        String notesPath = getPersonalNotesPath();
        return notesPath == null ? null : CommonsUtils.getCurrentDomain() + notesPath + "/" + note.getId();
      } else {
        return null;
      }
    }

    private String getPersonalNotesPath() {
      if (!personalNotesPathResolved) {
        personalNotesPath = NoteMcpTool.this.getPersonalNotesPath(getCurrentUserName());
        personalNotesPathResolved = true;
      }
      return personalNotesPath;
    }
  }

  /**
   * Finds the path of the page hosting the Notes application for a personal
   * notebook: the first displayed portal site, in display order, whose
   * navigation, as seen by the user, holds a {@code notes} node. Outside a
   * space the Notes application shows the viewer's personal notebook, so that
   * node opens any of the user's personal notes by id.
   *
   * @param username the user the navigation is loaded for
   * @return the page path, e.g. {@code /portal/myworkspace/dashboard/notes},
   *         or null when no site of the user hosts a notes node
   */
  private String getPersonalNotesPath(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    List<PortalConfig> sites = layoutService.getSites(PORTAL_SITES);
    if (sites == null) {
      return null;
    }
    for (PortalConfig site : sites) {
      try {
        UserNode rootNode = portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, site.getName(), username, false);
        UserNode notesNode = findNotesNode(rootNode);
        if (notesNode != null) {
          return PORTAL_URI + site.getName() + "/" + notesNode.getURI();
        }
      } catch (Exception e) {
        LOG.debug("Can't read the navigation of site {} to link a personal note", site.getName(), e);
      }
    }
    return null;
  }

  /**
   * Searches a navigation tree, breadth first, for the node named
   * {@code notes}.
   *
   * @param rootNode the root node of a site navigation, possibly null
   * @return the shallowest node named {@code notes}, or null
   */
  private UserNode findNotesNode(UserNode rootNode) {
    if (rootNode == null || rootNode.getChildren() == null) {
      return null;
    }
    Deque<UserNode> nodesToVisit = new ArrayDeque<>(rootNode.getChildren());
    while (!nodesToVisit.isEmpty()) {
      UserNode node = nodesToVisit.poll();
      if (NOTES_NODE_NAME.equals(node.getName())) {
        return node;
      }
      if (node.getChildren() != null) {
        nodesToVisit.addAll(node.getChildren());
      }
    }
    return null;
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
