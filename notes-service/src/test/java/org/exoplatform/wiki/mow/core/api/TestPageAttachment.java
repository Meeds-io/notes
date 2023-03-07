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

package org.exoplatform.wiki.mow.core.api;

import java.util.Arrays;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.WikiService;

public class TestPageAttachment extends BaseTest {

  private WikiService wikiService;

  @Override
  public void setUp() throws Exception {
    super.setUp();
    wikiService = getContainer().getComponentInstanceOfType(WikiService.class);
  }

  public void testAddPageAttachment() throws Exception {
    Wiki wiki = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, "wikiAttachement1");
    Page wikiHome = wiki.getWikiHome();

    Attachment attachment = new Attachment();
    attachment.setName("attachment1.png");
    attachment.setContent("logo".getBytes());
    attachment.setMimeType("image/png");
    attachment.setCreator("root");
    wikiService.addAttachmentToPage(attachment, wikiHome);

    Attachment storedAttachment = wikiService.getAttachmentOfPageByName("attachment1.png", wikiHome, true);

    assertNotNull(storedAttachment);
    assertNotNull(storedAttachment.getName());
    assertEquals(attachment.getName(), storedAttachment.getName());
    assertNotNull(storedAttachment.getContent());
    assertTrue(Arrays.equals(attachment.getContent(), storedAttachment.getContent()));
  }
}
