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
package io.meeds.notes.notification.plugin;

import io.meeds.notes.notifications.plugin.MentionInNoteNotificationPlugin;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.utils.Utils;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MentionInNoteNotificationPluginTest {

  @Mock
  private UserHandler                                    userhandler;

  @Mock
  private InitParams                                     initParams;

  @Mock
  private OrganizationService                            orgService;

  @Mock
  private SpaceService                                   spaceService;

  @Mock
  private PortalContainer                                portalContainer;

  @Mock
  private IdentityManager                                identityManager;

  private static final MockedStatic<CommonsUtils>        COMMONS_UTILS         = mockStatic(CommonsUtils.class);

  private static final MockedStatic<Utils>               SOCIAL_UTILS          = mockStatic(Utils.class);

  private static final MockedStatic<PluginKey>           PLUGIN_KEY            = mockStatic(PluginKey.class);

  private static final MockedStatic<ExoContainerContext> EXO_CONTAINER_CONTEXT = mockStatic(ExoContainerContext.class);

  private static final MockedStatic<PortalContainer>     PORTAL_CONTAINER      = mockStatic(PortalContainer.class);

  @AfterClass
  public static void afterRunBare() throws Exception { // NOSONAR
    COMMONS_UTILS.close();
    SOCIAL_UTILS.close();
    PLUGIN_KEY.close();
    EXO_CONTAINER_CONTEXT.close();
    PORTAL_CONTAINER.close();
  }

  @Test
    public void testShouldMakeNotificationForMentionInNoteContext() throws Exception {
        // Given
        when(orgService.getUserHandler()).thenReturn(userhandler);
        MentionInNoteNotificationPlugin notePlugin = new MentionInNoteNotificationPlugin(initParams);
        Set<String> mentionedIds = new HashSet<>(Collections.singleton("1"));

        COMMONS_UTILS.when(() -> CommonsUtils.getService(NotificationService.class)).thenReturn(null);
        COMMONS_UTILS.when(() -> CommonsUtils.getService(NotificationCompletionService.class)).thenReturn(null);
        NotificationContext ctx =
                NotificationContextImpl.cloneInstance()
                        .append(MentionInNoteNotificationPlugin.NOTE_TITLE, "title")
                        .append(MentionInNoteNotificationPlugin.NOTE_AUTHOR, "root")
                        .append(MentionInNoteNotificationPlugin.CURRENT_USER, "root")
                        .append(MentionInNoteNotificationPlugin.SPACE_ID, "1")
                        .append(MentionInNoteNotificationPlugin.MENTIONED_IDS, mentionedIds)
                        .append(MentionInNoteNotificationPlugin.AUTHOR_AVATAR_URL,
                                "http://localhost:8080/portal/rest/v1/social/users/default-image/avatar")
                        .append(MentionInNoteNotificationPlugin.ACTIVITY_LINK,
                                "http://localhost:8080/portal/g/:spaces:space_test/space_test/notes/1");

        User currentUser = mock(User.class);
        when(userhandler.findUserByName("root")).thenReturn(currentUser);
        when(currentUser.getFullName()).thenReturn("root root");

        Space space = new Space();
        space.setId("1");
        space.setGroupId("space_test");

        COMMONS_UTILS.when(() -> CommonsUtils.getService(OrganizationService.class)).thenReturn(orgService);
        PORTAL_CONTAINER.when(PortalContainer::getInstance).thenReturn(portalContainer);
        when(portalContainer.getComponentInstanceOfType(SpaceService.class)).thenReturn(spaceService);
        when(portalContainer.getComponentInstanceOfType(IdentityManager.class)).thenReturn(identityManager);
        Identity identity = mock(Identity.class);
        when(identity.getRemoteId()).thenReturn("receiver");
        when(identityManager.getIdentity(anyString(), anyBoolean())).thenReturn(identity);
        when(spaceService.isMember(any(Space.class), anyString())).thenReturn(true);
        when(spaceService.getSpaceById(anyString())).thenReturn(space);

        // When
        NotificationInfo notificationInfo = notePlugin.makeNotification(ctx);

        // Then
        assertEquals("root", notificationInfo.getFrom());
        assertEquals("", notificationInfo.getTitle());
        assertEquals("title", notificationInfo.getValueOwnerParameter("NOTE_TITLE"));
        assertEquals("root", notificationInfo.getValueOwnerParameter("NOTE_AUTHOR"));
        assertEquals("http://localhost:8080/portal/rest/v1/social/users/default-image/avatar",
                notificationInfo.getValueOwnerParameter("AUTHOR_AVATAR_URL"));
        assertEquals("http://localhost:8080/portal/g/:spaces:space_test/space_test/notes/1",
                notificationInfo.getValueOwnerParameter("ACTIVITY_LINK"));
    }

}
