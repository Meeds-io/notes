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
package org.exoplatform.wiki.service;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.NoteConstants;

public class TestWikiService extends BaseTest {

  private WikiService wService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    wService = getContainer().getComponentInstanceOfType(WikiService.class);
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
  }

  public void testWikiService() {
    assertNotNull(wService);
  }

  public void testCreateWiki() {
    Wiki wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNull(wiki);
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "wiki1");
    wiki = wService.getWikiByTypeAndOwner(PortalConfig.PORTAL_TYPE, "wiki1");
    assertNotNull(wiki);
  }

  public void testCreateWikiBroadcastsHomePageCreation() {
    RecordingPageListener listener = new RecordingPageListener();
    wService.addComponentPlugin(listener);

    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "wiki2");

    assertEquals(1, listener.addedPages.size());
    Page homePage = listener.addedPages.get(0);
    assertEquals(NoteConstants.NOTE_HOME_NAME, homePage.getName());
    assertNotNull("the home page must carry its id so that it can be indexed", homePage.getId());
    assertEquals("the broadcast page id must be the home name so that the listeners not concerned by system pages can skip it",
                 NoteConstants.NOTE_HOME_NAME,
                 listener.addedPageIds.get(0));
  }

  private static class RecordingPageListener extends PageWikiListener {
    private final List<Page>   addedPages   = new ArrayList<>();

    private final List<String> addedPageIds = new ArrayList<>();

    @Override
    public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) {
      addedPages.add(page);
      addedPageIds.add(pageId);
    }
  }

}
