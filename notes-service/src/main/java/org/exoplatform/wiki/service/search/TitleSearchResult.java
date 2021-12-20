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

import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.rest.entity.IdentityEntity;

import java.util.List;
import java.util.Map;

public class TitleSearchResult {
  private String title;

  private String id;

  private String activityId;

  private IdentityEntity poster;

  private IdentityEntity wikiOwner;

  private String excerpt;

  private long createdDate;

  private SearchResultType type;

  private String url;

  private Map<String, List<MetadataItem>> metadatas;

  public TitleSearchResult() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getActivityId() {
    return activityId;
  }

  public void setActivityId(String activityId) {
    this.activityId = activityId;
  }

  public SearchResultType getType() {
    return type;
  }

  public void setType(SearchResultType type) {
    this.type = type;
  }
  
  public void setTitle(String title) {
    this.title = title;
  }
  
  public String getTitle() {
    return title;
  }
  
  public void setUrl(String url) {
    this.url = url;
  }
  
  public String getUrl() {
    return url;
  }

  public String getExcerpt() {
    return excerpt;
  }

  public void setExcerpt(String excerpt) {
    this.excerpt = excerpt;
  }

  public IdentityEntity getWikiOwner() {
    return wikiOwner;
  }

  public void setWikiOwner(IdentityEntity wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public IdentityEntity getPoster() {
    return poster;
  }

  public void setPoster(IdentityEntity poster) {
    this.poster = poster;
  }

  public long getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(long createdDate) {
    this.createdDate = createdDate;
  }

  public Map<String, List<MetadataItem>> getMetadatas() {
    return metadatas;
  }

  public void setMetadatas(Map<String, List<MetadataItem>> metadatas) {
    this.metadatas = metadatas;
  }
}
