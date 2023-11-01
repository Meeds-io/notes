/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service.listener;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;

/**
 * Listener to trigger actions on page operations
 */
public abstract class PageWikiListener extends BaseComponentPlugin {
  public abstract void postAddPage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;

  public abstract void postUpdatePage(final String wikiType, final String wikiOwner, final String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException;

  public abstract void postDeletePage(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;

  public abstract void postgetPagefromTree(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;

  public abstract void postgetPagefromBreadCrumb(final String wikiType, final String wikiOwner, final String pageId, Page page) throws WikiException;

  public void postUpdatePageVersion(String pageVersionId) {
    // Nothing
  }

  public void postDeletePageVersion(String pageVersionId) {
    // Nothing
  }

}
