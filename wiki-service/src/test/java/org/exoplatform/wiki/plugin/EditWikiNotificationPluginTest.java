package org.exoplatform.wiki.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.NotificationCompletionService;
import org.exoplatform.commons.api.notification.service.storage.NotificationService;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.plugin.EditWikiNotificationPlugin;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.*;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore("javax.management.*")
public class EditWikiNotificationPluginTest {

  @Mock
  private InitParams initParams;

  @PrepareForTest({ PluginKey.class, CommonsUtils.class, ExoContainerContext.class })
  @Test
  public void shouldMakeNotificationWikiContext() {
    // Given
    PowerMockito.mockStatic(CommonsUtils.class);
    when(CommonsUtils.getService(NotificationService.class)).thenReturn(null);
    when(CommonsUtils.getService(NotificationCompletionService.class)).thenReturn(null);
    PowerMockito.mockStatic(ExoContainerContext.class);
    when(ExoContainerContext.getService(IDGeneratorService.class)).thenReturn(new IDGeneratorService() {
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

    Page page = new Page();
    page.setTitle("title");
    page.setAuthor("root");
    page.setId("id123");
    Set<String> recievers = new HashSet<>();
    recievers.add("jean");

    EditWikiNotificationPlugin editWikiNotificationPlugin = new EditWikiNotificationPlugin(initParams);
    NotificationContext ctx = NotificationContextImpl.cloneInstance()
                                                     .append(NotificationsUtils.WIKI_PAGE_NAME, "title")
                                                     .append(NotificationsUtils.WIKI_EDITOR, page.getAuthor())
                                                     .append(NotificationsUtils.WIKI_URL, "/portal/spaceTest/WikiPage")
                                                     .append(NotificationsUtils.CONTENT_CHANGE, "Changes")
                                                     .append(NotificationsUtils.WATCHERS, recievers)
                                                     .append(NotificationsUtils.PAGE, page);

    // When
    NotificationInfo notificationInfo = editWikiNotificationPlugin.makeNotification(ctx);

    // Then
    Assert.assertEquals("/portal/spaceTest/WikiPage", notificationInfo.getValueOwnerParameter("wiki_url"));
    Assert.assertEquals("title", notificationInfo.getValueOwnerParameter("wiki_page_name"));
    Assert.assertEquals("root", notificationInfo.getValueOwnerParameter("wiki_editor"));
    Assert.assertEquals("Changes", notificationInfo.getValueOwnerParameter("content_change"));
  }
}
