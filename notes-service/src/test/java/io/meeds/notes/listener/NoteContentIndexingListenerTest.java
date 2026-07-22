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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.service.PageContentBlockPluginService;

@RunWith(MockitoJUnitRunner.class)
public class NoteContentIndexingListenerTest {

  private static final String                 NOTE_NAME = "note1";

  @Mock
  private ListenerService                     listenerService;

  @Mock
  private PageContentBlockPluginService        pluginService;

  @InjectMocks
  private NoteContentIndexingListener         listener;

  private org.exoplatform.wiki.model.Page     note;

  @Before
  public void setup() {
    note = mock(org.exoplatform.wiki.model.Page.class);
    when(note.getName()).thenReturn(NOTE_NAME);
  }

  @Test
  public void shouldRegisterListenersOnInit() {
    listener.init();

    verify(listenerService).addListener("note.posted", listener);
    verify(listenerService).addListener("note.updated", listener);
    verify(listenerService).addListener("note.deleted", listener);
  }

  @Test
  public void shouldIgnoreEventWithBlankNoteName() throws Exception {
    when(note.getName()).thenReturn("");

    listener.onEvent(new Event<>("note.posted", "user", note));

    verify(pluginService, never()).reindexContentBlock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void shouldIgnoreEventWithNullNote() throws Exception {
    listener.onEvent(new Event<>("note.posted", "user", null));

    verify(pluginService, never()).reindexContentBlock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void shouldDelegateReindexToPluginServiceWhenNoteContentChanges() throws Exception {
    listener.onEvent(new Event<>("note.updated", "user", note));

    verify(pluginService).reindexContentBlock(NotePageViewService.CMS_CONTENT_TYPE, NOTE_NAME);
  }

  @Test
  public void shouldDelegateUnindexToPluginServiceWhenNoteIsDeleted() throws Exception {
    listener.onEvent(new Event<>("note.deleted", "user", note));

    verify(pluginService).unindexContentBlock(NotePageViewService.CMS_CONTENT_TYPE, NOTE_NAME);
    verify(pluginService, never()).reindexContentBlock(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

}
