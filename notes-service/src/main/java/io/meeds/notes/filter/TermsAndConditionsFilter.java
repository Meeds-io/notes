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

package io.meeds.notes.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.LocalePolicy;
import org.exoplatform.web.filter.Filter;

public class TermsAndConditionsFilter implements Filter {

  private static final String TERMS_AND_CONDITIONS_PAGE          = "/terms-and-conditions";

  private static final String TERMS_AND_CONDITIONS_SETTINGS_PAGE = "/settings#terms-and-conditions";

  private final Set<String>   excludedUrls                       = new HashSet<>(Arrays.asList("/portal/skins",
                                                                                               "/portal/scripts",
                                                                                               "/portal/javascript",
                                                                                               "/portal/rest",
                                                                                               "/portal/service-worker.js"));

  @Override
  public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException,
                                                                                                          ServletException {

    HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
    HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

    String requestURI = httpRequest.getRequestURI();
    String remoteUser = httpRequest.getRemoteUser();

    if (remoteUser == null || isExcludedUrl(requestURI)) {
      chain.doFilter(servletRequest, servletResponse);
      return;
    }

    PortalContainer container = PortalContainer.getInstance();
    TermsAndConditionsService termsService = container.getComponentInstanceOfType(TermsAndConditionsService.class);
    UserPortalConfigService portalConfigService = container.getComponentInstanceOfType(UserPortalConfigService.class);

    if (termsService == null || portalConfigService == null) {
      chain.doFilter(servletRequest, servletResponse);
      return;
    }

    boolean hasAcceptedTerms = termsService.isTermsAcceptedForUser(remoteUser, getLanguage(remoteUser));

    if (hasAcceptedTerms && requestURI.contains(TERMS_AND_CONDITIONS_PAGE) && httpRequest.getQueryString() == null) {
      redirect(httpResponse, portalConfigService, TERMS_AND_CONDITIONS_SETTINGS_PAGE);
      return;
    }

    if (!hasAcceptedTerms && !isTermsPage(requestURI)) {
      redirect(httpResponse, portalConfigService, TERMS_AND_CONDITIONS_PAGE);
      return;
    }

    chain.doFilter(servletRequest, servletResponse);
  }

  private boolean isExcludedUrl(String requestURI) {
    return excludedUrls.stream().anyMatch(requestURI::startsWith);
  }

  private boolean isTermsPage(String requestURI) {
    return requestURI.contains(TERMS_AND_CONDITIONS_PAGE);
  }

  private void redirect(HttpServletResponse response,
                        UserPortalConfigService portalConfigService,
                        String path) throws IOException {
    response.sendRedirect("/portal/" + portalConfigService.getMetaPortal() + path);
  }

  private String getLanguage(String username) {
    LocaleContextInfo localeCtx = LocaleContextInfoUtils.buildLocaleContextInfo(username);
    LocalePolicy localePolicy = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(LocalePolicy.class);
    if (localePolicy != null) {
      Locale locale = localePolicy.determineLocale(localeCtx);
      return locale.toString();
    }
    return null;
  }
}
