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

package org.exoplatform.wiki.jpa.entity;

import org.exoplatform.commons.api.persistence.ExoEntity;

import javax.persistence.*;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity(name = "WikiPageVersionEntity")
@ExoEntity
@Table(name = "WIKI_PAGE_VERSIONS")
@NamedQueries({
        @NamedQuery(name = "wikiPageVersion.getLastversionNumberOfPage", query = "SELECT max(p.versionNumber) FROM WikiPageVersionEntity p WHERE p.page.id = :pageId"),
        @NamedQuery(name = "wikiPageVersion.getPageversionByPageIdAndVersion", query = "SELECT p FROM WikiPageVersionEntity p WHERE p.page.id = :pageId AND p.versionNumber = :versionNumber"),
        @NamedQuery(name = "wikiPageVersion.getAllPagesVersionsBySyntax", query = "SELECT p FROM WikiPageVersionEntity p WHERE p.syntax = :syntax OR p.syntax IS NULL ORDER BY p.updatedDate DESC"),
        @NamedQuery(name = "wikiPageVersion.countAllPagesVersionsBySyntax", query = "SELECT COUNT(p) FROM WikiPageVersionEntity p WHERE p.syntax = :syntax OR p.syntax IS NULL"),
        @NamedQuery(name = "wikiPageVersion.getPageVersionsByPageIdAndLang", query = "SELECT p FROM WikiPageVersionEntity p WHERE p.page.id = :pageId AND ((:lang IS NULL AND p.lang IS NULL) OR (:lang IS NOT NULL AND p.lang = :lang))"),
        @NamedQuery(name = "wikiPageVersion.getLatestPageVersionsByPageIdAndLang", query = "SELECT p FROM WikiPageVersionEntity p WHERE p.page.id = :pageId AND ((:lang IS NULL AND p.lang IS NULL) OR (:lang IS NOT NULL AND p.lang = :lang)) ORDER BY  p.versionNumber DESC"),
        @NamedQuery(name = "wikiPageVersion.getPageAvailableTranslationLanguages", query = "SELECT DISTINCT p.lang FROM WikiPageVersionEntity p WHERE p.page.id = :pageId AND p.lang IS NOT NULL")

})
public class PageVersionEntity extends BasePageEntity {
  @Id
  @SequenceGenerator(name="SEQ_WIKI_PAGE_VERSIONS_VERS_ID", sequenceName="SEQ_WIKI_PAGE_VERSIONS_VERS_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_PAGE_VERSIONS_VERS_ID")
  @Column(name = "PAGE_VERSION_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "PAGE_ID")
  private PageEntity page;

  @Column(name = "VERSION_NUMBER")
  private long versionNumber;

  @Column(name = "EDITION_COMMENT")
  private String comment;

  @Column(name = "MINOR_EDIT")
  private boolean minorEdit;

  @Column(name = "LANG")
  private String lang;

  public long getId() {
    return id;
  }

  public PageEntity getPage() {
    return page;
  }

  public void setPage(PageEntity page) {
    this.page = page;
  }

  public long getVersionNumber() {
    return versionNumber;
  }

  public void setVersionNumber(long versionNumber) {
    this.versionNumber = versionNumber;
  }

  public String getComment() {
    return comment;
  }

  public void setComment(String comment) {
    this.comment = comment;
  }

  public boolean isMinorEdit() {
    return minorEdit;
  }

  public void setMinorEdit(boolean minorEdit) {
    this.minorEdit = minorEdit;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
