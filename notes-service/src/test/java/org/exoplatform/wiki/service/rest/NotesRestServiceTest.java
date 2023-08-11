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

package org.exoplatform.wiki.service.rest;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.*;
import org.exoplatform.wiki.service.impl.BeanToJsons;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ ConversationState.class, EnvironmentContext.class, TreeUtils.class, Utils.class, ExoContainerContext.class,
    ExoContainer.class })
@PowerMockIgnore({ "javax.management.*", "jdk.internal.*", "javax.xml.*", "org.apache.xerces.*", "org.xml.*",
        "com.sun.org.apache.*", "org.w3c.*" })
public class NotesRestServiceTest {

  @Mock
  private NoteService      noteService;

  @Mock
  private WikiService      noteBookService;

  @Mock
  private UploadService    uploadService;

  @Mock
  private NotesExportService notesExportService;

  private NotesRestService notesRestService;

  private ResourceBundleService resourceBundleService;

  @Mock
  private Identity         identity;

  @Before
  public void setUp() throws Exception {
    this.notesRestService = new NotesRestService(noteService, noteBookService, uploadService, new MockResourceBundleService(), notesExportService);
    PowerMockito.mockStatic(ConversationState.class);
    ConversationState conversationState = mock(ConversationState.class);
    when(ConversationState.getCurrent()).thenReturn(conversationState);
    when(ConversationState.getCurrent().getIdentity()).thenReturn(identity);

    PowerMockito.mockStatic(EnvironmentContext.class);
    EnvironmentContext environmentContext = mock(EnvironmentContext.class);
    when(EnvironmentContext.getCurrent()).thenReturn(environmentContext);

    HttpServletRequest request = mock(HttpServletRequest.class);
    when(request.getLocale()).thenReturn(new Locale("en"));
    when(environmentContext.get(HttpServletRequest.class)).thenReturn(request);

    PowerMockito.mockStatic(TreeUtils.class);
    PowerMockito.mockStatic(Utils.class);

    PowerMockito.mockStatic(ExoContainerContext.class);
    PowerMockito.mockStatic(ExoContainer.class);
    ExoContainer exoContainer = mock(ExoContainer.class);
    when(ExoContainerContext.getCurrentContainer()).thenReturn(exoContainer);
    when(exoContainer.getComponentInstanceOfType(WikiService.class)).thenReturn(noteBookService);
    when(exoContainer.getComponentInstanceOfType(NoteService.class)).thenReturn(noteService);
  }

  @Test
  public void getNoteById() throws WikiException, IllegalAccessException {
    Page page = new Page();
    List<Page> children = new ArrayList<>();
    children.add(new Page("child1"));
    List<BreadcrumbData> breadcrumb = new ArrayList<>();
    breadcrumb.add(new BreadcrumbData("1", "test", "note", "user"));
    page.setDeleted(true);
    when(noteService.getNoteById("1", identity, "source")).thenReturn(null);
    Response response = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatus());
    when(noteService.getNoteById("1", identity, "source")).thenReturn(page);
    Response response1 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response1.getStatus());
    page.setDeleted(false);
    page.setWikiType("type");
    Response response2 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response2.getStatus());
    page.setWikiType("note");
    page.setWikiOwner("owner");
    Response response3 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response3.getStatus());

    page.setWikiOwner("user");
    page.setContent("any wiki-children-pages ck-widget any");
    when(identity.getUserId()).thenReturn("userId");
    when(noteService.getChildrenNoteOf(page, "userId", false, true)).thenReturn(children);

    when(noteService.getBreadCrumb("note", "user", "1", false)).thenReturn(breadcrumb);
    when(noteService.updateNote(page)).thenReturn(page);
    Response response4 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.OK.getStatusCode(), response4.getStatus());

    doThrow(new IllegalAccessException()).when(noteService).getNoteById("1", identity, "source");
    Response response5 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response5.getStatus());

    doThrow(new RuntimeException()).when(noteService).getNoteById("1", identity, "source");
    Response response6 = notesRestService.getNoteById("1", "note", "user", true, "source");
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response6.getStatus());
  }

  @Test
  public void getFullTreeData() throws Exception {
    Page homePage = new Page("home");
    homePage.setWikiOwner("user");
    homePage.setWikiType("WIKIHOME");
    homePage.setOwner("user");
    homePage.setId("1");
    homePage.setParentPageId("0");
    homePage.setHasChild(true);
    Wiki noteBook = new Wiki();
    noteBook.setOwner("user");
    noteBook.setType("WIKI");
    noteBook.setId("0");
    noteBook.setWikiHome(homePage);
    Page page = new Page("testPage");
    page.setName("testPage");
    page.setTitle("testPage");
    page.setId("10");
    page.setParentPageId("1");
    page.setWikiType("PAGE");
    Page page1 = new Page("testPage 1");
    page1.setName("testPage 1");
    page1.setTitle("testPage 1");
    page1.setId("11");
    page1.setParentPageId("1");
    page1.setWikiType("PAGE");
    Page page2 = new Page("testPage 2");
    page2.setName("testPage 22");
    page2.setTitle("testPage 22");
    page2.setId("12");
    page2.setParentPageId("1");
    page2.setWikiType("PAGE");
    Page page10 = new Page("testPage 10");
    page10.setName("testPage 10");
    page10.setTitle("testPage 10");
    page10.setId("13");
    page10.setParentPageId("1");
    page10.setWikiType("PAGE");
    Page page12 = new Page("testPage 12");
    page12.setName("testPage 2");
    page12.setTitle("testPage 2");
    page12.setId("14");
    page12.setParentPageId("1");
    page12.setWikiType("PAGE");
    Page draftPage = new DraftPage();
    draftPage.setId("3");
    draftPage.setName("testPageDraft");
    page.setWikiType("PAGE");
    draftPage.setParentPageId("1");
    draftPage.setDraftPage(true);
    draftPage.setWikiType("PAGE");
    WikiPageParams pageParams = new WikiPageParams();
    pageParams.setPageName("home");
    pageParams.setOwner("user");
    pageParams.setType("WIKI");
    List<Page> childrenWithDraft = new ArrayList<>(List.of(page, draftPage, page10, page12, page2, page1));
    List<Page> childrenWithoutDrafts = new ArrayList<>(List.of(page12, page10, page1, page, page2)); // return an unordered list
    Deque paramsDeque = mock(Deque.class);
    when(identity.getUserId()).thenReturn("1");
    when(TreeUtils.getPageParamsFromPath("path")).thenReturn(pageParams);
    when(Utils.getStackParams(homePage)).thenReturn(paramsDeque);
    when(paramsDeque.pop()).thenReturn(pageParams);
    when(noteService.getNoteOfNoteBookByName(pageParams.getType(),
                                             pageParams.getOwner(),
                                             pageParams.getPageName(),
                                             identity)).thenReturn(null);
    when(noteService.getNoteOfNoteBookByName(pageParams.getType(),
                                             pageParams.getOwner(),
                                             NoteConstants.NOTE_HOME_NAME)).thenReturn(homePage);

    when(noteBookService.getWikiByTypeAndOwner(pageParams.getType(), pageParams.getOwner())).thenReturn(noteBook);
    when(noteBookService.getWikiByTypeAndOwner(homePage.getWikiType(), homePage.getWikiOwner())).thenReturn(noteBook);
    doCallRealMethod().when(TreeUtils.class, "getPathFromPageParams", ArgumentMatchers.any());
    doCallRealMethod().when(Utils.class, "validateWikiOwner", homePage.getWikiType(), homePage.getWikiOwner());
    doCallRealMethod().when(TreeUtils.class, "tranformToJson", ArgumentMatchers.any(), ArgumentMatchers.any());
    when(noteService.getChildrenNoteOf(homePage, ConversationState.getCurrent().getIdentity().getUserId(), true, false)).thenReturn(childrenWithDraft);
    when(noteService.getChildrenNoteOf(homePage, ConversationState.getCurrent().getIdentity().getUserId(), false, false)).thenReturn(childrenWithoutDrafts);
    when(Utils.getObjectFromParams(pageParams)).thenReturn(homePage);
    when(Utils.isDescendantPage(homePage, page)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, page1)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, page2)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, page10)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, page12)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, draftPage)).thenReturn(true);

    Response response = notesRestService.getFullTreeData("path", true);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    Response response3 = notesRestService.getFullTreeData("path", false);
    assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());
    assertEquals(6, ((BeanToJsons) response3.getEntity()).getJsonList().size());
    List<JsonNodeData> treeNodeList = ((BeanToJsons) response3.getEntity()).getTreeNodeData();
    JsonNodeData jsonNodeData = treeNodeList.get(0);
    assertEquals(5, jsonNodeData.getChildren().size());
    assertEquals("testPage", jsonNodeData.getChildren().get(0).getName());
    assertEquals("testPage 1", jsonNodeData.getChildren().get(1).getName());
    assertEquals("testPage 2", jsonNodeData.getChildren().get(2).getName());
    assertEquals("testPage 10", jsonNodeData.getChildren().get(3).getName());
    assertEquals("testPage 22", jsonNodeData.getChildren().get(4).getName());


    doThrow(new IllegalAccessException()).when(noteService)
                                         .getNoteOfNoteBookByName(pageParams.getType(),
                                                                  pageParams.getOwner(),
                                                                  pageParams.getPageName(),
                                                                  identity);
    Response response1 = notesRestService.getFullTreeData("path", true);
    assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), response1.getStatus());

    doThrow(new RuntimeException()).when(noteService)
                                   .getNoteOfNoteBookByName(pageParams.getType(),
                                                            pageParams.getOwner(),
                                                            pageParams.getPageName(),
                                                            identity);
    Response response2 = notesRestService.getFullTreeData("path", true);
    assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), response2.getStatus());
  }
}
