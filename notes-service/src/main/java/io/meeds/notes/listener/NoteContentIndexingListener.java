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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerBase;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.cms.storage.elasticsearch.PageContentIndexingConnector;

import jakarta.annotation.PostConstruct;

/**
 * Reacts to Notes' {@code note.posted} / {@code note.updated} events to
 * re-index the portal Page carrying this note as a Single Note View content
 * block, so Social's generic "page" search index stays in sync when the
 * note's content (not the page layout) is what changed.
 * <p>
 * A note is a Single Note View's content when a {@code notePage}
 * {@link CMSSetting} exists whose name equals the note's own name (that
 * invariant is guaranteed by {@link NotePageViewService}, which always
 * creates such a note as {@code new Page(name, name)}).
 */
@Component
public class NoteContentIndexingListener implements ListenerBase<String, org.exoplatform.wiki.model.Page> {

  @Autowired
  private ListenerService  listenerService;

  @Autowired
  private IndexingService  indexingService;

  @Autowired
  private CMSService       cmsService;

  @Autowired
  private LayoutService    layoutService;

  @PostConstruct
  public void init() {
    listenerService.addListener("note.posted", this);
    listenerService.addListener("note.updated", this);
  }

  @Override
  public void onEvent(Event<String, org.exoplatform.wiki.model.Page> event) throws Exception {
    org.exoplatform.wiki.model.Page note = event.getData();
    if (note == null || StringUtils.isBlank(note.getName())) {
      return;
    }
    CMSSetting setting = cmsService.getSettingsByType(NotePageViewService.CMS_CONTENT_TYPE)
                                   .stream()
                                   .filter(s -> StringUtils.equals(s.getName(), note.getName()))
                                   .findFirst()
                                   .orElse(null);
    if (setting == null) {
      return;
    }
    Page page = layoutService.getPage(PageKey.parse(setting.getPageReference()));
    if (page == null) {
      return;
    }
    indexingService.reindex(PageContentIndexingConnector.TYPE, page.getStorageId());
  }

}
