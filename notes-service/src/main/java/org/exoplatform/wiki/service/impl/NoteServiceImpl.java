/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.service.impl;

import static io.meeds.notes.service.TermsAndConditionsService.TC_NOTE_NAME;
import static io.meeds.notes.service.TermsAndConditionsService.TC_NOTE_TYPE;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.LocaleUtils;
import org.apache.commons.lang3.StringUtils;
import org.gatein.api.EntityNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.thumbnail.ImageThumbnailService;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.attachment.model.UploadedAttachmentDetail;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.ImportList;
import org.exoplatform.wiki.model.NoteToExport;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.plugin.WikiDraftPageAttachmentPlugin;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.storage.NoteDataStorage;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NoteMetadataObject;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.notes.plugin.NoteContentLinkPlugin;
import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.html.model.HtmlProcessorContext;
import io.meeds.social.html.utils.HtmlUtils;

import lombok.SneakyThrows;

public class NoteServiceImpl implements NoteService {

  private static final String                             DRAFT_PAGE_ID_PROP_NAME                = "draftPageId";

  private static final String                             PAGE_VERSION_ID_PROP_NAME              = "pageVersionId";

  private static final String                             UNTITLED_PREFIX                        = "Untitled_";

  private static final String                             TEMP_DIRECTORY_PATH                    = "java.io.tmpdir";

  private static final String                             FEATURED_IMAGE_FOLDER                  = "featuredImages";

  private static final String                             FILE_NAME_SPACE                        = "wiki";

  private static final String                             IMAGE_URL_REPLACEMENT_PREFIX           = "//-";

  private static final String                             IMAGE_URL_REPLACEMENT_SUFFIX           = "-//";

  private static final String                             CONTENT_LINK_TAG                       = "content-link";

  private static final String                             CONTENT_LINK_TAG_REPLACEMENT           = "--content--link-to-insert";

  private static final Pattern                            IMAGES_IMPORT_PATTERN                  =
                                                                                Pattern.compile("src=\"//-(.*?)-//\"");

  private static final Log                                log                                    =
                                                              ExoLogger.getLogger(NoteServiceImpl.class);

  private static final MetadataType                       NOTES_METADATA_TYPE                    =
                                                                              new MetadataType(1001, "notes");

  private static final MetadataKey                        NOTES_METADATA_KEY                     =
                                                                             new MetadataKey(NOTES_METADATA_TYPE.getName(),
                                                                                             Utils.NOTES_METADATA_OBJECT_TYPE,
                                                                                             0);

  public static final String                              NOTE_METADATA_PAGE_OBJECT_TYPE         = "notePage";

  public static final String                              NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE   = "noteDraftPage";

  public static final String                              NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE = "noteVersionPage";

  public static final String                              SUMMARY_PROP                           = "summary";

  public static final String                              HIDE_AUTHOR_PROP                       = "hideAuthor";

  public static final String                              HIDE_REACTION_PROP                     = "hideReaction";

  public static final String                              FEATURED_IMAGE_ID                      = "featuredImageId";

  public static final String                              FEATURED_IMAGE_UPDATED_DATE            = "featuredImageUpdatedDate";

  public static final String                              FEATURED_IMAGE_ALT_TEXT                = "featuredImageAltText";

  public static final String                              NOTE_DELETED                           = "note.deleted";

  private final WikiService                               wikiService;

  private final NoteDataStorage                           dataStorage;

  private final IdentityManager                           identityManager;

  private final SpaceService                              spaceService;

  private final CMSService                                cmsService;

  private final ListenerService                           listenerService;

  private final FileService                               fileService;

  private final UploadService                             uploadService;

  private final MetadataService                           metadataService;

  private final ImageThumbnailService                     imageThumbnailService;

  private final AttachmentService                         attachmentService;

  private final LocaleConfigService                       localeConfigService;

  private final LayoutService                             layoutService;

  private final UserACL                                   userAcl;

  public NoteServiceImpl(NoteDataStorage dataStorage, // NOSONAR
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         ListenerService listenerService,
                         LocaleConfigService localeConfigService,
                         FileService fileService,
                         UploadService uploadService,
                         MetadataService metadataService,
                         ImageThumbnailService imageThumbnailService,
                         AttachmentService attachmentService,
                         LayoutService layoutService,
                         UserACL userAcl) {
    this.userAcl = userAcl;
    this.layoutService = layoutService;
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.localeConfigService = localeConfigService;
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.metadataService = metadataService;
    this.imageThumbnailService = imageThumbnailService;
    this.attachmentService = attachmentService;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page createNote(Wiki noteBook,
                         String parentNoteName,
                         Page note,
                         Identity userIdentity,
                         boolean importMode,
                         boolean broadcast) throws WikiException, IllegalAccessException {
    if (userIdentity == null
        || !wikiService.canManageWiki(noteBook.getType(), noteBook.getOwner(), userIdentity.getUserId())) {
      throw new IllegalAccessException(String.format("Authorization error while addin new note in wiki %s:%s",
                                                     noteBook.getType(),
                                                     noteBook.getOwner()));
    }
    if (importMode) {
      String pageName = TitleResolver.getId(note.getName(), false);
      if (pageName == null) {
        pageName = TitleResolver.getId(note.getTitle(), false);
      }
      note.setName(pageName);
    }
    if (isExisting(noteBook.getType(), noteBook.getOwner(), note.getName())) {
      throw new WikiException("Page " + noteBook.getType() + ":" + noteBook.getOwner() + ":" + note.getName() +
          " already exists, cannot create it.");
    }

    Page parentPage = getNoteOfNoteBookByName(noteBook.getType(), noteBook.getOwner(), parentNoteName);
    if (parentPage != null) {
      note.setOwner(userIdentity.getUserId());
      note.setAuthor(userIdentity.getUserId());
      note.setContent(note.getContent());
      Page createdPage = createNote(noteBook, parentPage, note, broadcast);
      NotePageProperties properties = note.getProperties();
      String draftPageId = properties != null && properties.isDraft() ? String.valueOf(properties.getNoteId()) : null;
      try {
        if (properties != null) {
          properties.setNoteId(Long.parseLong(createdPage.getId()));
          properties.setDraft(false);
          properties = saveNoteMetadata(properties,
                                        note.getLang(),
                                        Long.valueOf(identityManager.getOrCreateUserIdentity(userIdentity.getUserId()).getId()));
        }
      } catch (Exception e) {
        log.error("Failed to save note metadata", e);
      }
      createdPage.setProperties(properties);
      if (StringUtils.isNotEmpty(createdPage.getContent())) {
        createdPage.setAttachmentObjectType(note.getAttachmentObjectType());
        createdPage = processImagesOnNoteCreation(createdPage,
                                                  draftPageId,
                                                  Long.valueOf(identityManager.getOrCreateUserIdentity(userIdentity.getUserId())
                                                                              .getId()));
      }
      createdPage.setCanManage(canEditNote(note, note.getAuthor()));
      createdPage.setCanImport(canEditNote(note, note.getAuthor()));
      createdPage.setCanView(canViewNote(note, note.getAuthor()));
      dataStorage.addPageVersion(createdPage, userIdentity.getUserId());
      PageVersion pageVersion = dataStorage.getPublishedVersionByPageIdAndLang(Long.valueOf(createdPage.getId()),
                                                                               createdPage.getLang());
      createdPage.setLatestVersionId(pageVersion != null ? pageVersion.getId() : null);
      if (pageVersion != null && draftPageId != null) {
        Map<String, String> eventData = new HashMap<>();
        eventData.put(DRAFT_PAGE_ID_PROP_NAME, draftPageId);
        eventData.put(PAGE_VERSION_ID_PROP_NAME, pageVersion.getId());
        Utils.broadcast(listenerService, "note.page.version.created", this, eventData);
      }
      return createdPage;
    } else {
      throw new EntityNotFoundException("Parent note not found");
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException,
                                                                                                 IllegalAccessException {
    return createNote(noteBook, parentNoteName, note, userIdentity, true);
  }

  @Override
  public Page createNote(Wiki noteBook,
                         String parentNoteName,
                         Page note,
                         Identity userIdentity,
                         boolean broadcast) throws WikiException, IllegalAccessException {
    return createNote(noteBook, parentNoteName, note, userIdentity, true, broadcast);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page createNote(Wiki noteBook, Page parentPage, Page note) throws WikiException {
    return createNote(noteBook, parentPage, note, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page createNote(Wiki noteBook, Page parentPage, Page note, boolean broadcast) throws WikiException {
    Page createdPage = dataStorage.createPage(noteBook, parentPage, note);
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setAppName(note.getAppName());
    createdPage.setUrl(Utils.getPageUrl(createdPage));
    createdPage.setLang(note.getLang());

    Utils.broadcast(listenerService, "note.posted", note.getAuthor(), createdPage);
    processPageContent(createdPage, note.getLang());
    if (broadcast) {
      postAddPage(noteBook.getType(), noteBook.getOwner(), note.getName(), createdPage);
      Matcher mentionMatcher = Utils.MENTION_PATTERN.matcher(createdPage.getContent());
      if (mentionMatcher.find()) {
        Utils.sendMentionInNoteNotification(createdPage, null, createdPage.getAuthor());
      }
    }
    return createdPage;
  }

  /**
   * {@inheritDoc}
   */
  @SneakyThrows
  @Override
  public Page updateNote(Page note, PageUpdateType type) throws WikiException {
    return saveNote(note, type, null, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws Exception {
    return updateNote(note, type, userIdentity, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page updateNote(Page note, PageUpdateType type, Identity userIdentity, boolean broadcast) throws Exception {
    Page existingNote = getNoteById(note.getId());
    if (existingNote == null) {
      throw new EntityNotFoundException("Note to update not found");
    } else if (userIdentity == null || !canEditNote(existingNote, userIdentity.getUserId())) {
      throw new IllegalAccessException("User does not have edit the note.");
    }
    if (PageUpdateType.EDIT_PAGE_TITLE.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)
        || PageUpdateType.EDIT_PAGE_PROPERTIES.equals(type)) {
      note.setUpdatedDate(Calendar.getInstance().getTime());
    }
    return saveNote(note, type, userIdentity, broadcast);
  }

  @Override
  public Page updateNote(Page note) {
    return saveNote(note, null, null, false);
  }

  @Override
  public void deleteNote(String noteId) {
    dataStorage.deletePage(noteId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteNote(String wikiType, String wikiOwner, String noteName) {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    dataStorage.deletePage(wikiType, wikiOwner, noteName);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteNote(String wikiType, String wikiOwner, String noteName, Identity userIdentity) throws WikiException,
                                                                                                       IllegalAccessException,
                                                                                                       EntityNotFoundException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    Page note = getNoteOfNoteBookByName(wikiType, wikiOwner, noteName);
    if (note == null) {
      log.error("Can't delete note '" + noteName + "'. This note does not exist.");
      throw new EntityNotFoundException("Note to delete not found");
    } else if (userIdentity == null || !canEditNote(note, userIdentity.getUserId())) {
      throw new IllegalAccessException("User does not have edit permissions on the note.");
    }

    // Store all children to launch post deletion listeners
    List<Page> allChrildrenPages = new ArrayList<>();
    Queue<Page> queue = new LinkedList<>();
    queue.add(note);
    Page tempPage;
    while (!queue.isEmpty()) {
      tempPage = queue.poll();
      List<Page> childrenPages = getChildrenNoteOf(tempPage, false, false);
      for (Page childPage : childrenPages) {
        queue.add(childPage);
        allChrildrenPages.add(childPage);
      }
    }

    deleteNote(wikiType, wikiOwner, noteName);
    postDeletePage(wikiType, wikiOwner, noteName, note);
    Utils.broadcast(listenerService, NOTE_DELETED, userIdentity, note);
    processPageContent(note.getId(), "", null);
    // Post delete activity for all children pages
    for (Page childNote : allChrildrenPages) {
      postDeletePage(childNote.getWikiType(), childNote.getWikiOwner(), childNote.getName(), childNote);
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean renameNote(String noteType,
                            String noteOwner,
                            String noteName,
                            String newName,
                            String newTitle) throws WikiException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    if (!noteName.equals(newName) && isExisting(noteType, noteOwner, newName)) {
      throw new WikiException("Note " + noteType + ":" + noteOwner + ":" + newName + " already exists, cannot rename it.");
    }

    dataStorage.renamePage(noteType, noteOwner, noteName, newName, newTitle);

    // Invaliding cache
    Page page = new Page(noteName);
    page.setWikiType(noteType);
    page.setWikiOwner(noteOwner);
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void moveNote(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    dataStorage.movePage(currentLocationParams, newLocationParams);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean moveNote(WikiPageParams currentLocationParams,
                          WikiPageParams newLocationParams,
                          Identity userIdentity) throws WikiException, IllegalAccessException, EntityNotFoundException {
    try {
      Page moveNote = getNoteOfNoteBookByName(currentLocationParams.getType(),
                                              currentLocationParams.getOwner(),
                                              currentLocationParams.getPageName());
      if (moveNote == null) {
        throw new EntityNotFoundException("Note to move not found");
      } else if (userIdentity == null || !canEditNote(moveNote, userIdentity.getUserId())) {
        throw new IllegalAccessException("User does not have edit the note.");
      }
      moveNote(currentLocationParams, newLocationParams);

      Page note = new Page(currentLocationParams.getPageName());
      note.setWikiType(currentLocationParams.getType());
      note.setWikiOwner(currentLocationParams.getOwner());

      postUpdatePage(newLocationParams.getType(),
                     newLocationParams.getOwner(),
                     moveNote.getName(),
                     moveNote,
                     PageUpdateType.MOVE_PAGE);
    } catch (WikiException e) {
      log.error("Can't move note '" + currentLocationParams.getPageName() + "' ", e);
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName) throws WikiException {
    Page page = null;

    // check in the cache first
    page = dataStorage.getPageOfWikiByName(noteType, noteOwner, noteName);
    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteOfNoteBookByName(String noteType,
                                      String noteOwner,
                                      String noteName,
                                      Identity userIdentity,
                                      String source) throws IllegalAccessException, WikiException {
    Page page = getNoteOfNoteBookByName(noteType, noteOwner, noteName, userIdentity);
    if (StringUtils.isNotEmpty(source)) {
      if (source.equals("tree")) {
        postOpenByTree(noteType, noteOwner, noteName, page);
      }
      if (source.equals("breadCrumb")) {
        postOpenByBreadCrumb(noteType, noteOwner, noteName, page);
      }
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteOfNoteBookByName(String noteType,
                                      String noteOwner,
                                      String noteName,
                                      String lang,
                                      Identity userIdentity) throws WikiException, IllegalAccessException {
    Page page = getNoteOfNoteBookByName(noteType, noteOwner, noteName, userIdentity);
    if (lang != null) {
      page.setMetadatas(retrieveMetadataItems(page.getId() + "-" + lang, userIdentity.getUserId()));
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteOfNoteBookByName(String noteType,
                                      String noteOwner,
                                      String noteName,
                                      Identity userIdentity) throws IllegalAccessException, WikiException {
    Page page = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (page == null) {
      throw new EntityNotFoundException("page not found");
    } else if (!canViewNote(page, userIdentity.getUserId())) {
      throw new IllegalAccessException("User does not have view the note.");
    }
    page.setCanView(true);
    page.setCanManage(canEditNote(page, userIdentity.getUserId()));
    page.setCanImport(wikiService.canManageWiki(page.getWikiType(), page.getWikiOwner(), userIdentity.getUserId()));
    Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(page.getId(), userIdentity.getUserId());
    page.setMetadatas(metadata);
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    return dataStorage.getPageById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getDraftNoteById(String id, String userId) throws IllegalAccessException {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    DraftPage draftPage = getDraftNoteById(id);
    computeDraftProps(draftPage, userId);
    return draftPage;
  }

  @Override
  public DraftPage getDraftNoteById(String id) {
    if (StringUtils.isBlank(id)) {
      return null;
    }
    return dataStorage.getDraftPageById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getLatestDraftOfPage(Page targetPage) throws WikiException {
    if (targetPage == null) {
      return null;
    }
    return dataStorage.getLatestDraftOfPage(targetPage);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getLatestDraftOfPage(Page targetPage, String username) throws WikiException {
    return getLatestDraftOfPage(targetPage);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteById(String id, Identity userIdentity) throws IllegalAccessException, WikiException {
    Page page = getNoteById(id);
    if (page == null) {
      return null;
    } else if (!canViewNote(page, userIdentity.getUserId())) {
      throw new IllegalAccessException("User does not have view the note.");
    }
    page.setCanView(true);
    page.setCanManage(canEditNote(page, userIdentity.getUserId()));
    page.setCanImport(canEditNote(page, userIdentity.getUserId()));
    return page;
  }

  @Override
  public boolean canViewNote(Page page, String username) {
    if (page == null) {
      return false;
    }
    String wikiOwner = page.getWikiOwner();
    String wikiType = page.getWikiType();
    String pageOwner = page.getOwner();

    if (page.isDraftPage()) {
      return canEditNote(page, username);
    } else if (TC_NOTE_TYPE.equals(wikiType) && TC_NOTE_NAME.equals(page.getName())) {
      return true;
    } else if ((StringUtils.equals(pageOwner, IdentityConstants.SYSTEM) || StringUtils.isBlank(pageOwner))
               && cmsService.hasAccessPermission(Utils.getIdentity(username),
                                                 NotePageViewService.CMS_CONTENT_TYPE,
                                                 page.getName())) {
      return true;
    } else if (StringUtils.equalsIgnoreCase(WikiType.PORTAL.name(), wikiType)) {
      PortalConfig portalConfig = layoutService.getPortalConfig(wikiOwner);
      return userAcl.hasAccessPermission(portalConfig, userAcl.getUserIdentity(username));
    } else if (StringUtils.isBlank(wikiType) || StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      Space space = getSpaceByWiki(wikiType, wikiOwner);
      return space == null ? userAcl.hasPermission(userAcl.getUserIdentity(username),
                                                   String.format("%s:%s",
                                                                 userAcl.getAdminMSType(),
                                                                 wikiOwner)) :
                           spaceService.canViewSpace(space, username);
    } else if (StringUtils.isBlank(wikiType) || StringUtils.equalsIgnoreCase(WikiType.USER.name(), wikiType)) {
      return wikiOwner.equals(username);
    } else {
      return spaceService.isSuperManager(username) || StringUtils.equals(pageOwner, username);
    }
  }

  @Override
  public boolean canEditNote(Page page, String username) {
    if (page == null) {
      return false;
    } else if (wikiService.canManageWiki(page.getWikiType(), page.getWikiOwner(), username)) {
      return true;
    } else if (page.isDraftPage()) {
      return page.getParent() == null && StringUtils.equals(username, page.getAuthor());
    } else if ((StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM) || StringUtils.isBlank(page.getOwner()))
               && cmsService.hasEditPermission(Utils.getIdentity(username),
                                               NotePageViewService.CMS_CONTENT_TYPE,
                                               page.getName())) {
      return true;
    } else if (StringUtils.equalsIgnoreCase(WikiType.PORTAL.name(), page.getWikiType())) {
      PortalConfig portalConfig = layoutService.getPortalConfig(page.getWikiOwner());
      return userAcl.hasEditPermission(portalConfig, userAcl.getUserIdentity(username));
    } else {
      return false;
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteById(String id, Identity userIdentity, String source) throws IllegalAccessException, WikiException {
    Page page = getNoteById(id);
    String username = userIdentity == null ? null : userIdentity.getUserId();
    if (page == null) {
      return null;
    } else if (!canViewNote(page, username)) {
      throw new IllegalAccessException("User does not have view the note.");
    }
    page.setCanView(true);
    page.setUrl(Utils.getPageUrl(page));
    boolean canManageNotes = wikiService.canManageWiki(page.getWikiType(), page.getWikiOwner(), username);
    page.setCanManage(canManageNotes || canEditNote(page, username));
    page.setCanImport(canManageNotes);
    if (username != null) {
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(id, username);
      page.setMetadatas(metadata);
    }
    if (StringUtils.isNotBlank(source)) {
      if (source.equals("tree")) {
        postOpenByTree(page.getWikiType(), page.getWikiOwner(), page.getName(), page);
      }
      if (source.equals("breadCrumb")) {
        postOpenByBreadCrumb(page.getWikiType(), page.getWikiOwner(), page.getName(), page);
      }
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getParentNoteOf(Page note) throws WikiException {
    return dataStorage.getParentPageOf(note);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NoteToExport getParentNoteOf(NoteToExport note) throws WikiException {
    Page page = new Page();
    page.setId(note.getId());
    page.setName(note.getName());
    page.setWikiId(note.getWikiId());
    page.setWikiOwner(note.getWikiOwner());
    page.setWikiType(note.getWikiType());

    Page parent = getParentNoteOf(page);
    if (parent == null) {
      return null;
    }
    return new NoteToExport(parent.getId(),
                            parent.getName(),
                            parent.getOwner(),
                            parent.getAuthor(),
                            parent.getContent(),
                            parent.getSyntax(),
                            parent.getTitle(),
                            parent.getComment(),
                            parent.getWikiId(),
                            parent.getWikiType(),
                            parent.getWikiOwner());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Page> getChildrenNoteOf(Page note, boolean withDrafts, boolean withChild) throws WikiException {
    return dataStorage.getChildrenPageOf(note, withDrafts, withChild);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasChildren(long pageId) {
    return dataStorage.hasChildren(pageId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasDrafts(long pageId) {
    return dataStorage.hasDrafts(pageId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Page> getChildrenNoteOf(Page note, String userId, boolean withDrafts, boolean withChild) throws WikiException {
    return getChildrenNoteOf(note, withDrafts, withChild);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NoteToExport> getChildrenNoteOf(NoteToExport note) throws WikiException {

    Page page = new Page();
    page.setId(note.getId());
    page.setName(note.getName());
    page.setWikiId(note.getWikiId());
    page.setWikiOwner(note.getWikiOwner());
    page.setWikiType(note.getWikiType());

    List<Page> pages = getChildrenNoteOf(page, false, false);
    List<NoteToExport> children = new ArrayList<>();

    for (Page child : pages) {
      if (child == null) {
        continue;
      }
      children.add(new NoteToExport(child.getId(),
                                    child.getName(),
                                    child.getOwner(),
                                    child.getAuthor(),
                                    child.getContent(),
                                    child.getSyntax(),
                                    child.getTitle(),
                                    child.getComment(),
                                    child.getWikiId(),
                                    child.getWikiType(),
                                    child.getWikiOwner()));
    }
    return children;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<NoteToExport> getChildrenNoteOf(NoteToExport note, String userId) throws WikiException {
    return getChildrenNoteOf(note);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BreadcrumbData> getBreadCrumb(String noteType,
                                            String noteOwner,
                                            String noteName,
                                            boolean isDraftNote) throws WikiException, IllegalAccessException {
    return getBreadCrumb(null, noteType, noteOwner, noteName, null, null, isDraftNote);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<BreadcrumbData> getBreadCrumb(String noteType,
                                            String noteOwner,
                                            String noteName,
                                            String lang,
                                            Identity userIdentity,
                                            boolean isDraftNote) throws WikiException, IllegalAccessException {
    return getBreadCrumb(null, noteType, noteOwner, noteName, lang, userIdentity, isDraftNote);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Page> getDuplicateNotes(Page parentNote,
                                      Wiki targetNoteBook,
                                      List<Page> resultList) throws WikiException {
    if (resultList == null) {
      resultList = new ArrayList<>();
    }

    // if the result list have more than 6 elements then return
    if (resultList.size() > 6) {
      return resultList;
    }

    // if parent note is duppicated then add to list
    if (isExisting(targetNoteBook.getType(), targetNoteBook.getOwner(), parentNote.getName())) {
      resultList.add(parentNote);
    }

    // Check the duplication of all children
    List<Page> childrenNotes = getChildrenNoteOf(parentNote, false, false);
    for (Page note : childrenNotes) {
      getDuplicateNotes(note, targetNoteBook, resultList);
    }
    return resultList;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Page> getDuplicateNotes(Page parentNote,
                                      Wiki targetNoteBook,
                                      List<Page> resultList,
                                      String userId) throws WikiException {
    return getDuplicateNotes(parentNote, targetNoteBook, resultList);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftOfNote(WikiPageParams param) throws WikiException {
    Page page = getNoteOfNoteBookByName(param.getType(), param.getOwner(), param.getPageName());
    removeDraftOfNote(page);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftOfNote(Page page, String username) throws WikiException {
    removeDraftOfNote(page);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftOfNote(Page page) throws WikiException {
    List<DraftPage> draftPages = dataStorage.getDraftsOfPage(Long.valueOf(page.getId()));
    for (DraftPage draftPage : draftPages) {
      if (draftPage != null) {
        try {
          deleteNoteMetadataProperties(draftPage, draftPage.getLang(), NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE);
        } catch (Exception e) {
          log.error("Error while deleting draft properties");
        }
      }
    }
    dataStorage.deleteDraftOfPage(page);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getDraftOfPageByLang(WikiPageParams param, String lang) throws WikiException {
    Page page = getNoteOfNoteBookByName(param.getType(), param.getOwner(), param.getPageName());
    return dataStorage.getDraftOfPageByLang(page, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftOfNote(WikiPageParams param, String lang) throws Exception {
    DraftPage draftPage = getDraftOfPageByLang(param, lang);
    if (draftPage != null) {
      removeDraftById(draftPage.getId());
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraft(String draftName) throws WikiException {
    dataStorage.deleteDraftByName(draftName);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftById(String draftId) throws Exception {
    DraftPage draftNote = dataStorage.getDraftPageById(draftId);
    deleteNoteMetadataProperties(draftNote, draftNote.getLang(), NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE);
    dataStorage.deleteDraftById(draftId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PageHistory> getVersionsHistoryOfNote(Page note) {
    return dataStorage.getHistoryOfPage(note);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createVersionOfNote(Page note, String userName, boolean broadcast) throws WikiException {
    DraftPage draftPage = dataStorage.getLatestDraftPageByTargetPageAndLang(Long.valueOf(note.getId()), note.getLang());
    PageVersion previousPageVersion = getPublishedVersionByPageIdAndLang(Long.parseLong(note.getId()), note.getLang());
    PageVersion pageVersion = dataStorage.addPageVersion(note, userName);
    pageVersion.setAttachmentObjectType(note.getAttachmentObjectType());
    updateVersionContentImages(pageVersion);
    String pageVersionId = pageVersion.getId();
    note.setLatestVersionId(pageVersionId);
    if (note.getLang() != null) {
      try {
        NotePageProperties properties = note.getProperties();
        if (properties != null) {
          properties.setNoteId(Long.parseLong(note.getId()));
          properties.setDraft(false);
          saveNoteMetadata(properties, note.getLang(), Long.valueOf(identityManager.getOrCreateUserIdentity(userName).getId()));
        }
      } catch (Exception e) {
        log.error("Error while saving note version language metadata", e);
      }
      if (broadcast) {
        String versionLangId = note.getId() + "-" + note.getLang();
        postUpdatePageVersionLanguage(versionLangId);
      }
    } else {
      pageVersion.setId(note.getId() + "-" + pageVersion.getName());
      copyNotePageProperties(note,
                             pageVersion,
                             note.getLang(),
                             null,
                             NOTE_METADATA_PAGE_OBJECT_TYPE,
                             NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE,
                             userName);
    }
    broadcastPageVersionCreationEvent(pageVersionId,
                                      draftPage != null ? draftPage.getId() : null,
                                      previousPageVersion != null ? previousPageVersion.getId() : null);
    processPageContent(note, note.getLang());
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createVersionOfNote(Page note, String userName) throws WikiException {
    createVersionOfNote(note, userName, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void restoreVersionOfNote(String versionName, Page note, String userName) throws WikiException {
    PageVersion pageVersion = dataStorage.restoreVersionOfPage(versionName, note);
    pageVersion.setId(note.getId() + "-" + pageVersion.getName());
    copyNotePageProperties(pageVersion,
                           note,
                           null,
                           null,
                           NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE,
                           NOTE_METADATA_PAGE_OBJECT_TYPE,
                           userName);
    createVersionOfNote(note, userName, true);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getPreviousNamesOfNote(Page note) throws WikiException {
    return dataStorage.getPreviousNamesOfPage(note);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Page> getNotesOfWiki(String noteType, String noteOwner) {
    return dataStorage.getPagesOfWiki(noteType, noteOwner);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isExisting(String noteBookType, String noteBookOwner, String noteId) throws WikiException {
    return getNoteByRootPermission(noteBookType, noteBookOwner, noteId) != null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage updateDraftForExistPage(DraftPage draftNoteToUpdate,
                                           Page targetPage,
                                           String revision,
                                           long clientTime,
                                           String username) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);
    Long userIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setId(draftNoteToUpdate.getId());
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftNoteToUpdate.getTitle());
    newDraftPage.setTargetPageId(draftNoteToUpdate.getTargetPageId());
    newDraftPage.setParentPageId(draftNoteToUpdate.getParentPageId());
    newDraftPage.setContent(processImagesOnDraftUpdate(draftNoteToUpdate, userIdentityId));
    newDraftPage.setLang(draftNoteToUpdate.getLang());
    newDraftPage.setSyntax(draftNoteToUpdate.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    if (StringUtils.isEmpty(revision)) {
      List<PageHistory> versions = getVersionsHistoryOfNote(targetPage);
      if (versions != null && !versions.isEmpty()) {
        newDraftPage.setTargetPageRevision(String.valueOf(versions.get(0).getVersionNumber()));
      } else {
        newDraftPage.setTargetPageRevision("1");
      }
    } else {
      newDraftPage.setTargetPageRevision(revision);
    }
    newDraftPage = dataStorage.updateDraftPageForUser(newDraftPage, username);
    NotePageProperties properties = draftNoteToUpdate.getProperties();
    try {
      if (properties != null) {
        NoteFeaturedImage featuredImage = properties.getFeaturedImage();
        if (featuredImage != null && featuredImage.getUploadId() != null
            && isOriginalFeaturedImage(draftNoteToUpdate, targetPage)) {
          featuredImage.setId(0L);
        }
        properties = saveNoteMetadata(properties,
                                      newDraftPage.getLang(),
                                      Long.valueOf(identityManager.getOrCreateUserIdentity(username).getId()));
      }
    } catch (Exception e) {
      log.error("Failed to save draft note metadata", e);
    }
    newDraftPage.setProperties(properties);
    return newDraftPage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage updateDraftForNewPage(DraftPage draftNoteToUpdate, long clientTime, long userIdentityId) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setId(draftNoteToUpdate.getId());
    newDraftPage.setName(UNTITLED_PREFIX + draftSuffix);
    newDraftPage.setNewPage(true);
    newDraftPage.setTitle(draftNoteToUpdate.getTitle());
    newDraftPage.setTargetPageId(draftNoteToUpdate.getTargetPageId());
    newDraftPage.setParentPageId(draftNoteToUpdate.getParentPageId());
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setContent(processImagesOnDraftUpdate(draftNoteToUpdate, userIdentityId));
    newDraftPage.setLang(draftNoteToUpdate.getLang());
    newDraftPage.setSyntax(draftNoteToUpdate.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    org.exoplatform.social.core.identity.model.Identity userIdentity = identityManager.getIdentity(userIdentityId);
    newDraftPage = dataStorage.updateDraftPageForUser(newDraftPage, userIdentity.getRemoteId());
    NotePageProperties properties = draftNoteToUpdate.getProperties();
    try {
      properties = saveNoteMetadata(properties, newDraftPage.getLang(), userIdentityId);
    } catch (Exception e) {
      log.error("Failed to save draft note metadata", e);
    }
    newDraftPage.setProperties(properties);
    return newDraftPage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage createDraftForExistPage(DraftPage draftPage,
                                           Page targetPage,
                                           String revision,
                                           long clientTime,
                                           String username) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    org.exoplatform.social.core.identity.model.Identity userIdentity = identityManager.getOrCreateUserIdentity(username);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setTargetPageId(targetPage.getId());
    newDraftPage.setParentPageId(draftPage.getParentPageId());
    newDraftPage.setLang(draftPage.getLang());
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    if (StringUtils.isEmpty(revision)) {
      List<PageHistory> versions = getVersionsHistoryOfNote(targetPage);
      if (versions != null && !versions.isEmpty()) {
        newDraftPage.setTargetPageRevision(String.valueOf(versions.get(0).getVersionNumber()));
      } else {
        newDraftPage.setTargetPageRevision("1");
      }
    } else {
      newDraftPage.setTargetPageRevision(revision);
    }
    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, username);
    NotePageProperties properties = draftPage.getProperties();
    try {
      if (properties != null) {
        NoteFeaturedImage featuredImage = properties.getFeaturedImage();
        if (featuredImage != null && featuredImage.getUploadId() != null) {
          featuredImage.setId(0L);
        }
        properties.setNoteId(Long.parseLong(newDraftPage.getId()));
        properties = saveNoteMetadata(properties, draftPage.getLang(), Long.valueOf(userIdentity.getId()));
      }
    } catch (Exception e) {
      log.error("Failed to save draft note metadata", e);
    }
    newDraftPage.setProperties(properties);
    newDraftPage.setAttachmentObjectType(draftPage.getAttachmentObjectType());
    newDraftPage = processImagesOnDraftCreation(newDraftPage, Long.parseLong(userIdentity.getId()));
    //
    PageVersion pageVersion = getPublishedVersionByPageIdAndLang(Long.valueOf(newDraftPage.getTargetPageId()),
                                                                 newDraftPage.getLang());
    if (pageVersion != null) {
      Map<String, String> eventData = new HashMap<>();
      eventData.put(PAGE_VERSION_ID_PROP_NAME, pageVersion.getId());
      eventData.put("draftForExistingPageId", newDraftPage.getId());
      Utils.broadcast(listenerService, "note.draft.for.exist.page.created", this, eventData);
    }
    return newDraftPage;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage createDraftForNewPage(DraftPage draftPage, long clientTime, long userIdentityId) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(UNTITLED_PREFIX + draftSuffix);
    newDraftPage.setNewPage(true);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(draftPage.getTargetPageId());
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setParentPageId(draftPage.getParentPageId());
    newDraftPage.setAuthor(draftPage.getAuthor());
    newDraftPage.setLang(draftPage.getLang());
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getIdentity(userIdentityId);
    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, identity.getRemoteId());
    NotePageProperties properties = draftPage.getProperties();
    try {
      if (properties != null) {
        properties.setNoteId(Long.parseLong(newDraftPage.getId()));
        properties = saveNoteMetadata(properties, draftPage.getLang(), userIdentityId);
      }
    } catch (Exception e) {
      log.error("Failed to save draft note metadata", e);
    }
    newDraftPage.setProperties(properties);
    newDraftPage.setAttachmentObjectType(draftPage.getAttachmentObjectType());
    newDraftPage = processImagesOnDraftCreation(newDraftPage, userIdentityId);
    return newDraftPage;
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, String username) {
    if (StringUtils.equals(IdentityConstants.SYSTEM, page.getOwner())) {
      return false;
    } else {
      return switch (permissionType) {
      case VIEWPAGE -> canViewNote(page, username);
      case EDITPAGE, ADMINPAGE -> canEditNote(page, username);
      default -> wikiService.canManageWiki(page.getWikiType(), page.getWikiOwner(), username);
      };
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteByRootPermission(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importNotes(String zipLocation, Page parent, String conflict, Identity userIdentity) throws Exception {
    List<String> files = Utils.unzip(zipLocation, System.getProperty(TEMP_DIRECTORY_PATH));
    importNotes(files, parent, conflict, userIdentity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importNotes(List<String> files, Page parent, String conflict, Identity userIdentity) throws Exception {

    Map<String, String> featuredImages = new HashMap<>();
    String notesFilePath = "";
    for (String file : files) {
      if (file.contains("notesExport_")) {
        notesFilePath = file;
      }
      if (file.contains("/" + FEATURED_IMAGE_FOLDER + "/")) {
        String fileName = file.substring(file.lastIndexOf("/") + 1);
        String imageId = fileName.substring(0, fileName.lastIndexOf("."));
        featuredImages.put(imageId, file);
      }
    }
    if (!notesFilePath.isEmpty()) {
      ObjectMapper mapper = new ObjectMapper();
      File notesFile = new File(notesFilePath);
      ImportList notes = mapper.readValue(notesFile, new TypeReference<ImportList>() {
      });
      Wiki wiki = wikiService.getWikiByTypeAndOwner(parent.getWikiType(), parent.getWikiOwner());
      if (StringUtils.isNotEmpty(conflict) && (conflict.equals("replaceAll"))) {
        List<Page> notesTodelete = getAllNotes(parent);
        for (Page noteTodelete : notesTodelete) {
          if (!NoteConstants.NOTE_HOME_NAME.equals(noteTodelete.getName()) && !noteTodelete.getId().equals(parent.getId())) {
            try {
              deleteNote(wiki.getType(), wiki.getOwner(), noteTodelete.getName(), userIdentity);
            } catch (Exception e) {
              log.warn("Note {} connot be deleted for import", noteTodelete.getName(), e);
            }
          }
        }
      }
      for (Page note : notes.getNotes()) {
        importNote(note,
                   parent,
                   featuredImages,
                   wikiService.getWikiByTypeAndOwner(parent.getWikiType(), parent.getWikiOwner()),
                   conflict,
                   userIdentity);
      }
      for (Page note : notes.getNotes()) {
        replaceInsertedNotes(note, wiki, userIdentity);
        replaceIncludedPages(note, wiki, userIdentity);
      }
      cleanUp(notesFile);
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageList<SearchResult> search(WikiSearchData data) throws WikiException {
    try {
      PageList<SearchResult> result = dataStorage.search(data);

      if ((data.getTitle() != null) && (data.getWikiType() != null)
          && (data.getWikiOwner() != null)
          && (result.getPageSize() > 0)) {
        Page homePage = wikiService.getWikiByTypeAndOwner(data.getWikiType(), data.getWikiOwner()).getWikiHome();
        if (data.getTitle().equals("") || homePage != null && homePage.getTitle().contains(data.getTitle())) {
          Calendar wikiHomeCreateDate = Calendar.getInstance();
          wikiHomeCreateDate.setTime(homePage.getCreatedDate());

          Calendar wikiHomeUpdateDate = Calendar.getInstance();
          wikiHomeUpdateDate.setTime(homePage.getUpdatedDate());

          SearchResult wikiHomeResult = new SearchResult(data.getWikiType(),
                                                         data.getWikiOwner(),
                                                         homePage.getName(),
                                                         null,
                                                         homePage.getTitle(),
                                                         SearchResultType.PAGE,
                                                         wikiHomeUpdateDate,
                                                         wikiHomeCreateDate);
          List<SearchResult> tempSearchResult = result.getAll();
          tempSearchResult.add(wikiHomeResult);
          result = new ObjectPageList<>(tempSearchResult, result.getPageSize());
        }
      }
      return result;
    } catch (Exception e) {
      log.error("Cannot search on wiki " + data.getWikiType() + ":" + data.getWikiOwner() + " - Cause : " + e.getMessage(), e);
    }
    return new ObjectPageList<>(new ArrayList<SearchResult>(), 0);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteByIdAndLang(Long pageId, Identity userIdentity, String source, String lang) throws WikiException,
                                                                                                 IllegalAccessException {
    Page page = getNoteById(String.valueOf(pageId), userIdentity, source);
    PageVersion publishedVersion = dataStorage.getPublishedVersionByPageIdAndLang(pageId, lang);
    if (page != null && publishedVersion != null) {
      page.setTitle(publishedVersion.getTitle());
      page.setContent(publishedVersion.getContent());
      page.setLang(publishedVersion.getLang());
      page.setProperties(publishedVersion.getProperties());
      page.setLatestVersionId(publishedVersion.getId());
      page.setLastUpdater(publishedVersion.getAuthor());
      if (lang != null) {
        page.setMetadatas(retrieveMetadataItems(pageId + "-" + lang, userIdentity.getUserId()));
      }
    }
    if (page != null && publishedVersion == null && lang != null) {
      // no version with lang, set the latest version id without lang
      publishedVersion = dataStorage.getPublishedVersionByPageIdAndLang(pageId, null);
      page.setLatestVersionId(publishedVersion == null ? null : publishedVersion.getId());
    }
    return page;
  }

  @Override
  @SneakyThrows
  public Page getNoteByIdAndLang(Long pageId, String lang) {
    Page page = getNoteById(String.valueOf(pageId));
    PageVersion publishedVersion = dataStorage.getPublishedVersionByPageIdAndLang(pageId, lang);
    if (page != null && publishedVersion != null) {
      page.setTitle(publishedVersion.getTitle());
      page.setContent(publishedVersion.getContent());
      page.setLang(publishedVersion.getLang());
      page.setProperties(publishedVersion.getProperties());
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  public PageVersion getPublishedVersionByPageIdAndLang(Long pageId, String lang) {
    return dataStorage.getPublishedVersionByPageIdAndLang(pageId, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getPageAvailableTranslationLanguages(Long pageId,
                                                           boolean withDrafts) throws WikiException {
    Set<String> langs = new HashSet<>(dataStorage.getPageAvailableTranslationLanguages(pageId));
    if (withDrafts) {
      List<DraftPage> drafts = dataStorage.getDraftsOfPage(pageId);
      drafts = drafts.stream()
                     .filter(jsonNodeData -> StringUtils.isNotBlank(jsonNodeData.getLang()))
                     .toList();
      langs.addAll(drafts.stream().map(DraftPage::getLang).toList());
    }
    return langs.stream().toList();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getPageAvailableTranslationLanguages(Long pageId, String userId, boolean withDrafts) throws WikiException {
    return getPageAvailableTranslationLanguages(pageId, withDrafts);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<PageHistory> getVersionsHistoryOfNoteByLang(Page note, String userName, String lang) throws WikiException {
    List<PageHistory> pageHistories = dataStorage.getPageHistoryVersionsByPageIdAndLang(Long.valueOf(note.getId()), lang);
    if (lang == null && pageHistories.isEmpty()) {
      PageVersion pageVersion = dataStorage.addPageVersion(note, userName);
      pageVersion.setId(note.getId() + "-" + pageVersion.getName());
      copyNotePageProperties(note,
                             pageVersion,
                             note.getLang(),
                             null,
                             NOTE_METADATA_PAGE_OBJECT_TYPE,
                             NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE,
                             userName);
      pageHistories = dataStorage.getPageHistoryVersionsByPageIdAndLang(Long.valueOf(note.getId()), null);
    }
    return pageHistories;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getLatestDraftPageByTargetPageAndLang(Long targetPageId, String lang) {
    return dataStorage.getLatestDraftPageByTargetPageAndLang(targetPageId, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getLatestDraftPageByUserAndTargetPageAndLang(Long targetPageId, String username, String lang) {
    return getLatestDraftPageByTargetPageAndLang(targetPageId, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String lang, boolean broadcast) throws Exception {
    Page note = getNoteById(String.valueOf(noteId));
    if (note != null) {
      deleteNoteMetadataProperties(note, lang, NOTE_METADATA_PAGE_OBJECT_TYPE);
    }
    dataStorage.deleteVersionsByNoteIdAndLang(noteId, lang);
    List<DraftPage> drafts = dataStorage.getDraftsOfPage(noteId);
    for (DraftPage draftPage : drafts) {
      if (StringUtils.equals(draftPage.getLang(), lang)) {
        removeDraftById(draftPage.getId());
      }
    }
    processPageContent(String.valueOf(noteId), StringUtils.EMPTY, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String lang) throws Exception {
    deleteVersionsByNoteIdAndLang(noteId, lang, false);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String userName, String lang) throws Exception {
    deleteVersionsByNoteIdAndLang(noteId, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<DraftPage> getDraftsOfWiki(String wikiOwner, String wikiType, String wikiHome) {
    return dataStorage.getDraftsOfWiki(wikiOwner, wikiType, wikiHome);
  }

  public void removeOrphanDraftPagesByParentPage(long parentPageId) {
    dataStorage.deleteOrphanDraftPagesByParentPage(parentPageId);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Long saveNoteFeaturedImage(Page note, NoteFeaturedImage featuredImage) throws Exception {
    if (featuredImage == null) {
      return null;
    }
    long featuredImageId = featuredImage.getId() != null ? featuredImage.getId() : 0L;
    String uploadId = featuredImage.getUploadId();
    if (uploadId != null) {

      UploadResource uploadResource = uploadService.getUploadResource(uploadId);
      if (uploadResource != null) {
        String fileDiskLocation = uploadResource.getStoreLocation();
        try (InputStream inputStream = new FileInputStream(fileDiskLocation);) {
          FileItem fileItem = new FileItem(featuredImageId,
                                           note.getName(),
                                           featuredImage.getMimeType(),
                                           FILE_NAME_SPACE,
                                           inputStream.available(),
                                           new Date(),
                                           null,
                                           false,
                                           inputStream);
          if (featuredImageId == 0) {
            fileItem = fileService.writeFile(fileItem);
          } else {
            fileItem = fileService.updateFile(fileItem);
          }
          if (fileItem != null && fileItem.getFileInfo() != null) {
            return fileItem.getFileInfo().getId();
          }
        } catch (Exception e) {
          log.error("Error while saving note featured image", e);
        } finally {
          uploadService.removeUploadResource(uploadId);
        }
      }
    }
    return featuredImageId;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NoteFeaturedImage getNoteFeaturedImageInfo(Long noteId,
                                                    String lang,
                                                    boolean isDraft,
                                                    String thumbnailSize,
                                                    long userIdentityId) throws Exception {
    if (noteId == null) {
      throw new IllegalArgumentException("note id is mandatory");
    }
    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getIdentity(userIdentityId);
    Page note;
    if (isDraft) {
      note = getDraftNoteById(String.valueOf(noteId), identity.getRemoteId());
    } else {
      note = getNoteByIdAndLang(noteId, lang);
    }
    if (note == null) {
      throw new ObjectNotFoundException("Note with id: " + noteId + " and lang: " + lang + " not found");
    }

    MetadataItem metadataItem = getNoteMetadataItem(note,
                                                    lang,
                                                    isDraft ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE :
                                                            NOTE_METADATA_PAGE_OBJECT_TYPE);
    if (metadataItem != null && !MapUtils.isEmpty(metadataItem.getProperties())) {
      String featuredImageIdProp = metadataItem.getProperties().get(FEATURED_IMAGE_ID);
      String featuredImageAltText = metadataItem.getProperties().get(FEATURED_IMAGE_ALT_TEXT);
      long noteFeaturedImageId = featuredImageIdProp != null
                                 && !featuredImageIdProp.equals("null") ? Long.parseLong(featuredImageIdProp) : 0L;
      FileItem fileItem = fileService.getFile(noteFeaturedImageId);
      if (fileItem != null && fileItem.getFileInfo() != null) {
        FileInfo fileInfo = fileItem.getFileInfo();
        if (thumbnailSize != null) {
          int[] dimension = org.exoplatform.social.common.Utils.parseDimension(thumbnailSize);
          fileItem = imageThumbnailService.getOrCreateThumbnail(fileItem, dimension[0], dimension[1]);
        }
        return new NoteFeaturedImage(fileInfo.getId(),
                                     fileInfo.getName(),
                                     fileInfo.getMimetype(),
                                     fileInfo.getSize(),
                                     fileInfo.getUpdatedDate().getTime(),
                                     fileItem.getAsStream(),
                                     featuredImageAltText);
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeNoteFeaturedImage(Long noteId,
                                      Long featuredImageId,
                                      String lang,
                                      boolean isDraft,
                                      Long userIdentityId) throws Exception {
    boolean removeFeaturedImageFile = true;
    Page note;
    if (isDraft) {
      DraftPage draftPage = getDraftNoteById(String.valueOf(noteId),
                                             identityManager.getIdentity(userIdentityId).getRemoteId());
      if (draftPage != null && (draftPage.getTargetPageId() == null
                                || isOriginalFeaturedImage(draftPage,
                                                           getNoteByIdAndLang(Long.valueOf(draftPage.getTargetPageId()),
                                                                              lang)))) {
        removeFeaturedImageFile = false;
      }
      note = draftPage;
    } else {
      note = getNoteByIdAndLang(noteId, lang);
    }
    if (note == null) {
      throw new ObjectNotFoundException("note not found");
    }
    if (removeFeaturedImageFile && featuredImageId != null && featuredImageId > 0) {
      fileService.deleteFile(featuredImageId);
    }
    MetadataItem metadataItem = getNoteMetadataItem(note,
                                                    lang,
                                                    isDraft ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE :
                                                            NOTE_METADATA_PAGE_OBJECT_TYPE);
    if (metadataItem != null) {
      Map<String, String> properties = metadataItem.getProperties();
      properties.remove(FEATURED_IMAGE_ID);
      properties.remove(FEATURED_IMAGE_UPDATED_DATE);
      properties.remove(FEATURED_IMAGE_ALT_TEXT);
      metadataItem.setProperties(properties);
      metadataService.updateMetadataItem(metadataItem, userIdentityId, false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NotePageProperties saveNoteMetadata(NotePageProperties notePageProperties,
                                             String lang,
                                             Long userIdentityId) throws Exception {
    if (notePageProperties == null) {
      return null;
    }
    Page note;
    Long featuredImageId = null;
    NoteFeaturedImage featuredImage = notePageProperties.getFeaturedImage();
    if (notePageProperties.isDraft()) {
      note = getDraftNoteById(String.valueOf(notePageProperties.getNoteId()),
                              identityManager.getIdentity(userIdentityId).getRemoteId());
    } else {
      note = getNoteByIdAndLang(notePageProperties.getNoteId(), lang);
    }
    if (note == null) {
      throw new ObjectNotFoundException("note not found");
    }

    if (featuredImage != null && featuredImage.isToDelete()) {
      removeNoteFeaturedImage(Long.valueOf(note.getId()),
                              featuredImage.getId(),
                              lang,
                              notePageProperties.isDraft(),
                              userIdentityId);
    } else {
      featuredImageId = saveNoteFeaturedImage(note, featuredImage);
    }
    NoteMetadataObject noteMetadataObject =
                                          buildNoteMetadataObject(note,
                                                                  lang,
                                                                  notePageProperties.isDraft() ?
                                                                                               NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE :
                                                                                               NOTE_METADATA_PAGE_OBJECT_TYPE);
    MetadataItem metadataItem = getNoteMetadataItem(note,
                                                    lang,
                                                    notePageProperties.isDraft() ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE :
                                                                                 NOTE_METADATA_PAGE_OBJECT_TYPE);

    Map<String, String> properties = new HashMap<>();
    if (metadataItem != null && metadataItem.getProperties() != null) {
      properties = metadataItem.getProperties();
    }
    properties.put(SUMMARY_PROP, notePageProperties.getSummary() != null ? notePageProperties.getSummary() : "");
    properties.put(HIDE_AUTHOR_PROP, String.valueOf(notePageProperties.isHideAuthor()));
    properties.put(HIDE_REACTION_PROP, String.valueOf(notePageProperties.isHideReaction()));
    if (featuredImageId != null) {
      properties.put(FEATURED_IMAGE_ID, String.valueOf(featuredImageId));
      properties.put(FEATURED_IMAGE_UPDATED_DATE, String.valueOf(new Date().getTime()));
      properties.put(FEATURED_IMAGE_ALT_TEXT, notePageProperties.getFeaturedImage().getAltText());
    }
    if (metadataItem == null) {
      metadataService.createMetadataItem(noteMetadataObject, NOTES_METADATA_KEY, properties, userIdentityId, false);
    } else {
      metadataItem.setProperties(properties);
      metadataService.updateMetadataItem(metadataItem, userIdentityId, false);
    }
    if (featuredImage != null) {
      featuredImage.setId(featuredImageId);
      featuredImage.setLastUpdated(Long.valueOf(properties.getOrDefault(FEATURED_IMAGE_UPDATED_DATE, "0")));
      featuredImage.setUploadId(null);
      notePageProperties.setFeaturedImage(featuredImage);
    }
    return notePageProperties;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public PageVersion getPageVersionById(Long versionId) {
    if (versionId == null) {
      throw new IllegalArgumentException("version id is mandatory");
    }
    return dataStorage.getPageVersionById(versionId);
  }

  // ******* Listeners *******/

  public void postUpdatePageVersionLanguage(String versionPageId) {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      l.postUpdatePageVersion(versionPageId);
    }
  }

  public void postDeletePageVersionLanguage(PageVersion pageVersion) {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      l.postDeletePageVersion(pageVersion);
    }
  }

  public void postUpdatePage(final String wikiType,
                             final String wikiOwner,
                             final String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postUpdatePage(wikiType, wikiOwner, pageId, page, wikiUpdateType);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
    }
  }

  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postAddPage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
    }
  }

  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postDeletePage(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
    }
  }

  public void postOpenByTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postgetPagefromTree(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
    }
  }

  public void postOpenByBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.postgetPagefromBreadCrumb(wikiType, wikiOwner, pageId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
    }
  }

  @Override
  public void markNoteAsViewed(Page note, Identity userIdentity) {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      try {
        l.markNoteAsViewed(note, userIdentity.getUserId());
      } catch (Exception e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on [%s] failed", l, note.getName()), e);
        }
      }
    }
  }

  /******* Private methods *******/

  private void deleteNoteMetadataProperties(Page note, String lang, String objectType) throws Exception {
    MetadataItem draftNoteMetadataItem = getNoteMetadataItem(note, lang, objectType);
    if (draftNoteMetadataItem != null) {
      Map<String, String> properties = draftNoteMetadataItem.getProperties();
      if (properties != null && properties.getOrDefault(FEATURED_IMAGE_ID, null) != null) {
        String featuredImageId = properties.get(FEATURED_IMAGE_ID);
        if (note.isDraftPage() && ((DraftPage) note).getTargetPageId() != null) {
          removeNoteFeaturedImage(Long.parseLong(note.getId()),
                                  Long.parseLong(featuredImageId),
                                  lang,
                                  true,
                                  Long.parseLong(identityManager.getOrCreateUserIdentity(note.getOwner()).getId()));
        } else if (note.isDraftPage()) {
          // When delete a draft of non-existing page
          // check if its featured image was linked to a saved page
          List<MetadataItem> metadataItems =
                                           metadataService.getMetadataItemsByMetadataNameAndTypeAndObjectAndMetadataItemProperty(NOTES_METADATA_TYPE.getName(),
                                                                                                                                 NOTES_METADATA_TYPE.getName(),
                                                                                                                                 NOTE_METADATA_PAGE_OBJECT_TYPE,
                                                                                                                                 FEATURED_IMAGE_ID,
                                                                                                                                 featuredImageId,
                                                                                                                                 0,
                                                                                                                                 0);
          if (metadataItems == null || metadataItems.isEmpty()) {
            fileService.deleteFile(Long.parseLong(featuredImageId));
          }
        } else {
          fileService.deleteFile(Long.parseLong(featuredImageId));
        }
      }
      metadataService.deleteMetadataItem(draftNoteMetadataItem.getId(), false);
    }
  }

  private void importNote(Page note,
                          Page parent,
                          Map<String, String> featuredImages,
                          Wiki wiki,
                          String conflict,
                          Identity userIdentity) throws Exception {

    Page targetNote = null;
    File featuredImageFile = extractNoteFeaturedImageFileToImport(note, featuredImages);
    Page parent_ = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), parent.getName());
    if (parent_ == null) {
      parent_ = wiki.getWikiHome();
    }
    Page note_ = note;
    if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName())) {
      note.setId(null);
      Page note_2 = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
      if (note_2 == null) {
        note_ = createNote(wiki, parent_.getName(), note, userIdentity, false, true);
        targetNote = note_;
      } else {
        if (StringUtils.isNotEmpty(conflict)) {
          if (conflict.equals("overwrite") || conflict.equals("replaceAll")) {
            deleteNote(wiki.getType(), wiki.getOwner(), note.getName());
            note_ = createNote(wiki, parent_.getName(), note, userIdentity, false, true);
            targetNote = note_;
          }
          if (conflict.equals("duplicate")) {
            note_ = createNote(wiki, parent_.getName(), note, userIdentity, true, true);
            targetNote = note_;
          }
          if (conflict.equals("update")) {
            if (!note_2.getTitle().equals(note.getTitle()) || !note_2.getContent().equals(note.getContent())
                || !Objects.equals(note_2.getProperties(), note.getProperties())) {
              note_2.setTitle(note.getTitle());
              note_2 = updateNote(note_2, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
              createVersionOfNote(note_2, userIdentity.getUserId(), true);
              targetNote = note_2;
            }
          }
        }
      }
    } else {
      if (StringUtils.isNotEmpty(conflict)
          && (conflict.equals("update") || conflict.equals("overwrite") || conflict.equals("replaceAll"))) {
        Page note_1 = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
        if (!note.getContent().equals(note_1.getContent()) || !Objects.equals(note_1.getProperties(), note.getProperties())) {
          note_1.setContent(note.getContent());
          note_1 = updateNote(note_1, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
          createVersionOfNote(note_1, userIdentity.getUserId(), true);
          targetNote = note_1;
        }
      }
    }
    if (featuredImageFile != null) {
      saveImportedFeaturedImage(featuredImageFile,
                                targetNote,
                                Long.parseLong(identityManager.getOrCreateUserIdentity(userIdentity.getUserId()).getId()));
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        importNote(child, note_, featuredImages, wiki, conflict, userIdentity);
      }
    }
  }

  private List<Page> getAllNotes(Page note) throws WikiException {
    List<Page> listOfNotes = new ArrayList<>();
    addAllNodes(note, listOfNotes);
    return listOfNotes;
  }

  private void cleanUp(File file) throws IOException {
    if (Files.exists(file.toPath())) {
      Files.delete(file.toPath());
    }
  }

  private void computeDraftProps(DraftPage draftPage, String username) throws WikiException, IllegalAccessException {
    if (draftPage == null) {
      return;
    } else if (!canViewNote(draftPage, username)) {
      throw new IllegalAccessException("User does not have the right view the note.");
    }
    draftPage.setCanView(true);
    boolean canManageNotes = wikiService.canManageWiki(draftPage.getWikiType(), draftPage.getWikiOwner(), username);
    draftPage.setCanManage(canManageNotes || canEditNote(draftPage, username));
    draftPage.setCanImport(canManageNotes);
    String authorFullName = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, draftPage.getAuthor())
                                           .getProfile()
                                           .getFullName();
    draftPage.setAuthorFullName(authorFullName);
  }

  private void checkToRemoveDomainInUrl(Page note) {
    if (note == null) {
      return;
    }

    String url = note.getUrl();
    if (url != null && url.contains("://")) {
      try {
        URL oldURL = new URL(url);
        note.setUrl(oldURL.getPath());
      } catch (MalformedURLException ex) {
        if (log.isWarnEnabled()) {
          log.warn("Malformed url " + url, ex);
        }
      }
    }
  }

  private List<BreadcrumbData> getBreadCrumb(List<BreadcrumbData> list,
                                             String noteType,
                                             String noteOwner,
                                             String noteName,
                                             String lang,
                                             Identity userIdentity,
                                             boolean isDraftNote) throws WikiException, IllegalAccessException {
    if (list == null) {
      list = new ArrayList<>(5);
    }
    if (noteName == null) {
      return list;
    }
    Page note = isDraftNote ? dataStorage.getDraftPageById(noteName) : getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (note == null) {
      return list;
    }
    list.add(0,
             new BreadcrumbData(note.getName(),
                                note.getId(),
                                getNoteTitleWithTraduction(note, userIdentity, "", lang),
                                noteType,
                                noteOwner));
    Page parentNote = isDraftNote ? getNoteById(note.getParentPageId()) : getParentNoteOf(note);
    if (parentNote != null) {
      getBreadCrumb(list, noteType, noteOwner, parentNote.getName(), lang, userIdentity, false);
    }

    return list;
  }

  private String getNoteTitleWithTraduction(Page note, Identity userIdentity, String source, String lang) throws WikiException,
                                                                                                          IllegalAccessException {
    if (userIdentity == null || StringUtils.isEmpty(lang)) {
      return note.getTitle();
    }
    Page page = getNoteByIdAndLang(Long.valueOf(note.getId()), userIdentity, source, lang);
    if (page != null) {
      return page.getTitle();
    }
    return note.getTitle();
  }

  private String getDraftNameSuffix(long clientTime) {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));
  }

  private void replaceIncludedPages(Page note, Wiki wiki, Identity userIdentity) throws WikiException {
    Page note_ = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
    if (note_ != null) {
      String content = note_.getContent();
      if (content.contains("class=\"noteLink\" href=\"//-")) {
        while (content.contains("class=\"noteLink\" href=\"//-")) {
          String linkedParams = content.split("class=\"noteLink\" href=\"//-")[1].split("-//\"")[0];
          String NoteName = linkedParams.split("-////-")[2];
          Page linkedNote = null;
          linkedNote = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), NoteName);
          if (linkedNote != null) {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//",
                                      "\"noteLink\" href=\"" + linkedNote.getUrl());
          } else {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//", "\"noteLink\" href=\"" + NoteName);
          }
          if (content.equals(note_.getContent()))
            break;
        }
        if (!content.equals(note_.getContent())) {
          note_.setContent(content);
          updateNote(note_);
          createVersionOfNote(note_, userIdentity.getUserId(), true);
        }
      }
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        replaceIncludedPages(child, wiki, userIdentity);
      }
    }
  }

  private void replaceInsertedNotes(Page note, Wiki wiki, Identity userIdentity) {
    Page note_ = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
    if (note_ != null) {
      String content = note_.getContent();
      Pattern pattern = Pattern.compile("(<)" + CONTENT_LINK_TAG_REPLACEMENT + "([^>]*?>/notes:)([^<]+)(</)" + CONTENT_LINK_TAG_REPLACEMENT + "(>)");
      Matcher matcher = pattern.matcher(content);
      StringBuilder result = new StringBuilder();
      while (matcher.find()) {
        String noteName = matcher.group(3);
        Page noteToInsert = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), noteName);
        String noteId = "0";
        if (noteToInsert != null) {
          noteId = matcher.group(1) + CONTENT_LINK_TAG + matcher.group(2)
                  + noteToInsert.getId() + matcher.group(4) + CONTENT_LINK_TAG + matcher.group(5);
        }
        matcher.appendReplacement(result, Matcher.quoteReplacement(noteId));
      }
      matcher.appendTail(result);
      content = result.toString();
      if (!content.equals(note_.getContent())) {
        note_.setContent(content);
        updateNote(note_);
        createVersionOfNote(note_, userIdentity.getUserId(), true);
      }
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        replaceInsertedNotes(child, wiki, userIdentity);
      }
    }
  }

  private void addAllNodes(Page note, List<Page> listOfNotes) throws WikiException {
    if (note != null) {
      listOfNotes.add(note);
      List<Page> children = getChildrenNoteOf(note, true, false);
      if (children != null) {
        for (Page child : children) {
          addAllNodes(child, listOfNotes);
        }
      }
    }
  }

  private Map<String, List<MetadataItem>> retrieveMetadataItems(String noteId, String username) {
    org.exoplatform.social.core.identity.model.Identity currentIdentity =
                                                                        identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                            username);
    long currentUserId = Long.parseLong(currentIdentity.getId());
    MetadataObject metadataObject = new MetadataObject(Utils.NOTES_METADATA_OBJECT_TYPE, noteId);
    List<MetadataItem> metadataItems = metadataService.getMetadataItemsByObject(metadataObject);
    Map<String, List<MetadataItem>> metadata = new HashMap<>();
    metadataItems.stream()
                 .filter(metadataItem -> metadataItem.getMetadata().getAudienceId() == 0
                                         || metadataItem.getMetadata().getAudienceId() == currentUserId)
                 .forEach(metadataItem -> {
                   String type = metadataItem.getMetadata().getType().getName();
                   metadata.computeIfAbsent(type, k -> new ArrayList<>());
                   metadata.get(type).add(metadataItem);
                 });
    return metadata;
  }

  private NoteMetadataObject buildNoteMetadataObject(Page note, String lang, String objectType) {
    Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
    long spaceId = space != null ? Long.parseLong(space.getId()) : 0L;
    String noteId = String.valueOf(note.getId());
    noteId = lang != null ? noteId + "-" + lang : noteId;
    return new NoteMetadataObject(objectType, noteId, note.getParentPageId(), spaceId);
  }

  private MetadataItem getNoteMetadataItem(Page note, String lang, String objectType) {
    NoteMetadataObject noteMetadataObject = buildNoteMetadataObject(note, lang, objectType);
    return metadataService.getMetadataItemsByMetadataAndObject(NOTES_METADATA_KEY, noteMetadataObject)
                          .stream()
                          .findFirst()
                          .orElse(null);
  }

  private Page saveNote(Page note, PageUpdateType type, Identity userIdentity, boolean broadcast) {
    Page existingNote = getNoteById(note.getId());
    note.setContent(updateNoteContentImages(note, userIdentity));
    Page updatedPage = dataStorage.updatePage(note);
    NotePageProperties properties = note.getProperties();
    String updater = userIdentity == null ? null : userIdentity.getUserId();
    if (properties != null && updater != null) {
      try {
        properties.setNoteId(Long.parseLong(updatedPage.getId()));
        properties.setDraft(false);
        properties = saveNoteMetadata(properties,
                                      note.getLang(),
                                      Long.valueOf(identityManager.getOrCreateUserIdentity(updater).getId()));
        updatedPage.setProperties(properties);
      } catch (Exception e) {
        log.error("Error while updating note metadata properties", e);
      }
    }

    updatedPage.setUrl(Utils.getPageUrl(updatedPage));
    updatedPage.setToBePublished(note.isToBePublished());
    updatedPage.setCanManage(note.isCanManage());
    updatedPage.setCanImport(note.isCanImport());
    updatedPage.setCanView(note.isCanView());
    updatedPage.setAppName(note.getAppName());
    if (userIdentity != null) {
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(note.getId(), updater);
      updatedPage.setMetadatas(metadata);
      note.setAuthor(updater);
      updatedPage.setLastUpdater(updater);
    }
    Utils.broadcast(listenerService, "note.updated", note.getAuthor(), updatedPage);
    processPageContent(updatedPage, note.getLang());
    if (broadcast) {
      postUpdatePage(updatedPage.getWikiType(), updatedPage.getWikiOwner(), updatedPage.getName(), new Page(updatedPage), type);
      Matcher mentionsMatcher = Utils.MENTION_PATTERN.matcher(note.getContent());
      if (mentionsMatcher.find()) {
        Utils.sendMentionInNoteNotification(note,
                                            existingNote,
                                            updater == null ? existingNote.getAuthor() : updater);
      }
    }
    updatedPage.setLang(note.getLang());
    return updatedPage;
  }

  private void copyNotePageProperties(Page oldNote,
                                      Page note,
                                      String oldLang,
                                      String lang,
                                      String oldObjectType,
                                      String newObjectType,
                                      String username) {
    if (note == null || oldNote == null) {
      return;
    }
    if (username != null) {
      org.exoplatform.social.core.identity.model.Identity identity =
                                                                   identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                       username);
      NoteMetadataObject newNoteMetadataObject = buildNoteMetadataObject(note, lang, newObjectType);
      MetadataItem oldNoteMetadataItem = getNoteMetadataItem(oldNote, oldLang, oldObjectType);
      MetadataItem newNoteMetadataItem = getNoteMetadataItem(note, lang, newObjectType);
      if (oldNoteMetadataItem != null && oldNoteMetadataItem.getProperties() != null) {
        if (newNoteMetadataItem != null) {
          newNoteMetadataItem.setProperties(oldNoteMetadataItem.getProperties());
          metadataService.updateMetadataItem(newNoteMetadataItem, Long.parseLong(identity.getId()), false);
        } else {
          try {
            metadataService.createMetadataItem(newNoteMetadataObject,
                                               NOTES_METADATA_KEY,
                                               oldNoteMetadataItem.getProperties(),
                                               Long.parseLong(identity.getId()),
                                               false);
          } catch (Exception e) {
            log.error("Error while creating note metadata item", e);
          }
        }
      }
    }
  }

  private boolean isOriginalFeaturedImage(Page draftPage, Page targetPage) {
    if (draftPage == null || targetPage == null) {
      return false;
    }
    if (draftPage.getProperties() == null || targetPage.getProperties() == null) {
      return false;
    }
    NoteFeaturedImage draftFeaturedImage = draftPage.getProperties().getFeaturedImage();
    NoteFeaturedImage targetFeaturedImage = targetPage.getProperties().getFeaturedImage();
    return draftFeaturedImage != null && targetFeaturedImage != null
           && targetFeaturedImage.getId().equals(draftFeaturedImage.getId());
  }

  private File extractNoteFeaturedImageFileToImport(Page note, Map<String, String> featuredImages) {
    File featuredImageFile = null;
    NotePageProperties properties = note.getProperties();
    if (properties != null && properties.getFeaturedImage() != null) {
      NoteFeaturedImage featuredImage = properties.getFeaturedImage();
      String path = featuredImages.getOrDefault(String.valueOf(featuredImage.getId()), null);
      if (path != null) {
        featuredImageFile = new File(path);
      }
    }
    return featuredImageFile;
  }

  private void saveImportedFeaturedImage(File featuredImage, Page note, long userIdentityId) {
    try (InputStream inputStream = new FileInputStream(featuredImage)) {
      String mimeType = Files.probeContentType(featuredImage.toPath());
      FileItem fileItem = new FileItem(0L,
                                       note.getName(),
                                       mimeType,
                                       FILE_NAME_SPACE,
                                       featuredImage.length(),
                                       new Date(),
                                       null,
                                       false,
                                       inputStream);
      fileItem = fileService.writeFile(fileItem);
      NoteMetadataObject noteMetadataObject = buildNoteMetadataObject(note, note.getLang(), NOTE_METADATA_PAGE_OBJECT_TYPE);
      MetadataItem metadataItem = getNoteMetadataItem(note, note.getLang(), NOTE_METADATA_PAGE_OBJECT_TYPE);
      Map<String, String> properties = new HashMap<>();
      if (metadataItem != null && metadataItem.getProperties() != null) {
        properties = metadataItem.getProperties();
      }
      if (fileItem != null && fileItem.getFileInfo() != null) {
        FileInfo fileInfo = fileItem.getFileInfo();
        properties.put(FEATURED_IMAGE_ID, String.valueOf(fileInfo.getId()));
        properties.put(FEATURED_IMAGE_UPDATED_DATE, String.valueOf(new Date().getTime()));
        properties.put(FEATURED_IMAGE_ALT_TEXT, note.getProperties().getFeaturedImage().getAltText());
      }
      if (metadataItem == null) {
        metadataService.createMetadataItem(noteMetadataObject, NOTES_METADATA_KEY, properties, userIdentityId);
      } else {
        metadataItem.setProperties(properties);
        metadataService.updateMetadataItem(metadataItem, userIdentityId);
      }
    } catch (Exception e) {
      log.error("Error while saving imported featured image");
    }
  }

  private DraftPage processImagesOnDraftCreation(DraftPage draftPage,
                                                 long userIdentityId) throws WikiException {
    String newDraftContent = saveUploadedContentImages(draftPage.getContent(),
                                                       draftPage.getAttachmentObjectType(),
                                                       StringUtils.isNotEmpty(draftPage.getTargetPageId()) ?
                                                                                                           draftPage.getTargetPageId() :
                                                                                                           draftPage.getId(),
                                                       userIdentityId);
    newDraftContent = saveCopiedContentImages(newDraftContent,
                                              draftPage.getAttachmentObjectType(),
                                              StringUtils.isNotEmpty(draftPage.getTargetPageId()) ? draftPage.getTargetPageId()
                                                                                                  : draftPage.getId(),
                                              userIdentityId);
    if (!newDraftContent.equals(draftPage.getContent())) {
      draftPage.setContent(newDraftContent);
      return updateDraftPageContent(Long.parseLong(draftPage.getId()), draftPage.getContent());
    }
    return draftPage;
  }

  private String processImagesOnDraftUpdate(DraftPage draftPage, long userIdentityId) {
    try {
      String updatedContent = saveUploadedContentImages(draftPage.getContent(),
                                                        draftPage.getAttachmentObjectType(),
                                                        StringUtils.isNotEmpty(draftPage.getTargetPageId()) ? draftPage.getTargetPageId() :
                                                                                                              draftPage.getId(),
                                                        userIdentityId);
      updatedContent = saveCopiedContentImages(updatedContent,
                                               draftPage.getAttachmentObjectType(),
                                               StringUtils.isNotEmpty(draftPage.getTargetPageId()) ? draftPage.getTargetPageId()
                                                                                                   : draftPage.getId(),
                                               userIdentityId);
      return updatedContent;
    } catch (Exception exception) {
      return draftPage.getContent();
    }
  }

  private Page processImagesOnNoteCreation(Page note, String draftId, long userIdentityId) throws WikiException {
    String newContent = note.getContent();
    if (StringUtils.isNotEmpty(draftId) && !draftId.equals("null")) {
      String sourceObjectType = WikiDraftPageAttachmentPlugin.OBJECT_TYPE;
      attachmentService.moveAttachments(sourceObjectType,
                                        draftId,
                                        note.getAttachmentObjectType(),
                                        note.getId(),
                                        null,
                                        userIdentityId);
      newContent = note.getContent()
                       .replaceAll("/attachments/" + sourceObjectType + "/" + draftId,
                                   "/attachments/" + note.getAttachmentObjectType() + "/" + note.getId());
    }

    if (IMAGES_IMPORT_PATTERN.matcher(newContent).find()) {
      // process imported images
      newContent = processImportedNoteImages(note, userIdentityId);
    }
    if (!newContent.equals(note.getContent())) {
      return updateNoteContent(note, newContent);
    }
    return note;
  }

  private String saveUploadedContentImages(String content, String objectType, String objectId, long userId) {
    if (StringUtils.isEmpty(content)) {
      return content;
    }
    try {
      String regex = "<img[^>]*cke_upload_id=\"([^\"]+)\"[^>]*(src=\"[^/][^>^\"]+\")?[^>]*>";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(content);
      // Check if the pattern matches and extract the upload ID
      while (matcher.find()) {
        String imgElement = matcher.group(0);
        String uploadId = matcher.group(1);
        String imgSrc = matcher.group(2);
        UploadResource uploadResource = uploadService.getUploadResource(uploadId);

        if (uploadResource != null) {
          UploadedAttachmentDetail uploadedAttachmentDetail = new UploadedAttachmentDetail(uploadResource);
          attachmentService.saveAttachment(uploadedAttachmentDetail, objectType, objectId, null, userId);

          String fileId = uploadedAttachmentDetail.getId();
          String newSrc = String.format("src=\"/portal/rest/v1/social/attachments/%s/%s/%s\"", objectType, objectId, fileId);
          String archivedUploadId = String.format("archived_cke_uploadId=\"%s\"", uploadId);
          // Replace the entire img tag with the new src
          String newImgTag = imgElement.replaceAll("cke_upload_id=\"[^\"]*\"", String.format("%s %s", archivedUploadId, newSrc));
          if (StringUtils.isNotBlank(imgSrc)) {
            newImgTag = newImgTag.replace(imgSrc, "");
          }
          content = content.replace(imgElement, newImgTag);
        }
      }
    } catch (Exception e) {
      log.error("Error while saving uploaded content images");
      return content;
    }
    return content;
  }

  private String saveCopiedContentImages(String content, String objectType, String objectId, long userId) {
    if (StringUtils.isEmpty(content)) {
      return content;
    }
    try {
      String regex = "<img[^>]*src=[\"'][^\"']*/attachments/([^/]+)/([^/]+)/([^\"'/]+)[\"'][^>]*>";
      Pattern pattern = Pattern.compile(regex);
      Matcher matcher = pattern.matcher(content);
      while (matcher.find()) {
        String imgElement = matcher.group(0);
        String oldObjectType = matcher.group(1);
        String oldObjectId = matcher.group(2);
        String fileId = matcher.group(3);
        if (oldObjectId.equals(String.valueOf(objectId))) {
          continue;
        }
        if (attachmentService.getAttachment(oldObjectType, oldObjectId, fileId) != null) {
          attachmentService.createAttachment(fileId, objectType, objectId, null, userId, Collections.emptyMap());
          String newSrc = String.format("src=\"/portal/rest/v1/social/attachments/%s/%s/%s\"", objectType, objectId, fileId);
          String newImgTag = imgElement.replaceAll("src=[\"'][^\"']+[\"']", newSrc);
          content = content.replace(imgElement, newImgTag);
        }
      }
    } catch (Exception e) {
      log.error("Error while saving copied content images");
      return content;
    }
    return content;
  }

  private String updateNoteContentImages(Page note, Identity userIdentity) {
    String processedContent = note.getContent();
    if (userIdentity != null && note.getContent().contains("cke_upload_id=")) {
      processedContent = saveUploadedContentImages(note.getContent(),
                                                   note.getAttachmentObjectType(),
                                                   note.getId(),
                                                   Long.parseLong(identityManager.getOrCreateUserIdentity(userIdentity.getUserId())
                                                                                 .getId()));
    }

    if (userIdentity != null && IMAGES_IMPORT_PATTERN.matcher(processedContent).find()) {
      // process imported images
      processedContent = processImportedNoteImages(note,
                                                   Long.parseLong(identityManager.getOrCreateUserIdentity(userIdentity.getUserId())
                                                                                 .getId()));
    }
    if (userIdentity != null) {
      processedContent = saveCopiedContentImages(processedContent,
                                                 note.getAttachmentObjectType(),
                                                 note.getId(),
                                                 Long.parseLong(identityManager.getOrCreateUserIdentity(userIdentity.getUserId())
                                                                               .getId()));
    
    }
   return processedContent;
  }

  private void updateVersionContentImages(PageVersion pageVersion) throws WikiException {
    List<String> fileIds = Utils.getContentImagesIds(pageVersion.getContent(),
                                                     pageVersion.getAttachmentObjectType(),
                                                     pageVersion.getParentPageId());
    List<String> existingFiles = attachmentService.getAttachmentFileIds(pageVersion.getAttachmentObjectType(),
                                                                        pageVersion.getParentPageId());
    List<String> removedFiles = existingFiles.stream().filter(item -> !fileIds.contains(item)).toList();
    // remove image if not exist in any other version of note
    List<String> versionsOfNoteContentList = dataStorage.getVersionsOfPage(pageVersion.getParent())
                                                        .stream()
                                                        .map(PageVersion::getContent)
                                                        .toList();
    removedFiles.stream()
                .filter(fileId -> versionsOfNoteContentList.stream()
                                                           .noneMatch(content -> content.contains(pageVersion.getAttachmentObjectType()
                                                                                                             .concat("/".concat(pageVersion.getParentPageId()
                                                                                                                                           .concat("/".concat(fileId)))))))
                .forEach(fileId -> attachmentService.deleteAttachment(pageVersion.getAttachmentObjectType(),
                                                                      pageVersion.getParentPageId(),
                                                                      fileId));
  }

  private DraftPage updateDraftPageContent(long draftId, String content) throws WikiException {
    return dataStorage.updateDraftContent(draftId, content);
  }

  private Page updateNoteContent(Page note, String content) throws WikiException {
    return dataStorage.updatePageContent(note, content);
  }

  private String processImportedNoteImages(Page note, long userIdentityId) {
    String processedContent = note.getContent();
    Matcher matcher = IMAGES_IMPORT_PATTERN.matcher(processedContent);
    while (matcher.find()) {
      String fileName = processedContent.split("src=\"//-")[1].split("-//")[0];
      File file = new File(System.getProperty(TEMP_DIRECTORY_PATH) + File.separator + fileName);
      try (InputStream inputStream = new FileInputStream(file)) {
        String mimeType = Files.probeContentType(file.toPath());
        FileItem fileItem = new FileItem(0L,
                                         fileName,
                                         mimeType,
                                         "attachment",
                                         inputStream.available(),
                                         new Date(),
                                         null,
                                         false,
                                         inputStream);
        fileItem = fileService.writeFile(fileItem);
        if (fileItem.getFileInfo().getId() != null) {
          attachmentService.createAttachment(String.valueOf(fileItem.getFileInfo().getId()),
                                             note.getAttachmentObjectType(),
                                             note.getId(),
                                             null,
                                             userIdentityId,
                                             new HashMap<>());
          String urlToReplace = IMAGE_URL_REPLACEMENT_PREFIX + fileName + IMAGE_URL_REPLACEMENT_SUFFIX;
          String newImageUrl = new StringBuilder("/portal/rest/v1/social/attachments/").append(note.getAttachmentObjectType())
                                                                                       .append("/")
                                                                                       .append(note.getId())
                                                                                       .append("/")
                                                                                       .append(fileItem.getFileInfo().getId())
                                                                                       .toString();
          processedContent = processedContent.replace(urlToReplace, newImageUrl);

        }
      } catch (Exception exception) {
        log.error("Error while processing note images");
      }
    }
    return processedContent;
  }

  private void broadcastPageVersionCreationEvent(String pageVersionId, String draftPageId, String previousPageVersionId) {
    Map<String, String> eventData = new HashMap<>();
    if (draftPageId != null) {
      eventData.put(DRAFT_PAGE_ID_PROP_NAME, draftPageId);
    } else if (previousPageVersionId != null) {
      eventData.put("previousPageVersionId", previousPageVersionId);
    }
    if (!eventData.isEmpty()) {
      eventData.put(PAGE_VERSION_ID_PROP_NAME, pageVersionId);
      Utils.broadcast(listenerService, "note.page.version.created", this, eventData);
    }
  }

  private void processPageContent(Page page, String lang) {
    if (page != null) {
      String content = page.getContent();
      String id = page.getId();
      processPageContent(id, content, lang);
    }
  }

  private void processPageContent(String id, String content, String lang) {
    HtmlUtils.process(content,
                      new HtmlProcessorContext(NoteContentLinkPlugin.OBJECT_TYPE,
                                               id,
                                               null,
                                               getLocale(lang)));
  }

  private Space getSpaceByWiki(String wikiType, String wikiOwner) {
    if (StringUtils.isBlank(wikiType)
        || StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      return spaceService.getSpaceByGroupId(wikiOwner);
    } else {
      return null;
    }
  }

  private Locale getLocale(String lang) {
    return StringUtils.isBlank(lang) ? getDefaultLocale() : LocaleUtils.toLocale(lang);
  }

  private Locale getDefaultLocale() {
    return localeConfigService.getDefaultLocaleConfig().getLocale();
  }

}
