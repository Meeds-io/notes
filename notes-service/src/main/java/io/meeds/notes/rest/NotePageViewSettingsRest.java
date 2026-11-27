/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.notes.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.notes.service.NotePageViewService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("note-page-view")
@Tag(name = "note-page-view", description = "Managing the settings of the blocks of content")
public class NotePageViewSettingsRest {

  @Autowired
  private NotePageViewService notePageViewService;

  @PutMapping("{name}/allow-hide")
  @Secured("users")
  @Operation(summary = "Saves whether each user may hide a block of content from its page", method = "PUT",
             description = "Saves whether each user may hide a block of content from its page. Turned off, the block shows again to every user who hid it.")
  @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Request fulfilled"),
    @ApiResponse(responseCode = "403", description = "The user can't edit the block"),
    @ApiResponse(responseCode = "404", description = "The block doesn't exist"), })
  public ResponseEntity<Void> saveHideAllowed(
                                              @Parameter(description = "Block setting name", required = true)
                                              @PathVariable("name")
                                              String name,
                                              @Parameter(description = "Whether the block allows hiding", required = true)
                                              @RequestParam("allowed")
                                              boolean allowed) {
    try {
      notePageViewService.saveHideAllowed(name, allowed, RestUtils.getCurrentUserAclIdentity());
      return ResponseEntity.noContent().build();
    } catch (IllegalAccessException e) {
      throw new ResponseStatusException(HttpStatus.FORBIDDEN, "The block of content can't be edited");
    } catch (ObjectNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "The block of content doesn't exist");
    }
  }

}
