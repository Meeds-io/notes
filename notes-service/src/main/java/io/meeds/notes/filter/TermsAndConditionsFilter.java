/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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

package io.meeds.notes.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.web.filter.Filter;

import jakarta.servlet.http.HttpServletResponse;

public class TermsAndConditionsFilter implements Filter {

  public static final String TERMS_AND_CONDITIONS          = "/terms-and-conditions";

  public static final String TERMS_AND_CONDITIONS_SETTINGS = "/settings#terms-and-conditions";

  private final List<String> excludedUrls                  = new ArrayList<>(Arrays.asList("/portal/skins",
                                                                                           "/portal/scripts",
                                                                                           "/portal/javascript",
                                                                                           "/portal/rest",
                                                                                           "/portal/service-worker.js"));

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, // NOSONAR
                                                                                                          ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;
    PortalContainer container = PortalContainer.getInstance();
    TermsAndConditionsService termsAndConditionsService = container.getComponentInstanceOfType(TermsAndConditionsService.class);

    // Check if the request is for the terms and conditions page or if the user has
    // accepted terms
    String requestURI = httpRequest.getRequestURI();
    String remoteUser = httpRequest.getRemoteUser();
    boolean hasAcceptedTerms = remoteUser != null
        && termsAndConditionsService.isTermsAcceptedForUser(remoteUser, getLanguage(remoteUser));
    UserPortalConfigService portalConfigService =
                                                (UserPortalConfigService) PortalContainer.getComponent(UserPortalConfigService.class);
    if (hasAcceptedTerms && requestURI.contains(TERMS_AND_CONDITIONS) && httpRequest.getQueryString() == null) {
      httpResponse.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + TERMS_AND_CONDITIONS_SETTINGS);
      return;
    } else if (!hasAcceptedTerms && !requestURI.contains(TERMS_AND_CONDITIONS)
        && excludedUrls.stream().noneMatch(requestURI::startsWith)) {
      httpResponse.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + TERMS_AND_CONDITIONS);
      return;
    }
    chain.doFilter(servletRequest, servletResponse);
  }

  private String getLanguage(String username) {
    LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(username);
    LocalePolicy localePolicy = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LocalePolicy.class);
    String lang = null;
    if (localePolicy != null) {
      Locale locale = localePolicy.determineLocale(localeCtx);
      lang = locale.toString();
    }
    return lang;
  }
}
