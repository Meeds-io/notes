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

import java.io.Writer;

import io.meeds.notes.notifications.plugin.MentionInNoteNotificationPlugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.annotation.TemplateConfig;
import org.exoplatform.commons.api.notification.annotation.TemplateConfigs;
import org.exoplatform.commons.api.notification.channel.template.AbstractTemplateBuilder;
import org.exoplatform.commons.api.notification.channel.template.TemplateProvider;
import org.exoplatform.commons.api.notification.model.MessageInfo;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.model.PluginKey;
import org.exoplatform.commons.api.notification.service.template.TemplateContext;
import org.exoplatform.commons.notification.template.TemplateUtils;
import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;

@TemplateConfigs(templates = {
    @TemplateConfig(pluginId = MentionInNoteNotificationPlugin.ID, template = "war:/notification/templates/push/MentionInNoteNotificationPlugin.gtmpl") })
public class PushTemplateProvider extends TemplateProvider {
  protected static Log          log = ExoLogger.getLogger(MailTemplateProvider.class);

  private final IdentityManager identityManager;

  public PushTemplateProvider(InitParams initParams, IdentityManager identityManager) {
    super(initParams);
    this.templateBuilders.put(PluginKey.key(MentionInNoteNotificationPlugin.ID), new TemplateBuilder());
    this.identityManager = identityManager;
  }

  protected class TemplateBuilder extends AbstractTemplateBuilder {
    @Override
    protected MessageInfo makeMessage(NotificationContext ctx) {
      NotificationInfo notification = ctx.getNotificationInfo();
      String pluginId = notification.getKey().getId();

      String language = getLanguage(notification);
      TemplateContext templateContext = TemplateContext.newChannelInstance(getChannelKey(), pluginId, language);

      HTMLEntityEncoder encoder = HTMLEntityEncoder.getInstance();
      templateContext.put(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey(),
                          encoder.encode(notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.NOTE_TITLE.getKey())));
      templateContext.put(MentionInNoteNotificationPlugin.CURRENT_USER.getKey(),
              encoder.encode(notification.getValueOwnerParameter(MentionInNoteNotificationPlugin.CURRENT_USER.getKey())));
      // Receiver
      Identity receiver = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, notification.getTo());
      if (receiver == null || receiver.getRemoteId().equals(notification.getFrom())) {
        return null;
      }
      String subject = TemplateUtils.processSubject(templateContext);
      String body = TemplateUtils.processGroovy(templateContext);
      // binding the exception throws by processing template
      ctx.setException(templateContext.getException());
      MessageInfo messageInfo = new MessageInfo();
      return messageInfo.subject(subject).body(body).end();
    }

    @Override
    protected boolean makeDigest(NotificationContext ctx, Writer writer) {
      return false;
    }

  }
}
