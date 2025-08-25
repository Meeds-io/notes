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

import java.util.Calendar;
import java.util.List;
import java.util.Map;

import lombok.Data;
import org.exoplatform.commons.utils.HTMLSanitizer;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.metadata.model.MetadataItem;

@Data
public class SearchResult {
  private static Log                        log = ExoLogger.getLogger(SearchResult.class);

  protected Long                            id;

  protected String                          wikiType;

  protected String                          wikiOwner;

  protected Identity                        wikiOwnerIdentity;

  protected Identity                        poster;

  protected String                          pageName;

  protected String                          excerpt;

  protected String                          title;

  protected SearchResultType                type;

  protected String                          url;

  protected long                            score;

  protected Calendar                        updatedDate;

  protected Calendar                        createdDate;

  protected Map<String, List<MetadataItem>> metadata;

  protected String                          lang;

  public SearchResult() {
  }

  public SearchResult(String wikiType,
                      String wikiOwner,
                      String pageName,
                      String excerpt,
                      String title,
                      SearchResultType type,
                      Calendar updatedDate,
                      Calendar createdDate) {
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
    this.pageName = pageName;
    this.excerpt = excerpt;
    this.title = title;
    this.type = type;
    this.updatedDate = updatedDate;
    this.createdDate = createdDate;
  }

  public SearchResult(String wikiType,
                      Identity poster,
                      Identity wikiOwnerIdentity,
                      String pageName,
                      String excerpt,
                      String title,
                      SearchResultType type,
                      Calendar updatedDate,
                      Calendar createdDate) {
    this.wikiType = wikiType;
    this.poster = poster;
    this.wikiOwnerIdentity = wikiOwnerIdentity;
    this.pageName = pageName;
    this.excerpt = excerpt;
    this.title = title;
    this.type = type;
    this.updatedDate = updatedDate;
    this.createdDate = createdDate;
  }

  public String getExcerpt() {
    try {
      return HTMLSanitizer.sanitize(excerpt);
    } catch (Exception e) {

      log.error("Fail to sanitize input [" + excerpt + "], " + e.getMessage(), e);

    }
    return "";
  }
}
