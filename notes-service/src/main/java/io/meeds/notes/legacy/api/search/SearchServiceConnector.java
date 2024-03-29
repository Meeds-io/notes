package io.meeds.notes.legacy.api.search;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.component.BaseComponentPlugin;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;

import io.meeds.notes.legacy.api.search.data.SearchContext;
import io.meeds.notes.legacy.api.search.data.SearchResult;

import java.util.Collection;

/**
 * Is extended by all SearchService connectors, and allows to build configuration needed by a list of connectors that is used for the Unified Search.
 * @deprecated Copied from commons-search to this module.
 *  Should be reworked to be more simple.
 */
@Deprecated(forRemoval = true, since = "6.0.0")
public abstract class SearchServiceConnector extends BaseComponentPlugin {
  private String searchType; //search type name
  private String displayName; //for use when rendering
  private boolean enable = true;
  private boolean enabledForAnonymous = false;
  
  /**
   * Gets a search type.
   * @return The string.
   * @LevelAPI Experimental
   */
  public String getSearchType() {
    return searchType;
  }

  /**
   * Sets a search type.
   * @param searchType The search type to be set.
   * @LevelAPI Experimental
   */
  public void setSearchType(String searchType) {
    this.searchType = searchType;
  }

  /**
   * Gets a display name.
   * @return The string.
   * @LevelAPI Experimental
   */
  public String getDisplayName() {
    return displayName;
  }

  /**
   * Sets a display name.
   * @param displayName The display name to be set.
   * @LevelAPI Experimental
   */
  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }
  
  /**
   * is enable by default
   */
  public boolean isEnable() {
    return enable;
  }

  /**
   * set enable by default
   */
  public void setEnable(boolean enable) {
    this.enable = enable;
  }
  
  public boolean isEnabledForAnonymous() {
    return enabledForAnonymous;
  }
  
  public void setEnabledForAnonymous(boolean enabledForAnonymous) {
    this.enabledForAnonymous = enabledForAnonymous;
  }

  /**
   * Initializes a search service connector. The constructor is default that connectors must implement.
   * @param initParams The parameters which are used for initializing the search service connector from configuration.
   * @LevelAPI Experimental
   */
  public SearchServiceConnector(InitParams initParams) {
    PropertiesParam param = initParams.getPropertiesParam("constructor.params");
    this.searchType = param.getProperty("searchType");
    this.displayName = param.getProperty("displayName");
    if (StringUtils.isNotBlank(param.getProperty("enable"))) this.setEnable(Boolean.parseBoolean(param.getProperty("enable")));
  }

  /**
   * Returns a collection of search results from the connectors. 
   * The connectors must implement this search method, with the parameters below.
   * @param context The search context.
   * @param query The query statement.
   * @param sites Specified sites where the search is performed (for example Acme, or Intranet).
   * @param offset The start point from which the search results are returned.
   * @param limit The limitation number of search results.
   * @param sort The sorting criteria (title, relevancy and date).
   * @param order The sorting order (ascending and descending).
   * @return The collection of search results.
   * @LevelAPI Experimental 
   */
  public abstract Collection<SearchResult> search(SearchContext context, String query, Collection<String> sites, int offset, int limit, String sort, String order);
  
  /**
   * Returns the status of the indexation for the id in parameter
   * The connectors must implement this search method, with the parameters below.
   * @param context The search context.
   * @param id The id of the element
   * @return true if the document is indexed
   * @LevelAPI Experimental
   */
  public boolean isIndexed(SearchContext context, String id) {
    throw new UnsupportedOperationException();
  }
}
