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
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;
import org.gatein.api.EntityNotFoundException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
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
import org.exoplatform.social.core.space.SpaceUtils;
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
import org.exoplatform.wiki.model.Wiki;
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

  public static final String                              CACHE_NAME          = "wiki.PageRenderingCache";

  private static final String                             UNTITLED_PREFIX     = "Untitled_";

  private static final String                             TEMP_DIRECTORY_PATH = "java.io.tmpdir";

  private static final Log                                LOG                 = ExoLogger.getLogger(NoteServiceImpl.class);

  private final WikiService                               wikiService;

  private final DataStorage                               dataStorage;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final IdentityManager                           identityManager;

  private final SpaceService                              spaceService;

  private final CMSService                                cmsService;

  private final UserACL                                   userAcl;

  private final LayoutService                             layoutService;

  private final ListenerService                           listenerService;

  private final HTMLUploadImageProcessor                  htmlUploadImageProcessor;

  public NoteServiceImpl(DataStorage dataStorage, // NOSONAR
                         CacheService cacheService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         LayoutService layoutService,
                         ListenerService listenerService,
                         UserACL userACL) {
    this(dataStorage,
         cacheService,
         wikiService,
         identityManager,
         spaceService,
         cmsService,
         layoutService,
         listenerService,
         userACL,
         null);
  }

  public NoteServiceImpl(DataStorage dataStorage, // NOSONAR
                         CacheService cacheService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         SpaceService spaceService,
                         CMSService cmsService,
                         LayoutService layoutService,
                         ListenerService listenerService,
                         UserACL userACL,
                         HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.dataStorage = dataStorage;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.spaceService = spaceService;
    this.listenerService = listenerService;
    this.cmsService = cmsService;
    this.layoutService = layoutService;
    this.userAcl = userACL;
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

  @Override
  public Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException,
                                                                                                 IllegalAccessException {
    return createNote(noteBook, parentNoteName, note, userIdentity, true);
  }

  @SneakyThrows
  @Override
  public Page createNote(Wiki noteBook, Page parentPage, Page note) throws WikiException {
    return createNote(noteBook, parentPage.getName(), note, null, false);
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
    return updateNote(note, type, userIdentity, true);
  }

  @SneakyThrows
  @Override
  public Page updateNote(Page note, PageUpdateType type) throws WikiException {
    return updateNote(note, type, null, false);
  }

  @Override
  public boolean deleteNote(String noteType, String noteOwner, String noteName) throws WikiException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    try {
      Queue<Page> queue = new LinkedList<>();
      Page note = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
      queue.add(note);

      List<Page> allChrildrenPages = new ArrayList<>();
      Page tempPage;
      while (!queue.isEmpty()) {
        tempPage = queue.poll();
        List<Page> childrenPages = getChildrenNoteOf(tempPage, null, false, false);
        for (Page childPage : childrenPages) {
          queue.add(childPage);
          allChrildrenPages.add(childPage);
        }
      }
      dataStorage.deletePage(noteType, noteOwner, noteName);
      postDeletePage(noteType, noteOwner, noteName, note);

      // Post delete activity for all children pages
      for (Page childNote : allChrildrenPages) {
        postDeletePage(childNote.getWikiType(), childNote.getWikiOwner(), childNote.getName(), childNote);
      }
      return true;
    } catch (WikiException e) {
      LOG.error("Can't delete note '" + noteName + "' ", e);
      return false;
    }
  }

  @Override
  public boolean deleteNote(String noteType, String noteOwner, String noteName, Identity userIdentity) throws WikiException,
                                                                                                       IllegalAccessException,
                                                                                                       EntityNotFoundException {
    if (NoteConstants.NOTE_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }
    Page note = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (note == null) {
      throw new EntityNotFoundException("Note to delete not found");
    } else if (!canManagePage(note, userIdentity)) {
      throw new IllegalAccessException("User does not have edit permissions on the note.");
    } else {
      invalidateCachesOfPageTree(note, userIdentity.getUserId());
      return deleteNote(noteType, noteOwner, noteName);
    }
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
      } else if (!canManagePage(moveNote, userIdentity)) {
        throw new IllegalAccessException("User does not have enough permissions to edit the note.");
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
      LOG.error("Can't move note '" + currentLocationParams.getPageName() + "' ", e);
      return false;
    }
    return true;
  }

  @Override
  public Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName) throws WikiException {
    Page page = dataStorage.getPageOfWikiByName(noteType, noteOwner, noteName);
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
    Page page = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (page == null) {
      throw new EntityNotFoundException("page not found");
    } else if (!canViewPage(page, userIdentity)) {
      throw new IllegalAccessException("User does not have view the note.");
    } else {
      page.setCanManage(canManagePage(page, userIdentity));
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
  public DraftPage getDraftNoteById(String id, Identity userIdentity) throws WikiException, IllegalAccessException {
    if (id == null) {
      return null;
    }
    DraftPage draftPage = dataStorage.getDraftPageById(id);
    if (draftPage == null) {
      return null;
    }
    if (!canViewPage(draftPage, userIdentity)) {
      throw new IllegalAccessException("User does not have the right view the note.");
    }
    draftPage.setCanManage(canManagePage(draftPage, userIdentity));
    String authorFullName = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, draftPage.getAuthor())
                                           .getProfile()
                                           .getFullName();
    draftPage.setAuthorFullName(authorFullName);
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
    return getNoteById(id, userIdentity, null);
  }

  @Override
  public Page getNoteById(String id, Identity userIdentity, String source) throws IllegalAccessException, WikiException {
    if (id == null) {
      return null;
    }
    Page page = getNoteById(id);
    if (page == null) {
      return null;
    } else if (!canViewPage(page, userIdentity)) {
      throw new IllegalAccessException("User does not have view the note.");
    }
    page.setUrl(Utils.getPageUrl(page));
    page.setCanManage(canManagePage(page, userIdentity));
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

    List<Page> pages = getChildrenNoteOf(page, userId, false, false);
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

    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());

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
    newDraftPage.setSyntax(draftPage.getSyntax());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    newDraftPage = dataStorage.createDraftPageForUser(newDraftPage, Utils.getCurrentUser());

    return newDraftPage;
  }

  @Override
  public boolean canManagePage(Page page, Identity identity) {
    Space space = StringUtils.contains(page.getWikiOwner(),
                                       SpaceUtils.SPACE_GROUP) ? spaceService.getSpaceByGroupId(page.getWikiOwner()) :
                                                               null;
    if (identity == null || IdentityConstants.ANONIM.equals(identity.getUserId())) {
      return false;
    } else if (space != null) {
      String username = identity.getUserId();
      return (spaceService.isSuperManager(username)
              || spaceService.isManager(space, username)
              || spaceService.canRedactOnSpace(space, identity));
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM) || StringUtils.isBlank(page.getOwner())) {
      return cmsService.hasEditPermission(identity, NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return isPageOwner(page, identity) || isPortalOwner(page, identity);
    }
  }

  @Override
  public boolean canViewPage(Page page, Identity identity) {
    Space space = StringUtils.contains(page.getWikiOwner(),
                                       SpaceUtils.SPACE_GROUP) ? spaceService.getSpaceByGroupId(page.getWikiOwner()) :
                                                               null;
    if (space != null && identity != null) {
      return spaceService.isMember(space, identity.getUserId());
    } else if (StringUtils.equals(page.getOwner(), IdentityConstants.SYSTEM) || StringUtils.isBlank(page.getOwner())) {
      return cmsService.hasAccessPermission(identity, NotePageViewService.CMS_CONTENT_TYPE, page.getName());
    } else {
      return isPageOwner(page, identity) || isPortalAccessible(page, identity);
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
      List<Page> childrenPages = getChildrenNoteOf(currentPage, userId, false, false);
      for (Page child : childrenPages) {
        queue.add(child);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
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
        if (LOG.isWarnEnabled()) {
          LOG.warn(String.format("Executing listener [%s] on [%s] failed", l, page.getName()), e);
        }
      }
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
        if (LOG.isWarnEnabled()) {
          LOG.warn("Malformed url " + url, ex);
        }
      }
    }
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
      LOG.error(String.format("Failed to get rendered content of note [%s:%s:%s]",
                              note.getWikiType(),
                              note.getWikiOwner(),
                              note.getName()),
                e);
    }
    return renderedContent;
  }

  /**
   * importe a list of notes from zip
   *
   * @param zipLocation the path to the zip file
   * @param parent parent note where note will be imported
   * @param conflict import mode if there in conflicts it can be : overwrite,
   *          duplicate, update or nothing
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
   * @param files List of files
   * @param parent parent note where note will be imported
   * @param conflict import mode if there in conflicts it can be : overwrite,
   *          duplicate, update or nothing
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
              LOG.warn("Note {} connot be deleted for import", noteTodelete.getName(), e);
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
   * @param note note to import
   * @param parent parent note where note will be imported
   * @param wiki the Notebook where note will be imported
   * @param conflict import mode if there in conflicts it can be : overwrite,
   *          duplicate, update or nothing
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
            while (getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), newTitle) != null ||
                   isExisting(wiki.getType(), wiki.getOwner(), TitleResolver.getId(newTitle, false))) {
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
              String processedContent = htmlUploadImageProcessor.processSpaceImages(note_2.getContent(),
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
      LOG.error("Cannot search on wiki " + data.getWikiType() + ":" + data.getWikiOwner() + " - Cause : " + e.getMessage(), e);
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
    if (Files.exists(file.toPath())) {
      Files.delete(file.toPath());
    }
  }

  public List<Page> getAllNotes(Page note, String userName) throws WikiException {
    List<Page> listOfNotes = new ArrayList<Page>();
    addAllNodes(note, listOfNotes, userName);
    return listOfNotes;
  }

  private Page createNote(Wiki noteBook,
                          String parentNoteName,
                          Page note,
                          Identity userIdentity,
                          boolean checkAcl) throws WikiException,
                                            IllegalAccessException {

    String pageName = TitleResolver.getId(note.getTitle(), false);
    note.setName(pageName);

    if (isExisting(noteBook.getType(), noteBook.getOwner(), pageName)) {
      throw new WikiException("Page " + noteBook.getType() + ":" + noteBook.getOwner() + ":" + pageName +
          " already exists, cannot create it.");
    }

    Page parentPage = userIdentity
        == null ? getNoteOfNoteBookByName(noteBook.getType(), noteBook.getOwner(), parentNoteName) :
                getNoteOfNoteBookByName(noteBook.getType(), noteBook.getOwner(), parentNoteName, userIdentity);
    if (parentPage == null) {
      throw new EntityNotFoundException("Parent note not foond");
    } else if (checkAcl && (userIdentity == null || !canManagePage(parentPage, userIdentity))) {
      throw new IllegalAccessException("User does not have enough permissions to create the note.");
    } else {
      if (userIdentity == null) {
        note.setOwner(note.getOwner());
        note.setAuthor(note.getAuthor());
      } else {
        note.setOwner(userIdentity.getUserId());
        note.setAuthor(userIdentity.getUserId());
      }
      note.setContent(note.getContent());
      Page createdPage = dataStorage.createPage(noteBook, parentPage, note);
      createdPage = userIdentity == null ? getNoteById(createdPage.getId()) : getNoteById(createdPage.getId(), userIdentity);
      createdPage.setToBePublished(note.isToBePublished());
      Utils.broadcast(listenerService, "note.posted", createdPage.getAuthor(), createdPage);
      postAddPage(noteBook.getType(), noteBook.getOwner(), createdPage.getName(), createdPage);

      return createdPage;
    }
  }

  private Page updateNote(Page note,
                          PageUpdateType type,
                          Identity userIdentity,
                          boolean checkAcl) throws WikiException,
                                            IllegalAccessException,
                                            EntityNotFoundException {
    Page existingNote = getNoteById(note.getId());
    if (existingNote == null) {
      throw new EntityNotFoundException("Note to update not found");
    }
    if (checkAcl && (userIdentity == null || !canManagePage(existingNote, userIdentity))) {
      throw new IllegalAccessException("User does not have enough permissions to edit the note.");
    }
    if (PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)) {
      note.setUpdatedDate(Calendar.getInstance().getTime());
    }
    note.setContent(note.getContent());
    Page updatedPage = dataStorage.updatePage(note);
    updatedPage.setUrl(Utils.getPageUrl(updatedPage));
    updatedPage.setToBePublished(note.isToBePublished());
    updatedPage.setAppName(note.getAppName());
    if (userIdentity != null) {
      updatedPage.setCanManage(canManagePage(updatedPage, userIdentity));
      Map<String, List<MetadataItem>> metadata = retrieveMetadataItems(note.getId(), userIdentity.getUserId());
      updatedPage.setMetadatas(metadata);
    }
    Utils.broadcast(listenerService, "note.updated", note.getAuthor(), updatedPage);
    postUpdatePage(updatedPage.getWikiType(), updatedPage.getWikiOwner(), updatedPage.getName(), updatedPage, type);

    return updatedPage;
  }

  private void addAllNodes(Page note, List<Page> listOfNotes, String userName) throws WikiException {
    if (note != null) {
      listOfNotes.add(note);
      List<Page> children = getChildrenNoteOf(note, userName, true, false);
      if (children != null) {
        for (Page child : children) {
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

  private boolean isPageOwner(Page page, Identity identity) {
    return StringUtils.equalsIgnoreCase(page.getWikiType(), "user") && StringUtils.equals(page.getOwner(), identity.getUserId());
  }
  
  private boolean isPortalOwner(Page page, Identity identity) {
    if (!StringUtils.equalsIgnoreCase(page.getWikiType(), "portal")) {
      return false;
    }
    PortalConfig portalConfig = layoutService.getPortalConfig(page.getWikiOwner());
    return StringUtils.equals(userAcl.getSuperUser(), identity.getUserId())
        || (portalConfig != null && userAcl.hasPermission(identity, portalConfig.getEditPermission()));
  }

  private boolean isPortalAccessible(Page page, Identity identity) {
    if (!StringUtils.equalsIgnoreCase(page.getWikiType(), "portal")) {
      return false;
    }
    PortalConfig portalConfig = layoutService.getPortalConfig(page.getWikiOwner());
    return StringUtils.equals(userAcl.getSuperUser(), identity.getUserId())
           || (portalConfig != null
               && portalConfig.getAccessPermissions() != null
               && Arrays.stream(portalConfig.getAccessPermissions())
                        .allMatch(perm -> userAcl.hasPermission(identity, perm)));
  }
}
