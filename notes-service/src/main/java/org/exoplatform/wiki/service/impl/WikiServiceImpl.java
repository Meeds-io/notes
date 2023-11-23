package org.exoplatform.wiki.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Permission;
import org.exoplatform.wiki.model.PermissionEntry;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiPreferences;
import org.exoplatform.wiki.model.WikiPreferencesSyntax;
import org.exoplatform.wiki.rendering.cache.MarkupData;
import org.exoplatform.wiki.service.DataStorage;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.Utils;

public class WikiServiceImpl implements WikiService {

  public static final String                              CACHE_NAME        = "wiki.PageRenderingCache";

  private static final Log                                LOG               = ExoLogger.getLogger(WikiServiceImpl.class);

  private static final String                             DEFAULT_SYNTAX    = "defaultSyntax";

  private static final String                             DEFAULT_WIKI_NAME = "wiki";

  private final SpaceService                              spaceService;

  private final LayoutService                             layoutService;

  private final OrganizationService                       orgService;

  private final UserACL                                   userACL;

  private final DataStorage                               dataStorage;

  private PropertiesParam                                 preferencesParams;

  private final List<ComponentPlugin>                     plugins           = new ArrayList<>();

  private String                                          wikiWebappUri;

  private final ExoCache<Integer, MarkupData>             renderingCache;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap      = new ConcurrentHashMap<>();

  public WikiServiceImpl(UserACL userACL,
                         SpaceService spaceService,
                         LayoutService layoutService,
                         DataStorage dataStorage,
                         CacheService cacheService,
                         OrganizationService orgService) {

    this.userACL = userACL;
    this.dataStorage = dataStorage;
    this.orgService = orgService;
    this.spaceService = spaceService;
    this.layoutService = layoutService;

    this.renderingCache = cacheService.getCacheInstance(CACHE_NAME);

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
      plugins.add(plugin);
    }
  }

  @Override
  public List<PageWikiListener> getPageListeners() {
    List<PageWikiListener> pageListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins) {
      if (c instanceof PageWikiListener) {
        pageListeners.add((PageWikiListener) c);
      }
    }
    return pageListeners;
  }

  @Override
  public List<AttachmentWikiListener> getAttachmentListeners() {
    List<AttachmentWikiListener> attachmentListeners = new ArrayList<>();
    for (ComponentPlugin c : plugins) {
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
      try {
        PortalConfig portalConfig = layoutService.getPortalConfig(wikiOwner);
        if (portalConfig != null) {
          PermissionEntry portalPermissionEntry = new PermissionEntry(portalConfig.getEditPermission(),
                                                                      "",
                                                                      IDType.MEMBERSHIP,
                                                                      allPermissions);
          permissions.add(portalPermissionEntry);
        }
      } catch (Exception e) {
        throw new WikiException("Cannot get user portal config for wiki " + wikiType + ":" + wikiOwner + " - Cause : " +
            e.getMessage(), e);
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
    StringBuilder sb = new StringBuilder("<h1> Welcome to ");
    String wikiLabel = owner;
    if (wikiType.equals(PortalConfig.GROUP_TYPE)) {
      sb.append("Space ");
      wikiLabel = getSpaceNameByGroupId(owner);
    } else if (wikiType.equals(PortalConfig.USER_TYPE)) {
      wikiLabel = this.getUserDisplayName(wiki.getOwner());
    }
    sb.append(wikiLabel).append(" Notes Home </h1>");
    createdWiki.getWikiHome().setContent(sb.toString());
    dataStorage.updatePage(createdWiki.getWikiHome());
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

  private String getSpaceNameByGroupId(String groupId) {
    Space space = spaceService.getSpaceByGroupId(groupId);
    if (space == null) {
      LOG.warn("Can't find space with group id " + groupId);
      return groupId.substring(groupId.lastIndexOf('/') + 1);
    } else {
      return space.getDisplayName();
    }
  }

}
