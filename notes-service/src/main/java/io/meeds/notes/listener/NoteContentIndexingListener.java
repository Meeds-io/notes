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

import io.meeds.common.ContainerTransactional;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.listener.Asynchronous;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerBase;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.PageContentBlockPluginService;

import jakarta.annotation.PostConstruct;

/**
 * Reacts to Notes' {@code note.posted} / {@code note.updated} /
 * {@code note.deleted} events to keep the portal Page carrying this note as
 * a Single Note View content block in sync with Social's generic "page"
 * search index, whether the note's content changed or the note itself was
 * deleted.
 * <p>
 * Resolving the page and the search index's document id is entirely
 * {@link PageContentBlockPluginService}'s responsibility — Notes only
 * knows its own content type and the {@code CMSSetting} name (which
 * equals the note's own name, an invariant {@link NotePageViewService}
 * guarantees by always creating such a note as {@code new Page(name, name)}).
 * <p>
 * Deletion can't be handled by just re-indexing: the {@code CMSSetting}
 * binding the note to its page survives the note's deletion (nothing
 * deletes it), so a plain re-index still resolves a page/block id — it's
 * only the connector's {@code create()} that then finds no note content
 * and returns {@code null}, which the indexing framework treats as "skip
 * this operation", not "delete the existing document". The stale document
 * would linger forever unless deletion is unindexed explicitly.
 */
@Component
@Asynchronous
public class NoteContentIndexingListener implements ListenerBase<String, org.exoplatform.wiki.model.Page> {

  private static final String           NOTE_DELETED_EVENT = "note.deleted";

  @Autowired
  private ListenerService               listenerService;

  @Autowired
  private PageContentBlockPluginService pluginService;

  @PostConstruct
  public void init() {
    listenerService.addListener("note.posted", this);
    listenerService.addListener("note.updated", this);
    listenerService.addListener(NOTE_DELETED_EVENT, this);
  }

  @Override
  @ContainerTransactional
  public void onEvent(Event<String, org.exoplatform.wiki.model.Page> event) throws Exception {
    org.exoplatform.wiki.model.Page note = event.getData();
    if (note == null || StringUtils.isBlank(note.getName())) {
      return;
    }
    if (StringUtils.equals(event.getEventName(), NOTE_DELETED_EVENT)) {
      pluginService.unindexContentBlock(NotePageViewService.CMS_CONTENT_TYPE, note.getName());
    } else {
      pluginService.reindexContentBlock(NotePageViewService.CMS_CONTENT_TYPE, note.getName());
    }
  }

}
