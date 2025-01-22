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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package io.meeds.notes.service;

import org.exoplatform.social.websocket.entity.WebSocketMessage;
import org.exoplatform.ws.frameworks.cometd.ContinuationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static io.meeds.notes.listener.TermsAndConditionsWebSocketListener.TERMS_AND_CONDITIONS_ACCEPTED;

@Service
public class TermsAndConditionsWebSocketService {

  public static final String  COMETD_CHANNEL = "/TermsAndConditions";

  @Autowired
  private ContinuationService continuationService;

  /**
   * Propagate an event from Backend to frontend through WebSocket Message when
   * terms and conditions added, updated or accepted
   *
   * @param eventName event name that will allow Browser to distinguish which
   *          behavior to adopt in order to update UI
   */
  public void sendMessage(String eventName, String remoteId) {
    String wsMessage = new WebSocketMessage(eventName).toJsonString();
    if (TERMS_AND_CONDITIONS_ACCEPTED.equals(eventName)) {
      continuationService.sendMessage(remoteId, COMETD_CHANNEL, wsMessage);
    } else {
      continuationService.sendBroadcastMessage(COMETD_CHANNEL, wsMessage);
    }
  }
}
