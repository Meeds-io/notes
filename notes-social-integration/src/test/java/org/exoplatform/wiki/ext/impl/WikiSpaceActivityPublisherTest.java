package org.exoplatform.wiki.ext.impl;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.utils.NoteConstants;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.Mockito.*;

/**
 * Test class for WikiSpaceActivityPublisher
 */
@RunWith(MockitoJUnitRunner.class)
public class WikiSpaceActivityPublisherTest {

  @Mock
  private WikiService      wikiService;

  @Mock
  private IdentityManager  identityManager;

  @Mock
  private ActivityManager  activityManager;

  @Mock
  private SpaceService     spaceService;

  @Mock
  private NoteConstants noteConstants ;


  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsNull() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           activityManager,
            spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", page, null);

    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal", "portal1", "page1", page, null);
  }

  @Test
  public void shouldNotCreateActivityWhenPageIsnull() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
                                                                                           identityManager,
                                                                                           activityManager,
                                                                                           spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);


    Page page1 = new Page();

    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", null,  PageUpdateType.EDIT_PAGE_PERMISSIONS);

    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal", "portal1", "page1", page1, PageUpdateType.EDIT_PAGE_PERMISSIONS);
  }


  @Test
  public void  shouldNotDeleteActivityWhenActivityIdIsEmpty()throws Exception{
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
            identityManager,
            activityManager,
            spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page2 = new Page();
    page2.setActivityId("");
    //when
    wikiSpaceActivityPublisher.postDeletePage( "wikiType", "wikiOwner", "pageId", page2);
    //then
    verify(activityManager, never()).deleteActivity(page2.getActivityId());
  }
  @Test
  public void  shouldDeleteActivityWhenActivityIdIsNotEmpty()throws Exception{
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(wikiService,
            identityManager,
            activityManager,
            spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page2 = new Page();
    page2.setActivityId("activityId");
    //when
    wikiSpaceActivityPublisher.postDeletePage( "wikiType", "wikiOwner", "pageId", page2);
    //then
    verify(activityManager, times(1)).deleteActivity(page2.getActivityId());
  }

}
