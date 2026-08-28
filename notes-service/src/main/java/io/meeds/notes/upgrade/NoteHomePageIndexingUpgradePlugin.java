/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package io.meeds.notes.upgrade;

import java.util.List;

import org.apache.commons.collections.CollectionUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.commons.upgrade.UpgradePluginExecutionContext;
import org.exoplatform.commons.upgrade.UpgradeProductPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;

/**
 * Note book home pages used to be created without broadcasting their creation,
 * hence they never entered the search index and stayed invisible in the unified
 * search and in the notes tree filter until they happened to be edited. The
 * creation is broadcast since then, but the home pages of the note books created
 * before that stay missing from the index, and the notes indexing connector does
 * not support a full reindex. This plugin queues them back for indexing.
 * <p>
 * The execution is guarded by a dedicated setting rather than by the product
 * version, so that every existing instance repairs its index once, whatever
 * version it is upgrading from.
 */
public class NoteHomePageIndexingUpgradePlugin extends UpgradeProductPlugin {

  private static final Log       LOG                 = ExoLogger.getExoLogger(NoteHomePageIndexingUpgradePlugin.class);

  private static final int       BATCH_SIZE          = 100;

  private static final String    PLUGIN_NAME         = "NoteHomePageIndexingUpgradePlugin";

  private static final String    PLUGIN_EXECUTED_KEY = String.format("%sExecuted", PLUGIN_NAME);

  private final WikiDAO          wikiDAO;

  private final IndexingService  indexingService;

  private final SettingService   settingService;

  private boolean                upgradeSucceeded    = false;

  public NoteHomePageIndexingUpgradePlugin(WikiDAO wikiDAO,
                                           IndexingService indexingService,
                                           SettingService settingService,
                                           InitParams initParams) {
    super(settingService, initParams);
    this.wikiDAO = wikiDAO;
    this.indexingService = indexingService;
    this.settingService = settingService;
  }

  @Override
  public boolean shouldProceedToUpgrade(String newVersion,
                                        String previousGroupVersion,
                                        UpgradePluginExecutionContext upgradePluginExecutionContext) {
    return settingService.get(Context.GLOBAL.id(PLUGIN_NAME),
                              Scope.APPLICATION.id(PLUGIN_NAME),
                              PLUGIN_EXECUTED_KEY) == null;
  }

  @Override
  public void afterUpgrade() {
    // only flag it when it went through, so that a failed run is retried on the
    // next startup instead of silently leaving home pages out of the index
    if (upgradeSucceeded) {
      settingService.set(Context.GLOBAL.id(PLUGIN_NAME),
                         Scope.APPLICATION.id(PLUGIN_NAME),
                         PLUGIN_EXECUTED_KEY,
                         SettingValue.create(true));
    }
  }

  @Override
  public void processUpgrade(String oldVersion, String newVersion) {
    long startTime = System.currentTimeMillis();
    int indexedCount = 0;
    int offset = 0;
    LOG.info("Start upgrade: queue note book home pages for indexing");
    try {
      List<Long> homePageIds = wikiDAO.findAllHomePageIds(offset, BATCH_SIZE);
      while (CollectionUtils.isNotEmpty(homePageIds)) {
        for (Long homePageId : homePageIds) {
          if (homePageId == null) {
            continue;
          }
          try {
            // reindex and not index: the operation has to stay idempotent since
            // the home pages that were edited at least once are already indexed
            indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, String.valueOf(homePageId));
            indexedCount++;
          } catch (Exception e) {
            LOG.warn("Cannot queue the note book home page {} for indexing, it stays out of the search results",
                     homePageId,
                     e);
          }
        }
        offset += BATCH_SIZE;
        homePageIds = wikiDAO.findAllHomePageIds(offset, BATCH_SIZE);
      }
      upgradeSucceeded = true;
      LOG.info("End upgrade: {} note book home pages queued for indexing in {}ms",
               indexedCount,
               System.currentTimeMillis() - startTime);
    } catch (Exception e) {
      LOG.error("Error while queueing note book home pages for indexing, {} of them were queued, it will be retried on next startup",
                indexedCount,
                e);
    }
  }

}
