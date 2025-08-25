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

import org.exoplatform.wiki.jpa.entity.EmotionIconEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class EmotionIconDAO extends WikiBaseDAO<EmotionIconEntity, Long> {

  public EmotionIconEntity getEmotionIconByName(String emotionIconName) {
    TypedQuery<EmotionIconEntity> query = getEntityManager().createNamedQuery("emotionIcon.getEmotionIconByName", EmotionIconEntity.class)
            .setParameter("name", emotionIconName);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
