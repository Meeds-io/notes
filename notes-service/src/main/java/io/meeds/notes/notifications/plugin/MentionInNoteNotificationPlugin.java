/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
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
package io.meeds.notes.notifications.plugin;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.notification.Utils;

public class MentionInNoteNotificationPlugin extends BaseNotificationPlugin {

  public final static String                  ID                 = "MentionInNoteNotificationPlugin";

  private static final Log                    LOG                = ExoLogger.getLogger(MentionInNoteNotificationPlugin.class);

  public static final ArgumentLiteral<String> CURRENT_USER       = new ArgumentLiteral<>(String.class, "CURRENT_USER");

  public static final ArgumentLiteral<String> NOTE_AUTHOR        = new ArgumentLiteral<>(String.class, "NOTE_AUTHOR");

  public static final ArgumentLiteral<String> AUTHOR_AVATAR_URL  = new ArgumentLiteral<>(String.class, "AUTHOR_AVATAR_URL");

  public static final ArgumentLiteral<String> AUTHOR_PROFILE_URL = new ArgumentLiteral<>(String.class, "AUTHOR_PROFILE_URL");

  public static final ArgumentLiteral<String> ACTIVITY_LINK      = new ArgumentLiteral<>(String.class, "ACTIVITY_LINK");

  public static final ArgumentLiteral<Set>    MENTIONED_IDS      = new ArgumentLiteral<Set>(Set.class, "MENTIONED_IDS");

  public static final ArgumentLiteral<String> NOTE_URL           = new ArgumentLiteral<>(String.class, "NOTE_URL");

  public static final ArgumentLiteral<String> NOTE_TITLE         = new ArgumentLiteral<>(String.class, "NOTE_TITLE");

  public static final ArgumentLiteral<String> SPACE_ID           = new ArgumentLiteral<>(String.class, "SPACE_ID");

  public MentionInNoteNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  public NotificationInfo makeNotification(NotificationContext ctx) {
    String currentUserName = ctx.value(CURRENT_USER);
    String currentUserFullName = currentUserName;
    try {
      currentUserFullName = getUserFullName(currentUserName);
    } catch (Exception e) {
      LOG.error("An error occured when trying to retreive a user with username " + currentUserName + " " + e.getMessage(), e);
    }
    List<String> mentionedIds = new ArrayList<>(ctx.value(MENTIONED_IDS));
    String spaceId = ctx.value(SPACE_ID);
    Set<String> receivers = new HashSet<>();
    String[] mentionnedIdArray = new String[mentionedIds.size()];
    Utils.sendToMentioners(receivers, mentionedIds.toArray(mentionnedIdArray), currentUserName, spaceId);
    return NotificationInfo.instance()
                           .setFrom(currentUserName)
                           .to(new ArrayList<>(receivers))
                           .key(getKey())
                           .with(NOTE_URL.getKey(), ctx.value(NOTE_URL))
                           .with(NOTE_TITLE.getKey(), ctx.value(NOTE_TITLE))
                           .with(NOTE_AUTHOR.getKey(), ctx.value(NOTE_AUTHOR))
                           .with(AUTHOR_AVATAR_URL.getKey(), ctx.value(AUTHOR_AVATAR_URL))
                           .with(AUTHOR_PROFILE_URL.getKey(), ctx.value(AUTHOR_PROFILE_URL))
                           .with(ACTIVITY_LINK.getKey(), ctx.value(ACTIVITY_LINK))
                           .with(MENTIONED_IDS.getKey(), String.valueOf(mentionedIds))
                           .with(CURRENT_USER.getKey(), currentUserFullName)
                           .end();
  }

  private String getUserFullName(String userName) throws Exception {
    OrganizationService organizationService = CommonsUtils.getService(OrganizationService.class);
    UserHandler userHandler = organizationService.getUserHandler();
    User user = userHandler.findUserByName(userName);
    if (user == null) {
      throw new Exception("An error occured when trying to retreive a user with username " + userName);
    }
    return user.getFullName();
  }
}
