package org.exoplatform.wiki.service.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfig;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.service.LinkProvider;
import org.exoplatform.social.core.space.SpaceFilter;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.*;
import org.exoplatform.wiki.rendering.cache.AttachmentCountData;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.rendering.cache.MarkupKey;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.Utils;

public class WikiServiceImpl implements WikiService {

  public static final String                        CACHE_NAME                       = "wiki.PageRenderingCache";
  public static final String                        ATT_CACHE_NAME                   = "wiki.PageAttachmentCache";
  private static final Log                          LOG                              = ExoLogger.getLogger(WikiServiceImpl.class);
  final static private String                       DEFAULT_SYNTAX                   = "defaultSyntax";
  private static final String                       DEFAULT_WIKI_NAME                = "wiki";
  private static final Log                          log                              = ExoLogger.getLogger(WikiServiceImpl.class);
  private final OrganizationService                       orgService;
  private final UserACL                                   userACL;
  private final DataStorage                               dataStorage;
  private PropertiesParam                           preferencesParams;
  private final List<ComponentPlugin>                     plugins_                         = new ArrayList<>();

  private String                                    wikiWebappUri;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final ExoCache<Integer, AttachmentCountData>    attachmentCountCache;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap                     = new ConcurrentHashMap<>();


  public WikiServiceImpl(UserACL userACL,
                         DataStorage dataStorage,
                         CacheService cacheService,
                         OrganizationService orgService) {

    this.userACL = userACL;
    this.dataStorage = dataStorage;
    this.orgService = orgService;

    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);
    this.attachmentCountCache = cacheService.getCacheInstance(ATT_CACHE_NAME);

    wikiWebappUri = System.getProperty("wiki.permalink.appuri");
    if (StringUtils.isEmpty(wikiWebappUri)) {
      wikiWebappUri = DEFAULT_WIKI_NAME;
    }
  }

  public ExoCache<Integer, MarkupData> getRenderingCache() {
    return renderingCache;
  }

  public Map<WikiPageParams, List<WikiPageParams>> getPageLinksMap() {
    return pageLinksMap;
  }

  /******* Configuration *******/

  @Override
  public void addComponentPlugin(ComponentPlugin plugin) {
    if (plugin != null) {
      plugins_.add(plugin);
    }
  }

  @Override
  public List<PageWikiListener> getPageListeners() {
    List<PageWikiListener> pageListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins_) {
      if (c instanceof PageWikiListener) {
        pageListeners.add((PageWikiListener) c);
      }
    }
    return pageListeners;
  }

  @Override
  public List<AttachmentWikiListener> getAttachmentListeners() {
    List<AttachmentWikiListener> attachmentListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins_) {
      if (c instanceof AttachmentWikiListener) {
        attachmentListeners.add((AttachmentWikiListener) c);
      }
    }
    return attachmentListeners;
  }

  @Override
  public String getWikiWebappUri() {
    return wikiWebappUri;
  }

  @Override
  public String getDefaultWikiSyntaxId() {
    if (preferencesParams != null) {
      return preferencesParams.getProperty(DEFAULT_SYNTAX);
    }
    return "xhtml/1.0";
  }


  /******* Wiki *******/

  @Override
  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException {
    return dataStorage.getWikiByTypeAndOwner(wikiType, owner);
  }

  @Override
  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    return dataStorage.getWikisByType(wikiType);
  }

  @Override
  public Wiki getOrCreateUserWiki(String username) throws WikiException {
    return getWikiByTypeAndOwner(PortalConfig.USER_TYPE, username);
  }

  @Override
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException {
    return dataStorage.getWikiPermission(wikiType, wikiOwner);
  }

  @Override
  public void updateWikiPermission(String wikiType,
                                   String wikiOwner,
                                   List<PermissionEntry> permissionEntries) throws WikiException {
    dataStorage.updateWikiPermission(wikiType, wikiOwner, permissionEntries);
  }

  @Override
  public List<PermissionEntry> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws WikiException {
    Permission[] allPermissions = new Permission[] { new Permission(PermissionType.ADMINPAGE, true),
        new Permission(PermissionType.ADMINSPACE, true) };
    List<PermissionEntry> permissions = new ArrayList<>();
    if (PortalConfig.PORTAL_TYPE.equals(wikiType)) {
      Iterator<Map.Entry<String, IDType>> iter = Utils.getACLForAdmins().entrySet().iterator();
      while (iter.hasNext()) {
        Map.Entry<String, IDType> entry = iter.next();
        PermissionEntry permissionEntry = new PermissionEntry(entry.getKey(), "", entry.getValue(), allPermissions);
        permissions.add(permissionEntry);
      }
      UserPortalConfigService userPortalConfigService =
                                                      ExoContainerContext.getCurrentContainer()
                                                                         .getComponentInstanceOfType(UserPortalConfigService.class);
      try {
        if (userPortalConfigService != null) {
          UserPortalConfig userPortalConfig = userPortalConfigService.getUserPortalConfig(wikiOwner, null);
          if (userPortalConfig != null) {
            PortalConfig portalConfig = userPortalConfig.getPortalConfig();
            PermissionEntry portalPermissionEntry = new PermissionEntry(portalConfig.getEditPermission(),
                                                                        "",
                                                                        IDType.MEMBERSHIP,
                                                                        allPermissions);
            permissions.add(portalPermissionEntry);
          }
        }
      } catch (Exception e) {
        throw new WikiException("Cannot get user portal config for wiki " + wikiType + ":" + wikiOwner + " - Cause : "
            + e.getMessage(), e);
      }
    } else if (PortalConfig.GROUP_TYPE.equals(wikiType)) {
      PermissionEntry groupPermissionEntry = new PermissionEntry(userACL.getMakableMT() + ":" + wikiOwner,
                                                                 "",
                                                                 IDType.MEMBERSHIP,
                                                                 allPermissions);
      permissions.add(groupPermissionEntry);
    } else if (PortalConfig.USER_TYPE.equals(wikiType)) {
      PermissionEntry ownerPermissionEntry = new PermissionEntry(wikiOwner, "", IDType.USER, allPermissions);
      permissions.add(ownerPermissionEntry);
    }

    return permissions;
  }

  @Override
  public Wiki getWikiById(String wikiId) throws WikiException {
    Wiki wiki;
    if (wikiId.startsWith("/spaces/")) {
      wiki = getWikiByTypeAndOwner(PortalConfig.GROUP_TYPE, wikiId);
    } else if (wikiId.startsWith("/user/")) {
      wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      wiki = getWikiByTypeAndOwner(PortalConfig.USER_TYPE, wikiId);
    } else {
      if (wikiId.startsWith("/")) {
        wikiId = wikiId.substring(wikiId.lastIndexOf('/') + 1);
      }
      wiki = getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, wikiId);
    }
    return wiki;
  }

  @Override
  public String getWikiNameById(String wikiId) throws WikiException {
    Wiki wiki = getWikiById(wikiId);
    if (WikiType.PORTAL.equals(wiki.getType())) {
      String displayName = wiki.getId();
      int slashIndex = displayName.lastIndexOf('/');
      if (slashIndex > -1) {
        displayName = displayName.substring(slashIndex + 1);
      }
      return displayName;
    }

    if (WikiType.USER.equals(wiki.getType())) {
      String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
      if (wiki.getOwner().equals(currentUser)) {
        WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
        ResourceBundle res = context.getApplicationResourceBundle();
        return res.getString("UISpaceSwitcher.title.my-space");
      }
      return wiki.getOwner();
    }

    WikiService wikiService = (WikiService) PortalContainer.getComponent(WikiService.class);
    return wikiService.getSpaceNameByGroupId(wiki.getOwner());
  }

  @Override
  public Wiki createWiki(String wikiType, String owner) throws WikiException {
    Wiki wiki = getWikiByTypeAndOwner(wikiType, owner);
    if (wiki != null) {
      throw new WikiException("Wiki with type '" + wikiType + "' and owner = '" + owner + "' already exists");
    }
    wiki = new Wiki(wikiType, owner);
    wiki.setPermissions(getWikiDefaultPermissions(wikiType, owner));
    // set wiki syntax
    WikiPreferences wikiPreferences = new WikiPreferences();
    WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
    wikiPreferencesSyntax.setDefaultSyntax(getDefaultWikiSyntaxId());
    wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
    wiki.setPreferences(wikiPreferences);
    Wiki createdWiki = dataStorage.createWiki(wiki);
    return createdWiki;
  }

  private String getUserDisplayName(String username) {
    try {
      User user = orgService.getUserHandler().findUserByName(username, UserStatus.ANY);
      StringBuilder nameBuilder = new StringBuilder(user.getFirstName());
      nameBuilder.append(" ").append(user.getLastName());
      return nameBuilder.toString();
    } catch (Exception e) {
      return username;
    }
  }

  protected void invalidateAttachmentCache(Page page) {
    WikiPageParams wikiPageParams = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());

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
        LOG.warn(String.format("Failed to invalidate cache of page [%s:%s:%s]",
                               linkedWikiPageParams.getType(),
                               linkedWikiPageParams.getOwner(),
                               linkedWikiPageParams.getPageName()));
      }
    }
  }

  /******* Attachment *******/

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    return dataStorage.getAttachmentsOfPage(page);
  }

  @Override
  public List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    return dataStorage.getAttachmentsOfPage(page, loadContent);
  }

  @Override
  public int getNbOfAttachmentsOfPage(Page page) throws WikiException {
    int nbOfAttachments = 0;

    WikiPageParams wikiPageParams = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
    MarkupKey key = new MarkupKey(wikiPageParams, false);
    Integer cacheKey = new Integer(key.hashCode());
    AttachmentCountData cachedNbOfAttachments = attachmentCountCache.get(cacheKey);
    if (cachedNbOfAttachments != null) {
      nbOfAttachments = cachedNbOfAttachments.build();
    } else {
      try {
        List<Attachment> attachments = dataStorage.getAttachmentsOfPage(page, false);
        nbOfAttachments = attachments == null ? 0 : attachments.size();
        attachmentCountCache.put(cacheKey, new AttachmentCountData(nbOfAttachments));
      } catch (WikiException e) {
        log.error("Cannot get number of attachments of " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName()
            + " - Cause : " + e.getMessage(), e);
      }
    }
    return nbOfAttachments;
  }

  @Override
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page) throws WikiException {
    return getAttachmentOfPageByName(attachmentName, page, false);
  }

  @Override
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page, boolean loadContent) throws WikiException {
    Attachment attachment = null;
    List<Attachment> attachments = dataStorage.getAttachmentsOfPage(page, loadContent);
    for (Attachment att : attachments) {
      if (att.getName().equals(attachmentName)) {
        attachment = att;
        break;
      }
    }
    return attachment;
  }

  @Override
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {
    dataStorage.addAttachmentToPage(attachment, page);

    invalidateAttachmentCache(page);

    // Call listener
    addAttachment(attachment, page);
  }

  @Override
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException {

    // Call listener
    deleteAttachment(attachmentId, page);

    dataStorage.deleteAttachmentOfPage(attachmentId, page);

    invalidateAttachmentCache(page);
  }

  /******* Spaces *******/

  @Override
  public List<SpaceBean> searchSpaces(String keyword) throws WikiException {
    List<SpaceBean> spaceBeans = new ArrayList<>();

    // Get group wiki
    String currentUser = org.exoplatform.wiki.utils.Utils.getCurrentUser();
    try {
      if (StringUtils.isEmpty(keyword)) {
        keyword = "*";
      }
      keyword = keyword.trim();
      // search by keyword
      SpaceFilter spaceFilter = new SpaceFilter(keyword);
      spaceFilter.setRemoteId(currentUser);
      spaceFilter.setAppId("Wiki");

      SpaceService spaceService = ExoContainerContext.getCurrentContainer()
                                                                    .getComponentInstanceOfType(SpaceService.class);
      // search by appId(wiki)
      ListAccess<Space> spaces = spaceService.getAccessibleSpacesByFilter(currentUser, spaceFilter);

      for (Space space : spaces.load(0, spaces.getSize())) {
        String groupId = space.getGroupId();
        String spaceName = space.getDisplayName();
        String avatarUrl = space.getAvatarUrl();
        if (StringUtils.isBlank(avatarUrl)) {
          avatarUrl = getDefaultSpaceAvatarUrl();
        }
        spaceBeans.add(new SpaceBean(groupId, spaceName, PortalConfig.GROUP_TYPE, avatarUrl));
      }
    } catch (ClassNotFoundException e) {
      Collection<Wiki> wikis = getWikisByType(WikiType.GROUP.toString());
      if (keyword != null) {
        keyword = keyword.trim();
      }

      if (keyword != null) {
        for (Wiki wiki : wikis) {
          if (wiki.getId().contains(keyword)) {
            spaceBeans.add(new SpaceBean(wiki.getOwner(), wiki.getId(), PortalConfig.GROUP_TYPE, ""));
          }
        }
      }
    } catch (Exception e) {
      throw new WikiException("Error while searching in wikis for user " + currentUser + " - Cause : " + e.getMessage(), e);
    }
    return spaceBeans;
  }

  private String getDefaultSpaceAvatarUrl() {
    return LinkProvider.SPACE_DEFAULT_AVATAR_URL;
  }

  @Override
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws WikiException {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminSpacePermission(wikiType, owner, user);
  }

  @Override
  public String getSpaceNameByGroupId(String groupId) {
    SpaceService spaceService = ExoContainerContext.getCurrentContainer()
                                                                  .getComponentInstanceOfType(SpaceService.class);
    Space space = spaceService.getSpaceByGroupId(groupId);
    if (space == null) {
      LOG.warn("Can't find space with group id " + groupId);
      return groupId.substring(groupId.lastIndexOf('/') + 1);
    } else {
      return space.getDisplayName();
    }
  }

  // ******* Listeners *******/

  public void postUpdatePage(final String wikiType,
                             final String wikiOwner,
                             final String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
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
    List<PageWikiListener> listeners = getPageListeners();
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
    List<PageWikiListener> listeners = getPageListeners();
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

  public void addAttachment(Attachment attachment, Page page) throws WikiException {
    List<AttachmentWikiListener> listeners = getAttachmentListeners();
    for (AttachmentWikiListener l : listeners) {
      try {
        l.addAttachment(attachment, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on attachment with name = [%s] failed",
                  l,
                                 attachment.getName()),
                   e);
        }
      }
    }
  }

  public void deleteAttachment(String attachmentId, Page page) throws WikiException {
    List<AttachmentWikiListener> listeners = getAttachmentListeners();
    for (AttachmentWikiListener l : listeners) {
      try {
        l.deleteAttachment(attachmentId, page);
      } catch (WikiException e) {
        if (log.isWarnEnabled()) {
          log.warn(String.format("Executing listener [%s] on attachment with name = [%s] failed", l, attachmentId), e);
        }
      }
    }
  }

  @Override
  public WikiPageParams getWikiPageParams(BreadcrumbData data) {
    if (data != null) {
      return new WikiPageParams(data.getWikiType(), data.getWikiOwner(), data.getId());
    }
    return null;
  }

  @Override
  public boolean hasAdminPagePermission(String wikiType, String owner) throws WikiException {
    ConversationState conversationState = ConversationState.getCurrent();
    Identity user;
    if (conversationState != null) {
      user = conversationState.getIdentity();
      if (userACL != null && userACL.getSuperUser().equals(user.getUserId())) {
        return true;
      }
    } else {
      user = new Identity(IdentityConstants.ANONIM);
    }

    return dataStorage.hasAdminPagePermission(wikiType, owner, user);
  }

  @Override
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    Page page = null;

    // check in the cache first
    page = dataStorage.getPageOfWikiByName(wikiType, wikiOwner, pageName);

    if (page != null) {
      Identity user = ConversationState.getCurrent().getIdentity();
      if (!dataStorage.hasPermissionOnPage(page, PermissionType.VIEWPAGE, user)) {
        page = null;
      }
    }

    // Check to remove the domain in page url
    checkToRemoveDomainInUrl(page);

    return page;
  }

  @Override
  public boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity user) throws WikiException {
    return dataStorage.hasPermissionOnWiki(wiki, permissionType, user);
  }

  /******* Private methods *******/

  private void checkToRemoveDomainInUrl(Page page) {
    if (page == null) {
      return;
    }

    String url = page.getUrl();
    if (url != null && url.contains("://")) {
      try {
        URL oldURL = new URL(url);
        page.setUrl(oldURL.getPath());
      } catch (MalformedURLException ex) {
        if (log.isWarnEnabled()) {
          log.warn("Malformed url " + url, ex);
        }
      }
    }
  }

}
