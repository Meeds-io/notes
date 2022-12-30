/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.indexing.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.indexing.IndexingService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;

@RunWith(MockitoJUnitRunner.class)
public class UnifiedSearchPageWikiListenerTest {
  @Mock
  private WikiService     wikiService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private ActivityManager activityManager;

  @Mock
  private SpaceService    spaceService;

  @Mock
  private IndexingService indexingService;

  @Test
  public void testPostAddPage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id123");

    // When
    unifiedSearchPageWikiListener.postAddPage("wikiType", "root", "id123", page);

    // Then
    verify(indexingService, times(1)).add(any());
  }

  @Test
  public void testPostUpdatePage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id1234");

    // When
    unifiedSearchPageWikiListener.postUpdatePage("wikiType", "root", "id123", page, PageUpdateType.EDIT_PAGE_CONTENT);

    // Then
    verify(indexingService, times(1)).update(any(), any());
  }

  @Test
  public void testPostDeletePage() throws Exception {
    // Given
    UnifiedSearchPageWikiListener unifiedSearchPageWikiListener = new UnifiedSearchPageWikiListener(indexingService);

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id123");

    // When
    unifiedSearchPageWikiListener.postDeletePage("wikiType", "root", "id123", page);

    // Then
    verify(indexingService, times(1)).delete(any());
  }

}
