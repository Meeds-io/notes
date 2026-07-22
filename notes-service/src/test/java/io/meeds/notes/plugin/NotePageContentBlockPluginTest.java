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
package io.meeds.notes.plugin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.wiki.model.Page;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.model.PageContentBlock;
import io.meeds.social.cms.service.PageContentBlockPluginService;

@RunWith(MockitoJUnitRunner.class)
public class NotePageContentBlockPluginTest {

  private static final String              SETTING_NAME = "note1";

  private static final CMSSetting          SETTING      = new CMSSetting(NotePageViewService.CMS_CONTENT_TYPE,
                                                                          SETTING_NAME,
                                                                          "portal::site::page",
                                                                          0);

  @Mock
  private NotePageViewService              notePageViewService;

  @Mock
  private PageContentBlockPluginService    pluginService;

  @InjectMocks
  private NotePageContentBlockPlugin       plugin;

  @Before
  public void setup() {
    when(notePageViewService.getNotePages(SETTING_NAME)).thenReturn(Map.of());
  }

  @Test
  public void shouldRegisterItselfOnInit() {
    plugin.init();

    org.mockito.Mockito.verify(pluginService).addPlugin(plugin);
  }

  @Test
  public void shouldExposeNotePageContentType() {
    assertEquals(NotePageViewService.CMS_CONTENT_TYPE, plugin.getContentType());
  }

  @Test
  public void shouldReturnNullWhenNoDefaultLanguagePageExists() {
    assertNull(plugin.getContent(SETTING));
  }

  @Test
  public void shouldExtractPlainTextContentPerLanguageUsingUpdatedDate() {
    Page defaultPage = mock(Page.class);
    when(defaultPage.getAuthor()).thenReturn("john");
    Date updatedDate = new Date();
    when(defaultPage.getUpdatedDate()).thenReturn(updatedDate);
    when(defaultPage.getContent()).thenReturn("<p>Hello</p>");

    Page frenchPage = mock(Page.class);
    when(frenchPage.getContent()).thenReturn("<p>Bonjour</p>");

    when(notePageViewService.getNotePages(SETTING_NAME)).thenReturn(Map.of("", defaultPage, "fr", frenchPage));

    PageContentBlock content = plugin.getContent(SETTING);

    assertEquals("john", content.getAuthor());
    assertEquals(updatedDate, content.getDate());
    assertEquals("Hello", content.getContent().get(""));
    assertEquals("Bonjour", content.getContent().get("fr"));
  }

  @Test
  public void shouldFallBackToCreatedDateWhenNeverUpdated() {
    Page defaultPage = mock(Page.class);
    when(defaultPage.getContent()).thenReturn("<p>Hello</p>");
    when(defaultPage.getUpdatedDate()).thenReturn(null);
    Date createdDate = new Date();
    when(defaultPage.getCreatedDate()).thenReturn(createdDate);

    when(notePageViewService.getNotePages(SETTING_NAME)).thenReturn(Map.of("", defaultPage));

    PageContentBlock content = plugin.getContent(SETTING);

    assertEquals(createdDate, content.getDate());
  }

}
