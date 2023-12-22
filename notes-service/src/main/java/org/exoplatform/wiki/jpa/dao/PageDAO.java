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

package org.exoplatform.wiki.jpa.dao;

import java.util.List;

import jakarta.persistence.NonUniqueResultException;
import jakarta.persistence.TypedQuery;

import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.model.WikiType;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com Jun
 * 24, 2015
 */
public class PageDAO extends WikiBaseDAO<PageEntity, Long> {

  public List<PageEntity> getAllPagesOfWiki(String wikiType, String wikiOwner) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (wikiType.toUpperCase().equals(WikiType.GROUP.name())) wikiOwner = validateGroupWikiOwner(wikiOwner);

    TypedQuery<PageEntity> query = getEntityManager().createNamedQuery("wikiPage.getAllPagesOfWiki", PageEntity.class)
        .setParameter("type", wikiType)
        .setParameter("owner", wikiOwner);

    return query.getResultList();
  }

  public List<PageEntity> getPagesOfWiki(String wikiType, String wikiOwner, boolean deleted) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (wikiType.toUpperCase().equals(WikiType.GROUP.name())) wikiOwner = validateGroupWikiOwner(wikiOwner);

    TypedQuery<PageEntity> query = getEntityManager().createNamedQuery("wikiPage.getPagesOfWiki", PageEntity.class)
        .setParameter("type", wikiType)
        .setParameter("owner", wikiOwner)
        .setParameter("deleted", deleted);

    return query.getResultList();
  }

  public PageEntity getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (WikiType.GROUP.isSame(wikiType)) wikiOwner = validateGroupWikiOwner(wikiOwner);

    PageEntity pageEntity = null;
    TypedQuery<PageEntity> query = getEntityManager().createNamedQuery("wikiPage.getPageOfWikiByName", PageEntity.class)
                                               .setParameter("name", pageName)
                                               .setParameter("type", wikiType)
                                               .setParameter("owner", wikiOwner);

    // We don't use "query.getSingleResult()" because there is no good solution to have a case sensitive comparison
    // on the page name between all supported databases (I look at you MySQL). Having several pages in a wiki
    // with the same name with different cases is allowed functionally speaking, so we post-process results in Java
    // to be sure to have a case sensitive match in a database agnostic way
    List<PageEntity> results = query.getResultList();
    if(results != null) {
      for (PageEntity pageEntityResult : results) {
        // compare names with case sensitivity
        if (pageEntityResult.getName().equals(pageName)) {
          if (pageEntity == null) {
            pageEntity = pageEntityResult;
          } else {
            throw new NonUniqueResultException("More than 1 page with the name " + pageName
                    + " in the wiki " + wikiType + ":" + wikiOwner + " has been returned");
          }
        }
      }
    }

    return pageEntity;
  }

  public List<PageEntity> getChildrenPages(PageEntity page) {
    TypedQuery<PageEntity> query = getEntityManager().createNamedQuery("wikiPage.getChildrenPages", PageEntity.class)
                                               .setParameter("id", page.getId());
    return query.getResultList();
  }


  public List<Long> findAllIds(int offset, int limit) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("wikiPage.getAllIds", Long.class);
    if (offset > 0) {
      query.setFirstResult(offset);
    }
    if (limit > 0) {
      query.setMaxResults(limit);
    }
    return query.getResultList();
  }

  public Long countAllIds() {
    return (Long) getEntityManager().createNamedQuery("wikiPage.countAllIds").getSingleResult();
  }

  public List<PageEntity> findAllBySyntax(String syntax, int offset, int limit) {
    return getEntityManager().createNamedQuery("wikiPage.getAllPagesBySyntax")
            .setParameter("syntax", syntax)
            .setFirstResult(offset)
            .setMaxResults(limit)
            .getResultList();
  }

  public Long countPagesBySyntax(String syntax) {
    return (Long) getEntityManager().createNamedQuery("wikiPage.countAllPagesBySyntax")
            .setParameter("syntax", syntax)
            .getSingleResult();
  }
  public Long countPageChildrenById(Long pageId) {
    return (Long) getEntityManager().createNamedQuery("wikiPage.countPageChildrenById")
            .setParameter("id", pageId)
            .getSingleResult();
  }

}
