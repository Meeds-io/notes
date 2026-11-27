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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.rest.api.RestUtils;

import io.meeds.notes.service.NotePageViewService;

@ExtendWith(MockitoExtension.class)
class NotePageViewSettingsRestTest {

  private static final String      NAME     = "Untitled-5-abc";

  private final Identity           identity = new Identity("editor");

  @Mock
  private NotePageViewService      notePageViewService;

  @InjectMocks
  private NotePageViewSettingsRest rest;

  @Test
  void saveHideAllowedForTheCurrentUser() throws Exception {
    try (MockedStatic<RestUtils> restUtils = mockStatic(RestUtils.class)) {
      restUtils.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);

      assertEquals(204, rest.saveHideAllowed(NAME, true).getStatusCode().value());
      verify(notePageViewService).saveHideAllowed(NAME, true, identity);
    }
  }

  @Test
  void saveHideAllowedMapsTheRefusalsOfTheService() throws Exception {
    try (MockedStatic<RestUtils> restUtils = mockStatic(RestUtils.class)) {
      restUtils.when(RestUtils::getCurrentUserAclIdentity).thenReturn(identity);
      doThrow(new IllegalAccessException()).when(notePageViewService).saveHideAllowed(NAME, true, identity);
      doThrow(new ObjectNotFoundException("")).when(notePageViewService).saveHideAllowed(NAME, false, identity);

      assertEquals(403, assertThrows(ResponseStatusException.class, () -> rest.saveHideAllowed(NAME, true)).getStatusCode().value());
      assertEquals(404, assertThrows(ResponseStatusException.class, () -> rest.saveHideAllowed(NAME, false)).getStatusCode().value());
    }
  }

}
