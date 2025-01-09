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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.notes.service;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class TermsAndConditionsServiceTest extends BaseTest {

  private TermsAndConditionsService          service;

  @Mock
  private NoteService                        noteService;

  @Mock
  private WikiService                        wikiService;

  @Mock
  private SettingService                     settingService;

  @Mock
  private TermsAndConditionsWebSocketService webSocketService;

  @Mock
  private UserACL                            userACL;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    MockitoAnnotations.openMocks(this);
    service = new TermsAndConditionsService(noteService, wikiService, settingService, webSocketService, userACL);
  }

  public void testSaveTermsAndConditionsAdminUser() throws Exception {
    Identity adminIdentity = mock(Identity.class);

    // When
    when(adminIdentity.getUserId()).thenReturn("admin");
    when(userACL.isAdministrator(adminIdentity)).thenReturn(true);

    Page page = new Page("termsAndConditions", "content");
    page.setId("1");
    when(noteService.createNote(any(), any(), any())).thenReturn(new Page("termsAndConditions", "content"));
    when(noteService.getNoteOfNoteBookByName(any(), any(), any())).thenReturn(page);
    when(wikiService.getWikiByTypeAndOwner(any(), any())).thenReturn(new Wiki("termsAndConditions", "__system"));

    // Then
    Page result = service.saveTermsAndConditions("content", "en", adminIdentity);

    assertNotNull(result);
  }

  public void testSaveTermsAndConditionsSimpleUser() throws WikiException {
    Identity identity = mock(Identity.class);

    // When
    when(identity.getUserId()).thenReturn("user");
    when(userACL.isAdministrator(identity)).thenReturn(false);

    // Then
    assertThrows(IllegalAccessException.class, () -> {
      service.saveTermsAndConditions("content", "en", identity);
    });

    verify(noteService, never()).createNote(any(), any(), any());
  }

  public void testIsTermsAcceptedForUserWhenAccepted() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    page.setSettings(new HashMap<>() {
      {
        put("published", "true");
      }
    });
    page.setId("1");
    page.setLatestVersionId("123");

    // When
    when(noteService.getNoteOfNoteBookByName(any(), any(), any())).thenReturn(page);
    Map<String, String> settingValues = new HashMap<>();
    settingValues.put("userTERMS_AND_CONDITIONS", "123");

    when(settingService.get(any(Context.class), any(Scope.class), anyString())).thenAnswer(invocation -> {
      String key = invocation.getArgument(2, String.class);
      if ("TERMS_AND_CONDITIONS_ACCEPTED_VERSION".equals(key)) {
        return SettingValue.create("123");
      }
      return null;
    });

    // Then
    assertTrue(service.isTermsAcceptedForUser("user", "en"));
  }

  public void testIsTermsAcceptedForUserWhenNotAccepted() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    page.setSettings(new HashMap<>() {
      {
        put("published", "true");
      }
    });
    page.setId("1");
    page.setLatestVersionId("123");

    // When
    when(noteService.getNoteOfNoteBookByName(any(), any(), any())).thenReturn(page);

    when(settingService.get(any(Context.class), any(Scope.class), anyString())).thenAnswer(invocation -> {
      String key = invocation.getArgument(2, String.class);
      if ("TERMS_AND_CONDITIONS_ACCEPTED_VERSION".equals(key)) {
        return SettingValue.create("456");
      }
      return null;
    });

    // Then
    assertFalse(service.isTermsAcceptedForUser("user", "en"));
  }

  public void testMarkTermsAsAcceptedForUser() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    page.setId("1");
    page.setLatestVersionId("123");

    // When
    when(noteService.getNoteOfNoteBookByName(any(), any(), any())).thenReturn(page);

    service.markTermsAsAcceptedForUser("user", "en");

    // Then
    ArgumentCaptor<SettingValue> captor = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(any(), any(), any(), captor.capture());

    assertEquals("123", captor.getValue().getValue());
  }

}
