package org.exoplatform.wiki.service.impl;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;
import org.gatein.api.EntityNotFoundException;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class NoteServiceImpl implements NoteService {


  public static final String                              CACHE_NAME                       = "wiki.PageRenderingCache";

  public static final String                              ATT_CACHE_NAME                   = "wiki.PageAttachmentCache";

  private static final String                             UNTITLED_PREFIX                  = "Untitled_";

  private static final Log                                log                              =
                                                              ExoLogger.getLogger(NoteServiceImpl.class);

  private final ConfigurationManager                      configManager;

  private final OrganizationService                       orgService;

  private final WikiService                               wikiService;

  private final UserACL                                   userACL;

  private final HTMLUploadImageProcessor                  htmlUploadImageProcessor;

  private final DataStorage                               dataStorage;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final ExoCache<Integer, AttachmentCountData>    attachmentCountCache;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap                     = new ConcurrentHashMap<>();

  private IdentityManager identityManager;

  private SpaceService spaceService;


  public NoteServiceImpl(ConfigurationManager configManager,
                         UserACL userACL,
                         DataStorage dataStorage,
                         CacheService cacheService,
                         OrganizationService orgService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         HTMLUploadImageProcessor htmlUploadImageProcessor,
                         SpaceService spaceService) {
    this.configManager = configManager;
    this.userACL = userACL;
    this.dataStorage = dataStorage;
    this.orgService = orgService;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
    this.spaceService = spaceService;
  }

  public ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }

  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
  }

  @Override
  public Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException, IllegalAccessException {

    String pageName = TitleResolver.getId(note.getTitle(), false);
    note.setName(pageName);

    if (isExisting(noteBook.getType(), noteBook.getOwner(), pageName)) {
      throw new WikiException("Page " + noteBook.getType() + ":" + noteBook.getOwner() + ":" + pageName
          + " already exists, cannot create it.");
    }

    Page parentPage = getNoteOfNoteBookByName(noteBook.getType(), noteBook.getOwner(), parentNoteName);
    if (parentPage != null) {
      note.setOwner(userIdentity.getUserId());
      try {
        if (StringUtils.equalsIgnoreCase(noteBook.getType(),WikiType.GROUP.name())) {
          note.setContent(htmlUploadImageProcessor.processSpaceImages(note.getContent(), noteBook.getOwner(), "Notes"));
        }
        if (StringUtils.equalsIgnoreCase(noteBook.getType(),WikiType.USER.name())) {
          note.setContent(htmlUploadImageProcessor.processUserImages(note.getContent(), noteBook.getOwner(), "Notes"));
        }
      } catch (Exception e) {
       log.warn("can't process note's images");
      }
      Page createdPage = createNote(noteBook, parentPage, note);
      createdPage.setToBePublished(note.isToBePublished());
      createdPage.setAppName(note.getAppName());
      createdPage.setUrl(Utils.getPageUrl(createdPage));
      invalidateCache(parentPage);
      invalidateCache(note);

      // call listeners
      postAddPage(noteBook.getType(), noteBook.getOwner(), note.getName(), createdPage);

      return createdPage;
    }else{
      throw new EntityNotFoundException("Parent note not foond");
    }
  }

  @Override
  public Page createNote(Wiki noteBook, Page parentPage, Page note) throws WikiException {

    return dataStorage.createPage(noteBook, parentPage, note);
  }


  @Override
  public void updateNote(Page note) throws WikiException {
    dataStorage.updatePage(note);
  }

  @Override
  public Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws WikiException,
          IllegalAccessException,
          EntityNotFoundException {
    Page note_ = getNoteById(note.getId());
    if (note_ == null) {
      throw new EntityNotFoundException("Note to update not found");
    }
    if (note_ != null) {
      Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
      if (!canManageNotes(userIdentity.getUserId(), space, note_)) {
        throw new IllegalAccessException("User does not have edit the note.");
      }
    }
    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    ListenerService listenerService = (ListenerService) PortalContainer.getComponent(ListenerService.class);
    // update updated date if the page content has been updated
    if (PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)) {
      note.setUpdatedDate(Calendar.getInstance().getTime());
    }
    try {
      if (note.getWikiType().toUpperCase().equals(WikiType.GROUP.name())) {
        note.setContent(htmlUploadImageProcessor.processSpaceImages(note.getContent(), note.getWikiOwner(), "Notes"));
      }
      if (note.getWikiType().toUpperCase().equals(WikiType.USER.name())) {
        note.setContent(htmlUploadImageProcessor.processUserImages(note.getContent(), note.getWikiOwner(), "Notes"));
      }
    } catch (Exception e) {
      log.warn("can't process note's images");
    }
    updateNote(note);
    invalidateCache(note);

    if (PageUpdateType.EDIT_PAGE_CONTENT.equals(type) || PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE.equals(type)) {
      try {
        listenerService.broadcast("exo.wiki.edit", wikiService, note);
      } catch (Exception e) {
        log.error("Error while broadcasting wiki edition event", e);
      }
    }

    Page updatedPage = getNoteById(note.getId());
    updatedPage.setUrl(Utils.getPageUrl(updatedPage));
    updatedPage.setToBePublished(note.isToBePublished());
    updatedPage.setAppName(note.getAppName());
    postUpdatePage(updatedPage.getWikiType(), updatedPage.getWikiOwner(), updatedPage.getName(), updatedPage, type);

    return updatedPage;
  }


  @Override
  public boolean deleteNote(String noteType, String noteOwner, String noteName) throws WikiException {
    if (WikiConstants.WIKI_HOME_NAME.equals(noteName) || noteName == null) {
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
    if (WikiConstants.WIKI_HOME_NAME.equals(noteName) || noteName == null) {
      return false;
    }

    try {
      Page note = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
      if (note == null) {
        log.error("Can't delete note '" + noteName + "'. This note does not exist.");
        throw new EntityNotFoundException("Note to update not found");
      }
      Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
      if (note != null) {
        if (!canManageNotes(userIdentity.getUserId(), space, note)) {
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
          List<Page> childrenPages = getChildrenNoteOf(tempPage);
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
    if (WikiConstants.WIKI_HOME_NAME.equals(noteName) || noteName == null) {
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
        throw new EntityNotFoundException("Note to update not found");
      }
      if (moveNote != null) {
        Space space = spaceService.getSpaceByGroupId(moveNote.getWikiOwner());
        if (!canManageNotes( userIdentity.getUserId(), space, moveNote)) {
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
    Page page = getNoteOfNoteBookByName(noteType,noteOwner,noteName,userIdentity);
    if(StringUtils.isNotEmpty(source)) {
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
      page.setCanManage(canManageNotes( userIdentity.getUserId(), space, page));
      page.setCanView(canViewNotes( userIdentity.getUserId(), space, page));
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
  public DraftPage getDraftNoteById(String id) throws WikiException {
    if (id == null) {
      return null;
    }

    return dataStorage.getDraftPageById(id);
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
      page.setCanManage(canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanView(canViewNotes(userIdentity.getUserId(), space, page));
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
      page.setCanManage(canManageNotes(userIdentity.getUserId(), space, page));
      page.setCanView(canViewNotes(userIdentity.getUserId(), space, page));
      if (StringUtils.isNotEmpty(source)) {
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
    if(parent==null){
      return null;
    }
    return new NoteToExport(parent.getId() ,parent.getName(), parent.getOwner(), parent.getAuthor(), parent.getContent(), parent.getSyntax(), parent.getTitle(), parent.getComment(), parent.getWikiId(), parent.getWikiType(), parent.getWikiOwner());
  }

  @Override
  public List<Page> getChildrenNoteOf(Page note) throws WikiException {

    return dataStorage.getChildrenPageOf(note);
  }

  @Override
  public List<NoteToExport> getChildrenNoteOf(NoteToExport note) throws WikiException {

    Page page = new Page();
    page.setId(note.getId());
    page.setName(note.getName());
    page.setWikiId(note.getWikiId());
    page.setWikiOwner(note.getWikiOwner());
    page.setWikiType(note.getWikiType());

    List<Page> pages = getChildrenNoteOf(page);

    List<NoteToExport> children = new ArrayList<>();

    for(Page child : pages){
      if(child==null){
        continue;
      }
      children.add(new NoteToExport(child.getId() ,child.getName(), child.getOwner(), child.getAuthor(), child.getContent(), child.getSyntax(), child.getTitle(), child.getComment(), child.getWikiId(), child.getWikiType(), child.getWikiOwner()));
    }
    return children;
  }

  @Override
  public List<BreadcrumbData> getBreadcumb(String noteType, String noteOwner, String noteName) throws WikiException {
    return getBreadcumb(null, noteType, noteOwner, noteName);
  }

  @Override
  public List<Page> getDuplicateNotes(Page parentNote, Wiki targetNoteBook, List<Page> resultList) throws WikiException {
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
    List<Page> childrenNotes = getChildrenNoteOf(parentNote);
    for (Page note : childrenNotes) {
      getDuplicateNotes(note, targetNoteBook, resultList);
    }
    return resultList;
  }



  @Override
  public void removeDraftOfNote(WikiPageParams param) throws WikiException {
    Page page = getNoteOfNoteBookByName(param.getType(), param.getOwner(), param.getPageName());
    dataStorage.deleteDraftOfPage(page, Utils.getCurrentUser());
  }





  @Override
  public List<PageHistory> getVersionsHistoryOfNote(Page note, String userName) throws WikiException {
    List<PageHistory> versionsHistory = dataStorage.getHistoryOfPage(note);
    if (versionsHistory == null || versionsHistory.isEmpty()) {
      dataStorage.addPageVersion(note, userName);
      versionsHistory = dataStorage.getHistoryOfPage(note);
    }
    for( PageHistory version: versionsHistory ) {
      if (version.getAuthor() != null) {
        org.exoplatform.social.core.identity.model.Identity authorIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, version.getAuthor());
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
  public Page getNoteByRootPermission(String noteBookType, String noteBookOwner, String noteId) throws WikiException {
    return dataStorage.getPageOfWikiByName(noteBookType, noteBookOwner, noteId);
  }

  @Override
  public DraftPage updateDraftForExistPage(DraftPage draftNoteToUpdate, Page targetPage, String revision, long clientTime, String username) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setId(draftNoteToUpdate.getId());
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftNoteToUpdate.getTitle());
    newDraftPage.setTargetPageId(draftNoteToUpdate.getTargetPageId());
    newDraftPage.setContent(draftNoteToUpdate.getContent());
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
    newDraftPage.setTargetPageRevision("1");
    newDraftPage.setContent(draftNoteToUpdate.getContent());
    newDraftPage.setCreatedDate(new Date(clientTime));
    newDraftPage.setUpdatedDate(new Date(clientTime));

    newDraftPage = dataStorage.updateDraftPageForUser(newDraftPage, Utils.getCurrentUser());

    return newDraftPage;
  }

  @Override
  public DraftPage createDraftForExistPage(DraftPage draftPage, Page targetPage, String revision, long clientTime, String username) throws WikiException {
    // Create suffix for draft name
    String draftSuffix = getDraftNameSuffix(clientTime);

    DraftPage newDraftPage = new DraftPage();
    newDraftPage.setName(targetPage.getName() + "_" + draftSuffix);
    newDraftPage.setNewPage(false);
    newDraftPage.setTitle(draftPage.getTitle());
    newDraftPage.setTargetPageId(targetPage.getId());
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
    newDraftPage.setContent(draftPage.getContent());
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
   * @throws WikiException if an error occured
   */
  protected void invalidateCachesOfPageTree(Page note) throws WikiException {
    Queue<Page> queue = new LinkedList<>();
    queue.add(note);
    while (!queue.isEmpty()) {
      Page currentPage = queue.poll();
      invalidateCache(currentPage);
      List<Page> childrenPages = getChildrenNoteOf(currentPage);
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

  //******* Listeners *******/

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
    if (space != null){
      return (spaceService.isSuperManager(authenticatedUser) || spaceService.isManager(space, authenticatedUser) || spaceService.isRedactor(space, authenticatedUser) ||
              spaceService.isMember(space, authenticatedUser) && ArrayUtils.isEmpty(space.getRedactors()));
    } else return page.getOwner().equals(authenticatedUser);

  }

  private boolean canViewNotes(String authenticatedUser, Space space, Page page) throws WikiException {
    if (space != null){
      return space != null && spaceService.isMember(space, authenticatedUser);
    } else return spaceService.isSuperManager(authenticatedUser) || page.getOwner().equals(authenticatedUser);
  }

  /**
   * Recursive method to build the breadcump of a note
   * 
   * @param list
   * @param noteType
   * @param noteOwner
   * @param noteName
   * @return
   * @throws WikiException
   */
  private List<BreadcrumbData> getBreadcumb(List<BreadcrumbData> list,
                                            String noteType,
                                            String noteOwner,
                                            String noteName) throws WikiException {
    if (list == null) {
      list = new ArrayList<>(5);
    }
    if (noteName == null) {
      return list;
    }
    Page note = getNoteOfNoteBookByName(noteType, noteOwner, noteName);
    if (note == null) {
      return list;
    }
    list.add(0, new BreadcrumbData(note.getName(),note.getId(), note.getTitle(), noteType, noteOwner));
    Page parentNote = getParentNoteOf(note);
    if (parentNote != null) {
      getBreadcumb(list, noteType, noteOwner, parentNote.getName());
    }

    return list;
  }

  private String getDraftNameSuffix(long clientTime) {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));
  }

  /**
   * Recursive method to build the children and parent of a note
   *
   * @param note

   * @return
   * @throws WikiException
   */
  @Override
  public NoteToExport getNoteToExport(NoteToExport note) throws WikiException {
    note.setContent(processImagesForExport(note.getContent()));
    List<NoteToExport> children = getChildrenNoteOf(note);
    for (NoteToExport child: children){
      child.setParent(note);
    }
    note.setChildren(children);
    note.setParent(getParentNoteOf(note));
    for (NoteToExport child: children){
      getNoteToExport( child);
    }
    return note;
  }

  @Override
  public List<File>  getFilesfromContent(NoteToExport note, List<File> files) throws WikiException {
    String contentUpdated = note.getContent();
    String fileName = "";
    String filePath = "";
    while (contentUpdated.contains("//-")){
       fileName = contentUpdated.split("//-")[1].split("-//")[0];
       filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
      files.add(new File(filePath));
      contentUpdated =  contentUpdated.replace("//-"+fileName+"-//","");
    }
    List<NoteToExport> children = getChildrenNoteOf(note);
    for (NoteToExport child: children){
      getFilesfromContent(child,files);
    }
    return files;
  }

  /**
   *
   *
   * @param content

   * @return
   * @throws WikiException
   */
  @Override
  public String processImagesForExport(String content) throws WikiException {
   return htmlUploadImageProcessor.processImagesForExport(content);
  }

  /**
   * Recursive method to importe a note
   *
   * @param note

   * @return
   * @throws WikiException
   */
  @Override
  public void importNote(Page note, Page parent, Wiki wiki, String conflict) throws WikiException {

    Page  parent_ = getNoteOfNoteBookByName(wiki.getType(),wiki.getOwner(),parent.getName());
    if(parent_ == null){
      parent_ = wiki.getWikiHome();
    }
    Page note_ = note;
    if(!note.getName().equals("Home")){
      note.setId(null);
      Page note_2 = getNoteOfNoteBookByName(wiki.getType(),wiki.getOwner(),note.getName());
      if(note_2!=null){
        note_ = createNote(wiki,parent_,note);
      }else{
        if(conflict.equals("overwrite")){
          deleteNote(wiki.getType(),wiki.getOwner(),note.getName());
          note_ = createNote(wiki,parent_,note);
        }if(conflict.equals("duplicate")){
          int i = 1;
          String newTitle = note.getTitle() + "_" + i;
          while (getNoteOfNoteBookByName(wiki.getType(),wiki.getOwner(),newTitle)!=null){
            i++;
            newTitle = note.getTitle() + "_" + i;
          }
          note.setName(newTitle);
          note.setTitle(newTitle);
          note_ = createNote(wiki,parent_,note);
        }if(conflict.equals("update")){
          if(!note_2.getTitle().equals(note.getTitle())||!note_2.getContent().equals(note.getContent())){
            note_2.setContent(note.getContent());
            note_2.setTitle(note.getTitle());
            updateNote(note);
          }
        }
      }
    }else{
      if(conflict.equals("update")){
        Page note_1 = getNoteOfNoteBookByName(wiki.getType(),wiki.getOwner(),note.getName());
        note_1.setContent(note.getContent());
        updateNote(note);
      }
    }
    for (Page child: note.getChildren()){
      importNote(child, note_, wiki, conflict);
    }
  }

}
