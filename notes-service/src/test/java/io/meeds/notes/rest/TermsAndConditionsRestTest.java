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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;

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
    assertEquals(HTMLEntityEncoder.getInstance().encodeHTML(CONTENT), getContent("x"));
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
