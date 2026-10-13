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
package io.meeds.notes.mcp;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.mcp.server.plugin.SpaceScopeResolver;

/**
 * Tells which space a note belongs to, for the space scope of the notes
 * tools: the space whose group owns the notebook the note is in. A note of a
 * personal (user) or portal notebook, a notebook owned by a group outside the
 * spaces, a space that no longer exists, a missing note and an id that isn't
 * one belong to no space. Read as the system, for the space scope decision
 * only.
 */
@Service("noteSpaceScopeResolver")
@Profile("mcp-server")
public class NoteSpaceScopeResolver implements SpaceScopeResolver {

  /** The logger. */
  private static final Log    LOG           = ExoLogger.getLogger(NoteSpaceScopeResolver.class);

  /** The name the notes tools' {@code space_scope.resolver} gives. */
  public static final String NAME          = "note";

  /** The prefix of the group id of a space. */
  private static final String SPACES_PREFIX = "/spaces/";

  /** Reads the note. */
  @Autowired
  private NoteService         noteService;

  /** Reads the space of a group. */
  @Autowired
  private SpaceService        spaceService;

  /**
   * @return {@value #NAME}
   */
  @Override
  public String getName() {
    return NAME;
  }

  /**
   * @param objectId the note id
   * @return the id of the space whose notebook holds the note, or null for a
   *         note of a personal or portal notebook, a missing note or space,
   *         an id that isn't one, or a lookup that fails
   */
  @Override
  public Long spaceOf(String objectId) {
    if (!StringUtils.isNumeric(objectId)) {
      return null;
    }
    try {
      Page note = noteService.getNoteById(objectId);
      if (note == null || !WikiType.GROUP.isSame(note.getWikiType())) {
        return null;
      }
      String groupId = note.getWikiOwner();
      if (groupId == null || !groupId.startsWith(SPACES_PREFIX)) {
        return null;
      }
      Space space = spaceService.getSpaceByGroupId(groupId);
      return space != null && StringUtils.isNumeric(space.getId()) ? Long.valueOf(space.getId()) : null;
    } catch (Exception e) {
      LOG.debug("The space of note '{}' couldn't be read", objectId, e);
      return null;
    }
  }

}
