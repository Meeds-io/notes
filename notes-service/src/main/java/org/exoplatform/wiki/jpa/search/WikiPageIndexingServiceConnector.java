/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.jpa.search;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

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

  private final NoteService noteService;

  private final MetadataService metadataService;

  public WikiPageIndexingServiceConnector(InitParams initParams, NoteService noteService, MetadataService metadataService) {
    super(initParams);
    this.noteService = noteService;
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
    Page page = null;
    try {
      page = noteService.getNoteById(id);
    } catch (WikiException e) {
      LOGGER.error("Error while getting note with id {}", id, e);
    }
    
    if (page == null) {
      LOGGER.warn("The page with id {} wasn't found, thus it can't be indexed", id);
      return null;
    } else if (StringUtils.equalsIgnoreCase(page.getOwner(), IdentityConstants.SYSTEM)) {
      LOGGER.debug("The page with id {} is a system note pge, thus will not be searchable as a note", id);
      return null;
    }

    try {
      Map<String, String> fields = new HashMap<>();
      fields.put("owner", page.getOwner());
      fields.put("name", page.getName());
      fields.put("id", String.valueOf(page.getId()));
      // Remove HTML tag when indexing wiki page
      fields.put("content", Utils.html2text(page.getContent()));
      fields.put("title", page.getTitle());
      fields.put("createdDate", String.valueOf(page.getCreatedDate().getTime()));
      fields.put("updatedDate", String.valueOf(page.getUpdatedDate().getTime()));
      fields.put("comment", page.getComment());
      fields.put("wikiType", page.getWikiType());
      fields.put("wikiOwner", Utils.validateWikiOwner(page.getWikiType(), page.getWikiOwner()));
      DocumentWithMetadata document = new DocumentWithMetadata();
      document.setId(id);
      document.setUrl(Utils.getPageUrl(page));
      document.setLastUpdatedDate(page.getUpdatedDate());
      document.setPermissions(computePermissions(page));
      document.setFields(fields);
      addDocumentMetadata(document, page.getId());

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

  protected Set<String> computePermissions(Page page) {
    IdentityManager identityManager    = CommonsUtils.getService(IdentityManager.class);
    Set<String> permissions = new HashSet<>();
    try {
      if (page.getWikiType().toUpperCase().equals(WikiType.GROUP.name())) {
        SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
        Space space = spaceService.getSpaceByGroupId(page.getWikiOwner());
        if (space != null) {
          permissions.add(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName()).getId());
        }
      } else if (page.getWikiType().toUpperCase().equals(WikiType.USER.name())) {
        permissions.add(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, page.getWikiOwner()).getId());
      }
    } catch (Exception e) {
      LOGGER.warn("Cannot get Identity of the wiki Owner", e.getMessage());
    }
    return permissions;
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    throw new UnsupportedOperationException();
  }

  protected void addDocumentMetadata(DocumentWithMetadata document, String documentId) {
    MetadataObject metadataObject = new MetadataObject(Utils.NOTES_METADATA_OBJECT_TYPE, documentId);
    List<MetadataItem> metadataItems = metadataService.getMetadataItemsByObject(metadataObject);
    document.setMetadataItems(metadataItems);
  }
}
