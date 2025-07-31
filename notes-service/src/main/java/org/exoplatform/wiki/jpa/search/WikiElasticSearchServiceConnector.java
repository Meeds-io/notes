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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.social.core.space.SpaceUtils;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import org.exoplatform.commons.search.es.ElasticSearchException;
import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.commons.utils.IOUtil;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.PropertyManager;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.identity.provider.SpaceIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.legacy.search.es.ElasticSearchServiceConnector;

/**
 * Created by The eXo Platform SAS Author : Thibault Clement
 * tclement@exoplatform.com 11/24/15
 */
public class WikiElasticSearchServiceConnector extends ElasticSearchServiceConnector {

  private static final Log           LOG                                     =
                                         ExoLogger.getLogger(WikiElasticSearchServiceConnector.class);

  private static final String        SEARCH_QUERY_FILE_PATH_PARAM            = "query.file.path";

  private final IdentityManager      identityManager;

  private final ConfigurationManager configurationManager;

  private final SpaceService         spaceService;

  private String                     searchQuery;

  private String                     searchQueryFilePath;

  public static final String         SEARCH_QUERY_TERM                       = """
      ,"must":{
        "query_string":{
          "fields": ["name","title","content","comment"],
          "default_operator": "AND",
          "query": "@term@"
        }
      }
      """;

  public static final String         SEARCH_QUERY_TERM_FOR_NOTES_APPLICATION = """
      ,"must": [
        @wildcard_query@
      ]
      """;

  public static final String         WILDCARD_QUERY                          = """
      {
        "wildcard": {
          "title": "*@term@*"
        }
      }
      """;

  public static final String         DEFAULT_SORTING_QUERY                   = """
          {
            "_score": {
              "order": "desc"
            }
          }
      """;

  public static final String         SORTING_QUERY                           = """
          {
            "@sortField@": {
              "order": "@sortOrder@"
            }
          },
          "_score"
      """;

  public WikiElasticSearchServiceConnector(ConfigurationManager configurationManager,
                                           InitParams initParams,
                                           ElasticSearchingClient client,
                                           IdentityManager identityManager,
                                           SpaceService spaceService) {
    super(initParams, client);
    this.configurationManager = configurationManager;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
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

  public List<SearchResult> searchWiki(String searchedText, WikiSearchData wikiSearchData) {
      return filteredWikiSearch(searchedText, wikiSearchData);
  }

  protected List<SearchResult> filteredWikiSearch(String query, WikiSearchData wikiSearchData) {
    Set<String> ids = getUserSpaceIds(wikiSearchData.getUserId(), wikiSearchData.getWikiOwner());
    if (!CollectionUtils.isEmpty(wikiSearchData.getSpaceIds())) {
      List<String> spaceIdentityIds = SpaceUtils.getSpaceIdentityIds(wikiSearchData.getUserId(),wikiSearchData.getSpaceIds());
      ids.retainAll(spaceIdentityIds);
      if (ids.isEmpty()) {
        return Collections.emptyList();
      }
    }
    String esQuery = buildQueryStatement(ids, query, wikiSearchData);
    String jsonResponse = getClient().sendRequest(esQuery, getIndex());
    return buildWikiResult(jsonResponse);
  }

  private String buildTagsQueryStatement(List<String> values) {
    if (CollectionUtils.isEmpty(values)) {
      return "";
    }
    List<String> tagsQueryParts = values.stream()
            .map(value -> new StringBuilder().append("{\"term\": {\n")
                    .append("            \"metadatas.tags.metadataName.keyword\": {\n")
                    .append("              \"value\": \"")
                    .append(value)
                    .append("\",\n")
                    .append("              \"case_insensitive\":true\n")
                    .append("            }\n")
                    .append("          }}")
                    .toString())
            .collect(Collectors.toList());
    return new StringBuilder().append(",\"should\": [\n")
            .append(StringUtils.join(tagsQueryParts, ","))
            .append("      ],\n")
            .append("      \"minimum_should_match\": 1")
            .toString();
  }

  private String buildTermQuery(String termQuery, boolean isNotesTreeFilter) {
    if (StringUtils.isBlank(termQuery)) {
      return "";
    }

    termQuery = removeSpecialCharacters(termQuery);
    if (isNotesTreeFilter) {
      termQuery = termQuery.trim().replaceAll("\\s+", " ");
      List<String> listTermQuery = List.of(termQuery.split(" "));
      if (listTermQuery.size() == 0) {
        return "";
      }
      String allWildcardQuery = "";
      String termWildcardQuery = WILDCARD_QUERY.replace("@term@",listTermQuery.get(0));
      allWildcardQuery = allWildcardQuery.concat(termWildcardQuery);
      if (listTermQuery.size() > 1) {
        int index = 0;
        for(String term : listTermQuery) {
          if (index > 0){
            termWildcardQuery = WILDCARD_QUERY.replace("@term@", term);
            allWildcardQuery = allWildcardQuery.concat(",").concat(termWildcardQuery);
          }
          index++;
        };
      }
      return SEARCH_QUERY_TERM_FOR_NOTES_APPLICATION.replace("@wildcard_query@", allWildcardQuery);
    } else {
    List<String> termsQuery = Arrays.stream(termQuery.split(" ")).filter(StringUtils::isNotBlank).map(word -> {
      word = word.trim();
      word = word + "*";
      return word;
    }).toList();
      return SEARCH_QUERY_TERM.replace("@term@", StringUtils.join(termsQuery, " "));
    }
  }
  
  private String buildQueryStatement(Set<String> calendarOwnersOfUser,
                                     String term,
                                     WikiSearchData wikiSearchData) {
    term = removeSpecialCharacters(term);
    Map<String, List<String>> metadataFilters = buildMetadataFilter(wikiSearchData.isFavorites(), wikiSearchData.getUserId());
    String metadataQuery = buildMetadataQueryStatement(metadataFilters);
    String tagsQuery = buildTagsQueryStatement(wikiSearchData.getTagNames());
    String termsQuery = buildTermQuery(term, wikiSearchData.isNotesTreeFilter());
    String sortQuery = buildSortQueryStatement(wikiSearchData);
    return retrieveSearchQuery().replace("@term_query@", termsQuery)
                                .replace("@metadatas_query@", metadataQuery)
                                .replace("@tags_query@", tagsQuery)
                                .replace("@permissions@", StringUtils.join(calendarOwnersOfUser, ","))
                                .replace("@sortQuery@", sortQuery)
                                .replace("@offset@", String.valueOf(wikiSearchData.getOffset()))
                                .replace("@limit@", String.valueOf(wikiSearchData.getLimit()));
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
    string = string.replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
    string = string.replace("''", "'");
    if (!string.contains("\"\\")) {
      string = string.replace("\\-", "\\\"\\\\-\\\"");
    }
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

      String lang = (String) hitSource.get("lang");

      String wikiType = (String) hitSource.get("wikiType");
      String wikiOwner = (String) hitSource.get("wikiOwner");
      String owner = (String) hitSource.get("owner");
      String id = (String) hitSource.get("id");

      Calendar createdDate = Calendar.getInstance();
      createdDate.setTimeInMillis(Long.parseLong((String) hitSource.get("createdDate")));
      Calendar updatedDate = Calendar.getInstance();
      updatedDate.setTimeInMillis(Long.parseLong((String) hitSource.get("updatedDate")));

      SearchResultType type = SearchResultType.PAGE;
      String pageName = (String) hitSource.get("name");

      // Get the excerpt
      JSONObject hitHighlight = (JSONObject) ((JSONObject) jsonHit).get("highlight");
      StringBuilder excerpt = new StringBuilder();
      if (hitHighlight != null) {
        Iterator<?> keys = hitHighlight.keySet().iterator();
        List<String> allHighlights = new ArrayList<>();

        while (keys.hasNext()) {
          String key = (String) keys.next();
          JSONArray highlights = (JSONArray) hitHighlight.get(key);
          for (Object highlight : highlights) {
            allHighlights.add(highlight.toString());
          }
        }

        for (int i = 0; i < allHighlights.size(); i++) {
          excerpt.append(allHighlights.get(i));
          if (i < allHighlights.size() - 1) {
            excerpt.append(" ... ");
          }
        }
      }

      // Create the wiki search result
      SearchResult wikiSearchResult = new SearchResult();
      if (StringUtils.isNumeric(id)) {
        wikiSearchResult.setId(Long.parseLong(id));
      }
      wikiSearchResult.setLang(lang);
      wikiSearchResult.setWikiType(wikiType);
      wikiSearchResult.setWikiOwner(wikiOwner);
      wikiSearchResult.setPageName(pageName);

      // replace HTML tag for indexing page
      String content = Utils.html2text(excerpt.toString());
      wikiSearchResult.setExcerpt(content);
      wikiSearchResult.setTitle(title);
      wikiSearchResult.setType(type);
      wikiSearchResult.setCreatedDate(createdDate);
      wikiSearchResult.setUpdatedDate(updatedDate);
      wikiSearchResult.setUrl(url);
      wikiSearchResult.setScore(score);

      if (wikiOwner != null && wikiOwner.startsWith("spaces/")) {
        Space space = spaceService.getSpaceByGroupId(wikiOwner.startsWith("/") ? wikiOwner : "/".concat(wikiOwner));
        if (space != null) {
          Identity wikiOwnerIdentity = identityManager.getOrCreateSpaceIdentity(space.getPrettyName());
          wikiSearchResult.setWikiOwnerIdentity(wikiOwnerIdentity);
        }
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

  protected Set<String> getUserSpaceIds(String userId, String wikiOwner) {
    if (StringUtils.isEmpty(userId)) {
      throw new IllegalStateException("No Identity found: userId is empty");
    }  else {
      Set<String> permissions = new HashSet<>();
      ListAccess<Space> userSpaces = spaceService.getMemberSpaces(userId);
      List<Space> spaceList = new ArrayList<>();
      try {
        spaceList = Arrays.asList(userSpaces.load(0, userSpaces.getSize()));
      } catch (Exception e) {
        LOG.warn("Can't get user space Ids");
      }
      for (Space space : spaceList) {
        if (space != null) {
          if (wikiOwner == null || space.getGroupId().equals(wikiOwner)) {
            permissions.add(identityManager.getOrCreateIdentity(SpaceIdentityProvider.NAME, space.getPrettyName()).getId());
            if (wikiOwner != null) {
              break;  
            }
          }
        }
      }
      Identity userIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
              userId);
      if (userId != null) {
        permissions.add(userIdentity.getId());
      }
      return permissions;
    }

  }

  public void setSearchQuery(String searchQuery){
    this.searchQuery=searchQuery;
  }

  private String buildMetadataQueryStatement(Map<String, List<String>> metadataFilters) {
    StringBuilder metadataQuerySB = new StringBuilder();
    Set<Map.Entry<String, List<String>>> metadataFilterEntries = metadataFilters.entrySet();
    for (Map.Entry<String, List<String>> metadataFilterEntry : metadataFilterEntries) {
      metadataQuerySB.append("{\"terms\":{\"metadatas.")
                     .append(metadataFilterEntry.getKey())
                     .append(".metadataName.keyword")
                     .append("\": [\"")
                     .append(StringUtils.join(metadataFilterEntry.getValue(), "\",\""))
                     .append("\"]}},");
    }
    return metadataQuerySB.toString();
  }

  private Map<String, List<String>> buildMetadataFilter(boolean isFavorites, String userId) {
    Identity viewerIdentity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME, userId);
    Map<String, List<String>> metadataFilters = new HashMap<>();
    if (isFavorites) {
      metadataFilters.put(FavoriteService.METADATA_TYPE.getName(), Collections.singletonList(viewerIdentity.getId()));
    }
    return metadataFilters;
  }

  private String buildSortQueryStatement(WikiSearchData wikiSearchData) {
    String sortFiled = wikiSearchData.getSortField();
    String sortDirection = wikiSearchData.getSortDirection();

    if (StringUtils.isBlank(sortFiled)) {
      return DEFAULT_SORTING_QUERY;
    }
    return switch (sortFiled) {
      case "date" -> SORTING_QUERY.replace("@sortField@", "lastUpdatedDate").replace("@sortOrder@", sortDirection);
      default -> SORTING_QUERY.replace("@sortField@", sortFiled).replace("@sortOrder@", sortDirection);
    };
  }

}
