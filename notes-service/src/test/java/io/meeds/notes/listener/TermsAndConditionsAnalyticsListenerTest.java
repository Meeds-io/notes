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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static io.meeds.analytics.utils.AnalyticsUtils.*;

import io.meeds.analytics.model.StatisticData;
import io.meeds.analytics.utils.AnalyticsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.ListenerService;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = { TermsAndConditionsAnalyticsListener.class, })
@TestPropertySource(properties = { "spring.profiles.active=analytics", })
class TermsAndConditionsAnalyticsListenerTest {

  @MockBean
  private ListenerService                     listenerService;

  @MockBean
  private Event<String, Object>               event;

  @Autowired
  private TermsAndConditionsAnalyticsListener listener;

  @Test
  void onEvent() {
    try (MockedStatic<AnalyticsUtils> mockedStatic = mockStatic(AnalyticsUtils.class)) {
      String username = "testUser";
      long userId = 123L;

      // When
      mockedStatic.when(() -> AnalyticsUtils.getUserIdentityId(username)).thenReturn(userId);
      when(event.getSource()).thenReturn(username);
      listener.onEvent(event);

      // Then
      ArgumentCaptor<StatisticData> captor = ArgumentCaptor.forClass(StatisticData.class);
      mockedStatic.verify(() -> AnalyticsUtils.addStatisticData(argThat(statisticData -> {
        assertEquals("social", statisticData.getModule());
        assertEquals("terms", statisticData.getSubModule());
        assertEquals("acceptTermsAndConditions", statisticData.getOperation());
        assertEquals(userId, statisticData.getUserId());
        return true;
      })), times(1));
      addStatisticData(captor.capture());
    }
  }
}
