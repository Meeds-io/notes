/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service.impl;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import lombok.SneakyThrows;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.wiki.model.*;
import org.gatein.api.EntityNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.WikiException;
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

public class NoteServiceImpl implements NoteService {

  public static final String                              CACHE_NAME          = "wiki.PageRenderingCache";

  public static final String                              ATT_CACHE_NAME      = "wiki.PageAttachmentCache";

  private static final String                             UNTITLED_PREFIX     = "Untitled_";

  private static final String                             TEMP_DIRECTORY_PATH = "java.io.tmpdir";

  private static final Log                                log                 = ExoLogger.getLogger(NoteServiceImpl.class);

  private final WikiService                               wikiService;

  private final DataStorage                               dataStorage;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final ExoCache<Integer, AttachmentCountData>    attachmentCountCache;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap        = new ConcurrentHashMap<>();

  private final IdentityManager                           identityManager;

  private final SpaceService                              spaceService;

  private final CMSService                                cmsService;

  private final IdentityRegistry                          identityRegistry;

  private final OrganizationService                       organizationService;

  private final ListenerService                           listenerService;

  private final HTMLUploadImageProcessor                  htmlUploadImageProcessor;

  public NoteServiceImpl( DataStorage dataStorage,
                          CacheService cacheService,
                          WikiService wikiService,
                          IdentityManager identityManager,
                          SpaceService spaceService,
                          CMSService cmsService,
                          IdentityRegistry identityRegistry,
                          OrganizationService organizationService,
                          ListenerService listenerService) {
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.identityRegistry = identityRegistry;
    this.organizationService = organizationService;
    this.htmlUploadImageProcessor = null;
  }

  public NoteServiceImpl(DataStorage dataStorage,
                         CacheService cacheService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         IdentityRegistry identityRegistry,
                         OrganizationService organizationService,
                         ListenerService listenerService,
                         HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.identityRegistry = identityRegistry;
    this.organizationService = organizationService;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
  }

  public static File zipFiles(String zipFileName, List<File> addToZip) throws IOException {

    String zipPath = System.getProperty(TEMP_DIRECTORY_PATH) + File.separator + zipFileName;
    cleanUp(new File(zipPath));
    try (FileOutputStream fos = new FileOutputStream(zipPath);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
      zos.setLevel(9);

      for (File file : addToZip) {
        if (file.exists()) {
          try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(file.getName());
            zos.putNextEntry(entry);
            for (int c = fis.read(); c != -1; c = fis.read()) {
              zos.write(c);
            }
            zos.flush();
          }
        }
      }
    }
    File zip = new File(zipPath);
    if (!zip.exists()) {
      throw new FileNotFoundException("The created zip file could not be found");
    }
    return zip;
  }

  public ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }

  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
  }

  @Override
  public Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException,
                                                                                                 IllegalAccessException {

    String pageName = TitleResolver.getId(note.getTitle(), false);
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

      Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
      createdPage.setCanManage(canManageNotes(note.getAuthor(), space, note));
      createdPage.setCanImport(canImportNotes(note.getAuthor(), space, note));
      createdPage.setCanView(canViewNotes(note.getAuthor(), space, note));
      return createdPage;
    } else {
      throw new EntityNotFoundException("Parent note not foond");
    }
  }

  @Override
  public Page createNote(Wiki noteBook, Page parentPage, Page note) throws WikiException {
    Page createdPage = dataStorage.createPage(noteBook, parentPage, note);
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setToBePublished(note.isToBePublished());
    createdPage.setAppName(note.getAppName());
    createdPage.setUrl(Utils.getPageUrl(createdPage));
    createdPage.setLang(note.getLang());
    invalidateCache(parentPage);
    invalidateCache(note);

    Utils.broadcast(listenerService, "note.posted", note.getAuthor(), createdPage);
    postAddPage(noteBook.getType(), noteBook.getOwner(), note.getName(), createdPage);
    return createdPage;
  }

  @SneakyThrows
  @Override
  public Page updateNote(Page note) throws WikiException {
    return updateNote(note, null);
  }

  @Override
  public Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws WikiException,
                                                                                IllegalAccessException,
                                                                                EntityNotFoundException {
    Page note_ = getNoteById(note.getId());
    if (note_ == null) {
      throw new EntityNotFoundException("Note to update not found");
    }
    Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
    if (userIdentity != null && !canManageNotes(userIdentity.getUserId(), space, note_)) {
      throw new IllegalAccessException("User does not have edit the note.");
    }
    if (PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)) {
      note.setUpdatedDate(Calendar.getInstance().getTime());
    }
    note.setContent(note.getContent());
    Page updatedPage = dataStorage.updatePage(note);
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

    return updatedPage;
  }

  @SneakyThrows
  @Override
  public Page updateNote(Page note, PageUpdateType type) throws WikiException {
    return updateNote(note, type, null);
  }

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
        if (!canManageNotes(userIdentity.getUserId(), space, note)) {
          log.error("Can't delete note '" + noteName + "'. does not have edit permission on it.");
          throw new IllegalAccessException("User does not have edit permissions on the note.");
        }

        invalidateCachesOfPageTree(note, userIdentity.getUserId());
        invalidateAttachmentCache(note);

        // Store all children to launch post deletion listeners
        List<Page> allChrildrenPages = new ArrayList<>();
        Queue<Page> queue = new LinkedList<>();
        queue.add(note);
        Page tempPage;
        while (!queue.isEmpty()) {
          tempPage = queue.poll();
          List<Page> childrenPages = getChildrenNoteOf(tempPage, userIdentity.getUserId(), false, false);
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

  @Override
  public void moveNote(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    dataStorage.movePage(currentLocationParams, newLocationParams);
  }

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
        if (!canManageNotes(userIdentity.getUserId(), space, moveNote)) {
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

  @Override
  public Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName) throws WikiException {
    Page page = null;

    // check in the cache first
    page = dataStorage.getPageOfWikiByName(noteType, noteOwner, noteName);
    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

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
      page.setCanManage(canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanImport(canImportNotes(userIdentity.getUserId(), space, page));
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(page.getId(), userIdentity.getUserId());
      page.setMetadatas(metadata);
    }
    return page;
  }

  @Override
  public Page getNoteById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getPageById(id);
  }

  @Override
  public DraftPage getDraftNoteById(String id, String userId) throws WikiException, IllegalAccessException {
    if (id == null) {
      return null;
    }
    DraftPage draftPage = dataStorage.getDraftPageById(id);
    computeDraftProps(draftPage, userId);

    return draftPage;
  }

  private void computeDraftProps(DraftPage draftPage, String userId) throws WikiException, IllegalAccessException {
    if (draftPage != null) {
      Space space = spaceService.getSpaceByGroupId(draftPage.getWikiOwner());
      if (!canViewNotes(userId, space, draftPage)) {
        throw new IllegalAccessException("User does not have the right view the note.");
      }
      draftPage.setCanView(true);
      draftPage.setCanManage(canManageNotes(userId, space, draftPage));
      draftPage.setCanImport(canImportNotes(userId, space, draftPage));
      String authorFullName = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, draftPage.getAuthor())
              .getProfile()
              .getFullName();
      draftPage.setAuthorFullName(authorFullName);
    }
  }

  @Override
  public DraftPage getDraftNoteByIdAndLang(Long draftNoteId, String userId, String lang) throws WikiException, IllegalAccessException {
    DraftPage draftPage = dataStorage.getDraftPageByIdAndLang(draftNoteId, lang);
    computeDraftProps(draftPage, userId);

    return draftPage;
  }

  @Override
  public DraftPage getLatestDraftOfPage(Page targetPage, String username) throws WikiException {
    if (targetPage == null || StringUtils.isEmpty(username)) {
      return null;
    }

    return dataStorage.getLatestDraftOfPage(targetPage, username);
  }

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
      page.setCanManage(canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanImport(canImportNotes(userIdentity.getUserId(), space, page));
    }
    return page;
  }

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
      page.setCanManage(canManageNotes(userIdentity.getUserId(), space, page));
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

  @Override
  public Page getParentNoteOf(Page note) throws WikiException {
    return dataStorage.getParentPageOf(note);
  }

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

  @Override
  public List<Page> getChildrenNoteOf(Page note, String userId, boolean withDrafts, boolean withChild) throws WikiException {
    List<Page> pages = dataStorage.getChildrenPageOf(note, userId, withDrafts);
    if (withChild) {
      for (Page page : pages) {
        long pageId = Long.parseLong(page.getId());
        page.setHasChild(dataStorage.hasChildren(pageId));
      }
    }
    return pages;
  }

  @Override
  public List<NoteToExport> getChildrenNoteOf(NoteToExport note, String userId) throws WikiException {

    Page page = new Page();
    page.setId(note.getId());
    page.setName(note.getName());
    page.setWikiId(note.getWikiId());
    page.setWikiOwner(note.getWikiOwner());
    page.setWikiType(note.getWikiType());

    List<Page> pages = getChildrenNoteOf(page, userId,false, false);
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

  @Override
  public List<BreadcrumbData> getBreadCrumb(String noteType,
                                            String noteOwner,
                                            String noteName,
                                            boolean isDraftNote) throws WikiException {
    return getBreadCrumb(null, noteType, noteOwner, noteName, isDraftNote);
  }

  @Override
  public List<Page> getDuplicateNotes(Page parentNote,
                                      Wiki targetNoteBook,
                                      List<Page> resultList,
                                      String userId) throws WikiException {
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

    // Check the duplication of all childrent
    List<Page> childrenNotes = getChildrenNoteOf(parentNote, userId, false, false);
    for (Page note : childrenNotes) {
      getDuplicateNotes(note, targetNoteBook, resultList, userId);
    }
    return resultList;
  }

  @Override
  public void removeDraftOfNote(WikiPageParams param) throws WikiException {
    Page page = getNoteOfNoteBookByName(param.getType(), param.getOwner(), param.getPageName());
    removeDraftOfNote(page, Utils.getCurrentUser());
  }

  @Override
  public void removeDraftOfNote(Page page, String username) throws WikiException {
    dataStorage.deleteDraftOfPage(page, username);
  }

  @Override
  public void removeDraft(String draftName) throws WikiException {
    dataStorage.deleteDraftByName(draftName, Utils.getCurrentUser());
  }

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

  @Override
  public void createVersionOfNote(Page note, String userName) throws WikiException {
    dataStorage.addPageVersion(note, userName);
  }

  @Override
  public void restoreVersionOfNote(String versionName, Page note, String userName) throws WikiException {
    dataStorage.restoreVersionOfPage(versionName, note);
    createVersionOfNote(note, userName);
    invalidateCache(note);
  }

  @Override
  public List<String> getPreviousNamesOfNote(Page note) throws WikiException {
    return dataStorage.getPreviousNamesOfPage(note);
  }

  @Override
  public List<Page> getNotesOfWiki(String noteType, String noteOwner) {
    return dataStorage.getPagesOfWiki(noteType, noteOwner);
  }

  @Override
  public boolean isExisting(String noteBookType, String noteBookOwner, String noteId) throws WikiException {
    return getNoteByRootPermission(noteBookType, noteBookOwner, noteId) != null;
  }

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

    return newDraftPage;
  }

  @Override
  public DraftPage updateDraftForNewPage(DraftPage draftNoteToUpdate, long clientTime) throws WikiException {
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

    return newDraftPage;
  }

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

    return newDraftPage;
  }

  @Override
  public DraftPage createDraftForNewPage(DraftPage draftPage, long clientTime) throws WikiException {
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
    newDraftPage.setLang(draftPage.getLang());
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());

    return newDraftPage;
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

  /**
   * Invalidate all caches of a page and all its descendants
   *
   * @param note root page
   * @param userId
   * @throws WikiException if an error occured
   */
  protected void invalidateCachesOfPageTree(Page note, String userId) throws WikiException {
    Queue<Page> queue = new LinkedList<>();
    queue.add(note);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      invalidateCache(currentPage);
      List<Page> childrenPages = getChildrenNoteOf(currentPage, userId, false,false);
      for (Page child : childrenPages) {
        queue.add(child);
      }
    }
  }

  // ******* Listeners *******/

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

  /******* Private methods *******/

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

  private boolean canManageNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null) {
      return (spaceService.isSuperManager(authenticatedUser)
          || spaceService.isManager(space, authenticatedUser)
          || spaceService.isRedactor(space, authenticatedUser)
          || spaceService.isMember(space, authenticatedUser) && ArrayUtils.isEmpty(space.getRedactors()));
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM)) {
      return cmsService.hasEditPermission(getIdentity(authenticatedUser), NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return StringUtils.equals(page.getOwner(), authenticatedUser);
    }
  }

  private boolean canImportNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null) {
      return (spaceService.isSuperManager(authenticatedUser) || spaceService.isManager(space, authenticatedUser)
              || spaceService.isRedactor(space, authenticatedUser));
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM)) {
      return cmsService.hasAccessPermission(getIdentity(authenticatedUser), NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return StringUtils.equals(page.getOwner(), authenticatedUser);
    }
  }

  private boolean canViewNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null) {
      return spaceService.isMember(space, authenticatedUser);
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM) || StringUtils.isBlank(page.getOwner())) {
      return cmsService.hasAccessPermission(getIdentity(authenticatedUser), NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return spaceService.isSuperManager(authenticatedUser) || StringUtils.equals(page.getOwner(), authenticatedUser);
    }
  }

  @Override
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException {
    if (StringUtils.equals(IdentityConstants.SYSTEM, page.getOwner())) {
      return false;
    } else if (page.isDraftPage()) {
      return page.getAuthor().equals(user.getUserId());
    }
    return dataStorage.hasPermissionOnPage(page, permissionType, user);
  }


  /**
   * Recursive method to build the breadcump of a note
   *
   * @param list
   * @param noteType
   * @param noteOwner
   * @param noteName
   * @param isDraftNote
   * @return
   * @throws WikiException
   */
  private List<BreadcrumbData> getBreadCrumb(List<BreadcrumbData> list,
                                             String noteType,
                                             String noteOwner,
                                             String noteName,
                                             boolean isDraftNote) throws WikiException {
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
    list.add(0, new BreadcrumbData(note.getName(), note.getId(), note.getTitle(), noteType, noteOwner));
    Page parentNote = isDraftNote ? getNoteById(note.getParentPageId()) : getParentNoteOf(note);
    if (parentNote != null) {
      getBreadCrumb(list, noteType, noteOwner, parentNote.getName(), false);
    }

    return list;
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


  @Override
  public Page getNoteByRootPermission(String wikiType, String wikiOwner, String pageId) throws WikiException {
    return dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageId);
  }

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
   * importe a list of notes from zip
   *
   * @param zipLocation  the path to the zip file
   * @param parent       parent note where note will be imported
   * @param conflict     import mode if there in conflicts it can be : overwrite,
   *                     duplicate, update or nothing
   * @param userIdentity Identity of the user that execute the import
   * @throws WikiException
   */
  @Override
  public void importNotes(String zipLocation, Page parent, String conflict, Identity userIdentity) throws WikiException,
          IllegalAccessException,
          IOException {
    List<String> files = Utils.unzip(zipLocation, System.getProperty(TEMP_DIRECTORY_PATH));
    importNotes(files, parent, conflict, userIdentity);
  }

  /**
   * importe a list of notes from zip
   *
   * @param files        List of files
   * @param parent       parent note where note will be imported
   * @param conflict     import mode if there in conflicts it can be : overwrite,
   *                     duplicate, update or nothing
   * @param userIdentity Identity of the user that execute the import
   * @throws WikiException
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
        List<Page> notesTodelete = getAllNotes(parent, userIdentity.getUserId());
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
   * Recursive method to importe a note
   *
   * @param note         note to import
   * @param parent       parent note where note will be imported
   * @param wiki         the Notebook where note will be imported
   * @param conflict     import mode if there in conflicts it can be : overwrite,
   *                     duplicate, update or nothing
   * @param userIdentity Identity of the user that execute the import
   * @throws WikiException
   */
  public void importNote(Page note, Page parent, Wiki wiki, String conflict, Identity userIdentity) throws WikiException,
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
        String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(), wiki.getOwner(), imagesSubLocationPath);
        note.setContent(processedContent);
        note_ = createNote(wiki, parent_.getName(), note, userIdentity);
      } else {
        if (StringUtils.isNotEmpty(conflict)) {
          if (conflict.equals("overwrite") || conflict.equals("replaceAll")) {
            deleteNote(wiki.getType(), wiki.getOwner(), note.getName());
            String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(), wiki.getOwner(), imagesSubLocationPath);
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
            while (getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), newTitle) != null ||
                    isExisting(wiki.getType(), wiki.getOwner(), TitleResolver.getId(newTitle, false))) {
              i++;
              newTitle = note.getTitle() + "_" + i;
            }
            note.setName(newTitle);
            note.setTitle(newTitle);
            String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(), wiki.getOwner(), imagesSubLocationPath);
            note.setContent(processedContent);
            note_ = createNote(wiki, parent_.getName(), note, userIdentity);
          }
          if (conflict.equals("update")) {
            if (!note_2.getTitle().equals(note.getTitle()) || !note_2.getContent().equals(note.getContent())) {
              note_2.setTitle(note.getTitle());
              String processedContent = htmlUploadImageProcessor.processSpaceImages(note_2.getContent(), wiki.getOwner(), imagesSubLocationPath);
              note_2.setContent(processedContent);
              note_2 = updateNote(note_2, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
              createVersionOfNote(note_2, userIdentity.getUserId());
            }
          }
        }
      }
    } else {
      if (StringUtils.isNotEmpty(conflict) && (conflict.equals("update") || conflict.equals("overwrite") || conflict.equals("replaceAll"))) {
        Page note_1 = getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
        if (!note.getContent().equals(note_1.getContent())) {
          String processedContent = htmlUploadImageProcessor.processSpaceImages(note.getContent(), wiki.getOwner(), imagesSubLocationPath);
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


  public static void cleanUp(File file) throws IOException {
    if(Files.exists(file.toPath())){
      Files.delete(file.toPath());
    }
  }

  public List<Page> getAllNotes(Page note, String userName) throws WikiException {
    List<Page> listOfNotes = new ArrayList<Page>();
    addAllNodes(note, listOfNotes, userName);
    return listOfNotes;
  }

  private void addAllNodes(Page note, List<Page> listOfNotes, String userName) throws WikiException {
    if (note != null) {
      listOfNotes.add(note);
      List<Page> children = getChildrenNoteOf(note, userName, true, false);
      if (children != null) {
        for (Page child: children) {
          addAllNodes(child, listOfNotes, userName);
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

  private Identity getIdentity(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    Identity aclIdentity = identityRegistry.getIdentity(username);
    if (aclIdentity == null) {
      try {
        List<MembershipEntry> entries = organizationService.getMembershipHandler()
                                                           .findMembershipsByUser(username)
                                                           .stream()
                                                           .map(membership -> new MembershipEntry(membership.getGroupId(),
                                                                                                  membership.getMembershipType()))
                                                           .toList();
        aclIdentity = new Identity(username, entries);
        identityRegistry.register(aclIdentity);
      } catch (Exception e) {
        throw new IllegalStateException("Unable to retrieve user " + username + " memberships", e);
      }
    }
    return aclIdentity;
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
    }
    return page;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<String> getPageAvailableTranslationLanguages(Long pageId) {
    return dataStorage.getPageAvailableTranslationLanguages(pageId);
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
  public DraftPage getLatestDraftPageByUserAndTargetPageAndLang(Long targetPageId, String username, String lang) {
    return dataStorage.getLatestDraftPageByUserAndTargetPageAndLang(targetPageId, username, lang);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void deleteVersionsByNoteIdAndLang(Long noteId, String lang) throws WikiException{
    dataStorage.deleteVersionsByNoteIdAndLang(noteId, lang);
  }
}
