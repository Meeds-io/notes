package org.exoplatform.wiki.service.rest;

import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mock.MockResourceBundleService;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.service.BreadcrumbData;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
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

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.assertEquals;
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

  private NotesRestService notesRestService;

  @Mock
  private Identity         identity;

  @Before
  public void setUp() throws Exception {
    this.notesRestService = new NotesRestService(noteService, noteBookService, uploadService, new MockResourceBundleService());
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
    Wiki noteBook = new Wiki();
    noteBook.setOwner("user");
    noteBook.setType("WIKI");
    noteBook.setId("0");
    noteBook.setWikiHome(homePage);
    Page page = new Page("testPage");
    page.setId("2");
    page.setParentPageId("1");
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
    List<Page> children = new ArrayList<>(List.of(page, draftPage));
    homePage.setChildren(children);
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
    when(noteBookService.getChildrenPageOf(homePage, ConversationState.getCurrent().getIdentity().getUserId(), true)).thenReturn(children);
    when(Utils.getObjectFromParams(pageParams)).thenReturn(homePage);
    when(Utils.isDescendantPage(homePage, page)).thenReturn(true);
    when(Utils.isDescendantPage(homePage, draftPage)).thenReturn(true);

    Response response = notesRestService.getFullTreeData("path", true);
    assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());

    Response response3 = notesRestService.getFullTreeData("path", false);
    assertEquals(Response.Status.OK.getStatusCode(), response3.getStatus());


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
