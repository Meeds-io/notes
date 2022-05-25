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

package org.exoplatform.wiki.mow.core.api;

import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;


public class TestPageContent extends BaseTest {

  private WikiService wikiService;
  private NoteService noteService;

  public void setUp() throws Exception {
    super.setUp();
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
    noteService = getContainer().getComponentInstanceOfType(NoteService.class);
  }

  public void testGetPageContent() throws Exception {
    Wiki wiki = getOrCreateWiki(wikiService, WikiType.PORTAL.toString(), "classic");
    Page page = new Page("AddPageContent-001", "AddPageContent-001");
    page.setSyntax("xhtml/1.0");
    page.setContent("This is a content of page");
    noteService.createNote(wiki, wiki.getWikiHome(), page);

    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), "AddPageContent-001");
    assertNotNull(page);
    assertEquals("xhtml/1.0", page.getSyntax());
    assertEquals("This is a content of page", page.getContent());
  }
}
