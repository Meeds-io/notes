package io.meeds.notes.notification.provider;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

import io.meeds.notes.notifications.plugin.MentionInNoteNotificationPlugin;
import io.meeds.notes.notifications.provider.MailTemplateProvider;
import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class MailTemplateProviderTest {

  private MailTemplateProvider.TemplateBuilder               templateBuilder;

  @Mock
  private IdentityManager                                    identityManager;

  @Mock
  private NotificationContext                                ctx;

  @Mock
  private NotificationInfo                                   notificationInfo;

  @Mock
  private Identity                                           receiver;

  @Mock
  private Profile                                            profile;

  private static final MockedStatic<TemplateUtils>           TEMPLATE_UTILS            = mockStatic(TemplateUtils.class);

  private static final MockedStatic<NotificationPluginUtils> NOTIFICATION_PLUGIN_UTILS =
                                                                                       mockStatic(NotificationPluginUtils.class);

  private static final MockedStatic<LinkProviderUtils>       LINK_PROVIDER_UTILS       = mockStatic(LinkProviderUtils.class);

  @Before
  public void setUp() {
    MailTemplateProvider mailTemplateProvider = new MailTemplateProvider(new InitParams(), identityManager);
    templateBuilder = mailTemplateProvider.new TemplateBuilder();
  }

  @After
  public void tearDown() {
    TEMPLATE_UTILS.close();
    NOTIFICATION_PLUGIN_UTILS.close();
    LINK_PROVIDER_UTILS.close();
  }

  @Test
  public void testMakeMessage() {
      
      when(ctx.getNotificationInfo()).thenReturn(notificationInfo);
      when(notificationInfo.getKey()).thenReturn(new PluginKey(MentionInNoteNotificationPlugin.ID));
      when(notificationInfo.getValueOwnerParameter(anyString())).thenReturn("testValue");
      when(notificationInfo.getTo()).thenReturn("receiverId");
      when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, "receiverId")).thenReturn(receiver);
      when(receiver.getProfile()).thenReturn(profile);
      when(receiver.getRemoteId()).thenReturn("receiverId");
      when(profile.getProperty(Profile.FIRST_NAME)).thenReturn("ReceiverFirstName");
      TEMPLATE_UTILS.when(() -> TemplateUtils.processGroovy(any(TemplateContext.class)))
              .thenReturn("you have been mentioned in a note");
      TEMPLATE_UTILS.when(() -> TemplateUtils.processSubject(any(TemplateContext.class)))
              .thenReturn("author mentioned you in the note test");
      NOTIFICATION_PLUGIN_UTILS.when(() -> NotificationPluginUtils.getLanguage(anyString())).thenReturn("en");
      LINK_PROVIDER_UTILS.when(() -> LinkProviderUtils.getBaseUrl()).thenReturn("baseUrl");
      LINK_PROVIDER_UTILS.when(() -> LinkProviderUtils.getRedirectUrl(anyString(), anyString()))
              .thenReturn("notifications/settings/redirect/url");
    MessageInfo messageInfo = templateBuilder.makeMessage(ctx);

    assertNotNull(messageInfo);
    assertTrue(messageInfo.getBody().equals("you have been mentioned in a note"));
    assertTrue(messageInfo.getSubject().equals("author mentioned you in the note test"));
  }
}
