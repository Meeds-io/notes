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

package org.exoplatform.wiki.service.impl;

import org.apache.commons.lang.StringUtils;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.legacy.api.search.SearchServiceConnector;
import io.meeds.notes.legacy.api.search.data.SearchContext;
import io.meeds.notes.legacy.api.search.data.SearchResult;

import java.text.SimpleDateFormat;
import java.util.*;


/**
 *  Provides a connector service for the unified search.
 *  It implements a direct search based on several criteria.
 *
 * @LevelAPI Experimental
 */
public class WikiSearchServiceConnector extends SearchServiceConnector {
  
  private static final Log LOG = ExoLogger.getLogger("org.exoplatform.wiki.service.impl.WikiSearchServiceConnector");

  /**
   * URL pointing to the icon representing the wiki pages in the unified search results.
   */
  public static final String WIKI_PAGE_ICON = "/eXoSkin/skin/images/system/unified-search/PageIcon.png";

  /**
   * Date and time format used in the unified search.
   */
  public static String  DATE_TIME_FORMAT = "EEEEE, MMMMMMMM d, yyyy K:mm a";
  
  private WikiService wikiService;
  private NoteService noteService;

  /**
   * Initializes the Wiki search service.
   *
   * @param initParams The params object which is used for initializing the Wiki search service.
   */
  public WikiSearchServiceConnector(InitParams initParams) {
    super(initParams);
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    noteService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NoteService.class);
  }

    /**
     * Implements the search method by given criteria.
     *
     * @param context The search context.
     * @param query The query statement.
     * @param sites Specified sites where the search is performed (for example Acme, or Intranet).
     * @param offset The start point from which the search results are returned.
     * @param limit The limitation number of search results.
     * @param sort The sorting criteria (title, relevancy and date).
     * @param order The sorting order (ascending and descending).
     * @return Search results.
     */
  @Override
  public Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order) {
    // When limit is 0 then return all search result
    if (limit == 0) {
      limit = Integer.MAX_VALUE;
    }

    // Prepare search data
    WikiSearchData searchData = new WikiSearchData(query, query, null, null);
    searchData.setOffset(offset);
    searchData.setLimit(limit);
    
    // Execute the search
    List<SearchResult> searchResults = new ArrayList<SearchResult>();
    try {
      PageList<org.exoplatform.wiki.service.search.SearchResult> wikiSearchPageList = noteService.search(searchData);
      if (wikiSearchPageList != null) {
        List<org.exoplatform.wiki.service.search.SearchResult> wikiSearchResults = wikiSearchPageList.getAll();
        for (org.exoplatform.wiki.service.search.SearchResult wikiSearchResult : wikiSearchResults) {
          SearchResult searchResult = buildResult(context, wikiSearchResult);
          if (searchResult != null) {
            searchResults.add(searchResult);
          }
        }
      }
    } catch (Exception e) {
      LOG.info("Could not execute unified seach on wiki" , e) ; 
    }
    
    // Sort search result
    sortSearchResult(searchResults, sort, order);
    
    // Return the result
    return searchResults;
  }

  /**
   * Sorts search results by order and sorting criteria.
   *
   * @param searchResults The list of search results.
   * @param sort The sorting criteria, including title, relevancy and date.
   * @param order The sorting order, including ascending and descending.
   */
  private void sortSearchResult(List<SearchResult> searchResults, String sort, String order) {
    if (StringUtils.isEmpty(sort)) {
      return;
    }
    
    if (StringUtils.isEmpty(order)) {
      order = "ASC";
    }
    final String orderValue = order;
    
    if ("title".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            return o1.getTitle().compareTo(o2.getTitle());
          }
          return o2.getTitle().compareTo(o1.getTitle());
        }
      });
    } else if ("relevancy".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            return  Long.valueOf(o1.getRelevancy()).compareTo(Long.valueOf(o2.getRelevancy()));
          }
          return Long.valueOf(o2.getRelevancy()).compareTo(Long.valueOf(o1.getRelevancy()));
        }
      });
      
    } else if ("date".equalsIgnoreCase(sort)) {
      Collections.sort(searchResults, new Comparator<SearchResult>() {
        @Override
        public int compare(SearchResult o1, SearchResult o2) {
          if ("ASC".equalsIgnoreCase(orderValue)) {
            return Long.valueOf(o1.getDate()).compareTo(Long.valueOf(o2.getDate()));
          }
          return Long.valueOf(o2.getDate()).compareTo(Long.valueOf(o1.getDate()));
        }
      });
    } 
  }

  /**
   * Gets an URL to the icon representing the wiki pages.
   *
   * @param wikiSearchResult The search result of Wiki.
   * @return The icon URL.
   * @return URL of the icon.
   */
  private String getResultIcon(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    return WIKI_PAGE_ICON;
  }

  /**
   * Gets a wiki page by a given search result.
   *
   * @param result The search result of Wiki.
   * @return The wiki page.
   * @throws Exception
   */
  private Page getPage(org.exoplatform.wiki.service.search.SearchResult result) throws Exception {
    return wikiService.getPageOfWikiByName(result.getWikiType(), result.getWikiOwner(), result.getPageName());
  }

  /**
   * Gets a string which represents the wiki page containing the search result.
   * 
   * The string format is: <b>PageOwner - UpdateDate</b>.
   *
   * @param wikiSearchResult The search result of Wiki.
   * @return The string.
   */
  private String getPageDetail(org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    StringBuffer pageDetail = new StringBuffer();
    try {
      
      // Get space name
      Page page = getPage(wikiSearchResult);
      String spaceName;
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
      if (wiki.getType().equals(PortalConfig.GROUP_TYPE)) {
        String wikiOwner = wiki.getOwner();
        if (wikiOwner.indexOf('/') == -1) {
          spaceName = wikiService.getSpaceNameByGroupId("/spaces/" + wiki.getOwner());
        } else {
          spaceName = wikiService.getSpaceNameByGroupId(wiki.getOwner());
        }
      } else {
        spaceName = wiki.getOwner();
      }
      
      // Get update date
      Calendar updateDate = wikiSearchResult.getUpdatedDate();
      SimpleDateFormat format = new SimpleDateFormat(DATE_TIME_FORMAT);
      
      // Build page detail
      pageDetail.append(spaceName);
      pageDetail.append(" - ");
      pageDetail.append(format.format(updateDate.getTime()));
    } catch (Exception e) {
      LOG.info("Can not get page detail ", e);
    }
    return pageDetail.toString();
  }

  /**
   * Gets a permalink of the wiki page by a given search result.
   *
   * @param context The search context.
   * @param wikiSearchResult The search result of Wiki.
   * @return The wiki page permalink.
   */
  private String getPagePermalink(SearchContext context, org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    StringBuffer permalink = new StringBuffer();
    try {
      Page page = getPage(wikiSearchResult);
      Wiki wiki = wikiService.getWikiByTypeAndOwner(page.getWikiType(), page.getWikiOwner());
      if (wiki.getType().equalsIgnoreCase(WikiType.GROUP.toString())) {
        String portalContainerName = Utils.getPortalName();
        String portalOwner = context.getSiteName();
        String wikiWebappUri = wikiService.getWikiWebappUri();
        String spaceGroupId = wiki.getOwner();
        
        permalink.append("/");
        permalink.append(portalContainerName);
        permalink.append("/");
        permalink.append(portalOwner);
        permalink.append("/");
        permalink.append(wikiWebappUri);
        permalink.append("/");
        permalink.append(PortalConfig.GROUP_TYPE);
        permalink.append(spaceGroupId);
        permalink.append("/");
        permalink.append(page.getName());
      } else {
        String portalContainerName = Utils.getPortalName();
        String url = page.getUrl();
        if (url != null) {
          url = url.substring(url.indexOf("/" + portalContainerName + "/"));
          permalink.append(url);
        }
      }
    } catch (Exception ex) {
      LOG.info("Can not build the permalink for wiki page ", ex);
    }
    return permalink.toString();
  }

  /**
   * Formats a search result which is used for the unified search.
   *
   * @param context The context URL.
   * @param wikiSearchResult The search result of Wiki.
   * @return The formated search result.
   */
  private SearchResult buildResult(SearchContext context, org.exoplatform.wiki.service.search.SearchResult wikiSearchResult) {
    try {
      String title = wikiSearchResult.getTitle();
      String url = getPagePermalink(context, wikiSearchResult);
      String excerpt = wikiSearchResult.getExcerpt();
      String detail = getPageDetail(wikiSearchResult);
      long relevancy = wikiSearchResult.getScore();
      long date = wikiSearchResult.getUpdatedDate().getTime().getTime();
      String imageUrl = getResultIcon(wikiSearchResult);
      return new SearchResult(url, title, excerpt, detail, imageUrl, date, relevancy);
    } catch (Exception e) {
      LOG.info("Error when getting property from node ", e);
      return null;
    }
  }
}
