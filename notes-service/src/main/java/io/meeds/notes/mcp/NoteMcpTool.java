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

import static io.meeds.mcp.server.tool.util.McpToolPluginUtils.getInteger;
import static io.meeds.mcp.server.util.McpToolUtils.formatDate;
import static io.meeds.mcp.server.util.McpToolUtils.markdownToHtml;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.container.component.RequestLifeCycle;
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
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.mcp.server.plugin.McpToolPlugin;
import io.meeds.mcp.server.tool.model.ActivityModel;
import io.meeds.mcp.server.tool.model.UserModel;
import io.meeds.mcp.server.tool.util.ActivityToolUtils;
import io.meeds.mcp.server.tool.util.UserToolUtils;
import io.meeds.notes.mcp.model.NoteBreadcrumbModel;
import io.meeds.notes.mcp.model.NoteModel;
import io.meeds.notes.mcp.model.NoteRootTreeModel;
import io.meeds.notes.mcp.model.NoteTreeModel;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.notes.plugin.NotePermanentLinkPlugin;
import io.meeds.portal.permlink.model.PermanentLinkObject;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.html.model.HtmlTransformerContext;
import io.meeds.social.html.utils.HtmlUtils;
import io.meeds.social.translation.service.TranslationService;

import lombok.SneakyThrows;

@Service
@Profile("mcp-server")
public class NoteMcpTool implements McpToolPlugin {

  private static final String     NOTE_EDIT_DENIED = "User isn't allowed to update the Note with id '%s'";

  private static final String     SPACE_WIKI_TYPE  = WikiType.GROUP.toString().toLowerCase();

  private WikiService             wikiService;

  private NoteService             noteService;

  private IdentityManager         identityManager;

  private ActivityManager         activityManager;

  private I18NActivityProcessor   i18NActivityProcessor;

  private SpaceService            spaceService;

  private TranslationService      translationService;

  private ProfilePropertyService  profilePropertyService;

  private UserACL                 userAcl;

  private UserPortalConfigService portalConfigService;

  private PermanentLinkService    permanentLinkService;

  public NoteMcpTool(WikiService wikiService,
                          NoteService noteService,
                          ActivityManager activityManager,
                          IdentityManager identityManager,
                          I18NActivityProcessor i18NActivityProcessor,
                          SpaceService spaceService,
                          TranslationService translationService,
                          ProfilePropertyService profilePropertyService,
                          UserACL userAcl,
                          UserPortalConfigService portalConfigService,
                          PermanentLinkService permanentLinkService) {
    this.wikiService = wikiService;
    this.noteService = noteService;
    this.activityManager = activityManager;
    this.identityManager = identityManager;
    this.i18NActivityProcessor = i18NActivityProcessor;
    this.spaceService = spaceService;
    this.translationService = translationService;
    this.profilePropertyService = profilePropertyService;
    this.userAcl = userAcl;
    this.portalConfigService = portalConfigService;
    this.permanentLinkService = permanentLinkService;
  }

  public NoteRootTreeModel getSpaceNoteTree(long spaceId) throws ObjectNotFoundException, IllegalAccessException {
    Page rootNote = getSpaceParentPage(spaceId);
    return toNoteRootTreeModel(rootNote, spaceId);
  }

  public NoteModel getNote(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    return toNoteModel(note);
  }

  public NoteModel createSpaceNote(Long spaceId,
                                   String title,
                                   String summary,
                                   String htmlContent) throws IllegalAccessException, ObjectNotFoundException {
    if (spaceId == null) {
      throw new IllegalArgumentException("Either choose a target space identified by its id or a parent note to add a child note into.");
    }
    Page parentPage = getSpaceParentPage(spaceId);
    return createNote(parentPage, title, summary, markdownToHtml(htmlContent));
  }

  public NoteModel createChildNote(Long parentNoteId,
                                   String title,
                                   String summary,
                                   String htmlContent) throws IllegalAccessException, ObjectNotFoundException {
    if (parentNoteId == null) {
      throw new IllegalArgumentException("Either choose a target space identified by its id or a parent note to add a child note into.");
    }
    Page parentPage = getNoteById(parentNoteId);
    return createNote(parentPage, title, summary, markdownToHtml(htmlContent));
  }

  @SneakyThrows
  public NoteModel updateNote(long noteId,
                              String title,
                              String htmlContent,
                              String language) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    if (StringUtils.isBlank(language)) {
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
    } else {
      note.setLang(language);
      if (StringUtils.isNotBlank(title)) {
        note.setTitle(title);
      }
      if (StringUtils.isNotBlank(htmlContent)) {
        note.setContent(markdownToHtml(htmlContent));
      }
      note = noteService.updateNote(note, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, currentUserAclIdentity);
    }
    noteService.createVersionOfNote(note, currentUserAclIdentity.getUserId(), true);
    WikiPageParams noteParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    noteService.removeDraftOfNote(noteParams, language);
    return getNote(noteId);
  }

  @SneakyThrows
  public ActivityModel publishNote(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    note.setToBePublished(true);
    noteService.updateNote(note, PageUpdateType.PUBLISH, currentUserAclIdentity);
    note = getNoteById(noteId);
    return toActivityModel(note.getActivityId());
  }

  public void deleteNote(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Page note = getNoteById(noteId);
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    noteService.deleteNote(String.valueOf(noteId));
  }

  public void moveNote(long noteId,
                       long targetParentNoteId) throws IllegalAccessException, ObjectNotFoundException {
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    Page note = getNoteById(noteId);
    if (!noteService.canEditNote(note, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    Page targetNote = getNoteById(targetParentNoteId);
    if (!noteService.canEditNote(targetNote, currentUserAclIdentity.getUserId())) {
      throw new IllegalAccessException(NOTE_EDIT_DENIED);
    }
    WikiPageParams currentLocationParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
    WikiPageParams newLocationParams = new WikiPageParams(targetNote.getWikiType(),
                                                          targetNote.getWikiOwner(),
                                                          targetNote.getName());
    noteService.moveNote(currentLocationParams, newLocationParams, currentUserAclIdentity);
  }

  @SneakyThrows
  public List<NoteModel> searchNotes(String query,
                                     Long spaceId,
                                     Integer offset,
                                     Integer limit,
                                     Boolean isFavorites) {
    Identity currentIdentity = ConversationState.getCurrent().getIdentity();
    WikiSearchData data = new WikiSearchData(StringUtils.lowerCase(query),
                                             currentIdentity.getUserId());
    data.setOffset(getInteger(offset, DEFAULT_OFFSET));
    data.setLimit(getInteger(limit, DEFAULT_LIMIT));
    data.setNotesTreeFilter(false);
    data.setFavorites(isFavorites != null && isFavorites.booleanValue());
    if (spaceId != null && spaceId > 0) {
      data.setSpaceIds(List.of(String.valueOf(spaceId)));
    }
    List<SearchResult> results = noteService.search(data).getAll();
    return results.stream()
                  .map(SearchResult::getId)
                  .map(this::getNoteByIdNoException)
                  .map(this::toNoteModel)
                  .toList();
  }

  private NoteModel createNote(Page parentPage, String title, String summary, String htmlContent) throws IllegalAccessException,
                                                                                                  ObjectNotFoundException {
    Identity currentUserAclIdentity = getCurrentUserAclIdentity();
    Wiki wiki = wikiService.getWikiByTypeAndOwner(parentPage.getWikiType(), parentPage.getWikiOwner());
    String currentUser = currentUserAclIdentity.getUserId();
    Page note = new Page(UUID.randomUUID().toString());
    note.setAuthor(currentUser);
    note.setOwner(currentUser);
    note.setLastUpdater(currentUser);
    note.setSyntax(wikiService.getDefaultWikiSyntaxId());
    note.setTitle(title);
    note.setContent(htmlContent);
    note.setWikiId(wiki.getId());
    note.setWikiOwner(wiki.getOwner());
    note.setWikiType(wiki.getType());
    NotePageProperties properties = new NotePageProperties();
    properties.setSummary(summary);
    note.setProperties(properties);
    Page createdNote = noteService.createNote(wiki,
                                              parentPage.getName(),
                                              note,
                                              currentUserAclIdentity,
                                              false,
                                              true);
    return getNote(Long.parseLong(createdNote.getId()));
  }

  private String sanitizeAndSubstituteMentions(String htmlContent, Locale locale) {
    try {
      htmlContent = HtmlUtils.transform(htmlContent,
                                        new HtmlTransformerContext(ConversationState.getCurrent().getIdentity(),
                                                                   locale));
      String sanitizedBody = HTMLSanitizer.sanitize(htmlContent);
      sanitizedBody = sanitizedBody.replace("&#64;", "@");
      return MentionUtils.substituteUsernames(CommonsUtils.getCurrentPortalOwner(), sanitizedBody, locale);
    } catch (Exception e) {
      return htmlContent;
    }
  }

  @SneakyThrows
  private NoteModel toNoteModel(Page note) {
    Locale currentUserLocale = getCurrentUserLocale();
    String htmlContent = sanitizeAndSubstituteMentions(note.getContent(), currentUserLocale);
    note.setContent(htmlContent);
    String currentUserName = getCurrentUserName();
    boolean canEdit = noteService.canEditNote(note, currentUserName);
    return new NoteModel(Long.parseLong(note.getId()),
                         note.getTitle(),
                         htmlContent,
                         getUrl(note),
                         formatDate(note.getCreatedDate()),
                         formatDate(note.getUpdatedDate()),
                         note.isHasChild(),
                         canEdit,
                         toUserModel(note.getAuthor()),
                         toUserModel(note.getLastUpdater()),
                         toNoteBreadcrumb(note));
  }

  private NoteRootTreeModel toNoteRootTreeModel(Page rootNote, long spaceId) {
    NoteRootTreeModel rootTreeModel = new NoteRootTreeModel(Long.parseLong(rootNote.getId()),
                                                            rootNote.getTitle(),
                                                            getUrl(rootNote),
                                                            null,
                                                            spaceId);
    addChildren(rootNote, rootTreeModel);
    return rootTreeModel;
  }

  @SneakyThrows
  private NoteTreeModel toNoteTreeModel(Page note) {
    NoteTreeModel noteTreeModel = new NoteTreeModel(Long.parseLong(note.getId()),
                                                    note.getTitle(),
                                                    getUrl(note),
                                                    null);
    addChildren(note, noteTreeModel);
    return noteTreeModel;
  }

  private List<NoteBreadcrumbModel> toNoteBreadcrumb(Page note) {
    List<NoteBreadcrumbModel> breadcrumb = new ArrayList<>();
    addNoteBreadcrumb(breadcrumb, note);
    return breadcrumb;
  }

  private void addNoteBreadcrumb(List<NoteBreadcrumbModel> breadcrumb, Page note) {
    breadcrumb.add(0,
                   new NoteBreadcrumbModel(Long.parseLong(note.getId()),
                                           note.getTitle(),
                                           getUrl(note)));
    if (StringUtils.isNotBlank(note.getParentPageId())) {
      Page parentNote = noteService.getNoteById(note.getParentPageId());
      addNoteBreadcrumb(breadcrumb, parentNote);
    }
  }

  private void addChildren(Page note, NoteTreeModel noteTreeModel) {
    Collection<Page> childNotes = noteService.getChildrenNoteOf(note, false, false);
    if (CollectionUtils.isNotEmpty(childNotes)) {
      childNotes.stream().map(this::toNoteTreeModel).forEach(noteTreeModel::addChildNote);
    }
  }

  private Page getSpaceParentPage(long spaceId) throws ObjectNotFoundException, IllegalAccessException {
    String currentUsername = getCurrentUserName();
    Space space = spaceService.getSpaceById(spaceId);
    if (space == null) {
      throw new ObjectNotFoundException("Space with id '%s' doesn't exist.".formatted(spaceId));
    } else if (!spaceService.canViewSpace(space, currentUsername)) {
      throw new IllegalAccessException("The current user can't access space with id '%s'.".formatted(spaceId));
    }
    Wiki wiki = wikiService.getWikiByTypeAndOwner(SPACE_WIKI_TYPE, space.getGroupId());
    if (wiki == null) {
      wiki = wikiService.createWiki(SPACE_WIKI_TYPE, space.getGroupId());
      RequestLifeCycle.restartTransaction();
    }
    Page rootNote = noteService.getNoteById(wiki.getWikiHome().getId());
    if (rootNote == null) {
      throw new ObjectNotFoundException("Space with id %s doesn't have notes yet".formatted(spaceId));
    }
    return rootNote;
  }

  @SneakyThrows
  private Page getNoteByIdNoException(long noteId) {
    return getNoteById(noteId);
  }

  private Page getNoteById(long noteId) throws IllegalAccessException, ObjectNotFoundException {
    Locale currentUserLocale = getCurrentUserLocale();
    Page note = noteService.getNoteByIdAndLang(Long.valueOf(noteId),
                                               getCurrentUserAclIdentity(),
                                               null,
                                               currentUserLocale.getLanguage());
    if (note == null) {
      throw new ObjectNotFoundException("Note with id %s doesn't exists".formatted(noteId));
    } else if (!noteService.canViewNote(note, getCurrentUserName())) {
      throw new IllegalAccessException("User can't access the note with id %s".formatted(noteId));
    }
    return note;
  }

  private UserModel toUserModel(String username) {
    if (StringUtils.isBlank(username)) {
      return null;
    }
    return UserToolUtils.toUserModel(identityManager,
                                     profilePropertyService,
                                     userAcl,
                                     translationService,
                                     portalConfigService,
                                     username,
                                     getCurrentUserName(),
                                     getCurrentUserLocale(),
                                     true);
  }

  @SneakyThrows
  private String getUrl(Page note) {
    return CommonsUtils.getCurrentDomain() +
        permanentLinkService.getLink(new PermanentLinkObject(NotePermanentLinkPlugin.OBJECT_TYPE, note.getId()));
  }

  private ActivityModel toActivityModel(String activityId) {
    return ActivityToolUtils.toActivityModel(activityManager,
                                             spaceService,
                                             identityManager,
                                             userAcl,
                                             permanentLinkService,
                                             profilePropertyService,
                                             translationService,
                                             i18NActivityProcessor,
                                             portalConfigService,
                                             activityId,
                                             getCurrentUserAclIdentity(),
                                             getCurrentUserLocale());
  }

}
