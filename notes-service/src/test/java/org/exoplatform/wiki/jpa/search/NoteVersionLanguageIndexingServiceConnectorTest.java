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
package org.exoplatform.wiki.jpa.search;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.service.NoteService;

@RunWith(MockitoJUnitRunner.class)
public class NoteVersionLanguageIndexingServiceConnectorTest {

  @Mock
  private MetadataService                             metadataService;

  @Mock
  private NoteService                                 noteService;

  @Mock
  private InitParams                                  initParams;

  @Mock
  private PropertiesParam                             propertiesParam;

  @Mock
  private PageVersion                                 pageVersion;

  @Mock
  private Page                                        page;

  private static final String                         PAGE_VERSION_ID = "223-en";

  private NoteVersionLanguageIndexingServiceConnector noteVersionLanguageIndexingServiceConnector;

  @Before
  public void setUp() throws Exception {
    when(initParams.getPropertiesParam("constructor.params")).thenReturn(propertiesParam);
    when(propertiesParam.getProperty("index_current")).thenReturn("currentIndex");
    when(propertiesParam.getProperty("index_alias")).thenReturn("currentAlias");
    when(pageVersion.getParent()).thenReturn(page);
    when(page.getWikiType()).thenReturn("group");
    when(page.getWikiOwner()).thenReturn("owner");
    when(pageVersion.getCreatedDate()).thenReturn(new Date());
    when(pageVersion.getUpdatedDate()).thenReturn(new Date());
    noteVersionLanguageIndexingServiceConnector = new NoteVersionLanguageIndexingServiceConnector(initParams,
                                                                                                  noteService,
                                                                                                  metadataService);
  }

  @Test
  public void create() {
    assertNull(noteVersionLanguageIndexingServiceConnector.create(PAGE_VERSION_ID));

    when(noteService.getPublishedVersionByPageIdAndLang(223L, "en")).thenReturn(pageVersion);
    assertNotNull(noteVersionLanguageIndexingServiceConnector.create(PAGE_VERSION_ID));

  }

  @Test
  public void update() {
    assertNull(noteVersionLanguageIndexingServiceConnector.update(PAGE_VERSION_ID));

    when(noteService.getPublishedVersionByPageIdAndLang(223L, "en")).thenReturn(pageVersion);
    assertNotNull(noteVersionLanguageIndexingServiceConnector.update(PAGE_VERSION_ID));
  }
}
