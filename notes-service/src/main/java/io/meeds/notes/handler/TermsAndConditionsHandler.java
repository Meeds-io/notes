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
import java.util.ArrayList;
import java.util.List;

import io.meeds.notes.service.TermsAndConditionsService;
import jakarta.annotation.PostConstruct;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.web.ControllerContext;
import org.exoplatform.web.WebAppController;
import org.exoplatform.web.WebRequestHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TermsAndConditionsHandler extends WebRequestHandler {

  public static final String              PAGE_URI     = "terms-and-conditions";

  private final UserPortalConfigService   userPortalConfigService;

  private final TermsAndConditionsService termsAndConditionsService;

  private final WebAppController          webAppController;

  @Value("#{'${io.meeds.termsAndConditions.excludedUris:}'.split(',')}")
  private List<String>                    excludedUris = new ArrayList<>();

  @Autowired
  public TermsAndConditionsHandler(UserPortalConfigService userPortalConfigService,
                                   TermsAndConditionsService termsAndConditionsService,
                                   WebAppController webAppController) {
    this.userPortalConfigService = userPortalConfigService;
    this.termsAndConditionsService = termsAndConditionsService;
    this.webAppController = webAppController;
  }

  @PostConstruct
  public void init() {
    webAppController.register(this);
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

    if (username == null || isExcludedUri(requestURI)) {
      return false;
    }

    String language = controllerContext.getRequest().getLocale().getLanguage();
    boolean hasAcceptedTerms = termsAndConditionsService.isTermsAcceptedForUser(username, language);

    if (hasAcceptedTerms && isTermsPage(requestURI) && controllerContext.getRequest().getQueryString() == null) {
      redirectToUserSettings(controllerContext);
      return true;
    }

    if (!hasAcceptedTerms && !isTermsPage(requestURI)) {
      redirectToTermsPage(controllerContext, requestURI, username);
      return true;
    }

    return false;
  }

  private void redirectToUserSettings(ControllerContext ctx) throws Exception {
    String target = String.format("%s/%s/settings#terms-and-conditions",
                                  ctx.getRequest().getContextPath(),
                                  userPortalConfigService.getMetaPortal());
    ctx.getResponse().sendRedirect(target);
  }

  private void redirectToTermsPage(ControllerContext ctx, String requestURI, String username) throws Exception {
    String queryString = ctx.getRequest().getQueryString();
    if (queryString != null) {
      requestURI += "?" + queryString;
    }

    String basePortalPath = "/" + PortalContainer.getCurrentPortalContainerName() + "/";
    String contextPath = ctx.getRequest().getContextPath();
    String metaPortal = userPortalConfigService.getMetaPortal();

    String redirectUrl = requestURI.equals(basePortalPath)
                                                           ? String.format("%s/%s/settings#terms-and-conditions?redirect=%s",
                                                                           contextPath,
                                                                           metaPortal,
                                                                           userPortalConfigService.getDefaultPath(username))
                                                           : String.format("%s/%s/terms-and-conditions?redirect=%s",
                                                                           contextPath,
                                                                           metaPortal,
                                                                           URLEncoder.encode(requestURI, StandardCharsets.UTF_8));

    ctx.getResponse().sendRedirect(redirectUrl);
  }

  private boolean isTermsPage(String uri) {
    return uri.contains(PAGE_URI);
  }

  private boolean isExcludedUri(String uri) {
    return excludedUris.stream().anyMatch(uri::contains);
  }
}
