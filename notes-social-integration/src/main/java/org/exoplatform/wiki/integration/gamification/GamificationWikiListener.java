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
package org.exoplatform.wiki.integration.gamification;

import static io.meeds.gamification.constant.GamificationConstant.*;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.NoteConstants;

@Asynchronous
public class GamificationWikiListener extends PageWikiListener {

  public static final String GAMIFICATION_WIKI_ADD_PAGE    = "addWikiPage";

  public static final String GAMIFICATION_WIKI_UPDATE_PAGE = "updateWikiPage";

  public static final String NOTES_OBJECT_TYPE             = "notes";

  protected IdentityManager  identityManager;

  protected ListenerService  listenerService;

  protected SpaceService      spaceService;

  public GamificationWikiListener(IdentityManager identityManager,
                                  ListenerService listenerService,
                                  SpaceService      spaceService) {
    this.identityManager = identityManager;
    this.listenerService = listenerService;
    this.spaceService = spaceService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) {
    if (NoteConstants.NOTE_HOME_NAME.equals(pageId)) {
      // catch the case of the Wiki Home added as it's created by the system,
      // not by users.
      return;
    }

    if (ConversationState.getCurrent() != null) {
      // Get the space's creator username
      String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();
      // Compute user id
      String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
      createGamificationRealization(actorId, actorId, GAMIFICATION_WIKI_ADD_PAGE, page.getId(), wikiOwner, wikiType);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    // Nothing to do
  }

  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    // Nothing to do
  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    // Nothing to do
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) {
    // Generate an activity only in the following cases
    if (page != null && wikiUpdateType != null
        && (wikiUpdateType.equals(PageUpdateType.ADD_PAGE) || wikiUpdateType.equals(PageUpdateType.EDIT_PAGE_CONTENT)
            || wikiUpdateType.equals(PageUpdateType.EDIT_PAGE_CONTENT_AND_TITLE))
        && ConversationState.getCurrent() != null) {

      // Get the space's creator username
      String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();

      // Compute user id
      String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();
      createGamificationRealization(actorId, actorId, GAMIFICATION_WIKI_UPDATE_PAGE, page.getId(), wikiOwner, wikiType);
    }
  }

  private void createGamificationRealization(String earnerIdentityId,
                                             String receiverId,
                                             String gamificationEventName,
                                             String pageId,
                                             String wikiOwner,
                                             String wikiType) {
    String eventDetails = "";
    if (StringUtils.isNotBlank(wikiOwner) && StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      Space space = spaceService.getSpaceByGroupId(wikiOwner);
      eventDetails = "{spaceId: " + space.getId() + "}";
    }
    Map<String, String> gam = new HashMap<>();
    try {
      gam.put(EVENT_NAME, gamificationEventName);
      gam.put(OBJECT_ID_PARAM, pageId);
      gam.put(OBJECT_TYPE_PARAM, NOTES_OBJECT_TYPE);
      gam.put(SENDER_ID, earnerIdentityId);
      gam.put(RECEIVER_ID, receiverId);
      gam.put(EVENT_DETAILS_PARAM, eventDetails);
      listenerService.broadcast(GENERIC_EVENT_NAME, gam, null);
    } catch (Exception e) {
      throw new IllegalStateException("Error triggering Gamification Listener Event: " + gam, e);
    }
  }


}
