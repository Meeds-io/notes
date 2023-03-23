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

import org.exoplatform.addons.gamification.entities.domain.effective.GamificationActionsHistory;
import org.exoplatform.addons.gamification.service.configuration.RuleService;
import org.exoplatform.addons.gamification.service.effective.GamificationService;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.NoteConstants;

@Asynchronous
public class GamificationWikiListener extends PageWikiListener {

  private static final String   GAMIFICATION_WIKI_ADD_PAGE    = "addWikiPage";

  private static final String   GAMIFICATION_WIKI_UPDATE_PAGE = "updateWikiPage";
  
  private static final String   NOTES_OBJECT_TYPE             = "notes";

  protected RuleService         ruleService;

  protected IdentityManager     identityManager;

  protected GamificationService gamificationService;

  public GamificationWikiListener(RuleService ruleService,
                                  IdentityManager identityManager,
                                  GamificationService gamificationService) {

    this.ruleService = ruleService;
    this.identityManager = identityManager;
    this.gamificationService = gamificationService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) {
    if (NoteConstants.NOTE_HOME_NAME.equals(pageId)) {
      // catch the case of the Wiki Home added as it's created by the system,
      // not by users.
      return;
    }

    GamificationActionsHistory aHistory = null;
    if (ConversationState.getCurrent() != null) {

      // Get the space's creator username
      String actorUsername = ConversationState.getCurrent().getIdentity().getUserId();

      // Compute user id
      String actorId = identityManager.getOrCreateUserIdentity(actorUsername).getId();

      gamificationService.createHistory(GAMIFICATION_WIKI_ADD_PAGE, actorId, actorId, page.getId(), NOTES_OBJECT_TYPE);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }


  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

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

      gamificationService.createHistory(GAMIFICATION_WIKI_UPDATE_PAGE, actorId, actorId, page.getId(), NOTES_OBJECT_TYPE);
    }
  }
}
