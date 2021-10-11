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

import java.io.*;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.math.NumberUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.NoteToExport;
import org.exoplatform.wiki.mow.api.Page;
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
import org.json.JSONObject;

import io.swagger.annotations.*;
import io.swagger.jaxrs.PATCH;
import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URLDecoder;
import java.util.*;
import java.util.stream.Collectors;

@Path("/notes")
@Api(value = "/notes", description = "Managing notes")
@RolesAllowed("users")

public class NotesRestService implements ResourceContainer {

  private static final String         NOTE_NAME_EXISTS             = "Note name already exists";

  private static final Log            log                          = ExoLogger.getLogger(NotesRestService.class);

  private static final String         IMAGE_URL_REPLACEMENT_PREFIX = "//-";

  private static final String         IMAGE_URL_REPLACEMENT_SUFFIX = "-//";

  private static final String         EXPORT_ZIP_NAME              = "ziped.zip";

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

  public static File zipFiles(String zipFileName, List<File> addToZip) throws IOException {

    String zipPath = System.getProperty("java.io.tmpdir") + File.separator + zipFileName;
    new File(zipPath).delete();

    try (FileOutputStream fos = new FileOutputStream(zipPath);
        ZipOutputStream zos = new ZipOutputStream(new BufferedOutputStream(fos))) {
      zos.setLevel(9);

      for (File file : addToZip) {
        if (file.exists()) {
          try (FileInputStream fis = new FileInputStream(file)) {
            ZipEntry entry = new ZipEntry(file.getName());
            zos.putNextEntry(entry);
            for (int c = fis.read(); c != -1; c = fis.read()) {
              zos.write(c);
            }
            zos.flush();
          }
        }
      }
    }
    File zip = new File(zipPath);
    if (!zip.exists()) {
      throw new FileNotFoundException("The created zip file could not be found");
    }
    return zip;
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
      Wiki noteBook = null;
      noteBook = noteBookService.getWikiByTypeAndOwner(noteBookType, noteBookOwner);
      if (noteBook == null) {
        noteBook = noteBookService.createWiki(noteBookType, noteBookOwner);
      }
      Page note = null;
      if (noteId.equals("homeNote")) {
        noteId = noteBook.getWikiHome().getId();
        note = noteService.getNoteById(noteId, identity, source);
      } else {
        note = noteService.getNoteOfNoteBookByName(noteBookType, noteBookOwner, noteId, identity, source);
      }
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      String content = note.getContent();
      if (content.contains("class=\"noteLink\" href=\"//-")) {
        while (content.contains("class=\"noteLink\" href=\"//-")) {
          String linkedParams = content.split("class=\"noteLink\" href=\"//-")[1].split("-//\"")[0];
          String NoteName = linkedParams.split("-////-")[2];
          Page linkedNote = null;
          linkedNote = noteService.getNoteOfNoteBookByName(note.getWikiType(), note.getWikiOwner(), NoteName);
          if (linkedNote != null) {
            content = content.replaceAll("\"noteLink\" href=\"//-" + linkedParams + "-//",
                    "\"noteLink\" href=\"" + linkedNote.getId());
            if(content.equals(note.getContent())) break;
          }
        }
        if(!content.equals(note.getContent())){
          note.setContent(content);
          noteService.updateNote(note);
        }
      }
      note.setContent(HTMLSanitizer.sanitize(note.getContent()));
      note.setBreadcrumb(noteService.getBreadCrumb(noteBookType, noteBookOwner, noteId, false));
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
  @ApiOperation(value = "Get note by id", httpMethod = "GET", response = Response.class, notes = "This get the note if the authenticated user has permissions to view the objects linked to this note.")
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
      if (StringUtils.isNotEmpty(noteBookType) && !note.getWikiType().equals(noteBookType)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      if (StringUtils.isNotEmpty(noteBookOwner) && !note.getWikiOwner().equals(noteBookOwner)) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      note.setContent(HTMLSanitizer.sanitize(note.getContent()));
      note.setBreadcrumb(noteService.getBreadCrumb(note.getWikiType(), note.getWikiOwner(), note.getName(), false));
      return Response.ok(note).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have view permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception e) {
      log.error("Can't get note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/draftNote/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get draft note by id", httpMethod = "GET", response = Response.class, notes = "This returns the draft note if the authenticated user is the author of the draft.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getDraftNoteById(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      String currentUserId = identity.getUserId();
      DraftPage draftNote = noteService.getDraftNoteById(noteId, currentUserId);
      if (draftNote == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      Page parentPage = noteService.getNoteById(draftNote.getParentPageId(), identity);
      if (parentPage == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      draftNote.setContent(HTMLSanitizer.sanitize(draftNote.getContent()));
      draftNote.setBreadcrumb(noteService.getBreadCrumb(parentPage.getWikiType(), parentPage.getWikiOwner(), draftNote.getId(), true));
      
      return Response.ok(draftNote).build();
    } catch (Exception e) {
      log.error("Can't get draft note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/latestDraftNote/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get latest draft note of page", httpMethod = "GET", response = Response.class, notes = "This returns the latest draft of the note if the authenticated user is the author of the draft.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getLatestDraftOfPage(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      String currentUserId = identity.getUserId();
      Page targetPage = noteService.getNoteById(noteId);
      if (targetPage == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      DraftPage draftNote = noteService.getLatestDraftOfPage(targetPage, currentUserId);

      return Response.ok(draftNote != null ? draftNote : JSONObject.NULL).build();
    } catch (Exception e) {
      log.error("Can't get draft note {}", noteId, e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  @GET
  @Path("/versions/{noteId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  @ApiOperation(value = "Get versions of note by id", httpMethod = "GET", response = Response.class, notes = "This get the versions of a note if the authenticated user has permissions to view the objects linked to this note.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response getNoteVersions(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note = noteService.getNoteById(noteId, identity);
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok(noteService.getVersionsHistoryOfNote(note, identity.getUserId())).build();
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

  @POST
  @Path("saveDraft")
  @RolesAllowed("users")
  @ApiOperation(value = "Add or update a new note draft page", httpMethod = "POST", response = Response.class, notes = "This adds a new note draft page or updates an existing one.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
          @ApiResponse(code = 404, message = "Resource not found")})
  public Response saveDraft(@ApiParam(value = "Note draft page object to be created", required = true) DraftPage draftNoteToSave) {
    if (draftNoteToSave == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    if (NumberUtils.isNumber(draftNoteToSave.getTitle())) {
      log.warn("Draft Note's title should not be number");
      return Response.status(Response.Status.BAD_REQUEST).entity("{ message: Draft Note's title should not be number}").build();
    }

    String noteBookType = draftNoteToSave.getWikiType();
    String noteBookOwner = draftNoteToSave.getWikiOwner();
    Page parentNote;
    Page targetNote = null;
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      if (StringUtils.isNoneEmpty(draftNoteToSave.getParentPageId())) {
        parentNote = noteService.getNoteById(draftNoteToSave.getParentPageId(), identity);
        if (parentNote != null) {
          noteBookType = parentNote.getWikiType();
          noteBookOwner = parentNote.getWikiOwner();
          draftNoteToSave.setParentPageName(parentNote.getName());
        } else {
          return Response.status(Response.Status.BAD_REQUEST).build();
        }
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
        if (targetNote == null) {
          return Response.status(Response.Status.BAD_REQUEST).build();
        }
      }

      String syntaxId = noteBookService.getDefaultWikiSyntaxId();
      String currentUser = identity.getUserId();
      draftNoteToSave.setAuthor(currentUser);
      draftNoteToSave.setSyntax(syntaxId);

      if (StringUtils.isNoneEmpty(draftNoteToSave.getId())) {
        if (targetNote != null) {
          draftNoteToSave = noteService.updateDraftForExistPage(draftNoteToSave, targetNote, null, System.currentTimeMillis(), currentUser);
        } else {
          draftNoteToSave = noteService.updateDraftForNewPage(draftNoteToSave, System.currentTimeMillis());
        }
      } else {
        if (targetNote != null) {
          draftNoteToSave = noteService.createDraftForExistPage(draftNoteToSave, targetNote, null, System.currentTimeMillis(), currentUser);
        } else {
          draftNoteToSave = noteService.createDraftForNewPage(draftNoteToSave, System.currentTimeMillis());
        }
      }

      return Response.ok(draftNoteToSave, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      log.warn("Failed to perform save noteBook draft note {}:{}:{}", noteBookType, noteBookOwner, draftNoteToSave.getId(), ex);
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

      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      note_.setToBePublished(note.isToBePublished());
      if ((!note_.getTitle().equals(note.getTitle()))
          && (noteBookService.isExisting(noteBookType, noteBookOwner, TitleResolver.getId(note.getTitle(), false)))) {
        return Response.status(Response.Status.CONFLICT).entity(NOTE_NAME_EXISTS).build();
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
        noteService.createVersionOfNote(note_, identity.getUserId());
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
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(noteBookType, noteBookOwner, newNoteName);
          // noteService.removeDraftOfPage(noteParams);
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
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
      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      if ((!note_.getTitle().equals(note.getTitle()))
          && (noteBookService.isExisting(note.getWikiType(), note.getWikiOwner(), TitleResolver.getId(note.getTitle(), false)))) {
        return Response.status(Response.Status.CONFLICT).entity(NOTE_NAME_EXISTS).build();
      }
      note_.setToBePublished(note.isToBePublished());
      String newNoteName = TitleResolver.getId(note.getTitle(), false);
      if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        note_.setTitle(note.getTitle());
        note_.setContent(note.getContent());
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams);
        }
      } else if (!note_.getTitle().equals(note.getTitle())) {
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
            && !note.getName().equals(newNoteName)) {
          noteService.renameNote(note_.getWikiType(), note_.getWikiOwner(), note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_.setTitle(note.getTitle());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_TITLE, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams);
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        note_ = noteService.updateNote(note_, PageUpdateType.EDIT_PAGE_CONTENT, identity);
        noteService.createVersionOfNote(note_, identity.getUserId());
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(note_.getWikiType(), note_.getWikiOwner(), newNoteName);
          noteService.removeDraftOfNote(noteParams);
        }
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

  @PUT
  @Path("/restore/{noteVersion}")
  @RolesAllowed("users")
  @ApiOperation(value = "Restore a specific note version by version id", httpMethod = "PUT", response = Response.class, notes = "This restore the note if the authenticated user has UPDATE permissions.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
    @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
    @ApiResponse(code = 404, message = "Resource not found") })
  public Response RestoreNoteVersion(@ApiParam(value = "Version Number", required = true) @PathParam("noteVersion") String noteVersion,
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
      String currentUser = identity.getUserId();
      Page note_ = noteService.getNoteById(note.getId(), identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      if (!note_.isCanManage()) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }
      noteService.restoreVersionOfNote(noteVersion,note,currentUser);
      return Response.ok(note_, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (IllegalAccessException e) {
      log.error("User does not have permissions to restore the note {} version", note.getId(), e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.error("Failed to perform restore note version {}", noteVersion, ex);
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
      // remove draft note
      if (!"__anonim".equals(identity.getUserId())) {
        WikiPageParams noteParams = new WikiPageParams(note.getWikiType(), note.getWikiOwner(), noteName);
        noteService.removeDraftOfNote(noteParams);
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

  @DELETE
  @Path("/draftNote/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Delete note by note's params", httpMethod = "PUT", response = Response.class, notes = "This delets the note if the authenticated user has EDIT permissions.")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
          @ApiResponse(code = 404, message = "Resource not found")})
  public Response deleteDraftNote(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId) {

    try {
      String currentUserId = ConversationState.getCurrent().getIdentity().getUserId();
      DraftPage draftNote = noteService.getDraftNoteById(noteId, currentUserId);
      String draftNoteName = draftNote.getName();
      if (draftNote == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      noteService.removeDraft(draftNoteName);
      return Response.ok().build();
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
  @Path("/note/export/{notes}")
  @RolesAllowed("users")
  @ApiOperation(value = "Export notes", httpMethod = "PUT", response = Response.class, notes = "This export selected notes and provide a zip file.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response exportNote(@ApiParam(value = "List of notes ids", required = true) @PathParam("notes") String notesList,
                             @ApiParam(value = "exportChildren") @QueryParam("exportChildren") Boolean exportChildren) {

    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      File zipped = null;

      String[] notes = notesList.split(",");
      List<NoteToExport> notes_ = noteService.getNotesToExport(notes, exportChildren, identity);
      List<File> files = new ArrayList<>();
      File temp;

      temp = File.createTempFile("notesExport_" + new Date().getTime(), ".txt");
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(notes_);
      String contentUpdated = json;
      String fileName = "";
      String filePath = "";
      while (contentUpdated.contains(IMAGE_URL_REPLACEMENT_PREFIX)) {
        fileName = contentUpdated.split(IMAGE_URL_REPLACEMENT_PREFIX)[1].split(IMAGE_URL_REPLACEMENT_SUFFIX)[0];
        filePath = System.getProperty("java.io.tmpdir") + File.separator + fileName;
        files.add(new File(filePath));
        contentUpdated = contentUpdated.replace(IMAGE_URL_REPLACEMENT_PREFIX + fileName + IMAGE_URL_REPLACEMENT_SUFFIX, "");
      }
      BufferedWriter bw = null;
      bw = new BufferedWriter(new FileWriter(temp));
      bw.write(json);
      if (bw != null)
        bw.close();
      files.add(temp);
      zipped = zipFiles(EXPORT_ZIP_NAME, files);
      for(File file:files){
        file.delete();
      }
      return Response.ok(FileUtils.readFileToByteArray(zipped))
                     .type("application/zip")
                     .header("Content-Disposition", "attachment; filename=\"notesExport_" + new Date().getTime() + ".zip\"")
                     .build();

    } catch (Exception ex) {
      log.warn("Failed to export notes ", ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  @POST
  @Path("/note/import/{noteId}")
  @RolesAllowed("users")
  @ApiOperation(value = "Import notes from a zip file", httpMethod = "POST", response = Response.class, notes = "This import notes from defined zip file under given note.")
  @ApiResponses(value = { @ApiResponse(code = 200, message = "Request fulfilled"),
      @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
      @ApiResponse(code = 404, message = "Resource not found") })
  public Response importNote(@ApiParam(value = "Note id", required = true) @PathParam("noteId") String noteId,
                             @ApiParam(value = "Conflict", required = true) @QueryParam("conflict") String conflict) {

    try {

      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteService.getNoteById(noteId, identity);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String zipPath = System.getProperty("java.io.tmpdir") + File.separator + EXPORT_ZIP_NAME;
      List<String> files = unzip(zipPath);
      String notesFilePath = "";
      for (String file :files){
        if(file.contains("notesExport_")){{
          notesFilePath = file;
          break;
        }}
      }
      if(!notesFilePath.equals("")) {
        ObjectMapper mapper = new ObjectMapper();
        List<Page> notes = mapper.readValue(new File(notesFilePath), new TypeReference<List<Page>>() {
        });
        Wiki wiki = noteBookService.getWikiByTypeAndOwner(note_.getWikiType(), note_.getWikiOwner());
        noteService.importNotes(notes, note_, wiki, conflict);
      }
      return Response.ok().build();

    } catch (IllegalAccessException e) {
      log.error("User does not have move permissions on the note {}", noteId, e);
      return Response.status(Response.Status.NOT_FOUND).build();
    } catch (Exception ex) {
      log.warn("Failed to export note {} ", noteId, ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  private List<String> unzip(String zipFilePath) throws IOException {
/*    String fileName = "zip";
    List<String> files = new ArrayList<>();
    int index = EXPORT_ZIP_NAME.lastIndexOf('.');
    if(index > 0) {
      fileName = EXPORT_ZIP_NAME.substring(0,index);
    }*/
    List<String> files = new ArrayList<>();
    String folderPath = System.getProperty("java.io.tmpdir");//+ File.separator + fileName;
    File destDir = new File(folderPath);
    if (!destDir.exists()) {
      destDir.mkdir();
    }
    ZipInputStream zipIn = new ZipInputStream(new FileInputStream(zipFilePath));
    ZipEntry entry = zipIn.getNextEntry();
    while (entry != null) {
      String filePath = folderPath + File.separator + entry.getName();
      if (!entry.isDirectory()) {
        extractFile(zipIn, filePath);
        files.add(filePath);
      } else {
        File dir = new File(filePath);
        dir.mkdirs();
      }
      zipIn.closeEntry();
      entry = zipIn.getNextEntry();
    }
    zipIn.close();
    return files;
  }

  private void extractFile(ZipInputStream zipIn, String filePath) throws IOException {
    BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
    byte[] bytesIn = new byte[4096];
    int read = 0;
    while ((read = zipIn.read(bytesIn)) != -1) {
      bos.write(bytesIn, 0, read);
    }
    bos.close();
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
        currentPath = URLDecoder.decode(currentPath, StandardCharsets.UTF_8);
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
      path = URLDecoder.decode(path, StandardCharsets.UTF_8);
      context.put(TreeNode.PATH, path);
      WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
      Page note =
                noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.getPageName(), identity);
      if (note == null) {
        log.warn("User [{}] can not get noteBook path [{}]. Home is used instead",
                 ConversationState.getCurrent().getIdentity().getUserId(),
                 path);
        note = noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), WikiPageParams.WIKI_HOME);
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
        Deque<WikiPageParams> stk = Utils.getStackParams(note);
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

  @GET
  @Path("/tree/full")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @ApiOperation(value = "Get node's tree", httpMethod = "GET", response = Response.class, notes = "Display the current tree of a noteBook based on is path")
  @ApiResponses(value = {@ApiResponse(code = 200, message = "Request fulfilled"),
          @ApiResponse(code = 400, message = "Invalid query input"), @ApiResponse(code = 403, message = "Unauthorized operation"),
          @ApiResponse(code = 404, message = "Resource not found")})
  public Response getFullTreeData(@ApiParam(value = "Note path", required = true) @QueryParam(TreeNode.PATH) String path,
                                  @ApiParam(value = "With draft notes", required = true) @QueryParam("withDrafts") Boolean withDrafts) {
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      List<JsonNodeData> responseData;
      HashMap<String, Object> context = new HashMap<>();
      context.put(TreeNode.WITH_DRAFTS, withDrafts);

      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);

      // Put select note to context
      path = URLDecoder.decode(path, "utf-8");
      context.put(TreeNode.PATH, path);
      WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
      Page note = noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.getPageName(), identity);
      if (note == null) {
        log.warn("User [{}] can not get noteBook path [{}]. Home is used instead",
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
      context.put(TreeNode.CAN_EDIT, null);
      context.put(TreeNode.SHOW_EXCERPT, null);
      Deque<WikiPageParams> stk = Utils.getStackParams(note);
      context.put(TreeNode.STACK_PARAMS, stk);

      List<JsonNodeData> finalTree = new ArrayList<>();
      responseData = getJsonTree(noteParam, context);
      JsonNodeData rootNodeData = responseData.get(0);
      finalTree.add(rootNodeData);
      context.put(TreeNode.DEPTH, "1");

      List<JsonNodeData> children = new ArrayList<>(rootNodeData.getChildren());
      List<JsonNodeData> parents = new ArrayList<>();

      do {
        parents.addAll(children);
        children.clear();
        for (JsonNodeData parent : parents) {
          if (parent.isHasChild()) {
            // Put select note to context
            path = URLDecoder.decode(parent.getPath(), "utf-8");
            context.put(TreeNode.PATH, path);
            noteParam = TreeUtils.getPageParamsFromPath(path);
            Page parentNote = noteService.getNoteOfNoteBookByName(noteParam.getType(), noteParam.getOwner(), noteParam.getPageName(), identity);
            context.put(TreeNode.SELECTED_PAGE, parentNote);
            List<JsonNodeData> childNotes = getJsonDescendants(noteParam, context);

            children.addAll(childNotes);
            parent.setChildren(childNotes);
          }
          finalTree.add(parent);
        }
        parents.clear();

      } while (!children.isEmpty());

      // from the bottom
      List<JsonNodeData> bottomChildren = withDrafts ? finalTree.stream().filter(JsonNodeData::isDraftPage).collect(Collectors.toList()) :
              finalTree.stream().filter(jsonNodeData -> !jsonNodeData.isHasChild()).collect(Collectors.toList());

      while (bottomChildren.size() > 1 || (bottomChildren.size() == 1 && bottomChildren.get(0).getParentPageId() != null)) {
        for (JsonNodeData bottomChild : bottomChildren) {
          String parentPageId = bottomChild.getParentPageId();
          Optional<JsonNodeData> parentOptional = finalTree.stream().filter(jsonNodeData -> StringUtils.equals(jsonNodeData.getNoteId(), parentPageId)).findFirst();
          if (parentOptional.isPresent()) {
            JsonNodeData parent = parentOptional.get();
            children = parent.getChildren();
            children.remove(bottomChild);
            children.add(bottomChild);
            if (withDrafts) {
              children = children.stream().filter(jsonNodeData -> jsonNodeData.isDraftPage() || !jsonNodeData.getChildren().isEmpty()).collect(Collectors.toList());
            }
            parent.setChildren(children);

            // update final tree
            if (finalTree.contains(parent)) {
              int index = finalTree.indexOf(parent);
              finalTree.set(index, parent);
            }
            if (parents.contains(parent)) {
              int index = parents.indexOf(parent);
              parents.set(index, parent);
            } else {
              parents.add(parent);
            }
          }
        }
        bottomChildren.clear();
        bottomChildren.addAll(parents);
        parents.clear();
      }

      encodeWikiTree(bottomChildren, request.getLocale());
      return Response.ok(new BeanToJsons(finalTree, bottomChildren), MediaType.APPLICATION_JSON).cacheControl(cc).build();
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
      if (CollectionUtils.isNotEmpty(data.getChildren())) {
        encodeWikiTree(data.getChildren(), locale);
      }
    }
  }

}
