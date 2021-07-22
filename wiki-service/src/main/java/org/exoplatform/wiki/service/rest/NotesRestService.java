/*
 * Copyright (C) 2003-2019 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.rest;

import java.net.URLDecoder;
import java.util.*;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.apache.commons.lang.math.NumberUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.mow.api.PermissionType;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.impl.BeanToJsons;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.WikiTreeNode;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiHTMLSanitizer;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;

@Path("/notes")
@Api(value = "/notes", description = "Managing notes")
@RolesAllowed("users")

public class NotesRestService implements ResourceContainer {

  private static Log                  log = ExoLogger.getLogger(NotesRestService.class);
  private final NoteService           noteService;
  private final WikiService           noteBookService;
  private final ResourceBundleService resourceBundleService;
  private final CacheControl          cc;

  public NotesRestService(NoteService noteService, WikiService noteBookService, ResourceBundleService resourceBundleService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.resourceBundleService = resourceBundleService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  @GET
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get note by notes params", httpMethod = "GET", response = Response.class, notes = "This get the not if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getNote(@ApiParam(value = "NoteBook Type", required = true) @PathParam("noteBookType") String noteBookType,
                          @ApiParam(value = "NoteBook Owner", required = true) @PathParam("noteBookOwner") String noteBookOwner,
                          @ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                          @ApiParam(value = "source", required = true) @QueryParam("source") String source) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId, identity, source);
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      note.setContent(WikiHTMLSanitizer.markupSanitize(note.getContent()));
      note.setBreadcrumb(noteService.getBreadcumb(noteBookType, noteBookOwner, noteId));
      return Response.ok(note).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}:{}:{}", noteBookType, noteBookOwner, noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Can't get note {}:{}:{}", noteBookType, noteBookOwner, noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/note/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get note by id", httpMethod = "GET", response = Response.class, notes = "This get the not if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getNoteById(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                              @ApiParam(value = "noteBookType", required = false) @QueryParam("noteBookType") String noteBookType,
                              @ApiParam(value = "noteBookOwner", required = false) @QueryParam("noteBookOwner") String noteBookOwner,
                              @ApiParam(value = "source", required = false) @QueryParam("source") String source) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity, source);
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if(StringUtils.isNotEmpty(noteBookType) && !note.getWikiType().equals(noteBookType)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if(StringUtils.isNotEmpty(noteBookOwner) && !note.getWikiOwner().equals(noteBookOwner)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      note.setContent(WikiHTMLSanitizer.markupSanitize(note.getContent()));
      note.setBreadcrumb(noteService.getBreadcumb(note.getWikiType(), note.getWikiOwner(), note.getName()));
      return Response.ok(note).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Can't get note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @POST
  @Path("/note")
  @RolesAllowed("users")
  @ApiOperation(value = "Add a new note", httpMethod = "POST", response = Response.class, notes = "This adds a new note.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response createNote(@ApiParam(value = "note object to be created", required = true) Page note) {
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
      if (noteBookService.isExisting(noteBookType, noteBookOwner, TitleResolver.getId(note.getTitle(), false))) {
        return Response.status(Response.Status.CONFLICT).entity("Note name already exists").build();
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
      note.setName(TitleResolver.getId(note.getTitle(), false));
      note.setUrl("");
      Page createdNote = noteService.createNote(noteBook, note.getParentPageName(), note, identity);
      return Response.ok(createdNote, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", note.getName(), e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.warn("Failed to perform save noteBook note {}:{}:{}", noteBookType, noteBookOwner, note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PUT
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Updates a specific note by note's params", httpMethod = "PUT", response = Response.class, notes = "This updates the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response updateNote(@ApiParam(value = "NoteBook Type", required = true) @PathParam("noteBookType") String noteBookType,
                             @ApiParam(value = "NoteBook Owner", required = true) @PathParam("noteBookOwner") String noteBookOwner,
                             @ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                             @ApiParam(value = "note object to be updated", required = true) Page note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (NumberUtils.isNumber(note.getTitle())) {
      log.warn("Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Note's title should not be number}").build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      if (!noteService.hasPermissionOnNote(note_, PermissionType.EDITPAGE, identity)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      note_.setToBePublished(note.isToBePublished());
      if ((!note_.getTitle().equals(note.getTitle()))
          && (noteBookService.isExisting(noteBookType, noteBookOwner, TitleResolver.getId(note.getTitle(), false)))) {
        return Response.status(Response.Status.CONFLICT).entity("Note name already exists").build();
      }
      if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        note_.setTitle(note.getTitle());
        note_.setContent(note.getContent());
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
        noteService.createVersionOfNote(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(noteBookType, noteBookOwner, newNoteName);
          // noteService.removeDraftOfNote(noteParams);
        }
      } else if (!note_.getTitle().equals(note.getTitle())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_.setTitle(note.getTitle());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
        noteService.createVersionOfNote(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(noteBookType, noteBookOwner, newNoteName);
          // noteService.removeDraftOfPage(noteParams);
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        noteService.createVersionOfNote(note_);
      }
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.error("Failed to perform update noteBook note {}:{}:{}", note.getWikiType(), note.getWikiOwner(), note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PUT
  @Path("/note/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Updates a specific note by id", httpMethod = "PUT", response = Response.class, notes = "This updates the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response updateNoteById(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                                 @ApiParam(value = "note object to be updated", required = true) Page note) {
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
      if (!noteService.hasPermissionOnNote(note_, PermissionType.EDITPAGE, identity)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      note_.setToBePublished(note.isToBePublished());
      if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        note_.setTitle(note.getTitle());
        note_.setContent(note.getContent());
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
        noteService.createVersionOfNote(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams);
        }
      } else if (!note_.getTitle().equals(note.getTitle())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_.setTitle(note.getTitle());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
        noteService.createVersionOfNote(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams);
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        noteService.createVersionOfNote(note_);
      }
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have edit permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.error("Failed to perform update noteBook note {}:{}:{}", note.getWikiType(), note.getWikiOwner(), note.getId(), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @DELETE
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Delete note by note's params", httpMethod = "PUT", response = Response.class, notes = "This delets the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response deleteNote(@ApiParam(value = "NoteBook Type", required = true) @PathParam("noteBookType") String noteBookType,
                             @ApiParam(value = "NoteBook Owner", required = true) @PathParam("noteBookOwner") String noteBookOwner,
                             @ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId, identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      noteService.deleteNote(noteBookType, noteBookOwner, noteId, identity);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      log.error("User does not have delete permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.warn("Failed to perform Delete of noteBook note {}:{}:{}", noteBookType, noteBookOwner, noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @DELETE
  @Path("/note/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Delete note by note's params", httpMethod = "PUT", response = Response.class, notes = "This delets the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response deleteNoteById(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity);
      String noteName = note.getName();
      if (note == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      noteService.deleteNote(note.getWikiType(), note.getWikiOwner(), noteName, identity);
      return Response.ok().build();
    } catch (IllegalAccessException e) {
      log.error("User does not have delete permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.warn("Failed to perform Delete of noteBook note {}", noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @PATCH
  @Path("/note/move/{noteId}/{destinationNoteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Move note under the destination one", httpMethod = "PUT", response = Response.class, notes = "This moves the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response moveNote(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                           @ApiParam(value = "Destination Note id", required = true) @PathParam("destinationNoteId") String toNoteId) {

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
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.warn("Failed to perform move of noteBook note {} under {}", noteId, toNoteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/tree/{type}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get node's tree", httpMethod = "GET", response = Response.class, notes = "Display the current tree of a noteBook based on is path")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getTreeData(@PathParam("type") String type,
                              @QueryParam(TreeNode.PATH) String path,
                              @QueryParam(TreeNode.CURRENT_PATH) String currentPath,
                              @QueryParam(TreeNode.CAN_EDIT) Boolean canEdit,
                              @QueryParam(TreeNode.SHOW_EXCERPT) Boolean showExcerpt,
                              @QueryParam(TreeNode.DEPTH) String depth) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      List<JsonNodeData> responseData = new ArrayList<JsonNodeData>();
      HashMap<String, Object> context = new HashMap<String, Object>();
      context.put(TreeNode.CAN_EDIT, canEdit);
      if (currentPath != null) {
        currentPath = URLDecoder.decode(currentPath, "utf-8");
        context.put(TreeNode.CURRENT_PATH, currentPath);
        WikiPageParams currentNoteParam = TreeUtils.getPageParamsFromPath(currentPath);
        Page currentNote = noteService.getNoteOfNoteBookByName(currentNoteParam.getType(),
                                                               currentNoteParam.getOwner(),
                                                               currentNoteParam.getPageName(),
                                                               identity);
        context.put(TreeNode.CURRENT_PAGE, currentNote);
      }

      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);

      // Put select note to context
      path = URLDecoder.decode(path, "utf-8");
      context.put(TreeNode.PATH, path);
      WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
      Page note =
                noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.getPageName(), identity);
      if (note == null) {
        log.warn("User [{}] can not get noteBook path [{}]. Wiki Home is used instead",
                 ConversationState.getCurrent().getIdentity().getUserId(),
                 path);
        note = noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.WIKI_HOME);
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

      context.put(TreeNode.SHOW_EXCERPT, showExcerpt);
      if (type.equalsIgnoreCase(TREETYPE.ALL.toString())) {
        Stack<WikiPageParams> stk = Utils.getStackParams(note);
        context.put(TreeNode.STACK_PARAMS, stk);
        responseData = getJsonTree(noteParam, context);
      } else if (type.equalsIgnoreCase(TREETYPE.CHILDREN.toString())) {
        // Get children only
        if (depth == null)
          depth = "1";
        context.put(TreeNode.DEPTH, depth);
        responseData = getJsonDescendants(noteParam, context);
      }

      encodeWikiTree(responseData, request.getLocale());
      return Response.ok(new BeanToJsons(responseData), MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", path, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Failed for get tree data by rest service - Cause : " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  private List<JsonNodeData> getJsonTree(WikiPageParams params, HashMap<String, Object> context) throws Exception {
    Wiki noteBook = noteBookService.getWikiByTypeAndOwner(params.getType(), params.getOwner());
    WikiTreeNode noteBookNode = new WikiTreeNode(noteBook);
    noteBookNode.pushDescendants(context);
    return TreeUtils.tranformToJson(noteBookNode, context);
  }

  private List<JsonNodeData> getJsonDescendants(WikiPageParams params, HashMap<String, Object> context) throws Exception {
    TreeNode treeNode = TreeUtils.getDescendants(params, context);
    return TreeUtils.tranformToJson(treeNode, context);
  }

  private void encodeWikiTree(List<JsonNodeData> responseData, Locale locale) throws Exception {
    ResourceBundle resourceBundle = resourceBundleService.getResourceBundle(Utils.WIKI_RESOUCE_BUNDLE_NAME, locale);
    String untitledLabel = "";
    if (resourceBundle == null) {
      // May happen in Tests
      log.warn("Cannot find resource bundle '{}'", Utils.WIKI_RESOUCE_BUNDLE_NAME);
    } else {
      untitledLabel = resourceBundle.getString("Page.Untitled");
    }

    for (JsonNodeData data : responseData) {
      if (StringUtils.isBlank(data.getName())) {
        data.setName(untitledLabel);
      }
      if (CollectionUtils.isNotEmpty(data.children)) {
        encodeWikiTree(data.children, locale);
      }
    }
  }

}
