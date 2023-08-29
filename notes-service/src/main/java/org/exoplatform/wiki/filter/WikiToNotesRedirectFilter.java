/*
 * Copyright (C) 2021 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wiki.filter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.web.filter.Filter;

public class WikiToNotesRedirectFilter implements Filter {

  public WikiToNotesRedirectFilter() {
  }

  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
    HttpServletRequest httpServletRequest = (HttpServletRequest) request;
    HttpServletResponse httpServletResponse = (HttpServletResponse) response;
    String reqUri = httpServletRequest.getRequestURI();
    boolean isRestUri = reqUri.contains(ExoContainerContext.getCurrentContainer().getContext().getRestContextName());
    if (!isRestUri) {
      List resList = Arrays.asList(reqUri.split("/"));
      int i = resList.indexOf("wiki");
      int j = resList.indexOf("WikiPortlet");
      if (i > 0 || j > 0) {
        if (i > 0)
          resList.set(i, "notes");
        if (j > 0)
          resList.set(j, "notes");
        reqUri = String.join("/", resList);
        httpServletResponse.sendRedirect(reqUri);
        return;
      }
    }
    chain.doFilter(request, response);
  }
}
