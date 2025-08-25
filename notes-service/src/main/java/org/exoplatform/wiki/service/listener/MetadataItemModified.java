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
package org.exoplatform.wiki.service.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.storage.api.ActivityStorage;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.jpa.search.NoteVersionLanguageIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

public class MetadataItemModified extends Listener<Long, MetadataItem> {

  private IndexingService       indexingService;

  private NoteService           noteService;

  private CachedActivityStorage cachedActivityStorage;

  public MetadataItemModified(NoteService noteService, IndexingService indexingService, ActivityStorage activityStorage) {
    this.noteService = noteService;
    this.indexingService = indexingService;
    if (activityStorage instanceof CachedActivityStorage) {
      this.cachedActivityStorage = (CachedActivityStorage) activityStorage;
    }
  }

  @Override
  public void onEvent(Event<Long, MetadataItem> event) throws Exception {
    MetadataItem metadataItem = event.getData();
    String objectType = metadataItem.getObjectType();
    String objectId = metadataItem.getObjectId();
    if (isNotesEvent(objectType)) {
      // Ensure to re-execute all ActivityProcessors to compute & cache
      // metadatas of the activity again
      if (!objectId.contains("-")) {
        Page page = noteService.getNoteById(objectId);
        if (page != null && StringUtils.isNotBlank(page.getActivityId())) {
          clearCache(page.getActivityId());
        }
        reindexNotes(objectId);
      } else {
        reindexLanguageVersion(objectId);
      }
    }
  }

  protected boolean isNotesEvent(String objectType) {
    return StringUtils.equals(objectType, Utils.NOTES_METADATA_OBJECT_TYPE);
  }

  private void clearCache(String activityId) {
    if (cachedActivityStorage != null) {
      cachedActivityStorage.clearActivityCached(activityId);
    }
  }

  private void reindexNotes(String pageId) {
    indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, pageId);
  }
  private void reindexLanguageVersion(String versionLangId) {
    indexingService.reindex(NoteVersionLanguageIndexingServiceConnector.TYPE, versionLangId);
  }

}
