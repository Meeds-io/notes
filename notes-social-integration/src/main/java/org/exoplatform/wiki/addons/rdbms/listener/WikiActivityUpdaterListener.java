/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.addons.rdbms.listener;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

public class WikiActivityUpdaterListener extends Listener<ExoSocialActivity, String> {
  private static final Log LOG = ExoLogger.getLogger(WikiActivityUpdaterListener.class);
  private final NoteService service;

  /**
   * Do not remove notes service on constructor, puts the service to
   * the constructor to make sure the service already created before using.
   * 
   * @param noteService
   *
   */
  public WikiActivityUpdaterListener(NoteService noteService) {
    this.service = noteService;
  }

  @Override
  public void onEvent(Event<ExoSocialActivity, String> event) throws Exception {
    ExoSocialActivity activity = event.getSource();
    if (WikiSpaceActivityPublisher.WIKI_APP_ID.equals(activity.getType())) {
      String newActivityId = event.getData();
      if (!activity.isComment()) {
        LOG.info(String.format("Migration the wiki activity '%s' with new id:: %s", activity.getTitle(), newActivityId));
        String pageId = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_ID_KEY);
        if (pageId == null) return;
        String pageType = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_TYPE_KEY);
        String pageOwner = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
        //
        Page page = service.getNoteByRootPermission(pageType, pageOwner, pageId);
        if (page != null) {
          page.setActivityId(newActivityId);
          service.updateNote(page);
        } else {
          LOG.warn("Cannot update the activity id of the page " + pageId + " because the page can not be retrieved");
        }
      }
    }
  }
}