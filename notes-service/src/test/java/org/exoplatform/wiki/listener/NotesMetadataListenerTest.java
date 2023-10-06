package org.exoplatform.wiki.listener;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.Page;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NotesMetadataListenerTest {

  private NotesMetadataListener notesMetadataListener;

  @Mock
  private TagService            tagService;

  @Mock
  private IndexingService       indexingService;

  @Mock
  private IdentityManager       identityManager;

  @Mock
  private SpaceService          spaceService;

  @Before
  public void setUp() throws Exception {
    notesMetadataListener =
                          new NotesMetadataListener(indexingService, identityManager, spaceService, tagService);
  }

  @Test
  public void onEvent() {
    Identity identity = mock(Identity.class);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity);
    when(identity.getId()).thenReturn("1");
    Identity spaceidentity = mock(Identity.class);
    when(identityManager.getOrCreateSpaceIdentity("space")).thenReturn(spaceidentity);
    when(spaceidentity.getId()).thenReturn("2");
    when(identity.getId()).thenReturn("1");
    Space space = new Space();
    space.setPrettyName("space");
    when(spaceService.getSpaceByGroupId("/spaces/space")).thenReturn(space);
    Page note = new Page();
    note.setId("1");
    note.setWikiOwner("/spaces/space");
    note.setContent("test ><a class=\"metadata-tag\">#testTag</a></span>");
    Event<String, Page> event = new Event<>("note.posted", "user", note);
    notesMetadataListener.onEvent(event);
    Set<TagName> tagNames = tagService.detectTagNames(note.getContent());
    verify(tagService, times(1)).saveTags(new TagObject("notes", note.getId(), note.getParentPageId()), tagNames, 2L, 1L);
    verify(indexingService, times(1)).reindex(WikiPageIndexingServiceConnector.TYPE, note.getId());
  }
}
