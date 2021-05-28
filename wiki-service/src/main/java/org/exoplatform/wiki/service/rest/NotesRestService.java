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
import javax.ws.rs.core.*;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.TitleResolver;
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

import io.swagger.annotations.ApiParam;

/**
 * Notes REST service
 */
@SuppressWarnings("deprecation")
@Path("/notes")
public class NotesRestService implements ResourceContainer {

  private final WikiService           noteBookService;

  private final ResourceBundleService resourceBundleService;

  private static Log                  log = ExoLogger.getLogger("wiki:WikiRestService");

  private final CacheControl          cc;

  public NotesRestService(WikiService noteBookService, ResourceBundleService resourceBundleService) {
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
  public Response getNote(@Context UriInfo uriInfo,
                          @PathParam("noteBookType") String noteBookType,
                          @PathParam("noteBookOwner") String noteBookOwner,
                          @PathParam("noteId") String noteId) {
    try {
      Page note = noteBookService.getPageOfWikiByName(noteBookType, noteBookOwner, noteId);
      if (note == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      note.setContent(WikiHTMLSanitizer.markupSanitize(note.getContent()));
      return Response.ok(note).build();
    } catch (Exception e) {
      log.error("Can't get note", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }

  /**
   * Create new note
   *
   * @param note Note.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is
   *         performed successfully with status HTTPStatus.INTERNAL_ERROR if there
   *         is any unknown error in the saving process
   */
  @POST
  @Path("/note")
  @RolesAllowed("users")
  public Response createNote(@ApiParam(value = "task object to be updated", required = true) Page note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      /* TODO: check noteBook permissions */
      Wiki noteBook = noteBookService.getWikiByTypeAndOwner(note.getWikiType(), note.getWikiOwner());
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
      Page createdNote = noteBookService.createPage(noteBook, note.getParentPageName(), note);
      return Response.ok(createdNote, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    } catch (Exception ex) {
      log.warn(String.format("Failed to perform auto save noteBook note %s:%s:%s",
              note.getWikiType(),
              note.getWikiOwner(),
              note.getId()),
              ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Update Note
   *
   * @param note Note.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is
   *         performed successfully with status HTTPStatus.INTERNAL_ERROR if there
   *         is any unknown error in the saving process
   */
  @PUT
  @Path("/note/{noteBookType}/{noteBookOwner:.+}/{noteId}")
  @RolesAllowed("users")
  public Response updateNote(@ApiParam(value = "note object to be updated", required = true) @Context UriInfo uriInfo,
                             @PathParam("noteBookType") String noteBookType,
                             @PathParam("noteBookOwner") String noteBookOwner,
                             @PathParam("noteId") String noteId,
                             Page note) {
    if (note == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Page note_ = noteBookService.getPageOfWikiByName(noteBookType, noteBookOwner, noteId);
      if (note_ == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }

      if (!noteBookService.hasPermissionOnPage(note_, PermissionType.EDITPAGE, identity)) {
        return Response.status(Response.Status.FORBIDDEN).build();
      }

      if (!note_.getTitle().equals(note.getTitle())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
                && !note.getName().equals(newNoteName)) {
          noteBookService.renamePage(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        note_.setTitle(note.getTitle());
        noteBookService.updatePage(note_, PageUpdateType.EDIT_PAGE_TITLE);
        noteBookService.createVersionOfPage(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(noteBookType, noteBookOwner, newNoteName);
          noteBookService.removeDraftOfPage(noteParams);
        }
      } else if (!note_.getContent().equals(note.getContent())) {
        note_.setContent(note.getContent());
        noteBookService.updatePage(note_, PageUpdateType.EDIT_PAGE_CONTENT);
        noteBookService.createVersionOfPage(note_);
      } else if (!note_.getTitle().equals(note.getTitle()) && !note_.getContent().equals(note.getContent())) {
        String newNoteName = TitleResolver.getId(note.getTitle(), false);
        note_.setTitle(note.getTitle());
        note_.setContent(note.getContent());
        if (!org.exoplatform.wiki.utils.WikiConstants.WIKI_HOME_NAME.equals(note.getName())
                && !note.getName().equals(newNoteName)) {
          noteBookService.renamePage(noteBookType, noteBookOwner, note_.getName(), newNoteName, note.getTitle());
          note_.setName(newNoteName);
        }
        noteBookService.updatePage(note_, PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE);
        noteBookService.createVersionOfPage(note_);
        if (!"__anonim".equals(identity.getUserId())) {
          WikiPageParams noteParams = new WikiPageParams(noteBookType, noteBookOwner, newNoteName);
          noteBookService.removeDraftOfPage(noteParams);
        }
      }
      return Response.ok().build();
    } catch (Exception ex) {
      log.warn(String.format("Failed to perform update noteBook note %s:%s:%s",
              note.getWikiType(),
              note.getWikiOwner(),
              note.getId()),
              ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }

  /**
   * Display the current tree of a noteBook based on is path
   *
   * @param type It can be a Portal, Group, User type of noteBook
   * @param path Contains the path of the noteBook note
   * @param currentPath Contains the path of the current note
   * @param showExcerpt Boolean to display or not the excerpt
   * @param depth Defined the depth of the children we want to display
   * @return List of descendants including the note itself.
   */
  @GET
  @Path("/tree/{type}")
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  public Response getTreeData(@PathParam("type") String type,
                              @QueryParam(TreeNode.PATH) String path,
                              @QueryParam(TreeNode.CURRENT_PATH) String currentPath,
                              @QueryParam(TreeNode.CAN_EDIT) Boolean canEdit,
                              @QueryParam(TreeNode.SHOW_EXCERPT) Boolean showExcerpt,
                              @QueryParam(TreeNode.DEPTH) String depth) {
    try {
      List<JsonNodeData> responseData = new ArrayList<JsonNodeData>();
      HashMap<String, Object> context = new HashMap<String, Object>();
      context.put(TreeNode.CAN_EDIT, canEdit);
      if (currentPath != null) {
        currentPath = URLDecoder.decode(currentPath, "utf-8");
        context.put(TreeNode.CURRENT_PATH, currentPath);
        WikiPageParams currentNoteParam = TreeUtils.getPageParamsFromPath(currentPath);
        org.exoplatform.wiki.mow.api.Page currentNote = noteBookService.getPageOfWikiByName(currentNoteParam.getType(),
                currentNoteParam.getOwner(),
                currentNoteParam.getPageName());
        context.put(TreeNode.CURRENT_PAGE, currentNote);
      }

      EnvironmentContext env = EnvironmentContext.getCurrent();
      HttpServletRequest request = (HttpServletRequest) env.get(HttpServletRequest.class);

      // Put select note to context
      path = URLDecoder.decode(path, "utf-8");
      context.put(TreeNode.PATH, path);
      WikiPageParams noteParam = TreeUtils.getPageParamsFromPath(path);
      org.exoplatform.wiki.mow.api.Page note = noteBookService.getPageOfWikiByName(noteParam.getType(),
              noteParam.getOwner(),
              noteParam.getPageName());
      if (note == null) {
        log.warn("User [{}] can not get noteBook path [{}]. Wiki Home is used instead",
                ConversationState.getCurrent().getIdentity().getUserId(),
                path);
        note = noteBookService.getPageOfWikiByName(noteParam.getType(), noteParam.getOwner(), noteParam.WIKI_HOME);
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
