/*
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2022 Meeds Association contact@meeds.io
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
package org.exoplatform.notes.listener.analytics;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

public class NotesMetadataListener extends Listener<Long, MetadataItem> {

  private final NoteService  noteService;

  private final SpaceService spaceService;

  public NotesMetadataListener(NoteService noteService, SpaceService spaceService) {
    this.noteService = noteService;
    this.spaceService = spaceService;
  }

  @Override
  public void onEvent(Event<Long, MetadataItem> event) throws Exception {
    ConversationState conversationstate = ConversationState.getCurrent();
    Identity currentIdentity = conversationstate == null
        || conversationstate.getIdentity() == null ? null : conversationstate.getIdentity();
    MetadataItem metadataItem = event.getData();
    String objectType = event.getData().getObjectType();
    String objectId = metadataItem.getObjectId();
    if (StringUtils.equals(objectType, Utils.NOTES_METADATA_OBJECT_TYPE) && currentIdentity != null) {
      Page note = noteService.getNoteById(objectId);
      if (note != null) {
        StatisticData statisticData = new StatisticData();
        statisticData.setModule("portal");
        statisticData.setSubModule("ui");
        statisticData.setOperation("Bookmark");
        statisticData.addParameter("type", "Notes");
        if (StringUtils.isNotBlank(note.getWikiOwner())) {
          Space space = spaceService.getSpaceByGroupId(note.getWikiOwner());
          statisticData.setSpaceId(Long.parseLong(space.getId()));
        }
        statisticData.addParameter("wikiPageId", note.getId());
        statisticData.setUserId(AnalyticsUtils.getUserIdentityId(currentIdentity.getUserId()));

        AnalyticsUtils.addStatisticData(statisticData);
      }

    }
  }
}
