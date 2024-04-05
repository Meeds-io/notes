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

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;
import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 *          exo@exoplatform.com
 * Jun 24, 2015  
 */
public class DraftPageDAO extends WikiBaseDAO<DraftPageEntity, Long> {

  public DraftPageEntity findLatestDraftPageByName(String draftPageName) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPageByName", DraftPageEntity.class)
            .setMaxResults(1)
            .setParameter("draftPageName", draftPageName);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<DraftPageEntity> findDraftPagesByTargetPage(long targetPageId) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPageByTargetPage", DraftPageEntity.class)
            .setParameter("targetPageId", targetPageId);
    return query.getResultList();
  }

  public List<DraftPageEntity> findDraftPagesByParentPage(long parentPageId) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findDraftPagesByParentPage", DraftPageEntity.class)
            .setParameter("parentPageId", parentPageId);
    return query.getResultList();
  }

  @ExoTransactional
  public void deleteDraftPagesByTargetPage(long targetPageId) {

    List<DraftPageEntity> draftPages = findDraftPagesByTargetPage(targetPageId);
    for (DraftPageEntity draftPage: draftPages) {
      delete(draftPage);
    }

  }

  @ExoTransactional
  public void deleteDraftPagesByName(String draftName) {
    DraftPageEntity draftPage = findLatestDraftPageByName(draftName);
    if(draftPage != null) {
      delete(draftPage);
    }
  }

  public DraftPageEntity findLatestDraftPageByTargetPage(Long targetPageId) {
    TypedQuery<DraftPageEntity> query = getEntityManager().createNamedQuery("wikiDraftPage.findLatestDraftPageByTargetPage", DraftPageEntity.class)
            .setParameter("targetPageId", targetPageId);

    try {
      query.setMaxResults(1);
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public DraftPageEntity findLatestDraftPageByTargetPageAndLang(Long targetPageId, String lang) {
    TypedQuery<DraftPageEntity> query =
                                      getEntityManager().createNamedQuery("wikiDraftPage.findLatestDraftPageByTargetPageAndLang",
                                                                          DraftPageEntity.class)
                                                        .setParameter("targetPageId", targetPageId)
                                                        .setParameter("lang", lang);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

}
