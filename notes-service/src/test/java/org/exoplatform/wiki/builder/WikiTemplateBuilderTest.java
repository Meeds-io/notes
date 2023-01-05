package org.exoplatform.wiki.builder;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.Locale;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.ChannelKey;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.plugin.NotificationPluginUtils;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.impl.NotificationContextImpl;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.webui.utils.TimeConvertUtils;
import org.exoplatform.wiki.notification.Utils.NotificationsUtils;
import org.exoplatform.wiki.notification.builder.WikiTemplateBuilder;
import org.exoplatform.wiki.notification.provider.MailTemplateProvider;

@RunWith(MockitoJUnitRunner.Silent.class)
public class WikiTemplateBuilderTest extends AbstractKernelTest {

  @Mock
  private IdentityManager                       identityManager;

  @Mock
  private InitParams                            initParams;

  private MockedStatic<CommonsUtils>            commonsUtils;

  private MockedStatic<PluginKey>               pluginKey;

  private MockedStatic<NotificationContextImpl> notificationContextImpl;

  private MockedStatic<NotificationPluginUtils> notificationPluginUtils;

  private MockedStatic<TemplateContext>         templateContext;

  private MockedStatic<HTMLEntityEncoder>       htmlEntityEncoder;

  private MockedStatic<TimeConvertUtils>        timeConvertUtils;

  private MockedStatic<TemplateUtils>           templateUtils;

  @Override
  @Before
  public void setUp() throws Exception {
    super.setUp();
    commonsUtils = mockStatic(CommonsUtils.class);
    pluginKey = mockStatic(PluginKey.class);
    notificationContextImpl = mockStatic(NotificationContextImpl.class);
    notificationPluginUtils = mockStatic(NotificationPluginUtils.class);
    templateContext = mockStatic(TemplateContext.class);
    htmlEntityEncoder = mockStatic(HTMLEntityEncoder.class);
    timeConvertUtils = mockStatic(TimeConvertUtils.class);
    templateUtils = mockStatic(TemplateUtils.class);
  }

  @Override
  @After
  public void tearDown() throws Exception {
    commonsUtils.close();
    pluginKey.close();
    notificationContextImpl.close();
    notificationPluginUtils.close();
    templateContext.close();
    htmlEntityEncoder.close();
    timeConvertUtils.close();
    templateUtils.close();
    super.tearDown();
  }

  @SuppressWarnings("deprecation")
  @Test
  public void shoudIntantiateTemplate() {
    // Given
    commonsUtils.when(() -> CommonsUtils.getService(any())).thenReturn(null);
    PluginKey plugin = mock(PluginKey.class);
    pluginKey.when(() -> PluginKey.key(NotificationsUtils.EDIT_WIKI_NOTIFICATION_ID)).thenReturn(plugin);
    ValueParam channelParam = new ValueParam();
    channelParam.setName(TemplateProvider.CHANNEL_ID_KEY);
    channelParam.setValue("MAIL_CHANNEL");
    when(initParams.getValueParam(TemplateProvider.CHANNEL_ID_KEY)).thenReturn(channelParam);
    MailTemplateProvider mailTemplate = new MailTemplateProvider(initParams);
    WikiTemplateBuilder wikiTemplateBuilder = (WikiTemplateBuilder) mailTemplate.getTemplateBuilder().get(plugin);
    NotificationContext ctx = mock(NotificationContext.class);
    notificationContextImpl.when(() -> NotificationContextImpl.cloneInstance()).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_URL, "/portal/spaceTest/WikiPage")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_PAGE_NAME, "Test Page")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.CONTENT_CHANGE, "changes for test")).thenReturn(ctx);
    when(ctx.append(NotificationsUtils.WIKI_EDITOR, "root")).thenReturn(ctx);
    NotificationInfo notification = mock(NotificationInfo.class);
    when(ctx.getNotificationInfo()).thenReturn(notification);
    when(notification.getKey()).thenReturn(plugin);
    when(plugin.getId()).thenReturn("EditWikiNotificationPlugin");
    notificationPluginUtils.when(() -> NotificationPluginUtils.getLanguage(anyString())).thenReturn("en");
    TemplateContext context = mock(TemplateContext.class);
    ChannelKey key = mock(ChannelKey.class);
    MailTemplateProvider mailTemplateSpy = Mockito.spy(mailTemplate);
    when(mailTemplateSpy.getChannelKey()).thenReturn(key);
    templateContext.when(() -> TemplateContext.newChannelInstance(any(),
                                                                  eq("EditWikiNotificationPlugin"),
                                                                  eq("en")))
                   .thenReturn(context);

    when(notification.getValueOwnerParameter("wiki_url")).thenReturn("/portal/spaceTest/WikiPage");
    when(notification.getValueOwnerParameter("wiki_page_name")).thenReturn("Test Page");
    when(notification.getValueOwnerParameter("content_change")).thenReturn("changes for test");
    when(notification.getValueOwnerParameter("wiki_editor")).thenReturn("root");

    HTMLEntityEncoder encoder = mock(HTMLEntityEncoder.class);
    htmlEntityEncoder.when(() -> HTMLEntityEncoder.getInstance()).thenReturn(encoder);
    when(encoder.encode("title")).thenReturn("title");
    when(encoder.encode("jean")).thenReturn("jean");
    when(encoder.encode("root")).thenReturn("root");
    when(encoder.encode("/portal/spaceTest/WikiPage")).thenReturn("/portal/spaceTest/WikiPage");
    when(encoder.encode("changes for test")).thenReturn("changes for test");
    when(notification.getValueOwnerParameter("read")).thenReturn("true");
    when(notification.getId()).thenReturn("NotifId123");

    Date date = new Date();
    Long time = date.getTime();
    when(notification.getLastModifiedDate()).thenReturn(time);
    timeConvertUtils.when(() -> TimeConvertUtils.convertXTimeAgoByTimeServer(date,
                                                                             "EE, dd yyyy",
                                                                             new Locale("en"),
                                                                             TimeConvertUtils.YEAR))
                    .thenReturn("9-09-2020");

    when(notification.getTo()).thenReturn("jean");
    Identity receiverIdentity = new Identity(OrganizationIdentityProvider.NAME, "jean");
    receiverIdentity.setRemoteId("jean");
    Profile profile = new Profile(receiverIdentity);
    receiverIdentity.setProfile(profile);
    profile.setProperty(Profile.FIRST_NAME, "jean");
    when(identityManager.getOrCreateIdentity(eq(OrganizationIdentityProvider.NAME),
                                             eq("jean"),
                                             anyBoolean())).thenReturn(receiverIdentity);
    when(encoder.encode("jean")).thenReturn("jean");

    templateUtils.when(() -> TemplateUtils.processSubject(context)).thenReturn("root edited your wiki");
    templateUtils.when(() -> TemplateUtils.processGroovy(context)).thenReturn("root edit your wiki \"title\" ");
    when(context.getException()).thenReturn(null);

    // When
    MessageInfo messageInfo = wikiTemplateBuilder.makeMessage(ctx);

    // Then
    assertNotNull(messageInfo);
    assertEquals("root edit your wiki \"title\" ", messageInfo.getBody());
    assertEquals("root edited your wiki", messageInfo.getSubject());
  }
}
