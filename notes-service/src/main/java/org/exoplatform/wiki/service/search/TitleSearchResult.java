/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
import lombok.NoArgsConstructor;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.rest.entity.IdentityEntity;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
public class TitleSearchResult {
  private String                          title;

  private String                          id;

  private String                          activityId;

  private IdentityEntity                  poster;

  private IdentityEntity                  wikiOwner;

  private String                          excerpt;

  private long                            createdDate;

  private SearchResultType                type;

  private String                          url;

  private Map<String, List<MetadataItem>> metadatas;

  private String                          lang;

}
