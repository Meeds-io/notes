/**
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
package org.exoplatform.wiki.jpa.search;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;

@RunWith(MockitoJUnitRunner.class)
public class WikiPageIndexingServiceConnectorTest {

  private static final String PAGE_ID = "223";

  @Mock
  private MetadataService     metadataService;

  @Mock
  private NoteService         noteService;

  @Mock
  private InitParams          initParams;

  @Mock
  private PropertiesParam     propertiesParam;

  @Mock
  private Page page;

  @Before
  public void setUp() {
    when(initParams.getPropertiesParam("constructor.params")).thenReturn(propertiesParam);
    when(propertiesParam.getProperty("index_current")).thenReturn("currentIndex");
    when(propertiesParam.getProperty("index_alias")).thenReturn("currentAlias");
    when(page.getWikiType()).thenReturn("group");
    when(page.getWikiOwner()).thenReturn("owner");
    when(page.getCreatedDate()).thenReturn(new Date());
    when(page.getUpdatedDate()).thenReturn(new Date());
  }

  @Test
  public void testCreateNoteIndex() throws WikiException {
    WikiPageIndexingServiceConnector indexingServiceConnector = new WikiPageIndexingServiceConnector(initParams,
                                                                                                     noteService,
                                                                                                     metadataService);

    assertNull(indexingServiceConnector.create(PAGE_ID));

    when(noteService.getNoteById(PAGE_ID)).thenReturn(page);
    assertNotNull(indexingServiceConnector.create(PAGE_ID));

    when(page.getOwner()).thenReturn(IdentityConstants.SYSTEM);
    assertNull(indexingServiceConnector.create(PAGE_ID));
  }

  @Test
  public void testUpdateNoteIndex() throws WikiException {
    WikiPageIndexingServiceConnector indexingServiceConnector = new WikiPageIndexingServiceConnector(initParams,
                                                                                                     noteService,
                                                                                                     metadataService);

    assertNull(indexingServiceConnector.update(PAGE_ID));

    when(noteService.getNoteById(PAGE_ID)).thenReturn(page);
    assertNotNull(indexingServiceConnector.update(PAGE_ID));

    when(page.getOwner()).thenReturn(IdentityConstants.SYSTEM);
    assertNull(indexingServiceConnector.update(PAGE_ID));
  }

}
