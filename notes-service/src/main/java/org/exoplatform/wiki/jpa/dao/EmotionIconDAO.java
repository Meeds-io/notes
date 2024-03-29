/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.jpa.dao;

import org.exoplatform.commons.persistence.impl.GenericDAOJPAImpl;
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
