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

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

/**
 * Created by The eXo Platform SAS
 * Author : eXoPlatform
 * exo@exoplatform.com
 * 7/16/15
 */
@Entity(name = "WikiTemplateEntity")
@Table(name = "WIKI_TEMPLATES")
@NamedQueries({
        @NamedQuery(name = "template.getTemplatesOfWiki", query = "SELECT t FROM WikiTemplateEntity t JOIN t.wiki w WHERE w.type = :type AND w.owner = :owner"),
        @NamedQuery(name = "template.getTemplateOfWikiByName", query = "SELECT t FROM WikiTemplateEntity t JOIN t.wiki w WHERE t.name = :name AND w.type = :type AND w.owner = :owner"),
        @NamedQuery(name = "template.searchTemplatesByTitle", query = "SELECT t FROM WikiTemplateEntity t JOIN t.wiki w WHERE w.type = :type AND w.owner = :owner AND t.title like :searchText"),
        @NamedQuery(name = "template.getAllTemplatesBySyntax", query = "SELECT t FROM WikiTemplateEntity t WHERE t.syntax = :syntax OR t.syntax IS NULL ORDER BY t.updatedDate DESC"),
        @NamedQuery(name = "template.countAllTemplatesBySyntax", query = "SELECT COUNT(t) FROM WikiTemplateEntity t WHERE t.syntax = :syntax OR t.syntax IS NULL")
})
public class TemplateEntity extends BasePageEntity {

  @Id
  @SequenceGenerator(name="SEQ_WIKI_TEMPLATES_TEMPLATE_ID", sequenceName="SEQ_WIKI_TEMPLATES_TEMPLATE_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_TEMPLATES_TEMPLATE_ID")
  @Column(name = "TEMPLATE_ID")
  private long id;

  @Column(name = "DESCRIPTION")
  private String description;

  @ManyToOne
  @JoinColumn(name = "WIKI_ID")
  private WikiEntity wiki;

  public long getId() {
    return id;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public WikiEntity getWiki() {
    return wiki;
  }

  public void setWiki(WikiEntity wiki) {
    this.wiki = wiki;
  }
}
