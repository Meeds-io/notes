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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.rest.NotesRestService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

@SuppressWarnings("deprecation")
@RunWith(MockitoJUnitRunner.Silent.class)
public class TestWikiRestService extends AbstractKernelTest { // NOSONAR

  private MockedStatic<EntityBuilder> entityBuilder;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    entityBuilder = mockStatic(EntityBuilder.class);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    entityBuilder.close();
    super.tearDown();
  }

  @Test
  public void testSearchData() throws Exception {
    org.exoplatform.services.security.Identity root = new org.exoplatform.services.security.Identity("root");
    ConversationState.setCurrent(new ConversationState(root));
    // Given
    WikiService wikiService = mock(WikiService.class);
    NoteService noteService = mock(NoteService.class);
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
    when(noteService.getNoteOfNoteBookByName(any(),
                                             any(),
                                             any(),
                                             any(org.exoplatform.services.security.Identity.class))).thenReturn(page);
    List<org.exoplatform.wiki.service.search.SearchResult> results = new ArrayList<>();
    results.add(result1);
    results.add(result2);
    PageList<org.exoplatform.wiki.service.search.SearchResult> pageList = new ObjectPageList<>(results, 2);
    when(noteService.search(nullable(WikiSearchData.class))).thenReturn(pageList);
    when(uriInfo.getPath()).thenReturn("/notes/contextsearch");
    entityBuilder.when(() -> EntityBuilder.buildEntityIdentity(nullable(Identity.class), anyString(), anyString()))
                 .thenReturn(entity);
    NotesRestService wikiRestService =
                                     new NotesRestService(noteService, wikiService, null, new MockResourceBundleService(), null);

    // When
    List<String> tagNames = new ArrayList<>();
    tagNames.add("testTag");
    Response response = wikiRestService.searchData(uriInfo, "wiki", 10, "page", "alioua", false, tagNames);

    // Then
    assertEquals(200, response.getStatus());
  }
}
