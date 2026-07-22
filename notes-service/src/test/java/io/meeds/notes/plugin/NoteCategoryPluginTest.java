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
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.social.category.model.CategoryEntryItem;
import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.service.CategoryLinkService;

@RunWith(MockitoJUnitRunner.class)
public class NoteCategoryPluginTest {

  private static final String                     ACTIVITY_ID   = "activity-1";

  @Mock
  private NoteService                              noteService;

  @Mock
  private SpaceService                             spaceService;

  @Mock
  private IdentityManager                          identityManager;

  @InjectMocks
  private NoteCategoryPlugin                       noteCategoryPlugin;

  private static final MockedStatic<CommonsUtils>  COMMONS_UTILS = mockStatic(CommonsUtils.class);

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
  }

  @Test
  public void testToCategoryObjectResolvesToActivityWhenNotePosted() {
    Page note = new Page();
    note.setId("1");
    note.setActivityId(ACTIVITY_ID);

    CategoryObject object = NoteCategoryPlugin.toCategoryObject(note);

    assertEquals(NoteCategoryPlugin.ACTIVITY_OBJECT_TYPE, object.getType());
    assertEquals(ACTIVITY_ID, object.getId());
  }

  @Test
  public void testToCategoryObjectResolvesToNoteWhenNotPosted() {
    Page note = new Page();
    note.setId("1");

    CategoryObject object = NoteCategoryPlugin.toCategoryObject(note);

    assertEquals(NoteCategoryPlugin.OBJECT_TYPE, object.getType());
    assertEquals("1", object.getId());
  }

  @Test
  public void testGetEntryItemResolvesCategoryIdsThroughCategoryLinkService() {
    // getEntryItem() must resolve category ids via the static getCategoryIds(note) helper
    // (which itself resolves the correct CategoryObject via toCategoryObject before querying
    // CategoryLinkService), not via a stale/unpopulated instance getter on Page.
    Page note = new Page();
    note.setId("1");
    note.setTitle("note title");
    when(noteService.getNoteById("1")).thenReturn(note);

    CategoryLinkService categoryLinkService = mock(CategoryLinkService.class);
    COMMONS_UTILS.when(() -> CommonsUtils.getService(CategoryLinkService.class)).thenReturn(categoryLinkService);
    List<Long> linkedCategoryIds = Arrays.asList(3L, 5L);
    when(categoryLinkService.getLinkedIds(NoteCategoryPlugin.toCategoryObject(note))).thenReturn(linkedCategoryIds);

    CategoryEntryItem item = noteCategoryPlugin.getEntryItem("1", "testuser");

    assertEquals(linkedCategoryIds, item.getCategoryIds());
  }

}
