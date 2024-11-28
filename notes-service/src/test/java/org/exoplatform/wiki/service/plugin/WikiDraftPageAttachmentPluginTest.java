/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.service.plugin;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class WikiDraftPageAttachmentPluginTest {

  @Mock
  private NoteService                                                 noteService;

  @Mock
  private SpaceService                                                spaceService;

  private static final MockedStatic<org.exoplatform.wiki.utils.Utils> UTILS = mockStatic(org.exoplatform.wiki.utils.Utils.class);

  private WikiDraftPageAttachmentPlugin                               plugin;

  @Before
  public void setUp() {
    plugin = new WikiDraftPageAttachmentPlugin(noteService, spaceService);
  }

  @AfterClass
  public static void afterRunBare() {
    UTILS.close();
  }

  @Test
  public void testGetObjectType() {
    Assert.assertEquals("wikiDraft", plugin.getObjectType());
  }

  @Test
  public void testHasAccessPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    DraftPage draftPage = mock(DraftPage.class);

    when(userIdentity.getUserId()).thenReturn("user123");
    when(noteService.getDraftNoteById("draft123", "user123")).thenReturn(draftPage);
    when(draftPage.isCanView()).thenReturn(true);

    assertTrue(plugin.hasAccessPermission(userIdentity, "draft123"));
  }

  @Test
  public void testHasEditPermission() throws Exception {
    org.exoplatform.services.security.Identity userIdentity = mock(org.exoplatform.services.security.Identity.class);
    DraftPage draftPage = mock(DraftPage.class);

    when(userIdentity.getUserId()).thenReturn("user123");
    when(noteService.getDraftNoteById("draft123", "user123")).thenReturn(draftPage);
    when(draftPage.isCanManage()).thenReturn(true);

    assertTrue(plugin.hasEditPermission(userIdentity, "draft123"));
  }

  @Test
  public void getSpaceId() throws Exception {

    UTILS.when(Utils::getCurrentUser).thenReturn("user123");
    DraftPage draftPage = mock(DraftPage.class);
    when(draftPage.getWikiOwner()).thenReturn("spaces/test");
    Space space = mock(Space.class);
    when(space.getId()).thenReturn("1");
    when(noteService.getDraftNoteById("draft123", "user123")).thenReturn(draftPage);
    when(spaceService.getSpaceByGroupId(draftPage.getWikiOwner())).thenReturn(space);
    assertEquals(1L, plugin.getSpaceId("draft123"));
  }

}
