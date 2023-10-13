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
package io.meeds.notes.rest;

import static org.mockito.Mockito.mockStatic;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.mockito.MockedStatic;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.PageType;
import org.exoplatform.portal.mop.page.PageContext;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.page.PageState;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.security.IdentityRegistry;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.service.test.AbstractResourceTest;
import org.exoplatform.wiki.model.Page;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.cms.service.CMSServiceImpl;

public class NotePageViewRestTest extends AbstractResourceTest { // NOSONAR

  private static final String            BASE_URL             = "/notes/view";               // NOSONAR

  private static MockedStatic<RestUtils> REST_UTILS;                                         // NOSONAR

  protected static final Random          RANDOM               = new Random();

  private static final long              USER_IDENTITY_ID     = 5l;

  private static final String            USERS_GROUP          = "*:/platform/users";

  private static final String            ADMINISTRATORS_GROUP = "*:/platform/administrators";

  private static final String            USERNAME             = "testUser";

  private CMSService                     cmsService;

  private LayoutService                  layoutService;

  private IdentityRegistry               identityRegistry;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    this.cmsService = getContainer().getComponentInstanceOfType(CMSServiceImpl.class);
    this.layoutService = getContainer().getComponentInstanceOfType(LayoutService.class);
    this.identityRegistry = getContainer().getComponentInstanceOfType(IdentityRegistry.class);

    NotePageViewService notePageViewService = getContainer().getComponentInstanceOfType(NotePageViewService.class);
    registry(new NotePageViewRest(notePageViewService));

    ExoContainerContext.setCurrentContainer(getContainer());
    restartTransaction();
    begin();
    REST_UTILS = mockStatic(RestUtils.class); // NOSONAR
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      removeResource(NotePageViewRest.class);
      end();
      super.tearDown();
    } finally {
      REST_UTILS.close(); // NOSONAR
    }
  }

  public void testGetNotePageWithAnonim() throws ObjectAlreadyExistsException {
    resetRestUtils();
    assertNull(getNotePage("notExistingPage", null));

    String pageNoteName = "pageNoteName" + RANDOM.nextLong();
    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageReference = createPage("pageName" + RANDOM.nextLong(), UserACL.EVERYONE, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    Page notePage = getNotePage(pageNoteName, null);
    assertNull(notePage);

    resetRestUtils();
    assertEquals(401, saveNotePage(pageNoteName, pageContent, null).getStatus());
    assertEquals(404, saveNotePage(pageNoteName + "22", pageContent, null).getStatus());

    registerAdministratorUser(USERNAME);
    saveNotePage(pageNoteName, pageContent, null);

    resetRestUtils();
    notePage = getNotePage(pageNoteName, null);
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());
  }

  public void testGetNotePageWithAuthenticated() throws ObjectAlreadyExistsException {
    String pageNoteName = "pageNoteName" + RANDOM.nextLong();
    String pageContent = "pageContent" + RANDOM.nextLong();
    String pageReference = createPage("pageName" + RANDOM.nextLong(), USERS_GROUP, ADMINISTRATORS_GROUP);
    cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE, pageNoteName, pageReference, 0l, USER_IDENTITY_ID);

    resetRestUtils();
    assertEquals(401, getNotePageResponse(pageNoteName, null).getStatus());
    registerInternalUser(USERNAME);
    Page notePage = getNotePage(pageNoteName, null);
    assertNull(notePage);

    resetRestUtils();
    assertEquals(404, saveNotePage(pageNoteName + "22", pageContent, null).getStatus());
    assertEquals(401, saveNotePage(pageNoteName, pageContent, null).getStatus());

    registerInternalUser(USERNAME);
    assertEquals(401, saveNotePage(pageNoteName, pageContent, null).getStatus());

    registerAdministratorUser(USERNAME);
    saveNotePage(pageNoteName, pageContent, null);

    resetRestUtils();
    assertEquals(401, getNotePageResponse(pageNoteName, null).getStatus());

    registerInternalUser(USERNAME);
    notePage = getNotePage(pageNoteName, null);
    assertNotNull(notePage);
    assertEquals(pageContent, notePage.getContent());

    ContainerResponse response = getNotePageResponse(pageNoteName, null);
    String eTagValue = getETagValue(response);
    response = getNotePageWithETag(pageNoteName, null, eTagValue);
    assertEquals(304, response.getStatus());

    registerAdministratorUser(USERNAME);
    saveNotePage(pageNoteName, pageContent, null);

    registerInternalUser(USERNAME);
    response = getNotePageWithETag(pageNoteName, null, eTagValue);
    assertEquals(200, response.getStatus());
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

  private String getUrl(String name, String lang) {
    return BASE_URL + "/" + name + "?lang=" + (lang == null ? "en" : lang);
  }

  private String getUrl(String name) {
    return BASE_URL + "/" + name;
  }

  private Page getNotePage(String name, String lang) {
    try {
      return (Page) getNotePageResponse(name, lang).getEntity();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ContainerResponse getNotePageResponse(String name, String lang) {
    try {
      return getResponse("GET", getUrl(name, lang), null);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private ContainerResponse getNotePageWithETag(String name, String lang, String eTagValue) {
    try {
      MultivaluedMap<String, String> h = new MultivaluedMapImpl();
      h.putSingle("If-None-Match", eTagValue);
      return service("GET", getUrl(name, lang), "", h, new byte[0]);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private String getETagValue(ContainerResponse responseWithEtag) {
    List<Object> eTag = responseWithEtag.getHttpHeaders().get("Etag");
    assertNotNull(eTag);
    assertEquals(1, eTag.size());
    return eTag.get(0).toString();
  }

  private ContainerResponse saveNotePage(String name, String content, String lang) {
    try {
      String urlParam = "content=" + content + "&lang=" + (lang == null ? "en" : lang);
      MultivaluedMap<String, String> h = new MultivaluedMapImpl();
      h.putSingle("content-type", MediaType.APPLICATION_FORM_URLENCODED);
      h.putSingle("content-length", "" + urlParam.length());
      return service("PUT", getUrl(name), "", h, urlParam.getBytes());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  private org.exoplatform.services.security.Identity registerAdministratorUser(String user) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(user,
                                                                                                       Arrays.asList(MembershipEntry.parse(ADMINISTRATORS_GROUP)));
    identityRegistry.register(identity);
    resetRestUtils();
    REST_UTILS.when(RestUtils::getCurrentUser).thenReturn(USERNAME);
    REST_UTILS.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);
    return identity;
  }

  private org.exoplatform.services.security.Identity registerInternalUser(String username) {
    org.exoplatform.services.security.Identity identity =
                                                        new org.exoplatform.services.security.Identity(username,
                                                                                                       Arrays.asList(MembershipEntry.parse(USERS_GROUP)));
    identityRegistry.register(identity);
    resetRestUtils();
    REST_UTILS.when(RestUtils::getCurrentUser).thenReturn(USERNAME);
    REST_UTILS.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);
    return identity;
  }

  private void resetRestUtils() {
    REST_UTILS.reset();
    REST_UTILS.when(RestUtils::getBaseRestUrl).thenReturn("");
  }

}
