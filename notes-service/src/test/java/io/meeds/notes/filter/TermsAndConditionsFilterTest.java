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
package io.meeds.notes.filter;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

class TermsAndConditionsFilterTest {

  private TermsAndConditionsFilter  filter;

  private HttpServletRequest        httpRequest;

  private HttpServletResponse       httpResponse;

  private FilterChain               filterChain;

  private TermsAndConditionsService termsService;

  private UserPortalConfigService   portalConfigService;

  @BeforeEach
  void setUp() {
    filter = new TermsAndConditionsFilter();
    httpRequest = mock(HttpServletRequest.class);
    httpResponse = mock(HttpServletResponse.class);
    filterChain = mock(FilterChain.class);
    termsService = mock(TermsAndConditionsService.class);
    portalConfigService = mock(UserPortalConfigService.class);
    PortalContainer container = mock(PortalContainer.class);
    when(container.getComponentInstanceOfType(TermsAndConditionsService.class)).thenReturn(termsService);
    when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(portalConfigService);
    PortalContainer.setInstance(container);
  }

  @Test
  void testUserHasAcceptedTermsAndRequestIsForTermsPage() throws ServletException, IOException {
    when(httpRequest.getRemoteUser()).thenReturn("testUser");
    when(httpRequest.getRequestURI()).thenReturn("/portal/terms-and-conditions");
    when(httpRequest.getQueryString()).thenReturn(null);
    when(termsService.isTermsAcceptedForUser("testUser", null)).thenReturn(true);
    when(portalConfigService.getMetaPortal()).thenReturn("classic");

    filter.doFilter(httpRequest, httpResponse, filterChain);

    verify(httpResponse).sendRedirect("/portal/classic/settings#terms-and-conditions");
    verify(filterChain, never()).doFilter(httpRequest, httpResponse);
  }

  @Test
  void testUserHasNotAcceptedTerms() throws ServletException, IOException {
    when(httpRequest.getRemoteUser()).thenReturn("testUser");
    when(httpRequest.getRequestURI()).thenReturn("/portal/home");
    when(termsService.isTermsAcceptedForUser("testUser", null)).thenReturn(false);
    when(portalConfigService.getMetaPortal()).thenReturn("classic");

    filter.doFilter(httpRequest, httpResponse, filterChain);

    verify(httpResponse).sendRedirect("/portal/classic/terms-and-conditions");
    verify(filterChain, never()).doFilter(httpRequest, httpResponse);
  }

  @Test
  void testExcludedUrl() throws ServletException, IOException {
    when(httpRequest.getRemoteUser()).thenReturn("testUser");
    when(httpRequest.getRequestURI()).thenReturn("/portal/skins/theme.css");

    filter.doFilter(httpRequest, httpResponse, filterChain);

    verify(filterChain).doFilter(httpRequest, httpResponse);
    verify(httpResponse, never()).sendRedirect(anyString());
  }

  @Test
  void testAnonymousUser() throws ServletException, IOException {
    when(httpRequest.getRemoteUser()).thenReturn(null);

    filter.doFilter(httpRequest, httpResponse, filterChain);

    verify(filterChain).doFilter(httpRequest, httpResponse);
    verify(httpResponse, never()).sendRedirect(anyString());
  }
}
