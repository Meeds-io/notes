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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.nullable;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.rest.NotesRestService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 *
 */
@RunWith(PowerMockRunner.class)
@PowerMockIgnore({ "javax.management.*", "javax.xml.*", "org.xml.*", "org.w3c.*", "com.sun.org.apache.xalan.internal.*",
    "jdk.xml.internal.*", "com.sun.org.apache.xerces.*" })
public class TestWikiRestService {
  @PrepareForTest({ EntityBuilder.class })
  @Test
  public void testSearchData() throws Exception {
    org.exoplatform.services.security.Identity root = new org.exoplatform.services.security.Identity("root");
    ConversationState.setCurrent(new ConversationState(root));
    // Given
    WikiService wikiService = mock(WikiService.class);
    NoteService noteService = mock(NoteService.class);
    EntityBuilder entityBuilder = mock(EntityBuilder.class);
    java.util.Calendar cDate1 = java.util.Calendar.getInstance();
    UriInfo uriInfo = mock(UriInfo.class);
    org.exoplatform.social.core.identity.model.Identity identityResult =
                                                                       new org.exoplatform.social.core.identity.model.Identity();
    identityResult.setProviderId("organization");
    identityResult.setRemoteId("root");
    identityResult.setId("1");
    Page page = new Page();
    page.setWikiId("1");
    page.setWikiType("Page");
    page.setWikiOwner("alioua");
    page.setName("Wiki_one");
    page.setTitle("Wiki one");
    page.setUrl("/exo/wiki");
    org.exoplatform.wiki.service.search.SearchResult result1 = new SearchResult();
    org.exoplatform.wiki.service.search.SearchResult result2 = new SearchResult();
    result1.setPageName("wiki");
    result1.setExcerpt("admin");
    result1.setPoster(null);
    result2.setExcerpt("admin");
    result2.setPageName("wik");
    result2.setPoster(identityResult);
    result2.setTitle("wiki test");
    result2.setCreatedDate(cDate1);
    org.exoplatform.social.rest.entity.IdentityEntity entity = new org.exoplatform.social.rest.entity.IdentityEntity();
    entity.setProviderId("organization");
    entity.setRemoteId("root");
    entity.setId("1");
    entity.setDeleted(false);
    when(wikiService.getPageOfWikiByName(any(), any(), any())).thenReturn(page);
    when(noteService.getNoteOfNoteBookByName(any(),
                                             any(),
                                             any(),
                                             any(org.exoplatform.services.security.Identity.class))).thenReturn(page);
    List<org.exoplatform.wiki.service.search.SearchResult> results =
                                                                   new ArrayList<org.exoplatform.wiki.service.search.SearchResult>();
    results.add(result1);
    results.add(result2);
    PowerMockito.mockStatic(EntityBuilder.class);
    PageList<org.exoplatform.wiki.service.search.SearchResult> pageList = new ObjectPageList<>(results, 2);
    when(noteService.search(nullable(WikiSearchData.class))).thenReturn(pageList);
    when(uriInfo.getPath()).thenReturn("/notes/contextsearch");
    when(EntityBuilder.buildEntityIdentity(nullable(Identity.class), anyString(), anyString())).thenReturn(entity);
    NotesRestService wikiRestService =
                                     new NotesRestService(noteService, wikiService, null, new MockResourceBundleService(), null);

    // When
    Response response = wikiRestService.searchData(uriInfo, "wiki", 10, "page", "alioua", false);

    // Then
    assertEquals(200, response.getStatus());

    PowerMockito.verifyStatic(EntityBuilder.class, times(1));
    EntityBuilder.buildEntityIdentity(nullable(Identity.class), anyString(), anyString());
  }
}
