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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.cache.CacheService;
import org.exoplatform.services.cache.ExoCache;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiPreferences;
import org.exoplatform.wiki.model.WikiPreferencesSyntax;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.storage.NoteDataStorage;

public class WikiServiceImpl implements WikiService {

  private static final Log                                LOG               = ExoLogger.getLogger(WikiServiceImpl.class);

  private static final String                             DEFAULT_SYNTAX    = "defaultSyntax";

  private static final String                             DEFAULT_WIKI_NAME = "wiki";

  private final LayoutService                             layoutService;

  private final SpaceService                              spaceService;

  private final UserACL                                   userAcl;

  private final NoteDataStorage                           dataStorage;

  private PropertiesParam                                 preferencesParams;

  private final List<ComponentPlugin>                     plugins           = new ArrayList<>();

  private String                                          wikiWebappUri;

  private final Map<WikiPageParams, List<WikiPageParams>> pageLinksMap      = new ConcurrentHashMap<>();

  public WikiServiceImpl(LayoutService layoutService,
                         UserACL userAcl,
                         SpaceService spaceService,
                         NoteDataStorage dataStorage) {
    this.layoutService = layoutService;
    this.userAcl = userAcl;
    this.spaceService = spaceService;
    this.dataStorage = dataStorage;

    wikiWebappUri = System.getProperty("wiki.permalink.appuri");
    if (StringUtils.isEmpty(wikiWebappUri)) {
      wikiWebappUri = DEFAULT_WIKI_NAME;
    }
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
      if (c instanceof PageWikiListener pageWikiListener) {
        pageListeners.add(pageWikiListener);
      }
    }
    return pageListeners;
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
    // set wiki syntax
    WikiPreferences wikiPreferences = new WikiPreferences();
    WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
    wikiPreferencesSyntax.setDefaultSyntax(getDefaultWikiSyntaxId());
    wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
    wiki.setPreferences(wikiPreferences);
    return dataStorage.createWiki(wiki);
  }

  @Override
  public String getSpaceNameByGroupId(String groupId) {
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
        LOG.warn("Executing listener {} on {} failed", l, page.getName(), e);
      }
    }
  }

  public void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException {
    List<PageWikiListener> listeners = getPageListeners();
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
    List<PageWikiListener> listeners = getPageListeners();
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

  @Override
  public WikiPageParams getWikiPageParams(BreadcrumbData data) {
    if (data != null) {
      return new WikiPageParams(data.getWikiType(), data.getWikiOwner(), data.getId());
    }
    return null;
  }

  @Override
  public boolean canManageWiki(String wikiType, String wikiOwner, String username) {
    if (StringUtils.isBlank(wikiType) || StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      Space space = spaceService.getSpaceByGroupId(wikiOwner);
      return space == null ? userAcl.hasPermission(userAcl.getUserIdentity(username),
                                                   String.format("%s:%s",
                                                                 userAcl.getAdminMSType(),
                                                                 wikiOwner)) :
                           spaceService.canRedactOnSpace(space, username);
    } else if (StringUtils.equalsIgnoreCase(WikiType.PORTAL.name(), wikiType)) {
      PortalConfig portalConfig = layoutService.getPortalConfig(wikiOwner);
      return userAcl.hasEditPermission(portalConfig, userAcl.getUserIdentity(username));
    } else {
      return StringUtils.equals(wikiOwner, username);
    }
  }

}
