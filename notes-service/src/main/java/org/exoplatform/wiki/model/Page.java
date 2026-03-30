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
package org.exoplatform.wiki.model;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import io.meeds.notes.model.NotePageProperties;
import lombok.AllArgsConstructor;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.service.BreadcrumbData;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Page{

  private String                          id;

  private String                          name;

  private String                          owner;

  private String                          author;

  private String                          authorFullName;

  private Date                            createdDate;

  private Date                            updatedDate;

  private String                          content;

  private String                          syntax;

  private String                          title;

  private String                          comment;

  private String                          url;

  private String                          activityId;

  private String                          wikiId;

  private String                          wikiType;

  private String                          wikiOwner;

  private String                          parentPageName;

  private String                          parentPageId;

  private String                          appName;

  private boolean                         isMinorEdit;

  private boolean                         isDraftPage = isDraftPage();

  private boolean                         toBePublished;

  private List<BreadcrumbData>            breadcrumb;

  private boolean                         canManage;

  private boolean                         canView;

  private boolean                         canImport;

  private List<Page>                      children;

  private boolean                         hasChild;

  private String                          latestVersionId;

  private boolean                         isDeleted;

  private Page                            parent;

  private Map<String, List<MetadataItem>> metadatas;

  private String                          lang;

  private String                          lastUpdater;

  private NotePageProperties              properties;

  private String                          attachmentObjectType;

  private Integer                         position;

  public Page(String name) {
    this.name = name;
  }

  public Page(String name, String title) {
    this();
    this.name = name;
    this.title = title;
  }

  public boolean isDraftPage() {
    return false;
  }

  public String getAttachmentObjectType() {
    return attachmentObjectType != null ? attachmentObjectType : "wikiPage";
  }

  public Page(Page other) {
    this.id = other.id;
    this.name = other.name;
    this.owner = other.owner;
    this.author = other.author;
    this.authorFullName = other.authorFullName;
    this.createdDate = other.createdDate;
    this.updatedDate = other.updatedDate;
    this.content = other.content;
    this.syntax = other.syntax;
    this.title = other.title;
    this.comment = other.comment;

    this.url = other.url;
    this.activityId = other.activityId;
    this.wikiId = other.wikiId;
    this.wikiType = other.wikiType;
    this.wikiOwner = other.wikiOwner;
    this.parentPageName = other.parentPageName;
    this.parentPageId = other.parentPageId;
    this.appName = other.appName;
    this.isMinorEdit = other.isMinorEdit;
    this.isDraftPage = other.isDraftPage;
    this.toBePublished = other.toBePublished;
    this.lastUpdater = other.lastUpdater;

    // Shallow copy
    this.breadcrumb = other.breadcrumb != null ? new ArrayList<>(other.breadcrumb) : null;

    this.canManage = other.canManage;
    this.canView = other.canView;
    this.canImport = other.canImport;

    // Shallow copy
    this.children = other.children != null ? new ArrayList<>(other.children) : null;

    this.hasChild = other.hasChild;
    this.latestVersionId = other.latestVersionId;
    this.isDeleted = other.isDeleted;

    // Shallow copy
    this.parent = other.parent;

    // Shallow copy
    this.metadatas = other.metadatas != null ? new HashMap<>(other.metadatas) : null;

    this.lang = other.lang;

    // Shallow copy
    this.properties = other.properties;
    this.attachmentObjectType = other.attachmentObjectType;

    this.position = other.position;
  }
}
