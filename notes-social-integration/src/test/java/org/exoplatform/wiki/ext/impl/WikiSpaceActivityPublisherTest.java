package org.exoplatform.wiki.ext.impl;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;

/**
 * Test class for WikiSpaceActivityPublisher
 */
@RunWith(MockitoJUnitRunner.class)
public class WikiSpaceActivityPublisherTest {

  @Mock
  private NoteService noteService;

  @Mock
  private IdentityManager identityManager;

  @Mock
  private ActivityManager activityManager;

  @Mock
  private SpaceService    spaceService;

  @Test
  public void shouldNotCreateActivityWhenUpdateTypeIsNull() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        noteService,
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
  public void shouldNotCreateActivityWhenPageIsNull() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
         noteService,
         identityManager,
         activityManager,
         spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    // When
    wikiSpaceActivityPublisher.postUpdatePage("portal", "portal1", "page1", null, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    // Then
    verify(wikiSpaceActivityPublisherSpy, never()).saveActivity("portal", "portal1", "page1", null, PageUpdateType.EDIT_PAGE_PERMISSIONS);
  }

  @Test
  public void shouldNotGenerateActivityWhenIsNotToBePublished() throws Exception {

    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        noteService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
    page.setToBePublished(false);
    Identity identity = new Identity("user");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    Space space = new Space();
    space.setPrettyName("user");
    org.exoplatform.social.core.identity.model.Identity identity1 = new org.exoplatform.social.core.identity.model.Identity("user");
    when(spaceService.getSpaceByGroupId("portal1")).thenReturn(space);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity1);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(identity1);
    // When
    wikiSpaceActivityPublisherSpy.postUpdatePage("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    // Then
    //verify not  saveActivity
    verify(wikiSpaceActivityPublisherSpy, times(1)).saveActivity("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    verify(activityManager, never()).saveActivityNoReturn(identity1, new ExoSocialActivityImpl());
  }

  @Test
  public void shouldGenerateNewActivityWhenIsToBePublished() throws Exception {
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        noteService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
    page.setToBePublished(true);
    Identity identity = new Identity("user");
    ConversationState conversationState = new ConversationState(identity);
    ConversationState.setCurrent(conversationState);
    Space space = new Space();
    space.setPrettyName("user");
    org.exoplatform.social.core.identity.model.Identity identity1 = new org.exoplatform.social.core.identity.model.Identity("user");
    when(spaceService.getSpaceByGroupId("portal1")).thenReturn(space);
    when(identityManager.getOrCreateUserIdentity("user")).thenReturn(identity1);
    when(identityManager.getOrCreateSpaceIdentity(space.getPrettyName())).thenReturn(identity1);
    // When
    wikiSpaceActivityPublisherSpy.postUpdatePage("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    //then
    //verify save new Activity
    verify(wikiSpaceActivityPublisherSpy, times(1)).saveActivity("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    verify(activityManager, times(1)).saveActivityNoReturn(identity1, new ExoSocialActivityImpl());
  }

  @Test
  public void shouldUpdateActivityWhenIsNotNewAndNotToBePublished() throws Exception {
    // Given
    WikiSpaceActivityPublisher wikiSpaceActivityPublisher = new WikiSpaceActivityPublisher(
        noteService,
        identityManager,
        activityManager,
        spaceService);
    WikiSpaceActivityPublisher wikiSpaceActivityPublisherSpy = spy(wikiSpaceActivityPublisher);
    Page page = new Page();
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
    //verify call method saveActivity
    verify(wikiSpaceActivityPublisherSpy, times(1)).saveActivity("group", "portal1", "page1", page, PageUpdateType.EDIT_PAGE_PERMISSIONS);
    //verify update activity
    verify(activityManager, times(1)).updateActivity(activity, page.isToBePublished());
  }

}
