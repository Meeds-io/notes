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
package org.exoplatform.notes.activity.processor;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

public class ActivityNotesProcessor extends BaseActivityProcessorPlugin {

  private static final Log LOG = ExoLogger.getLogger(ActivityNotesProcessor.class);

  private NoteService      noteService;

  public ActivityNotesProcessor(NoteService noteService, InitParams initParams) {
    super(initParams);
    this.noteService = noteService;
  }

  @Override
  public void processActivity(ExoSocialActivity activity) {
    if (activity.isComment()
        || activity.getType() == null
        || activity.getTemplateParams() == null
        || !activity.getTemplateParams().containsKey(Utils.PAGE_TYPE_KEY)
        || !activity.getTemplateParams().containsKey(Utils.PAGE_OWNER_KEY)
        || !activity.getTemplateParams().containsKey(Utils.PAGE_ID_KEY)) {
      return;
    }
    String pageOwnerType = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_TYPE_KEY);
    String pageOwner = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
    String pageId = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_ID_KEY);
    try {
      Page page = noteService.getNoteOfNoteBookByName(pageOwnerType, pageOwner, pageId);
      if (page != null) {
        activity.setMetadataObjectType(Utils.NOTES_METADATA_OBJECT_TYPE);
        activity.setMetadataObjectId(page.getId());
      }
    } catch (WikiException e) {
      LOG.warn("Error getting notes page {}/{}/{}", pageOwnerType, pageOwner, pageId, e);
    }
  }

}
