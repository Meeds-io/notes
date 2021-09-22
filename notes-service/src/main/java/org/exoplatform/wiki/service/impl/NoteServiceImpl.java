package org.exoplatform.wiki.service.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.wiki.utils.Utils;
import org.gatein.api.EntityNotFoundException;

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
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.WikiConstants;

public class NoteServiceImpl implements NoteService {


  public static final String                              CACHE_NAME                       = "wiki.PageRenderingCache";

  public static final String                              ATT_CACHE_NAME                   = "wiki.PageAttachmentCache";

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


  public NoteServiceImpl(ConfigurationManager configManager,
                         UserACL userACL,
                         DataStorage dataStorage,
                         CacheService cacheService,
                         OrganizationService orgService,
                         WikiService wikiService,
                         IdentityManager identityManager,
                         HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.configManager = configManager;
    this.userACL = userACL;
    this.dataStorage = dataStorage;
    this.orgService = orgService;
    this.wikiService = wikiService;
    this.identityManager = identityManager;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);
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
/*      if (!hasPermissionOnNote(note, PermissionType.EDITPAGE, userIdentity)) {
        log.error("User does not have permission to add a note.");
        throw new IllegalAccessException("User does not have permission to add a note.");
      }*/
      List<PermissionEntry> permissions = note.getPermissions();
      // if permissions are not set, init with parent page permissions
      if (permissions == null) {
        if (parentPage.getPermissions() != null) {
          permissions = parentPage.getPermissions();
        } else {
          Page wikiHomePage = noteBook.getWikiHome();
          permissions = wikiHomePage.getPermissions();
        }
        note.setPermissions(permissions);
      }
      try {
        if (StringUtils.equalsIgnoreCase(note.getWikiType(),WikiType.GROUP.name())) {
          note.setContent(htmlUploadImageProcessor.processSpaceImages(note.getContent(), noteBook.getOwner(), "Notes"));
        }
        if (StringUtils.equalsIgnoreCase(note.getWikiType(),WikiType.USER.name())) {
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
      if (!hasPermissionOnNote(note_, PermissionType.EDITPAGE, userIdentity)) {
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
      if (note != null) {
        if (!hasPermissionOnNote(note, PermissionType.EDITPAGE, userIdentity)) {
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
        if (!hasPermissionOnNote(moveNote, PermissionType.EDITPAGE, userIdentity)) {
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
    if (page != null) {
      if (!hasPermissionOnNote(page, PermissionType.VIEWPAGE, userIdentity)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
      boolean canEdit = hasPermissionOnNote(page, PermissionType.EDITPAGE, userIdentity);
      page.setCanEdit(canEdit);
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
  public Page getNoteById(String id, Identity userIdentity) throws IllegalAccessException, WikiException {
    if (id == null) {
      return null;
    }
    Page page = null;
    page = getNoteById(id);
    if (page != null) {
      if (!hasPermissionOnNote(page, PermissionType.VIEWPAGE, userIdentity)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
      boolean canEdit = hasPermissionOnNote(page, PermissionType.EDITPAGE, userIdentity);
      page.setCanEdit(canEdit);
    }
    return page;
  }

  @Override
  public Page getNoteById(String id, Identity userIdentity, String source) throws IllegalAccessException, WikiException {
    if (id == null) {
      return null;
    }
    Page page = null;
    page = getNoteById(id);
    if (page != null) {
      if (!hasPermissionOnNote(page, PermissionType.VIEWPAGE, userIdentity)) {
        throw new IllegalAccessException("User does not have view the note.");
      }
    }
    boolean canEdit = hasPermissionOnNote(page, PermissionType.EDITPAGE, userIdentity);
    page.setCanEdit(canEdit);
    if(StringUtils.isNotEmpty(source)) {
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
  public List<Page> getChildrenNoteOf(Page note) throws WikiException {
    return dataStorage.getChildrenPageOf(note);
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
  public boolean hasPermissionOnNote(Page note, PermissionType permissionType, Identity user) throws WikiException {
    return dataStorage.hasPermissionOnPage(note, permissionType, user);
  }

  @Override
  public boolean hasAdminSpacePermission(String noteBookType, String owner, Identity user) throws WikiException {
    if (user != null) {
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminSpacePermission(noteBookType, owner, user);
  }

  @Override
  public boolean hasAdminNotePermission(String noteType, String owner, Identity user) throws WikiException {
    if (user != null) {
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminPagePermission(noteType, owner, user);
  }

  @Override
  public boolean canModifyNotePermission(Page currentNote, Identity currentIdentity) throws WikiException {
    boolean canModifyPage = false;
    String currentUser = currentIdentity.getUserId();
    String owner = currentNote.getOwner();
    boolean isPageOwner = owner != null && owner.equals(currentUser);
    boolean hasEditPagePermissionOnPage = false;
    if (currentNote.getPermissions() != null) {
      for (PermissionEntry permissionEntry : currentNote.getPermissions()) {
        if (permissionEntry.getId().equals(currentUser)) {
          for (Permission permission : permissionEntry.getPermissions()) {
            if (permission.getPermissionType().equals(PermissionType.EDITPAGE) && permission.isAllowed()) {
              hasEditPagePermissionOnPage = true;
              break;
            }
          }
        }
        if (hasEditPagePermissionOnPage) {
          break;
        }
      }
    }

    if (isPageOwner && hasEditPagePermissionOnPage) {
      canModifyPage = true;
    } else {
      Wiki wiki = wikiService.getWikiByTypeAndOwner(currentNote.getWikiType(), currentNote.getWikiOwner());
      canModifyPage = (hasAdminSpacePermission(wiki.getType(), wiki.getOwner(), currentIdentity))
          || hasAdminNotePermission(wiki.getType(), wiki.getOwner(), currentIdentity);
    }

    return canModifyPage;
  }

  @Override
  public boolean canPublicAndRetrictNote(Page currentNote, Identity currentIdentity) throws WikiException {
    String currentUser = currentIdentity.getUserId();
    if (currentNote.getPermissions() != null) {
      for (PermissionEntry permissionEntry : currentNote.getPermissions()) {
        if (permissionEntry.getId().equals(currentUser)) {
          for (Permission permission : permissionEntry.getPermissions()) {
            if (permission.getPermissionType().equals(PermissionType.EDITPAGE) && permission.isAllowed()) {
              return true;
            }
          }
        }
      }
    }
    Wiki wiki = wikiService.getWikiByTypeAndOwner(currentNote.getWikiType(), currentNote.getWikiOwner());
    return hasAdminNotePermission(wiki.getType(), wiki.getOwner(), currentIdentity)
        || hasAdminSpacePermission(wiki.getType(), wiki.getOwner(), currentIdentity);
  }

  @Override
  public List<PageVersion> getVersionsOfNote(Page note) throws WikiException {
    List<PageVersion> versions = dataStorage.getVersionsOfPage(note);
    if (versions == null || versions.isEmpty()) {
      dataStorage.addPageVersion(note);
      versions = dataStorage.getVersionsOfPage(note);
    }
    return versions;
  }

  @Override
  public List<PageHistory> getVersionsHistoryOfNote(Page note) throws WikiException {
    List<PageHistory> versionsHistory = dataStorage.getHistoryOfPage(note);
    if (versionsHistory == null || versionsHistory.isEmpty()) {
      dataStorage.addPageVersion(note);
      versionsHistory = dataStorage.getHistoryOfPage(note);
    }
    for( PageHistory version: versionsHistory ) {
      org.exoplatform.social.core.identity.model.Identity authorIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, version.getAuthor());
      version.setAuthorFullName(authorIdentity.getProfile().getFullName());
    }
    return versionsHistory;
  }

  @Override
  public PageVersion getVersionOfNoteByName(String versionName, Page note) throws WikiException {
    List<PageVersion> versions = getVersionsOfNote(note);
    PageVersion pageVersion = null;
    if (versions != null) {
      for (PageVersion version : versions) {
        if (version.getName().equals(versionName)) {
          pageVersion = version;
          break;
        }
      }
    }
    return pageVersion;
  }

  @Override
  public void createVersionOfNote(Page note) throws WikiException {
    dataStorage.addPageVersion(note);
  }

  @Override
  public void restoreVersionOfNote(String versionName, Page note) throws WikiException {
    dataStorage.restoreVersionOfPage(versionName, note);
    createVersionOfNote(note);
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

}
