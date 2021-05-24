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

import io.swagger.annotations.ApiParam;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.fileupload.DiskFileUpload;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadBase;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.StringUtils;
import org.exoplatform.common.http.HTTPStatus;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.commons.utils.MimeTypeResolver;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.rest.api.EntityBuilder;
import org.exoplatform.social.rest.entity.IdentityEntity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.resolver.TitleResolver;
import org.exoplatform.wiki.service.Relations;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.image.ResizeImageService;
import org.exoplatform.wiki.service.impl.BeanToJsons;
import org.exoplatform.wiki.service.impl.DraftData;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.related.JsonRelatedData;
import org.exoplatform.wiki.service.related.RelatedUtil;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TitleSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.tree.JsonNodeData;
import org.exoplatform.wiki.tree.TreeNode;
import org.exoplatform.wiki.tree.TreeNode.TREETYPE;
import org.exoplatform.wiki.tree.WikiTreeNode;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.WikiConstants;
import org.exoplatform.wiki.utils.WikiHTMLSanitizer;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.Status;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.util.*;
import java.lang.Object;
import java.lang.Class;

/**
 * Notes REST service
 */
@SuppressWarnings("deprecation")
@Path("/notes")
public class NotesRestService implements ResourceContainer {

  private final WikiService      wikiService;

  private final ResourceBundleService resourceBundleService;

  private static Log             log = ExoLogger.getLogger("wiki:WikiRestService");

  private final CacheControl     cc;


  public NotesRestService(WikiService wikiService, ResourceBundleService resourceBundleService) {
    this.wikiService = wikiService;
    this.resourceBundleService = resourceBundleService;
    cc = new CacheControl();
    cc.setNoCache(true);
    cc.setNoStore(true);
  }

  /**
   * Return the note page content as html or wiki syntax.
   * @param text contain the data as html
   * @return the instance of javax.ws.rs.core.Response
   *
   * @LevelAPI Experimental
   */
  @POST
  @Path("/content/")
  public Response getNotePageContent(@FormParam("text") String text) {
    try {
      String outputText = WikiHTMLSanitizer.markupSanitize(text);

      return Response.ok(outputText, MediaType.TEXT_HTML).cacheControl(cc).build();
    } catch (Exception e) {
      log.error("Error while converting wiki page content: " + e.getMessage(), e);
      return Response.serverError().entity(e.getMessage()).cacheControl(cc).build();
    }
  }

  @GET
  @Path("/{wikiType}/{wikiOwner:.+}/pages/{pageId}")
  @Produces(MediaType.APPLICATION_JSON)
  @RolesAllowed("users")
  public Response getPage(@Context UriInfo uriInfo,
                                                              @PathParam("wikiType") String wikiType,
                                                              @PathParam("wikiOwner") String wikiOwner,
                                                              @PathParam("pageId") String pageId) {
    try {
      Page page = wikiService.getPageOfWikiByName(wikiType, wikiOwner, pageId);
      if (page == null) {
        return Response.status(Response.Status.NOT_FOUND).build();
      }
      return Response.ok(page).build();
    } catch (Exception e) {
      log.error("Can't get page", e);
      return Response.serverError().entity(e.getMessage()).build();
    }
  }


  /**
   * Save draft title and content for a page specified by the given page params
   * 

   * @param page WIKI PAGE.
   * @return {@link Response} with status HTTPStatus.ACCEPTED if saving process is performed successfully
   *                          with status HTTPStatus.INTERNAL_ERROR if there is any unknown error in the saving process
   */                          
  @POST
  @Path("/page")
  @RolesAllowed("users")
  public Response savePage(@ApiParam(value = "task object to be updated", required = true) Page page) {
    if (page == null) {
      return Response.status(Response.Status.BAD_REQUEST).build();
    }
    try {
      Identity identity = ConversationState.getCurrent().getIdentity();
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(),page.getWikiOwner());
      if(wiki == null) {
        return Response.status(Response.Status.BAD_REQUEST).build();
      }
      String syntaxId = wikiService.getDefaultWikiSyntaxId();
      String currentUser = identity.getUserId();
      page.setAuthor(currentUser);
      page.setOwner(currentUser);
      page.setSyntax(syntaxId);
      page.setName(TitleResolver.getId(page.getTitle(), false));
      page.setUrl("");
      Page createdPage = wikiService.createPage(wiki, page.getParentPageName(), page);
      return Response.ok(createdPage, MediaType.APPLICATION_JSON).cacheControl(cc).build();
    }
    catch (Exception ex) {
      log.warn(String.format("Failed to perform auto save wiki page %s:%s:%s", page.getWikiType(),page.getWikiOwner(),page.getId()), ex);
      return Response.status(HTTPStatus.INTERNAL_ERROR).cacheControl(cc).build();
    }
  }
}
