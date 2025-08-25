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
package org.exoplatform.wiki.service.search;

import lombok.Data;
import org.exoplatform.wiki.utils.Utils;

import java.util.List;

@Data
public class SearchData {
  public String        title;

  public String        content;

  public String        wikiType;

  public String        wikiOwner;

  public String        userId;

  public String        pageId;

  private long         offset = 0;

  private boolean      isFavorites;

  public int           limit  = Integer.MAX_VALUE;

  private List<String> tagNames;

  private List<String> spaceIds;

  private String       sortField;

  private String       sortDirection;

  private boolean      isNotesTreeFilter;

  public SearchData(String title, String content, String wikiType, String wikiOwner, String pageId, String userId) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
    this.wikiType = wikiType;
    this.wikiOwner = Utils.validateWikiOwner(wikiType, wikiOwner);
    this.pageId = pageId;
    this.userId = userId;
  }
}
