 /**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */

package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PermissionEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.model.PermissionType;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 7/31/15
 */
public class PageDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testGetPageOfWikiByName() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity parentPage = new PageEntity();
    parentPage.setWiki(wiki);
    parentPage.setName("parentPage1");
    parentPage.setCreatedDate(new Date());
    parentPage.setUpdatedDate(new Date());
    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(parentPage);
    pageDAO.create(page);

    assertEquals(2, pageDAO.findAll().size());

    PageEntity pageOfWikiByName = pageDAO.getPageOfWikiByName("portal", "wiki1", "page1");
    assertNotNull(pageOfWikiByName);
    assertEquals("portal", pageOfWikiByName.getWiki().getType());
    assertEquals("wiki1", pageOfWikiByName.getWiki().getOwner());
    assertEquals("page1", pageOfWikiByName.getName());
    assertEquals("Page 1", pageOfWikiByName.getTitle());
  }

  @Test
  public void testInsert() {
    // Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    PageEntity page = new PageEntity();
    page.setWiki(wiki);

    PermissionEntity per = new PermissionEntity();
    per.setIdentity("user");
    per.setIdentityType("User");
    per.setPermissionType(PermissionType.EDITPAGE);
    List<PermissionEntity> permissions = new ArrayList<PermissionEntity>();
    permissions.add(per);
    page.setPermissions(permissions);

    page.setAuthor("author");
    page.setContent("content");
    page.setComment("comment");
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("name");
    page.setMinorEdit(true);
    page.setOwner("owner");
    page.setSyntax("syntax");
    page.setTitle("title");
    page.setUrl("url");

    // When
    pageDAO.create(page);
    PageEntity got = pageDAO.find(page.getId());

    // Then
    assertNotNull(got);
    assertEquals("name", got.getName());
  }

  @Test
  public void testDuplicatedWikiOwnerTypeInsert() {
    // Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);

    // When
    try {
      WikiEntity wiki1 = new WikiEntity();
      wiki1.setType("portal");
      wiki1.setOwner("wiki1");
      wiki1 = wikiDAO.create(wiki1);

      // Then
      fail();
    } catch (Exception e) {
    }
  }

  @Test
  public void testCountAllIds() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity parentPage = new PageEntity();
    parentPage.setWiki(wiki);
    parentPage.setName("parentPage1");
    parentPage.setCreatedDate(new Date());
    parentPage.setUpdatedDate(new Date());
    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");

    pageDAO.create(parentPage);
    pageDAO.create(page);

    assertEquals(2, pageDAO.countAllIds().intValue());
  }

  @Test
  public void testFindAllBySyntax() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity parentPage = new PageEntity();
    parentPage.setWiki(wiki);
    parentPage.setName("parentPage1");
    parentPage.setCreatedDate(new Date());
    parentPage.setUpdatedDate(new Date());
    parentPage.setSyntax("syntax1");
    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parentPage);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page.setTitle("Page 1");
    page.setSyntax("syntax2");

    pageDAO.create(parentPage);
    pageDAO.create(page);
    List<PageEntity> fetchedPages = pageDAO.findAllBySyntax("syntax2", 0, 10);

    assertEquals(1, fetchedPages.size());
    assertEquals("page1", fetchedPages.get(0).getName());
  }

}
