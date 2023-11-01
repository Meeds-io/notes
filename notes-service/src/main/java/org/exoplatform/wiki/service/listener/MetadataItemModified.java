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
