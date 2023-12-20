/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
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

package org.exoplatform.wiki.service.impl;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

import jakarta.servlet.http.HttpSessionEvent;

public class SessionDestroyedListener extends Listener<PortalContainer, HttpSessionEvent> {

  private static Log LOG = ExoLogger.getLogger("SessionDestroyedListener");

  @Override
  public void onEvent(Event<PortalContainer, HttpSessionEvent> event) {
    PortalContainer container = event.getSource();
    String sessionId = event.getData().getSession().getId();
    if (LOG.isTraceEnabled()) {
      LOG.trace("Removing the key: " + sessionId);
    }
    try {
      SessionManager sessionManager = container.getComponentInstanceOfType(SessionManager.class);
      sessionManager.removeSessionContainer(sessionId);
    } catch (Exception e) {
      LOG.warn("Can't remove the key: " + sessionId, e);
    }
    if (LOG.isTraceEnabled()) {
      LOG.trace("Removed the key: " + sessionId);
    }
    if (container.isStarted()) {
      String currentUser = Utils.getCurrentUser();
      if(currentUser != null) {
        NoteService noteService = container.getComponentInstanceOfType(NoteService.class);
        String draftPageName = null;
        try {
          RequestLifeCycle.begin(PortalContainer.getInstance());
          draftPageName = Utils.getPageNameForAddingPage(sessionId);
          noteService.removeDraft(draftPageName);
        } catch (WikiException e) {
          if (LOG.isDebugEnabled()) {
            LOG.debug("No draft page to be removed for user " + currentUser
                    + " (page name = " + draftPageName + ") on logout.", e);
          }
        } finally {
          RequestLifeCycle.end();
        }
      }
    }
  }
}
