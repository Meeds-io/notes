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

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainer;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.utils.Utils;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WikiToNotesRedirectFilterTest {

  private static final String        REST_CONTEXT_NAME = "rest";

  @Mock
  private HttpServletRequest         request;

  @Mock
  private HttpServletResponse        response;

  @Mock
  private FilterChain                chain;

  @Mock
  private ExoContainer               container;

  @Mock
  private ExoContainerContext        containerContext;

  @Mock
  private SpaceService               spaceService;

  private WikiToNotesRedirectFilter  filter = new WikiToNotesRedirectFilter();

  @Before
  public void setUp() {
    when(container.getContext()).thenReturn(containerContext);
    when(containerContext.getRestContextName()).thenReturn(REST_CONTEXT_NAME);
  }

  @Test
  public void testNoRedirectWhenSpaceSiteHasWikiNode() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/g/:spaces:foo/wiki");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
      utilsStatic.when(() -> Utils.siteNavigationContainsNode(SiteKey.group("/spaces/foo"), "wiki")).thenReturn(true);

      filter.doFilter(request, response, chain);

      verify(chain).doFilter(request, response);
      verify(response, never()).sendRedirect(anyString());
    }
  }

  @Test
  public void testRedirectsLegacySpaceWikiUri() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/g/:spaces:foo/wiki/123");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
      utilsStatic.when(() -> Utils.siteNavigationContainsNode(SiteKey.group("/spaces/foo"), "wiki")).thenReturn(false);

      filter.doFilter(request, response, chain);

      verify(response).sendRedirect("/portal/g/:spaces:foo/notes/123");
      verify(chain, never()).doFilter(request, response);
    }
  }

  @Test
  public void testRedirectPreservesQueryString() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/intranet/wiki");
    when(request.getQueryString()).thenReturn("path=home&lang=en");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
      utilsStatic.when(() -> Utils.siteNavigationContainsNode(SiteKey.portal("intranet"), "wiki")).thenReturn(false);

      filter.doFilter(request, response, chain);

      verify(response).sendRedirect("/portal/intranet/notes?path=home&lang=en");
    }
  }

  @Test
  public void testRedirectsLegacyWikiPortletUri() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/intranet/home/WikiPortlet/page");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);

      filter.doFilter(request, response, chain);

      verify(response).sendRedirect("/portal/intranet/home/notes/page");
      verify(chain, never()).doFilter(request, response);
    }
  }

  @Test
  public void testNoRedirectOnRestUri() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/rest/wiki/something");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);

      filter.doFilter(request, response, chain);

      verify(chain).doFilter(request, response);
      verify(response, never()).sendRedirect(anyString());
    }
  }

  @Test
  public void testNoRedirectWhenWikiIsTheSiteName() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/wiki/home");
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);

      filter.doFilter(request, response, chain);

      verify(chain).doFilter(request, response);
      verify(response, never()).sendRedirect(anyString());
      utilsStatic.verifyNoInteractions();
    }
  }

  @Test
  public void testSpaceIdUriResolvesGroupSite() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/s/42/wiki");
    Space space = mock(Space.class);
    when(space.getGroupId()).thenReturn("/spaces/foo");
    when(spaceService.getSpaceById("42")).thenReturn(space);
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<CommonsUtils> commonsStatic = mockStatic(CommonsUtils.class);
         MockedStatic<Utils> utilsStatic = mockStatic(Utils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
      commonsStatic.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);
      utilsStatic.when(() -> Utils.siteNavigationContainsNode(SiteKey.group("/spaces/foo"), "wiki")).thenReturn(true);

      filter.doFilter(request, response, chain);

      verify(chain).doFilter(request, response);
      verify(response, never()).sendRedirect(anyString());
    }
  }

  @Test
  public void testRedirectsWhenSiteUnresolvable() throws Exception {
    when(request.getRequestURI()).thenReturn("/portal/s/42/wiki");
    when(spaceService.getSpaceById("42")).thenReturn(null);
    try (MockedStatic<ExoContainerContext> containerStatic = mockStatic(ExoContainerContext.class);
         MockedStatic<CommonsUtils> commonsStatic = mockStatic(CommonsUtils.class)) {
      containerStatic.when(ExoContainerContext::getCurrentContainer).thenReturn(container);
      commonsStatic.when(() -> CommonsUtils.getService(SpaceService.class)).thenReturn(spaceService);

      filter.doFilter(request, response, chain);

      verify(response).sendRedirect("/portal/s/42/notes");
      verify(chain, never()).doFilter(request, response);
    }
  }
}
