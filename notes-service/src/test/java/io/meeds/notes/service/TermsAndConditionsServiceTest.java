/**
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

import io.meeds.notes.model.TermsAndConditionPage;
import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static io.meeds.notes.service.TermsAndConditionsService.TC_METADATA_KEY;
import static io.meeds.notes.service.TermsAndConditionsService.TC_METADATA_OBJECT_TYPE;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { TermsAndConditionsService.class, })
class TermsAndConditionsServiceTest {

  @MockBean
  private NoteService               noteService;

  @MockBean
  private WikiService               wikiService;

  @MockBean
  private SettingService            settingService;

  @MockBean
  private ListenerService           listenerService;

  @MockBean
  private MetadataService           metadataService;

  @MockBean
  private UserACL                   userACL;

  @Autowired
  private TermsAndConditionsService termsAndConditionsService;

  @Test
  void testSaveTermsAndConditionsAdminUser() throws Exception {
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
    TermsAndConditionPage result = termsAndConditionsService.saveTermsAndConditions("content", "en", adminIdentity);

    assertNotNull(result);
  }

  @Test
  void testSaveTermsAndConditionsSimpleUser() throws WikiException {
    Identity identity = mock(Identity.class);

    // When
    when(identity.getUserId()).thenReturn("user");
    when(userACL.isAdministrator(identity)).thenReturn(false);

    // Then
    assertThrows(IllegalAccessException.class, () -> {
      termsAndConditionsService.saveTermsAndConditions("content", "en", identity);
    });

    verify(noteService, never()).createNote(any(), any(), any());
  }

  @Test
  void testIsTermsAcceptedForUserWhenAccepted() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    /*
     * page.setSettings(new HashMap<>() { { put("published", "true"); } });
     */
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
    assertTrue(termsAndConditionsService.isTermsAcceptedForUser("user", "en"));
  }

  @Test
  void testIsTermsAcceptedForUserWhenNotAccepted() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getProperties()).thenReturn(new HashMap<>() {
      {
        put("published", "true");
      }
    });
    MetadataObject metadataObject = new MetadataObject(TC_METADATA_OBJECT_TYPE, "1");
    when(metadataService.getMetadataItemsByMetadataAndObject(TC_METADATA_KEY,
                                                             metadataObject)).thenReturn(Collections.singletonList(metadataItem));

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
    assertFalse(termsAndConditionsService.isTermsAcceptedForUser("user", "en"));
  }

  @Test
  void testMarkTermsAsAcceptedForUser() throws WikiException {
    // Given
    Page page = new Page("termsAndConditions", "content");
    page.setId("1");
    page.setLatestVersionId("123");

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getProperties()).thenReturn(new HashMap<>() {
      {
        put("published", "true");
        put("latestVersionId", "123");
        put("publishedDate", "112322341");
      }
    });
    MetadataObject metadataObject = new MetadataObject(TC_METADATA_OBJECT_TYPE, "1");
    when(metadataService.getMetadataItemsByMetadataAndObject(TC_METADATA_KEY,
                                                             metadataObject)).thenReturn(Collections.singletonList(metadataItem));

    // When
    when(noteService.getNoteOfNoteBookByName(any(), any(), any())).thenReturn(page);

    termsAndConditionsService.markTermsAsAcceptedForUser("user", "en");

    // Then
    ArgumentCaptor<SettingValue> captor = ArgumentCaptor.forClass(SettingValue.class);
    verify(settingService).set(any(), any(), any(), captor.capture());

    assertEquals("123", captor.getValue().getValue());
  }

}
