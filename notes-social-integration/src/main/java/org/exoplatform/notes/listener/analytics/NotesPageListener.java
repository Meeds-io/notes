/**
 * This file is part of the Meeds project (https://meeds.io/).
 * 
 * Copyright (C) 2022 Meeds Association contact@meeds.io
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
package org.exoplatform.notes.listener.analytics;

import static org.exoplatform.analytics.utils.AnalyticsUtils.addSpaceStatistics;

import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.analytics.model.StatisticData;
import org.exoplatform.analytics.utils.AnalyticsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

public class NotesPageListener extends PageWikiListener {

  private static final Log    LOG                        = ExoLogger.getLogger(NotesPageListener.class);

  private static final String WIKI_ADD_PAGE_OPERATION    = "noteCreated";

  private static final String WIKI_UPDATE_PAGE_OPERATION = "noteUpdated";

  private static final String WIKI_DELETE_PAGE_OPERATION = "noteDeleted";

  private static final String WIKI_OPEN_PAGE_TREE = "openNoteByTree";

  private static final String WIKI_OPEN_PAGE_BREAD_CRUMB = "openNoteByBreadCrumb";

  protected PortalContainer   container;

  protected IdentityManager   identityManager;

  protected SpaceService      spaceService;

  public NotesPageListener() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_ADD_PAGE_OPERATION, null);
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    if (!(page instanceof DraftPage) && wikiUpdateType != null) {
      computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_UPDATE_PAGE_OPERATION, wikiUpdateType);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_DELETE_PAGE_OPERATION, null);
  }

  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_OPEN_PAGE_TREE, null);
  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_OPEN_PAGE_BREAD_CRUMB, null);
  }

  private void computeWikiPageStatistics(Page page,
                                         String wikiType,
                                         String wikiOwner,
                                         String operation,
                                         PageUpdateType wikiUpdateType) {
    ConversationState conversationstate = ConversationState.getCurrent();
    final String modifierUsername = conversationstate == null
        || conversationstate.getIdentity() == null ? null : conversationstate.getIdentity().getUserId();

    computeWikiPageStatisticsAsync(page, wikiType, wikiOwner, modifierUsername, operation, wikiUpdateType);
  }

  private void computeWikiPageStatisticsAsync(final Page page,
                                              final String wikiType,
                                              final String wikiOwner,
                                              final String modifierUsername,
                                              final String operation,
                                              final PageUpdateType wikiUpdateType) {
    CompletableFuture.supplyAsync(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        long userIdentityId = getUserIdentityId(modifierUsername);
        createWikiPageStatistic(page, wikiType, wikiOwner, userIdentityId, operation, wikiUpdateType);
      } catch (Exception e) {
        LOG.warn("Error computing wiki statistics", e);
      } finally {
        RequestLifeCycle.end();
      }
      return null;
    });
  }

  private void createWikiPageStatistic(Page page,
                                       String wikiType,
                                       String wikiOwner,
                                       long userIdentityId,
                                       String operation,
                                       PageUpdateType wikiUpdateType) {
    StatisticData statisticData = new StatisticData();
    statisticData.setModule("Note");
    statisticData.setSubModule("Note");
    statisticData.setOperation(operation);
    statisticData.setUserId(userIdentityId);

    if (StringUtils.isNotBlank(wikiOwner)
        && StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
      Space space = getSpaceService().getSpaceByGroupId(wikiOwner);
      addSpaceStatistics(statisticData, space);
    }
    if (page != null) {
      statisticData.addParameter("wikiPageId", page.getId());
      statisticData.addParameter("wikiId", page.getWikiId());
      statisticData.addParameter("contentLength", page.getContent() == null ? 0 : page.getContent().length());
      statisticData.addParameter("titleLength", page.getTitle() == null ? 0 : page.getTitle().length());
      statisticData.addParameter("authorId", getUserIdentityId(page.getAuthor()));
      statisticData.addParameter("ownerId", getUserIdentityId(page.getOwner()));
      statisticData.addParameter("wikiType", page.getWikiType());
      statisticData.addParameter("createdDate", page.getCreatedDate());
    }
    if (wikiUpdateType != null) {
      statisticData.addParameter("updateType", wikiUpdateType.name());
    }

    AnalyticsUtils.addStatisticData(statisticData);
  }

  private long getUserIdentityId(final String username) {
    if (StringUtils.isBlank(username)) {
      return 0;
    }
    Identity userIdentity = getIdentityManager().getOrCreateIdentity(OrganizationIdentityProvider.NAME, username);
    if (userIdentity == null) {
      return 0;
    }
    return Long.parseLong(userIdentity.getId());
  }

  private SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = this.container.getComponentInstanceOfType(SpaceService.class);
    }
    return spaceService;
  }

  private IdentityManager getIdentityManager() {
    if (identityManager == null) {
      identityManager = this.container.getComponentInstanceOfType(IdentityManager.class);
    }
    return identityManager;
  }

}
