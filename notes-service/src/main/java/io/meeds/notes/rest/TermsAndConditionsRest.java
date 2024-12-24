/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package io.meeds.notes.rest;

import java.util.Date;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import com.google.javascript.jscomp.jarjar.com.google.common.base.Objects;

import io.meeds.notes.service.TermsAndConditionsService;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.rest.resource.ResourceContainer;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.wiki.model.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@Path("/notes/terms/")
@Tag(name = "/notes/terms/", description = "Managing terms and conditions")
public class TermsAndConditionsRest implements ResourceContainer {

  private static final CacheControl CACHE_CONTROL    = new CacheControl();

  private static final int          CACHE_IN_SECONDS = 365 * 86400;

  private static final Log          LOG              = ExoLogger.getLogger(TermsAndConditionsRest.class);

  static {
    CACHE_CONTROL.setMaxAge(CACHE_IN_SECONDS);
    CACHE_CONTROL.setMustRevalidate(true);
  }

  private final TermsAndConditionsService termsAndConditionsService;

  public TermsAndConditionsRest(TermsAndConditionsService termsAndConditionsService) {
    this.termsAndConditionsService = termsAndConditionsService;
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Retrieves the terms and conditions page", description = "Retrieves the terms and conditions page", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "304", description = "Not modified"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public Response getNotePage(@Context
                              Request request,
                              @Parameter(description = "User language")
                              @QueryParam("lang")
                              String lang) {
    try {
      Page note = termsAndConditionsService.getTermsAndConditions(lang);
      if (note == null) {
        return Response.status(Status.NOT_FOUND).build();
      }
      note.setContent(HTMLSanitizer.sanitize(note.getContent()));
      Date updatedDate = note.getUpdatedDate();
      EntityTag eTag = new EntityTag(String.valueOf(Objects.hashCode(lang, String.valueOf(updatedDate.getTime()))));
      Response.ResponseBuilder builder = request.evaluatePreconditions(eTag);
      if (builder == null) {
        builder = Response.ok(note);
      }
      builder.lastModified(updatedDate);
      builder.tag(eTag);
      builder.cacheControl(CACHE_CONTROL);
      return builder.build();
    } catch (Exception e) {
      LOG.warn("Error retrieving terms and conditions page content for user {}", RestUtils.getCurrentUser(), e);
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  @PUT
  @RolesAllowed("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Saves a terms and conditions page content",
             description = "Saves a terms and conditions page content",
             method = "PUT")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized") })
  public Response saveNotePage(@Context
                               Request request,
                               @Parameter(description = "Note Content", required = true)
                               @FormParam("content")
                               String content,
                               @Parameter(description = "User language")
                               @FormParam("lang")
                               String lang) {
    Page page;
    try {
      page = termsAndConditionsService.saveTermsAndConditions(content, lang, RestUtils.getCurrentUserAclIdentity());
    } catch (IllegalAccessException e) {
      LOG.warn("User '{}' doesn't have enough privileges to create terms and conditions page", RestUtils.getCurrentUser(), e);
      return Response.status(Status.UNAUTHORIZED).build();
    }
    return Response.ok(page).build();
  }

}
