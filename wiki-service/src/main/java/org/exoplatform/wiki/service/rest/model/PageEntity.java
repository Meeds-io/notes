/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.wiki.mow.api.PermissionEntry;
import org.exoplatform.wiki.service.BreadcrumbData;

import java.util.Date;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PageEntity {

  private String id;

  private String name;

  private String owner;

  private String author;

  private Date createdDate;

  private Date updatedDate;

  private String content;

  private String syntax;

  private String title;

  private String comment;

  private List<PermissionEntry> permissions;

  private String url;

  private String activityId;

  private String wikiId;

  private String wikiType;

  private String wikiOwner;

  private String parentPageName;

  private String parentPageId;

  private boolean isMinorEdit;

  private boolean canEdit;

  private boolean toBePublished;

  private List<BreadcrumbData> breadcrumb;


  @Override
  public PageEntity clone() {
    return new PageEntity(id,name,owner,author,createdDate,updatedDate,content,syntax,title,comment,permissions,url,activityId,wikiId,wikiType,wikiOwner,parentPageName,parentPageId,isMinorEdit,canEdit,toBePublished, breadcrumb);
  }
}
