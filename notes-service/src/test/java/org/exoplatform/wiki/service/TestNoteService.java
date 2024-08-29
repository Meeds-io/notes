 /**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
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


import static org.exoplatform.social.core.jpa.test.AbstractCoreTest.persist;
import static org.junit.Assert.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import org.apache.commons.io.FileUtils;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.model.*;
import org.junit.Assert;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.jpa.JPADataStorage;

 public class TestNoteService extends BaseTest {
  private WikiService wService;
  private NoteService noteService;
  private NotesExportService notesExportService;

  public TestNoteService() {
    setForceContainerReload(true);
  }

  public void setUp() throws Exception {
    super.setUp() ;
    wService = getContainer().getComponentInstanceOfType(WikiService.class) ;
    noteService = getContainer().getComponentInstanceOfType(NoteService.class) ;
    notesExportService = getContainer().getComponentInstanceOfType(NotesExportService.class);
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
  }
  

  public void testGetGroupPageById() throws WikiException, IllegalAccessException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.GROUP_TYPE, "/platform/users");
    Identity root = new Identity("root");

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", "Home")) ;

    Page notePage = noteService.createNote(wiki, "Home", new Page("testGetGroupPageById-101", "testGetGroupPageById-101"),root);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", notePage.getName())) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "unknown", "Home"));
  }

  public void testGetUserPageById() throws WikiException, IllegalAccessException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "john");
    Identity john = new Identity("john");
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", "Home")) ;

    Page notePage = noteService.createNote(wiki, "Home", new Page("testGetUserPageById-101", "testGetUserPageById-101"), john);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", notePage.getName())) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "unknown", "Home"));
  }

  public void testCreatePageAndSubNote() throws WikiException, IllegalAccessException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Identity root = new Identity("root");
    Page parentNotePage = noteService.createNote(wiki, "Home", new Page("parentPage_", "parentPage_"), root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", parentNotePage.getName(), root)) ;
    Page childNotePage = noteService.createNote(wiki, parentNotePage.getName(), new Page("childPage_", "childPage_"),root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", childNotePage.getName(), root)) ;
  }

  public void testGetBreadcumb() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page breadcrumb1NotPage = noteService.createNote(portalWiki, "Home", new Page("Breadcrumb1_", "Breadcrumb1_"),root) ;
    Page breadcrumb2NotPage = noteService.createNote(portalWiki, breadcrumb1NotPage.getName(), new Page("Breadcrumb2_", "Breadcrumb2_"),root) ;
    Page breadcrumb3NotPage = noteService.createNote(portalWiki, breadcrumb2NotPage.getName(), new Page("Breadcrumb3_", "Breadcrumb3_"),root) ;
    List<BreadcrumbData> breadCumbs = noteService.getBreadCrumb(PortalConfig.PORTAL_TYPE, "classic", breadcrumb3NotPage.getName(), false);
    assertEquals(4, breadCumbs.size());
    assertEquals("Home", breadCumbs.get(0).getId());
    assertEquals(breadcrumb1NotPage.getName(), breadCumbs.get(1).getId());
    assertEquals(breadcrumb2NotPage.getName(), breadCumbs.get(2).getId());
    assertEquals(breadcrumb3NotPage.getName(), breadCumbs.get(3).getId());
  }
  public void testGetBreadcumbWithLanguage() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("Breadcumb1", "Breadcumb1"),root) ;
    Page note2 = noteService.createNote(portalWiki, note1.getName(), new Page("Breadcumb2", "Breadcumb2"),root) ;
    Page note3 = noteService.createNote(portalWiki, note2.getName(), new Page("Breadcumb3", "Breadcumb3"),root) ;

    note1.setLang("fr");
    note1.setTitle("Breadcumb1_fr");
    noteService.createVersionOfNote(note1, "root");
    note2.setLang("fr");
    note2.setTitle("Breadcumb2_fr");
    noteService.createVersionOfNote(note2, "root");
    note3.setLang("fr");
    note3.setTitle("Breadcumb3_fr");
    noteService.createVersionOfNote(note3, "root");
    List<BreadcrumbData> breadCumbs = noteService.getBreadCrumb(PortalConfig.PORTAL_TYPE, "classic", note3.getName(), false);
    assertEquals(4, breadCumbs.size());
    assertEquals("Home", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1", breadCumbs.get(1).getTitle());
    assertEquals("Breadcumb2", breadCumbs.get(2).getTitle());
    assertEquals("Breadcumb3", breadCumbs.get(3).getTitle());
    breadCumbs = noteService.getBreadCrumb(PortalConfig.PORTAL_TYPE, "classic", note3.getName(), "fr", root, false);
    assertEquals(4, breadCumbs.size());
    assertEquals("Home", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1_fr", breadCumbs.get(1).getTitle());
    assertEquals("Breadcumb2_fr", breadCumbs.get(2).getTitle());
    assertEquals("Breadcumb3_fr", breadCumbs.get(3).getTitle());
  }

  public void testMoveNote() throws WikiException, IllegalAccessException {
    //moving page in same space
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page oldParentNotePage = noteService.createNote(portalWiki, "Home", new Page("oldParent_", "oldParent_"), root) ;
    Page childNotePage = noteService.createNote(portalWiki, oldParentNotePage.getName(), new Page("child_", "child_"), root) ;
    Page newsParentNotPage = noteService.createNote(portalWiki, "Home", new Page("newParent_", "newParent_"), root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", oldParentNotePage.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", childNotePage.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", newsParentNotPage.getName())) ;

    WikiPageParams currentLocationParams= new WikiPageParams();
    WikiPageParams newLocationParams= new WikiPageParams();
    currentLocationParams.setPageName(childNotePage.getName());
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("classic");
    newLocationParams.setPageName(newsParentNotPage.getName());
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");

    assertTrue(noteService.moveNote(currentLocationParams, newLocationParams, root)) ;

    //moving page from different spaces
    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");
    Page acmeNotePage = noteService.createNote(userWiki, "Home", new Page("acmePage_", "acmePage_"), root) ;
    Page classicNotePage = noteService.createNote(portalWiki, "Home", new Page("classicPage_", "classicPage_"), root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "root", acmeNotePage.getName(), root)) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", classicNotePage.getName(), root)) ;

    currentLocationParams.setPageName(acmeNotePage.getName());
    currentLocationParams.setType(PortalConfig.USER_TYPE);
    currentLocationParams.setOwner("root");
    newLocationParams.setPageName(classicNotePage.getName());
    newLocationParams.setType(PortalConfig.PORTAL_TYPE);
    newLocationParams.setOwner("classic");
    assertTrue(noteService.moveNote(currentLocationParams, newLocationParams, root)) ;

    // moving a page to another read-only page
    Wiki demoWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "root");
    Page toMovedNotePage = noteService.createNote(demoWiki, "Home", new Page("toMovedPage_", "toMovedPage_"), root);
    Page privateNotePage = noteService.createNote(userWiki, "Home", new Page("privatePage_", "privatePage_"), root);
    HashMap<String, String[]> permissionMap = new HashMap<>();
    permissionMap.put("any", new String[] {PermissionType.VIEWPAGE.toString(), PermissionType.EDITPAGE.toString()});
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    PermissionEntry permissionEntry = new PermissionEntry(IdentityConstants.ANY.toString(), "", IDType.USER, new Permission[]{
            new Permission(PermissionType.VIEWPAGE, true),
            new Permission(PermissionType.EDITPAGE, true)
    });
    permissionEntries.add(permissionEntry);
    privateNotePage.setPermissions(permissionEntries);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "root", toMovedNotePage.getName()));
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "root", privateNotePage.getName()));

    currentLocationParams.setPageName(toMovedNotePage.getName());
    currentLocationParams.setType(PortalConfig.PORTAL_TYPE);
    currentLocationParams.setOwner("root");
    newLocationParams.setPageName(privateNotePage.getName());
    newLocationParams.setType(PortalConfig.USER_TYPE);
    newLocationParams.setOwner("root");
  }

  public void testDeleteNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page notePage1 = noteService.createNote(portalWiki, "Home", new Page("deletePage_", "deletePage_"), root);
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", notePage1.getName()));
    // wait(10) ;
    Page notePage2 = noteService.createNote(portalWiki, "Home", new Page("deletePage_", "deletePage_"), root);
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", notePage2.getName()));
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", notePage2.getName()));
    assertFalse(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "Home"));
  }


  public void testRenameNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page currentNotePage = noteService.createNote(portalWiki, "Home", new Page("currentPage_", "currentPage_"),root);
    assertTrue(noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", currentNotePage.getName(), "renamedPage_", "renamedPage_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "renamedPage_")) ;
  }

  public void testRenamePageToExistingNote() throws WikiException, IllegalAccessException  {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    Page currentNotePage = noteService.createNote(portalWiki, "Home", new Page("currentPage_", "currentPage_"), root) ;
    Page currentNotePage2 = noteService.createNote(portalWiki, "Home", new Page("currentPage2_", "currentPage2_"), root) ;
    try {
      noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", currentNotePage.getName(), currentNotePage2.getName(), "renamedPage2_");
      fail("Renaming page currentPage to the existing page " + currentNotePage2.getName() + " should throw an exception");
    } catch (WikiException e) {
      assertEquals("Note portal:classic:" + currentNotePage2.getName() + " already exists, cannot rename it.", e.getMessage());
    }
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", currentNotePage.getName()));
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", currentNotePage2.getName())) ;
  }
  
  public void testHasPermisionOnSystemPage() throws WikiException, IllegalAccessException, ObjectAlreadyExistsException {
    JPADataStorage storage = getContainer().getComponentInstanceOfType(JPADataStorage.class);

    Wiki wiki = new Wiki();
    wiki.setType("portal");
    wiki.setOwner("testHasPermisionOnSystemPage");
    wiki = storage.createWiki(wiki);

    IdentityRegistry identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);

    Identity adminAclIdentity = new Identity("admin",
                                             Arrays.asList(
                                                           new MembershipEntry("/platform/users", "*"),
                                                           new MembershipEntry("/platform/administrators", "*")));
    identityRegistry.register(adminAclIdentity);

    Identity userAclIdentity = new Identity("user",
                                            Arrays.asList(
                                                          new MembershipEntry("/platform/users", "*")));
    identityRegistry.register(userAclIdentity);

    Page noPermissionPage = new Page();
    noPermissionPage.setWikiId(wiki.getId());
    noPermissionPage.setWikiType(wiki.getType());
    noPermissionPage.setWikiOwner(wiki.getOwner());
    noPermissionPage.setName("page1");
    noPermissionPage.setTitle("Page 1");
    noPermissionPage.setPermissions(new ArrayList<>());
    noPermissionPage = storage.createPage(wiki, wiki.getWikiHome(), noPermissionPage);

    Page systemPermissionPage = new Page();
    systemPermissionPage.setWikiId(wiki.getId());
    systemPermissionPage.setWikiType(wiki.getType());
    systemPermissionPage.setWikiOwner(wiki.getOwner());
    systemPermissionPage.setName("page1");
    systemPermissionPage.setTitle("Page 1");
    systemPermissionPage.setOwner(IdentityConstants.SYSTEM);
    systemPermissionPage.setPermissions(new ArrayList<>());
    Page storedSystemPermissionPage = storage.createPage(wiki, wiki.getWikiHome(), systemPermissionPage);

    assertTrue(noteService.hasPermissionOnPage(noPermissionPage, PermissionType.VIEWPAGE, adminAclIdentity));
    assertFalse(noteService.hasPermissionOnPage(storedSystemPermissionPage, PermissionType.VIEWPAGE, adminAclIdentity));

    assertNotNull(noteService.getNoteById(storedSystemPermissionPage.getId(), adminAclIdentity));
    assertThrows(IllegalAccessException.class,
                 () -> noteService.getNoteById(storedSystemPermissionPage.getId(), userAclIdentity));
  }

  public void testUpdateNote() throws WikiException, IllegalAccessException, Exception {
    Identity root = new Identity("root");
    // Get Home
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic").getWikiHome();

    // Create a wiki page for test
    Page page = new Page("new page_", "new page_");
    page.setContent("Page content");
    page = noteService.createNote(new Wiki(PortalConfig.PORTAL_TYPE, "classic"), "Home", page,root);
    assertNotNull(page);
    assertEquals("Page content", page.getContent());
    assertEquals("new page_", page.getTitle());

    // update content of page
    page.setContent("Page content updated_");
    DraftPage draftPage = new DraftPage();
    draftPage.setContent("test");
    draftPage.setTitle("test");
    noteService.createDraftForExistPage(draftPage, page, "", new Date().getTime(), "root");
    noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT,root);
    assertNotNull(page);
    assertEquals("Page content updated_", page.getContent());

    // update title of page
    page.setTitle("new page updated_");
    noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT,root);
    assertNotNull(page);
    assertEquals("new page updated_", page.getTitle());
  }

  public void testDraftPage() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");

    // Test create draft for new page
    DraftPage draftOfNewPage = new DraftPage();
    draftOfNewPage.setTitle("Draft page");
    draftOfNewPage.setContent("Draft page content");
    long now = new Date().getTime();
    draftOfNewPage = noteService.createDraftForNewPage(new DraftPage(), now, 1L);
    assertNotNull(draftOfNewPage);
    String draftNameForNewPage = draftOfNewPage.getName();
    assertTrue(draftOfNewPage.isNewPage());
    assertTrue(draftOfNewPage.isDraftPage());
    assertEquals("Untitled_" + getDraftNameSuffix(now), draftNameForNewPage);

    // Create a wiki page for test
    String pageName = "new page 10";
    Page targetPage = new Page(pageName, pageName);
    targetPage.setContent("Page content");
    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");
    targetPage = noteService.createNote(userWiki, "Home", new Page("TestPage", "TestPage"), root);

    // Test create draft for existing page
    DraftPage draftPageTosave = new DraftPage();
    String draftTitle = targetPage.getTitle() + "_draft";
    String draftContent = targetPage.getContent() + "_draft";
    draftPageTosave.setTitle(draftTitle);
    draftPageTosave.setContent(draftContent);
    String draftName = targetPage.getName() + "_" + getDraftNameSuffix(now);
    DraftPage draftOfExistingPage = noteService.createDraftForExistPage(draftPageTosave, targetPage, null, now, root.getUserId());
    assertNotNull(draftOfExistingPage);
    assertFalse(draftOfExistingPage.isNewPage());
    assertEquals(draftOfExistingPage.getName(), draftName);
    assertEquals(targetPage.getId(), draftOfExistingPage.getTargetPageId());
    assertEquals("1", draftOfExistingPage.getTargetPageRevision());

    //Test Update draft page
    draftOfNewPage.setTitle("Draft page updated");
    draftOfNewPage.setContent("Draft page content updated");
    DraftPage updatedDraftOfNewPage = noteService.updateDraftForNewPage(draftOfNewPage, now, 1L);
    assertNotNull(updatedDraftOfNewPage);
    assertEquals(updatedDraftOfNewPage.getTitle(), draftOfNewPage.getTitle());
    assertEquals(updatedDraftOfNewPage.getContent(), draftOfNewPage.getContent());
    assertTrue(updatedDraftOfNewPage.isNewPage());
    assertTrue(updatedDraftOfNewPage.isDraftPage());

    //Test Update draft of existing page
    draftOfExistingPage.setTitle("Draft of page");
    draftOfExistingPage.setContent("Draft of page updated");
    DraftPage updatedDraftOfExistingPage = noteService.updateDraftForExistPage(draftOfExistingPage, targetPage, null, now, root.getUserId());
    assertNotNull(updatedDraftOfExistingPage);
    assertEquals(updatedDraftOfExistingPage.getTitle(), draftOfExistingPage.getTitle());
    assertEquals(updatedDraftOfExistingPage.getContent(), draftOfExistingPage.getContent());
    assertFalse(updatedDraftOfExistingPage.isNewPage());
    assertTrue(updatedDraftOfExistingPage.isDraftPage());

  }

  private String getDraftNameSuffix(long clientTime) {
    return new SimpleDateFormat("yyyyMMddHHmmssSSS").format(new Date(clientTime));
  }

  public void testGEtNoteById() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("exported1", "exported1"),root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", note1.getName())) ;

    Page note = noteService.getNoteById(note1.getId(),root,"");

    assertNotNull(note);
    assertEquals(note.getName(),note1.getName());

    assertFalse(note.isDeleted());

    noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), note.getName());
    Page deletedNote = noteService.getNoteById(note1.getId(),root,"");

    assertNotNull(deletedNote);
    assertTrue(deletedNote.isDeleted());
  }

  public void testGetNoteByIdAndLang() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("testPage", "testPage"), root);
    note1.setLang("en");
    noteService.createVersionOfNote(note1, "root");

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", note1.getName()));

    Page note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "en");

    assertNotNull(note);
    assertEquals(note.getName(), note1.getName());

    assertFalse(note.isDeleted());

    noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), note.getName());
    Page deletedNote = noteService.getNoteById(note1.getId(), root, "");

    assertNotNull(deletedNote);
    assertTrue(deletedNote.isDeleted());
  }

  public void testGetPageAvailableTranslationLanguages() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testLang", "testLang"), root) ;
    note.setLang("ar");
    noteService.createVersionOfNote(note, "root");
    noteService.createVersionOfNote(note, "root");
    note.setLang("en");
    noteService.createVersionOfNote(note, "root");
    note.setLang("fr");
    noteService.createVersionOfNote(note, "root");

    List<String> langs = noteService.getPageAvailableTranslationLanguages(Long.valueOf(note.getId()), false);

    assertNotNull(langs);
    assertEquals(3, langs.size());
  }

  public void testGetVersionsHistoryOfNoteByLang() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testVersionHistory", "testVersionHistory"), root) ;
    List<PageHistory> pageHistories = noteService.getVersionsHistoryOfNoteByLang(note, "root", null);
    assertEquals(1, pageHistories.size());
    assertNull(pageHistories.get(0).getLang());
    note.setLang("ar");
    noteService.createVersionOfNote(note, "root");
    pageHistories = noteService.getVersionsHistoryOfNoteByLang(note, "root", "ar");
    assertEquals(1, pageHistories.size());
    assertNotNull(pageHistories.get(0).getLang());
    assertEquals("ar", pageHistories.get(0).getLang());
    pageHistories = noteService.getVersionsHistoryOfNoteByLang(note, "root", "en");
    assertEquals(0, pageHistories.size());
  }

  public void testGetLatestDraftPageByUserAndTargetPageAndLang() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testLatestDraft", "testLatestDraft"), root) ;
    DraftPage draftPage = new DraftPage();
    draftPage.setTitle("test draft");
    draftPage.setContent(note.getContent());
    draftPage.setName("test draft");
    draftPage.setTargetPageId(note.getId());
    noteService.createDraftForExistPage(draftPage, note, null, new Date().getTime(), "root");
    DraftPage latestDraft = noteService.getLatestDraftPageByTargetPageAndLang(Long.valueOf(note.getId()), null);
    assertNotNull(latestDraft);
  }

  public void testGetNoteOfNoteBookByName() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("test1", "test1"),root) ;

    assertNotNull(note1) ;
  }

  public void testExportNotes() throws Exception {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "exportPortal");
    Page page1 = new Page("exported1", "exported1");
    page1.setContent("<figure class=\"image\"><img src=\"/portal/rest/wiki/attachments/group/space//spaces/base_de_connaissances/page/4.01-_Profil_et_paramètres/Navigation paramètres.png\"></figure>");
    Page note1 = noteService.createNote(portalWiki, "Home", page1, root);

    Page page2 = new Page("exported2", "exported2");
    page2.setContent("<a class=\"noteLink\" href=\"exported1\" target=\"_blank\">Règles de rédaction des tutoriels </a>");
    Page note2 = noteService.createNote(portalWiki, "Home", page2, root);

    Page page3 = new Page("exported3", "exported3");
    page3.setContent("<a class=\"noteLink\" href=\"" + note2.getId() + "\">Home</a>");
    Page note3 = noteService.createNote(portalWiki, "Home", page3, root);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", note1.getName()));
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", note2.getName()));
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", note3.getName()));

    String[] notes = new String[3];
    notes[0] = note1.getId();
    notes[1] = note2.getId();
    notes[2] = note3.getId();

    notesExportService.startExportNotes(200231, notes, true, root);
    boolean exportDone = false;
    while (!exportDone) {
      if (notesExportService.getStatus(200231).getStatus().equals("ZIP_CREATED")) {
        exportDone = true;
      }
    }
    byte[] exportedNotes = notesExportService.getExportedNotes(200231);
    assertNotNull(exportedNotes);
  }

  public void testImportNotes() throws Exception {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "importPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported1", "to_be_imported1"),user) ;
    Page note2 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported2", "to_be_imported2"),user) ;
    Page note3 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported3", "to_be_imported3"),user) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", note1.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", note2.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", note3.getName())) ;

    String[] notes = new String[3];
    notes[0] = note1.getId();
    notes[1] = note2.getId();
    notes[2] = note3.getId();
    File zipFile = File.createTempFile("notesExport", ".zip");
    notesExportService.startExportNotes(200231, notes, true, user);
    boolean exportDone= false;
    while (!exportDone){
      if(notesExportService.getStatus(200231).getStatus().equals("ZIP_CREATED")){
        exportDone = true;
      }
    }
    byte[] exportedNotes = notesExportService.getExportedNotes(200231);
    assertNotNull(exportedNotes);
    FileUtils.writeByteArrayToFile(zipFile,exportedNotes);

    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");

    int childern = noteService.getChildrenNoteOf(userWiki.getWikiHome(), false, false).size();
    noteService.importNotes(zipFile.getPath(), userWiki.getWikiHome(), "update", user);
    assertTrue(zipFile.delete());
    assertEquals(noteService.getChildrenNoteOf(userWiki.getWikiHome(), false, false).size(), childern+3);
  }

  public void testGetNotesOfWiki() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal1");
    Page toBeImported1NotPage = noteService.createNote(portalWiki, "Home", new Page("to_be_imported1", "to_be_imported1"),user) ;
    Page toBeImported2NotPage = noteService.createNote(portalWiki, "Home", new Page("to_be_imported2", "to_be_imported2"),user) ;
    Page toBeImported3NotPage = noteService.createNote(portalWiki, "Home", new Page("to_be_imported3", "to_be_imported3"),user) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", toBeImported1NotPage.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", toBeImported2NotPage.getName())) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", toBeImported3NotPage.getName())) ;

    List<Page> pages = noteService.getNotesOfWiki(portalWiki.getType(),portalWiki.getOwner());

    assertEquals(pages.size(),4);
  }

  public void testDeleteNote1() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal2");
    Page notePage = noteService.createNote(portalWiki, "Home", new Page("note1", "note1"), user);
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal2", notePage.getName()));
    noteService.deleteNote(PortalConfig.PORTAL_TYPE, "testPortal2", notePage.getName(), user);

    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal2", notePage.getName()));
  }

  public void testDeleteVersionsByNoteIdAndLang() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("testPage1", "testPage1"), root);
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", note1.getName()));
    note1 = noteService.getNoteById(note1.getId());
    note1.setLang("en");
    note1.setTitle("englishTitle");
    noteService.createVersionOfNote(note1, "root");
    note1.setLang("fr");
    note1.setTitle("frenchTitle");
    noteService.createVersionOfNote(note1, "root");
    noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(note1.getId()), "en");
    Page note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "en");
    assertEquals(note.getTitle(), "testPage1");
    note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "fr");
    assertNotNull(note);
    assertEquals(note.getTitle(), "frenchTitle");
    noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(note.getId()), "fr");
    note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "fr");
    assertEquals(note.getTitle(), "testPage1");
    noteService.deleteNote(note1.getWikiType(), note1.getWikiOwner(), note1.getName());
    Page deletedNote = noteService.getNoteById(note1.getId(), root, "");

    assertNotNull(deletedNote);
    assertTrue(deletedNote.isDeleted());
  }

  public void testGetChildrenNoteOf() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "importPortal");
    noteService.createNote(portalWiki, "Home", new Page("imported1", "imported1"),user) ;
    noteService.createNote(portalWiki, "Home", new Page("imported2", "imported2"),user) ;
    Page home = portalWiki.getWikiHome();
    int childern = noteService.getChildrenNoteOf(home, false, false).size();
    NoteToExport note = new NoteToExport();
    note.setId(home.getId());
    note.setName(home.getName());
    note.setTitle(home.getTitle());
    note.setWikiId(home.getWikiId());
    note.setWikiOwner(home.getWikiOwner());
    note.setWikiType(home.getWikiType());
    int eXportCildren= noteService.getChildrenNoteOf(note).size();
    assertEquals(eXportCildren,childern);
  }

  public void testRemoveDraftOfNote() throws Exception {
    Identity root = new Identity("root");
    startSessionAs("root");
    long now = new Date().getTime();
    // Create a wiki page for test
    String pageName = "new page 10";
    Page targetPage = new Page(pageName, pageName);
    targetPage.setContent("Page content");
    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");
    targetPage = noteService.createNote(userWiki, "Home", new Page("TestPage1", "TestPage1"), root);
    DraftPage draftPageTosave = new DraftPage();
    String draftTitle = targetPage.getTitle() + "_draft";
    String draftContent = targetPage.getContent() + "_draft";
    draftPageTosave.setTitle(draftTitle);
    draftPageTosave.setContent(draftContent);
    draftPageTosave.setLang("fr");
    String draftName = targetPage.getName() + "_" + getDraftNameSuffix(now);
    DraftPage draftOfExistingPage = noteService.createDraftForExistPage(draftPageTosave, targetPage, null, now, root.getUserId());
    assertNotNull(draftOfExistingPage);
    assertFalse(draftOfExistingPage.isNewPage());
    assertEquals(draftOfExistingPage.getName(), draftName);
    assertEquals(targetPage.getId(), draftOfExistingPage.getTargetPageId());
    assertEquals("1", draftOfExistingPage.getTargetPageRevision());

    DraftPage draft = noteService.getLatestDraftOfPage(targetPage);
    assertEquals(draft.getId(), draftOfExistingPage.getId());
    WikiPageParams noteParams = new WikiPageParams(targetPage.getWikiType(), targetPage.getWikiOwner(), targetPage.getName());
    noteService.removeDraftOfNote(noteParams,"en");

    draft = noteService.getLatestDraftOfPage(targetPage);
    assertEquals(draft.getId(), draftOfExistingPage.getId());

    noteService.removeDraftOfNote(noteParams,"fr");
    draft = noteService.getLatestDraftOfPage(targetPage);
    assertNull(draft);
  }

  public void testGetNoteOfNoteBookByNameWithLangMetadata() throws WikiException, IllegalAccessException {
    Identity user = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testNoteLangMetadata", "testNoteLangMetadata"), user);
    assertNotNull(noteService.getNoteOfNoteBookByName(note.getWikiType(), note.getWikiOwner(), note.getName(), "en", user));
  }

  public void testGetPublishedVersionByPageIdAndLang() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testNotee", "testNotee"), user);
    assertNull(noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(note.getId()), "en"));
    note.setLang("en");
    note.setTitle("english title");
    note.setContent("english content");
    noteService.createVersionOfNote(note, user.getUserId());
    assertNotNull(noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(note.getId()), "en"));
  }

  public void testRemoveOrphanDraftPagesByParentPage() throws Exception {
    startSessionAs("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.GROUP_TYPE, "/platform/users");
    Page homePage = noteService.getNoteById(portalWiki.getWikiHome().getId());
    DraftPage draft = new DraftPage();
    draft.setParentPageId(homePage.getId());
    draft.setTargetPageId(null);
    draft = noteService.createDraftForNewPage(draft, new Date().getTime(), 1L);
    assertNotNull(draft);
    noteService.removeOrphanDraftPagesByParentPage(Long.parseLong(homePage.getId()));
    persist();
    assertNull(noteService.getDraftNoteById(draft.getId(), "root"));
  }
  
  private Page createTestNoteWithVersionLang(String name, String lang, Identity user) throws Exception {
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page(name, name), user);
    note.setLang(lang);
    note.setTitle("language title");
    note.setContent("language content");
    noteService.createVersionOfNote(note, user.getUserId());
    return note;
  } 

  private void bindMockedUploadService() throws Exception {
    UploadService uploadService = mock(UploadService.class);
    UploadResource uploadResource = mock(UploadResource.class);
    when(uploadResource.getUploadedSize()).thenReturn(12548d);
    when(uploadService.getUploadResource(anyString())).thenReturn(uploadResource);
    String location = getClass().getResource("/images/John.png").getPath();
    when(uploadResource.getStoreLocation()).thenReturn(location);
    Field field = noteService.getClass().getDeclaredField("uploadService");
    field.setAccessible(true);
    field.set(noteService, uploadService);
  }

  private NotePageProperties createNotePageProperties(long noteId, String altText, String summary) {
    NotePageProperties notePageProperties = new NotePageProperties();
    NoteFeaturedImage featuredImage = new NoteFeaturedImage();
    featuredImage.setMimeType("image/png");
    featuredImage.setUploadId("123");
    featuredImage.setAltText(altText);
    notePageProperties.setFeaturedImage(featuredImage);
    notePageProperties.setNoteId(noteId);
    notePageProperties.setSummary(summary);
    return notePageProperties;
  }

  public void testSaveNoteFeaturedImage() throws Exception {
    Identity user = new Identity("user");
    Page note = createTestNoteWithVersionLang("testMetadata", "en", user);
    
    this.bindMockedUploadService();

    NotePageProperties notePageProperties = createNotePageProperties(Long.parseLong(note.getId()), "alt text", "summary test");
    NotePageProperties properties = noteService.saveNoteMetadata(notePageProperties, null, 1L);
    assertEquals("summary test", properties.getSummary());

    notePageProperties.setSummary("version language summary");
    properties = noteService.saveNoteMetadata(notePageProperties, "en", 1L);
    assertEquals("version language summary", properties.getSummary());
  }
  
  public void testRemoveNoteFeaturedImage() throws Exception {
    Identity user = new Identity("user");
    Page note = createTestNoteWithVersionLang("testMetadataRemove", "fr", user);

    this.bindMockedUploadService();

    NotePageProperties notePageProperties = createNotePageProperties(Long.parseLong(note.getId()), "alt text", "summary test");
    NotePageProperties properties = noteService.saveNoteMetadata(notePageProperties, null, 1L);
    noteService.saveNoteMetadata(notePageProperties, "fr", 1L);

    assertNotNull(properties.getFeaturedImage().getId());
    assertNotNull(noteService.getNoteFeaturedImageInfo(Long.parseLong(note.getId()), null, false, null, 1L));

    noteService.removeNoteFeaturedImage(Long.parseLong(note.getId()),
                                        properties.getFeaturedImage().getId(),
                                        null,
                                        false,
                                        1L);

    NoteFeaturedImage savedFeaturedImage = noteService.getNoteFeaturedImageInfo(Long.parseLong(note.getId()),
                                                                                null,
                                                                                false,
                                                                                null,
                                                                                1L);
    assertNull(savedFeaturedImage);

    assertNotNull(noteService.getNoteFeaturedImageInfo(Long.parseLong(note.getId()), "fr", false, null, 1L));
  }

  public void testGetNoteFeaturedImageInfo() throws Exception {
    Identity user = new Identity("user");
    Page note = createTestNoteWithVersionLang("testGetMetadataInfo", "ar", user);

    this.bindMockedUploadService();

    NotePageProperties notePageProperties = createNotePageProperties(Long.parseLong(note.getId()), "alt text", "summary Test");
    noteService.saveNoteMetadata(notePageProperties, null, 1L);
    notePageProperties = createNotePageProperties(Long.parseLong(note.getId()), "alt text", "summary Test");
    noteService.saveNoteMetadata(notePageProperties, "ar", 1L);
    NoteFeaturedImage featuredImage = noteService.getNoteFeaturedImageInfo(Long.parseLong(note.getId()), null, false, "150x150", 1L);
    NoteFeaturedImage versionLanguageFeaturedImage = noteService.getNoteFeaturedImageInfo(Long.parseLong(note.getId()), "ar", false, "150x150", 1L);

    assertNotNull(featuredImage);
    assertTrue(featuredImage.getLastUpdated() > 0L);
    assertNotSame(featuredImage.getId(), versionLanguageFeaturedImage.getId());
  }

  public void testCreatePageWithProperties() throws Exception {
    Identity user = new Identity("user");
    this.bindMockedUploadService();
    NotePageProperties notePageProperties = createNotePageProperties(0L, "alt text", "summary Test");
    DraftPage draftPage = new DraftPage();
    draftPage.setTitle("test");
    draftPage.setContent("test");
    draftPage.setProperties(notePageProperties);
    draftPage = noteService.createDraftForNewPage(draftPage, new Date().getTime(), 1L);
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");

    // case save properties of new page from new page draft
    Page page = new Page();
    page.setTitle("testSaveProperties1");
    page.setName("testSaveProperties1");
    page.setContent("test");
    page.setProperties(draftPage.getProperties());
    Page note = noteService.createNote(portalWiki, "Home", page , user);
    assertNotNull(note);
    assertNotNull(note.getProperties());

    notePageProperties.setFeaturedImage(null);
    page.setTitle("testSaveProperties2");
    page.setName("testSaveProperties2");
    page.setProperties(notePageProperties);
    note = noteService.createNote(portalWiki, "Home", page , user);
    assertNotNull(note);
    assertNotNull(note.getProperties());
  }

  public void testCreateDraftForNewPageWithProperties() throws Exception {
    Identity user = new Identity("user");
    this.bindMockedUploadService();
    NotePageProperties notePageProperties = createNotePageProperties(0L, "alt text", "summary Test");
    DraftPage draftPage = new DraftPage();
    draftPage.setTitle("test");
    draftPage.setContent("test");
    draftPage.setProperties(notePageProperties);
    draftPage = noteService.createDraftForNewPage(draftPage, new Date().getTime(), 1L);
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    assertNotNull(draftPage);
    assertNotNull(draftPage.getProperties());
  }

  public void testCreateDraftForExistPageWithProperties() throws Exception {
    Identity user = new Identity("user");
    this.bindMockedUploadService();
    NotePageProperties notePageProperties = createNotePageProperties(0L, "alt text", "summary Test");
    DraftPage draftPage = new DraftPage();
    draftPage.setTitle("test");
    draftPage.setContent("test");
    draftPage.setProperties(notePageProperties);
    Page page = new Page();
    page.setTitle("testSaveProperties3");
    page.setName("testSaveProperties3");
    page.setContent("test");
    page.setProperties(draftPage.getProperties());
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", page , user);
    // Case copy from target page
    draftPage = noteService.createDraftForExistPage(draftPage, note, null, new Date().getTime(), "root");
    assertNotNull(draftPage);
    assertNotNull(draftPage.getProperties());

    // Case save from the draft page
    draftPage.setId("0");
    notePageProperties.getFeaturedImage().setId(0L);
    draftPage.setProperties(notePageProperties);
    draftPage = noteService.createDraftForExistPage(draftPage, note, null, new Date().getTime(), "root");
    assertNotNull(draftPage);
    assertNotNull(draftPage.getProperties());
  }

  public void testGetVersionById() throws Exception {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note = noteService.createNote(portalWiki, "Home", new Page("testGetVersionById", "testGetVersionById"), user);
    note.setLang("en");
    note.setTitle("english title");
    note.setContent("english content");
    noteService.createVersionOfNote(note, user.getUserId());
    PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.valueOf(note.getId()), "en");
    assertNotNull(noteService.getPageVersionById(Long.valueOf(pageVersion.getId())));
  }
}
