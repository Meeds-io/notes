/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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
package io.meeds.notes.listener;

import static io.meeds.analytics.utils.AnalyticsUtils.addStatisticData;
import static io.meeds.analytics.utils.AnalyticsUtils.getUserIdentityId;

import io.meeds.analytics.model.StatisticData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.*;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;

import jakarta.annotation.PostConstruct;

@Asynchronous
@Component
@Profile("analytics")
public class TermsAndConditionsAnalyticsListener extends Listener<String, Object> {

  private static final Log LOG = ExoLogger.getLogger(TermsAndConditionsAnalyticsListener.class);

  @Autowired
  private ListenerService  listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener("terms.condition.accepted", this);
  }

  @Override
  public void onEvent(Event<String, Object> event) {
    long userId = getUserIdentityId(event.getSource());
    if (userId <= 0) {
      LOG.debug("User not found in state, username= {} ", event.getSource());
      return;
    }
    StatisticData statisticData = new StatisticData();
    statisticData.setModule("social");
    statisticData.setSubModule("terms");
    statisticData.setOperation("acceptTermsAndConditions");
    statisticData.setUserId(userId);
    addStatisticData(statisticData);
  }
}
