/*
 *
 *  * Copyright (C) 2003-2015 eXo Platform SAS.
 *  *
 *  * This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Affero General Public License
 *  as published by the Free Software Foundation; either version 3
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see<http://www.gnu.org/licenses/>.
 *
 */

package org.exoplatform.wiki.jpa.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.utils.Utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by The eXo Platform SAS Author : eXoPlatform exo@exoplatform.com
 * 9/9/15
 */
public class WikiPageIndexingServiceConnector extends ElasticIndexingServiceConnector {

  public static final String    TYPE   = "wiki-page";

  private static final Log      LOGGER = ExoLogger.getExoLogger(WikiPageIndexingServiceConnector.class);

  private final PageDAO         dao;

  private final MetadataService metadataService;

  public WikiPageIndexingServiceConnector(InitParams initParams, PageDAO dao, MetadataService metadataService) {
    super(initParams);
    this.dao = dao;
    this.metadataService = metadataService;
  }

  @Override
  public String getMapping() {
    StringBuilder mapping = new StringBuilder()
                                               .append("{")
                                               .append("  \"properties\" : {\n")
                                               .append("    \"name\" : {")
                                               .append("      \"type\" : \"text\",")
                                               .append("      \"index_options\": \"offsets\",")
                                               .append("      \"fields\": {")
                                               .append("        \"raw\": {")
                                               .append("          \"type\": \"keyword\"")
                                               .append("        }")
                                               .append("      }")
                                               .append("    },\n")
                                               .append("    \"title\" : {")
                                               .append("      \"type\" : \"text\",")
                                               .append("      \"index_options\": \"offsets\",")
                                               .append("      \"fields\": {")
                                               .append("        \"raw\": {")
                                               .append("          \"type\": \"keyword\"")
                                               .append("        }")
                                               .append("      }")
                                               .append("    },\n")
                                               .append("    \"owner\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"id\" : {\"type\" : \"long\"},\n")
                                               .append("    \"wikiType\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"wikiOwner\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"permissions\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"url\" : {\"type\" : \"text\", \"index\": false},\n")
                                               .append("    \"sites\" : {\"type\" : \"keyword\"},\n")
                                               .append("    \"comment\" : {\"type\" : \"text\", \"index_options\": \"offsets\"},\n")
                                               .append("    \"content\" : {\"type\" : \"text\", \"store\": true, \"term_vector\": \"with_positions_offsets\"},\n")
                                               .append("    \"createdDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},\n")
                                               .append("    \"updatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},\n")
                                               .append("    \"lastUpdatedDate\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"}\n")
                                               .append("  }\n")
                                               .append("}");

    return mapping.toString();
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    // Get the Page object from BD
    PageEntity page = dao.find(Long.parseLong(id));
    if (page == null) {
      LOGGER.info("The page entity with id {} doesn't exist.", id);
      return null;
    }

    try {
      Map<String, String> fields = new HashMap<>();
      fields.put("owner", page.getOwner());
      fields.put("name", page.getName());
      fields.put("id", String.valueOf(page.getId()));
      // Remove HTML tag when indexing wiki page
      String content = Utils.html2text(page.getContent());
      fields.put("content", content);
      fields.put("title", page.getTitle());
      fields.put("createdDate", String.valueOf(page.getCreatedDate().getTime()));
      fields.put("updatedDate", String.valueOf(page.getUpdatedDate().getTime()));
      fields.put("comment", page.getComment());
      fields.put("wikiType", page.getWiki().getType());
      String wikiOwner = page.getWiki().getOwner();
      // We need to add the first "/" on the wiki owner if it's wiki group
      if (page.getWiki().getType().toUpperCase().equals(WikiType.GROUP.name())) {
        wikiOwner = dao.validateGroupWikiOwner(wikiOwner);
      }
      fields.put("wikiOwner", wikiOwner);
      DocumentWithMetadata document = new DocumentWithMetadata();
      document.setId(id);
      document.setUrl(page.getUrl());
      document.setLastUpdatedDate(page.getUpdatedDate());
      document.setPermissions(computePermissions(page));
      document.setFields(fields);
      addDocumentMetadata(document, Long.toString(page.getId()));

      return document;
    } catch (Exception e) {
      LOGGER.info("Cannot index page with id {} ", id, e);
      return null;
    }
  }


  @Override
  public Document update(String id) {
    return create(id);
  }

  @Override
  public String getConnectorName() {
    return TYPE;
  }

  private Set<String> computePermissions(PageEntity page) {
    IdentityManager identityManager    = CommonsUtils.getService(IdentityManager.class);
    Set<String> permissions = new HashSet<>();
    try {
      if (page.getWiki().getType().toUpperCase().equals(WikiType.GROUP.name())) {
        SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
        Space space = spaceService.getSpaceByGroupId(page.getWiki().getOwner());
        if(space!=null){
          permissions.add(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName()).getId());
        }
      }else if (page.getWiki().getType().toUpperCase().equals(WikiType.USER.name())) {
          permissions.add(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, page.getWiki().getOwner()).getId());
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot get Identity of the wiki Owner", e.getMessage());
    }
    return permissions;
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<String> result;

    List<Long> ids = this.dao.findAllIds(offset, limit);
    if (ids == null) {
      result = new ArrayList<>(0);
    } else {
      result = new ArrayList<>(ids.size());
      for (Long id : ids) {
        result.add(String.valueOf(id));
      }
    }
    return result;
  }

  private void addDocumentMetadata(DocumentWithMetadata document, String documentId) {
    MetadataObject metadataObject = new MetadataObject(Utils.NOTES_METADATA_OBJECT_TYPE, documentId);
    List<MetadataItem> metadataItems = metadataService.getMetadataItemsByObject(metadataObject);
    document.setMetadataItems(metadataItems);
  }
}
