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
package io.meeds.notes.upgrade;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.stream.LongStream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

@RunWith(MockitoJUnitRunner.class)
public class NoteHomePageIndexingUpgradePluginTest {

  private static final int                  BATCH_SIZE = 100;

  @Mock
  private WikiDAO                           wikiDAO;

  @Mock
  private IndexingService                   indexingService;

  @Mock
  private SettingService                    settingService;

  private NoteHomePageIndexingUpgradePlugin plugin;

  @Before
  public void setUp() {
    InitParams initParams = new InitParams();
    ValueParam productGroupId = new ValueParam();
    productGroupId.setName(UpgradeProductPlugin.PRODUCT_GROUP_ID);
    productGroupId.setValue("org.exoplatform.platform");
    initParams.put(UpgradeProductPlugin.PRODUCT_GROUP_ID, productGroupId);
    plugin = new NoteHomePageIndexingUpgradePlugin(wikiDAO, indexingService, settingService, initParams);
  }

  @Test
  public void testQueueEveryHomePageForIndexing() {
    when(wikiDAO.findAllHomePageIds(0, BATCH_SIZE)).thenReturn(List.of(1L, 7L, 12L));
    when(wikiDAO.findAllHomePageIds(BATCH_SIZE, BATCH_SIZE)).thenReturn(Collections.emptyList());

    plugin.processUpgrade(null, null);

    // reindex and not index: index would be rejected for the home pages that are
    // already in the index because they were edited at least once
    verify(indexingService).reindex(WikiPageIndexingServiceConnector.TYPE, "1");
    verify(indexingService).reindex(WikiPageIndexingServiceConnector.TYPE, "7");
    verify(indexingService).reindex(WikiPageIndexingServiceConnector.TYPE, "12");
    verify(indexingService, never()).index(anyString(), anyString());
  }

  @Test
  public void testIterateOverEveryBatch() {
    List<Long> firstBatch = LongStream.rangeClosed(1, BATCH_SIZE).boxed().toList();
    when(wikiDAO.findAllHomePageIds(0, BATCH_SIZE)).thenReturn(firstBatch);
    when(wikiDAO.findAllHomePageIds(BATCH_SIZE, BATCH_SIZE)).thenReturn(List.of(101L));
    when(wikiDAO.findAllHomePageIds(2 * BATCH_SIZE, BATCH_SIZE)).thenReturn(Collections.emptyList());

    plugin.processUpgrade(null, null);

    verify(indexingService, times(BATCH_SIZE + 1)).reindex(eq(WikiPageIndexingServiceConnector.TYPE), anyString());
  }

  @Test
  public void testKeepGoingWhenOneHomePageFails() {
    when(wikiDAO.findAllHomePageIds(0, BATCH_SIZE)).thenReturn(List.of(1L, 7L, 12L));
    when(wikiDAO.findAllHomePageIds(BATCH_SIZE, BATCH_SIZE)).thenReturn(Collections.emptyList());
    doThrow(new IllegalStateException("indexing queue unavailable")).when(indexingService)
                                                                   .reindex(WikiPageIndexingServiceConnector.TYPE, "7");

    plugin.processUpgrade(null, null);

    verify(indexingService).reindex(WikiPageIndexingServiceConnector.TYPE, "1");
    verify(indexingService).reindex(WikiPageIndexingServiceConnector.TYPE, "12");
  }

  @Test
  public void testRunOnlyOnceWhateverTheProductVersion() {
    when(settingService.get(any(Context.class), any(Scope.class), anyString())).thenReturn(null);
    assertTrue(plugin.shouldProceedToUpgrade("7.3.1", "7.3.0", null));

    when(settingService.get(any(Context.class), any(Scope.class),
                            anyString())).thenReturn((SettingValue) SettingValue.create(true));
    assertFalse(plugin.shouldProceedToUpgrade("7.3.1", "7.3.0", null));
  }

  @Test
  public void testFlagAsExecutedOnlyWhenItSucceeded() {
    // a failing run must be retried on the next startup
    when(wikiDAO.findAllHomePageIds(0, BATCH_SIZE)).thenThrow(new IllegalStateException("database unreachable"));
    plugin.processUpgrade(null, null);
    plugin.afterUpgrade();
    verify(settingService, never()).set(any(Context.class), any(Scope.class), anyString(), any());

    // doReturn and not when(...): when() would call the still throwing stub
    doReturn(Collections.emptyList()).when(wikiDAO).findAllHomePageIds(0, BATCH_SIZE);
    plugin.processUpgrade(null, null);
    plugin.afterUpgrade();
    verify(settingService).set(any(Context.class), any(Scope.class), anyString(), any());
  }

  @Test
  public void testNothingToUpgrade() {
    when(wikiDAO.findAllHomePageIds(0, BATCH_SIZE)).thenReturn(Collections.emptyList());

    plugin.processUpgrade(null, null);

    verify(indexingService, never()).reindex(anyString(), anyString());
  }

}
