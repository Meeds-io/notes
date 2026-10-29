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
package io.meeds.notes.service;

import static org.junit.Assert.assertThrows;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.plugin.WikiPageAttachmentPlugin;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageData;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.cms.service.CMSServiceImpl;

import lombok.SneakyThrows;

public class NotePageViewServiceTest extends BaseTest { // NOSONAR

  private static final String PAGE_NAME_PREFIX = "pageName";

  private static final String   PAGE_NOTE_NAME_PREFIX = "pageNoteName";

  protected static final Random RANDOM                = new Random();

  private static final long     USER_IDENTITY_ID      = 5l;

  private static final String   USERS_GROUP           = "*:/platform/users";

  private static final String   ADMINISTRATORS_GROUP  = "*:/platform/administrators";

  private static final String   USERNAME              = "testUser";

  private static final String   INTERNAL_USERNAME     = "testInternalUser";

  private NoteService           noteService;

  private NotePageViewService   notePageViewService;

  private CMSService            cmsService;

  private LayoutService         layoutService;

  private IdentityRegistry      identityRegistry;

  private OrganizationService  organizationService;

  private IdentityManager       identityManager;

  private AttachmentService     attachmentService;

  private FileService           fileService;

  private WikiService           wikiService;

  private MetadataService       metadataService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.notePageViewService = getContainer().getComponentInstanceOfType(NotePageViewService.class);
    this.cmsService = getContainer().getComponentInstanceOfType(CMSServiceImpl.class);
    this.layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    this.identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
    this.organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    this.noteService = getContainer().getComponentInstanceOfType(NoteService.class);
    this.identityManager = getContainer().getComponentInstanceOfType(IdentityManager.class);
    this.attachmentService = getContainer().getComponentInstanceOfType(AttachmentService.class);
    this.fileService = getContainer().getComponentInstanceOfType(FileService.class);
    this.wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
    this.metadataService = getContainer().getComponentInstanceOfType(MetadataService.class);
  }

  /**
   * An import of a window's data.init writes every language as the user and
   * attaches to the target, as the user, only the named files attached to a
   * note the user can read, leaving the other links as named. The mutants
   * that clone every named file, none, or as the super user fail it.
   */
  public void testImportNotePageDataClonesTheReadableAttachments() throws Exception {
    String sourceName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String targetName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String restrictedTargetName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, USERS_GROUP);
    String restrictedPageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, sourceName, pageReference, 0l, USER_IDENTITY_ID);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, targetName, pageReference, 0l, USER_IDENTITY_ID);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, restrictedTargetName, restrictedPageReference, 0l, USER_IDENTITY_ID);
    createUser(USERNAME);
    createUser(INTERNAL_USERNAME);
    org.exoplatform.services.security.Identity administrator = registerAdministratorUser(USERNAME);
    org.exoplatform.services.security.Identity internalUser = registerInternalUser(INTERNAL_USERNAME);
    long adminIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(USERNAME).getId());

    String readableId = notePageViewService.saveNotePage(sourceName, "source", null, administrator).getId();
    String readableFileId = writeFile();
    attachmentService.createAttachment(readableFileId, WikiPageAttachmentPlugin.OBJECT_TYPE, readableId, null, adminIdentityId, Collections.emptyMap());
    Wiki personalWiki = getOrCreateWiki(wikiService, "user", USERNAME);
    Page personalNote = new Page("personal" + RANDOM.nextLong(), "personal");
    personalNote.setContent("private");
    personalNote.setOwner(USERNAME);
    String restrictedId = noteService.createNote(personalWiki, personalWiki.getWikiHome(), personalNote).getId();
    String restrictedFileId = writeFile();
    attachmentService.createAttachment(restrictedFileId, WikiPageAttachmentPlugin.OBJECT_TYPE, restrictedId, null, adminIdentityId, Collections.emptyMap());
    assertTrue(noteService.canViewNote(noteService.getNoteById(readableId), INTERNAL_USERNAME));
    assertFalse(noteService.canViewNote(noteService.getNoteById(restrictedId), INTERNAL_USERNAME));

    Map<String, String> pages = new HashMap<>();
    String defaultContent = String.format("<p>d</p><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/888\">",
                                          readableId,
                                          readableFileId,
                                          restrictedId,
                                          restrictedFileId,
                                          readableId);
    String frContent = String.format("<p>fr</p><img src=\"/wikiPage/%s/%s\">", readableId, readableFileId);
    pages.put("", defaultContent);
    pages.put("fr", frContent);
    NotePageData data = new NotePageData(pages);

    assertThrows(ObjectNotFoundException.class, () -> notePageViewService.importNotePageData(targetName + "22", data, internalUser));
    assertThrows(IllegalAccessException.class,
                 () -> notePageViewService.importNotePageData(restrictedTargetName, data, internalUser));
    assertNull(notePageViewService.importNotePageData(targetName, null, internalUser));
    assertNull(notePageViewService.importNotePageData(targetName, new NotePageData(new HashMap<>()), internalUser));
    assertNull(notePageViewService.importNotePageData(targetName, new NotePageData(Map.of("fr", frContent)), internalUser));
    assertNull(notePageViewService.getNotePage(targetName, null, internalUser));

    Page target = notePageViewService.importNotePageData(targetName, data, internalUser);
    String targetId = target.getId();
    String expectedDefault = String.format("<p>d</p><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/888\">",
                                           targetId,
                                           readableFileId,
                                           restrictedId,
                                           restrictedFileId,
                                           readableId);
    assertEquals(expectedDefault, target.getContent());
    assertEquals(expectedDefault, notePageViewService.getNotePage(targetName, null, internalUser).getContent());
    assertEquals(String.format("<p>fr</p><img src=\"/wikiPage/%s/%s\">", targetId, readableFileId),
                 notePageViewService.getNotePage(targetName, "fr", internalUser).getContent());
    assertNotNull(attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, targetId, readableFileId));
    List<MetadataItem> attachmentItems = metadataService.getMetadataItemsByObject(new MetadataObject(WikiPageAttachmentPlugin.OBJECT_TYPE,
                                                                                                    targetId));
    assertEquals(1, attachmentItems.size());
    assertEquals(Long.parseLong(identityManager.getOrCreateUserIdentity(INTERNAL_USERNAME).getId()),
                 attachmentItems.get(0).getCreatorId());
    assertNull(attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, targetId, restrictedFileId));
    assertNull(attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, targetId, "888"));
    assertEquals(List.of("fr"), noteService.getPageAvailableTranslationLanguages(Long.parseLong(targetId), false));
  }

  /**
   * A translation write creates the version of its language, in the stored
   * form of the code, and leaves the default content as stored; a second
   * write in that language updates the version. The mutant that writes the
   * default content for a missing language fails on the English content.
   */
  public void testCreateNoteTranslationLeavesTheDefaultContentUnchanged() throws Exception {
    String pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);
    createUser(USERNAME);
    createUser(INTERNAL_USERNAME);
    org.exoplatform.services.security.Identity administrator = registerAdministratorUser(USERNAME);
    org.exoplatform.services.security.Identity internalUser = registerInternalUser(INTERNAL_USERNAME);

    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.createNoteTranslation(pageNoteName + "22", "fr content", "fr", administrator));
    assertThrows(IllegalAccessException.class,
                 () -> notePageViewService.createNoteTranslation(pageNoteName, "fr content", "fr", internalUser));
    assertThrows(IllegalArgumentException.class,
                 () -> notePageViewService.createNoteTranslation(pageNoteName, "fr content", "fr", administrator));

    notePageViewService.saveNotePage(pageNoteName, "en content", null, administrator);
    assertThrows(IllegalArgumentException.class,
                 () -> notePageViewService.createNoteTranslation(pageNoteName, "fr content", " ", administrator));

    Page frPage = notePageViewService.createNoteTranslation(pageNoteName, "fr content", "fr", administrator);
    assertEquals("fr content", frPage.getContent());
    assertEquals("fr", frPage.getLang());
    long pageId = Long.parseLong(frPage.getId());
    assertEquals("en content", noteService.getNoteByIdAndLang(pageId, null).getContent());
    assertEquals(List.of("fr"), noteService.getPageAvailableTranslationLanguages(pageId, false));
    assertEquals("en content", notePageViewService.getNotePage(pageNoteName, null, internalUser).getContent());
    assertEquals("fr content", notePageViewService.getNotePage(pageNoteName, "fr", internalUser).getContent());

    notePageViewService.createNoteTranslation(pageNoteName, "fr content 2", "fr", administrator);
    assertEquals("fr content 2", notePageViewService.getNotePage(pageNoteName, "fr", internalUser).getContent());
    assertEquals("en content", notePageViewService.getNotePage(pageNoteName, null, internalUser).getContent());
    assertEquals(List.of("fr"), noteService.getPageAvailableTranslationLanguages(pageId, false));

    notePageViewService.createNoteTranslation(pageNoteName, "es content", "es_ES", administrator);
    assertEquals("es content", notePageViewService.getNotePage(pageNoteName, "es_ES", internalUser).getContent());
    assertEquals("en content", notePageViewService.getNotePage(pageNoteName, "es-ES", internalUser).getContent());
    assertEquals("en content", notePageViewService.getNotePage(pageNoteName, null, internalUser).getContent());
  }

  /**
   * A copy carries the default content and every published translation onto
   * the target note, clones the source note's own attachments the content
   * names and leaves the source as stored. The mutants that clone every
   * named file, or skip the translations, fail it.
   */
  public void testCopyNotePageCopiesEveryLanguageAndTheSourceAttachments() throws Exception {
    String sourceName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String targetName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String emptySourceName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String restrictedSourceName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String sourcePageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    String targetPageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    String restrictedPageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(),
                                                ADMINISTRATORS_GROUP,
                                                ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, sourceName, sourcePageReference, 0l, USER_IDENTITY_ID);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, targetName, targetPageReference, 0l, USER_IDENTITY_ID);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, emptySourceName, sourcePageReference, 0l, USER_IDENTITY_ID);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE,
                               restrictedSourceName,
                               restrictedPageReference,
                               0l,
                               USER_IDENTITY_ID);
    createUser(USERNAME);
    createUser(INTERNAL_USERNAME);
    org.exoplatform.services.security.Identity administrator = registerAdministratorUser(USERNAME);
    org.exoplatform.services.security.Identity internalUser = registerInternalUser(INTERNAL_USERNAME);
    long userIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(USERNAME).getId());

    String sourceId = notePageViewService.saveNotePage(sourceName, "first", null, administrator).getId();
    String fileId = writeFile();
    attachmentService.createAttachment(fileId,
                                       WikiPageAttachmentPlugin.OBJECT_TYPE,
                                       sourceId,
                                       null,
                                       userIdentityId,
                                       Collections.emptyMap());
    String content = String.format("<p>default</p><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/888\"><img src=\"/wikiPage/999999/%s\">",
                                   sourceId,
                                   fileId,
                                   sourceId,
                                   fileId);
    notePageViewService.saveNotePage(sourceName, content, null, administrator);
    notePageViewService.createNoteTranslation(sourceName, "fr content", "fr", administrator);
    notePageViewService.saveNotePage(restrictedSourceName, "restricted", null, administrator);

    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.copyNotePage(sourceName + "22", targetName, administrator));
    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.copyNotePage(sourceName, targetName + "22", administrator));
    assertThrows(IllegalAccessException.class, () -> notePageViewService.copyNotePage(sourceName, targetName, internalUser));
    assertThrows(IllegalAccessException.class,
                 () -> notePageViewService.copyNotePage(restrictedSourceName, targetName, internalUser));
    assertNull(notePageViewService.copyNotePage(emptySourceName, targetName, administrator));
    assertNull(notePageViewService.getNotePage(targetName, null, administrator));

    Page targetPage = notePageViewService.copyNotePage(sourceName, targetName, administrator);
    String targetId = targetPage.getId();
    assertNotSame(sourceId, targetId);
    String expectedContent = String.format("<p>default</p><img src=\"/wikiPage/%s/%s\"><img src=\"/wikiPage/%s/888\"><img src=\"/wikiPage/999999/%s\">",
                                           targetId,
                                           fileId,
                                           sourceId,
                                           fileId);
    assertEquals(expectedContent, targetPage.getContent());
    assertEquals(expectedContent, notePageViewService.getNotePage(targetName, null, internalUser).getContent());
    assertNotNull(attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, targetId, fileId));
    assertNull(attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, targetId, "888"));
    assertEquals("fr content", notePageViewService.getNotePage(targetName, "fr", internalUser).getContent());
    assertEquals(List.of("fr"), noteService.getPageAvailableTranslationLanguages(Long.parseLong(targetId), false));
    assertEquals(content, notePageViewService.getNotePage(sourceName, null, internalUser).getContent());
    assertEquals("fr content", notePageViewService.getNotePage(sourceName, "fr", internalUser).getContent());
  }

  /**
   * A write to an existing translation versions that language's content
   * only: its own summary and cover, and the default content's, stay as
   * stored. The mutant that versions the default page's in-memory
   * properties under the translation writes the default's summary and a
   * cover id of 0 over them.
   */
  public void testSaveNotePageTranslationKeepsItsOwnSummaryAndCover() throws Exception {
    String pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);
    createUser(USERNAME);
    org.exoplatform.services.security.Identity administrator = registerAdministratorUser(USERNAME);
    long userIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(USERNAME).getId());

    notePageViewService.saveNotePage(pageNoteName, "default content", null, administrator);
    Page page = notePageViewService.getNotePage(pageNoteName, null, administrator);
    long pageId = Long.parseLong(page.getId());
    page.setLang("fr");
    page.setContent("fr content");
    noteService.createVersionOfNote(page, USERNAME);
    noteService.saveNoteMetadata(new NotePageProperties(pageId, "default summary", null, false, false, false),
                                 null,
                                 userIdentityId);
    NoteFeaturedImage frCover = new NoteFeaturedImage(4242L, "image/png", null, "fr cover", 0L, false);
    noteService.saveNoteMetadata(new NotePageProperties(pageId, "fr summary", frCover, false, false, false),
                                 "fr",
                                 userIdentityId);

    notePageViewService.saveNotePage(pageNoteName, "fr content 2", "fr", administrator);

    Page frPage = noteService.getNoteByIdAndLang(pageId, "fr");
    assertEquals("fr content 2", frPage.getContent());
    assertEquals("fr summary", frPage.getProperties().getSummary());
    assertEquals(4242L, frPage.getProperties().getFeaturedImage().getId().longValue());
    Page defaultPage = noteService.getNoteByIdAndLang(pageId, null);
    assertEquals("default content", defaultPage.getContent());
    assertEquals("default summary", defaultPage.getProperties().getSummary());
    assertEquals("fr content 2", notePageViewService.getNotePage(pageNoteName, "fr", administrator).getContent());
  }

  public void testGetNotePageWithAnonim() throws IllegalAccessException, ObjectAlreadyExistsException, ObjectNotFoundException {
    assertNull(notePageViewService.getNotePage("notExistingPage", null, null));

    String pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    Page notePage = notePageViewService.getNotePage(pageNoteName, null, null);
    assertNull(notePage);
    assertThrows(IllegalAccessException.class, () -> notePageViewService.saveNotePage(pageNoteName, pageContent, null, null));
    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.saveNotePage(pageNoteName + "22", pageContent, null, null));
    notePageViewService.saveNotePage(pageNoteName, pageContent, null, registerAdministratorUser(USERNAME));

    notePage = notePageViewService.getNotePage(pageNoteName, null, null);
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());
  }

  public void testGetNotePageWithAuthenticated() throws IllegalAccessException,
                                                 ObjectAlreadyExistsException,
                                                 ObjectNotFoundException,
                                                 WikiException {
    String pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    String pageContentEn = "pageContentEn" + RANDOM.nextLong();
    String pageContentFr = "pageContentFr" + RANDOM.nextLong();
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    assertThrows(IllegalAccessException.class, () -> notePageViewService.getNotePage(pageNoteName, null, null));
    Page notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNull(notePage);
    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.saveNotePage(pageNoteName + "22", pageContentEn, null, null));
    assertThrows(IllegalAccessException.class, () -> notePageViewService.saveNotePage(pageNoteName, pageContentEn, null, null));
    assertThrows(IllegalAccessException.class,
                 () -> notePageViewService.saveNotePage(pageNoteName, pageContentEn, null, registerInternalUser(USERNAME)));
    notePageViewService.saveNotePage(pageNoteName, pageContentFr, null, registerAdministratorUser(USERNAME));
    // Test save a language not saved, which has to change the default lang
    createUser(USERNAME);
    notePageViewService.saveNotePage(pageNoteName, pageContentEn, "fr", registerAdministratorUser(USERNAME));

    assertThrows(IllegalAccessException.class, () -> notePageViewService.getNotePage(pageNoteName, null, null));
    notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentEn, notePage.getContent());
    notePage = notePageViewService.getNotePage(pageNoteName, "fr", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentEn, notePage.getContent());

    notePageViewService.saveNotePage(pageNoteName, pageContentFr, "fr", registerAdministratorUser(USERNAME));

    notePage.setLang(null);
    notePage.setUpdatedDate(new Date());
    noteService.updateNote(notePage);
    notePage.setLang("fr");
    notePage.setContent(pageContentFr);
    noteService.createVersionOfNote(notePage, USERNAME);

    notePage = notePageViewService.getNotePage(pageNoteName, "fr", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentFr, notePage.getContent());

    notePage = notePageViewService.getNotePage(pageNoteName, "it", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentEn, notePage.getContent());
  }

  @SneakyThrows
  public void testGetNotePageNoLang() {
    String pageReference = createPage(PAGE_NAME_PREFIX + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);

    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageContentModified = "pageContentModified" + RANDOM.nextLong();

    String pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    notePageViewService.saveNotePage(pageNoteName, pageContent, null, registerAdministratorUser(USERNAME));

    Page notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());
    notePage = notePageViewService.getNotePage(pageNoteName, "fr", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());

    createUser(USERNAME);
    notePageViewService.saveNotePage(pageNoteName, pageContentModified, null, registerAdministratorUser(USERNAME));

    notePage = notePageViewService.getNotePage(pageNoteName, "fr", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentModified, notePage.getContent());

    notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContentModified, notePage.getContent());

    NotePageData notePageData = notePageViewService.getNotePageData(pageNoteName);
    assertNotNull(notePageData);
    assertEquals(pageContentModified, notePageData.getPages().get(""));
    assertEquals(1, notePageData.getPages().size());

    pageNoteName = PAGE_NOTE_NAME_PREFIX + RANDOM.nextLong();
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);
    notePageViewService.savePageData(pageNoteName, notePageData);

    notePageViewService.saveNotePage(pageNoteName, pageContent, null, registerAdministratorUser(USERNAME));

    notePage = notePageViewService.getNotePage(pageNoteName, "fr", registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());

    notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());

    notePageData = notePageViewService.getNotePageData(pageNoteName);
    assertNotNull(notePageData);
    assertEquals(pageContent, notePageData.getPages().get(""));
    assertEquals(1, notePageData.getPages().size());
  }

  private String writeFile() throws Exception {
    return String.valueOf(fileService.writeFile(new FileItem(null,
                                                             "image.png",
                                                             "image/png",
                                                             "wiki",
                                                             3,
                                                             new Date(),
                                                             USERNAME,
                                                             false,
                                                             new ByteArrayInputStream(new byte[] { 1, 2, 3 })))
                                     .getFileInfo()
                                     .getId());
  }

  private String createPage(String pageName, String accessPermission, String editPermission) {
    String siteType = "portal";
    String siteName = "classic";
    if (layoutService.getPortalConfig(siteName) == null) {
      PortalConfig portal = new PortalConfig();
      portal.setType(siteType);
      portal.setName(siteName);
      portal.setLocale("en");
      portal.setLabel("Test");
      portal.setDescription("Test");
      portal.setAccessPermissions(new String[] { UserACL.EVERYONE });
      layoutService.create(portal);
    }

    PageKey pageKey = new PageKey(siteType, siteName, pageName);
    PageState pageState = new PageState(pageName,
                                        null,
                                        false,
                                        null,
                                        Collections.singletonList(accessPermission),
                                        editPermission);
    layoutService.save(new PageContext(pageKey, pageState));
    return pageKey.format();
  }

  private org.exoplatform.services.security.Identity registerAdministratorUser(String user) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(user,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/administrators")));
    identityRegistry.register(identity);
    return identity;
  }

  private void createUser(String username) {
    try {
      organizationService.getUserHandler().createUser(
              organizationService.getUserHandler().createUserInstance(username), true);
    } catch (Exception e) {
      //Nothing
    }
  }

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/users")));
    identityRegistry.register(identity);
    return identity;
  }

}
