/*
 * Copyright (C) 2015 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.exoplatform.wiki.listener;

import static org.mockito.Mockito.mockStatic;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.plugin.EditWikiNotificationPlugin;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TestEditWikiNotificationListener extends AbstractKernelTest { // NOSONAR

  @Mock
  private InitParams                        initParams;

  private MockedStatic<CommonsUtils>        commonsUtils;

  private MockedStatic<ExoContainerContext> containerContext;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    commonsUtils = mockStatic(CommonsUtils.class);
    containerContext = mockStatic(ExoContainerContext.class);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    commonsUtils.close();
    containerContext.close();
    super.tearDown();
  }

  @Test
  public void testSendNotificationToWatchersOfWiki() {
    // Given
    commonsUtils.when(() -> CommonsUtils.getService(NotificationService.class)).thenReturn(null);
    commonsUtils.when(() -> CommonsUtils.getService(NotificationCompletionService.class)).thenReturn(null);
    containerContext.when(() -> ExoContainerContext.getService(IDGeneratorService.class)).thenReturn(new IDGeneratorService() {

      @Override
      public String generateStringID(Object o) {
        return String.valueOf(generateLongID(o));
      }

      @Override
      public long generateLongID(Object o) {
        return (long) (Objects.hashCode(o) * Math.random());
      }

      @Override
      public Serializable generateID(Object o) {
        return generateLongID(o);
      }

      @Override
      public int generatIntegerID(Object o) {
        return (int) generateLongID(o);
      }
    });

    Set<String> recievers = new HashSet<>();
    recievers.add("Jean");
    recievers.add("John");

    EditWikiNotificationPlugin editWikiNotificationPlugin = new EditWikiNotificationPlugin(initParams);
    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(NotificationsUtils.WIKI_PAGE_NAME, "title")
                                                     .append(NotificationsUtils.WIKI_EDITOR, "root")
                                                     .append(NotificationsUtils.WATCHERS, recievers);

    // When
    NotificationInfo notificationInfo = editWikiNotificationPlugin.makeNotification(ctx);

    // Then
    Assert.assertEquals(2, notificationInfo.getSendToUserIds().size());
    Assert.assertTrue(notificationInfo.getSendToUserIds().contains("Jean"));
    Assert.assertTrue(notificationInfo.getSendToUserIds().contains("John"));
    Assert.assertFalse(notificationInfo.getSendToUserIds().contains("root"));
  }

}
