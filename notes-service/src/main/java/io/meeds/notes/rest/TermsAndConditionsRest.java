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

import io.meeds.notes.model.TermsAndConditionPage;
import io.meeds.notes.service.TermsAndConditionsService;
import io.meeds.social.html.model.HtmlTransformerContext;
import io.meeds.social.html.model.HtmlUtils;

import jakarta.servlet.http.HttpServletRequest;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.rest.api.RestUtils;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.apache.commons.lang3.LocaleUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

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
          @ApiResponse(responseCode = "404", description = "Resource not found"), })
  public ResponseEntity<TermsAndConditionPage> getTermsAndConditionPage(HttpServletRequest request,
                                                                        @Parameter(description = "User language")
                                                                        @RequestParam("lang")
                                                                        String lang) {
    TermsAndConditionPage termsAndConditionPage = termsAndConditionsService.getTermsAndConditions(lang);
    if (termsAndConditionPage == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }
    termsAndConditionPage.setContent(transformContent(termsAndConditionPage.getContent(), lang));
    String eTagValue = String.valueOf(termsAndConditionPage.hashCode());
    String requestETag = request.getHeader(HttpHeaders.IF_NONE_MATCH);
    if (requestETag != null && requestETag.equals(eTagValue)) {
      return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
    }
    return ResponseEntity.ok().eTag(eTagValue).body(termsAndConditionPage);
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
  public TermsAndConditionPage saveTermsAndConditions(@Parameter(description = "Note Content", required = true)
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
          @ApiResponse(responseCode = "400", description = "Bad Request")
  })
  public TermsAndConditionPage updateTermsAndConditionsSettings(@Parameter(description = "Whether the terms and conditions update is a publish or not")
                                                                @RequestParam(name = "published")
                                                                Optional<Boolean> published,
                                                                @Parameter(description = "User language")
                                                                @RequestParam("lang")
                                                                String lang) {
    try {
      return termsAndConditionsService.updateTermsAndConditionsSettings(published.orElse(true),
                                                                        lang,
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
          @ApiResponse(responseCode = "401", description = "Unauthorized")
  })
  public boolean isTermsAcceptedForUser(HttpServletRequest request,
                                        @Parameter(description = "User language")
                                        @RequestParam("lang")
                                        String lang) {
    return termsAndConditionsService.isTermsAcceptedForUser(request.getRemoteUser(), lang);
  }

  private String transformContent(String content, String lang) {
    try {
      content = HtmlUtils.transform(content,
                                    new HtmlTransformerContext(ConversationState.getCurrent().getIdentity(),
                                                               LocaleUtils.toLocale(lang)));
      return HTMLSanitizer.sanitize(content);
    } catch (Exception e) {
      LOG.warn("Error sanitizing terms and conditions content", e);
      return content;
    }
  }
}
