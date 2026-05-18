/**
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
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = { TermsAndConditionsGamificationListener.class, })
@TestPropertySource(properties = { "spring.profiles.active=gamification", })
class TermsAndConditionsGamificationListenerTest {

  @MockitoBean
  private ListenerService                        listenerService;

  @MockitoBean
  private Event<String, Object>                  event;

  @Autowired
  private TermsAndConditionsGamificationListener listener;

  @Test
  void onEvent() throws Exception {
    String username = "testUser";
    Map<String, String> source = new HashMap<>();
    source.put("ruleTitle", "acceptConditions");
    source.put("objectId", username);
    source.put("senderId", username);
    source.put("receiverId", username);
    when(event.getSource()).thenReturn(username);
    listener.onEvent(event);
    verify(listenerService, times(1)).broadcast(GENERIC_EVENT_NAME, source, "");
  }
}
