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
package io.meeds.notes.plugin;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.social.category.model.CategoryObject;
import io.meeds.social.category.plugin.CategoryPlugin;
import io.meeds.social.category.service.CategoryLinkService;
import io.meeds.social.category.service.CategoryPluginService;

import jakarta.annotation.PostConstruct;

@Component
public class NoteCategoryPlugin implements CategoryPlugin {

  public static final String OBJECT_TYPE          = NotePermanentLinkPlugin.OBJECT_TYPE;

  public static final String ACTIVITY_OBJECT_TYPE = "activity";

  @Autowired
  private PortalContainer     container;

  @Autowired
  private NoteService         noteService;

  @PostConstruct
  public void init() {
    container.getComponentInstanceOfType(CategoryPluginService.class).addPlugin(this);
  }

  @Override
  public String getType() {
    return OBJECT_TYPE;
  }

  @Override
  public boolean canAccess(String objectId, String username) {
    return noteService.canViewNote(objectId, username);
  }

  @Override
  public boolean canEdit(String objectId, String username) {
    return noteService.canEditNote(objectId, username);
  }

  public static long getSpaceId(Page note) {
    if (note == null || note.getWikiOwner() == null) {
      return 0L;
    }
    Space space = CommonsUtils.getService(SpaceService.class).getSpaceByGroupId(note.getWikiOwner());
    return space == null ? 0L : Long.parseLong(space.getId());
  }

  /**
   * Resolves the category link object for a note. When the note is posted to
   * a feed (has an underlying Activity), categories are linked on the
   * Activity itself so that they stay consistent with the existing
   * publication flow. Otherwise, categories are linked directly on the note.
   */
  public static CategoryObject toCategoryObject(Page note) {
    if (note == null || note.getId() == null) {
      return null;
    }
    if (StringUtils.isNotBlank(note.getActivityId())) {
      return new CategoryObject(ACTIVITY_OBJECT_TYPE, note.getActivityId(), getSpaceId(note));
    }
    return new CategoryObject(OBJECT_TYPE, note.getId(), getSpaceId(note));
  }

  public static List<Long> getCategoryIds(Page note) {
    CategoryObject object = toCategoryObject(note);
    if (object == null) {
      return Collections.emptyList();
    }
    return CommonsUtils.getService(CategoryLinkService.class).getLinkedIds(object);
  }

}
