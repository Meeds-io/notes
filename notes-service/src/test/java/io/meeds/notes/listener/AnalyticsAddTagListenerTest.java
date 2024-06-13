/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import io.meeds.analytics.utils.AnalyticsUtils;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;
import org.exoplatform.wiki.model.Page;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AnalyticsAddTagListenerTest {

  @Mock
  private IdentityManager                              identityManager;

  @Mock
  private Identity                                     identity;

  private static final MockedStatic<ConversationState> CONVERSATION_STATE = mockStatic(ConversationState.class);

  private static final MockedStatic<AnalyticsUtils>    ANALYTICS_UTILS    = mockStatic(AnalyticsUtils.class);

  private AnalyticsAddTagListener analyticsAddTagListener;

  @Before
  public void setUp() throws Exception {
    this.analyticsAddTagListener = new AnalyticsAddTagListener(identityManager);
    ConversationState conversationState = mock(ConversationState.class);
    CONVERSATION_STATE.when(ConversationState::getCurrent).thenReturn(conversationState);
    CONVERSATION_STATE.when(() -> ConversationState.getCurrent().getIdentity()).thenReturn(identity);
    when(identity.getUserId()).thenReturn("user");
    org.exoplatform.social.core.identity.model.Identity socialIdentity =
                                                                       mock(org.exoplatform.social.core.identity.model.Identity.class);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(socialIdentity);
    when(socialIdentity.getId()).thenReturn("1");
  }

  @Test
  public void onEvent() {
    Page note = new Page();
    note.setId("1");
    note.setWikiOwner("/spaces/space");
    note.setContent("test ><a class=\"metadata-tag\">#testTag</a></span>");
    TagName testTag = new TagName("testTag");
    Set<TagName> tagSet = new HashSet<>();
    tagSet.add(testTag);
    Event<TagObject, Set<TagName>> event = new Event<>("metadata.tag.added",
                                                       new TagObject("notes", note.getId(), note.getParentPageId()),
                                                       tagSet);
    analyticsAddTagListener.onEvent(event);
    ANALYTICS_UTILS.verify(times(1), () -> AnalyticsUtils.addStatisticData(any()));
  }

  @After
  public void tearDown() throws Exception {
    CONVERSATION_STATE.close();
    ANALYTICS_UTILS.close();
  }
}
