/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wiki.service.search;

import lombok.Data;
import org.exoplatform.wiki.utils.Utils;

import java.util.List;

@Data
public class SearchData {
  public String title;

  public String content;

  public String wikiType;

  public String wikiOwner;

  public String userId;

  public String pageId;
  
  private long offset = 0;

  private boolean isFavorites;
  
  protected String sort;
  
  protected String order;
  
  public int limit = Integer.MAX_VALUE;

  private List<String> tagNames;

  public SearchData(String title, String content, String wikiType, String wikiOwner, String pageId, String userId) {
    this.title = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(title);
    this.content = org.exoplatform.wiki.utils.Utils.escapeIllegalCharacterInQuery(content);
    this.wikiType = wikiType;
    this.wikiOwner = Utils.validateWikiOwner(wikiType, wikiOwner);
    this.pageId = pageId;
    this.userId = userId;
  }
}
