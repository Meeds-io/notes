/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.mow.api.*;

public class TestNoteService extends BaseTest {
  private WikiService wService;
  private NoteService noteService;
  public void setUp() throws Exception {
    super.setUp() ;
    wService = getContainer().getComponentInstanceOfType(WikiService.class) ;
    noteService = getContainer().getComponentInstanceOfType(NoteService.class) ;
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
  }
  

  public void testGetGroupPageById() throws WikiException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.GROUP_TYPE, "/platform/users");
    Identity root = new Identity("root");

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", "WikiHome")) ;

    try {
      noteService.createNote(wiki, "WikiHome", new Page("testGetGroupPageById-101", "testGetGroupPageById-101"),root);
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", "testGetGroupPageById-101")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "unknown", "WikiHome"));
  }

  public void testGetUserPageById() throws WikiException, IllegalAccessException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "john");
    Identity john = new Identity("john");
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", "WikiHome")) ;

    noteService.createNote(wiki, "WikiHome", new Page("testGetUserPageById-101", "testGetUserPageById-101"), john);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", "testGetUserPageById-101")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "unknown", "WikiHome"));
  }

  public void testCreatePageAndSubNote() throws WikiException, IllegalAccessException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Identity root = new Identity("root");
    noteService.createNote(wiki, "WikiHome", new Page("parentPage_", "parentPage_"), root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "parentPage_", root)) ;
    noteService.createNote(wiki, "parentPage_", new Page("childPage_", "childPage_"),root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "childPage_", root)) ;
  }

  public void testGetBreadcumb() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "WikiHome", new Page("Breadcumb1_", "Breadcumb1_"),root) ;
    noteService.createNote(portalWiki, "Breadcumb1_", new Page("Breadcumb2_", "Breadcumb2_"),root) ;
    noteService.createNote(portalWiki, "Breadcumb2_", new Page("Breadcumb3_", "Breadcumb3_"),root) ;
    List<BreadcrumbData> breadCumbs = noteService.getBreadcumb(PortalConfig.PORTAL_TYPE, "classic", "Breadcumb3_");
    assertEquals(4, breadCumbs.size());
    assertEquals("WikiHome", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1_", breadCumbs.get(1).getId());
    assertEquals("Breadcumb2_", breadCumbs.get(2).getId());
    assertEquals("Breadcumb3_", breadCumbs.get(3).getId());
  }

  public void testMoveNote() throws WikiException, IllegalAccessException {
    //moving page in same space
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "WikiHome", new Page("oldParent_", "oldParent_"),root) ;
    noteService.createNote(portalWiki, "oldParent_", new Page("child_", "child_"),root) ;
    noteService.createNote(portalWiki, "WikiHome", new Page("newParent_", "newParent_"),root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "oldParent_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "child_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "newParent_")) ;

    WikiPageParams currentLocationParams= new WikiPageParams();
    WikiPageParams newLocationParams= new WikiPageParams();
    currentLocationParams.setPageName("child_");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("classic");
    newLocationParams.setPageName("newParent_");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");

    assertTrue(noteService.moveNote(currentLocationParams,newLocationParams,root)) ;

    //moving page from different spaces
    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");
    noteService.createNote(userWiki, "WikiHome", new Page("acmePage_", "acmePage_"),root) ;
    noteService.createNote(portalWiki, "WikiHome", new Page("classicPage_", "classicPage_"),root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "root", "acmePage_",root)) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "classicPage_",root)) ;

    currentLocationParams.setPageName("acmePage_");
    currentLocationParams.setType(PortalConfig.USER_TYPE);
    currentLocationParams.setOwner("root");
    newLocationParams.setPageName("classicPage_");
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");
    assertTrue(noteService.moveNote(currentLocationParams,newLocationParams,root)) ;

    // moving a page to another read-only page
    Wiki demoWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "root");
    noteService.createNote(demoWiki, "WikiHome", new Page("toMovedPage_", "toMovedPage_"),root);
    Page page = noteService.createNote(userWiki, "WikiHome", new Page("privatePage_", "privatePage_"),root);
    HashMap<String, String[]> permissionMap = new HashMap<>();
    permissionMap.put("any", new String[] {PermissionType.VIEWPAGE.toString(), PermissionType.EDITPAGE.toString()});
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    PermissionEntry permissionEntry = new PermissionEntry(IdentityConstants.ANY.toString(), "", IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    });
    permissionEntries.add(permissionEntry);
    page.setPermissions(permissionEntries);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "root", "toMovedPage_"));
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "root", "privatePage_"));

    currentLocationParams.setPageName("toMovedPage_");
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("root");
    newLocationParams.setPageName("privatePage_");
    newLocationParams.setType(PortalConfig.USER_TYPE);
    newLocationParams.setOwner("root");
  }

  public void testDeleteNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "WikiHome", new Page("deletePage_", "deletePage_"),root) ;
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    //wait(10) ;
    noteService.createNote(portalWiki, "WikiHome", new Page("deletePage_", "deletePage_"),root) ;
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    assertFalse(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "WikiHome")) ;
  }


  public void testRenameNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "WikiHome", new Page("currentPage_", "currentPage_"),root) ;
    assertTrue(noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", "currentPage_", "renamedPage_", "renamedPage_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "renamedPage_")) ;
  }

  public void testRenamePageToExistingNote() throws WikiException, IllegalAccessException  {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "WikiHome", new Page("currentPage_", "currentPage_"),root) ;
    noteService.createNote(portalWiki, "WikiHome", new Page("currentPage2_", "currentPage2_"),root) ;
    try {
      noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", "currentPage_", "currentPage2_", "renamedPage2_");
      fail("Renaming page currentPage to the existing page currentPage2_ should throw an exception");
    } catch (WikiException e) {
      assertEquals("Note portal:classic:currentPage2_ already exists, cannot rename it.", e.getMessage());
    }
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "currentPage_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "currentPage2_")) ;
  }

  public void testUpdateNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    // Get wiki home
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic").getWikiHome();

    // Create a wiki page for test
    Page page = new Page("new page_", "new page_");
    page.setContent("Page content");
    page = noteService.createNote(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "WikiHome", page,root);
    assertNotNull(page);
    assertEquals("Page content", page.getContent());
    assertEquals("new page_", page.getTitle());

    // update content of page
    page.setContent("Page content updated_");
    noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT,root);
    assertNotNull(page);
    assertEquals("Page content updated_", page.getContent());

    // update title of page
    page.setTitle("new page updated_");
    noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT,root);
    assertNotNull(page);
    assertEquals("new page updated_", page.getTitle());
  }

}
