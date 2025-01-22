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

import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static io.meeds.notes.service.TermsAndConditionsService.EVENT_NAME_ACCEPTED;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import jakarta.annotation.PostConstruct;

import java.util.HashMap;
import java.util.Map;

@Asynchronous
@Component
@Profile("gamification")
public class TermsAndConditionsGamificationListener extends Listener<String, Object> {

  private static final Log LOG = ExoLogger.getLogger(TermsAndConditionsGamificationListener.class);

  @Autowired
  private ListenerService  listenerService;

  @PostConstruct
  public void init() {
    listenerService.addListener(EVENT_NAME_ACCEPTED, this);
  }

  @Override
  public void onEvent(Event<String, Object> event) throws Exception {
    String username = event.getSource();
    try {
      Map<String, String> gam = new HashMap<>();
      gam.put("senderId", username);
      gam.put("receiverId", username);
      gam.put("objectId", username);
      gam.put("ruleTitle", "acceptConditions");
      listenerService.broadcast(GENERIC_EVENT_NAME, gam, "");
      LOG.info("Accept conditions action broadcasted for user {}", username);
    } catch (Exception e) {
      LOG.warn("An error occurred while broadcasting accept conditions event", e);
    }
  }
}
