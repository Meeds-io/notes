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

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

public class WikiPageAttachmentPlugin extends AttachmentPlugin {

  public static final String OBJECT_TYPE = "wikiPage";

  private final NoteService  noteService;

  private final SpaceService spaceService;

  public WikiPageAttachmentPlugin(NoteService noteService, SpaceService spaceService) {
    this.noteService = noteService;
    this.spaceService = spaceService;
  }

  @Override
  public String getObjectType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean hasAccessPermission(Identity identity, String noteId) {
    try {
      Page note = noteService.getNoteById(noteId, identity);
      return note != null && note.isCanView();
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public boolean hasEditPermission(Identity identity, String noteId) throws ObjectNotFoundException {
    try {
      Page note = noteService.getNoteById(noteId, identity);
      return note != null && note.isCanManage();
    } catch (Exception e) {
      return false;
    }
  }

  @Override
  public long getAudienceId(String s) throws ObjectNotFoundException {
    return 0;
  }

  @Override
  public long getSpaceId(String noteId) throws ObjectNotFoundException {
    try {
      Page note = noteService.getNoteById(noteId);
      return Long.parseLong(spaceService.getSpaceByGroupId(note.getWikiOwner()).getId());
    } catch (Exception exception) {
      return 0;
    }
  }
}
