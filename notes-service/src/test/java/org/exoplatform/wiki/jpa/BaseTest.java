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
package org.exoplatform.wiki.jpa;
import org.exoplatform.component.test.AbstractKernelTest;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.services.security.Authenticator;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.WikiService;


/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 25, 2015  
 */
public abstract class BaseTest extends AbstractKernelTest {

  @Override
  public void setUp() throws Exception {
    super.setUp();
    begin();
    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    System.setProperty("gatein.email.domain.url", "localhost");
  }

  @Override
  public void tearDown() throws Exception {
    end();
    super.tearDown();
  }

  protected void startSessionAs(String user) {
    try {
      Authenticator authenticator = getContainer().getComponentInstanceOfType(Authenticator.class);
      Identity userIdentity = authenticator.createIdentity(user);
      ConversationState.setCurrent(new ConversationState(userIdentity));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }
  public <T> T getService(Class<T> clazz) {
    return (T) getContainer().getComponentInstanceOfType(clazz);
  }

  protected Wiki getOrCreateWiki(WikiService wikiService, String type, String owner) throws WikiException {
    Wiki wiki = wikiService.getWikiByTypeAndOwner(type, owner);
    if (wiki == null) {
      return wikiService.createWiki(type, owner);
    }
    return wiki;
  }
}
