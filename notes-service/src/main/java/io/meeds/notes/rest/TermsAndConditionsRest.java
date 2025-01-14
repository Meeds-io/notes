/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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


import javax.ws.rs.*;
import javax.ws.rs.core.*;

import io.meeds.notes.rest.model.TermsAndConditionsSettings;
import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.http.HttpServletRequest;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.rest.api.RestUtils;
import org.exoplatform.wiki.model.Page;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("terms")
@Tag(name = "terms", description = "Managing terms and conditions") // NOSONAR
public class TermsAndConditionsRest {

  private static final Log          LOG = ExoLogger.getLogger(TermsAndConditionsRest.class);

  @Autowired
  private TermsAndConditionsService termsAndConditionsService;

  @GetMapping
  @Secured("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Retrieves the terms and conditions page", description = "Retrieves the terms and conditions page", method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "304", description = "Not modified"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public ResponseEntity<Page> getNotePage(HttpServletRequest request,
                                          @Parameter(description = "User language")
                                          @RequestParam("lang")
                                          String lang) {
    try {
      Page note = termsAndConditionsService.getTermsAndConditions(lang);
      if (note == null) {
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
      }
      note.setContent(HTMLSanitizer.sanitize(note.getContent()));
      String eTagValue = String.valueOf(note.hashCode());
      String requestETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
      if (requestETag != null && requestETag.equals(eTagValue)) {
        return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
      }
      return ResponseEntity.ok().eTag(eTagValue).body(note);
    } catch (Exception e) {
      LOG.warn("Error retrieving terms and conditions page content for user {}", RestUtils.getCurrentUser(), e);
      throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
    }
  }

  @PutMapping
  @Secured("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Saves a terms and conditions page content",
             description = "Saves a terms and conditions page content",
             method = "PUT")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized") })
  public Page saveNotePage(@Parameter(description = "Note Content", required = true)
                           @RequestParam("content")
                           String content,
                           @Parameter(description = "User language")
                           @RequestParam("lang")
                           String lang) {
    try {
      return termsAndConditionsService.saveTermsAndConditions(content, lang, RestUtils.getCurrentUserAclIdentity());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    }
  }

  @PutMapping(path = "/settings")
  @Secured("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Consumes(MediaType.APPLICATION_JSON)
  @Operation(summary = "Update terms and conditions settings",
             description = "Updates the settings for the terms and conditions page",
             method = "PUT")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "400", description = "Bad Request")
  })
  public Page updateTermsAndConditionsSettings(@RequestBody TermsAndConditionsSettings termsAndConditionsSettings) {
    try {
      return termsAndConditionsService.updateTermsAndConditionsSettings(termsAndConditionsSettings.getSettings(),
                                                                        termsAndConditionsSettings.getLang(),
                                                                        RestUtils.getCurrentUserAclIdentity());
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
    } catch (Exception e) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
    }
  }

  @PostMapping(path = "/accept")
  @Secured("users")
  @Produces(MediaType.APPLICATION_JSON)
  @Operation(summary = "Accept terms and conditions",
             description = "Marks the terms and conditions as accepted for the current user",
             method = "POST")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Terms accepted successfully"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  public void acceptTermsAndConditions(HttpServletRequest request,
                                       @Parameter(description = "User language")
                                       @RequestParam("lang")
                                       String lang) {
    termsAndConditionsService.markTermsAsAcceptedForUser(request.getRemoteUser(), lang);
  }

  @GetMapping(path = "/status")
  @Secured("users")
  @Produces(MediaType.TEXT_PLAIN)
  @Operation(summary = "Check terms and conditions status for the current user",
          description = "Check the terms and conditions status for the current user",
          method = "GET")
  @ApiResponses(value = {
          @ApiResponse(responseCode = "200", description = "Request fulfilled"),
          @ApiResponse(responseCode = "401", description = "Unauthorized"),
          @ApiResponse(responseCode = "500", description = "Internal Server Error")
  })
  public boolean isTermsAcceptedForUser(HttpServletRequest request,
                                        @Parameter(description = "User language")
                                        @RequestParam("lang")
                                        String lang) {
    return termsAndConditionsService.isTermsAcceptedForUser(request.getRemoteUser(), lang);
  }
}
