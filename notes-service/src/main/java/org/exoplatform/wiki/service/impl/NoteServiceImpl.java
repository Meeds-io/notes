 /**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NoteMetadataObject;
import io.meeds.notes.model.NotePageProperties;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.social.metadata.thumbnail.ImageThumbnailService;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.gatein.api.EntityNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.ImportList;
import org.exoplatform.wiki.model.NoteToExport;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.CMSService;
import lombok.SneakyThrows;

public class NoteServiceImpl implements NoteService {

  public static final String                              CACHE_NAME                             = "wiki.PageRenderingCache";

  public static final String                              ATT_CACHE_NAME                         = "wiki.PageAttachmentCache";

  private static final String                             UNTITLED_PREFIX                        = "Untitled_";

  private static final String                             TEMP_DIRECTORY_PATH                    = "java.io.tmpdir";

  private static final String                             FILE_NAME_SPACE                        = "wiki";

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

  public static final String                              FEATURED_IMAGE_ID                      = "featuredImageId";

  public static final String                              FEATURED_IMAGE_UPDATED_DATE            = "featuredImageUpdatedDate";

  public static final String                              FEATURED_IMAGE_ALT_TEXT                = "featuredImageAltText";

  private final WikiService                               wikiService;

  private final DataStorage                               dataStorage;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final ExoCache<Integer, AttachmentCountData>    attachmentCountCache;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap                           = new ConcurrentHashMap<>();

  private final IdentityManager                           identityManager;

  private final SpaceService                              spaceService;

  private final CMSService                                cmsService;

  private final ListenerService                           listenerService;
  
  private final FileService                               fileService;

  private final UploadService                             uploadService;

  private final HTMLUploadImageProcessor                  htmlUploadImageProcessor;

  public NoteServiceImpl(DataStorage dataStorage,
                         CacheService cacheService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         ListenerService listenerService, FileService fileService, UploadService uploadService) {
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.htmlUploadImageProcessor = null;
  }

  public NoteServiceImpl(DataStorage dataStorage,
                         CacheService cacheService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         ListenerService listenerService, FileService fileService, UploadService uploadService,
                         HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.fileService = fileService;
    this.uploadService = uploadService;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException,
                                                                                                 IllegalAccessException {

    String pageName = TitleResolver.getId(note.getName(), false);
    if (pageName == null) {
      pageName = TitleResolver.getId(note.getTitle(), false);
    }
    note.setName(pageName);

    if (isExisting(noteBook.getType(), noteBook.getOwner(), pageName)) {
      throw new WikiException("Page " + noteBook.getType() + ":" + noteBook.getOwner() + ":" + pageName
          + " already exists, cannot create it.");
    }

    Page parentPage = getNoteOfNoteBookByName(noteBook.getType(), noteBook.getOwner(), parentNoteName);
    if (parentPage != null) {
      note.setOwner(userIdentity.getUserId());
      note.setAuthor(userIdentity.getUserId());
      note.setContent(note.getContent());
      Page createdPage = createNote(noteBook, parentPage, note);
      NotePageProperties properties = note.getProperties();
      try {
        if (properties != null) {
          DraftPage draftPage = getDraftNoteById(String.valueOf(properties.getNoteId()), userIdentity.getUserId());
          if (draftPage != null) {
            NoteFeaturedImage featuredImage = properties.getFeaturedImage();
            if (featuredImage != null && featuredImage.getId() != null && featuredImage.getId() > 0) {
              moveOrCopyNotePageProperties(draftPage,
                                           createdPage,
                                           draftPage.getLang(),
                                           note.getLang(),
                                           NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE,
                                           NOTE_METADATA_PAGE_OBJECT_TYPE,
                                           userIdentity.getUserId(),
                                           true);
            } else {
              properties.setNoteId(Long.parseLong(createdPage.getId()));
              properties.setDraft(false);
              properties = saveNoteMetadata(properties,
                                            note.getLang(),
                                            Long.valueOf(identityManager.getOrCreateUserIdentity(userIdentity.getUserId())
                                                                        .getId()));
            }
            removeDraftById(draftPage.getId());
            invalidateCache(draftPage);
          }
        }
      } catch (Exception e) {
        log.error("Failed to save note metadata", e);
      }
      if (createdPage != null) {
        createdPage.setProperties(properties);
        Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
        createdPage.setCanManage(Utils.canManageNotes(note.getAuthor(), space, note));
        createdPage.setCanImport(canImportNotes(note.getAuthor(), space, note));
        createdPage.setCanView(canViewNotes(note.getAuthor(), space, note));
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
  public Page createNote(Wiki noteBook, Page parentPage, Page note) throws WikiException {
    Page createdPage = dataStorage.createPage(noteBook, parentPage, note);
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setAppName(note.getAppName());
    createdPage.setUrl(Utils.getPageUrl(createdPage));
    createdPage.setLang(note.getLang());
    if (parentPage != null) {
      invalidateCache(parentPage);
    }
    invalidateCache(note);

    Utils.broadcast(listenerService, "note.posted", note.getAuthor(), createdPage);
    postAddPage(noteBook.getType(), noteBook.getOwner(), note.getName(), createdPage);
    return createdPage;
  }

  /**
   * {@inheritDoc}
   */
  @SneakyThrows
  @Override
  public Page updateNote(Page note) throws WikiException {
    return updateNote(note, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws WikiException,
                                                                                IllegalAccessException,
                                                                                EntityNotFoundException {
    Page existingNote = getNoteById(note.getId());
    if (existingNote == null) {
      throw new EntityNotFoundException("Note to update not found");
    }
    Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
    if (userIdentity != null && !Utils.canManageNotes(userIdentity.getUserId(), space, existingNote)) {
      throw new IllegalAccessException("User does not have edit the note.");
    }
    if (PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)) {
      note.setUpdatedDate(Calendar.getInstance().getTime());
    }
    note.setContent(note.getContent());
    Page updatedPage = dataStorage.updatePage(note);
    DraftPage draftPage = getLatestDraftPageByTargetPageAndLang(Long.valueOf(note.getId()), note.getLang());
    if (userIdentity != null && draftPage != null) {
      moveOrCopyNotePageProperties(draftPage,
                                   note,
                                   draftPage.getLang(),
                                   note.getLang(),
                                   NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE,
                                   NOTE_METADATA_PAGE_OBJECT_TYPE,
                                   userIdentity.getUserId(),
                                   true);
      NotePageProperties notePageProperties = draftPage.getProperties();
      if (notePageProperties != null) {
        notePageProperties.setNoteId(Long.parseLong(updatedPage.getId()));
        notePageProperties.setDraft(false);
        updatedPage.setProperties(notePageProperties);
      }
    }

    invalidateCache(note);

    updatedPage.setUrl(Utils.getPageUrl(updatedPage));
    updatedPage.setToBePublished(note.isToBePublished());
    updatedPage.setCanManage(note.isCanManage());
    updatedPage.setCanImport(note.isCanImport());
    updatedPage.setCanView(note.isCanView());
    updatedPage.setAppName(note.getAppName());
    if (userIdentity != null) {
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(note.getId(), userIdentity.getUserId());
      updatedPage.setMetadatas(metadata);
      note.setAuthor(userIdentity.getUserId());
    }
    Utils.broadcast(listenerService, "note.updated", note.getAuthor(), updatedPage);
    postUpdatePage(updatedPage.getWikiType(), updatedPage.getWikiOwner(), updatedPage.getName(), updatedPage, type);
    updatedPage.setLang(note.getLang());
    return updatedPage;
  }

  /**
   * {@inheritDoc}
   */
  @SneakyThrows
  @Override
  public Page updateNote(Page note, PageUpdateType type) throws WikiException {
    return updateNote(note, type, null);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteNote(String noteType, String noteOwner, String noteName) throws WikiException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    try {
      dataStorage.deletePage(noteType, noteOwner, noteName);

    } catch (WikiException e) {
      log.error("Can't delete note '" + noteName + "' ", e);
      return false;
    }
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean deleteNote(String noteType, String noteOwner, String noteName, Identity userIdentity) throws WikiException,
                                                                                                       IllegalAccessException,
                                                                                                       EntityNotFoundException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    try {
      Page note = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
      if (note == null) {
        log.error("Can't delete note '" + noteName + "'. This note does not exist.");
        throw new EntityNotFoundException("Note to delete not found");
      }
      Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
      if (note != null) {
        if (!Utils.canManageNotes(userIdentity.getUserId(), space, note)) {
          log.error("Can't delete note '" + noteName + "'. does not have edit permission on it.");
          throw new IllegalAccessException("User does not have edit permissions on the note.");
        }

        invalidateCachesOfPageTree(note);
        invalidateAttachmentCache(note);

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

        deleteNote(noteType, noteOwner, noteName);

        postDeletePage(noteType, noteOwner, noteName, note);

        // Post delete activity for all children pages
        for (Page childNote : allChrildrenPages) {
          postDeletePage(childNote.getWikiType(), childNote.getWikiOwner(), childNote.getName(), childNote);
        }

      } else {
        log.error("Can't delete note '" + noteName + "'. This note does not exist.");
        return false;
      }
    } catch (WikiException e) {
      log.error("Can't delete note '" + noteName + "' ", e);
      return false;
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
    invalidateCache(page);

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
      }
      if (moveNote != null) {
        Space space = spaceService.getSpaceByGroupId(moveNote.getWikiOwner());
        if (!Utils.canManageNotes(userIdentity.getUserId(), space, moveNote)) {
          throw new IllegalAccessException("User does not have edit the note.");
        }
      }

      moveNote(currentLocationParams, newLocationParams);

      Page note = new Page(currentLocationParams.getPageName());
      note.setWikiType(currentLocationParams.getType());
      note.setWikiOwner(currentLocationParams.getOwner());
      invalidateCache(note);
      invalidateAttachmentCache(note);

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
    Page page = null;
    page = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (page == null) {
      throw new EntityNotFoundException("page not found");
    }
    if (page != null) {
      Space space = spaceService.getSpaceByGroupId(page.getWikiOwner());
      if (!canViewNotes(userIdentity.getUserId(), space, page)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
      page.setCanView(true);
      page.setCanManage(Utils.canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanImport(canImportNotes(userIdentity.getUserId(), space, page));
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(page.getId(), userIdentity.getUserId());
      page.setMetadatas(metadata);
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getPageById(id);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public DraftPage getDraftNoteById(String id, String userId) throws WikiException, IllegalAccessException {
    if (id == null) {
      return null;
    }
    DraftPage draftPage = dataStorage.getDraftPageById(id);
    computeDraftProps(draftPage, userId);

    return draftPage;
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
    if (id == null) {
      return null;
    }
    Page page = null;
    page = getNoteById(id);
    if (page != null) {
      Space space = spaceService.getSpaceByGroupId(page.getWikiOwner());
      if (!canViewNotes(userIdentity.getUserId(), space, page)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
      page.setCanView(true);
      page.setCanManage(Utils.canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanImport(canImportNotes(userIdentity.getUserId(), space, page));
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Page getNoteById(String id, Identity userIdentity, String source) throws IllegalAccessException, WikiException {
    if (id == null) {
      return null;
    }
    Page page;
    page = getNoteById(id);
    if (page != null) {
      Space space = spaceService.getSpaceByGroupId(page.getWikiOwner());
      if (!canViewNotes(userIdentity.getUserId(), space, page)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
      page.setCanView(true);
      page.setUrl(Utils.getPageUrl(page));
      page.setCanManage(Utils.canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanImport(canImportNotes(userIdentity.getUserId(), space, page));
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(id, userIdentity.getUserId());
      page.setMetadatas(metadata);
      if (StringUtils.isNotBlank(source)) {
        if (source.equals("tree")) {
          postOpenByTree(page.getWikiType(), page.getWikiOwner(), page.getName(), page);
        }
        if (source.equals("breadCrumb")) {
          postOpenByBreadCrumb(page.getWikiType(), page.getWikiOwner(), page.getName(), page);
        }
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
    List<Page> pages = dataStorage.getChildrenPageOf(note, withDrafts);
    if (withChild) {
      for (Page page : pages) {
        long pageId = Long.parseLong(page.getId());
        page.setHasChild(dataStorage.hasChildren(pageId));
      }
    }
    return pages;
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
    dataStorage.deleteDraftOfPage(page);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeDraftOfNote(WikiPageParams param, String lang) throws WikiException {
    Page page = getNoteOfNoteBookByName(param.getType(), param.getOwner(), param.getPageName());
    dataStorage.deleteDraftOfPage(page, lang);
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
  public void removeDraftById(String draftId) throws WikiException {
    dataStorage.deleteDraftById(draftId);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public List<PageHistory> getVersionsHistoryOfNote(Page note, String userName) throws WikiException {
    List<PageHistory> versionsHistory = dataStorage.getHistoryOfPage(note);
    if (versionsHistory == null || versionsHistory.isEmpty()) {
      dataStorage.addPageVersion(note, userName);
      versionsHistory = dataStorage.getHistoryOfPage(note);
    }
    for (PageHistory version : versionsHistory) {
      if (version.getAuthor() != null) {
        org.exoplatform.social.core.identity.model.Identity authorIdentity =
                                                                           identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                               version.getAuthor());
        version.setAuthorFullName(authorIdentity.getProfile().getFullName());
      }
    }
    return versionsHistory;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void createVersionOfNote(Page note, String userName) throws WikiException {
    PageVersion pageVersion = dataStorage.addPageVersion(note, userName);
    if (note.getLang() != null) {
      try {
        saveNoteMetadata(note.getProperties(),
                         note.getLang(),
                         Long.valueOf(identityManager.getOrCreateUserIdentity(userName).getId()));
      } catch (Exception e) {
        log.error("Error while saving note version language metadata", e);
      }
      String versionLangId = note.getId() + "-" + note.getLang();
      postUpdatePageVersionLanguage(versionLangId);
    } else {
      pageVersion.setId(note.getId() + "-" + pageVersion.getName());
      moveOrCopyNotePageProperties(note,
                                   pageVersion,
                                   note.getLang(),
                                   null,
                                   NOTE_METADATA_PAGE_OBJECT_TYPE,
                                   NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE,
                                   userName,
                                   false);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void restoreVersionOfNote(String versionName, Page note, String userName) throws WikiException {
    PageVersion pageVersion = dataStorage.restoreVersionOfPage(versionName, note);
    pageVersion.setId(note.getId() + "-" + pageVersion.getName());
    moveOrCopyNotePageProperties(pageVersion,
                                 note,
                                 null,
                                 null,
                                 NOTE_METADATA_VERSION_PAGE_OBJECT_TYPE,
                                 NOTE_METADATA_PAGE_OBJECT_TYPE,
                                 userName,
                                 false);
    createVersionOfNote(note, userName);
    invalidateCache(note);
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

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setId(draftNoteToUpdate.getId());
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftNoteToUpdate.getTitle());
    newDraftPage.setTargetPageId(draftNoteToUpdate.getTargetPageId());
    newDraftPage.setParentPageId(draftNoteToUpdate.getParentPageId());
    newDraftPage.setContent(draftNoteToUpdate.getContent());
    newDraftPage.setLang(draftNoteToUpdate.getLang());
    newDraftPage.setSyntax(draftNoteToUpdate.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    if (StringUtils.isEmpty(revision)) {
      List<PageHistory> versions = getVersionsHistoryOfNote(targetPage, username);
      if (versions != null && !versions.isEmpty()) {
        newDraftPage.setTargetPageRevision(String.valueOf(versions.get(0).getVersionNumber()));
      } else {
        newDraftPage.setTargetPageRevision("1");
      }
    } else {
      newDraftPage.setTargetPageRevision(revision);
    }

    newDraftPage = dataStorage.updateDraftPageForUser(newDraftPage, Utils.getCurrentUser());
    NotePageProperties properties = draftNoteToUpdate.getProperties();
    try {
      properties = saveNoteMetadata(properties,
                                    newDraftPage.getLang(),
                                    Long.valueOf(identityManager.getOrCreateUserIdentity(username).getId()));
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
    newDraftPage.setContent(draftNoteToUpdate.getContent());
    newDraftPage.setLang(draftNoteToUpdate.getLang());
    newDraftPage.setSyntax(draftNoteToUpdate.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    newDraftPage = dataStorage.updateDraftPageForUser(newDraftPage, Utils.getCurrentUser());
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

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(targetPage.getId());
    newDraftPage.setParentPageId(draftPage.getParentPageId());
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setLang(draftPage.getLang());
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    if (StringUtils.isEmpty(revision)) {
      List<PageHistory> versions = getVersionsHistoryOfNote(targetPage, username);
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
        if (featuredImage != null && featuredImage.getId() != null && featuredImage.getId() > 0) {
          Map<String, String> props = moveOrCopyNotePageProperties(targetPage,
                                                                   newDraftPage,
                                                                   targetPage.getLang(),
                                                                   newDraftPage.getLang(),
                                                                   NOTE_METADATA_PAGE_OBJECT_TYPE,
                                                                   NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE,
                                                                   username,
                                                                   false);
          if (props != null && props.getOrDefault(FEATURED_IMAGE_ID, null) == null) {
            properties.setFeaturedImage(null);
          }
        } else {
          properties = saveNoteMetadata(properties,
                                        draftPage.getLang(),
                                        Long.valueOf(identityManager.getOrCreateUserIdentity(username).getId()));
        }
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
  public DraftPage createDraftForNewPage(DraftPage draftPage, long clientTime, long userIdentityId) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(UNTITLED_PREFIX + draftSuffix);
    newDraftPage.setNewPage(true);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(draftPage.getTargetPageId());
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setParentPageId(draftPage.getParentPageId());
    newDraftPage.setContent(draftPage.getContent());
    newDraftPage.setAuthor(draftPage.getAuthor());
    newDraftPage.setLang(draftPage.getLang());
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));
    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());
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
    return newDraftPage;
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException {
    if (StringUtils.equals(IdentityConstants.SYSTEM, page.getOwner())) {
      return false;
    }
    else if (page.isDraftPage()) {
      return true;
    }
    return dataStorage.hasPermissionOnPage(page, permissionType, user);
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
  public String getNoteRenderedContent(Page note) {
    String renderedContent = StringUtils.EMPTY;
    try {
      MarkupKey key = new MarkupKey(new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName()), false);
      MarkupData cachedData = renderingCache.get(key.hashCode());
      if (cachedData != null) {
        return cachedData.build();
      }
      renderedContent = note.getContent();
      renderingCache.put(key.hashCode(), new MarkupData(renderedContent));
    } catch (Exception e) {
      log.error(String.format("Failed to get rendered content of note [%s:%s:%s]", note.getWikiType(), note.getWikiOwner(), note.getName()), e);
    }
    return renderedContent;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importNotes(String zipLocation, Page parent, String conflict, Identity userIdentity) throws WikiException,
          IllegalAccessException,
          IOException {
    List<String> files = Utils.unzip(zipLocation, System.getProperty(TEMP_DIRECTORY_PATH));
    importNotes(files, parent, conflict, userIdentity);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void importNotes(List<String> files, Page parent, String conflict, Identity userIdentity) throws WikiException,
          IllegalAccessException,
          IOException {

    String notesFilePath = "";
    for (String file : files) {
      if (file.contains("notesExport_")) {
        {
          notesFilePath = file;
          break;
        }
      }
    }
    if (!notesFilePath.equals("")) {
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
                wikiService.getWikiByTypeAndOwner(parent.getWikiType(), parent.getWikiOwner()),
                conflict,
                userIdentity);
      }
      for (Page note : notes.getNotes()) {
        replaceIncludedPages(note, wiki);
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

      if ((data.getTitle() != null) && (data.getWikiType() != null) && (data.getWikiOwner() != null)
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
      if (lang != null) {
        page.setMetadatas(retrieveMetadataItems(pageId + "-" + lang, userIdentity.getUserId()));
      }
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
      dataStorage.addPageVersion(note, userName);
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
  public DraftPage getLatestDraftPageByUserAndTargetPageAndLang(Long targetPageId, String username, String lang)  {
    return getLatestDraftPageByTargetPageAndLang(targetPageId, lang);
  }
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String lang) throws WikiException {
    dataStorage.deleteVersionsByNoteIdAndLang(noteId, lang);
    List<DraftPage> drafts = dataStorage.getDraftsOfPage(noteId);
    for (DraftPage draftPage : drafts) {
      if (StringUtils.equals(draftPage.getLang(),lang)) {
        removeDraft(draftPage.getName());
      }
    }
    postDeletePageVersionLanguage(noteId + "-" + lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String userName, String lang) throws WikiException {
    deleteVersionsByNoteIdAndLang(noteId, lang);
  }
  
  public ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }

  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
  }
  
  // ******* Listeners *******/

  public void postUpdatePageVersionLanguage(String versionPageId) {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      l.postUpdatePageVersion(versionPageId);
    }
  }

  public void postDeletePageVersionLanguage(String versionPageId) {
    List<PageWikiListener> listeners = wikiService.getPageListeners();
    for (PageWikiListener l : listeners) {
      l.postDeletePageVersion(versionPageId);
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
  
  protected void invalidateCache(Page page) {
    WikiPageParams params = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
    List<WikiPageParams> linkedPages = pageLinksMap.get(params);
    if (linkedPages == null) {
      linkedPages = new ArrayList<>();
    } else {
      linkedPages = new ArrayList<>(linkedPages);
    }
    linkedPages.add(params);

    for (WikiPageParams wikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(wikiPageParams, false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(wikiPageParams, false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(wikiPageParams, false);
        renderingCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        renderingCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        log.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]",
                               wikiPageParams.getType(),
                               wikiPageParams.getOwner(),
                               wikiPageParams.getPageName()));
      }
    }
  }

  protected void invalidateCachesOfPageTree(Page note) throws WikiException {
    Queue<Page> queue = new LinkedList<>();
    queue.add(note);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      invalidateCache(currentPage);
      List<Page> childrenPages = getChildrenNoteOf(currentPage, false, false);
      for (Page child : childrenPages) {
        queue.add(child);
      }
    }
  }

  protected void invalidateAttachmentCache(Page note) {
    WikiPageParams wikiPageParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());

    List<WikiPageParams> linkedPages = pageLinksMap.get(wikiPageParams);
    if (linkedPages == null) {
      linkedPages = new ArrayList<>();
    } else {
      linkedPages = new ArrayList<>(linkedPages);
    }
    linkedPages.add(wikiPageParams);

    for (WikiPageParams linkedWikiPageParams : linkedPages) {
      try {
        MarkupKey key = new MarkupKey(linkedWikiPageParams, false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));

        key = new MarkupKey(linkedWikiPageParams, false);
        attachmentCountCache.remove(new Integer(key.hashCode()));
        key.setSupportSectionEdit(true);
        attachmentCountCache.remove(new Integer(key.hashCode()));
      } catch (Exception e) {
        log.warn(String.format("Failed to invalidate cache of note [%s:%s:%s]",
                               linkedWikiPageParams.getType(),
                               linkedWikiPageParams.getOwner(),
                               linkedWikiPageParams.getPageName()));
      }
    }
  }
  
  /******* Private methods *******/
  
  private void importNote(Page note, Page parent, Wiki wiki, String conflict, Identity userIdentity) throws WikiException,
                                                                                                    IllegalAccessException {

    Page parent_ = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), parent.getName());
    if (parent_ == null) {
      parent_ = wiki.getWikiHome();
    }
    String imagesSubLocationPath = "Documents/notes/images";
    Page note_ = note;
    if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName())) {
      note.setId(null);
      Page note_2 = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
      if (note_2 == null) {
        String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(),
                                                                              wiki.getOwner(),
                                                                              imagesSubLocationPath);
        note.setContent(processedContent);
        note_ = createNote(wiki, parent_.getName(), note, userIdentity);
      } else {
        if (StringUtils.isNotEmpty(conflict)) {
          if (conflict.equals("overwrite") || conflict.equals("replaceAll")) {
            deleteNote(wiki.getType(), wiki.getOwner(), note.getName());
            String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(),
                                                                                  wiki.getOwner(),
                                                                                  imagesSubLocationPath);
            note.setContent(processedContent);
            note_ = createNote(wiki, parent_.getName(), note, userIdentity);

          }
          if (conflict.equals("duplicate")) {
            String title = note.getTitle();
            int i;
            try {
              i = title.lastIndexOf("_") != -1 ? Integer.valueOf(title.substring(title.lastIndexOf("_") + 1)) + 1 : 1;
            } catch (NumberFormatException e) {
              i = 1;
            }
            String newTitle = note.getTitle() + "_" + i;
            while (getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), newTitle) != null
                || isExisting(wiki.getType(), wiki.getOwner(), TitleResolver.getId(newTitle, false))) {
              i++;
              newTitle = note.getTitle() + "_" + i;
            }
            note.setName(newTitle);
            note.setTitle(newTitle);
            String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(),
                                                                                  wiki.getOwner(),
                                                                                  imagesSubLocationPath);
            note.setContent(processedContent);
            note_ = createNote(wiki, parent_.getName(), note, userIdentity);
          }
          if (conflict.equals("update")) {
            if (!note_2.getTitle().equals(note.getTitle()) || !note_2.getContent().equals(note.getContent())) {
              note_2.setTitle(note.getTitle());
              String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(),
                                                                                    wiki.getOwner(),
                                                                                    imagesSubLocationPath);
              note_2.setContent(processedContent);
              note_2 = updateNote(note_2, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
              createVersionOfNote(note_2, userIdentity.getUserId());
            }
          }
        }
      }
    } else {
      if (StringUtils.isNotEmpty(conflict)
          && (conflict.equals("update") || conflict.equals("overwrite") || conflict.equals("replaceAll"))) {
        Page note_1 = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
        if (!note.getContent().equals(note_1.getContent())) {
          String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(),
                                                                                wiki.getOwner(),
                                                                                imagesSubLocationPath);
          note.setContent(processedContent);
          note_1.setContent(processedContent);
          note_1 = updateNote(note_1, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
          createVersionOfNote(note_1, userIdentity.getUserId());
        }
      }
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        importNote(child, note_, wiki, conflict, userIdentity);
      }
    }
  }
  
  private List<Page> getAllNotes(Page note) throws WikiException {
    List<Page> listOfNotes = new ArrayList<Page>();
    addAllNodes(note, listOfNotes);
    return listOfNotes;
  }
  
  private void cleanUp(File file) throws IOException {
    if(Files.exists(file.toPath())){
      Files.delete(file.toPath());
    }
  }
  
  private void computeDraftProps(DraftPage draftPage, String userId) throws WikiException, IllegalAccessException {
    if (draftPage != null) {
      Space space = spaceService.getSpaceByGroupId(draftPage.getWikiOwner());
      if (!canViewNotes(userId, space, draftPage)) {
        throw new IllegalAccessException("User does not have the right view the note.");
      }
      draftPage.setCanView(true);
      draftPage.setCanManage(Utils.canManageNotes(userId, space, draftPage));
      draftPage.setCanImport(canImportNotes(userId, space, draftPage));
      String authorFullName = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, draftPage.getAuthor())
              .getProfile()
              .getFullName();
      draftPage.setAuthorFullName(authorFullName);
    }
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

  private boolean canImportNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null) {
      return (spaceService.isSuperManager(authenticatedUser) || spaceService.isManager(space, authenticatedUser)
              || spaceService.isRedactor(space, authenticatedUser));
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM)) {
      return cmsService.hasAccessPermission(Utils.getIdentity(authenticatedUser), NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return StringUtils.equals(page.getOwner(), authenticatedUser);
    }
  }

  private boolean canViewNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null) {
      return !page.isDraftPage() ? spaceService.isMember(space, authenticatedUser) : Utils.canManageNotes(authenticatedUser, space, page);
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM) || StringUtils.isBlank(page.getOwner())) {
      return cmsService.hasAccessPermission(Utils.getIdentity(authenticatedUser), NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return spaceService.isSuperManager(authenticatedUser) || StringUtils.equals(page.getOwner(), authenticatedUser);
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
  
  private LinkedList<String> getNoteAncestorsIds(String noteId) throws WikiException {
    return getNoteAncestorsIds(null, noteId);
  }

  private LinkedList<String> getNoteAncestorsIds(LinkedList<String> ancestorsIds, String noteId) throws WikiException {
    if (ancestorsIds == null) {
      ancestorsIds = new LinkedList<>();
    }
    if (noteId == null) {
      return ancestorsIds;
    }
    Page note = getNoteById(noteId);
    String parentId = note.getParentPageId();
    
    if (parentId != null) {
      ancestorsIds.push(parentId);
      getNoteAncestorsIds(ancestorsIds, parentId);
    }
    
    return ancestorsIds;
  }

  private String getDraftNameSuffix(long clientTime) {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));
  }

  private void replaceIncludedPages(Page note, Wiki wiki) throws WikiException {
    Page note_ = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
    if (note_ != null) {
      String content = note_.getContent();
      if (content.contains("class=\"noteLink\" href=\"//-")) {
        while (content.contains("class=\"noteLink\" href=\"//-")) {
          String linkedParams = content.split("class=\"noteLink\" href=\"//-")[1].split("-//\"")[0];
          String noteBookType = linkedParams.split("-////-")[0];
          String noteBookOwner = linkedParams.split("-////-")[1];
          String NoteName = linkedParams.split("-////-")[2];
          Page linkedNote = null;
          linkedNote = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), NoteName);
          if (linkedNote != null) {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//",
                                      "\"noteLink\" href=\"" + linkedNote.getId());
          } else {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//", "\"noteLink\" href=\"" + NoteName);
          }
          if (content.equals(note_.getContent()))
            break;
        }
        if (!content.equals(note_.getContent())) {
          note_.setContent(content);
          updateNote(note_);
        }
      }
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        replaceIncludedPages(child, wiki);
      }
    }
  }

  private String replaceUrl(String body, Map<String, String> urlToReplaces) {
    for (String url : urlToReplaces.keySet()) {
      while (body.contains(url)) {
        body = body.replace(url, urlToReplaces.get(url));
      }
    }
    return body;
  }

  private void addAllNodes(Page note, List<Page> listOfNotes) throws WikiException {
    if (note != null) {
      listOfNotes.add(note);
      List<Page> children = getChildrenNoteOf(note, true, false);
      if (children != null) {
        for (Page child: children) {
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
    MetadataService metadataService = CommonsUtils.getService(MetadataService.class);
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

  /**
   * {@inheritDoc}
   */
  @Override
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
    long featuredImageId = featuredImage.getId() != null ? featuredImage.getId(): 0L;
    String uploadId = featuredImage.getUploadId();
    if (uploadId != null && featuredImage.getBase64Data() != null) {
      UploadResource uploadResource = uploadService.getUploadResource(uploadId);
      if (uploadResource != null) {
        String data = featuredImage.getBase64Data().split("base64,")[1];
        byte[] bytes = Base64.getDecoder().decode(data.getBytes(Charset.defaultCharset()));
        FileItem fileItem = new FileItem(featuredImageId,
                                         note.getName(),
                                         featuredImage.getMimeType(),
                                         FILE_NAME_SPACE,
                                         (long) uploadResource.getUploadedSize(),
                                         new Date(),
                                         null,
                                         false,
                                         new ByteArrayInputStream(bytes));
        if (featuredImageId == 0) {
          fileItem = fileService.writeFile(fileItem);
        } else {
          fileItem = fileService.updateFile(fileItem);
        }
        if (fileItem != null && fileItem.getFileInfo() != null) {
          return fileItem.getFileInfo().getId();
        }
      }
    }
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NoteFeaturedImage getNoteFeaturedImageInfo(Long noteId, String lang, boolean isDraft, String thumbnailSize, long userIdentityId) throws Exception {
    if (noteId == null) {
      throw new IllegalArgumentException("note id is mandatory");
    }
    Page note;
    org.exoplatform.social.core.identity.model.Identity identity = identityManager.getIdentity(String.valueOf(userIdentityId));
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
                                                    isDraft ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE
                                                            : NOTE_METADATA_PAGE_OBJECT_TYPE);
    if (metadataItem != null && !MapUtils.isEmpty(metadataItem.getProperties())) {
      String featuredImageIdProp = metadataItem.getProperties().get(FEATURED_IMAGE_ID);
      long noteFeaturedImageId = featuredImageIdProp != null
          && !featuredImageIdProp.equals("null") ? Long.parseLong(featuredImageIdProp) : 0L;
      FileItem fileItem = fileService.getFile(noteFeaturedImageId);
      if (fileItem != null && fileItem.getFileInfo() != null) {
        FileInfo fileInfo = fileItem.getFileInfo();
        if (thumbnailSize != null) {
          ImageThumbnailService thumbnailService = CommonsUtils.getService(ImageThumbnailService.class);
          int[] dimension = org.exoplatform.social.common.Utils.parseDimension(thumbnailSize);
          fileItem = thumbnailService.getOrCreateThumbnail(fileItem, identity, dimension[0], dimension[1]);
        }
        return new NoteFeaturedImage(fileInfo.getId(),
                                     fileInfo.getName(),
                                     fileInfo.getMimetype(),
                                     fileInfo.getSize(),
                                     fileInfo.getUpdatedDate().getTime(),
                                     fileItem.getAsStream());
      }
    }
    return null;
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
    MetadataService metadataService = CommonsUtils.getService(MetadataService.class);
    return metadataService.getMetadataItemsByMetadataAndObject(NOTES_METADATA_KEY, noteMetadataObject)
                          .stream()
                          .findFirst()
                          .orElse(null);
  }

  private Map<String, String> moveOrCopyNotePageProperties(Page oldNote,
                                                           Page note,
                                                           String oldLang,
                                                           String lang,
                                                           String oldObjectType,
                                                           String newObjectType,
                                                           String username,
                                                           boolean move) {
    if (note == null || oldNote == null) {
      return null;
    }
    Map<String, String> properties = new HashMap<>();
    if (username != null) {
      org.exoplatform.social.core.identity.model.Identity identity =
                                                                   identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                       username);
      NoteMetadataObject newNoteMetadataObject = buildNoteMetadataObject(note, lang, newObjectType);
      MetadataItem oldNoteMetadataItem = getNoteMetadataItem(oldNote, oldLang, oldObjectType);
      MetadataItem newNoteMetadataItem = getNoteMetadataItem(note, lang, newObjectType);
      if (oldNoteMetadataItem != null && oldNoteMetadataItem.getProperties() != null) {
        MetadataService metadataService = CommonsUtils.getService(MetadataService.class);
        properties = oldNoteMetadataItem.getProperties();
        if (properties != null && oldLang == null && lang != null) {
          properties.remove(FEATURED_IMAGE_ID);
          properties.remove(FEATURED_IMAGE_ALT_TEXT);
          properties.remove(FEATURED_IMAGE_UPDATED_DATE);
        }
        if (newNoteMetadataItem != null) {
          newNoteMetadataItem.setProperties(properties);
          metadataService.updateMetadataItem(newNoteMetadataItem, Long.parseLong(identity.getId()));
        } else {
          try {
            metadataService.createMetadataItem(newNoteMetadataObject,
                                               NOTES_METADATA_KEY,
                                               properties,
                                               Long.parseLong(identity.getId()));
          } catch (Exception e) {
            log.error("Error while creating note metadata item", e);
          }
        }
        if (move) {
          metadataService.deleteMetadataById(oldNoteMetadataItem.getId());
        }
      }
    }
    return properties;
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
  
  /**
   * {@inheritDoc}
   */
  @Override
  public void removeNoteFeaturedImage(Long noteId,
                                      Long featuredImageId,
                                      String lang,
                                      boolean isDraft,
                                      Long userIdentityId) throws Exception {
    if (featuredImageId == null || featuredImageId <= 0) {
      return;
    }
    boolean removeFeaturedImageFile = true;
    Page note;
    if (isDraft) {
      DraftPage draftPage = getDraftNoteById(String.valueOf(noteId),
                                             identityManager.getIdentity(String.valueOf(userIdentityId)).getRemoteId());
      if (isOriginalFeaturedImage(draftPage, getNoteByIdAndLang(Long.valueOf(draftPage.getTargetPageId()), lang))) {
        removeFeaturedImageFile = false;
      }
      note = draftPage;
    } else {
      note = getNoteByIdAndLang(noteId, lang);
    }
    if (note == null) {
      throw new ObjectNotFoundException("note not found");
    }
    MetadataService metadataService = CommonsUtils.getService(MetadataService.class);
    if (removeFeaturedImageFile) {
      fileService.deleteFile(featuredImageId);
    }
    MetadataItem metadataItem = getNoteMetadataItem(note,
                                                    lang,
                                                    isDraft ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE
                                                            : NOTE_METADATA_PAGE_OBJECT_TYPE);
    if (metadataItem != null) {
      Map<String, String> properties = metadataItem.getProperties();
      properties.remove(FEATURED_IMAGE_ID);
      properties.remove(FEATURED_IMAGE_UPDATED_DATE);
      properties.remove(FEATURED_IMAGE_ALT_TEXT);
      metadataItem.setProperties(properties);
      metadataService.updateMetadataItem(metadataItem, userIdentityId);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public NotePageProperties saveNoteMetadata(NotePageProperties notePageProperties, String lang, Long userIdentityId) throws Exception {
    if (notePageProperties == null) {
      return null;
    }
    Page note;
    Long featuredImageId = null;
    if (notePageProperties.isDraft()) {
        note = getLatestDraftPageByTargetPageAndLang(notePageProperties.getNoteId(), lang);
        if (note == null) {
          note = getDraftNoteById(String.valueOf(notePageProperties.getNoteId()),
                                  identityManager.getIdentity(String.valueOf(userIdentityId)).getRemoteId());
        }
    } else {
      note = getNoteByIdAndLang(notePageProperties.getNoteId(), lang);
    }
    if (note == null) {
      throw new ObjectNotFoundException("note not found");
    }
    NoteFeaturedImage featuredImage = notePageProperties.getFeaturedImage();
    if (featuredImage != null && featuredImage.isToDelete()) {
      removeNoteFeaturedImage(Long.valueOf(note.getId()),
                              featuredImage.getId(),
                              lang,
                              notePageProperties.isDraft(),
                              userIdentityId);
    } else {
      featuredImageId = saveNoteFeaturedImage(note, featuredImage);
    }
    MetadataService metadataService = CommonsUtils.getService(MetadataService.class);

    NoteMetadataObject noteMetadataObject =
                                          buildNoteMetadataObject(note,
                                                                  lang,
                                                                  notePageProperties.isDraft() ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE
                                                                                               : NOTE_METADATA_PAGE_OBJECT_TYPE);
    MetadataItem metadataItem = getNoteMetadataItem(note,
                                                    lang,
                                                    notePageProperties.isDraft() ? NOTE_METADATA_DRAFT_PAGE_OBJECT_TYPE
                                                                                 : NOTE_METADATA_PAGE_OBJECT_TYPE);

    Map<String, String> properties = new HashMap<>();
    if (metadataItem != null && metadataItem.getProperties() != null) {
      properties = metadataItem.getProperties();
    }
    properties.put(SUMMARY_PROP, notePageProperties.getSummary());
    if (featuredImageId != null) {
      properties.put(FEATURED_IMAGE_ID, String.valueOf(featuredImageId));
      properties.put(FEATURED_IMAGE_UPDATED_DATE, String.valueOf(new Date().getTime()));
      properties.put(FEATURED_IMAGE_ALT_TEXT, notePageProperties.getFeaturedImage().getAltText());
    }
    if (metadataItem == null) {
      metadataService.createMetadataItem(noteMetadataObject, NOTES_METADATA_KEY, properties, userIdentityId);
    } else {
      metadataItem.setProperties(properties);
      metadataService.updateMetadataItem(metadataItem, userIdentityId);
    }
    if (featuredImage != null) {
      featuredImage.setId(featuredImageId);
      featuredImage.setLastUpdated(Long.valueOf(properties.getOrDefault(FEATURED_IMAGE_UPDATED_DATE, "0")));
      notePageProperties.setFeaturedImage(featuredImage);
    }
    return notePageProperties;
  }
}
