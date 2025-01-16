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
package io.meeds.notes.handler;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = { TermsAndConditionsHandler.class, })
class TermsAndConditionsHandlerTest {

  @Mock
  private WebAppController          webAppController;

  @Mock
  private ServletConfig             servletConfig;

  @MockBean
  ControllerContext                 controllerContext;

  @MockBean
  private HttpServletRequest        httpRequest;

  @MockBean
  private HttpServletResponse       httpResponse;

  @MockBean
  private TermsAndConditionsService termsService;

  @MockBean
  private UserPortalConfigService   portalConfigService;

  @MockBean
  private PortalContainer           container;

  @Autowired
  private TermsAndConditionsHandler handler;

  @Test
  void testUserHasAcceptedTermsAndRequestIsForTermsPage() throws Exception {
    when(container.getComponentInstanceOfType(TermsAndConditionsService.class)).thenReturn(termsService);
    when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(portalConfigService);
    when(controllerContext.getRequest()).thenReturn(httpRequest);
    when(controllerContext.getResponse()).thenReturn(httpResponse);
    when(httpRequest.getContextPath()).thenReturn("/portal");
    when(httpRequest.getRemoteUser()).thenReturn("testUser");
    when(httpRequest.getRequestURI()).thenReturn("/portal/terms-and-conditions");
    when(httpRequest.getQueryString()).thenReturn(null);
    when(httpRequest.getLocale()).thenReturn(Locale.ENGLISH);
    when(termsService.isTermsAcceptedForUser("testUser", "en")).thenReturn(true);
    when(portalConfigService.getMetaPortal()).thenReturn("classic");

    try (MockedStatic<PortalContainer> mockedStatic = mockStatic(PortalContainer.class)) {
      mockedStatic.when(PortalContainer::getInstance).thenReturn(container);
      handler.onInit(webAppController, servletConfig);
      boolean executed = handler.execute(controllerContext);

      verify(httpResponse).sendRedirect("/portal/classic/settings#terms-and-conditions");
      assertTrue(executed);
    }
  }

  @Test
  void testUserHasNotAcceptedTerms() throws Exception {
    when(container.getComponentInstanceOfType(TermsAndConditionsService.class)).thenReturn(termsService);
    when(container.getComponentInstanceOfType(UserPortalConfigService.class)).thenReturn(portalConfigService);
    when(controllerContext.getRequest()).thenReturn(httpRequest);
    when(controllerContext.getResponse()).thenReturn(httpResponse);
    when(httpRequest.getContextPath()).thenReturn("/portal");
    when(httpRequest.getRemoteUser()).thenReturn("testUser");
    when(httpRequest.getRequestURI()).thenReturn("/portal/home");
    when(httpRequest.getLocale()).thenReturn(Locale.ENGLISH);
    when(termsService.isTermsAcceptedForUser("testUser", null)).thenReturn(false);
    when(portalConfigService.getMetaPortal()).thenReturn("classic");

    try (MockedStatic<PortalContainer> mockedStatic = mockStatic(PortalContainer.class)) {
      mockedStatic.when(PortalContainer::getInstance).thenReturn(container);
      handler.onInit(webAppController, servletConfig);
      boolean executed = handler.execute(controllerContext);

      String encodedPreviousPage = URLEncoder.encode("/portal/home", StandardCharsets.UTF_8);
      verify(httpResponse).sendRedirect("/portal/classic/terms-and-conditions?redirect=" + encodedPreviousPage);
      assertTrue(executed);
    }
  }

  @Test
  void testAnonymousUser() throws Exception {
    when(controllerContext.getRequest()).thenReturn(httpRequest);
    when(controllerContext.getResponse()).thenReturn(httpResponse);
    when(httpRequest.getRemoteUser()).thenReturn(null);

    handler.onInit(webAppController, servletConfig);
    boolean executed = handler.execute(controllerContext);

    verify(httpResponse, never()).sendRedirect(anyString());
    assertFalse(executed);
  }
}
