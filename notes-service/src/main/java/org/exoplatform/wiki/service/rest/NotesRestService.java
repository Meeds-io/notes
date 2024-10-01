 /**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.exoplatform.wiki.service.rest;

import java.io.InputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.exoplatform.commons.comparators.NaturalComparator;
import org.exoplatform.wiki.tree.PageTreeNode;
import org.gatein.api.EntityNotFoundException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.comparators.NaturalComparator;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.http.PATCH;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.utils.MentionUtils;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.social.rest.entity.IdentityEntity;
import org.exoplatform.upload.UploadResource;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.NotesExportService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.BeanToJsons;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TitleSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.PageTreeNode;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.WikiTreeNode;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.notes.rest.model.DraftPageEntity;
import io.meeds.notes.rest.model.PageEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;

@Path("/notes")
@Tag(name = "/notes", description = "Managing notes")
@RolesAllowed("users")

public class NotesRestService implements ResourceContainer {

  private static final String         NOTE_NAME_EXISTS = "Note name already exists";

  private static final Log            log              = ExoLogger.getLogger(NotesRestService.class);

  private final NoteService           noteService;

  private final WikiService           noteBookService;

  private final NotesExportService    notesExportService;

  private final UploadService         uploadService;

  private final IdentityManager identityManager;

  private final ResourceBundleService resourceBundleService;

  private final CacheControl          cc;

  private static final int            CACHE_DURATION_SECONDS      = 31536000;

  private static final long           CACHE_DURATION_MILLISECONDS = CACHE_DURATION_SECONDS * 1000L;

  private static final CacheControl   ILLUSTRATION_CACHE_CONTROL  = new CacheControl();
  
  private static final String         DRAFTS_NOTE_TYPE            = "drafts";

  static {
    ILLUSTRATION_CACHE_CONTROL.setMaxAge(CACHE_DURATION_SECONDS);
  }
  

  public NotesRestService(NoteService noteService,
                          WikiService noteBookService,
                          UploadService uploadService,
                          IdentityManager identityManager,
                          ResourceBundleService resourceBundleService,
                          NotesExportService notesExportService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.notesExportService = notesExportService;
    this.uploadService = uploadService;
    this.identityManager = identityManager;
    this.resourceBundleService = resourceBundleService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  @GET
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get note by notes params", method = "GET", description = "This get the not if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getNote(@Parameter(description = "NoteBook Type", required = true)
  @PathParam("noteBookType")
  String noteBookType,
                          @Parameter(description = "NoteBook Owner", required = true)
                          @PathParam("noteBookOwner")
                          String noteBookOwner,
                          @Parameter(description = "Note id", required = true)
                          @PathParam("noteId")
                          String noteId,
                          @Parameter(description = "source", required = true)
                          @QueryParam("source")
                          String source,
                          @Parameter(description = "note content language")
                          @QueryParam("lang")
                          String lang) {
    try {
      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (noteBookType.toUpperCase().equals(WikiType.GROUP.name())) {
        noteBookOwner = formatWikiOwnerToGroupId(noteBookOwner);
      }

      Wiki noteBook = null;
      noteBook = noteBookService.getWikiByTypeAndOwner(noteBookType, noteBookOwner);
      if (noteBook == null) {
        noteBook = noteBookService.createWiki(noteBookType, noteBookOwner);
      }
      Page note;
      if (noteId.equals(NoteConstants.NOTE_HOME_OLD_NAME) || noteId.equals(NoteConstants.NOTE_HOME_NAME)) {
        noteId = noteBook.getWikiHome().getId();
        note = noteService.getNoteById(noteId, identity, source);
      } else {
        note = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId, identity, source);
      }
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (StringUtils.isNotBlank(lang)) {
        note = noteService.getNoteByIdAndLang(Long.valueOf(note.getId()), identity, source, lang);
      }
      String content = note.getContent();
      if (content.contains(Utils.NOTE_LINK)) {
        while (content.contains(Utils.NOTE_LINK)) {
          String linkedParams = content.split(Utils.NOTE_LINK)[1].split("-//\"")[0];
          String NoteName = linkedParams.split("-////-")[2];
          Page linkedNote = null;
          linkedNote = noteService.getNoteOfNoteBookByName(note.getWikiType(), note.getWikiOwner(), NoteName);
          if (linkedNote != null) {
            content = content.replaceAll("\"noteLink\" href=\"//-" + linkedParams + "-//",
                                         "\"noteLink\" href=\"" + linkedNote.getId());
            if (content.equals(note.getContent()))
              break;
          }
        }
        if (!content.equals(note.getContent())) {
          note.setContent(content);
          noteService.updateNote(note);
        }
      }
      note.setContent(HTMLSanitizer.sanitize(note.getContent()));
      note.setBreadcrumb(noteService.getBreadCrumb(noteBookType,
                                                   noteBookOwner,
                                                   note.getName(),
                                                   request.getLocale().getLanguage(),
                                                   identity,
                                                   false));
      return Response.ok(note).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}:{}:{}", noteBookType, noteBookOwner, noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      log.error("Can't get note {}:{}:{}", noteBookType, noteBookOwner, noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/note/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get note by id", method = "GET", description = "This get the note if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getNoteById(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  String noteId,
                              @Parameter(description = "noteBookType")
                              @QueryParam("noteBookType")
                              String noteBookType,
                              @Parameter(description = "noteBookOwner")
                              @QueryParam("noteBookOwner")
                              String noteBookOwner,
                              @Parameter(description = "withChildren")
                              @QueryParam("withChildren")
                              boolean withChildren,
                              @Parameter(description = "source")
                              @QueryParam("source") String source,
                              @Parameter(description = "note content language")
                              @QueryParam("lang") String lang) {
    try {
      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteByIdAndLang(Long.valueOf(noteId), identity, source, lang);
      if (note == null || note.isDeleted()) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (StringUtils.isNotEmpty(noteBookType) && !note.getWikiType().equals(noteBookType)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (StringUtils.isNotEmpty(noteBookOwner) && !note.getWikiOwner().equals(noteBookOwner)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (BooleanUtils.isTrue(withChildren)) {
        note.setChildren(noteService.getChildrenNoteOf(note, false, withChildren));
      }
      // check for old notes children container to update
      if (note.getContent().contains("wiki-children-pages ck-widget")) {
        note = updateChildrenContainer(note);
      }
      note.setContent(sanitizeAndSubstituteMentions(note.getContent(), lang));
      note.setBreadcrumb(noteService.getBreadCrumb(note.getWikiType(),
                                                   note.getWikiOwner(),
                                                   note.getName(),
                                                   request.getLocale().getLanguage(),
                                                   identity,
                                                   false));
      return Response.ok(note).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      log.error("Can't get note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/note/translation/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get available translation languages by page id",
             method = "GET",
             description = "This get gets the available translation languages by page id.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "500", description = "Server internal error") })
  public Response getPageAvailableTranslationLanguages(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  Long noteId, @QueryParam("withDrafts")
  Boolean withDrafts) {
    if (noteId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New document title is mandatory").build();
    }
    try {
      List<String> languages = noteService.getPageAvailableTranslationLanguages(noteId,
                                                                                Boolean.TRUE.equals(withDrafts));
      return Response.ok(languages).type(MediaType.APPLICATION_JSON_TYPE).build();
    } catch (Exception e) {
     log.error("Error while getting available translation languages of the page with id : {}", noteId, e);
     return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @DELETE
  @Path("/note/translation/{noteId}/{lang}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Delete translation by page id and language", method = "GET", description = "This deletes translation by page id and language")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "500", description = "Server internal error") })
  public Response deleteTranslations(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  Long noteId, @PathParam("lang")
  String lang) {
    if (noteId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("New document title is mandatory").build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      noteService.deleteVersionsByNoteIdAndLang(noteId, identity.getUserId(), lang);
      return Response.ok().type(MediaType.APPLICATION_JSON_TYPE).build();
    } catch (Exception e) {
      log.error("Error while deleting translations of language : {} for the page with id : {}", lang, noteId, e);
      return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @GET
  @Path("/draftNote/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get draft note by id", method = "GET", description = "This returns the draft note if the authenticated user is the author of the draft.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getDraftNoteById(@Parameter(description = "Note id", required = true)
                                   @PathParam("noteId") Long noteId,
                                   @Parameter(description = "draft content language")
                                   @QueryParam("lang") String lang) {
    Identity identity = ConversationState.getCurrent().getIdentity();
    String currentUserId = identity.getUserId();
    try {
      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);
      DraftPage draftNote = noteService.getDraftNoteById(String.valueOf(noteId), currentUserId);
      if (draftNote == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      Page parentPage = noteService.getNoteById(draftNote.getParentPageId(), identity);
      if (parentPage == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      draftNote.setContent(sanitizeAndSubstituteMentions(draftNote.getContent(), lang));
      draftNote.setBreadcrumb(noteService.getBreadCrumb(parentPage.getWikiType(),
                                                        parentPage.getWikiOwner(),
                                                        draftNote.getId(),
                                                        request.getLocale().getLanguage(),
                                                        identity,
                                                        true));

      return Response.ok(draftNote).build();
    } catch (IllegalAccessException e) {
      log.warn("User '{}' is not autorized to get draft note {}", currentUserId, noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
    } catch (Exception e) {
      log.error("Can't get draft note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    } 
  }

  @GET
  @Path("/latestDraftNote/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get latest draft note of page", method = "GET", description = "This returns the latest draft of the note if the authenticated user is the author of the draft.")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getLatestDraftOfPage(@Parameter(description = "Note id", required = true)
                                       @PathParam("noteId") String noteId,
                                       @Parameter(description = "draft content language")
                                       @QueryParam("lang") String lang) {
    try {
      Page targetPage = noteService.getNoteById(noteId);
      if (targetPage == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      DraftPage draftNote = noteService.getLatestDraftPageByTargetPageAndLang(Long.valueOf(targetPage.getId()),
                                                                                     lang);
      if (draftNote != null) {
        draftNote.setContent(sanitizeAndSubstituteMentions(draftNote.getContent(), lang));
      }
      return Response.ok(draftNote != null ? draftNote : org.json.JSONObject.NULL).build();
    } catch (Exception e) {
      log.error("Can't get draft note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/versions/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @Operation(summary = "Get versions of note by id", method = "GET", description = "This get the versions of a note if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
       @ApiResponse(responseCode = "400", description = "Invalid query input"),
       @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
       @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getNoteVersions(@Parameter(description = "Note id", required = true)
                                  @PathParam("noteId") String noteId,
                                  @Parameter(description = "versions content language")
                                  @QueryParam("lang") String lang) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity);
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      List<PageHistory> pageHistories = noteService.getVersionsHistoryOfNoteByLang(note, identity.getUserId(), lang);
      pageHistories.forEach(pageHistory -> pageHistory.setContent(sanitizeAndSubstituteMentions(pageHistory.getContent(), lang)));
      return Response.ok(pageHistories)
                     .build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      log.error("Can't get versions list of note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/note")
  @RolesAllowed("users")
  @Operation(summary = "Add a new note", method = "POST", description = "This adds a new note.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response createNote(@Parameter(description = "note object to be created", required = true)
                             PageEntity note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (NumberUtils.isNumber(note.getTitle())) {
      log.warn("Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Note's title should not be number}").build();
    }
    String noteBookType = note.getWikiType();
    String noteBookOwner = note.getWikiOwner();
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (StringUtils.isNotEmpty(note.getParentPageId())) {
        Page note_ = noteService.getNoteById(note.getParentPageId(), identity);
        if (note_ != null) {
          noteBookType = note_.getWikiType();
          noteBookOwner = note_.getWikiOwner();
          note.setParentPageName(note_.getName());
        } else {
          return Response.status(Response.Status.BAD_REQUEST).build();
        }
      }
      if (StringUtils.isEmpty(noteBookType) || StringUtils.isEmpty(noteBookOwner)) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      if (noteService.isExisting(noteBookType, noteBookOwner, TitleResolver.getId(note.getTitle(), false))) {
        return Response.status(Response.Status.CONFLICT).entity(NOTE_NAME_EXISTS).build();
      }
      /* TODO: check noteBook permissions */
      Wiki noteBook = noteBookService.getWikiByTypeAndOwner(noteBookType, noteBookOwner);
      if (noteBook == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String syntaxId = noteBookService.getDefaultWikiSyntaxId();
      String currentUser = identity.getUserId();
      note.setAuthor(currentUser);
      note.setOwner(currentUser);
      note.setSyntax(syntaxId);
      note.setName(note.getTitle());
      note.setUrl("");
      Page createdNote = noteService.createNote(noteBook,
                                                note.getParentPageName(),
                                                io.meeds.notes.rest.utils.EntityBuilder.toPage(note),
                                                identity);
      return Response.ok(createdNote, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", note.getName(), e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.warn("Failed to perform save noteBook note {}:{}:{}", noteBookType, noteBookOwner, note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @POST
  @Path("saveDraft")
  @RolesAllowed("users")
  @Operation(summary = "Add or update a new note draft page", method = "POST", description = "This adds a new note draft page or updates an existing one.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response saveDraft(@RequestBody(description = "Note draft page object to be created", required = true)
                            DraftPageEntity draftNoteToSave) {
    if (draftNoteToSave == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (NumberUtils.isNumber(draftNoteToSave.getTitle())) {
      log.warn("Draft Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Draft Note's title should not be number}").build();
    }

    String noteBookType = draftNoteToSave.getWikiType();
    String noteBookOwner = draftNoteToSave.getWikiOwner();
    Page parentNote = null;
    Page targetNote = null;
    DraftPage savedDraftPage = null;
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (StringUtils.isNoneEmpty(draftNoteToSave.getParentPageId())) {
        parentNote = noteService.getNoteById(draftNoteToSave.getParentPageId(), identity);
      }
      if (parentNote != null) {
        noteBookType = parentNote.getWikiType();
        noteBookOwner = parentNote.getWikiOwner();
        draftNoteToSave.setParentPageName(parentNote.getName());
      }
      if (StringUtils.isEmpty(noteBookType) || StringUtils.isEmpty(noteBookOwner)) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      Wiki noteBook = noteBookService.getWikiByTypeAndOwner(noteBookType, noteBookOwner);
      if (noteBook == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      if (StringUtils.isNoneEmpty(draftNoteToSave.getTargetPageId())) {
        targetNote = noteService.getNoteById(draftNoteToSave.getTargetPageId());
      }

      String syntaxId = noteBookService.getDefaultWikiSyntaxId();
      String currentUser = identity.getUserId();
      draftNoteToSave.setAuthor(currentUser);
      draftNoteToSave.setSyntax(syntaxId);

      if (targetNote != null) {
        DraftPage draftPage =
                            noteService.getLatestDraftPageByTargetPageAndLang(Long.valueOf(draftNoteToSave.getTargetPageId()),
                                                                                     draftNoteToSave.getLang());
        if (draftPage != null) {
          draftNoteToSave.setId(draftPage.getId());
        }
      }
      if (StringUtils.isNoneEmpty(draftNoteToSave.getId())) {
        savedDraftPage = targetNote != null
                                             ? noteService.updateDraftForExistPage(io.meeds.notes.rest.utils.EntityBuilder.toDraftPage(draftNoteToSave),
                                                                                   targetNote,
                                                                                   null,
                                                                                   System.currentTimeMillis(),
                                                                                   currentUser)
                                             : noteService.updateDraftForNewPage(io.meeds.notes.rest.utils.EntityBuilder.toDraftPage(draftNoteToSave),
                                                                                 System.currentTimeMillis(),
                                                                                 RestUtils.getCurrentUserIdentityId());
      } else {
        savedDraftPage = targetNote != null
                                             ? noteService.createDraftForExistPage(io.meeds.notes.rest.utils.EntityBuilder.toDraftPage(draftNoteToSave),
                                                                                   targetNote,
                                                                                   null,
                                                                                   System.currentTimeMillis(),
                                                                                   currentUser)
                                             : noteService.createDraftForNewPage(io.meeds.notes.rest.utils.EntityBuilder.toDraftPage(draftNoteToSave),
                                                                                 System.currentTimeMillis(),
                                                                                 RestUtils.getCurrentUserIdentityId());
      }

      return Response.ok(savedDraftPage, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      log.warn("Failed to perform save noteBook draft note {}:{}", noteBookType, noteBookOwner, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PUT
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @RolesAllowed("users")
  @Operation(summary = "Updates a specific note by note's params", method = "PUT", description = "This updates the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response updateNote(@Parameter(description = "NoteBook Type", required = true)
  @PathParam("noteBookType")
  String noteBookType,
                             @Parameter(description = "NoteBook Owner", required = true)
                             @PathParam("noteBookOwner")
                             String noteBookOwner,
                             @Parameter(description = "Note id", required = true)
                             @PathParam("noteId")
                             String noteId,
                             @RequestBody(description = "note object to be updated", required = true)
                             PageEntity note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (NumberUtils.isNumber(note.getTitle())) {
      log.warn("Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Note's title should not be number}").build();
    }
    try {
      if (noteBookType.toUpperCase().equals(WikiType.GROUP.name())) {
        noteBookOwner = formatWikiOwnerToGroupId(noteBookOwner);
      }

      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      note_.setToBePublished(note.isToBePublished());
      if ((!note_.getTitle().equals(note.getTitle()))
          && (noteService.isExisting(noteBookType, noteBookOwner, TitleResolver.getId(note.getTitle(), false)))) {
        return Response.status(Response.Status.CONFLICT).entity(NOTE_NAME_EXISTS).build();
      }
      if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        note_.setTitle(note.getTitle());
        note_.setContent(note.getContent());
        if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName()) && !note.getName().equals(newNoteName)) {
          noteService.renameNote(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
      } else if (!note_.getTitle().equals(note.getTitle())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName()) && !note.getName().equals(newNoteName)) {
          noteService.renameNote(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_.setTitle(note.getTitle());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
      }
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.error("Failed to perform update noteBook note {}:{}:{}", note.getWikiType(), note.getWikiOwner(), note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PUT
  @Path("/note/{noteId}")
  @RolesAllowed("users")
  @Operation(summary = "Updates a specific note by id", method = "PUT", description = "This updates the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
    public Response updateNoteById(@Parameter(description = "Note id", required = true) @PathParam("noteId") String noteId, 
                                   @RequestBody(description = "note object to be updated", required = true) PageEntity note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (NumberUtils.isNumber(note.getTitle())) {
      log.warn("Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Note's title should not be number}").build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteById(noteId, identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      if ((!note_.getTitle().equals(note.getTitle()))
          && (noteService.isExisting(note.getWikiType(), note.getWikiOwner(), TitleResolver.getId(note.getTitle(), false)))) {
        return Response.status(Response.Status.CONFLICT).entity(NOTE_NAME_EXISTS).build();
      }
      note_.setToBePublished(note.isToBePublished());
      NotePageProperties notePageProperties = io.meeds.notes.rest.utils.EntityBuilder.toNotePageProperties(note.getProperties());
      NoteFeaturedImage featuredImage = null;
      if (notePageProperties != null) {
        featuredImage = notePageProperties.getFeaturedImage();
      }
      String newNoteName = note_.getName();
      if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        if (StringUtils.isBlank(note.getLang())) {
          newNoteName = TitleResolver.getId(note.getTitle(), false);
          note_.setTitle(note.getTitle());
          note_.setContent(note.getContent());
          note_.setProperties(notePageProperties);
          if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName()) && !note.getName().equals(newNoteName)) {
            noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
            note_.setName(newNoteName);
          }
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
        } else {
          note_.setLang(note.getLang());
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
          note_.setTitle(note.getTitle());
          note_.setContent(note.getContent());
          note_.setProperties(notePageProperties);
        }
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!Utils.ANONYM_IDENTITY.equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams, note.getLang());
        }
      } else if (!note_.getTitle().equals(note.getTitle())) {
        if (StringUtils.isBlank(note.getLang())) {
          newNoteName = TitleResolver.getId(note.getTitle(), false);
          if (!NoteConstants.NOTE_HOME_NAME.equals(note.getName()) && !note.getName().equals(newNoteName)) {
            noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
            note_.setName(newNoteName);
          }
          note_.setTitle(note.getTitle());
          note_.setProperties(notePageProperties);
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
        } else {
          note_.setLang(note.getLang());
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
          note_.setTitle(note.getTitle());
          note_.setProperties(notePageProperties);
        }
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!Utils.ANONYM_IDENTITY.equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams, note.getLang());
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        if (StringUtils.isBlank(note.getLang())) {
          note_.setContent(note.getContent());
          note_.setProperties(notePageProperties);
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        } else {
          note_.setLang(note.getLang());
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
          note_.setContent(note.getContent());
          note_.setProperties(notePageProperties);
        }
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!Utils.ANONYM_IDENTITY.equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams, note.getLang());
        }
      } else if ((featuredImage != null && (featuredImage.isToDelete() || featuredImage.getUploadId() != null))
          || !Objects.equals(note_.getProperties(), notePageProperties)) {
        if (StringUtils.isBlank(note.getLang())) {
          note_.setProperties(notePageProperties);
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_PROPERTIES, identity);
        } else {
          note_.setLang(note.getLang());
          note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_PROPERTIES, identity);
          note_.setProperties(notePageProperties);
        }
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!Utils.ANONYM_IDENTITY.equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams, note.getLang());
        }
      } else if (note_.isToBePublished()){
        note_ = noteService.updateNote(note_, PageUpdateType.PUBLISH, identity);        
      } else {
        // in this case, the note didnt change on title nor content. As we need the page
        // url in front side, we compute it here
        note_.setUrl(Utils.getPageUrl(io.meeds.notes.rest.utils.EntityBuilder.toPage(note)));
      }
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have edit permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.error("Failed to perform update noteBook note {}:{}:{}", note.getWikiType(), note.getWikiOwner(), note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PUT
  @Path("/restore/{noteVersion}")
  @RolesAllowed("users")
  @Operation(summary = "Restore a specific note version by version id", method = "PUT", description = "This restore the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response RestoreNoteVersion(@Parameter(description = "Version Number", required = true)
  @PathParam("noteVersion")
  String noteVersion, @RequestBody(description = "note object to be updated", required = true)
  Page note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }

    if (NumberUtils.isNumber(note.getTitle())) {
      log.warn("Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Note's title should not be number}").build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      String currentUser = identity.getUserId();
      Page note_ = noteService.getNoteById(note.getId(), identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      noteService.restoreVersionOfNote(noteVersion, note, currentUser);
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have permissions to restore the note {} version", note.getId(), e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.error("Failed to perform restore note version {}", noteVersion, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @DELETE
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @RolesAllowed("users")
  @Operation(summary = "Delete note by note's params", method = "PUT", description = "This delets the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response deleteNote(@Parameter(description = "NoteBook Type", required = true)
  @PathParam("noteBookType")
  String noteBookType,
                             @Parameter(description = "NoteBook Owner", required = true)
                             @PathParam("noteBookOwner")
                             String noteBookOwner,
                             @Parameter(description = "Note id", required = true)
                             @PathParam("noteId")
                             String noteId) {

    try {
      if (noteBookType.toUpperCase().equals(WikiType.GROUP.name())) {
        noteBookOwner = formatWikiOwnerToGroupId(noteBookOwner);
      }

      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId, identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      noteService.deleteNote(noteBookType, noteBookOwner, noteId, identity);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      log.error("User does not have delete permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.warn("Failed to perform Delete of noteBook note {}:{}:{}", noteBookType, noteBookOwner, noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @DELETE
  @Path("/note/{noteId}")
  @RolesAllowed("users")
  @Operation(summary = "Delete note by note's params", method = "PUT", description = "This deletes the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response deleteNoteById(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  String noteId) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity);
      if (note == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String noteName = note.getName();
      // remove draft note
      if (!Utils.ANONYM_IDENTITY.equals(identity.getUserId())) {
        WikiPageParams noteParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), noteName);
        noteService.removeDraftOfNote(noteParams);
      }
      noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), noteName, identity);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      log.error("User does not have delete permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.warn("Failed to perform Delete of noteBook note {}", noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @DELETE
  @Path("/draftNote/{noteId}")
  @RolesAllowed("users")
  @Operation(summary = "Delete note by note's params", method = "PUT", description = "This deletes the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response deleteDraftNote(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  String noteId) {

    try {
      String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
      DraftPage draftNote = noteService.getDraftNoteById(noteId, currentUserId);
      if (draftNote == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      noteService.removeDraftById(draftNote.getId());
      return Response.ok().build();
    } catch (Exception ex) {
      log.warn("Failed to perform Delete of noteBook note {}", noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PATCH
  @Path("/note/move/{noteId}/{destinationNoteId}")
  @RolesAllowed("users")
  @Operation(summary = "Move note under the destination one", method = "PUT", description = "This moves the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response moveNote(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  String noteId,
                           @Parameter(description = "Destination Note id", required = true)
                           @PathParam("destinationNoteId")
                           String toNoteId) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity);
      if (note == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      Page toNote = noteService.getNoteById(toNoteId);
      if (toNote == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      WikiPageParams currentLocationParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), note.getName());
      WikiPageParams newLocationParams = new WikiPageParams(toNote.getWikiType(), toNote.getWikiOwner(), toNote.getName());
      boolean isMoved = noteService.moveNote(currentLocationParams, newLocationParams, identity);
      if (isMoved) {
        return Response.ok().build();
      } else {
        return Response.notModified().build();
      }
    } catch (IllegalAccessException e) {
      log.error("User does not have move permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.warn("Failed to perform move of noteBook note {} under {}", noteId, toNoteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/note/export/{exportId}/{notes}")
  @RolesAllowed("users")
  @Operation(summary = "Export notes", method = "GET", description = "This export selected notes and provide a zip file.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response exportNote(@Parameter(description = "List of notes ids", required = true)
  @PathParam("notes")
  String notesList,
                             @Parameter(description = "export ID", required = true)
                             @PathParam("exportId")
                             int exportId,
                             @Parameter(description = "exportAll")
                             @QueryParam("exportAll")
                             Boolean exportAll) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      String[] notes = notesList.split(",");
      notesExportService.startExportNotes(exportId, notes, exportAll, identity);
      return Response.ok().build();

    } catch (Exception ex) {
      log.warn("Failed to export notes ", ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/note/export/zip/{exportId}")
  @RolesAllowed("users")
  @Operation(summary = "Export notes", method = "GET", description = "This export selected notes and provide a zip file.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getExportedZip(@Parameter(description = "List of notes ids", required = true)
  @PathParam("exportId")
  int exportId) {

    try {
      byte[] filesBytes = notesExportService.getExportedNotes(exportId);
      return Response.ok(filesBytes)
                     .type("application/zip")
                     .header("Content-Disposition", "attachment; filename=\"notesExport_" + new Date().getTime() + ".zip\"")
                     .build();
    } catch (Exception ex) {
      log.warn("Failed to export notes ", ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/note/export/status/{exportId}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Export notes", method = "GET", description = "This export selected notes and provide a zip file.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getExportNoteStatus(@Parameter(description = "export id", required = true)
  @PathParam("exportId")
  int exportId) {

    try {
      return Response.ok(notesExportService.getStatus(exportId)).build();
    } catch (Exception ex) {
      log.warn("Failed to export notes ", ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/note/export/cancel/{exportId}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Export notes", method = "GET", description = "This cancel export.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"), @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response cancelExportNote(@Parameter(description = "export id", required = true)
  @PathParam("exportId")
  int exportId) {

    try {
      notesExportService.cancelExportNotes(exportId);
      return Response.ok().build();
    } catch (Exception ex) {
      log.warn("Failed to export notes ", ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @POST
  @Path("/note/import/{noteId}/{uploadId}")
  @RolesAllowed("users")
  @Operation(summary = "Import notes from a zip file", method = "POST", description = "This import notes from defined zip file under given note.")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response importNote(@Parameter(description = "Note id", required = true)
  @PathParam("noteId")
  String noteId,
                             @Parameter(description = "Upload id", required = true)
                             @PathParam("uploadId")
                             String uploadId,
                             @Parameter(description = "Conflict", required = true)
                             @QueryParam("conflict")
                             String conflict) {

    try {

      Identity identity = ConversationState.getCurrent().getIdentity();
      Page parent = noteService.getNoteById(noteId, identity);
      if (parent == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      UploadResource uploadResource = uploadService.getUploadResource(uploadId);

      if (uploadResource != null) {
        noteService.importNotes(uploadResource.getStoreLocation(), parent, conflict, identity);
        return Response.ok().build();
      } else {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
    } catch (IllegalAccessException e) {
      log.error("User does not have move permissions on the note {}", noteId, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception ex) {
      log.warn("Failed to export note {} ", noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/tree/{treeType}/{noteType}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Get node's tree data", method = "GET", description = "Display the current tree of a noteBook based on is path")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
      @ApiResponse(responseCode = "400", description = "Invalid query input"),
      @ApiResponse(responseCode = "403", description = "Unauthorized operation"),
      @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getNoteTreeData(@Parameter(description = "Tree of selected path children or all notes") @PathParam("treeType")  String treeType,
                                  @Parameter(description = "Tree of drafts or notes") @PathParam("noteType") String noteType,
                                  @Parameter(description = "Note path", required = true) @QueryParam(TreeNode.PATH) String path) {
    try {
      boolean withDrafts = Objects.equals(noteType, DRAFTS_NOTE_TYPE);
      Identity identity = ConversationState.getCurrent().getIdentity();
      List<JsonNodeData> responseData = new ArrayList<>();
      Map<String, Object> context = new HashMap<>();
      context.put(TreeNode.WITH_DRAFTS, withDrafts);

      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);

      // Put select note to context
      path = URLDecoder.decode(path, StandardCharsets.UTF_8);
      context.put(TreeNode.PATH, path);
      WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
      Page note =
                noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.getPageName(), identity);
      if (note == null) {
        log.warn("User [{}] can not get noteBook path [{}]. Home is used instead",
                 ConversationState.getCurrent().getIdentity().getUserId(),
                 path);
        note = noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), NoteConstants.NOTE_HOME_NAME);
        if (note == null) {
          ResourceBundle resourceBundle = resourceBundleService.getResourceBundle("locale.portlet.wiki.WikiPortlet",
                                                                                  request.getLocale());
          String errorMessage = "";
          if (resourceBundle != null) {
            errorMessage = resourceBundle.getString("UIWikiMovePageForm.msg.no-permission-at-wiki-destination");
          }
          return Response.serverError().entity("{ \"message\": \"" + errorMessage + "\"}").cacheControl(cc).build();
        }
      }

      context.put(TreeNode.SELECTED_PAGE, note);
      context.put(TreeNode.CAN_EDIT, null);
      context.put(TreeNode.SHOW_EXCERPT, null);
      Deque<WikiPageParams> stk = Utils.getStackParams(note);
      context.put(TreeNode.STACK_PARAMS, stk);

      JsonNodeData rootNodeData = null;
      List<JsonNodeData> treeNodeData = new ArrayList<>();
      if (withDrafts) {
        context.put(TreeNode.DEPTH, "1");
        rootNodeData = getJsonTree(noteParam, context, identity, request.getLocale()).getFirst();
        rootNodeData.setChildren(new ArrayList<>());
        rootNodeData.setHasDraftDescendant(true);
        buildNoteDraftsTree(rootNodeData, treeNodeData, noteParam, path, context, identity, request.getLocale());
      } else if (treeType.equalsIgnoreCase(TREETYPE.ALL.toString())) {
        responseData = getJsonTree(noteParam, context, identity, request.getLocale());
        rootNodeData = responseData.getFirst();
        buildNotesTree(rootNodeData, treeNodeData, context, identity, request.getLocale());
      } else if (treeType.equalsIgnoreCase(TREETYPE.CHILDREN.toString())) {
        context.put(TreeNode.DEPTH, "1");
        responseData = getJsonDescendants(noteParam, context, identity, request.getLocale());
        treeNodeData.addAll(responseData);
      } else {
        rootNodeData = getJsonTree(noteParam, context, identity, request.getLocale()).getFirst();
        treeNodeData.addAll(rootNodeData.getChildren());
      }
      if (rootNodeData != null) {
        responseData = List.of(rootNodeData);
      }
      BeanToJsons<JsonNodeData> toJsons = new BeanToJsons<>(responseData, treeNodeData);
      return Response.ok(toJsons, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", path, e);
      return Response.status(Response.Status.UNAUTHORIZED).build();
    } catch (Exception e) {
      log.error("Failed for get tree data by rest service - Cause : " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  /**
   * Return a list of title based on a searched words.
   *
   * @param uriInfo uriInfo
   * @param keyword Word to search
   * @param wikiType It can be a Portal, Group, User type of wiki
   * @param wikiOwner Is the owner of the wiki
   * @return List of title
   * @throws Exception if an error occured
   */
  @GET
  @Path("contextsearch/")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response searchData(@Context
                                     UriInfo uriInfo, @QueryParam("keyword")
                                     String keyword, @QueryParam("limit")
                                     int limit, @QueryParam("wikiType")
                                     String wikiType, @QueryParam("wikiOwner")
                                     String wikiOwner, @QueryParam("favorites")
                                     boolean favorites, @QueryParam("tags") List<String> tagNames) throws Exception {
    limit = limit > 0 ? limit : RestUtils.getLimit(uriInfo);
    try {

      keyword = keyword.toLowerCase();
      Identity currentIdentity = ConversationState.getCurrent().getIdentity();
      WikiSearchData data = new WikiSearchData(keyword, currentIdentity.getUserId());
      data.setLimit(limit);
      data.setFavorites(favorites);
      data.setTagNames(tagNames);
      List<SearchResult> results = noteService.search(data).getAll();
      List<TitleSearchResult> titleSearchResults = new ArrayList<>();
      for (SearchResult searchResult : results) {
        Page page = noteService.getNoteOfNoteBookByName(searchResult.getWikiType(),
                                                        searchResult.getWikiOwner(),
                                                        searchResult.getPageName(),
                                                        searchResult.getLang(),
                                                        currentIdentity);
        if (page != null) {
          page.setUrl(searchResult.getUrl() != null && !searchResult.getUrl().isBlank() ? searchResult.getUrl() : page.getUrl() + "?translation="+ searchResult.getLang());
          if (SearchResultType.ATTACHMENT.equals(searchResult.getType())) {
            Attachment attachment = noteBookService.getAttachmentOfPageByName(searchResult.getAttachmentName(),
                            page);
            TitleSearchResult titleSearchResult = new TitleSearchResult();
            titleSearchResult.setTitle(attachment.getName());
            titleSearchResult.setId(page.getId());
            titleSearchResult.setActivityId(page.getActivityId());
            titleSearchResult.setType(searchResult.getType());
            titleSearchResult.setUrl(attachment.getDownloadURL());
            titleSearchResult.setMetadatas(page.getMetadatas());
            titleSearchResult.setLang(searchResult.getLang());
            titleSearchResults.add(titleSearchResult);
          } else if (searchResult.getPoster() != null || searchResult.getPageName().equals(WikiPageParams.WIKI_HOME)) {
            PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()), null);
            org.exoplatform.social.core.identity.model.Identity poster = searchResult.getPoster();
            if (pageVersion != null) {
              poster = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, pageVersion.getAuthor());
            }
            IdentityEntity posterIdentity =
                    poster != null ? EntityBuilder.buildEntityIdentity(poster, uriInfo.getPath(), "all")
                            : null;
            IdentityEntity wikiOwnerIdentity =
                    searchResult.getWikiOwnerIdentity() != null ? EntityBuilder.buildEntityIdentity(searchResult.getWikiOwnerIdentity(),
                            uriInfo.getPath(),
                            "all")
                            : null;
            TitleSearchResult titleSearchResult = new TitleSearchResult();
            titleSearchResult.setTitle(searchResult.getTitle());
            titleSearchResult.setId(page.getId());
            titleSearchResult.setActivityId(page.getActivityId());
            if (posterIdentity != null) {
              titleSearchResult.setPoster(posterIdentity);
            }
            titleSearchResult.setWikiOwner(wikiOwnerIdentity);
            titleSearchResult.setExcerpt(searchResult.getExcerpt());
            titleSearchResult.setCreatedDate(searchResult.getCreatedDate().getTimeInMillis());
            titleSearchResult.setType(searchResult.getType());
            titleSearchResult.setUrl(page.getUrl());
            titleSearchResult.setLang(searchResult.getLang());
            titleSearchResult.setMetadatas(page.getMetadatas());
            titleSearchResults.add(titleSearchResult);
          }
        } else {
          log.warn("Cannot get page of search result " + searchResult.getWikiType() + ":" + searchResult.getWikiOwner() + ":"
                  + searchResult.getPageName());
        }
      }
      return Response.ok(new BeanToJsons(titleSearchResults), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Return a list of available languages.
   *
   * @return List of languages
   */
  @GET
  @Path("languages")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getAvailableLanguages(@Context
  UriInfo uriInfos, @QueryParam("lang")
  String lang) {
    try {
      Set<Locale> locales = LocaleContextInfoUtils.getSupportedLocales();
      List<Locale> localesList = new ArrayList<>(locales);
      JSONArray localesJSON = new JSONArray();
      Locale currentLocal = Locale.ENGLISH;
      if (!lang.isEmpty()) {
        Optional<Locale> opLocal = Arrays.stream(Locale.getAvailableLocales())
                                         .filter(local -> local.getLanguage().equals(lang))
                                         .findAny();
        if (opLocal.isPresent()) {
          currentLocal = opLocal.get();
        }
      }
      for (Locale locale : localesList) {
        JSONObject object = new JSONObject();
        if (locale.toString().equals("ma")) {
          continue;
        } else {
          object.put("value", locale.toString());
          object.put("text", locale.getDisplayName(currentLocal));
        }
        localesJSON.add(object);
      }
      return Response.ok(localesJSON, MediaType.APPLICATION_JSON).build();
    } catch (Exception e) {
      return Response.status(HTTPStatus.INTERNAL_ERROR).build();
    }
  }

  @GET
  @Path( "/illustration/{noteId}")
  @Operation(
          summary = "Gets a note featured image illustration by note Id",
          description = "Gets a note featured image illustration by note Id",
          method = "GET")
  @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "500", description = "Internal server error"),
          @ApiResponse(responseCode = "400", description = "Invalid query input"),
          @ApiResponse(responseCode = "404", description = "Resource not found") })
  public Response getFeaturedImageIllustration(@Context Request request,
                                       @Parameter(description = "target note id", required = true) @PathParam("noteId") Long noteId,
                                       @Parameter(description = "target version language", required = true) @QueryParam("isDraft") boolean isDraft,
                                       @Parameter(description = "target version language", required = true) @QueryParam("lang") String lang,
                                       @Parameter(description = "Optional size parameter", required = true) @QueryParam("size") String size,
                                       @Parameter(description = "Optional last modified parameter") @QueryParam("v") long lastModified) {

    if (noteId == null) {
      return Response.status(Response.Status.BAD_REQUEST).entity("note id is mandatory").build();
    }
    try {
      NoteFeaturedImage noteFeaturedImage = noteService.getNoteFeaturedImageInfo(noteId,
                                                                                 lang,
                                                                                 isDraft,
                                                                                 size,
                                                                                 RestUtils.getCurrentUserIdentityId());
      if (noteFeaturedImage == null) {
        return Response.status(HTTPStatus.NOT_FOUND).build();
      }
      Long lastUpdated = noteFeaturedImage.getLastUpdated();
      EntityTag eTag = new EntityTag(lastUpdated + noteId + lang + size, true);
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        InputStream stream = noteFeaturedImage.getFileInputStream();
        builder = Response.ok(stream, noteFeaturedImage.getMimeType());
        builder.tag(eTag);
        if (lastModified > 0) {
          builder.lastModified(new Date(lastUpdated));
          builder.expires(new Date(System.currentTimeMillis() + CACHE_DURATION_MILLISECONDS));
          builder.cacheControl(ILLUSTRATION_CACHE_CONTROL);
        }
      }
      return builder.build();
    } catch (ObjectNotFoundException e) {
      log.warn("target note not found", e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("An error occurred while getting featured image illustration", e);
      return Response.serverError().build();
    }
  }

  private List<JsonNodeData> getJsonTree(WikiPageParams params, Map<String, Object> context, Identity identity, Locale locale) throws Exception {
    Wiki noteBook = noteBookService.getWikiByTypeAndOwner(params.getType(), params.getOwner());
    WikiTreeNode noteBookNode = new WikiTreeNode(noteBook);
    noteBookNode.pushDescendants(context, ConversationState.getCurrent().getIdentity().getUserId());
    return TreeUtils.tranformToJson(noteBookNode, context, identity, locale);
  }

  private List<JsonNodeData> getJsonDescendants(WikiPageParams params,
                                                Map<String, Object> context,
                                                Identity identity,
                                                Locale locale) throws Exception {
    TreeNode treeNode = TreeUtils.getDescendants(params, context, ConversationState.getCurrent().getIdentity().getUserId());
    return TreeUtils.tranformToJson(treeNode, context, identity, locale);
  }

  private Page updateChildrenContainer(Page note) throws WikiException {
    String content = note.getContent();
    String oldChildrenContainer =
                                "<div class=\"wiki-children-pages ck-widget\" contenteditable=\"false\"><exo-wiki-children-pages>&nbsp;</exo-wiki-children-pages></div>";
    String childrenContainer = "<div class=\"navigation-img-wrapper\" contenteditable=\"false\" id=\"note-children-container\">\n"
        + "<figure class=\"image-navigation\" contenteditable=\"false\"><img alt=\"\" data-plugin-name=\"selectImage\" referrerpolicy=\"no-referrer\" role=\"presentation\" src=\"/notes/images/children.png\" /><img alt=\"remove treeview\" data-plugin-name=\"selectImage\" id=\"remove-treeview\" referrerpolicy=\"no-referrer\" src=\"/notes/images/trash.png\" />\n"
        + "<figcaption class=\"note-navigation-label\">Navigation</figcaption>\n" + "</figure>\n" + "</div>\n" + "\n"
        + "<p>&nbsp;</p>\n";
    content = content.replace(oldChildrenContainer, childrenContainer);
    note.setContent(content);
    return noteService.updateNote(note);
  }

  private String formatWikiOwnerToGroupId(String wikiOwner) {
    if (wikiOwner == null || wikiOwner.length() == 0) {
      return null;
    }
    if (!wikiOwner.startsWith("/")) {
      wikiOwner = "/" + wikiOwner;
    }
    if (wikiOwner.endsWith("/")) {
      wikiOwner = wikiOwner.substring(0, wikiOwner.length() - 1);
    }
    return wikiOwner;
  }

  private String sanitizeAndSubstituteMentions(String content, String local) {
    try {
      Locale locale = local == null ? null : Locale.forLanguageTag(local);
      String sanitizedBody = HTMLSanitizer.sanitize(content);
      sanitizedBody = sanitizedBody.replace("&#64;", "@");
      return MentionUtils.substituteUsernames(CommonsUtils.getCurrentPortalOwner(),sanitizedBody, locale);
    } catch (Exception e) {
      return content;
    }
  }

  private void buildNotesTree(JsonNodeData rootNode,
                              List<JsonNodeData> treeNodeData,
                              Map<String, Object> context,
                              Identity identity,
                              Locale locale) throws Exception {
    for (JsonNodeData child : rootNode.getChildren()) {
      treeNodeData.add(child);
      if (child.isHasChild()) {
        String path = URLDecoder.decode(child.getPath(), StandardCharsets.UTF_8);
        context.put(TreeNode.PATH, path);
        WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
        try {
          Page parentNote = noteService.getNoteOfNoteBookByName(noteParam.getType(),
                                                                noteParam.getOwner(),
                                                                noteParam.getPageName(),
                                                                identity);
          context.put(TreeNode.SELECTED_PAGE, parentNote);
        } catch (EntityNotFoundException e) {
          log.warn("Cannot find the note {}", noteParam.getPageName());
        }
        List<JsonNodeData> children = getJsonDescendants(noteParam, context, identity, locale);
        child.setChildren(children);
        treeNodeData.addAll(children);
      }
      buildNotesTree(child, treeNodeData, context, identity, locale);
    }
  }

  private void buildNoteDraftsTree(JsonNodeData rootNode,
                                   List<JsonNodeData> treeNodeData,
                                   WikiPageParams params,
                                   String path,
                                   Map<String, Object> context,
                                   Identity identity,
                                   Locale locale) {
    List<DraftPage> draftPages = noteService.getDraftsOfWiki(params.getOwner(), params.getType(), params.getPageName());
    Map<String, List<JsonNodeData>> draftsByParentPage = draftPages.stream().map(draftPage -> {
      try {
        PageTreeNode pageTreeNode = new PageTreeNode(draftPage);
        Boolean canEdit = context != null && context.get(TreeNode.CAN_EDIT) != null && (Boolean) context.get(TreeNode.CAN_EDIT);
        return TreeUtils.toJsonNodeData(pageTreeNode, path, draftPage, context, canEdit, true, identity, locale, noteService);
      } catch (Exception e) {
        return null;
      }}).filter(Objects::nonNull).collect(Collectors.groupingBy(
                                    JsonNodeData::getParentPageId,
                                    Collectors.collectingAndThen(
                                    Collectors.toList(),
                                    list -> list.stream()
                                    .sorted((x, y) -> new NaturalComparator().compare(x.getName(), y.getName()))
                                    .collect(Collectors.toList()))));
    draftsByParentPage.forEach((parentPageId, drafts) -> {
      try {
        drafts = TreeUtils.cleanDraftChildren(drafts, locale);
        JsonNodeData parentJsonData = findParent(parentPageId, treeNodeData);
        if (parentJsonData == null) {
          Page parent = noteService.getNoteById(String.valueOf(parentPageId));
          PageTreeNode pageTreeNode = new PageTreeNode(parent);
          Boolean canEdit = context != null && context.get(TreeNode.CAN_EDIT) != null && (Boolean) context.get(TreeNode.CAN_EDIT);
          parentJsonData = TreeUtils.toJsonNodeData(pageTreeNode,
                                                    path,
                                                    parent,
                                                    context,
                                                    canEdit,
                                                    true,
                                                    identity,
                                                    locale,
                                                    noteService);
        }
        if (!Objects.equals(parentJsonData.getNoteId(), rootNode.getNoteId())) {
          parentJsonData.addChildren(drafts);
          treeNodeData.add(parentJsonData);
          buildPageAscendants(rootNode, parentJsonData, treeNodeData, context, identity, locale, path);
        } else {
          rootNode.addChildren(drafts);
        }
      } catch (Exception e) {
        log.error("Error while building draft tree descendants", e);
      }
    });
  }

  private void buildPageAscendants(JsonNodeData rootNode,
                                   JsonNodeData parentJsonData,
                                   List<JsonNodeData> treeNodeData,
                                   Map<String, Object> context,
                                   Identity identity,
                                   Locale locale,
                                   String path) throws Exception {
    String parentPageId = parentJsonData.getParentPageId();
    if (parentPageId != null && !parentPageId.equals(rootNode.getNoteId())) {
      JsonNodeData parentNode = findParent(parentPageId, treeNodeData);
      if (parentNode == null) {
        Page parent = noteService.getNoteById(parentJsonData.getParentPageId());
        PageTreeNode pageTreeNode = new PageTreeNode(parent);
        Boolean canEdit = context != null && context.get(TreeNode.CAN_EDIT) != null && (Boolean) context.get(TreeNode.CAN_EDIT);
        parentNode = TreeUtils.toJsonNodeData(pageTreeNode, path, parent, context, canEdit, true, identity, locale, noteService);
      }
      addChildIfNoExists(parentNode, parentJsonData);
      treeNodeData.add(parentNode);
      buildPageAscendants(rootNode, parentNode, treeNodeData, context, identity, locale, path);
    } else {
      addChildIfNoExists(rootNode, parentJsonData);
    }
  }

  private JsonNodeData findParent(String parentId, List<JsonNodeData> list) {
    return list.stream().filter(obj -> obj.getNoteId().equals(parentId)).findFirst().orElse(null);
  }

  private void addChildIfNoExists(JsonNodeData parent, JsonNodeData child) {
    if (findParent(child.getNoteId(), parent.getChildren()) == null) {
      parent.addChildren(List.of(child));
    }
  }
}
