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
package io.meeds.notes.service;

import static io.meeds.notes.service.NotePageViewService.CMS_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;

import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.service.HiddenApplicationService;

/**
 * The option letting each user hide a block of content, which social's hidden
 * applications keep. The service resolves social's Spring service at first
 * use, stubbed here on a spy.
 */
@ExtendWith(MockitoExtension.class)
class NotePageViewServiceHideTest {

  private static final String      NAME = "Untitled-5-abc";

  @Mock
  private CMSService               cmsService;

  @Mock
  private HiddenApplicationService hiddenApplicationService;

  private NotePageViewService      notePageViewService;

  private Identity                 identity;

  @BeforeEach
  void setUp() {
    notePageViewService = spy(new NotePageViewService(null, null, cmsService, null, null, null, null));
    lenient().doReturn(hiddenApplicationService).when(notePageViewService).getHiddenApplicationService();
    identity = new Identity("editor");
  }

  @Test
  void saveHideAllowedStoresTheOptionForAnEditor() throws Exception {
    when(cmsService.isSettingNameExists(CMS_CONTENT_TYPE, NAME)).thenReturn(true);
    when(cmsService.hasEditPermission(identity, CMS_CONTENT_TYPE, NAME)).thenReturn(true);

    notePageViewService.saveHideAllowed(NAME, true, identity);

    verify(hiddenApplicationService).setHideAllowed(CMS_CONTENT_TYPE, NAME, true);
  }

  @Test
  void saveHideAllowedByANonEditorIsRefused() {
    when(cmsService.isSettingNameExists(CMS_CONTENT_TYPE, NAME)).thenReturn(true);

    assertThrows(IllegalAccessException.class, () -> notePageViewService.saveHideAllowed(NAME, false, identity));
    verify(hiddenApplicationService, never()).setHideAllowed(anyString(), anyString(), anyBoolean());
  }

  @Test
  void saveHideAllowedOfAnUnknownBlockIsNotFound() {
    assertThrows(ObjectNotFoundException.class, () -> notePageViewService.saveHideAllowed(NAME, true, identity));
    verify(hiddenApplicationService, never()).setHideAllowed(anyString(), anyString(), anyBoolean());
  }

  @Test
  void isHiddenNeedsTheOptionOn() {
    when(hiddenApplicationService.isHidden(CMS_CONTENT_TYPE, NAME, "user")).thenReturn(true);
    assertFalse(notePageViewService.isHidden(NAME, "user"));

    when(hiddenApplicationService.isHideAllowed(CMS_CONTENT_TYPE, NAME)).thenReturn(true);
    assertTrue(notePageViewService.isHideAllowed(NAME));
    assertTrue(notePageViewService.isHidden(NAME, "user"));
    assertFalse(notePageViewService.isHidden(NAME, "other"));
  }

}
