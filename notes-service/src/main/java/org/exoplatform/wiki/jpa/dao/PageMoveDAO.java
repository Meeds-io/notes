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

import org.exoplatform.wiki.jpa.entity.PageMoveEntity;
import org.exoplatform.wiki.model.WikiType;

import jakarta.persistence.TypedQuery;
import java.util.List;

public class PageMoveDAO extends WikiBaseDAO<PageMoveEntity,Long> {

  public List<PageMoveEntity> findInPageMoves(String wikiType, String wikiOwner, String pageName) {

    //We need to add the first "/" on the wiki owner if it's  wiki group
    if (wikiType.toUpperCase().equals(WikiType.GROUP.name())) wikiOwner = validateGroupWikiOwner(wikiOwner);

    TypedQuery<PageMoveEntity> query = getEntityManager().createNamedQuery("wikiPageMove.getPreviousPage", PageMoveEntity.class)
            .setParameter("wikiType", wikiType)
            .setParameter("wikiOwner", wikiOwner)
            .setParameter("pageName", pageName);
    return query.getResultList();
  }

}
