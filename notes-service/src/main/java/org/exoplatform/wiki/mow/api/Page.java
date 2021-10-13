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
package org.exoplatform.wiki.mow.api;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.exoplatform.wiki.service.BreadcrumbData;

import java.util.Date;
import java.util.List;

@Data
@NoArgsConstructor
public class Page {

  private String id;

  private String name;

  private String owner;

  private String author;

  private String authorFullName;

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

  private String appName;

  private boolean isMinorEdit;

  private boolean isDraftPage = isDraftPage();

  private boolean toBePublished;

  private List<BreadcrumbData> breadcrumb;

  private boolean canManage;

  private boolean canView;

  private List<Page> children;

  private Page parent;

  public boolean isDraftPage() {
    return false;
  }

  public Page(String name) {
    this.name = name;
  }

  public Page(String name, String title) {
    this();
    this.name = name;
    this.title = title;
  }


}
