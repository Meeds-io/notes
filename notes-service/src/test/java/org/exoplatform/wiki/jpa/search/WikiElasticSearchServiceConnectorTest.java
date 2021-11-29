package org.exoplatform.wiki.jpa.search;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.exoplatform.commons.search.es.ElasticSearchServiceConnector;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import org.exoplatform.commons.search.es.client.ElasticSearchingClient;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.PropertiesParam;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.SpaceListAccess;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.api.SpaceStorage;
import org.exoplatform.wiki.service.search.SearchResult;

/**
 *
 */
public class WikiElasticSearchServiceConnectorTest {

  private WikiElasticSearchServiceConnector searchServiceConnector;

  private ElasticSearchingClient elasticSearchingClient;

  private IdentityManager                   identityManager;

  private SpaceService                      spaceService;

  private SpaceStorage                      spaceStorage;

  private ConfigurationManager              configurationManager;

  @Before
  public void setUp() {
    spaceService = mock(SpaceService.class);
    identityManager = mock(IdentityManager.class);
    searchServiceConnector = mock(WikiElasticSearchServiceConnector.class);
    configurationManager = mock(ConfigurationManager.class);
    elasticSearchingClient = mock(ElasticSearchingClient.class);
    spaceStorage = mock(SpaceStorage.class);
  }

  @Test
  public void shouldReturnResultsWithoutExcerptWhenNoHighlight() {

    Identity systemIdentity = new Identity(IdentityConstants.SYSTEM);
    ConversationState.setCurrent(new ConversationState(systemIdentity));
    // Given
    when(elasticSearchingClient.sendRequest(anyString(),anyString()))
           .thenReturn("{\n" + "  \"took\": 939,\n" + "  \"timed_out\": false,\n" + "  \"_shards\": {\n" + "    \"total\": 5,\n"
               + "    \"successful\": 5,\n" + "    \"failed\": 0\n" + "  },\n" + "  \"hits\": {\n" + "    \"total\": 4,\n"
               + "    \"max_score\": 1.0,\n" + "    \"hits\": [{\n" + "      \"_index\": \"wiki\",\n"
               + "      \"_type\": \"wiki-page\",\n" + "      \"_id\": \"2\",\n" + "      \"_score\": 1.0,\n"
               + "      \"_source\": {\n" + "        \"wikiOwner\": \"intranet\",\n"
               + "        \"createdDate\": \"1494833363955\",\n" + "        \"name\": \"Page_1\",\n"
               + "        \"wikiType\": \"portal\",\n" + "        \"updatedDate\": \"1494833363955\",\n"
               + "        \"title\": \"Page 1\",\n" + "        \"url\": \"/portal/intranet/wiki/Page_1\"\n" + "      }\n"
               + "    }, {\n" + "      \"_index\": \"wiki\",\n" + "      \"_type\": \"wiki-page\",\n" + "      \"_id\": \"3\",\n"
               + "      \"_score\": 1.0,\n" + "      \"_source\": {\n" + "        \"wikiOwner\": \"intranet\",\n"
               + "        \"createdDate\": \"1494833380251\",\n" + "        \"name\": \"Page_2\",\n"
               + "        \"wikiType\": \"portal\",\n" + "        \"updatedDate\": \"1494833380251\",\n"
               + "        \"title\": \"Page 2\",\n" + "        \"url\": \"/portal/intranet/wiki/Page_2\"\n" + "      }\n"
               + "    }]\n" + "  }\n" + "}");

    InitParams initParams = new InitParams();
    PropertiesParam properties = new PropertiesParam();
    properties.setProperty("searchType", "wiki-es");
    properties.setProperty("displayName", "wiki-es");
    properties.setProperty("index", "wiki");
    properties.setProperty("type", "wiki,wiki-page,wiki-attachment");
    properties.setProperty("titleField", "title");
    properties.setProperty("searchFields", "name,title,content,comment,file");
    initParams.put("constructor.params", properties);
    this.searchServiceConnector = new WikiElasticSearchServiceConnector(configurationManager,
                                                                        initParams,
                                                                        elasticSearchingClient,
                                                                        identityManager) {
      @Override
      protected String getPermissionFilter() {
        return "";
      }
    };
    this.searchServiceConnector.setSearchQuery("{\n" + "  \"from\": \"@offset@\",\n" + "  \"size\": \"@limit@\",\n"
        + "  \"query\":{\n" + "    \"bool\":{\n" + "      \"filter\":{\n" + "        \"terms\":{\n"
        + "          \"permissions\": [@permissions@]\n" + "        }\n" + "      },\n" + "      \"should\": {\n"
        + "        \"match_phrase\": {\n" + "          \"summary\": {\n" + "            \"query\": \"@term@\",\n"
        + "            \"boost\": 3\n" + "          }\n" + "        }\n" + "      },\n" + "      \"must\":{\n"
        + "        \"query_string\":{\n"
        + "          \"fields\": [\"name\",\"title^5\",\"content\",\"comment\",\"attachment.content\"],\n"
        + "          \"query\": \"@term_query@\"\n" + "        }\n" + "      },\n" + "      \"must_not\": {\n"
        + "        \"exists\" : { \"field\" : \"sites\" }\n" + "      }\n" + "    }\n" + "  },\n" + "  \"highlight\" : {\n"
        + "    \"number_of_fragments\" : 2,\n" + "    \"fragment_size\" : 150,\n" + "    \"no_match_size\" : 0,\n"
        + "    \"order\": \"score\",\n" + "    \"fields\" : {\n" + "      \"description\" : {\n"
        + "        \"pre_tags\" : [\"<span class='searchMatchExcerpt'>\"],\n" + "        \"post_tags\" : [\"</span>\"]\n"
        + "      },\n" + "      \"summary\" : {\n" + "        \"pre_tags\" : [\"<span class='searchMatchExcerpt'>\"],\n"
        + "        \"post_tags\" : [\"</span>\"]\n" + "      },\n" + "      \"location\" : {\n"
        + "        \"pre_tags\" : [\"<span class='searchMatchExcerpt'>\"],\n" + "        \"post_tags\" : [\"</span>\"]\n"
        + "      }\n" + "    }\n" + "  }\n" + "}");
    when(spaceService.getMemberSpaces("__system")).thenReturn(new SpaceListAccess(spaceStorage,
                                                                                  "__system",
                                                                                  SpaceListAccess.Type.MEMBER));
    when(spaceStorage.getMemberSpacesCount("__system")).thenReturn(0);
    when(identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                             "__system")).thenReturn(new org.exoplatform.social.core.identity.model.Identity("1"));

    // when
    List<SearchResult> searchResults = searchServiceConnector.searchWiki("*", 0, 20);

    // Then
    assertNotNull(searchResults);
    assertEquals(2, searchResults.size());
    assertEquals("", searchResults.get(0).getExcerpt());
    assertEquals("", searchResults.get(1).getExcerpt());
  }

}
