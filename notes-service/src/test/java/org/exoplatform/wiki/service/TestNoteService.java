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


import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.PortalContainer;


import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.jpa.JPADataStorage;
import org.exoplatform.wiki.model.*;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.CMSService;

import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

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
    notesExportService = getContainer().getComponentInstanceOfType(NotesExportService.class) ;
    getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
  }
  

  public void testGetGroupPageById() throws WikiException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.GROUP_TYPE, "/platform/users");
    Identity root = new Identity("root");

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", "Home")) ;

    try {
      noteService.createNote(wiki, "Home", new Page("testGetGroupPageById-101", "testGetGroupPageById-101"),root);
    } catch (IllegalAccessException e) {
      Assert.fail("Current user don't have needed permissions to create the note");
    }

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "platform/users", "testGetGroupPageById-101")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.GROUP_TYPE, "unknown", "Home"));
  }

  public void testGetUserPageById() throws WikiException, IllegalAccessException {
    Wiki wiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "john");
    Identity john = new Identity("john");
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", "Home")) ;

    noteService.createNote(wiki, "Home", new Page("testGetUserPageById-101", "testGetUserPageById-101"), john);

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "john", "testGetUserPageById-101")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.USER_TYPE, "unknown", "Home"));
  }

  public void testCreatePageAndSubNote() throws WikiException, IllegalAccessException {
    Wiki wiki = new Wiki(PortalConfig.PORTAL_TYPE, "classic");
    Identity root = new Identity("root");
    noteService.createNote(wiki, "Home", new Page("parentPage_", "parentPage_"), root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "parentPage_", root)) ;
    noteService.createNote(wiki, "parentPage_", new Page("childPage_", "childPage_"),root) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "childPage_", root)) ;
  }

  public void testGetBreadcumb() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "Home", new Page("Breadcumb1_", "Breadcumb1_"),root) ;
    noteService.createNote(portalWiki, "Breadcumb1_", new Page("Breadcumb2_", "Breadcumb2_"),root) ;
    noteService.createNote(portalWiki, "Breadcumb2_", new Page("Breadcumb3_", "Breadcumb3_"),root) ;
    List<BreadcrumbData> breadCumbs = noteService.getBreadCrumb(PortalConfig.PORTAL_TYPE, "classic", "Breadcumb3_", false);
    assertEquals(4, breadCumbs.size());
    assertEquals("Home", breadCumbs.get(0).getId());
    assertEquals("Breadcumb1_", breadCumbs.get(1).getId());
    assertEquals("Breadcumb2_", breadCumbs.get(2).getId());
    assertEquals("Breadcumb3_", breadCumbs.get(3).getId());
  }

  public void testMoveNote() throws WikiException, IllegalAccessException {
    //moving page in same space
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "Home", new Page("oldParent_", "oldParent_"),root) ;
    noteService.createNote(portalWiki, "oldParent_", new Page("child_", "child_"),root) ;
    noteService.createNote(portalWiki, "Home", new Page("newParent_", "newParent_"),root) ;

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
    noteService.createNote(userWiki, "Home", new Page("acmePage_", "acmePage_"),root) ;
    noteService.createNote(portalWiki, "Home", new Page("classicPage_", "classicPage_"),root) ;

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
    noteService.createNote(demoWiki, "Home", new Page("toMovedPage_", "toMovedPage_"),root);
    Page page = noteService.createNote(userWiki, "Home", new Page("privatePage_", "privatePage_"),root);
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
    noteService.createNote(portalWiki, "Home", new Page("deletePage_", "deletePage_"),root) ;
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    //wait(10) ;
    noteService.createNote(portalWiki, "Home", new Page("deletePage_", "deletePage_"),root) ;
    assertTrue(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "deletePage_")) ;
    assertFalse(noteService.deleteNote(PortalConfig.PORTAL_TYPE, "classic", "Home")) ;
  }


  public void testRenameNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "Home", new Page("currentPage_", "currentPage_"),root) ;
    assertTrue(noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", "currentPage_", "renamedPage_", "renamedPage_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "renamedPage_")) ;
  }

  public void testRenamePageToExistingNote() throws WikiException, IllegalAccessException  {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "classic");
    noteService.createNote(portalWiki, "Home", new Page("currentPage_", "currentPage_"),root) ;
    noteService.createNote(portalWiki, "Home", new Page("currentPage2_", "currentPage2_"),root) ;
    try {
      noteService.renameNote(PortalConfig.PORTAL_TYPE, "classic", "currentPage_", "currentPage2_", "renamedPage2_");
      fail("Renaming page currentPage to the existing page currentPage2_ should throw an exception");
    } catch (WikiException e) {
      assertEquals("Note portal:classic:currentPage2_ already exists, cannot rename it.", e.getMessage());
    }
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "currentPage_")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "classic", "currentPage2_")) ;
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

  public void testUpdateNote() throws WikiException, IllegalAccessException {
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
    draftOfNewPage = noteService.createDraftForNewPage(new DraftPage(), now);
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
    WikiPageParams param = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", targetPage.getName());
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
    DraftPage updatedDraftOfNewPage = noteService.updateDraftForNewPage(draftOfNewPage, now);
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

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", "exported1")) ;

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
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("testPage", "testPage"), root) ;
    note1.setLang("en");
    noteService.createVersionOfNote(note1, "root");

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", "testPage")) ;

    Page note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()),root,"", "en");

    assertNotNull(note);
    assertEquals(note.getName(),note1.getName());

    assertFalse(note.isDeleted());

    noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), note.getName());
    Page deletedNote = noteService.getNoteById(note1.getId(),root,"");

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

    List<String> langs = noteService.getPageAvailableTranslationLanguages(Long.valueOf(note.getId()), "root", false);

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
    DraftPage latestDraft = noteService.getLatestDraftPageByUserAndTargetPageAndLang(Long.valueOf(note.getId()), "root", null);
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
    Page note1 = noteService.createNote(portalWiki, "Home",page1,root) ;

    Page page2 = new Page("exported2", "exported2");
    page2.setContent("<a class=\"noteLink\" href=\"exported1\" target=\"_blank\">Règles de rédaction des tutoriels </a>");
    Page note2 = noteService.createNote(portalWiki, "Home",page2,root) ;


    Page page3 = new Page("exported3", "exported3");
    page3.setContent("<a class=\"noteLink\" href=\""+note2.getId()+"\">Home</a>");
    Page note3 = noteService.createNote(portalWiki, "Home",page3,root) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", "exported1")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", "exported2")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "exportPortal", "exported3")) ;

    String[] notes = new String[3];
    notes[0] = note1.getId();
    notes[1] = note2.getId();
    notes[2] = note3.getId();

    notesExportService.startExportNotes(200231, notes, true, root);
    boolean exportDone= false;
    while (!exportDone){
      if(notesExportService.getStatus(200231).getStatus().equals("ZIP_CREATED")){
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

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", "to_be_imported1")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", "to_be_imported2")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "importPortal", "to_be_imported3")) ;

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

    int childern = noteService.getChildrenNoteOf(userWiki.getWikiHome(),"root" , false, false).size();
    noteService.importNotes(zipFile.getPath(), userWiki.getWikiHome(), "update", user);
    assertTrue(zipFile.delete());
    assertEquals(noteService.getChildrenNoteOf(userWiki.getWikiHome(),"root",false, false).size(),childern+3);
  }

  public void testGetNotesOfWiki() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal1");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported1", "to_be_imported1"),user) ;
    Page note2 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported2", "to_be_imported2"),user) ;
    Page note3 = noteService.createNote(portalWiki, "Home", new Page("to_be_imported3", "to_be_imported3"),user) ;

    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", "to_be_imported1")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", "to_be_imported2")) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal1", "to_be_imported3")) ;

    List<Page> pages = noteService.getNotesOfWiki(portalWiki.getType(),portalWiki.getOwner());

    assertEquals(pages.size(),4);
  }

  public void testDeleteNote1() throws WikiException, IllegalAccessException {
    Identity user = new Identity("user");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal2");
    noteService.createNote(portalWiki, "Home", new Page("note1", "note1"),user) ;
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal2", "note1")) ;
    noteService.deleteNote(PortalConfig.PORTAL_TYPE, "testPortal2", "note1",user);

    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal2", "note1")) ;
  }

  public void testDeleteVersionsByNoteIdAndLang() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    Wiki portalWiki = getOrCreateWiki(wService, PortalConfig.PORTAL_TYPE, "testPortal");
    Page note1 = noteService.createNote(portalWiki, "Home", new Page("testPage1", "testPage1"), root);
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, "testPortal", "testPage1"));
    note1 = noteService.getNoteById(note1.getId());
    note1.setLang("en");
    note1.setTitle("englishTitle");
    noteService.createVersionOfNote(note1, "root");
    note1.setLang("fr");
    note1.setTitle("frenchTitle");
    noteService.createVersionOfNote(note1, "root");
    noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(note1.getId()), "root", "en");
    Page note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "en");
    assertEquals(note.getTitle(), "testPage1");
    note = noteService.getNoteByIdAndLang(Long.valueOf(note1.getId()), root, "", "fr");
    assertNotNull(note);
    assertEquals(note.getTitle(), "frenchTitle");
    noteService.deleteVersionsByNoteIdAndLang(Long.valueOf(note.getId()), "root", "fr");
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
    int childern = noteService.getChildrenNoteOf(home,user.getUserId() ,false, false).size();
     NoteToExport note = new NoteToExport();
     note.setId(home.getId());
     note.setName(home.getName());
     note.setTitle(home.getTitle());
     note.setWikiId(home.getWikiId());
     note.setWikiOwner(home.getWikiOwner());
     note.setWikiType(home.getWikiType());
    int eXportCildren= noteService.getChildrenNoteOf(note, user.getUserId()).size();
    assertEquals(eXportCildren,childern);
  }

  public void testRemoveDraftOfNote() throws WikiException, IllegalAccessException {
    Identity root = new Identity("root");
    startSessionAs("root");
    long now = new Date().getTime();
    // Create a wiki page for test
    String pageName = "new page 10";
    Page targetPage = new Page(pageName, pageName);
    targetPage.setContent("Page content");
    Wiki userWiki = getOrCreateWiki(wService, PortalConfig.USER_TYPE, "root");
    targetPage = noteService.createNote(userWiki, "Home", new Page("TestPage1", "TestPage1"), root);
    WikiPageParams param = new WikiPageParams(PortalConfig.PORTAL_TYPE, "classic", targetPage.getName());
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

    DraftPage draft = noteService.getLatestDraftOfPage(targetPage,root.getUserId());
    assertEquals(draft.getId(), draftOfExistingPage.getId());
    WikiPageParams noteParams = new WikiPageParams(targetPage.getWikiType(), targetPage.getWikiOwner(), targetPage.getName());
    noteService.removeDraftOfNote(noteParams,"en");

    draft = noteService.getLatestDraftOfPage(targetPage,root.getUserId());
    assertEquals(draft.getId(), draftOfExistingPage.getId());

    noteService.removeDraftOfNote(noteParams,"fr");
    draft = noteService.getLatestDraftOfPage(targetPage,root.getUserId());
    assertNull(draft);
  }

}
