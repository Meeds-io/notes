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
package org.exoplatform.wiki.jpa.dao;

import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;

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

  /**
   * The tree of a notebook, read on the real engine: one row per page that
   * is not deleted, in page id order, with its parent id (null for the
   * Home), name and owner; a deleted page and another notebook's pages are
   * left out, and a group notebook's owner is found with or without its
   * leading slash.
   */
  @Test
  public void testGetPageTreeOfWiki() {
    WikiEntity wiki = new WikiEntity();
    wiki.setType("group");
    wiki.setOwner("/spaces/tree");
    wiki = wikiDAO.create(wiki);
    WikiEntity otherWiki = new WikiEntity();
    otherWiki.setType("group");
    otherWiki.setOwner("/spaces/other");
    otherWiki = wikiDAO.create(otherWiki);
    PageEntity home = pageDAO.create(treePage(wiki, null, "WikiHome", null, false));
    PageEntity child = pageDAO.create(treePage(wiki, home, "child", "john", false));
    pageDAO.create(treePage(wiki, home, "deletedChild", "john", true));
    PageEntity grandChild = pageDAO.create(treePage(wiki, child, "grandChild", "mary", false));
    pageDAO.create(treePage(otherWiki, null, "WikiHome", null, false));

    List<Object[]> tree = pageDAO.getPageTreeOfWiki("group", "spaces/tree");

    assertEquals(3, tree.size());
    assertTreeRow(tree.get(0), home.getId(), null, "WikiHome", null);
    assertTreeRow(tree.get(1), child.getId(), home.getId(), "child", "john");
    assertTreeRow(tree.get(2), grandChild.getId(), child.getId(), "grandChild", "mary");
    assertEquals(3, pageDAO.getPageTreeOfWiki("group", "/spaces/tree").size());
  }

  private PageEntity treePage(WikiEntity wiki, PageEntity parent, String name, String owner, boolean deleted) {
    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setParentPage(parent);
    page.setName(name);
    page.setTitle(name);
    page.setOwner(owner);
    page.setDeleted(deleted);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    return page;
  }

  private void assertTreeRow(Object[] row, long id, Long parentId, String name, String owner) {
    assertEquals(id, ((Number) row[0]).longValue());
    assertEquals(parentId, row[1] == null ? null : ((Number) row[1]).longValue());
    assertEquals(name, row[2]);
    assertEquals(owner, row[3]);
  }

}
