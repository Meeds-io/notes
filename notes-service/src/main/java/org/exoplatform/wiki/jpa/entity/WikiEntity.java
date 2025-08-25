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
package org.exoplatform.wiki.jpa.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OneToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;

@Entity(name = "WikiWikiEntity")
@Table(name = "WIKI_WIKIS")
@NamedQuery(name = "wiki.getAllIds", query = "SELECT w.id FROM WikiWikiEntity w ORDER BY w.id")
@NamedQuery(name = "wiki.getWikisByType", query = "SELECT w FROM WikiWikiEntity w WHERE w.type = :type")
@NamedQuery(name = "wiki.getWikiByTypeAndOwner", query = "SELECT w FROM WikiWikiEntity w WHERE w.type = :type AND w.owner = :owner")
public class WikiEntity {
  @Id
  @Column(name = "WIKI_ID")
  @SequenceGenerator(name="SEQ_WIKI_WIKIS_WIKI_ID", sequenceName="SEQ_WIKI_WIKIS_WIKI_ID", allocationSize = 1)
  @GeneratedValue(strategy=GenerationType.AUTO, generator="SEQ_WIKI_WIKIS_WIKI_ID")
  private long id;

  @Column(name = "NAME")
  private String name;

  @Column(name = "OWNER")
  private String owner;

  @Column(name = "TYPE")
  private String type;

  @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
  @JoinColumn(name = "WIKI_HOME")
  private PageEntity wikiHome;

  @Column(name = "SYNTAX")
  private String syntax;

  @Column(name = "ALLOW_MULTI_SYNTAX")
  private boolean allowMultipleSyntax;

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public WikiEntity setName(String name) {
    this.name = name;
    return this;
  }

  public String getOwner() {
    return owner;
  }

  public WikiEntity setOwner(String owner) {
    this.owner = owner;
    return this;
  }

  public String getType() {
    return type;
  }

  public WikiEntity setType(String type) {
    this.type = type;
    return this;
  }

  public PageEntity getWikiHome() {
    return wikiHome;
  }

  public WikiEntity setWikiHome(PageEntity wikiHome) {
    this.wikiHome = wikiHome;
    return this;
  }

  public String getSyntax() {
    return syntax;
  }

  public void setSyntax(String syntax) {
    this.syntax = syntax;
  }

  public boolean isAllowMultipleSyntax() {
    return allowMultipleSyntax;
  }

  public void setAllowMultipleSyntax(boolean allowMultipleSyntax) {
    this.allowMultipleSyntax = allowMultipleSyntax;
  }
}
