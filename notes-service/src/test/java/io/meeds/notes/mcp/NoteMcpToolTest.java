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
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.mcp.server.util.McpToolUtils;
import io.meeds.notes.mcp.model.NoteModel;
import io.meeds.notes.mcp.model.NoteRootTreeModel;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

@RunWith(MockitoJUnitRunner.class)
public class NoteMcpToolTest {

  private static final String     OTHER_TITLE    = "Updated";

  private static final String     CONTENT        = "Content";

  private static final String     SUMMARY        = "Summary";

  private static final String     TITLE          = "Title";

  private static final String     WIKI_TYPE      = "group";

  private static final String     GROUP_ID       = "/spaces/test";

  private static final String     USER           = "root";

  private static final long       NOTE_ID        = 12L;

  private static final long       PARENT_NOTE_ID = 13L;

  private static final long       SPACE_ID       = 33L;

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
  private PermanentLinkService    permanentLinkService;

  @Mock
  private Identity                currentIdentity;

  @Mock
  private ConversationState       conversationState;

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
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(null);

    tool.getNote(NOTE_ID);
  }

  @Test(expected = IllegalAccessException.class)
  public void getNoteWhenUserCannotViewShouldThrowException() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(false);

    tool.getNote(NOTE_ID);
  }

  @Test
  public void getNoteShouldReturnNoteModel() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.getNote(NOTE_ID));

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
    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.createSpaceNote(SPACE_ID, TITLE, SUMMARY, CONTENT));

    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).createNote(eq(wiki), eq(name), any(Page.class), eq(currentIdentity), eq(false), eq(true));
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

    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(parent);
    when(noteService.canViewNote(parent, USER)).thenReturn(true);
    when(wikiService.getWikiByTypeAndOwner(parent.getWikiType(), parent.getWikiOwner())).thenReturn(wiki);
    when(wikiService.getDefaultWikiSyntaxId()).thenReturn("xhtml/1.0");
    String parentName = parent.getName();
    when(noteService.createNote(eq(wiki), eq(parentName), any(Page.class), eq(currentIdentity), eq(false), eq(true)))
                                                                                                                     .thenReturn(created);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(created);
    when(noteService.canViewNote(created, USER)).thenReturn(true);
    when(noteService.canEditNote(created, USER)).thenReturn(true);

    NoteModel result = runWithStaticMocks(() -> tool.createChildNote(PARENT_NOTE_ID, TITLE, SUMMARY, CONTENT));

    assertEquals(NOTE_ID, result.noteId());
    verify(noteService).createNote(eq(wiki), eq(parentName), any(Page.class), eq(currentIdentity), eq(false), eq(true));
  }

  @Test(expected = IllegalAccessException.class)
  public void updateNoteWhenUserCannotEditShouldThrowException() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(false);

    tool.updateNote(NOTE_ID, OTHER_TITLE, CONTENT, null);
  }

  @Test
  public void updateNoteShouldUpdateTitleAndContent() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
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

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);
    when(noteService.updateNote(eq(note), eq(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE), eq(currentIdentity)))
                                                                                                               .thenReturn(note);

    runWithStaticMocks(() -> tool.updateNote(NOTE_ID, "FR title", "FR content", "fr"));

    verify(note).setLang("fr");
    verify(noteService).removeDraftOfNote(any(WikiPageParams.class), eq("fr"));
  }

  @Test
  public void publishNoteShouldPublishAndReturnActivity() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    ActivityModel activityModel = mock(ActivityModel.class);

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
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

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    tool.deleteNote(NOTE_ID);

    verify(noteService).deleteNote(String.valueOf(NOTE_ID));
  }

  @Test
  public void moveNoteShouldMoveNote() throws Exception { // NOSONAR
    Page note = mockPage(String.valueOf(NOTE_ID), "Note");
    Page target = mockPage(String.valueOf(PARENT_NOTE_ID), "Target");

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.getNoteByIdAndLang(eq(PARENT_NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(target);
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

    when(noteService.getNoteByIdAndLang(eq(NOTE_ID), eq(currentIdentity), eq(null), eq("en"))).thenReturn(note);
    when(noteService.canViewNote(note, USER)).thenReturn(true);
    when(noteService.canEditNote(note, USER)).thenReturn(true);

    List<NoteModel> result = runWithConversationAndStaticMocks(() -> tool.searchNotes("note", SPACE_ID, null, null, true));

    assertEquals(1, result.size());
    assertEquals(NOTE_ID, result.get(0).noteId());
    verify(noteService).search(any(WikiSearchData.class));
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
            permanentLinkService);
    }

    @Override
    public Identity getCurrentUserAclIdentity() {
      return currentIdentity;
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
