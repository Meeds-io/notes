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
package org.exoplatform.wiki.jpa.entity;

import java.util.List;

import org.exoplatform.commons.api.persistence.ExoEntity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * Jun 23, 2015
 */
@Entity(name = "WikiDraftPageEntity")
@ExoEntity
@Table(name = "WIKI_DRAFT_PAGES")
@NamedQueries({
        @NamedQuery(name = "wikiDraftPage.findDraftPages", query = "SELECT d FROM WikiDraftPageEntity d ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findDraftPageByName", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.name = :draftPageName ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findLatestDraftPageByTargetPage", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.targetPage.id = :targetPageId ORDER BY d.updatedDate DESC"),
        @NamedQuery(name = "wikiDraftPage.findDraftPageByTargetPage", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.targetPage.id = :targetPageId"),
        @NamedQuery(name = "wikiDraftPage.findDraftPagesByParentPage", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.parentPage.id = :parentPageId"),
        @NamedQuery(name = "wikiDraftPage.findLatestDraftPageByTargetPageAndLang", query = "SELECT d FROM WikiDraftPageEntity d WHERE d.targetPage.id = :targetPageId AND " +
                                                                                                  "((:lang IS NULL AND d.lang IS NULL) OR (:lang IS NOT NULL AND d.lang = :lang)) ORDER BY d.updatedDate DESC"), })
public class DraftPageEntity extends BasePageEntity {

  @Id
  @SequenceGenerator(name="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID", sequenceName="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_DRAFT_PAGES_DRAFT_ID")
  @Column(name = "DRAFT_PAGE_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "TARGET_PAGE_ID")
  private PageEntity targetPage;

  @ManyToOne
  @JoinColumn(name = "PARENT_PAGE_ID")
  private PageEntity parentPage;

  @Column(name = "TARGET_PAGE_REVISION")
  private String targetRevision;

  @Column(name = "NEW_PAGE")
  private boolean newPage;

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "draftPage")
  private List<DraftPageAttachmentEntity> attachments;

  @Column(name = "LANG")
  private String lang;

  public PageEntity getTargetPage() {
    return targetPage;
  }

  public void setTargetPage(PageEntity targetPage) {
    this.targetPage = targetPage;
  }

  public PageEntity getParentPage() {
    return parentPage;
  }

  public void setParentPage(PageEntity parentPage) {
    this.parentPage = parentPage;
  }

  public String getTargetRevision() {
    return targetRevision;
  }

  public void setTargetRevision(String targetRevision) {
    this.targetRevision = targetRevision;
  }

  public boolean isNewPage() {
    return newPage;
  }

  public void setNewPage(boolean newPage) {
    this.newPage = newPage;
  }

  public long getId() {
    return id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public List<DraftPageAttachmentEntity> getAttachments() {
    return attachments;
  }

  public void setAttachments(List<DraftPageAttachmentEntity> attachments) {
    this.attachments = attachments;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }
}
