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

import java.util.Collections;
import java.util.List;

import org.apache.commons.collections4.CollectionUtils;

import org.exoplatform.wiki.jpa.entity.PageVersionEntity;

import jakarta.persistence.NoResultException;
import jakarta.persistence.TypedQuery;

public class PageVersionDAO extends WikiBaseDAO<PageVersionEntity, Long> {

  private static final String PAGE_ID_PARAM = "pageId";

  public Long getLastversionNumberOfPage(Long pageId) {
    TypedQuery<Long> query = getEntityManager().createNamedQuery("wikiPageVersion.getLastversionNumberOfPage", Long.class)
                                               .setParameter(PAGE_ID_PARAM, pageId);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public PageVersionEntity getPageversionByPageIdAndVersion(Long pageId, Long versionNumber) {
    TypedQuery<PageVersionEntity> query = getEntityManager()
                                                            .createNamedQuery("wikiPageVersion.getPageversionByPageIdAndVersion",
                                                                              PageVersionEntity.class)
                                                            .setParameter(PAGE_ID_PARAM, pageId)
                                                            .setParameter("versionNumber", versionNumber);

    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<PageVersionEntity> findAllVersionsBySyntax(String syntax, int offset, int limit) {
    return getEntityManager().createNamedQuery("wikiPageVersion.getAllPagesVersionsBySyntax", PageVersionEntity.class)
                             .setParameter("syntax", syntax)
                             .setFirstResult(offset)
                             .setMaxResults(limit)
                             .getResultList();
  }

  public Long countPagesVersionsBySyntax(String syntax) {
    return (Long) getEntityManager().createNamedQuery("wikiPageVersion.countAllPagesVersionsBySyntax")
                                    .setParameter("syntax", syntax)
                                    .getSingleResult();
  }

  public List<PageVersionEntity> findPageVersionsByPageIdAndLang(Long pageId, String lang) {
    TypedQuery<PageVersionEntity> query = getEntityManager()
                                                            .createNamedQuery("wikiPageVersion.getPageVersionsByPageIdAndLang",
                                                                              PageVersionEntity.class)
                                                            .setParameter(PAGE_ID_PARAM, pageId)
                                                            .setParameter("lang", lang);
    try {
      return query.getResultList();
    } catch (NoResultException e) {
      return Collections.emptyList();
    }
  }

  public PageVersionEntity findLatestVersionByPageIdAndLang(Long pageId, String lang) {
    TypedQuery<PageVersionEntity> query = getEntityManager()
                                                            .createNamedQuery("wikiPageVersion.getLatestPageVersionsByPageIdAndLang",
                                                                              PageVersionEntity.class)
                                                            .setParameter(PAGE_ID_PARAM, pageId)
                                                            .setParameter("lang", lang);
    query.setMaxResults(1);
    try {
      return query.getSingleResult();
    } catch (NoResultException e) {
      return null;
    }
  }

  public List<String> findPageAvailableTranslationLanguages(Long pageId) {
    return getEntityManager().createNamedQuery("wikiPageVersion.getPageAvailableTranslationLanguages", String.class)
                             .setParameter(PAGE_ID_PARAM, pageId)
                             .getResultList();
  }

  public List<PageVersionEntity> findVersionsByPage(Long pageId) {
    return getEntityManager().createNamedQuery("wikiPageVersion.findVersionsByPage", PageVersionEntity.class)
                             .setParameter(PAGE_ID_PARAM, pageId)
                             .getResultList();
  }

  public void deletePageVersions(long pageId) {
    List<PageVersionEntity> versions = findVersionsByPage(pageId);
    if (CollectionUtils.isNotEmpty(versions)) {
      versions.forEach(this::delete);
    }
  }

}
