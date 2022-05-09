package org.exoplatform.wiki.ext.impl;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.*;

/**
 * Test class for WikiSpaceActivityPublisher
 */
@RunWith(MockitoJUnitRunner.class)
public class WikiSpaceActivityPublisherTest {

  @Mock
  private WikiService wikiService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private ActivityManager activityManager;

  @Mock
  private SpaceService spaceService;

  @Mock
  private ConversationState conversationState;

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
    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", null, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal",
                                                                "portal1",
                                                                "page1",
                                                                null,
                                                                PageUpdateType.EDIT_PAGE_PERMISSIONS);
  }

  @Test
  //fail to generate new activity when is not to be published
  public void sholNotgenerateActivityWhenIsNotToBpublished() throws Exception {

    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        wikiService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
    page.setCanView(true);
    page.setToBePublished(false);
    Identity identity = new Identity("user");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    Space space = new Space();
    space.setPrettyName("user");
    org.exoplatform.social.core.identity.model.Identity
        identity1 =
        new org.exoplatform.social.core.identity.model.Identity("user");
    when(spaceService.getSpaceByGroupId("portal1")).thenReturn(space);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity1);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(identity1);
    // When
    wikiSpaceActivityPublisherSpy.saveActivity("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    // Then
    //verify not  saveActivity
    verify(activityManager, never()).saveActivityNoReturn(identity1, new ExoSocialActivityImpl());
    identity = null;
    identity1 = null;
    page = null;
    space = null;
    conversationState = null;
  }

  @Test
  //Generate new activity when is to be published
  public void sholdGenerateNewActivityWhenIsToBublished() throws Exception {
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        wikiService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
    page.setCanView(true);
    page.setToBePublished(true);
    Identity identity = new Identity("user");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    Space space = new Space();
    space.setPrettyName("user");
    org.exoplatform.social.core.identity.model.Identity
        identity1 =
        new org.exoplatform.social.core.identity.model.Identity("user");
    when(spaceService.getSpaceByGroupId("portal1")).thenReturn(space);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity1);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(identity1);
    // When
    wikiSpaceActivityPublisherSpy.saveActivity("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    //then
    //verify save new Activity
    verify(activityManager, times(1)).saveActivityNoReturn(identity1, new ExoSocialActivityImpl());
    identity = null;
    identity1 = null;
    page = null;
    space = null;
    conversationState = null;
  }

  @Test
  //update activity when Is not new activity and not to be published
  public void sholdupdateActivityWhenIsNotNewAndNotToBublished() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        wikiService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
    page.setCanView(true);
    page.setToBePublished(false);
    page.setActivityId("notNull");
    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setId(page.getId());
    Identity identity = new Identity("user");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    Space space = new Space();
    space.setPrettyName("user");
    org.exoplatform.social.core.identity.model.Identity
        identity1 =
        new org.exoplatform.social.core.identity.model.Identity("user");
    when(spaceService.getSpaceByGroupId("portal1")).thenReturn(space);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity1);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(identity1);
    when(activityManager.getActivity(page.getActivityId())).thenReturn(activity);
    // When
    wikiSpaceActivityPublisherSpy.postUpdatePage("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    // Then
    //verify call methode saveActivity
    verify(wikiSpaceActivityPublisherSpy, times(1)).saveActivity("group",
                                                                 "portal1",
                                                                 "page1",
                                                                 page,
                                                                 PageUpdateType.EDIT_PAGE_PERMISSIONS);
    //verify update activity
    verify(activityManager, times(1)).updateActivity(activity, page.isToBePublished());
    identity = null;
    identity1 = null;
    page = null;
    activity = null;
    space = null;
    conversationState = null;
  }

}
