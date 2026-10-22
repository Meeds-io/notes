/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.notes.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.notes.mcp.model.NoteModel;
import io.meeds.notes.mcp.model.NoteRootTreeModel;
import io.meeds.notes.mcp.model.NoteSearchCollectionModel;
import io.meeds.notes.mcp.model.NoteSearchHitModel;
import io.meeds.notes.mcp.model.NoteVersionCollectionModel;
import io.meeds.notes.mcp.model.NoteVersionModel;
import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@RunWith(MockitoJUnitRunner.class)
public class NoteMcpToolTest {

  private static final String     OTHER_TITLE    = "Updated";

  private static final String     CONTENT        = "Content";

  private static final String     SUMMARY        = "Summary";

  private static final String     TITLE          = "Title";

  // 1x1 transparent PNG
  private static final String     PNG_1PX        =
                                           "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

  private static final String     WIKI_TYPE      = "group";

  private static final String     GROUP_ID       = "/spaces/test";

  private static final String     USER           = "root";

  private static final String     OTHER_USER     = "alice";

  private static final String     USER_WIKI_TYPE = "user";

  private static final long       NOTE_ID        = 12L;

  private static final long       PARENT_NOTE_ID = 13L;

  private static final long       SPACE_ID       = 33L;

  private static final String     LONG_HTML      =
                                            "<p>Hello <b>world</b></p><p>a&amp;b <a href=\"https://meeds.test/x?a=1&amp;b=2\">link</a></p>";

  private static final JsonMapper JSON           = JsonMapper.builder().build();

  @Mock
  private WikiService             wikiService;

  @Mock
  private NoteService             noteService;

  @Mock
  private ActivityManager         activityManager;

  @Mock
  private IdentityManager         identityManager;

  @Mock
  private I18NActivityProcessor   i18NActivityProcessor;

  @Mock
  private SpaceService            spaceService;

  @Mock
  private TranslationService      translationService;

  @Mock
  private ProfilePropertyService  profilePropertyService;

  @Mock
  private UserACL                 userAcl;

  @Mock
  private UserPortalConfigService portalConfigService;

  @Mock
  private LayoutService           layoutService;

  @Mock
  private PermanentLinkService    permanentLinkService;

  @Mock
  private UploadService           uploadService;

  @Mock
  private AttachmentService       attachmentService;

  @Mock
  private FileService             fileService;

  @Mock
  private Identity                currentIdentity;

  @Mock
  private ConversationState       conversationState;

  private Long                    currentScopeSpaceId;

  private boolean                 scopeUnreadable;

  private NoteMcpTool             tool;

  @Before
  public void setUp() throws Exception { // NOSONAR
    lenient().when(currentIdentity.getUserId()).thenReturn(USER);
    lenient().when(permanentLinkService.getLink(any())).thenReturn("/note-link");

    tool = new TestableNoteMcpTool();
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getSpaceNoteTreeWhenSpaceDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(null);

    tool.getSpaceNoteTree(SPACE_ID);
  }

  @Test(expected = IllegalAccessException.class)
  public void getSpaceNoteTreeWhenUserCannotViewSpaceShouldThrowException() throws Exception { // NOSONAR
    Space space = mockSpace();

    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    when(spaceService.canViewSpace(space, USER)).thenReturn(false);

    tool.getSpaceNoteTree(SPACE_ID);
  }

  @Test
  public void getSpaceNoteTreeShouldReturnRootTreeWithChildren() throws Exception { // NOSONAR
    Space space = mockSpace();
    Wiki wiki = mockWiki();
    Page root = mockPage("1", "Root");
    Page child = mockPage("2", "Child");

    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    when(spaceService.canViewSpace(space, USER)).thenReturn(true);
    when(wikiService.getWikiByTypeAndOwner(WIKI_TYPE, GROUP_ID)).thenReturn(wiki);
    when(wiki.getWikiHome()).thenReturn(root);
    when(noteService.getNoteById("1")).thenReturn(root);
    when(noteService.getChildrenNoteOf(root, false, false)).thenReturn(Collections.singletonList(child));
    when(noteService.getChildrenNoteOf(child, false, false)).thenReturn(Collections.emptyList());

    NoteRootTreeModel result = runWithStaticMocks(() -> tool.getSpaceNoteTree(SPACE_ID));

    assertEquals(1L, result.getNoteId());
    assertEquals(Long.valueOf(SPACE_ID), result.getSpaceId());
    assertEquals(1, result.getChildNotes().size());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void getNoteWhenNoteDoesNotExistShouldThrowException() throws Exception { // NOSONAR
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(null);

    tool.getNote(NOTE_ID, null, null, null, null);
  }

  @Test(expected = IllegalAccessException.class)
  public void getNoteWhenUserCannotViewShouldThrowException() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(false);

    tool.getNote(NOTE_ID, null, null, null, null);
  }

  @Test
  public void getNoteShouldReturnNoteModel() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, null));

    assertEquals(NOTE_ID, result.noteId());
    assertEquals("Note", result.title());
    assertEquals(true, result.canEdit());
    assertEquals(1, result.breadcrumb().size());
  }

  @Test(expected = IllegalArgumentException.class)
  public void createSpaceNoteWhenSpaceIdNullShouldThrowException() throws Exception { // NOSONAR
    tool.createSpaceNote(null, TITLE, SUMMARY, CONTENT);
  }

  @Test
  public void createSpaceNoteShouldCreateNoteUnderSpaceRoot() throws Exception { // NOSONAR
    Space space = mockSpace();
    Wiki wiki = mockWiki();
    Page root = mockPage("1", "Root");
    Page created = mockPage(String.valueOf(NOTE_ID), TITLE);

    when(spaceService.getSpaceById(SPACE_ID)).thenReturn(space);
    when(spaceService.canViewSpace(space, USER)).thenReturn(true);
    when(wikiService.getWikiByTypeAndOwner(WIKI_TYPE, GROUP_ID)).thenReturn(wiki);
    when(wiki.getWikiHome()).thenReturn(root);
    when(noteService.getNoteById("1")).thenReturn(root);

    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    String name = root.getName();
    when(noteService.createNote(eq(wiki),
                                eq(name),
                                any(Page.class),
                                eq(currentIdentity),
                                eq(false),
                                eq(true)))
                                          .thenReturn(created);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.createSpaceNote(SPACE_ID, TITLE, SUMMARY, CONTENT));

    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).createNote(eq(wiki), eq(name), any(Page.class), eq(currentIdentity), eq(false), eq(true));
  }

  @Test(expected = IllegalArgumentException.class)
  public void createPersonalNoteWhenTitleBlankShouldThrowException() throws Exception { // NOSONAR
    tool.createPersonalNote(" ", SUMMARY, CONTENT);
  }

  @Test(expected = IllegalAccessException.class)
  public void createPersonalNoteWhenNoAuthenticatedUserShouldThrowException() throws Exception { // NOSONAR
    NoteMcpTool anonymousTool = new TestableNoteMcpTool() {
      @Override
      public String getCurrentUserName() {
        return null;
      }
    };

    anonymousTool.createPersonalNote(TITLE, SUMMARY, CONTENT);
  }

  @Test
  public void createPersonalNoteShouldCreateNoteAtRootOfOwnNotebook() throws Exception { // NOSONAR
    Wiki wiki = mockPersonalWiki(USER);
    mockPersonalNotesSite();
    Page root = mockPersonalPage("1", "Root", USER);
    Page created = mockPersonalPage(String.valueOf(NOTE_ID), TITLE, USER);

    when(wikiService.getWikiByTypeAndOwner(USER_WIKI_TYPE, USER)).thenReturn(wiki);
    when(wiki.getWikiHome()).thenReturn(root);
    when(noteService.getNoteById("1")).thenReturn(root);
    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    String rootName = root.getName();
    when(noteService.createNote(eq(wiki), eq(rootName), any(Page.class), eq(currentIdentity), eq(false), eq(true)))
                                                                                                                   .thenReturn(created);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.createPersonalNote(TITLE, SUMMARY, CONTENT));

    assertEquals(NOTE_ID, result.noteId());
    assertEquals(TITLE, result.title());
    // A personal note has no permanent link: it is linked under the node
    // hosting the Notes application in the user's personal workspace
    assertEquals("https://meeds.test/portal/myworkspace/dashboard/notes/" + NOTE_ID, result.url());
    ArgumentCaptor<Page> noteCaptor = ArgumentCaptor.forClass(Page.class);
    verify(noteService).createNote(eq(wiki), eq(rootName), noteCaptor.capture(), eq(currentIdentity), eq(false), eq(true));
    Page note = noteCaptor.getValue();
    assertEquals(TITLE, note.getTitle());
    assertEquals(SUMMARY, note.getProperties().getSummary());
    assertEquals(USER, note.getAuthor());
    assertEquals(USER, note.getOwner());
    assertEquals(USER_WIKI_TYPE, note.getWikiType());
    assertEquals(USER, note.getWikiOwner());
    verify(wikiService, never()).createWiki(anyString(), anyString());
  }

  /**
   * A personal note is linked only when a portal site of the user hosts a
   * notes node: without one the url is left out instead of failing the call.
   */
  @Test
  public void getNoteWhenPersonalAndNoSiteHostsNotesShouldReturnNoUrl() throws Exception { // NOSONAR
    Page note = mockPersonalPage(String.valueOf(NOTE_ID), TITLE, USER);
    PortalConfig site = new PortalConfig(PortalConfig.PORTAL_TYPE, "dw");
    UserNode rootNode = mockUserNode("default", null, List.of(mockUserNode("tasks", "tasks", List.of())));

    when(layoutService.getSites(any())).thenReturn(List.of(site));
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "dw", USER, false)).thenReturn(rootNode);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, null));

    assertEquals(NOTE_ID, result.noteId());
    assertNull(result.url());
  }

  /**
   * A site whose navigation can't be read, or that the user can't access, is
   * skipped and the next site still links the personal note.
   */
  @Test
  public void getNoteWhenFirstSitesHaveNoNavigationShouldLinkThroughTheNextSite() throws Exception { // NOSONAR
    Page note = mockPersonalPage(String.valueOf(NOTE_ID), TITLE, USER);
    PortalConfig unreadableSite = new PortalConfig(PortalConfig.PORTAL_TYPE, "dw");
    PortalConfig inaccessibleSite = new PortalConfig(PortalConfig.PORTAL_TYPE, "global");
    PortalConfig personalSite = new PortalConfig(PortalConfig.PORTAL_TYPE, "myworkspace");
    UserNode notesNode = mockUserNode("notes", "dashboard/notes", List.of());
    UserNode dashboardNode = mockUserNode("dashboard", "dashboard", List.of(notesNode));
    UserNode personalRoot = mockUserNode("default", null, List.of(dashboardNode));

    when(layoutService.getSites(any())).thenReturn(List.of(unreadableSite, inaccessibleSite, personalSite));
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "dw", USER, false))
                                                                                          .thenThrow(new IllegalStateException("navigation unavailable"));
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "global", USER, false)).thenReturn(null);
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "myworkspace", USER, false)).thenReturn(personalRoot);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, null));

    assertEquals("https://meeds.test/portal/myworkspace/dashboard/notes/" + NOTE_ID, result.url());
  }

  /**
   * The page hosting the Notes application depends on the caller, not on the
   * note: a search over personal notes looks it up once, however many notes
   * and breadcrumb levels it links.
   */
  @Test
  public void searchNotesWhenPersonalShouldResolveNotesPathOnce() throws Exception { // NOSONAR
    SearchResult parentResult = mock(SearchResult.class);
    SearchResult childResult = mock(SearchResult.class);
    ObjectPageList wikiSearchResult = mock(ObjectPageList.class);
    Page parent = mockPersonalPage(String.valueOf(NOTE_ID), "Parent", USER);
    Page child = mockPersonalPage(String.valueOf(NOTE_ID + 1), "Child", USER);
    lenient().when(child.getParentPageId()).thenReturn(String.valueOf(NOTE_ID));
    mockPersonalNotesSite();

    when(parentResult.getId()).thenReturn(NOTE_ID);
    when(childResult.getId()).thenReturn(NOTE_ID + 1);
    when(wikiSearchResult.getAll()).thenReturn(List.of(parentResult, childResult));
    when(noteService.search(any(WikiSearchData.class))).thenReturn(wikiSearchResult);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(parent);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID + 1), eq(currentIdentity), eq(null), eq(null))).thenReturn(child);
    when(noteService.canViewNote(parent, USER)).thenReturn(true);
    when(noteService.canViewNote(child, USER)).thenReturn(true);

    List<NoteSearchHitModel> result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note",
                                                                                                null,
                                                                                                null,
                                                                                                null,
                                                                                                false)).notes();

    String notesUrl = "https://meeds.test/portal/myworkspace/dashboard/notes/";
    assertEquals(2, result.size());
    assertEquals(notesUrl + NOTE_ID, result.get(0).url());
    assertEquals(notesUrl + (NOTE_ID + 1), result.get(1).url());
    assertNull(result.get(0).space());
    // Two notes linked, one navigation lookup
    verify(layoutService, times(1)).getSites(any());
    verify(portalConfigService, times(1)).getSiteRootNode(PortalConfig.PORTAL_TYPE, "dw", USER, false);
    verify(portalConfigService, times(1)).getSiteRootNode(PortalConfig.PORTAL_TYPE, "myworkspace", USER, false);
  }

  @Test
  public void createPersonalNoteShouldCreateNotebookOnFirstUse() throws Exception { // NOSONAR
    Wiki wiki = mockPersonalWiki(USER);
    Page root = mockPersonalPage("1", "Root", USER);
    Page created = mockPersonalPage(String.valueOf(NOTE_ID), TITLE, USER);

    // Not found on the first lookup, found once created (the second lookup
    // happens when the note is written into the notebook)
    when(wikiService.getWikiByTypeAndOwner(USER_WIKI_TYPE, USER)).thenReturn(null, wiki);
    when(wikiService.createWiki(USER_WIKI_TYPE, USER)).thenReturn(wiki);
    when(wiki.getWikiHome()).thenReturn(root);
    when(noteService.getNoteById("1")).thenReturn(root);
    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    String rootName = root.getName();
    when(noteService.createNote(eq(wiki), eq(rootName), any(Page.class), eq(currentIdentity), eq(false), eq(true)))
                                                                                                                   .thenReturn(created);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result;
    try (MockedStatic<RequestLifeCycle> lifeCycle = mockStatic(RequestLifeCycle.class)) {
      result = runWithStaticMocks(() -> tool.createPersonalNote(TITLE, SUMMARY, CONTENT));
      lifeCycle.verify(RequestLifeCycle::restartTransaction);
    }

    assertEquals(NOTE_ID, result.noteId());
    verify(wikiService).createWiki(USER_WIKI_TYPE, USER);
    verify(wikiService, never()).createWiki(eq(USER_WIKI_TYPE), eq(OTHER_USER));
  }

  /**
   * ACL pin: the target notebook is the authenticated user's, whatever the
   * caller passes. Every free-text argument carries another user's name, and
   * that user's notebook exists: none of the arguments may become the notebook
   * owner, so that user's notebook is never looked up nor written to.
   */
  @Test
  public void createPersonalNoteShouldNeverTargetAnotherUsersNotebook() throws Exception { // NOSONAR
    Wiki ownWiki = mockPersonalWiki(USER);
    Wiki otherWiki = mockPersonalWiki(OTHER_USER);
    Page ownRoot = mockPersonalPage("1", "Root", USER);
    Page otherRoot = mockPersonalPage("2", "Root", OTHER_USER);
    Page created = mockPersonalPage(String.valueOf(NOTE_ID), OTHER_USER, USER);

    when(wikiService.getWikiByTypeAndOwner(USER_WIKI_TYPE, USER)).thenReturn(ownWiki);
    lenient().when(wikiService.getWikiByTypeAndOwner(USER_WIKI_TYPE, OTHER_USER)).thenReturn(otherWiki);
    when(ownWiki.getWikiHome()).thenReturn(ownRoot);
    lenient().when(otherWiki.getWikiHome()).thenReturn(otherRoot);
    when(noteService.getNoteById("1")).thenReturn(ownRoot);
    lenient().when(noteService.getNoteById("2")).thenReturn(otherRoot);
    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    when(noteService.createNote(any(Wiki.class), anyString(), any(Page.class), eq(currentIdentity), eq(false), eq(true)))
                                                                                                                       .thenReturn(created);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    runWithStaticMocks(() -> tool.createPersonalNote(OTHER_USER, OTHER_USER, OTHER_USER));

    verify(wikiService, never()).getWikiByTypeAndOwner(USER_WIKI_TYPE, OTHER_USER);
    verify(wikiService, never()).createWiki(anyString(), anyString());
    verify(noteService, never()).createNote(eq(otherWiki), anyString(), any(Page.class), any(), anyBoolean(), anyBoolean());
    String ownRootName = ownRoot.getName();
    ArgumentCaptor<Page> noteCaptor = ArgumentCaptor.forClass(Page.class);
    verify(noteService).createNote(eq(ownWiki), eq(ownRootName), noteCaptor.capture(), eq(currentIdentity), eq(false), eq(true));
    assertEquals(USER, noteCaptor.getValue().getWikiOwner());
    assertEquals(USER, noteCaptor.getValue().getOwner());
    assertEquals(USER, noteCaptor.getValue().getAuthor());
  }

  @Test(expected = IllegalArgumentException.class)
  public void createChildNoteWhenParentNoteIdNullShouldThrowException() throws Exception { // NOSONAR
    tool.createChildNote(null, TITLE, SUMMARY, CONTENT);
  }

  @Test
  public void createChildNoteShouldCreateNoteUnderParentNote() throws Exception { // NOSONAR
    Page parent = mockPage(String.valueOf(PARENT_NOTE_ID), "Parent");
    Page created = mockPage(String.valueOf(NOTE_ID), TITLE);
    Wiki wiki = mockWiki();

    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(parent);
    when(noteService.canViewNote(parent, USER)).thenReturn(true);
    when(wikiService.getWikiByTypeAndOwner(parent.getWikiType(), parent.getWikiOwner())).thenReturn(wiki);
    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    String parentName = parent.getName();
    when(noteService.createNote(eq(wiki), eq(parentName), any(Page.class), eq(currentIdentity), eq(false), eq(true)))
                                                                                                                     .thenReturn(created);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.createChildNote(PARENT_NOTE_ID, TITLE, SUMMARY, CONTENT));

    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).createNote(eq(wiki), eq(parentName), any(Page.class), eq(currentIdentity), eq(false), eq(true));
  }

  @Test(expected = IllegalAccessException.class)
  public void updateNoteWhenUserCannotEditShouldThrowException() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(false);

    tool.updateNote(NOTE_ID, OTHER_TITLE, CONTENT, null);
  }

  @Test
  public void updateNoteShouldUpdateTitleAndContent() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.updateNote(eq(note), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                               .thenReturn(note);

    NoteModel result = runWithStaticMocks(() -> tool.updateNote(NOTE_ID, OTHER_TITLE, CONTENT, null));

    assertEquals(NOTE_ID, result.noteId());
    verify(note).setTitle(OTHER_TITLE);
    verify(note, atLeastOnce()).setContent("<p>Content</p>");
    verify(noteService).createVersionOfNote(note, USER, true);
    verify(noteService).removeDraftOfNote(any(WikiPageParams.class), eq(null));
  }

  @Test
  public void updateNoteWithLanguageShouldSetLanguage() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    lenient().when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.updateNote(eq(note), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                               .thenReturn(note);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, "FR title", "FR content", "fr"));

    verify(note).setLang("fr");
    verify(noteService).removeDraftOfNote(any(WikiPageParams.class), eq("fr"));
  }

  // Regression for EXO-88373: editing a translation must not clobber that
  // translation's OWN cover with the default's. The "fr" version already has its
  // own distinct featured image (600); updating it in "fr" must keep 600, never
  // copy the default's (500).
  @Test
  public void updateNoteWithLanguageShouldPreserveExistingTranslationCover() throws Exception { // NOSONAR
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    NotePageProperties frProperties = new NotePageProperties();
    NoteFeaturedImage frCover = new NoteFeaturedImage();
    frCover.setId(600L);
    frProperties.setFeaturedImage(frCover);
    lenient().when(frNote.getProperties()).thenReturn(frProperties);

    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties defaultProperties = new NotePageProperties();
    NoteFeaturedImage defaultCover = new NoteFeaturedImage();
    defaultCover.setId(500L);
    defaultProperties.setFeaturedImage(defaultCover);
    lenient().when(defaultNote.getProperties()).thenReturn(defaultProperties);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(defaultNote, USER)).thenReturn(true);
    when(noteService.canEditNote(defaultNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.updateNote(eq(defaultNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                                     .thenReturn(defaultNote);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, null, null, "fr"));

    verify(defaultNote).setProperties(captor.capture());
    assertEquals(Long.valueOf(600L), captor.getValue().getFeaturedImage().getId());
  }

  // A brand-new translation (no per-language properties yet) still inherits the
  // default's cover (500).
  @Test
  public void updateNoteWithLanguageNewTranslationShouldInheritDefaultCover() throws Exception { // NOSONAR
    // no "fr" version yet: getNoteByIdAndLang falls back to the default page and
    // leaves its lang unset, which is how a brand-new translation is recognised
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(frNote.getProperties()).thenReturn(null);

    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties defaultProperties = new NotePageProperties();
    NoteFeaturedImage defaultCover = new NoteFeaturedImage();
    defaultCover.setId(500L);
    defaultProperties.setFeaturedImage(defaultCover);
    lenient().when(defaultNote.getProperties()).thenReturn(defaultProperties);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(defaultNote, USER)).thenReturn(true);
    when(noteService.canEditNote(defaultNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.updateNote(eq(defaultNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                                     .thenReturn(defaultNote);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, null, null, "fr"));

    verify(defaultNote).setProperties(captor.capture());
    assertEquals(Long.valueOf(500L), captor.getValue().getFeaturedImage().getId());
  }

  // EXO-90294: a blank language is the note's OWN default version. The read
  // used to fall back to the caller's profile locale, so an update with no
  // language wrote that translation's text onto the main note.
  @Test
  public void updateNoteWithoutLanguageShouldUpdateTheDefaultNoteNotTheCallerLocaleTranslation() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    // the caller's profile locale is "en" and the note has an "en" translation
    Page localeNote = mockPage(String.valueOf(NOTE_ID), "Note EN");
    lenient().when(localeNote.getLang()).thenReturn("en");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    lenient().when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(localeNote);
    when(noteService.canViewNote(defaultNote, USER)).thenReturn(true);
    when(noteService.canEditNote(defaultNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(localeNote, USER)).thenReturn(true);
    lenient().when(noteService.canEditNote(localeNote, USER)).thenReturn(true);
    when(noteService.updateNote(eq(defaultNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                                     .thenReturn(defaultNote);
    lenient().when(noteService.updateNote(eq(localeNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                                              .thenReturn(localeNote);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, OTHER_TITLE, null, null));

    verify(defaultNote).setTitle(OTHER_TITLE);
    verify(noteService).updateNote(eq(defaultNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity));
    verify(noteService).createVersionOfNote(defaultNote, USER, true);
    verify(localeNote, never()).setTitle(anyString());
    verify(noteService, never()).updateNote(eq(localeNote), any(PageUpdateType.class), eq(currentIdentity));
  }

  // EXO-90294: building a translation can take two calls; the second must start
  // from the translation the first wrote, not from the default note.
  @Test
  public void updateNoteWithLanguageShouldKeepTheTranslationsOwnTitleWhenOnlyTheContentChanges() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Titre FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getContent()).thenReturn("<p>contenu</p>");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(defaultNote, USER)).thenReturn(true);
    when(noteService.canEditNote(defaultNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.updateNote(eq(defaultNote), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                                     .thenReturn(defaultNote);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, null, "nouveau contenu", "fr"));

    // the version write carries the translation's own title, and the new content
    verify(defaultNote).setTitle("Titre FR");
    verify(defaultNote).setContent("<p>nouveau contenu</p>");
    verify(defaultNote).setLang("fr");
    verify(noteService).createVersionOfNote(defaultNote, USER, true);
  }

  @Test
  public void publishNoteShouldPublishAndReturnActivity() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    ActivityModel activityModel = mock(ActivityModel.class);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.updateNote(eq(note), eq(PageUpdateType.PUBLISH), eq(currentIdentity))).thenReturn(note);

    ActivityModel result = runWithStaticMocksWithActivity(activityModel, () -> tool.publishNote(NOTE_ID));

    assertNotNull(result);
    verify(note).setToBePublished(true);
    verify(noteService).updateNote(note, PageUpdateType.PUBLISH, currentIdentity);
  }

  @Test
  public void deleteNoteShouldDeleteNote() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    tool.deleteNote(NOTE_ID);

    verify(noteService).deleteNote(String.valueOf(NOTE_ID));
  }

  /**
   * A note moves neither under itself nor under one of its sub-notes, at any
   * depth, on any call: the storage would
   * re-parent it into a cycle. A tree that already holds a cycle above the
   * new parent ends the walk. A new parent outside the note's subtree is
   * accepted.
   * Mutants: the check removed, the walk stopped at the new parent, or the
   * visited set removed (the cyclic tree never ends).
   *
   * @throws Exception never
   */
  @Test(timeout = 10000)
  public void aNoteNeverMovesUnderItselfOrItsSubNotes() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    Page child = mockPage("20", "Child");
    lenient().when(child.getParentPageId()).thenReturn(String.valueOf(NOTE_ID));
    Page grandChild = mockPage("21", "Grand child");
    lenient().when(grandChild.getParentPageId()).thenReturn("20");
    Page loopA = mockPage("22", "Loop A");
    lenient().when(loopA.getParentPageId()).thenReturn("23");
    Page loopB = mockPage("23", "Loop B");
    lenient().when(loopB.getParentPageId()).thenReturn("22");
    Page sibling = mockPage(String.valueOf(PARENT_NOTE_ID), "Sibling");
    lenient().when(sibling.getParentPageId()).thenReturn("1");
    Page root = mockPage("1", "Root");
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(20L), eq(currentIdentity), eq(null), eq(null))).thenReturn(child);
    when(noteService.getNoteByIdAndLang(eq(21L), eq(currentIdentity), eq(null), eq(null))).thenReturn(grandChild);
    when(noteService.getNoteByIdAndLang(eq(22L), eq(currentIdentity), eq(null), eq(null))).thenReturn(loopA);
    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(sibling);
    lenient().when(noteService.getNoteById(String.valueOf(NOTE_ID))).thenReturn(note);
    lenient().when(noteService.getNoteById("20")).thenReturn(child);
    lenient().when(noteService.getNoteById("22")).thenReturn(loopA);
    lenient().when(noteService.getNoteById("23")).thenReturn(loopB);
    lenient().when(noteService.getNoteById("1")).thenReturn(root);
    when(noteService.canViewNote(any(Page.class), eq(USER))).thenReturn(true);
    when(noteService.canEditNote(any(Page.class), eq(USER))).thenReturn(true);

    for (long target : new long[] { NOTE_ID, 20L, 21L }) {
      assertThrows(String.valueOf(target), IllegalArgumentException.class, () -> tool.moveNote(NOTE_ID, target));
    }
    verify(noteService, never()).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), any());

    tool.moveNote(NOTE_ID, 22L);
    tool.moveNote(NOTE_ID, PARENT_NOTE_ID);
    verify(noteService, times(2)).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), eq(currentIdentity));
  }

  /**
   * The space a call is limited to is the one its acting identity carries,
   * none for an identity without a scope or no identity, and an identity that
   * can't be read fails the read rather than passing for an unlimited call.
   * Runs the real {@code getScopeSpaceId}, which the other tests stub.
   * Mutants: the scope not read, or the read failure swallowed.
   */
  @Test
  public void theScopeSpaceIsTheOneTheActingIdentityCarries() { // NOSONAR
    NoteMcpTool realTool = new NoteMcpTool(wikiService,
                                           noteService,
                                           activityManager,
                                           identityManager,
                                           i18NActivityProcessor,
                                           spaceService,
                                           translationService,
                                           profilePropertyService,
                                           userAcl,
                                           portalConfigService,
                                           permanentLinkService,
                                           uploadService,
                                           attachmentService,
                                           fileService,
                                           layoutService);
    ActingIdentity limited = mock(ActingIdentity.class);
    when(limited.spaceScope()).thenReturn(7L);
    ActingIdentity unlimited = mock(ActingIdentity.class);
    when(unlimited.spaceScope()).thenReturn(null);
    try (MockedStatic<McpToolUtils> mocked = mockStatic(McpToolUtils.class)) {
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(limited);
      assertEquals(Long.valueOf(7L), realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(unlimited);
      assertNull(realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenReturn(null);
      assertNull(realTool.getScopeSpaceId());
      mocked.when(McpToolUtils::getCurrentActingIdentity).thenThrow(new IllegalStateException("refused"));
      assertThrows(IllegalStateException.class, realTool::getScopeSpaceId);
    }
  }

  /**
   * In a call limited to a space, a note moves only under a new parent of
   * that space: a parent in another space's notebook or in a personal one is
   * refused, and so is any move when the call's identity can't be read. A
   * call that isn't limited moves the note wherever the user can edit.
   * Mutants: the check removed, the comparison inverted, an unreadable
   * identity taken as unlimited, or an unlimited call checked.
   *
   * @throws Exception never
   */
  @Test
  public void aMoveLimitedToASpaceKeepsTheNoteInIt() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    Page inSpace = mockPage(String.valueOf(PARENT_NOTE_ID), "Target");
    Page otherSpace = mockPage("14", "Elsewhere");
    lenient().when(otherSpace.getWikiOwner()).thenReturn("/spaces/other");
    Page personal = mockPage("15", "Mine");
    lenient().when(personal.getWikiType()).thenReturn(USER_WIKI_TYPE);
    lenient().when(personal.getWikiOwner()).thenReturn(USER);
    Space space = mock(Space.class);
    when(space.getId()).thenReturn(String.valueOf(SPACE_ID));
    Space other = mock(Space.class);
    when(other.getId()).thenReturn("34");
    when(spaceService.getSpaceByGroupId(GROUP_ID)).thenReturn(space);
    when(spaceService.getSpaceByGroupId("/spaces/other")).thenReturn(other);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(inSpace);
    when(noteService.getNoteByIdAndLang(eq(14L), eq(currentIdentity), eq(null), eq(null))).thenReturn(otherSpace);
    when(noteService.getNoteByIdAndLang(eq(15L), eq(currentIdentity), eq(null), eq(null))).thenReturn(personal);
    when(noteService.canViewNote(any(Page.class), eq(USER))).thenReturn(true);
    when(noteService.canEditNote(any(Page.class), eq(USER))).thenReturn(true);

    currentScopeSpaceId = SPACE_ID;
    assertThrows(IllegalAccessException.class, () -> tool.moveNote(NOTE_ID, 14L));
    assertThrows(IllegalAccessException.class, () -> tool.moveNote(NOTE_ID, 15L));
    scopeUnreadable = true;
    assertThrows(IllegalAccessException.class, () -> tool.moveNote(NOTE_ID, PARENT_NOTE_ID));
    verify(noteService, never()).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), any());

    scopeUnreadable = false;
    tool.moveNote(NOTE_ID, PARENT_NOTE_ID);
    verify(noteService, times(1)).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), eq(currentIdentity));

    currentScopeSpaceId = null;
    tool.moveNote(NOTE_ID, 14L);
    verify(noteService, times(2)).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), eq(currentIdentity));
  }

  @Test
  public void moveNoteShouldMoveNote() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    Page target = mockPage(String.valueOf(PARENT_NOTE_ID), "Target");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(target);
    when(noteService.canViewNote(any(Page.class), eq(USER))).thenReturn(true);
    when(noteService.canEditNote(any(Page.class), eq(USER))).thenReturn(true);

    tool.moveNote(NOTE_ID, PARENT_NOTE_ID);

    verify(noteService).moveNote(any(WikiPageParams.class), any(WikiPageParams.class), eq(currentIdentity));
  }

  @Test
  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  public void searchNotesShouldReturnMappedNotes() throws Exception { // NOSONAR
    SearchResult searchResult = mock(SearchResult.class);
    ObjectPageList wikiSearchResult = mock(ObjectPageList.class);
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(searchResult.getId()).thenReturn(NOTE_ID);
    when(wikiSearchResult.getAll()).thenReturn(Collections.singletonList(searchResult));
    when(noteService.search(any(WikiSearchData.class))).thenReturn(wikiSearchResult);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    Space space = mockSpace();
    when(spaceService.getSpaceByGroupId(GROUP_ID)).thenReturn(space);

    List<NoteSearchHitModel> result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note",
                                                                                                SPACE_ID,
                                                                                                null,
                                                                                                null,
                                                                                                true)).notes();

    assertEquals(1, result.size());
    assertEquals(NOTE_ID, result.get(0).noteId());
    verify(noteService).search(any(WikiSearchData.class));
  }

  /**
   * Runs {@code searchNotes} with the given query and returns the
   * {@link WikiSearchData} handed to the note service.
   */
  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  private WikiSearchData searchAndCaptureData(String query) throws Exception { // NOSONAR
    ObjectPageList wikiSearchResult = mock(ObjectPageList.class);
    when(wikiSearchResult.getAll()).thenReturn(Collections.emptyList());
    when(noteService.search(any(WikiSearchData.class))).thenReturn(wikiSearchResult);
    runWithConversationAndStaticMocks(() -> tool.searchNotes(query, null, null, null, null));
    ArgumentCaptor<WikiSearchData> captor = ArgumentCaptor.forClass(WikiSearchData.class);
    verify(noteService).search(captor.capture());
    return captor.getValue();
  }

  /**
   * A blank query has no relevance score to rank on: the most recently
   * updated notes must come first.
   */
  @Test
  public void searchNotesWithBlankQueryShouldSortByDateDescending() throws Exception { // NOSONAR
    for (String blank : new String[] { "", "   ", null }) {
      clearInvocations(noteService);
      WikiSearchData data = searchAndCaptureData(blank);
      assertEquals("date", data.getSortField());
      assertEquals("desc", data.getSortDirection());
    }
  }

  /**
   * A non-blank query keeps the relevance order and is passed as typed. The
   * connector makes each word a wildcard term, which Elasticsearch normalises
   * to lower case (Pricing*, pricing* and PRICING* match the same notes on
   * Elasticsearch 9.4.5); an OR becomes the required word OR*, so the tool
   * description asks for keywords without boolean syntax.
   */
  @Test
  public void searchNotesWithQueryShouldKeepRelevanceOrderAndCase() throws Exception { // NOSONAR
    WikiSearchData data = searchAndCaptureData("Pricing OR Pricing decisions");
    assertNull(data.getSortField());
    assertNull(data.getSortDirection());
    assertEquals("Pricing OR Pricing decisions", data.getContent());
    assertEquals("Pricing OR Pricing decisions", data.getTitle());
  }

  @Test
  public void getNoteVersionsShouldReturnVersionList() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    PageHistory version = mock(PageHistory.class);
    lenient().when(version.getVersionNumber()).thenReturn(1L);
    lenient().when(version.getId()).thenReturn("12-v1");
    lenient().when(version.getTitle()).thenReturn(TITLE);
    lenient().when(version.getContent()).thenReturn("<p>old</p>");
    lenient().when(version.getUpdatedDate()).thenReturn(new Date());
    lenient().when(version.getAuthor()).thenReturn(USER);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(version));

    List<NoteVersionModel> result = runWithStaticMocks(() -> tool.getNoteVersions(NOTE_ID, null, null, null)).versions();

    assertEquals(1, result.size());
    assertEquals(Long.valueOf(1L), result.get(0).versionNumber());
    assertEquals("12-v1", result.get(0).versionId());
  }

  @Test
  public void restoreNoteVersionShouldPassTheVersionNumberNotThePageName() throws Exception { // NOSONAR
    Page note = restorablePage();
    PageHistory version = pageHistory(note, 2L, "Old title", "<p>old</p>");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(version));

    runWithStaticMocks(() -> tool.restoreNoteVersion(NOTE_ID, 2L, null));

    verify(noteService).restoreVersionOfNote("2", note, USER);
  }

  @Test
  public void restoreNoteVersionShouldPublishTheRestoredTitleAndContent() throws Exception { // NOSONAR
    Page note = restorablePage();
    PageHistory version = pageHistory(note, 2L, "Old title", "<p>old</p>");

    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(version));

    ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);

    runWithStaticMocks(() -> tool.restoreNoteVersion(NOTE_ID, 2L, null));

    verify(noteService).restoreVersionOfNote(eq("2"), captor.capture(), eq(USER));
    assertEquals("Old title", captor.getValue().getTitle());
    assertEquals("<p>old</p>", captor.getValue().getContent());
  }

  @Test
  public void restoreNoteVersionWithoutLanguageShouldUseTheDefaultVersion() throws Exception { // NOSONAR
    Page note = restorablePage();
    // a lang carried by the loaded page must not redirect the restore to that
    // translation: a blank language means the note's own default version
    note.setLang("fr");
    PageHistory version = pageHistory(note, 2L, "Old title", "<p>old</p>");

    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(version));
    lenient().when(noteService.getVersionsHistoryOfNoteByLang(note, USER, "fr")).thenReturn(Collections.emptyList());

    runWithStaticMocks(() -> tool.restoreNoteVersion(NOTE_ID, 2L, null));

    verify(noteService).getVersionsHistoryOfNoteByLang(note, USER, null);
    verify(noteService).restoreVersionOfNote("2", note, USER);
  }

  @Test
  public void getNoteVersionsWithoutLanguageShouldListTheDefaultVersion() throws Exception { // NOSONAR
    Page note = restorablePage();
    // a lang carried by the loaded page must not redirect the listing to that
    // translation: a blank language means the note's own default version
    note.setLang("fr");
    PageHistory version = pageHistory(note, 2L, "Old title", "<p>old</p>");

    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(version));
    lenient().when(noteService.getVersionsHistoryOfNoteByLang(note, USER, "fr")).thenReturn(Collections.emptyList());

    List<NoteVersionModel> result = runWithStaticMocks(() -> tool.getNoteVersions(NOTE_ID, null, null, null)).versions();

    assertEquals(1, result.size());
    assertEquals(Long.valueOf(2L), result.get(0).versionNumber());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void restoreNoteVersionWhenVersionMissingShouldThrow() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(Collections.emptyList());

    tool.restoreNoteVersion(NOTE_ID, 99L, null);
  }

  @Test
  public void setNoteCoverFromBase64ShouldSaveMetadata() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", null));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), any(), eq(42L));
  }

  // Regression for EXO-88373: setting a cover on the "fr" translation must base
  // the write on the "fr" metadata, so the translation's OWN summary is
  // preserved rather than overwritten with the default/en one.
  @Test
  public void setNoteCoverInLanguageShouldPreserveTranslationSummary() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties enProperties = new NotePageProperties();
    enProperties.setSummary("en summary");
    lenient().when(note.getProperties()).thenReturn(enProperties);

    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    NotePageProperties frProperties = new NotePageProperties();
    frProperties.setSummary("fr summary");
    lenient().when(frNote.getProperties()).thenReturn(frProperties);

    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    // the note is loaded, written and read back in the target language ("fr"):
    // the default version is never read on this path
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "fr"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(42L));
    assertEquals("fr summary", captor.getValue().getSummary());
  }

  @Test
  public void setNoteSummaryShouldSaveMetadata() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    runWithStaticMocks(() -> tool.setNoteSummary(NOTE_ID, "new summary", null));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), any(), eq(42L));
  }

  // EXO-90294: createVersionOfNote re-saves the note's own properties under its
  // own language, so it must carry the ones just saved -- it used to revert them.
  @Test
  public void setNoteSummaryInLanguageShouldNotRevertTheSavedMetadata() throws Exception { // NOSONAR
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    NotePageProperties frProperties = new NotePageProperties();
    frProperties.setSummary("ancien resume");
    lenient().when(frNote.getProperties()).thenReturn(frProperties);
    // the second read returns a distinct Page in production; that aliasing is why
    // the version write used to revert the summary, so the stub models it
    Page frReread = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frReread.getLang()).thenReturn("fr");
    NotePageProperties frRereadProperties = new NotePageProperties();
    frRereadProperties.setSummary("ancien resume");
    lenient().when(frReread.getProperties()).thenReturn(frRereadProperties);
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote, frReread);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(frReread, USER)).thenReturn(true);
    lenient().when(noteService.canEditNote(frReread, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.setNoteSummary(NOTE_ID, "nouveau resume", "fr"));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("fr"), eq(42L));
    verify(frNote).setProperties(captor.capture());
    assertEquals("nouveau resume", captor.getValue().getSummary());
    verify(noteService).createVersionOfNote(frNote, USER, true);
  }

  // EXO-90294: a cover set on a language with no version yet was written as a
  // DEFAULT version, overwriting the main note's metadata.
  @Test
  public void setNoteCoverOnANewTranslationShouldTagTheVersionWithThatLanguage() throws Exception { // NOSONAR
    // no "de" version yet: getNoteByIdAndLang returns the default page, lang unset
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("de"))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "de"));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("de"), eq(42L));
    verify(note).setLang("de");
    verify(noteService).createVersionOfNote(note, USER, true);
  }

  // EXO-90294: a language with no version of its own inherits the default's
  // cover id; reusing it replaces the default note's binary in place.
  @Test
  public void setNoteCoverOnAnInheritedCoverShouldNotReuseTheDefaultsFileId() throws Exception { // NOSONAR
    // the note has no "de" version, so getNoteByIdAndLang returns the default
    // page (lang unset) carrying the DEFAULT's cover
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(note.getProperties()).thenReturn(coverProperties(500L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("de"))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "de"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("de"), eq(42L));
    // no id: saveNoteFeaturedImage writes a NEW file for this translation
    assertNull(captor.getValue().getFeaturedImage().getId());
  }

  // EXO-90294: a translation may have a version of its OWN and still name the
  // default's file, so "does a version exist" is the wrong question; the id is.
  @Test
  public void setNoteCoverOnATranslationSharingTheDefaultsFileShouldNotReuseTheFileId() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties defaultProperties = coverProperties(500L);
    lenient().when(defaultNote.getProperties()).thenReturn(defaultProperties);
    // the "fr" version exists, but its metadata still points at the default's file
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(500L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "fr"));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(42L));
    assertNull(captor.getValue().getFeaturedImage().getId());
  }

  // Same sequence on the removal side: the fr version exists but shares the
  // default's file, so deleting it would destroy the default note's image.
  @Test
  public void removeNoteCoverOnATranslationSharingTheDefaultsFileShouldRefuse() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(defaultNote.getProperties()).thenReturn(coverProperties(500L));
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(500L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    lenient().when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    try {
      runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, "fr"));
      fail("removing a cover shared with the default note must not be accepted");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("fr"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: the code is normalized once at the tool boundary, so the metadata,
  // the ownership verdict and the write cannot resolve to different languages.
  @Test
  public void setNoteCoverWithAPaddedUpperCaseLanguageShouldTargetTheSameTranslation() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(defaultNote.getProperties()).thenReturn(coverProperties(500L));
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(600L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    // decoy answering the RAW code, so dropping the normalization fails on the
    // assertion below rather than on an unresolvable note
    Page rawCodeNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(rawCodeNote.getLang()).thenReturn("  FR  ");
    lenient().when(rawCodeNote.getProperties()).thenReturn(coverProperties(700L));
    lenient().when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("  FR  ")))
             .thenReturn(rawCodeNote);
    lenient().when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("FR")))
             .thenReturn(rawCodeNote);
    lenient().when(noteService.canViewNote(rawCodeNote, USER)).thenReturn(true);
    lenient().when(noteService.canEditNote(rawCodeNote, USER)).thenReturn(true);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<NotePageProperties> captor = ArgumentCaptor.forClass(NotePageProperties.class);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "  FR  "));

    verify(noteService).saveNoteMetadata(captor.capture(), eq("fr"), eq(42L));
    // the translation's OWN file is replaced in place, as for a plain "fr"
    assertEquals(Long.valueOf(600L), captor.getValue().getFeaturedImage().getId());
    verify(frNote).setLang("fr");
  }

  // EXO-90294: without the default's metadata, ownership is unknowable and this
  // guard's callers delete or replace the file, so a failed read must refuse.
  @Test
  public void removeNoteCoverShouldRefuseWhenTheDefaultVersionCannotBeRead() throws Exception { // NOSONAR
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(500L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null)))
                                                                                              .thenThrow(new WikiException("storage down"));
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    // the destructive path is stubbed so the mutant fails on the deletion
    lenient().when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    try {
      runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, "fr"));
      fail("a cover whose ownership could not be established must not be deleted");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("fr"));
      // the message must say what actually happened: the tool could not tell
      // whose file it is, NOT that the cover belongs to the default note --
      // and it must not suggest remedies that would fail for the same reason
      assertTrue(e.getMessage().contains("could not be read"));
      assertFalse(e.getMessage().contains("set_note_cover"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // EXO-90294: getNoteByIdAndLang returns null for a missing page rather than
  // throwing, so a null read is as blind as a throw and must refuse too.
  @Test
  public void removeNoteCoverShouldRefuseWhenTheDefaultVersionReadsNull() throws Exception { // NOSONAR
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(500L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(null);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    // the destructive path is stubbed so the mutant fails on the deletion
    lenient().when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    try {
      runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, "fr"));
      fail("a cover whose ownership could not be established must not be deleted");
    } catch (IllegalStateException e) {
      assertTrue(e.getMessage().contains("could not be read"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  // A translation that owns a DISTINCT cover file may of course remove it.
  @Test
  public void removeNoteCoverOnATranslationWithItsOwnFileShouldRemoveIt() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(defaultNote.getProperties()).thenReturn(coverProperties(500L));
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    lenient().when(frNote.getProperties()).thenReturn(coverProperties(600L));
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, "fr"));

    verify(noteService).removeNoteFeaturedImage(eq(NOTE_ID), eq(600L), eq("fr"), eq(false), eq(42L));
  }

  // EXO-90294: on the removal side the same inheritance destroys data --
  // removeNoteFeaturedImage(isDraft=false) deletes the file unconditionally.
  @Test
  public void removeNoteCoverOnAnInheritedCoverShouldRefuseInsteadOfDeletingTheDefaultsFile() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(note.getProperties()).thenReturn(coverProperties(500L));

    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("de"))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    // the unguarded path is stubbed so the mutant fails on the deletion
    lenient().when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    try {
      runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, "de"));
      fail("removing an inherited cover must not be accepted");
    } catch (IllegalArgumentException e) {
      assertTrue(e.getMessage().contains("de"));
    }
    verify(noteService, never()).removeNoteFeaturedImage(anyLong(), anyLong(), anyString(), anyBoolean(), anyLong());
  }

  @Test(expected = ObjectNotFoundException.class)
  public void removeNoteCoverWhenNoCoverShouldThrow() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    tool.removeNoteCover(NOTE_ID, null);
  }

  @Test
  public void removeNoteCoverShouldRemoveExistingCover() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties properties = new NotePageProperties();
    NoteFeaturedImage featuredImage = new NoteFeaturedImage();
    featuredImage.setId(77L);
    properties.setFeaturedImage(featuredImage);
    lenient().when(note.getProperties()).thenReturn(properties);
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, null));

    verify(noteService).removeNoteFeaturedImage(eq(NOTE_ID), eq(77L), any(), eq(false), eq(42L));
  }

  // Regression for EXO-88373 (critical): removing a cover must not resurrect it.
  // The in-memory featured image on the note must be cleared BEFORE the version
  // resave, otherwise createVersionOfNote re-runs saveNoteMetadata with the stale
  // id-only image and re-adds the just-deleted cover file.
  @Test
  public void removeNoteCoverShouldNotResurrectRemovedCover() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    NotePageProperties properties = new NotePageProperties();
    NoteFeaturedImage featuredImage = new NoteFeaturedImage();
    featuredImage.setId(77L);
    properties.setFeaturedImage(featuredImage);
    lenient().when(note.getProperties()).thenReturn(properties);
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);

    runWithStaticMocks(() -> tool.removeNoteCover(NOTE_ID, null));

    verify(noteService).createVersionOfNote(captor.capture(), eq(USER), eq(true));
    assertNotNull(captor.getValue().getProperties());
    assertNull(captor.getValue().getProperties().getFeaturedImage());
  }

  // Regression for EXO-88373 (high): the language param must drive the version
  // write, not just the property save. setNoteCover("fr") must tag the new
  // version under "fr", where get_note_versions(..., "fr") can find it.
  @Test
  public void setNoteCoverShouldTagVersionWithRequestedLanguage() throws Exception { // NOSONAR
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    NotePageProperties frProperties = new NotePageProperties();
    frProperties.setSummary("fr summary");
    lenient().when(frNote.getProperties()).thenReturn(frProperties);
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    when(identityManager.getOrCreateUserIdentity(USER)).thenReturn(userIdentity);

    ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);

    runWithStaticMocks(() -> tool.setNoteCover(NOTE_ID, null, PNG_1PX, null, null, "cover", "fr"));

    verify(noteService).saveNoteMetadata(any(NotePageProperties.class), eq("fr"), eq(42L));
    verify(noteService).createVersionOfNote(captor.capture(), eq(USER), eq(true));
    assertEquals("fr", captor.getValue().getLang());
  }

  // Regression for EXO-88373 (high): restore_note_version must restore the note
  // in the REQUESTED language, so the restored version lands under "fr" and not
  // the caller's current "en" locale.
  @Test
  public void restoreNoteVersionShouldUseRequestedLanguageNote() throws Exception { // NOSONAR
    // "en" is the caller's current locale; the note must be loaded in the requested "fr"
    Page enNote = mockPage(String.valueOf(NOTE_ID), "Note EN");
    lenient().when(enNote.getLang()).thenReturn("en");
    Page frNote = mockPage(String.valueOf(NOTE_ID), "Note FR");
    lenient().when(frNote.getLang()).thenReturn("fr");
    PageHistory version = mock(PageHistory.class);
    lenient().when(version.getVersionNumber()).thenReturn(2L);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(frNote);
    lenient().when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(enNote);
    when(noteService.canViewNote(frNote, USER)).thenReturn(true);
    when(noteService.canEditNote(frNote, USER)).thenReturn(true);
    lenient().when(noteService.canViewNote(enNote, USER)).thenReturn(true);
    lenient().when(noteService.canEditNote(enNote, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(frNote, USER, "fr")).thenReturn(List.of(version));
    lenient().when(noteService.getVersionsHistoryOfNoteByLang(enNote, USER, "fr")).thenReturn(List.of(version));

    ArgumentCaptor<Page> captor = ArgumentCaptor.forClass(Page.class);

    NoteModel result = runWithStaticMocks(() -> tool.restoreNoteVersion(NOTE_ID, 2L, "fr"));

    verify(noteService).restoreVersionOfNote(eq("2"), captor.capture(), eq(USER));
    assertEquals("fr", captor.getValue().getLang());
    assertEquals("Note FR", result.title());
  }

  @Test
  public void getNoteTranslationsShouldReturnLanguages() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getPageAvailableTranslationLanguages(NOTE_ID, false)).thenReturn(List.of("en", "fr"));

    List<String> languages = tool.getNoteTranslations(NOTE_ID);

    assertEquals(List.of("en", "fr"), languages);
  }

  @Test
  public void removeNoteTranslationShouldRemoveExistingTranslation() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getPageAvailableTranslationLanguages(NOTE_ID, false)).thenReturn(List.of("en", "fr"));

    NoteModel result = runWithStaticMocks(() -> tool.removeNoteTranslation(NOTE_ID, "fr"));

    assertNotNull(result);
    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).deleteVersionsByNoteIdAndLang(NOTE_ID, "fr");
  }

  @Test(expected = IllegalArgumentException.class)
  public void removeNoteTranslationWhenLanguageBlankShouldThrow() throws Exception { // NOSONAR
    tool.removeNoteTranslation(NOTE_ID, " ");
  }

  @Test(expected = IllegalAccessException.class)
  public void removeNoteTranslationWhenUserCannotEditShouldThrow() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(false);

    tool.removeNoteTranslation(NOTE_ID, "fr");
  }

  // The default's own code can never appear in the translation list --
  // getPageAvailableTranslationLanguages filters `AND p.lang IS NOT NULL`
  // (PageVersionEntity) -- so it is the membership check that rejects it, and
  // that is all this pin exercises: there is no default-language branch left.
  @Test(expected = ObjectNotFoundException.class)
  public void removeNoteTranslationOfALanguageNotInTheListShouldThrow() throws Exception { // NOSONAR
    Page defaultNote = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(defaultNote);
    when(noteService.canViewNote(defaultNote, USER)).thenReturn(true);
    when(noteService.canEditNote(defaultNote, USER)).thenReturn(true);
    when(noteService.getPageAvailableTranslationLanguages(NOTE_ID, false)).thenReturn(List.of("fr"));

    tool.removeNoteTranslation(NOTE_ID, "en");
  }

  @Test(expected = ObjectNotFoundException.class)
  public void removeNoteTranslationWhenTranslationMissingShouldThrow() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.getPageAvailableTranslationLanguages(NOTE_ID, false)).thenReturn(List.of("fr"));

    tool.removeNoteTranslation(NOTE_ID, "de");
  }

  @Test
  public void getNoteInLanguageShouldReadRequestedLanguage() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, "fr", null, null, null));

    assertNotNull(result);
    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("fr"));
  }

  /**
   * A search hit is compact: the note's id, title, summary, URL, update date
   * and space, never its content, its users or its breadcrumb, and building
   * it reads neither the edit permission nor the note's ancestors.
   */
  @Test
  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  public void searchNotesShouldReturnCompactHitsWithoutContent() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    lenient().when(note.getParentPageId()).thenReturn(String.valueOf(PARENT_NOTE_ID));
    NotePageProperties properties = new NotePageProperties();
    properties.setSummary(SUMMARY);
    when(note.getProperties()).thenReturn(properties);
    mockSearchResults(List.of(NOTE_ID));
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq(null))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    Space space = mockSpace();
    when(spaceService.getSpaceByGroupId(GROUP_ID)).thenReturn(space);

    NoteSearchCollectionModel result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note", null, null, null, null));

    JsonNode hit = JSON.valueToTree(result).get("notes").get(0);
    assertEquals(Set.of("note_id", "title", "summary", "url", "updated_date", "space"), fieldNames(hit));
    assertEquals(NOTE_ID, hit.get("note_id").asLong());
    assertEquals("Note", hit.get("title").asString());
    assertEquals(SUMMARY, hit.get("summary").asString());
    assertEquals("https://meeds.test/note-link", hit.get("url").asString());
    assertEquals(SPACE_ID, hit.get("space").get("space_id").asLong());
    assertEquals("Space", hit.get("space").get("name").asString());
    verify(noteService, never()).canEditNote(any(Page.class), anyString());
    verify(noteService, never()).getNoteById(anyString());
  }

  /**
   * A limit above the maximum is clamped to 100, and the search asks for one
   * more hit than the limit, to tell whether another page follows.
   */
  @Test
  public void searchNotesShouldClampTheLimitTo100() throws Exception { // NOSONAR
    mockSearchResults(List.of());

    NoteSearchCollectionModel result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note", null, null, 101, null));

    ArgumentCaptor<WikiSearchData> captor = ArgumentCaptor.forClass(WikiSearchData.class);
    verify(noteService).search(captor.capture());
    assertEquals(100, result.usedLimit());
    assertEquals(101, captor.getValue().getLimit());
    assertEquals(0, captor.getValue().getOffset());
  }

  /**
   * When the search finds one hit more than the limit, the page holds the
   * limit's hits, has_more is true and next_offset is the next page's offset.
   */
  @Test
  public void searchNotesWithMoreHitsThanTheLimitShouldSayMoreFollow() throws Exception { // NOSONAR
    mockSearchablePages(3);

    NoteSearchCollectionModel result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note", null, 4, 2, null));

    assertEquals(List.of(NOTE_ID, NOTE_ID + 1), result.notes().stream().map(NoteSearchHitModel::noteId).toList());
    assertTrue(result.hasMore());
    assertEquals(Integer.valueOf(6), result.nextOffset());
    assertEquals(6, JSON.valueToTree(result).get("next_offset").asInt());
  }

  /**
   * When the search finds no more hits than the limit, has_more is false and
   * next_offset is left out of the answer.
   */
  @Test
  public void searchNotesWithoutMoreHitsShouldLeaveOutTheNextOffset() throws Exception { // NOSONAR
    mockSearchablePages(2);

    NoteSearchCollectionModel result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note", null, 4, 2, null));

    assertEquals(2, result.notes().size());
    assertFalse(result.hasMore());
    assertNull(result.nextOffset());
    JsonNode json = JSON.valueToTree(result);
    assertFalse(json.has("next_offset"));
    assertFalse(json.get("has_more").asBoolean());
  }

  /**
   * Without offset nor max_chars, get_note returns the whole content and none
   * of the part's fields.
   */
  @Test
  public void getNoteWithoutPartParametersShouldReturnTheWholeContent() throws Exception { // NOSONAR
    stubNoteContent(LONG_HTML);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, null));

    assertEquals(LONG_HTML, result.htmlContent());
    assertNull(result.offset());
    assertNull(result.endOffset());
    assertNull(result.totalChars());
    assertNull(result.nextOffset());
    assertNull(result.versionNumber());
    JsonNode json = JSON.valueToTree(result);
    for (String field : List.of("offset", "end_offset", "total_chars", "next_offset", "version_number")) {
      assertFalse(field, json.has(field));
    }
  }

  /**
   * A long note is read part by part: each part ends where it splits no tag,
   * says its bounds, the whole length and the next part's offset, and the
   * parts read from next_offset join into the whole content.
   */
  @Test
  public void getNoteShouldReadALongNotePartByPart() throws Exception { // NOSONAR
    stubNoteContent(LONG_HTML);

    NoteModel first = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 0L, 10, null));

    // "<p>Hello <b>world</b></p>": 10 characters end inside "<b>", cut before it
    assertEquals("<p>Hello ", first.htmlContent());
    assertEquals(Integer.valueOf(0), first.offset());
    assertEquals(Integer.valueOf(9), first.endOffset());
    assertEquals(Integer.valueOf(LONG_HTML.length()), first.totalChars());
    assertEquals(Integer.valueOf(9), first.nextOffset());
    JsonNode json = JSON.valueToTree(first);
    assertEquals(0, json.get("offset").asInt());
    assertEquals(LONG_HTML.length(), json.get("total_chars").asInt());

    StringBuilder whole = new StringBuilder(first.htmlContent());
    Integer next = first.nextOffset();
    int parts = 1;
    while (next != null) {
      long from = next;
      NoteModel part = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, from, 10, null));
      assertEquals(Integer.valueOf(next), part.offset());
      // longer only when a single tag is, which is returned whole
      assertTrue(part.htmlContent(), part.htmlContent().length() <= 10 || part.htmlContent().matches("<[^>]*>"));
      assertFalse(part.htmlContent(), part.htmlContent().matches("(?s).*<[^>]*"));
      whole.append(part.htmlContent());
      next = part.nextOffset();
      parts++;
    }
    assertEquals(LONG_HTML, whole.toString());
    assertTrue(parts > 2);
  }

  /**
   * An offset that falls inside a tag starts at that tag, and an offset past
   * the end returns an empty last part.
   */
  @Test
  public void getNoteWithAnOffsetInsideATagShouldStartAtTheTag() throws Exception { // NOSONAR
    stubNoteContent(LONG_HTML);

    NoteModel inside = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 11L, 3, null));
    NoteModel past = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 10_000L, null, null));

    assertEquals(Integer.valueOf(9), inside.offset());
    assertEquals("<b>", inside.htmlContent());
    assertEquals(Integer.valueOf(LONG_HTML.length()), past.offset());
    assertNull(past.nextOffset());
  }

  /**
   * max_chars is clamped to 200000 above it and to 1 below 1, and offset
   * alone reads 40000 characters.
   */
  @Test
  public void getNoteShouldClampMaxChars() throws Exception { // NOSONAR
    String html = "<p>" + "a".repeat(250_000) + "</p>";
    stubNoteContent(html);

    NoteModel capped = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 0L, 500_000, null));
    NoteModel single = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 3L, -5, null));
    NoteModel defaulted = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 3L, null, null));

    assertEquals(NoteMcpTool.MAX_PART_CHARS, capped.htmlContent().length());
    assertEquals(Integer.valueOf(NoteMcpTool.MAX_PART_CHARS), capped.nextOffset());
    assertEquals("a", single.htmlContent());
    assertEquals(NoteMcpTool.DEFAULT_PART_CHARS, defaulted.htmlContent().length());
  }

  /**
   * A tag longer than max_chars is returned whole rather than an empty part
   * that would never move on.
   */
  @Test
  public void getNoteWithATagLongerThanMaxCharsShouldReturnTheTag() throws Exception { // NOSONAR
    String html = "<img src=\"https://meeds.test/a-long-image-path.png\"><p>after</p>";
    stubNoteContent(html);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, 0L, 5, null));

    assertEquals("<img src=\"https://meeds.test/a-long-image-path.png\">", result.htmlContent());
    assertEquals(Integer.valueOf(html.indexOf("<p>")), result.nextOffset());
  }

  /**
   * A cut never splits a tag, a quoted attribute holding a {@code >}, a
   * comment, a character reference or a surrogate pair.
   */
  @Test
  public void safeBoundaryShouldNeverSplitATagAReferenceOrAPair() {
    String quoted = "<a title=\"a>b\" href=\"x\">link</a>";
    assertEquals(0, NoteMcpTool.safeBoundary(quoted, 12));
    assertEquals(24, NoteMcpTool.safeBoundary(quoted, 24));
    String comment = "ab<!-- x > y -->cd";
    assertEquals(2, NoteMcpTool.safeBoundary(comment, 10));
    assertEquals(16, NoteMcpTool.safeBoundary(comment, 16));
    String entity = "<p>a&nbsp;b</p>";
    assertEquals(4, NoteMcpTool.safeBoundary(entity, 7));
    assertEquals(10, NoteMcpTool.safeBoundary(entity, 10));
    String pair = "a\uD83D\uDE00b";
    assertEquals(1, NoteMcpTool.safeBoundary(pair, 2));
    assertEquals(3, NoteMcpTool.safeBoundary(pair, 3));
    assertEquals(0, NoteMcpTool.safeBoundary(pair, -1));
    assertEquals(pair.length(), NoteMcpTool.safeBoundary(pair, 99));
    assertEquals(10, NoteMcpTool.boundaryAfter(entity, 4));
    assertEquals(3, NoteMcpTool.boundaryAfter(pair, 1));
  }

  /**
   * get_note reads one version of the history by its number: its title and
   * content, with the version's number.
   */
  @Test
  public void getNoteWithAVersionNumberShouldReadThatVersion() throws Exception { // NOSONAR
    Page note = restorablePage();
    PageHistory version = pageHistory(note, 2L, "Old title", "<p>old</p>");
    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(pageHistory(note,
                                                                                                      3L,
                                                                                                      "Current title",
                                                                                                      "<p>current</p>"),
                                                                                          version));

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, 2L));

    assertEquals("Old title", result.title());
    assertTrue(result.htmlContent(), result.htmlContent().contains("old"));
    assertFalse(result.htmlContent(), result.htmlContent().contains("current"));
    assertEquals(Long.valueOf(2L), result.versionNumber());
    assertEquals(2, JSON.valueToTree(result).get("version_number").asInt());
  }

  /**
   * get_note refuses a version number the history doesn't hold.
   */
  @Test
  public void getNoteWithAnUnknownVersionNumberShouldThrow() throws Exception { // NOSONAR
    Page note = restorablePage();
    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(List.of(pageHistory(note, 1L, TITLE, CONTENT)));

    assertThrows(ObjectNotFoundException.class, () -> runWithStaticMocks(() -> tool.getNote(NOTE_ID, null, null, null, 7L)));
  }

  /**
   * The version history is read a page at a time, without the versions'
   * content: a first page says more follow and where, the last page says none
   * do and leaves out next_offset.
   */
  @Test
  public void getNoteVersionsShouldBePaginatedWithoutContent() throws Exception { // NOSONAR
    Page note = restorablePage();
    List<PageHistory> history = new java.util.ArrayList<>();
    for (long number = 25; number > 0; number--) {
      history.add(pageHistory(note, number, TITLE + number, "<p>content " + number + "</p>"));
    }
    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(history);

    NoteVersionCollectionModel first = runWithStaticMocks(() -> tool.getNoteVersions(NOTE_ID, null, null, null));
    NoteVersionCollectionModel last = runWithStaticMocks(() -> tool.getNoteVersions(NOTE_ID, null, 20, 10));

    assertEquals(10, first.versions().size());
    assertEquals(10, first.usedLimit());
    assertEquals(Long.valueOf(25L), first.versions().get(0).versionNumber());
    assertTrue(first.hasMore());
    assertEquals(Integer.valueOf(10), first.nextOffset());
    JsonNode version = JSON.valueToTree(first).get("versions").get(0);
    assertEquals(Set.of("version_number", "version_id", "title", "updated_date", "author"), fieldNames(version));

    assertEquals(5, last.versions().size());
    assertEquals(Long.valueOf(5L), last.versions().get(0).versionNumber());
    assertFalse(last.hasMore());
    assertNull(last.nextOffset());
    assertFalse(JSON.valueToTree(last).has("next_offset"));
  }

  /**
   * A version history limit above the maximum is clamped to 100.
   */
  @Test
  public void getNoteVersionsShouldClampTheLimitTo100() throws Exception { // NOSONAR
    Page note = restorablePage();
    List<PageHistory> history = new java.util.ArrayList<>();
    for (long number = 150; number > 0; number--) {
      history.add(pageHistory(note, number, TITLE, CONTENT));
    }
    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.getVersionsHistoryOfNoteByLang(note, USER, null)).thenReturn(history);

    NoteVersionCollectionModel result = runWithStaticMocks(() -> tool.getNoteVersions(NOTE_ID, null, null, 101));

    assertEquals(100, result.usedLimit());
    assertEquals(100, result.versions().size());
    assertEquals(Integer.valueOf(100), result.nextOffset());
  }

  /**
   * Stubs the note search to find the given note ids.
   *
   * @param noteIds the ids of the notes found
   */
  @SuppressWarnings({ "deprecation", "rawtypes", "unchecked" })
  private void mockSearchResults(List<Long> noteIds) throws Exception { // NOSONAR
    List<SearchResult> results = noteIds.stream().map(id -> {
      SearchResult result = mock(SearchResult.class);
      lenient().when(result.getId()).thenReturn(id);
      return result;
    }).toList();
    ObjectPageList wikiSearchResult = mock(ObjectPageList.class);
    when(wikiSearchResult.getAll()).thenReturn(results);
    when(noteService.search(any(WikiSearchData.class))).thenReturn(wikiSearchResult);
  }

  /**
   * Stubs the note search to find the given number of readable notes, from
   * {@link #NOTE_ID} on.
   *
   * @param count the number of notes found
   */
  private void mockSearchablePages(int count) throws Exception { // NOSONAR
    List<Long> ids = new java.util.ArrayList<>();
    for (long id = NOTE_ID; id < NOTE_ID + count; id++) {
      Page page = mockPage(String.valueOf(id), "Note " + id);
      lenient().when(noteService.getNoteByIdAndLang(eq(id), eq(currentIdentity), eq(null), eq(null))).thenReturn(page);
      lenient().when(noteService.canViewNote(page, USER)).thenReturn(true);
      ids.add(id);
    }
    Space space = mockSpace();
    lenient().when(spaceService.getSpaceByGroupId(GROUP_ID)).thenReturn(space);
    mockSearchResults(ids);
  }

  /**
   * Makes the note readable with the given HTML content, as a fresh page on
   * each read, so that one read's sanitizing doesn't leak into the next.
   *
   * @param html the note's content
   */
  private void stubNoteContent(String html) throws Exception { // NOSONAR
    when(noteService.getNoteByIdAndLang(NOTE_ID, currentIdentity, null, null)).thenAnswer(invocation -> {
      Page page = restorablePage();
      page.setContent(html);
      return page;
    });
    when(noteService.canViewNote(any(Page.class), eq(USER))).thenReturn(true);
    lenient().when(noteService.canEditNote(any(Page.class), eq(USER))).thenReturn(true);
  }

  /**
   * Lists the field names of a JSON object.
   *
   * @param node the JSON object
   * @return its field names
   */
  private Set<String> fieldNames(JsonNode node) {
    return new java.util.HashSet<>(node.propertyNames());
  }

  private Page restorablePage() {
    Page note = new Page();
    note.setId(String.valueOf(NOTE_ID));
    note.setName("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
    note.setTitle("Current title");
    note.setContent("<p>current</p>");
    note.setWikiType(WIKI_TYPE);
    note.setWikiOwner(GROUP_ID);
    note.setAuthor(USER);
    note.setLastUpdater(USER);
    note.setCreatedDate(new Date());
    note.setUpdatedDate(new Date());
    return note;
  }

  // as EntityConverter#convertPageVersionEntityToPageHistory builds one: the name is the PAGE's name
  private PageHistory pageHistory(Page note, long versionNumber, String title, String content) {
    PageHistory version = new PageHistory();
    version.setId(note.getId() + "-" + versionNumber);
    version.setVersionNumber(versionNumber);
    version.setName(note.getName());
    version.setTitle(title);
    version.setContent(content);
    version.setAuthor(USER);
    version.setCreatedDate(new Date());
    version.setUpdatedDate(new Date());
    return version;
  }

  private NotePageProperties coverProperties(long featuredImageId) {
    NotePageProperties properties = new NotePageProperties();
    NoteFeaturedImage cover = new NoteFeaturedImage();
    cover.setId(featuredImageId);
    properties.setFeaturedImage(cover);
    return properties;
  }

  private Page mockPage(String id, String title) {
    Page page = mock(Page.class);

    lenient().when(page.getId()).thenReturn(id);
    lenient().when(page.getTitle()).thenReturn(title);
    lenient().when(page.getContent()).thenReturn("<p>Content</p>");
    lenient().when(page.getCreatedDate()).thenReturn(new Date());
    lenient().when(page.getUpdatedDate()).thenReturn(new Date());
    lenient().when(page.getAuthor()).thenReturn(USER);
    lenient().when(page.getLastUpdater()).thenReturn(USER);
    lenient().when(page.getActivityId()).thenReturn("99");
    lenient().when(page.getName()).thenReturn("note-" + id);
    lenient().when(page.getWikiType()).thenReturn(WIKI_TYPE);
    lenient().when(page.getWikiOwner()).thenReturn(GROUP_ID);
    lenient().when(page.isHasChild()).thenReturn(false);
    lenient().when(page.getParentPageId()).thenReturn(null);

    return page;
  }

  /**
   * Makes the personal workspace the only site hosting a notes node, under its
   * dashboard node, after a site with no notes node, as on an eXo platform.
   */
  private void mockPersonalNotesSite() {
    PortalConfig metaSite = new PortalConfig(PortalConfig.PORTAL_TYPE, "dw");
    PortalConfig personalSite = new PortalConfig(PortalConfig.PORTAL_TYPE, "myworkspace");
    UserNode metaRoot = mockUserNode("default", null, List.of(mockUserNode("tasks", "tasks", List.of())));
    UserNode notesNode = mockUserNode("notes", "dashboard/notes", List.of());
    UserNode dashboardNode = mockUserNode("dashboard", "dashboard", List.of(notesNode));
    UserNode personalRoot = mockUserNode("default", null, List.of(dashboardNode));
    when(layoutService.getSites(any())).thenReturn(List.of(metaSite, personalSite));
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "dw", USER, false)).thenReturn(metaRoot);
    when(portalConfigService.getSiteRootNode(PortalConfig.PORTAL_TYPE, "myworkspace", USER, false)).thenReturn(personalRoot);
  }

  /**
   * Mocks a navigation node.
   *
   * @param name the node name
   * @param uri the node URI in its site
   * @param children the node children
   * @return the mocked node
   */
  private UserNode mockUserNode(String name, String uri, List<UserNode> children) {
    UserNode node = mock(UserNode.class);
    lenient().when(node.getName()).thenReturn(name);
    lenient().when(node.getURI()).thenReturn(uri);
    lenient().when(node.getChildren()).thenReturn(children);
    return node;
  }

  private Page mockPersonalPage(String id, String title, String owner) {
    Page page = mockPage(id, title);
    lenient().when(page.getWikiType()).thenReturn(USER_WIKI_TYPE);
    lenient().when(page.getWikiOwner()).thenReturn(owner);
    return page;
  }

  private Wiki mockPersonalWiki(String owner) {
    Wiki wiki = mock(Wiki.class);
    lenient().when(wiki.getId()).thenReturn("wiki-" + owner);
    lenient().when(wiki.getOwner()).thenReturn(owner);
    lenient().when(wiki.getType()).thenReturn(USER_WIKI_TYPE);
    return wiki;
  }

  private Wiki mockWiki() {
    Wiki wiki = mock(Wiki.class);
    lenient().when(wiki.getId()).thenReturn("wiki-id");
    lenient().when(wiki.getOwner()).thenReturn(GROUP_ID);
    lenient().when(wiki.getType()).thenReturn(WIKI_TYPE);
    return wiki;
  }

  private Space mockSpace() {
    Space space = mock(Space.class);
    lenient().when(space.getSpaceId()).thenReturn(SPACE_ID);
    lenient().when(space.getGroupId()).thenReturn(GROUP_ID);
    lenient().when(space.getDisplayName()).thenReturn("Space");
    return space;
  }

  private <T> T runWithStaticMocks(CheckedSupplier<T> supplier) throws Exception { // NOSONAR
    try (MockedStatic<McpToolUtils> mcp = mockStatic(McpToolUtils.class);
        MockedStatic<CommonsUtils> commons = mockStatic(CommonsUtils.class);
        MockedStatic<UserToolUtils> users = mockStatic(UserToolUtils.class)) {

      mcp.when(McpToolUtils::getUserTimeZone).thenReturn(TimeZone.getTimeZone("UTC"));
      mcp.when(() -> McpToolUtils.formatDate(any(Date.class))).thenReturn("2024-01-01T00:00:00Z");
      mcp.when(() -> McpToolUtils.markdownToHtml(anyString()))
         .thenAnswer(invocation -> "<p>" + invocation.getArgument(0) + "</p>");

      commons.when(CommonsUtils::getCurrentDomain).thenReturn("https://meeds.test");

      UserModel user = new UserModel();
      user.setUsername(USER);
      user.setDisplayName("Root User");

      users.when(() -> UserToolUtils.toUserModel(any(),
                                                 any(),
                                                 any(),
                                                 any(),
                                                 any(),
                                                 anyString(),
                                                 anyString(),
                                                 any(),
                                                 anyBoolean()))
           .thenReturn(user);

      return supplier.get();
    }
  }

  private <T> T runWithConversationAndStaticMocks(CheckedSupplier<T> supplier) throws Exception { // NOSONAR
    try (MockedStatic<ConversationState> conversation = mockStatic(ConversationState.class)) {
      conversation.when(ConversationState::getCurrent).thenReturn(conversationState);
      when(conversationState.getIdentity()).thenReturn(currentIdentity);
      return runWithStaticMocks(supplier);
    }
  }

  private <T> T runWithStaticMocksWithActivity(ActivityModel activityModel, CheckedSupplier<T> supplier) throws Exception { // NOSONAR
    try (MockedStatic<ActivityToolUtils> activities = mockStatic(ActivityToolUtils.class)) {
      activities.when(() -> ActivityToolUtils.toActivityModel(any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              any(),
                                                              anyString(),
                                                              any(),
                                                              any()))
                .thenReturn(activityModel);
      return runWithStaticMocks(supplier);
    }
  }

  private class TestableNoteMcpTool extends NoteMcpTool {

    TestableNoteMcpTool() {
      super(wikiService,
            noteService,
            activityManager,
            identityManager,
            i18NActivityProcessor,
            spaceService,
            translationService,
            profilePropertyService,
            userAcl,
            portalConfigService,
            permanentLinkService,
            uploadService,
            attachmentService,
            fileService,
            layoutService);
    }

    @Override
    public Identity getCurrentUserAclIdentity() {
      return currentIdentity;
    }

    @Override
    protected Long getScopeSpaceId() {
      if (scopeUnreadable) {
        throw new IllegalStateException("No acting identity");
      }
      return currentScopeSpaceId;
    }

    @Override
    public String getCurrentUserName() {
      return USER;
    }

    @Override
    public Locale getCurrentUserLocale() {
      return Locale.ENGLISH;
    }

    @Override
    public Locale getCurrentUserLocale(String username) {
      return Locale.ENGLISH;
    }
  }

  @FunctionalInterface
  private interface CheckedSupplier<T> {
    T get() throws Exception; // NOSONAR
  }
}
