/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.entity;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity(name = "WikiPageEntity")
@Table(name = "WIKI_PAGES")
@NamedQuery(name = "wikiPage.getAllIds", query = "SELECT p.id FROM WikiPageEntity p  WHERE p.deleted = false ORDER BY p.id")
@NamedQuery(name = "wikiPage.countAllIds", query = "SELECT COUNT(*) FROM WikiPageEntity p  WHERE p.deleted = false")
@NamedQuery(name = "wikiPage.getPageOfWikiByName", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE p.name = :name AND w.type = :type AND w.owner = :owner AND p.deleted = false")
@NamedQuery(name = "wikiPage.getAllPagesOfWiki", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE w.type = :type AND w.owner = :owner")
@NamedQuery(name = "wikiPage.getPagesOfWiki", query = "SELECT p FROM WikiPageEntity p JOIN p.wiki w WHERE w.type = :type AND w.owner = :owner AND p.deleted = :deleted")
@NamedQuery(name = "wikiPage.getChildrenPages", query = "SELECT p FROM WikiPageEntity p WHERE p.parentPage.id = :id AND p.deleted = false ORDER BY p.name")
@NamedQuery(name = "wikiPage.getAllChildrenPages", query = "SELECT p FROM WikiPageEntity p WHERE p.parentPage.id = :id ORDER BY p.name")
@NamedQuery(name = "wikiPage.getRelatedPages", query = "SELECT p FROM WikiPageEntity p INNER JOIN p.relatedPages r where r.id = :pageId")
@NamedQuery(name = "wikiPage.getAllPagesBySyntax", query = "SELECT p FROM WikiPageEntity p WHERE p.syntax = :syntax OR p.syntax IS NULL ORDER BY p.updatedDate DESC")
@NamedQuery(name = "wikiPage.countPageChildrenById", query = "SELECT COUNT(*) FROM WikiPageEntity p WHERE p.parentPage.id = :id AND p.deleted = false")
@Data
@EqualsAndHashCode(callSuper = true)
public class PageEntity extends BasePageEntity {

  @Id
  @Column(name = "PAGE_ID")
  @SequenceGenerator(name="SEQ_WIKI_PAGES_PAGE_ID", sequenceName="SEQ_WIKI_PAGES_PAGE_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_PAGES_PAGE_ID")
  private long id;

  @ManyToOne
  @JoinColumn(name = "WIKI_ID")
  @EqualsAndHashCode.Exclude
  private WikiEntity wiki;

  @ManyToOne
  @JoinColumn(name = "PARENT_PAGE_ID")
  @EqualsAndHashCode.Exclude
  private PageEntity parentPage;

  @OneToMany(mappedBy = "parentPage")
  @EqualsAndHashCode.Exclude
  private List<PageEntity>        children;

  @OneToMany(mappedBy = "targetPage")
  @EqualsAndHashCode.Exclude
  private List<DraftPageEntity>   drafts;

  @OneToMany(mappedBy = "page")
  @EqualsAndHashCode.Exclude
  private List<PageVersionEntity> versions;

  @ManyToMany
  @JoinTable(
    name = "WIKI_PAGES_RELATED_PAGES",
    joinColumns = {@JoinColumn(name = "PAGE_ID")},
    inverseJoinColumns = {@JoinColumn(name = "RELATED_PAGE_ID")}
  )
  @EqualsAndHashCode.Exclude
  private List<PageEntity> relatedPages;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "EDITION_COMMENT")
  private String comment;

  @Column(name = "URL")
  private String url;

  @Column(name = "MINOR_EDIT")
  private boolean minorEdit;

  @Column(name = "ACTIVITY_ID")
  private String activityId;

  @ElementCollection
  @CollectionTable(
      name = "WIKI_WATCHERS",
      joinColumns=@JoinColumn(name = "PAGE_ID")
  )
  @Column(name="USERNAME")
  @EqualsAndHashCode.Exclude
  private Set<String> watchers = new HashSet<>();

  @OneToMany(mappedBy = "page", cascade = CascadeType.ALL)
  @EqualsAndHashCode.Exclude
  private List<PageMoveEntity> moves = new ArrayList<>();

  @Column(name = "DELETED")
  private boolean deleted;

}
