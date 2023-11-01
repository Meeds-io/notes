package org.exoplatform.wiki.service.listener;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.storage.cache.CachedActivityStorage;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.jpa.search.NoteVersionLanguageIndexingServiceConnector;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class MetadataItemModifiedTest {

  @Mock
  private IndexingService       indexingService;

  @Mock
  private NoteService           noteService;

  @Mock
  private CachedActivityStorage activityStorage;

  @Test
  public void testNoInteractionWhenMetadataNotForNews() throws Exception {
    MetadataItemModified metadataItemModified = new MetadataItemModified(noteService, indexingService, activityStorage);
    MetadataItem metadataItem = mock(MetadataItem.class);
    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);
    when(event.getData().getObjectType()).thenReturn("activity");
    when(metadataItem.getObjectId()).thenReturn("1");

    metadataItemModified.onEvent(event);

    verifyNoInteractions(noteService);
  }

  @Test
  public void testReindexNoteWhenNoteSetAsFavorite() throws Exception {
    MetadataItemModified metadataItemModified = new MetadataItemModified(noteService, indexingService, activityStorage);
    String pageId = "100";

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getObjectType()).thenReturn(Utils.NOTES_METADATA_OBJECT_TYPE);
    when(metadataItem.getObjectId()).thenReturn(pageId);

    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);

    Page page = new Page();
    page.setId(pageId);
    when(noteService.getNoteById(pageId)).thenReturn(page);

    metadataItemModified.onEvent(event);
    verify(noteService, times(1)).getNoteById(pageId);
    verify(indexingService, times(1)).reindex(WikiPageIndexingServiceConnector.TYPE, pageId);
  }

  @Test
  public void testReindexNoteVersionLangWhenSetAsFavorite() throws Exception {
    MetadataItemModified metadataItemModified = new MetadataItemModified(noteService, indexingService, activityStorage);
    String pageId = "100-en";

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getObjectType()).thenReturn(Utils.NOTES_METADATA_OBJECT_TYPE);
    when(metadataItem.getObjectId()).thenReturn(pageId);

    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);


    metadataItemModified.onEvent(event);
    verify(noteService, times(0)).getNoteById(pageId);
    verify(indexingService, times(1)).reindex(NoteVersionLanguageIndexingServiceConnector.TYPE, pageId);
  }

  @Test
  public void testCleanPageActivityCacheWhenMarkAsFavorite() throws Exception {
    MetadataItemModified metadataItemModified = new MetadataItemModified(noteService, indexingService, activityStorage);
    String pageId = "200";

    MetadataItem metadataItem = mock(MetadataItem.class);
    when(metadataItem.getObjectType()).thenReturn(Utils.NOTES_METADATA_OBJECT_TYPE);
    when(metadataItem.getObjectId()).thenReturn(pageId);

    Event<Long, MetadataItem> event = mock(Event.class);
    when(event.getData()).thenReturn(metadataItem);

    String activityId = "activityId";
    Page page = new Page();
    page.setId(pageId);
    page.setActivityId(activityId);
    when(noteService.getNoteById(pageId)).thenReturn(page);

    metadataItemModified.onEvent(event);
    verify(noteService, times(1)).getNoteById(pageId);
    verify(indexingService, times(1)).reindex(WikiPageIndexingServiceConnector.TYPE, pageId);
    verify(activityStorage, times(1)).clearActivityCached(activityId);
  }

}
