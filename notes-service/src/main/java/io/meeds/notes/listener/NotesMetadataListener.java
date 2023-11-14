/*
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
package io.meeds.notes.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;
import org.exoplatform.wiki.model.Page;

import java.util.Set;

public class NotesMetadataListener extends Listener<String, Page> {

  private final IdentityManager identityManager;

  private final SpaceService    spaceService;

  private final TagService      tagService;

  private final UserACL         userACL;

  private static final String   NOTES_METADATA_OBJECT_TYPE = "notes";

  public NotesMetadataListener(IdentityManager identityManager,
                               SpaceService spaceService,
                               TagService tagService,
                               UserACL userACL) {
    this.identityManager = identityManager;
    this.spaceService = spaceService;
    this.tagService = tagService;
    this.userACL = userACL;
  }

  @Override
  public void onEvent(Event<String, Page> event) {
    Page note = event.getData();
    String username = event.getSource();
    if (username == null) {
      username = note.getAuthor();
    }
    if (username == null) {
      username = userACL.getSuperUser();
    }
    long creatorId = getPosterId(username);
    long audienceId = getStreamOwnerId(note.getWikiOwner(), username);

    Set<TagName> tagNames = tagService.detectTagNames(note.getContent());
    tagService.saveTags(new TagObject(NOTES_METADATA_OBJECT_TYPE, note.getId(), note.getParentPageId()),
                        tagNames,
                        audienceId,
                        creatorId);
  }

  private long getStreamOwnerId(String spaceGroupId, String username) {
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    return space == null ? getPosterId(username)
                         : Long.parseLong(identityManager.getOrCreateSpaceIdentity(space.getPrettyName()).getId());
  }

  private long getPosterId(String username) {
    return StringUtils.isBlank(username) ? 0 : Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
  }
}
