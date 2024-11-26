/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.service.plugin;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentPlugin;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

public class WikiDraftPageAttachmentPlugin  extends AttachmentPlugin {

    private final NoteService noteService;

    private final SpaceService spaceService;

    public static final String OBJECT_TYPE = "wikiDraft";

    public WikiDraftPageAttachmentPlugin(NoteService noteService, SpaceService spaceService) {
        this.noteService = noteService;
        this.spaceService = spaceService;
    }

    @Override
    public String getObjectType() {
        return OBJECT_TYPE;
    }

    @Override
    public boolean hasAccessPermission(Identity identity, String draftId) {
        try {
            DraftPage draftPage = noteService.getDraftNoteById(draftId, identity.getUserId());
            return draftPage != null && draftPage.isCanView();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public boolean hasEditPermission(Identity identity, String draftId) throws ObjectNotFoundException {
        try {
            DraftPage draftPage = noteService.getDraftNoteById(draftId, identity.getUserId());
            return draftPage != null && draftPage.isCanManage();
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public long getAudienceId(String s) throws ObjectNotFoundException {
        return 0;
    }

    @Override
    public long getSpaceId(String draftId) throws ObjectNotFoundException {
        try {
            String username = Utils.getCurrentUser();
            DraftPage draftPage = noteService.getDraftNoteById(draftId, username);
            return Long.parseLong(spaceService.getSpaceByGroupId(draftPage.getWikiOwner()).getId());
        } catch (Exception exception) {
            return 0;
        }
    }
}
