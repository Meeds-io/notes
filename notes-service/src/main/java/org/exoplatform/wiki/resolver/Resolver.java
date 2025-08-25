/**
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
package org.exoplatform.wiki.resolver;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.portal.mop.SiteKey;
import org.exoplatform.portal.mop.user.UserNode;
import org.exoplatform.wiki.service.WikiPageParams;

public abstract class Resolver extends BaseComponentPlugin{
  /**
   * 
   * @param requestURL URL of incoming request
   * @param siteKey site that contains wiki portlet
   * @param portalUserNode page node that contains wiki portlet  
   * @return Wiki params
   * @throws Exception if error occured
   */
  public abstract WikiPageParams extractPageParams(String requestURL, SiteKey siteKey, UserNode portalUserNode) throws Exception ;
  
}
