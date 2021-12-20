package org.exoplatform.wiki.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.listener.NotesMetadataListener;
import org.exoplatform.wiki.utils.Utils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
@SuppressWarnings("unchecked")
public class NotesMetadataListenerTest {

  @Mock
  private IndexingService indexingService;

  @Mock
  private NoteService     noteService;

  @Mock
  private FavoriteService favoriteService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private ActivityManager activityManager;

  @Test
  public void testReindexNoteWhenNoteSetAsFavorite() throws Exception {
    NotesMetadataListener notesMetadataListener = new NotesMetadataListener(indexingService,
                                                                            favoriteService,
                                                                            identityManager,
                                                                            noteService,
                                                                            activityManager);
    ConversationState.setCurrent(new ConversationState(new Identity("john")));
    MetadataItem metadataItem = mock(MetadataItem.class);
    Event<Long, MetadataItem> event = mock(Event.class);
    lenient().when(event.getData()).thenReturn(metadataItem);
    lenient().when(event.getData().getObjectType()).thenReturn("notes");
    lenient().when(metadataItem.getObjectId()).thenReturn("1");

    notesMetadataListener.onEvent(event);

    verifyNoInteractions(noteService);
  }

  @Test
  public void testReindexActivityWhenNoteSetAsFavorite() throws Exception {
    NotesMetadataListener notesMetadataListener = new NotesMetadataListener(indexingService,
                                                                            favoriteService,
                                                                            identityManager,
                                                                            noteService,
                                                                            activityManager);
    Identity johnIdentity = new Identity("1", Collections.singletonList(new MembershipEntry("john")));
    ConversationState.setCurrent(new ConversationState(johnIdentity));
    MetadataItem metadataItem = mock(MetadataItem.class);
    Event<Long, MetadataItem> event = mock(Event.class);
    lenient().when(event.getData()).thenReturn(metadataItem);
    lenient().when(event.getData().getObjectType()).thenReturn("activity");
    lenient().when(event.getEventName()).thenReturn("social.metadataItem.created");
    lenient().when(metadataItem.getObjectId()).thenReturn("1");
    org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                     new org.exoplatform.social.core.identity.model.Identity(OrganizationIdentityProvider.NAME,
                                                                                                                             "john");
    userIdentity.setId("1");
    ExoSocialActivity activity = new ExoSocialActivityImpl();
    Map<String, List<MetadataItem>> metadataItems = new HashMap<String, List<MetadataItem>>();
    Map<String, String> templateParams = new HashMap<String, String>();
    templateParams.put(Utils.PAGE_ID_KEY, "page");
    templateParams.put(Utils.PAGE_OWNER_KEY, "portal");
    templateParams.put(Utils.PAGE_TYPE_KEY, "group");
    activity.setTemplateParams(templateParams);
    activity.setType(Utils.WIKI_APP_ID);
    Page note = mock(Page.class);
    lenient().when(note.getId()).thenReturn("1");
    lenient().when(note.getMetadatas()).thenReturn(metadataItems);
    lenient().when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "1")).thenReturn(userIdentity);
    lenient().when(activityManager.getActivity("1")).thenReturn(activity);
    lenient().when(noteService.getNoteOfNoteBookByName("group", "portal", "page", johnIdentity)).thenReturn(note);

    notesMetadataListener.onEvent(event);
    verify(noteService, times(1)).getNoteOfNoteBookByName("group", "portal", "page", johnIdentity);
    verify(favoriteService, times(1)).createFavorite(any());
    verify(indexingService, times(1)).reindex(WikiPageIndexingServiceConnector.TYPE, note.getId());
  }

}
