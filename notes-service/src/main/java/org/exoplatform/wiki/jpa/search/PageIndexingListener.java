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
package org.exoplatform.wiki.jpa.search;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

/**
 * Listener on pages creation/update/deletion to index them
 */
public class PageIndexingListener extends PageWikiListener {

  private IndexingService indexingService;

  public PageIndexingListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    if (!page.isDraftPage()) {
      indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

  @Override
  public void postUpdatePageVersion(String pageVersionId) {
    indexingService.reindex(NoteVersionLanguageIndexingServiceConnector.TYPE, pageVersionId);
  }

  @Override
  public void postDeletePageVersion(String pageVersionId) {
    indexingService.unindex(NoteVersionLanguageIndexingServiceConnector.TYPE, pageVersionId);
  }
}
