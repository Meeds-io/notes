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

import io.meeds.common.ContainerTransactional;
import io.meeds.notes.service.TermsAndConditionsWebSocketService;
import jakarta.annotation.PostConstruct;
import org.exoplatform.services.listener.Asynchronous;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.listener.ListenerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

import static io.meeds.notes.service.TermsAndConditionsService.*;

@Component
@Asynchronous
public class TermsAndConditionsWebSocketListener extends Listener<String, Object> {

  public static final String                 TERMS_AND_CONDITIONS_ADDED    = "termsAndConditionsAdded";

  public static final String                 TERMS_AND_CONDITIONS_UPDATED  = "termsAndConditionsUpdated";

  public static final String                 TERMS_AND_CONDITIONS_ACCEPTED = "termsAndConditionsAccepted";

  protected static final List<String>        EVENT_NAMES                   =
                                                         Arrays.asList(EVENT_NAME_ADDED, EVENT_NAME_UPDATED, EVENT_NAME_ACCEPTED);

  @Autowired
  private TermsAndConditionsWebSocketService termsAndConditionsWebSocketService;

  @Autowired
  private ListenerService                    listenerService;

  @PostConstruct
  public void init() {
    EVENT_NAMES.forEach(eventName -> listenerService.addListener(eventName, this));
  }

  @Override
  @ContainerTransactional
  public void onEvent(Event<String, Object> event) throws Exception {
    String message;
    String remoteId = null;
    switch (event.getEventName()) {
    case EVENT_NAME_ADDED: {
      message = TERMS_AND_CONDITIONS_ADDED;
      break;
    }
    case EVENT_NAME_UPDATED: {
      message = TERMS_AND_CONDITIONS_UPDATED;
      break;
    }
    case EVENT_NAME_ACCEPTED: {
      message = TERMS_AND_CONDITIONS_ACCEPTED;
      remoteId = event.getSource();
      break;
    }
    default:
      throw new IllegalArgumentException("Unexpected listener event name: " + event.getEventName());
    }
    termsAndConditionsWebSocketService.sendMessage(message, remoteId);
  }
}
