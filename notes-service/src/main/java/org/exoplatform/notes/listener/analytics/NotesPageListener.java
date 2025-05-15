/*
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

import static io.meeds.analytics.utils.AnalyticsUtils.addSpaceStatistics;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;

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
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import io.meeds.social.cms.service.CMSService;

public class NotesPageListener extends PageWikiListener {

  private static final Log    LOG                         = ExoLogger.getLogger(NotesPageListener.class);

  private static final String WIKI_ADD_PAGE_OPERATION     = "createContent";

  private static final String WIKI_UPDATE_PAGE_OPERATION  = "updateContent";

  private static final String WIKI_DELETE_PAGE_OPERATION  = "deleteContent";

  private static final String NOTE_VIEW_CONTENT_OPERATION = "viewContent";

  protected PortalContainer   container;

  protected IdentityManager   identityManager;

  protected SpaceService      spaceService;
  
  protected CMSService        cmsService;
  
  protected NoteService       noteService;

  public NotesPageListener() {
    this.container = PortalContainer.getInstance();
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_ADD_PAGE_OPERATION);
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    if (!(page instanceof DraftPage) && wikiUpdateType != null) {
      computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_UPDATE_PAGE_OPERATION);
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    computeWikiPageStatistics(page, wikiType, wikiOwner, WIKI_DELETE_PAGE_OPERATION);
  }

  @Override
  public void markNoteAsViewed(Page note, String viewer) {
    if (!(note instanceof DraftPage)) {
      computeWikiPageStatisticsAsync(note, note.getWikiType(), note.getWikiOwner(), viewer, NOTE_VIEW_CONTENT_OPERATION);
    }
  }

  @Override
  public void postDeletePageVersion(PageVersion pageVersion) {
    computeWikiPageStatistics(pageVersion, pageVersion.getWikiType(), pageVersion.getWikiOwner(), WIKI_DELETE_PAGE_OPERATION);
  }

  @Override
  public void postUpdatePageVersion(String pageVersionId) {
    processPageVersionUpdate(pageVersionId, WIKI_UPDATE_PAGE_OPERATION);
  }

  private void computeWikiPageStatistics(Page page,
                                         String wikiType,
                                         String wikiOwner,
                                         String operation) {
    ConversationState conversationstate = ConversationState.getCurrent();
    final String modifierUsername = conversationstate == null
        || conversationstate.getIdentity() == null ? null : conversationstate.getIdentity().getUserId();

    computeWikiPageStatisticsAsync(page, wikiType, wikiOwner, modifierUsername, operation);
  }

  private void computeWikiPageStatisticsAsync(final Page page,
                                              final String wikiType,
                                              final String wikiOwner,
                                              final String modifierUsername,
                                              final String operation) {
    CompletableFuture.supplyAsync(() -> {
      ExoContainerContext.setCurrentContainer(container);
      RequestLifeCycle.begin(container);
      try {
        createWikiPageStatistic(page, wikiType, wikiOwner, modifierUsername, operation);
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
                                       String modifierUsername,
                                       String operation) {
    if (page != null && getCmsService().getSetting("notePage", page.getName()) == null) {
      long userIdentityId = getUserIdentityId(modifierUsername);
      StatisticData statisticData = new StatisticData();
      statisticData.setModule("contents");
      statisticData.setSubModule("contents");
      statisticData.setOperation(operation);
      statisticData.setUserId(userIdentityId);
      String contentId = page.getId();
      if (page instanceof PageVersion) {
        contentId = page.getParent().getId();
      }
      statisticData.addParameter("contentId", contentId);
      statisticData.addParameter("contentTitle", page.getTitle());
      if (!operation.equals(WIKI_ADD_PAGE_OPERATION)) {
        statisticData.addParameter("contentLanguage", page.getLang() != null ? page.getLang() : "originalVersion");
      }
      statisticData.addParameter("contentCreator", page.getAuthor());
      String lastModifier = page.getLastUpdater();
      if (lastModifier == null) {
        PageVersion pageVersion = getNoteService().getPublishedVersionByPageIdAndLang(Long.valueOf(page.getId()), page.getLang());
        lastModifier = Objects.requireNonNullElse(pageVersion, page).getAuthor();
      }
      statisticData.addParameter("contentLastModifier", lastModifier);
      statisticData.addParameter("contentType", "Note");
      statisticData.addParameter("contentUpdatedDate", page.getUpdatedDate());
      statisticData.addParameter("contentCreationDate", page.getCreatedDate());

      if (StringUtils.isNotBlank(wikiOwner) && StringUtils.equalsIgnoreCase(WikiType.GROUP.name(), wikiType)) {
        Space space = getSpaceService().getSpaceByGroupId(wikiOwner);
        addSpaceStatistics(statisticData, space);
      }
      AnalyticsUtils.addStatisticData(statisticData);
    }
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

  private void processPageVersionUpdate(String pageVersionId, String operation) {
    if (pageVersionId == null || pageVersionId.isEmpty()) {
      return;
    }

    String[] data = pageVersionId.split("-");
    Long versionId = Long.parseLong(data[0]);
    String lang = (data.length > 1) ? data[1] : null;

    PageVersion pageVersion = getNoteService().getPublishedVersionByPageIdAndLang(versionId, lang);
    if (pageVersion != null) {
      computeWikiPageStatistics(pageVersion, pageVersion.getWikiType(), pageVersion.getWikiOwner(), operation);
    }
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

  private CMSService getCmsService() {
    if (cmsService == null) {
      cmsService = this.container.getComponentInstanceOfType(CMSService.class);
    }
    return cmsService;
  }

  private NoteService getNoteService() {
    if (noteService == null) {
      noteService = this.container.getComponentInstanceOfType(NoteService.class);
    }
    return noteService;
  }
}
