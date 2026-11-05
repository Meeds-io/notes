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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.model.TermsAndConditionPage;
import io.meeds.notes.service.TermsAndConditionsService;

import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class TermsAndConditionsRestTest {

  private static final String       CONTENT = "<p>Terms</p>";

  @Mock
  private TermsAndConditionsService termsAndConditionsService;

  @InjectMocks
  private TermsAndConditionsRest    termsAndConditionsRest;

  @BeforeEach
  void setUp() {
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
  }

  @AfterEach
  void tearDown() {
    ConversationState.setCurrent(null);
  }

  @Test
  void getTermsAndConditionPageReturnsSanitizedContent() {
    assertEquals(CONTENT, getContent("en"));
  }

  @Test
  void getTermsAndConditionPageReturnsSanitizedContentWithoutLanguage() {
    assertEquals(CONTENT, getContent(null));
  }

  @Test
  void getTermsAndConditionPageReturnsEscapedContentWhenSanitizingFails() {
    try (MockedStatic<HTMLSanitizer> sanitizer = mockStatic(HTMLSanitizer.class)) {
      sanitizer.when(() -> HTMLSanitizer.sanitize(anyString())).thenThrow(new IllegalStateException("sanitizer failure"));

      assertEquals(HTMLEntityEncoder.getInstance().encodeHTML(CONTENT), getContent("en"));
    }
  }

  @Test
  void getTermsAndConditionPageAnswersBadRequestToAnUnreadableLang() {
    HttpServletRequest request = mock(HttpServletRequest.class);

    ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                      () -> termsAndConditionsRest.getTermsAndConditionPage(request, "x"));

    assertEquals(400, exception.getStatusCode().value());
    assertEquals(Utils.UNREADABLE_LANG_MESSAGE, exception.getReason());
    verifyNoInteractions(termsAndConditionsService);
  }

  @Test
  void getTermsAndConditionPageReadsTheLanguagesClientsSend() {
    for (String lang : List.of("pt-BR", "es_ES", "zh-TW", "")) {
      assertEquals(CONTENT, getContent(lang), lang);
    }
    HttpServletRequest request = mock(HttpServletRequest.class);
    for (String lang : List.of("es-es", " ")) {
      ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                                                        () -> termsAndConditionsRest.getTermsAndConditionPage(request, lang));
      assertEquals(400, exception.getStatusCode().value(), lang);
    }
  }

  @Test
  void getTermsAndConditionPageFailsWithoutCurrentUser() {
    ConversationState.setCurrent(null);
    TermsAndConditionPage page = new TermsAndConditionPage();
    page.setContent(CONTENT);
    when(termsAndConditionsService.getTermsAndConditions("en")).thenReturn(page);
    HttpServletRequest request = mock(HttpServletRequest.class);

    assertThrows(IllegalStateException.class, () -> termsAndConditionsRest.getTermsAndConditionPage(request, "en"));
    assertEquals(CONTENT, page.getContent());
  }

  private String getContent(String lang) {
    TermsAndConditionPage page = new TermsAndConditionPage();
    page.setContent(CONTENT);
    when(termsAndConditionsService.getTermsAndConditions(lang)).thenReturn(page);
    ResponseEntity<TermsAndConditionPage> response = termsAndConditionsRest.getTermsAndConditionPage(mock(HttpServletRequest.class),
                                                                                                   lang);
    assertEquals(200, response.getStatusCode().value());
    return response.getBody().getContent();
  }

}
