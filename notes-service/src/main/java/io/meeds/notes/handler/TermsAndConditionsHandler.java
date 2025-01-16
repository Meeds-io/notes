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

package io.meeds.notes.handler;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.*;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;

public class TermsAndConditionsHandler extends WebRequestHandler {

  public static final String        PAGE_URI = "terms-and-conditions";

  private UserPortalConfigService   userPortalConfigService;

  private TermsAndConditionsService termsAndConditionsService;

  @Override
  public void onInit(WebAppController controller, ServletConfig sConfig) throws Exception {
    super.onInit(controller, sConfig);

    PortalContainer container = PortalContainer.getInstance();
    this.userPortalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);
    this.termsAndConditionsService = container.getComponentInstanceOfType(TermsAndConditionsService.class);
  }

  @Override
  public String getHandlerName() {
    return PAGE_URI;
  }

  @Override
  protected boolean getRequiresLifeCycle() {
    return true;
  }

  @Override
  public boolean execute(ControllerContext controllerContext) throws Exception {
    String username = controllerContext.getRequest().getRemoteUser();
    String requestURI = controllerContext.getRequest().getRequestURI();
    if (username == null || termsAndConditionsService == null || userPortalConfigService == null) {
      return false;
    }
    String language = controllerContext.getRequest().getLocale().getLanguage();
    boolean hasAcceptedTerms = termsAndConditionsService.isTermsAcceptedForUser(username, language);

    if (hasAcceptedTerms && requestURI.contains(PAGE_URI) && controllerContext.getRequest().getQueryString() == null) {
      controllerContext.getResponse()
                       .sendRedirect(String.format("%s/%s/settings#terms-and-conditions",
                                                   controllerContext.getRequest().getContextPath(),
                                                   userPortalConfigService.getMetaPortal()));
      return true;
    }
    if (!hasAcceptedTerms && !isTermsPage(requestURI)) {
      String queryString = controllerContext.getRequest().getQueryString();

      if (queryString != null) {
        requestURI += "?" + queryString;
      }
      String initURI = "/" + PortalContainer.getCurrentPortalContainerName() + "/";

      if (initURI.equals(requestURI)) {
        controllerContext.getResponse()
                         .sendRedirect(String.format("%s/%s/settings#terms-and-conditions?redirect=%s",
                                                     controllerContext.getRequest().getContextPath(),
                                                     userPortalConfigService.getMetaPortal(),
                                                     userPortalConfigService.getDefaultPath(username)));
      } else {
        String encodedPreviousPage = URLEncoder.encode(requestURI, StandardCharsets.UTF_8);
        controllerContext.getResponse()
                         .sendRedirect(String.format("%s/%s/terms-and-conditions?redirect=%s",
                                                     controllerContext.getRequest().getContextPath(),
                                                     userPortalConfigService.getMetaPortal(),
                                                     encodedPreviousPage));
      }
      return true;
    }
    return false;
  }

  private boolean isTermsPage(String requestURI) {
    return requestURI.contains(PAGE_URI);
  }
}
