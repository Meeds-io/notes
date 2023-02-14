/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service;

import java.util.List;

import org.apache.commons.codec.binary.StringUtils;

import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionEntry;
import org.exoplatform.wiki.model.Wiki;

@SuppressWarnings("deprecation")
public class TestWikiService extends BaseTest {

  private WikiService wService;

  public void setUp() throws Exception {
    super.setUp();
    wService = getContainer().getComponentInstanceOfType(WikiService.class);
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
  }

  public void testWikiService() {
    assertNotNull(wService);
  }

  public void testCreateWiki() throws WikiException {
    Wiki wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNull(wiki);
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "wiki1");
    wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNotNull(wiki);

  }

  public void testCreateWikiPermissions() throws Exception {
    UserPortalConfigService portalConfigService = getContainer().getComponentInstanceOfType(UserPortalConfigService.class);
    String defaultPortal = portalConfigService.getDefaultPortal();
    Wiki siteWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, defaultPortal);
    assertNotNull(siteWiki);
    assertTrue(siteWiki.getPermissions()
                       .stream()
                       .noneMatch(permission -> StringUtils.equals(permission.getId(), IdentityConstants.ANY)));
    Page wikiHome = siteWiki.getWikiHome();
    assertNotNull(wikiHome);
    LayoutService layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    PortalConfig portalConfig = layoutService.getPortalConfig(defaultPortal);
    assertNotNull(portalConfig);
    List<PermissionEntry> permissions = wikiHome.getPermissions();
    assertNotNull(permissions);
    assertTrue(permissions.stream().noneMatch(permission -> StringUtils.equals(permission.getId(), IdentityConstants.ANY)));
  }
}
