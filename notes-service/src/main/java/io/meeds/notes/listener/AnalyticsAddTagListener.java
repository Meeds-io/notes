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
import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;

import java.util.Date;
import java.util.Set;

public class AnalyticsAddTagListener extends Listener<TagObject, Set<TagName>> {

  private final IdentityManager identityManager;

  private static final String   NOTES_METADATA_OBJECT_TYPE = "notes";

  public AnalyticsAddTagListener(IdentityManager identityManager) {
    this.identityManager = identityManager;
  }

  @Override
  public void onEvent(Event<TagObject, Set<TagName>> event) {
    TagObject tagObject = event.getSource();
    Set<TagName> tagNames = event.getData();
    if (tagObject.getType().equals(NOTES_METADATA_OBJECT_TYPE)) {
      int numberOfTags = tagNames.size();
      String currentUser = ConversationState.getCurrent().getIdentity().getUserId();
      long userId = Long.parseLong(identityManager.getOrCreateUserIdentity(currentUser).getId());
      StatisticData statisticData = new StatisticData();
      statisticData.setModule("portal");
      statisticData.setSubModule("ui");
      statisticData.setOperation("Add tag");
      statisticData.setTimestamp(new Date().getTime());
      statisticData.setUserId(userId);
      statisticData.addParameter("username", currentUser);
      statisticData.addParameter("dataType", StringUtils.lowerCase(tagObject.getType()));
      statisticData.addParameter("spaceId", tagObject.getSpaceId());

      for (int i = 0; i < numberOfTags; i++) {
        AnalyticsUtils.addStatisticData(statisticData);
      }
    }
  }
}
