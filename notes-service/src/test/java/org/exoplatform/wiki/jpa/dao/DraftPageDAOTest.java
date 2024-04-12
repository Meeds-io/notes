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

import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.junit.Test;

import org.exoplatform.wiki.jpa.BaseWikiJPAIntegrationTest;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 26, 2015  
 */
public class DraftPageDAOTest extends BaseWikiJPAIntegrationTest {

  @Test
  public void testInsert(){
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setName("name");
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page = pageDAO.create(page);
    DraftPageEntity dp = new DraftPageEntity();
    dp.setName("draft1");
    dp.setTargetPage(page);
    dp.setCreatedDate(new Date());
    dp.setUpdatedDate(new Date());
    draftPageDAO.create(dp);
    
    assertNotNull(draftPageDAO.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    
    DraftPageEntity got = draftPageDAO.find(dp.getId());
    got.getTargetPage().setName("name1");
    draftPageDAO.update(got);
    assertEquals("name1",page.getName());
    draftPageDAO.deleteAll();
    pageDAO.deleteAll();
    
    assertNull(draftPageDAO.find(dp.getId()));
  }

  @Test
  public void testFindDraftPagesByUserAndTargetPage(){
    //Given
    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page = new PageEntity();
    page.setWiki(wiki);
    page.setCreatedDate(new Date());
    page.setUpdatedDate(new Date());
    page.setName("page1");
    page = pageDAO.create(page);
    DraftPageEntity dp = new DraftPageEntity();
    dp.setName("draft1");
    dp.setTargetPage(page);
    dp.setAuthor("user1");
    dp.setCreatedDate(new Date());
    dp.setUpdatedDate(new Date());
    draftPageDAO.create(dp);

    //When
    List<DraftPageEntity> drafts1 = draftPageDAO.findDraftPagesByTargetPage(page.getId());
    List<DraftPageEntity> drafts2 = draftPageDAO.findDraftPagesByTargetPage(page.getId() + 1);

    //Then
    assertNotNull(draftPageDAO.find(dp.getId()));
    assertNotNull(pageDAO.find(page.getId()));
    assertNotNull(drafts1);
    assertEquals(1, drafts1.size());
    assertNotNull(drafts2);
    assertEquals(0, drafts2.size());

    draftPageDAO.deleteAll();
    pageDAO.deleteAll();
  }

  @Test
  public void testDeleteDraftPageByTargetPage(){
    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, 1);
    Date oneYearAgo = calendar.getTime();

    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page1 = new PageEntity();
    page1.setName("page1");
    page1.setWiki(wiki);
    page1.setUpdatedDate(oneYearAgo);
    page1.setCreatedDate(oneYearAgo);
    pageDAO.create(page1);
    PageEntity page2 = new PageEntity();
    page2.setName("page2");
    page2.setWiki(wiki);
    page2.setUpdatedDate(now);
    page2.setCreatedDate(now);
    pageDAO.create(page2);

    DraftPageEntity dp1 = new DraftPageEntity();
    dp1.setTargetPage(page1);
    dp1.setAuthor("user1");
    dp1.setName("draft1");
    dp1.setUpdatedDate(oneYearAgo);
    dp1.setCreatedDate(oneYearAgo);
    dp1.setTargetRevision("1");
    draftPageDAO.create(dp1);
    DraftPageEntity dp2 = new DraftPageEntity();
    dp2.setTargetPage(page2);
    dp2.setAuthor("user1");
    dp2.setName("draft2");
    dp2.setUpdatedDate(now);
    dp2.setCreatedDate(now);
    dp2.setTargetRevision("1");
    draftPageDAO.create(dp2);

    assertEquals(2, draftPageDAO.findAll().size());
    assertEquals(2, pageDAO.findAll().size());
    draftPageDAO.deleteDraftPagesByTargetPage(page1.getId());
    assertEquals(1, draftPageDAO.findAll().size());
    assertEquals("draft2", draftPageDAO.findAll().get(0).getName());
    assertEquals(2, pageDAO.findAll().size());
    draftPageDAO.deleteDraftPagesByTargetPage(page2.getId());
    assertEquals(0, draftPageDAO.findAll().size());
    assertEquals(2, pageDAO.findAll().size());

    draftPageDAO.deleteAll();
    pageDAO.deleteAll();
  }

  @Test
  public void testDeleteDraftPageByUserAndName(){
    Calendar calendar = Calendar.getInstance();
    Date now = calendar.getTime();
    calendar.roll(Calendar.YEAR, 1);
    Date oneYearAgo = calendar.getTime();

    WikiEntity wiki = new WikiEntity();
    wiki.setType("portal");
    wiki.setOwner("wiki1");
    wiki = wikiDAO.create(wiki);
    PageEntity page1 = new PageEntity();
    page1.setName("page1");
    page1.setWiki(wiki);
    page1.setUpdatedDate(oneYearAgo);
    page1.setCreatedDate(oneYearAgo);
    pageDAO.create(page1);
    PageEntity page2 = new PageEntity();
    page2.setName("page2");
    page2.setWiki(wiki);
    page2.setUpdatedDate(now);
    page2.setCreatedDate(now);
    pageDAO.create(page2);

    DraftPageEntity dp1 = new DraftPageEntity();
    dp1.setTargetPage(page1);
    dp1.setAuthor("user1");
    dp1.setName("draft1");
    dp1.setUpdatedDate(oneYearAgo);
    dp1.setCreatedDate(oneYearAgo);
    dp1.setTargetRevision("1");
    draftPageDAO.create(dp1);
    DraftPageEntity dp2 = new DraftPageEntity();
    dp2.setTargetPage(page2);
    dp2.setAuthor("user1");
    dp2.setName("draft2");
    dp2.setUpdatedDate(now);
    dp2.setCreatedDate(now);
    dp2.setTargetRevision("1");
    draftPageDAO.create(dp2);

    assertEquals(2, draftPageDAO.findAll().size());
    assertEquals(2, pageDAO.findAll().size());
    draftPageDAO.deleteDraftPagesByName("draft1");
    assertEquals(1, draftPageDAO.findAll().size());
    assertEquals("draft2", draftPageDAO.findAll().get(0).getName());
    assertEquals(2, pageDAO.findAll().size());
    draftPageDAO.deleteDraftPagesByName("draft2");
    assertEquals(0, draftPageDAO.findAll().size());
    assertEquals(2, pageDAO.findAll().size());

    draftPageDAO.deleteAll();
    pageDAO.deleteAll();
  }
}
