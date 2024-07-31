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
package io.meeds.notes.notifications.provider;

import java.io.IOException;
import java.io.Writer;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.meeds.notes.notifications.plugin.MentionInNoteNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.gatein.common.text.EntityEncoder;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.DigestTemplate;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.model.Profile;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.notification.LinkProviderUtils;
import org.exoplatform.webui.utils.TimeConvertUtils;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = MentionInNoteNotificationPlugin.ID, template = "war:/notification/templates/mail/MentionInNoteNotificationPlugin.gtmpl") })
public class MailTemplateProvider extends TemplateProvider {
  protected static Log          log = ExoLogger.getLogger(MailTemplateProvider.class);

  private final IdentityManager identityManager;

  public MailTemplateProvider(InitParams initParams, IdentityManager identityManager) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(MentionInNoteNotificationPlugin.ID), new TemplateBuilder());
    this.identityManager = identityManager;
  }

  public class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    public MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String pluginId = notification.getKey().getId();

      String language = getLanguage(notification);
      String activityLink = notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.ACTIVITY_LINK.getKey());
      String authorProfileUrl = notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.AUTHOR_PROFILE_URL.getKey());
      String currentDomain = CommonsUtils.getCurrentDomain();
      activityLink = currentDomain + activityLink;
      authorProfileUrl = currentDomain + authorProfileUrl;
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);

      HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
      templateContext.put(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey(),
                          encoder.encode(notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey())));
      templateContext.put(MentionInNoteNotificationPlugin.NOTE_AUTHOR.getKey(),
                          encoder.encode(notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.NOTE_AUTHOR.getKey())));
      templateContext.put(MentionInNoteNotificationPlugin.CURRENT_USER.getKey(),
                          notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.CURRENT_USER.getKey()));
      templateContext.put(MentionInNoteNotificationPlugin.AUTHOR_AVATAR_URL.getKey(),
                          encoder.encode(notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.AUTHOR_AVATAR_URL.getKey())));
      templateContext.put(MentionInNoteNotificationPlugin.AUTHOR_PROFILE_URL.getKey(),
                          encoder.encode(authorProfileUrl));
      templateContext.put(MentionInNoteNotificationPlugin.ACTIVITY_LINK.getKey(),
                          encoder.encode(activityLink));

      templateContext.put("READ", Boolean.valueOf(notification.isRead()) ? "read" : "unread");
      templateContext.put("NOTIFICATION_ID", notification.getId());
      Calendar lastModified = Calendar.getInstance();
      lastModified.setTimeInMillis(notification.getLastModifiedDate());
      templateContext.put("LAST_UPDATED_TIME",
                          TimeConvertUtils.convertXTimeAgoByTimeServer(lastModified.getTime(),
                                                                       "EE, dd yyyy",
                                                                       new Locale(language),
                                                                       TimeConvertUtils.YEAR));
      // Receiver
      Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo());
      if (receiver == null || receiver.getRemoteId().equals(notification.getFrom())) {
        return null;
      }
      templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
      // Footer
      templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));
      templateContext.put("COMPANY_LINK", LinkProviderUtils.getBaseUrl());
      String subject = TemplateUtils.processSubject(templateContext);
      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      EntityEncoder encoder = HTMLEntityEncoder.getInstance();
      List<NotificationInfo> notifications = ctx.getNotificationInfos();
      NotificationInfo notificationInfo = notifications.get(0);
      try {
        String pluginId = notificationInfo.getKey().getId();
        if (pluginId.equals(MentionInNoteNotificationPlugin.ID)) {
          String mentionedIds = notificationInfo.getValueOwnerParameter(MentionInNoteNotificationPlugin.MENTIONED_IDS.getKey());
          String ids = mentionedIds.substring(1, mentionedIds.length() - 1);
          List<String> mentionedList = Stream.of(ids.split(",")).map(String::trim).collect(Collectors.toList());
          if (!mentionedList.contains(notificationInfo.getTo())) {
            return false;
          }
        }
        String language = getLanguage(notificationInfo);
        TemplateContext templateContext = new TemplateContext(pluginId, language);
        //
        Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, notificationInfo.getTo());
        templateContext.put("FIRST_NAME", encoder.encode(receiver.getProfile().getProperty(Profile.FIRST_NAME).toString()));
        templateContext.put("FOOTER_LINK", LinkProviderUtils.getRedirectUrl("notification_settings", receiver.getRemoteId()));

        writer.append(buildDigestMsg(notifications, templateContext));
      } catch (IOException e) {
        ctx.setException(e);
        return false;
      }
      return true;
    }

    protected String buildDigestMsg(List<NotificationInfo> notifications, TemplateContext templateContext) {
      StringBuilder sb = new StringBuilder();
      for (NotificationInfo notification : notifications) {
        templateContext.put(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey(),
                            notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey()));
        templateContext.put("USER", notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.NOTE_AUTHOR.getKey()));
        templateContext.digestType(DigestTemplate.ElementType.DIGEST_ONE.getValue());

        sb.append("<li style=\"margin: 0 0 13px 14px; font-size: 13px; line-height: 18px; font-family: HelveticaNeue, Helvetica, Arial, sans-serif;\">");
        String digester = TemplateUtils.processDigest(templateContext);
        sb.append(digester);
        sb.append("</div></li>");
      }
      return sb.toString();
    }
  }
}
