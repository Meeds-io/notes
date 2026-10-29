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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Random;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

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

  private NoteService           noteService;

  private NotePageViewService   notePageViewService;

  private CMSService            cmsService;

  private LayoutService         layoutService;

  private IdentityRegistry      identityRegistry;

  private OrganizationService  organizationService;

  private IdentityManager       identityManager;

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
