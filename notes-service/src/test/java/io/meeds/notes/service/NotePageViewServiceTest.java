/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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
import java.util.Random;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;

import io.meeds.social.cms.service.CMSService;
import io.meeds.social.cms.service.CMSServiceImpl;

public class NotePageViewServiceTest extends BaseTest { // NOSONAR

  protected static final Random RANDOM               = new Random();

  private static final long     USER_IDENTITY_ID     = 5l;

  private static final String   USERS_GROUP          = "*:/platform/users";

  private static final String   ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String   USERNAME             = "testUser";

  private NotePageViewService   notePageViewService;

  private CMSService            cmsService;

  private LayoutService         layoutService;

  private IdentityRegistry      identityRegistry;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    this.notePageViewService = getContainer().getComponentInstanceOfType(NotePageViewService.class);
    this.cmsService = getContainer().getComponentInstanceOfType(CMSServiceImpl.class);
    this.layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    this.identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);
  }

  public void testGetNotePageWithAnonim() throws IllegalAccessException, ObjectAlreadyExistsException, ObjectNotFoundException {
    assertNull(notePageViewService.getNotePage("notExistingPage", null, null));

    String pageNoteName = "pageNoteName" + RANDOM.nextLong();
    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageReference = createPage("pageName" + RANDOM.nextLong(), UserACL.EVERYONE, ADMINISTRATORS_GROUP);
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
                                                 ObjectNotFoundException {
    String pageNoteName = "pageNoteName" + RANDOM.nextLong();
    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageReference = createPage("pageName" + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    assertThrows(IllegalAccessException.class, () -> notePageViewService.getNotePage(pageNoteName, null, null));
    Page notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNull(notePage);
    assertThrows(ObjectNotFoundException.class,
                 () -> notePageViewService.saveNotePage(pageNoteName + "22", pageContent, null, null));
    assertThrows(IllegalAccessException.class, () -> notePageViewService.saveNotePage(pageNoteName, pageContent, null, null));
    assertThrows(IllegalAccessException.class, () -> notePageViewService.saveNotePage(pageNoteName, pageContent, null, registerInternalUser(USERNAME)));
    notePageViewService.saveNotePage(pageNoteName, pageContent, null, registerAdministratorUser(USERNAME));

    assertThrows(IllegalAccessException.class, () -> notePageViewService.getNotePage(pageNoteName, null, null));
    notePage = notePageViewService.getNotePage(pageNoteName, null, registerInternalUser(USERNAME));
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());
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
                                        false,
                                        null,
                                        Collections.singletonList(accessPermission),
                                        editPermission,
                                        Collections.singletonList(editPermission),
                                        Collections.singletonList(editPermission),
                                        PageType.PAGE.name(),
                                        null);
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

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(new MembershipEntry("/platform/users")));
    identityRegistry.register(identity);
    return identity;
  }

}
