/*
 * Copyright (C) 2003-2015 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/ .
 */
package org.exoplatform.wiki.jpa.search;

import java.io.InputStream;
import java.text.Normalizer;
import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.wiki.utils.Utils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;

/**
 * Created by The eXo Platform SAS Author : Thibault Clement
 * tclement@exoplatform.com 11/24/15
 */
public class WikiElasticSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log           LOG                          = ExoLogger.getLogger(WikiElasticSearchServiceConnector.class);

  private static final String        SEARCH_QUERY_FILE_PATH_PARAM = "query.file.path";

  private final IdentityManager      identityManager;

  private final ConfigurationManager configurationManager;

  private String                     searchQuery;

  private String                     searchQueryFilePath;

  public WikiElasticSearchServiceConnector(ConfigurationManager configurationManager,
                                           InitParams initParams,
                                           ElasticSearchingClient client,
                                           IdentityManager identityManager) {
    super(initParams, client);
    this.configurationManager = configurationManager;
    this.identityManager = identityManager;
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    if (initParams.containsKey(SEARCH_QUERY_FILE_PATH_PARAM)) {
      searchQueryFilePath = initParams.getValueParam(SEARCH_QUERY_FILE_PATH_PARAM).getValue();
      try {
        retrieveSearchQuery();
      } catch (Exception e) {
        LOG.error("Can't read elasticsearch search query from path {}", searchQueryFilePath, e);
      }
    }
  }

  @Override
  protected String getSourceFields() {

    List<String> fields = new ArrayList<>();
    fields.add("title");
    fields.add("url");
    fields.add("wikiType");
    fields.add("owner");
    fields.add("wikiOwner");
    fields.add("createdDate");
    fields.add("updatedDate");
    fields.add("name");
    fields.add("pageName");
    fields.add("content");

    List<String> sourceFields = new ArrayList<>();
    for (String sourceField : fields) {
      sourceFields.add("\"" + sourceField + "\"");
    }

    return StringUtils.join(sourceFields, ",");
  }

  public List<SearchResult> searchWiki(String searchedText, int offset, int limit) {
    List<SearchResult> searchResults = filteredWikiSearch(searchedText, offset, limit);
    return searchResults;
  }

  protected List<SearchResult> filteredWikiSearch(String query, int offset, int limit) {
    Set<String> ids = getUserSpaceIds();
    String esQuery = buildQueryStatement(ids, query, offset, limit);
    String jsonResponse = getClient().sendRequest(esQuery, getIndex());
    return buildWikiResult(jsonResponse);
  }

  private String buildQueryStatement(Set<String> calendarOwnersOfUser, String term, long offset, long limit) {
    term = removeSpecialCharacters(term);
    List<String> termsQuery = Arrays.stream(term.split(" ")).filter(StringUtils::isNotBlank).map(word -> {
      word = word.trim();
      if (word.length() > 5) {
        word = word + "~1";
      }
      return word;
    }).collect(Collectors.toList());
    String termQuery = StringUtils.join(termsQuery, " AND ");
    return retrieveSearchQuery().replace("@term@", term)
                                .replace("@term_query@", termQuery)
                                .replace("@permissions@", StringUtils.join(calendarOwnersOfUser, ","))
                                .replace("@offset@", String.valueOf(offset))
                                .replace("@limit@", String.valueOf(limit));
  }

  private String retrieveSearchQuery() {
    if (StringUtils.isBlank(this.searchQuery) || PropertyManager.isDevelopping()) {
      try {
        InputStream queryFileIS = this.configurationManager.getInputStream(searchQueryFilePath);
        this.searchQuery = IOUtil.getStreamContentAsString(queryFileIS);
      } catch (Exception e) {
        throw new IllegalStateException("Error retrieving search query from file: " + searchQueryFilePath, e);
      }
    }
    return this.searchQuery;
  }

  private String removeSpecialCharacters(String string) {
    string = Normalizer.normalize(string, Normalizer.Form.NFD);
    string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "").replace("'", " ");
    return string;
  }

  protected List<SearchResult> buildWikiResult(String jsonResponse) {

    List<SearchResult> wikiResults = new ArrayList<>();
    JSONParser parser = new JSONParser();
    Map json;
    try {
      json = (Map) parser.parse(jsonResponse);
    } catch (ParseException e) {
      throw new ElasticSearchException("Unable to parse JSON response", e);
    }

    JSONObject jsonResult = (JSONObject) json.get("hits");
    JSONArray jsonHits = (JSONArray) jsonResult.get("hits");

    for (Object jsonHit : jsonHits) {

      long score = ((Double) ((JSONObject) jsonHit).get("_score")).longValue();

      JSONObject hitSource = (JSONObject) ((JSONObject) jsonHit).get("_source");

      String title = (String) hitSource.get("title");
      String url = (String) hitSource.get("url");

      String wikiType = (String) hitSource.get("wikiType");
      String wikiOwner = (String) hitSource.get("wikiOwner");
      String owner = (String) hitSource.get("owner");

      Calendar createdDate = Calendar.getInstance();
      createdDate.setTimeInMillis(Long.parseLong((String) hitSource.get("createdDate")));
      Calendar updatedDate = Calendar.getInstance();
      updatedDate.setTimeInMillis(Long.parseLong((String) hitSource.get("updatedDate")));

      SearchResultType type = SearchResultType.PAGE;
      String pageName = (String) hitSource.get("name");
      String attachmentName = null;

      // Result can be an attachment
      if (((JSONObject) jsonHit).get("_type").equals("wiki-attachment")) {
        pageName = (String) hitSource.get("pageName");
        attachmentName = (String) hitSource.get("name");
      }

      // Get the excerpt
      JSONObject hitHighlight = (JSONObject) ((JSONObject) jsonHit).get("highlight");
      StringBuilder excerpt = new StringBuilder();
      if (hitHighlight != null) {
        Iterator<?> keys = hitHighlight.keySet().iterator();
        while (keys.hasNext()) {
          String key = (String) keys.next();
          JSONArray highlights = (JSONArray) hitHighlight.get(key);
          for (Object highlight : highlights) {
            excerpt.append("... ").append(highlight);
          }
        }
      }

      // Create the wiki search result
      SearchResult wikiSearchResult = new SearchResult();
      wikiSearchResult.setWikiType(wikiType);
      wikiSearchResult.setWikiOwner(wikiOwner);
      wikiSearchResult.setPageName(pageName);
      wikiSearchResult.setAttachmentName(attachmentName);

      // replace HTML tag for indexing page
      String content = Utils.html2text(excerpt.toString());
      wikiSearchResult.setExcerpt(content);
      wikiSearchResult.setTitle(title);
      wikiSearchResult.setType(type);
      wikiSearchResult.setCreatedDate(createdDate);
      wikiSearchResult.setUpdatedDate(updatedDate);
      wikiSearchResult.setUrl(url);
      wikiSearchResult.setScore(score);

      if (wikiOwner != null && wikiOwner.startsWith("/spaces/")) {
        String wikiOwnerPrettyName = wikiOwner.split("/spaces/")[1];
        Identity wikiOwnerIdentity = identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, wikiOwnerPrettyName, true);
        wikiSearchResult.setWikiOwnerIdentity(wikiOwnerIdentity);
      }

      if (owner != null) {
        Identity posterIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, owner);
        wikiSearchResult.setPoster(posterIdentity);
      }

      // Add the wiki search result to the list of search results
      wikiResults.add(wikiSearchResult);

    }

    return wikiResults;

  }

  protected Set<String> getUserSpaceIds() {
    ConversationState conversationState = ConversationState.getCurrent();
    if (conversationState == null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent() is null");
    } else if (conversationState.getIdentity() == null) {
      throw new IllegalStateException("No Identity found: ConversationState.getCurrent().getIdentity() is null");
    } else {
      Set<String> permissions = new HashSet<>();
      IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
      SpaceService spaceService = CommonsUtils.getService(SpaceService.class);
      ListAccess<Space> userSpaces = spaceService.getMemberSpaces(conversationState.getIdentity().getUserId());
      List<Space> spaceList = new ArrayList<>();
      try {
        spaceList = Arrays.asList(userSpaces.load(0, userSpaces.getSize()));
      } catch (Exception e) {
        LOG.warn("Can't get user space Ids");
      }
      for (Space space : spaceList) {
        if (space != null) {
          permissions.add(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName()).getId());
        }
      }
      Identity userId = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                            conversationState.getIdentity().getUserId());
      if (userId != null) {
        permissions.add(userId.getId());
      }
      return permissions;
    }

  }
  public void setSearchQuery(String searchQuery){
    this.searchQuery=searchQuery;
  }

}
