/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

package org.exoplatform.wiki.jpa.search;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.search.DocumentWithMetadata;
import org.exoplatform.social.metadata.MetadataService;

import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

import java.util.*;

public class NoteVersionLanguageIndexingServiceConnector extends WikiPageIndexingServiceConnector {

  public static final String TYPE = "note-version";

  private static final Log   LOG  = ExoLogger.getExoLogger(NoteVersionLanguageIndexingServiceConnector.class);

  private final NoteService  noteService;

  public NoteVersionLanguageIndexingServiceConnector(InitParams initParams,
                                                     NoteService noteService,
                                                     MetadataService metadataService) {
    super(initParams, noteService, metadataService);
    this.noteService = noteService;
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    String pageId = id.substring(0, id.indexOf("-"));
    String lang = id.substring(id.indexOf("-") + 1);
    PageVersion pageVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(pageId), lang);

    if (pageVersion == null) {
      LOG.warn("The version language with id {} wasn't found, thus it can't be indexed", id);
      return null;
    }
    Page page = pageVersion.getParent();

    try {
      Map<String, String> fields = new HashMap<>();
      fields.put("owner", pageVersion.getOwner());
      fields.put("name", page.getName());
      fields.put("id", pageVersion.getId());
      // Remove HTML tag when indexing wiki page
      fields.put("content", Utils.html2text(pageVersion.getContent()));
      fields.put("title", pageVersion.getTitle());
      fields.put("createdDate", String.valueOf(pageVersion.getCreatedDate().getTime()));
      fields.put("updatedDate", String.valueOf(pageVersion.getUpdatedDate().getTime()));
      fields.put("comment", pageVersion.getComment());
      fields.put("wikiType", page.getWikiType());
      fields.put("wikiOwner", Utils.validateWikiOwner(page.getWikiType(), page.getWikiOwner()));
      fields.put("lang", lang);
      DocumentWithMetadata document = new DocumentWithMetadata();
      document.setId(id);
      String url = Utils.getPageUrl(page) + "?translation=" + pageVersion.getLang();
      document.setUrl(url);
      document.setLastUpdatedDate(pageVersion.getUpdatedDate());
      document.setPermissions(computePermissions(page));
      document.setFields(fields);
      addDocumentMetadata(document, id);
      return document;
    } catch (Exception e) {
      LOG.info("Cannot index page with id {} ", id, e);
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

}
