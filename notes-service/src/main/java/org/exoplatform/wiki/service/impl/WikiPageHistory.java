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
package org.exoplatform.wiki.service.impl;

import org.exoplatform.wiki.service.WikiPageParams;

public class WikiPageHistory {
  private WikiPageParams pageParams;
  
  private String username;
  
  private long editTime;
 
  private String draftName;
  
  private boolean isNewPage;

  public WikiPageHistory(WikiPageParams pageParams, String username, String draftName, boolean isNewPage) {
    this.pageParams = pageParams;
    this.username = username;
    this.draftName = draftName;
    this.isNewPage = isNewPage;
  }

  public WikiPageParams getPageParams() {
    return pageParams;
  }

  public String getUsername() {
    return username;
  }

  public long getEditTime() {
    return editTime;
  }
  
  public void setEditTime(long updateTime) {
    this.editTime = updateTime;
  }

  public String getDraftName() {
    return draftName;
  }

  public boolean isNewPage() {
    return isNewPage;
  }
}
