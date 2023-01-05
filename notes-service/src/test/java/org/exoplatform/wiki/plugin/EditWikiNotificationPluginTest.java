package org.exoplatform.wiki.plugin;

import static org.mockito.ArgumentMatchers.any;
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
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.idgenerator.IDGeneratorService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.plugin.EditWikiNotificationPlugin;

@RunWith(MockitoJUnitRunner.Silent.class)
public class EditWikiNotificationPluginTest extends AbstractKernelTest {

  @Mock
  private InitParams                        initParams;

  private MockedStatic<CommonsUtils>        commonsUtils;

  private MockedStatic<ExoContainerContext> containerContext;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    PortalContainer container = getContainer();
    commonsUtils = mockStatic(CommonsUtils.class);
    containerContext = mockStatic(ExoContainerContext.class);

    commonsUtils.when(() -> CommonsUtils.getService(any())).thenAnswer(invocation -> {
      Class<?> clazz = invocation.getArgument(0);
      if (clazz.equals(NotificationService.class) || clazz.equals(NotificationCompletionService.class)) {
        return null;
      } else {
        return container.getComponentInstanceOfType(clazz);
      }
    });
    containerContext.when(() -> ExoContainerContext.getService(any())).thenAnswer(invocation -> {
      Class<?> clazz = invocation.getArgument(0);
      if (clazz.equals(IDGeneratorService.class)) {
        return new IDGeneratorService() {
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
        };
      } else {
        return container.getComponentInstanceOfType(clazz);
      }
    });
  }

  @Override
  @After
  public void tearDown() throws Exception {
    commonsUtils.close();
    containerContext.close();
    super.tearDown();
  }

  @Test
  public void shouldMakeNotificationWikiContext() {
    // Given
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
