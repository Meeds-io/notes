/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.config.UserACL;
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
  private IdentityManager       identityManager;

  @Mock
  private SpaceService          spaceService;
  
  @Mock
  private UserACL               userACL;

  @Before
  public void setUp() throws Exception {
    notesMetadataListener =
                          new NotesMetadataListener(identityManager, spaceService, tagService, userACL);
  }

  @Test
  public void onEvent() {
    Identity identity = mock(Identity.class);
    when(userACL.getSuperUser()).thenReturn("user");
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

    clearInvocations(tagService);
    event = new Event<>("note.updated", null, note);
    notesMetadataListener.onEvent(event);
    verify(tagService, times(1)).saveTags(new TagObject("notes", note.getId(), note.getParentPageId()), tagNames, 2L, 1L);
  }
}
