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

package org.exoplatform.wiki.indexing.listener;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.commons.api.indexing.data.SearchEntry;
import org.exoplatform.commons.api.indexing.data.SearchEntryId;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

import java.util.HashMap;
import java.util.Map;

/**
 * Indexing with :
 * - collection : "wiki"
 * - type : wiki type
 * - name : page id
 */
public class UnifiedSearchPageWikiListener extends PageWikiListener {

  private static Log log = ExoLogger.getLogger(UnifiedSearchPageWikiListener.class);

  private final IndexingService indexingService;

  public UnifiedSearchPageWikiListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("page", page);
      SearchEntry searchEntry = new SearchEntry("wiki", wikiType, pageId, content);
      indexingService.add(searchEntry);
    }
  }

  @Override
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    if(indexingService != null) {
      Map<String, Object> content = new HashMap<String, Object>();
      content.put("page", page);
      SearchEntryId searchEntryId = new SearchEntryId("wiki", wikiType, pageId);
      indexingService.update(searchEntryId, content);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if(indexingService != null) {
      SearchEntryId searchEntryId = new SearchEntryId("wiki", wikiType, pageId);
      indexingService.delete(searchEntryId);
    }
  }


  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

}
