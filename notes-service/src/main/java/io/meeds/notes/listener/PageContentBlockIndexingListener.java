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
package io.meeds.notes.listener;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;

import org.exoplatform.wiki.jpa.search.PageContentIndexingServiceConnector;

import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

/**
 * Reacts to Layout's {@code layout.page.updated} / {@code layout.page.permissions.updated}
 * events (fired by {@code io.meeds.layout.service.PageLayoutService}) to keep the
 * "page" search index in sync with whether a page currently carries a Notes
 * "notePage" content block.
 */
public class PageContentBlockIndexingListener extends Listener<String, String> {

  private static final String CMS_CONTENT_TYPE = "notePage";

  private final IndexingService indexingService;

  public PageContentBlockIndexingListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void onEvent(Event<String, String> event) throws Exception {
    String pageRef = event.getData();
    if (StringUtils.isBlank(pageRef)) {
      return;
    }
    org.exoplatform.portal.config.model.Page page = getLayoutService().getPage(PageKey.parse(pageRef));
    if (page == null) {
      return;
    }
    String storageId = page.getStorageId();
    if (hasNotePageContentBlock(pageRef)) {
      indexingService.index(PageContentIndexingServiceConnector.TYPE, storageId);
    } else {
      indexingService.unindex(PageContentIndexingServiceConnector.TYPE, storageId);
    }
  }

  private boolean hasNotePageContentBlock(String pageRef) {
    return getCmsService().getSettingsByType(CMS_CONTENT_TYPE)
                          .stream()
                          .map(CMSSetting::getPageReference)
                          .anyMatch(pageReference -> StringUtils.equals(pageReference, pageRef));
  }

  private CMSService getCmsService() {
    return CommonsUtils.getService(CMSService.class);
  }

  private LayoutService getLayoutService() {
    return CommonsUtils.getService(LayoutService.class);
  }

}
