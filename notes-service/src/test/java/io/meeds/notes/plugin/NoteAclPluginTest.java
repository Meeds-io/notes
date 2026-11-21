/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.portal.plugin.AclPlugin;

@RunWith(MockitoJUnitRunner.class)
public class NoteAclPluginTest {

  @Mock
  private NoteService   noteService;

  @InjectMocks
  private NoteAclPlugin plugin;

  /**
   * A note's readers and editors keep their rights while it lives, and lose
   * them once it is deleted. Mutation: drop the deleted check, and this test
   * fails.
   */
  @Test
  public void aDeletedNoteGrantsNoRight() throws Exception {
    Identity reader = mock(Identity.class);
    Page note = new Page();
    note.setId("5");
    when(noteService.getNoteById("5")).thenReturn(note);
    when(noteService.hasPermissionOnPage(note, PermissionType.VIEWPAGE, reader)).thenReturn(true);
    when(noteService.hasPermissionOnPage(note, PermissionType.EDITPAGE, reader)).thenReturn(true);

    assertTrue(plugin.hasPermission("5", AclPlugin.VIEW_PERMISSION_TYPE, reader));
    assertTrue(plugin.hasPermission("5", AclPlugin.EDIT_PERMISSION_TYPE, reader));

    note.setDeleted(true);
    assertFalse(plugin.hasPermission("5", AclPlugin.VIEW_PERMISSION_TYPE, reader));
    assertFalse(plugin.hasPermission("5", AclPlugin.EDIT_PERMISSION_TYPE, reader));
  }

  @Test
  public void aMissingNoteOrAnUnknownRightGrantsNothing() throws Exception {
    Identity reader = mock(Identity.class);
    when(noteService.getNoteById("6")).thenReturn(new Page());

    assertFalse(plugin.hasPermission("7", AclPlugin.VIEW_PERMISSION_TYPE, reader));
    assertFalse(plugin.hasPermission("6", "unknown", reader));
    verify(noteService, never()).hasPermissionOnPage(any(Page.class), any(PermissionType.class), any(Identity.class));
  }
}
