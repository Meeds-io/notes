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

import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.cms.storage.elasticsearch.PageContentIndexingConnector;

@RunWith(MockitoJUnitRunner.class)
public class NoteContentIndexingListenerTest {

  private static final String                 NOTE_NAME  = "note1";

  private static final PageKey                PAGE_KEY   = PageKey.parse("portal::site::page");

  private static final String                 STORAGE_ID = "page_139";

  @Mock
  private ListenerService                     listenerService;

  @Mock
  private IndexingService                     indexingService;

  @Mock
  private CMSService                          cmsService;

  @Mock
  private LayoutService                       layoutService;

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
  }

  @Test
  public void shouldIgnoreEventWithBlankNoteName() throws Exception {
    when(note.getName()).thenReturn("");

    listener.onEvent(new Event<>("note.posted", "user", note));

    verify(indexingService, never()).reindex(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void shouldIgnoreEventWhenNoteIsNotBoundToAPage() throws Exception {
    when(cmsService.getSettingsByType(NotePageViewService.CMS_CONTENT_TYPE)).thenReturn(Collections.emptyList());

    listener.onEvent(new Event<>("note.updated", "user", note));

    verify(indexingService, never()).reindex(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void shouldIgnoreEventWhenBoundPageNoLongerExists() throws Exception {
    CMSSetting setting = new CMSSetting(NotePageViewService.CMS_CONTENT_TYPE, NOTE_NAME, PAGE_KEY.format(), 0);
    when(cmsService.getSettingsByType(NotePageViewService.CMS_CONTENT_TYPE)).thenReturn(List.of(setting));
    when(layoutService.getPage(PAGE_KEY)).thenReturn(null);

    listener.onEvent(new Event<>("note.posted", "user", note));

    verify(indexingService, never()).reindex(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
  }

  @Test
  public void shouldReindexBoundPageWhenNoteContentChanges() throws Exception {
    CMSSetting setting = new CMSSetting(NotePageViewService.CMS_CONTENT_TYPE, NOTE_NAME, PAGE_KEY.format(), 0);
    when(cmsService.getSettingsByType(NotePageViewService.CMS_CONTENT_TYPE)).thenReturn(List.of(setting));
    Page page = mock(Page.class);
    when(page.getStorageId()).thenReturn(STORAGE_ID);
    when(layoutService.getPage(PAGE_KEY)).thenReturn(page);

    listener.onEvent(new Event<>("note.updated", "user", note));

    verify(indexingService).reindex(PageContentIndexingConnector.TYPE,
                                    PageContentIndexingConnector.buildBlockId(STORAGE_ID,
                                                                              NotePageViewService.CMS_CONTENT_TYPE,
                                                                              NOTE_NAME));
  }

}
