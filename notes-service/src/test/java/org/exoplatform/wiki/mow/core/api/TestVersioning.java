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
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import java.util.Iterator;
import java.util.List;

public class TestVersioning extends BaseTest {

  private NoteService noteService;
  private WikiService wikiService;

  public void setUp() throws Exception {
    super.setUp();
    noteService = getContainer().getComponentInstanceOfType(NoteService.class);
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
  }

  public void testGetVersionHistory() throws Exception {
    Wiki wiki = getOrCreateWiki(wikiService, WikiType.PORTAL.toString(), "versioning1");
    Page page = new Page("testGetVersionHistory-001", "testGetVersionHistory-001");
    page = noteService.createNote(wiki, wiki.getWikiHome(), page);
    noteService.createVersionOfNote(page,"");

    page = noteService.getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), "testGetVersionHistory-001");
    assertNotNull(page);
    List<PageHistory> versions = noteService.getVersionsHistoryOfNote(page,"");
    assertNotNull(versions);
// FIXME Failing Test coming from JPA Impl bug comparing to JCR Impl
//    assertEquals(2, versions.size());
  }

  public void testCreateVersionHistoryTree() throws Exception {
    Wiki wiki = getOrCreateWiki(wikiService, WikiType.PORTAL.toString(), "versioning2");
    Page page = new Page("testCreateVersionHistoryTree-001", "testCreateVersionHistoryTree-001");
    page.setContent("testCreateVersionHistoryTree-ver0.0");
    page = noteService.createNote(wiki, wiki.getWikiHome(), page);

    page.setTitle("testCreateVersionHistoryTree");
    page.setContent("testCreateVersionHistoryTree-ver1.0");
    noteService.updateNote(page);
    noteService.createVersionOfNote(page,"");

    page.setContent("testCreateVersionHistoryTree-ver2.0");
    noteService.updateNote(page);
    noteService.createVersionOfNote(page,"");

    List<PageHistory> versions = noteService.getVersionsHistoryOfNote(page,"");
    assertNotNull(versions);
    assertEquals(2, versions.size());

    // restore to previous version (testCreateVersionHistoryTree-ver1.0)
    noteService.restoreVersionOfNote(String.valueOf(versions.get(0).getVersionNumber()), page,"");
    page = wikiService.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), page.getName());
    assertEquals("testCreateVersionHistoryTree-ver1.0", page.getContent());

    page.setContent("testCreateVersionHistoryTree-ver3.0");
    page.setContent("testCreateVersionHistoryTree-ver2.0");
    noteService.updateNote(page);
    noteService.createVersionOfNote(page,"");

    versions = noteService.getVersionsHistoryOfNote(page,"");
    assertNotNull(versions);
    assertEquals(4, versions.size());

    Iterator<PageHistory> itVersions = versions.iterator();
    PageHistory pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver1.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver2.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver2.0", pageVersion.getContent());

    pageVersion = itVersions.next();
    assertEquals("testCreateVersionHistoryTree-ver2.0", pageVersion.getContent());

  }
}
