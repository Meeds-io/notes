/**
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.wiki.ext.impl;

import static io.meeds.gamification.constant.GamificationConstant.EVENT_NAME;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_ID_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.OBJECT_TYPE_PARAM;
import static io.meeds.gamification.constant.GamificationConstant.RECEIVER_ID;
import static io.meeds.gamification.constant.GamificationConstant.SENDER_ID;
import static io.meeds.gamification.listener.GamificationGenericListener.GENERIC_EVENT_NAME;
import static org.exoplatform.wiki.integration.gamification.GamificationWikiListener.GAMIFICATION_WIKI_ADD_PAGE;
import static org.exoplatform.wiki.integration.gamification.GamificationWikiListener.GAMIFICATION_WIKI_UPDATE_PAGE;
import static org.exoplatform.wiki.integration.gamification.GamificationWikiListener.NOTES_OBJECT_TYPE;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.apache.commons.codec.binary.StringUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.integration.gamification.GamificationWikiListener;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.PageUpdateType;

@RunWith(MockitoJUnitRunner.class)
public class GamificationWikiListenerTest {

  private static final String                         USER_IDENTITY_ID = "1";

  private static final String                         USERNAME         = "testuser";

  private static final String                         WIKI_TYPE        = "space";

  private static final String                         WIKI_OWNER       = "testSpace";

  private static final String                         PAGE_ID          = "5";

  @Mock
  ListenerService                                     listenerService;

  @Mock
  IdentityManager                                     identityManager;

  @Mock
  Page                                                page;

  @Mock
  org.exoplatform.social.core.identity.model.Identity userIdentity;

  @Test
  public void testCreatePageEvent() throws Exception {
    GamificationWikiListener gamificationListener = new GamificationWikiListener(identityManager, listenerService);

    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
    when(userIdentity.getId()).thenReturn(USER_IDENTITY_ID);
    when(page.getId()).thenReturn(PAGE_ID);

    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
    gamificationListener.postAddPage(WIKI_TYPE, WIKI_OWNER, PAGE_ID, page);
    verify(listenerService, times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                                                argThat(param -> (param instanceof @SuppressWarnings("rawtypes")
                                                Map map)
                                                    && StringUtils.equals(((String) map.get(EVENT_NAME)),
                                                                          GAMIFICATION_WIKI_ADD_PAGE)
                                                    && StringUtils.equals(((String) map.get(OBJECT_ID_PARAM)), PAGE_ID)
                                                    && StringUtils.equals(((String) map.get(OBJECT_TYPE_PARAM)),
                                                                          NOTES_OBJECT_TYPE)
                                                    && StringUtils.equals(((String) map.get(SENDER_ID)), USER_IDENTITY_ID)
                                                    && StringUtils.equals(((String) map.get(RECEIVER_ID)), USER_IDENTITY_ID)),
                                                eq(null));
  }

  @Test
  public void testUpdatePageEvent() throws Exception {
    GamificationWikiListener gamificationListener = new GamificationWikiListener(identityManager, listenerService);

    when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
    when(userIdentity.getId()).thenReturn(USER_IDENTITY_ID);
    when(page.getId()).thenReturn(PAGE_ID);

    ConversationState.setCurrent(new ConversationState(new Identity(USERNAME)));
    gamificationListener.postUpdatePage(WIKI_TYPE, WIKI_OWNER, PAGE_ID, page, PageUpdateType.EDIT_PAGE_CONTENT);
    verify(listenerService, times(1)).broadcast(eq(GENERIC_EVENT_NAME),
                                                argThat(param -> (param instanceof @SuppressWarnings("rawtypes")
                                                Map map)
                                                    && StringUtils.equals(((String) map.get(EVENT_NAME)),
                                                                          GAMIFICATION_WIKI_UPDATE_PAGE)
                                                    && StringUtils.equals(((String) map.get(OBJECT_ID_PARAM)), PAGE_ID)
                                                    && StringUtils.equals(((String) map.get(OBJECT_TYPE_PARAM)),
                                                                          NOTES_OBJECT_TYPE)
                                                    && StringUtils.equals(((String) map.get(SENDER_ID)), USER_IDENTITY_ID)
                                                    && StringUtils.equals(((String) map.get(RECEIVER_ID)), USER_IDENTITY_ID)),
                                                eq(null));
  }
}
