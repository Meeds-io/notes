/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.Page;
import org.exoplatform.portal.mop.navigation.NodeContext;
import org.exoplatform.portal.mop.navigation.NodeData;
import org.exoplatform.portal.mop.navigation.NodeState;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.portal.mop.service.NavigationService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.services.security.MembershipEntry;

import io.meeds.layout.service.NavigationLayoutService;

import org.exoplatform.wiki.service.search.PageSearchResult;

/**
 * Query-time connector for the "page" unified-search content type: queries
 * the {@code page_content_alias} ES index (populated by
 * {@link PageContentIndexingServiceConnector}) and resolves each hit's
 * clickable URL (first navigation node found pointing to the page, if any).
 */
public class PageSearchConnector {

  private static final Log             LOGGER                   = ExoLogger.getExoLogger(PageSearchConnector.class);

  private static final String          SEARCH_QUERY_FILE_PATH_PARAM = "query.file.path";

  private static final String          PAGE_STORAGE_ID_PREFIX   = "page_";

  private final ConfigurationManager   configurationManager;

  private final ElasticSearchingClient client;

  private final String                 index;

  private String                       searchQueryFilePath;

  private String                       searchQuery;

  public PageSearchConnector(ConfigurationManager configurationManager,
                             ElasticSearchingClient client,
                             InitParams initParams) {
    this.configurationManager = configurationManager;
    this.client = client;

    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.index = param.getProperty("index");
    if (initParams.containsKey(SEARCH_QUERY_FILE_PATH_PARAM)) {
      this.searchQueryFilePath = initParams.getValueParam(SEARCH_QUERY_FILE_PATH_PARAM).getValue();
      try {
        retrieveSearchQuery();
      } catch (Exception e) {
        LOGGER.error("Can't read elasticsearch search query from path {}", searchQueryFilePath, e);
      }
    }
  }

  public List<PageSearchResult> search(String keyword, int offset, int limit) {
    if (StringUtils.isBlank(keyword)) {
      return List.of();
    }
    String esQuery = retrieveSearchQuery().replace("@keyword@", StringUtils.replace(keyword, "\"", "\\\""))
                                          .replace("@offset@", String.valueOf(Math.max(offset, 0)))
                                          .replace("@limit@", String.valueOf(Math.max(limit, 1)))
                                          .replace("@permissions_filter@", buildPermissionsFilter());
    String jsonResponse = client.sendRequest(esQuery, index);
    List<PageSearchResult> results = parseResults(jsonResponse);
    results.forEach(result -> result.setUrl(resolveUrl(result.getId())));
    return results;
  }

  private String buildPermissionsFilter() {
    Identity identity = ConversationState.getCurrent().getIdentity();
    StringBuilder filter = new StringBuilder();
    filter.append("{\"term\":{\"permissions\":\"").append(identity.getUserId()).append("\"}},")
          .append("{\"term\":{\"permissions\":\"").append(IdentityConstants.ANY).append("\"}},")
          .append("{\"term\":{\"permissions\":\"").append(UserACL.EVERYONE).append("\"}}");
    Set<String> memberships = new HashSet<>();
    if (identity.getMemberships() != null) {
      for (MembershipEntry entry : identity.getMemberships()) {
        String value = entry.toString();
        if (MembershipEntry.ANY_TYPE.equals(entry.getMembershipType())) {
          value = value.replace("*", ".*");
        }
        memberships.add(value);
      }
    }
    if (!memberships.isEmpty()) {
      filter.append(",{\"regexp\":{\"permissions\":\"")
            .append(StringUtils.join(memberships, "|"))
            .append("\"}}");
    }
    return filter.toString();
  }

  @SuppressWarnings("rawtypes")
  private List<PageSearchResult> parseResults(String jsonResponse) {
    List<PageSearchResult> results = new ArrayList<>();
    JSONParser parser = new JSONParser();
    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }
    JSONObject hitsWrapper = (JSONObject) json.get("hits");
    if (hitsWrapper == null) {
      return results;
    }
    JSONArray hits = (JSONArray) hitsWrapper.get("hits");
    if (hits == null) {
      return results;
    }
    for (Object hitObj : hits) {
      try {
        JSONObject hit = (JSONObject) hitObj;
        JSONObject source = (JSONObject) hit.get("_source");
        PageSearchResult result = new PageSearchResult();
        result.setId((String) hit.get("_id"));
        result.setTitle((String) source.get("title"));
        result.setSiteName((String) source.get("siteName"));
        result.setAuthor((String) source.get("author"));
        String date = (String) source.get("date");
        result.setDate(StringUtils.isBlank(date) ? 0 : Long.parseLong(date));
        result.setExcerpt(extractExcerpt(hit, source));
        results.add(result);
      } catch (Exception e) {
        LOGGER.warn("Error processing page search result item, ignore it from results", e);
      }
    }
    return results;
  }

  @SuppressWarnings("rawtypes")
  private String extractExcerpt(JSONObject hit, JSONObject source) {
    JSONObject highlight = (JSONObject) hit.get("highlight");
    if (highlight != null) {
      JSONArray contentFragments = (JSONArray) highlight.get("content");
      if (contentFragments != null && !contentFragments.isEmpty()) {
        return StringUtils.join(contentFragments, " ... ");
      }
    }
    String content = (String) source.get("content");
    return StringUtils.isBlank(content) ? "" : StringUtils.abbreviate(content, 150);
  }

  /**
   * Resolves the portal URL of the page identified by its storage id, using
   * the first navigation node found pointing to it. Returns {@code null}
   * when the page can't be resolved or no navigation node points to it.
   */
  private String resolveUrl(String storageId) {
    try {
      long numericId = Long.parseLong(StringUtils.removeStart(storageId, PAGE_STORAGE_ID_PREFIX));
      Page page = getLayoutService().getPage(numericId);
      if (page == null) {
        return null;
      }
      PageKey pageKey = page.getPageKey();
      NodeContext<NodeContext<Object>> root = getNavigationService().loadNode(pageKey.getSite());
      NodeData node = findNodeByPage(root, pageKey);
      // getNodeUri() returns the path relative to the portal context (e.g.
      // used as `/portal${uri}` by PagePreviewButton.vue in Layout) - the
      // "/portal" prefix must be added here, it is not part of the URI itself.
      return node == null ? null : "/portal" + getNavigationLayoutService().getNodeUri(node);
    } catch (Exception e) {
      LOGGER.debug("Cannot resolve a navigation node/url for page with storage id {}", storageId, e);
      return null;
    }
  }

  private NodeData findNodeByPage(NodeContext<NodeContext<Object>> node, PageKey pageKey) {
    if (node == null) {
      return null;
    }
    NodeState state = node.getState();
    if (state != null && pageKey.equals(state.getPageRef())) {
      return node.getData();
    }
    int count = node.getNodeCount();
    for (int i = 0; i < count; i++) {
      NodeData found = findNodeByPage(node.get(i), pageKey);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  private String retrieveSearchQuery() {
    if (StringUtils.isBlank(this.searchQuery)) {
      try {
        InputStream queryFileIS = this.configurationManager.getInputStream(searchQueryFilePath);
        this.searchQuery = IOUtil.getStreamContentAsString(queryFileIS);
      } catch (Exception e) {
        throw new IllegalStateException("Error retrieving search query from file: " + searchQueryFilePath, e);
      }
    }
    return this.searchQuery;
  }

  private LayoutService getLayoutService() {
    return CommonsUtils.getService(LayoutService.class);
  }

  private NavigationService getNavigationService() {
    return CommonsUtils.getService(NavigationService.class);
  }

  private NavigationLayoutService getNavigationLayoutService() {
    return CommonsUtils.getService(NavigationLayoutService.class);
  }

}
