/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.web.filter.Filter;
import org.exoplatform.wiki.utils.Utils;

/**
 * Legacy redirect keeping pre-Notes wiki URLs working: rewrites the "wiki"
 * navigation-node segment (and old "WikiPortlet" URLs) to "notes". The
 * rewrite only applies when the addressed site has no genuine "wiki"
 * navigation node: a page named "wiki" created today (e.g. in a space) must
 * render itself, not be redirected to a "notes" node that may not exist in
 * that site.
 */
public class WikiToNotesRedirectFilter implements Filter {

  private static final String LEGACY_WIKI_SEGMENT         = "wiki";

  private static final String LEGACY_WIKI_PORTLET_SEGMENT = "WikiPortlet";

  private static final String NOTES_SEGMENT               = "notes";

  private static final String GROUP_SITE_URI_MARKER       = "g";

  private static final String SPACE_SITE_URI_MARKER       = "s";

  public WikiToNotesRedirectFilter() {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    String reqUri = httpServletRequest.getRequestURI();
    boolean isRestUri = reqUri.contains(ExoContainerContext.getCurrentContainer().getContext().getRestContextName());
    if (!isRestUri) {
      List<String> segments = new ArrayList<>(Arrays.asList(reqUri.split("/")));
      int wikiIndex = segments.indexOf(LEGACY_WIKI_SEGMENT);
      int wikiPortletIndex = segments.indexOf(LEGACY_WIKI_PORTLET_SEGMENT);
      boolean rewriteWikiSegment = wikiIndex > 0 && isLegacyWikiNodeUri(segments, wikiIndex);
      if (rewriteWikiSegment || wikiPortletIndex > 0) {
        if (rewriteWikiSegment) {
          segments.set(wikiIndex, NOTES_SEGMENT);
        }
        if (wikiPortletIndex > 0) {
          segments.set(wikiPortletIndex, NOTES_SEGMENT);
        }
        String location = String.join("/", segments);
        String queryString = httpServletRequest.getQueryString();
        httpServletResponse.sendRedirect(queryString == null ? location : location + "?" + queryString);
        return;
      }
    }
    chain.doFilter(request, response);
  }

  private boolean isLegacyWikiNodeUri(List<String> segments, int wikiIndex) {
    if (wikiIndex < navigationStartIndex(segments)) {
      return false;
    }
    SiteKey siteKey = resolveSiteKey(segments);
    return siteKey == null || !Utils.siteNavigationContainsNode(siteKey, LEGACY_WIKI_SEGMENT);
  }

  private int navigationStartIndex(List<String> segments) {
    if (segments.size() > 2
        && (GROUP_SITE_URI_MARKER.equals(segments.get(2)) || SPACE_SITE_URI_MARKER.equals(segments.get(2)))) {
      return 4;
    }
    return 3;
  }

  private SiteKey resolveSiteKey(List<String> segments) {
    try {
      if (segments.size() > 3 && GROUP_SITE_URI_MARKER.equals(segments.get(2))) {
        return SiteKey.group(segments.get(3).replace(':', '/'));
      }
      if (segments.size() > 3 && SPACE_SITE_URI_MARKER.equals(segments.get(2))) {
        SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
        Space space = spaceService.getSpaceById(segments.get(3));
        return space == null ? null : SiteKey.group(space.getGroupId());
      }
      if (segments.size() > 2) {
        return SiteKey.portal(segments.get(2));
      }
      return null;
    } catch (Exception e) {
      return null;
    }
  }
}
