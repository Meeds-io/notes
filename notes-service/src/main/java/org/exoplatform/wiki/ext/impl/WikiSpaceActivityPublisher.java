package org.exoplatform.wiki.ext.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.activity.model.ExoSocialActivityImpl;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.core.storage.SpaceStorageException;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

public class WikiSpaceActivityPublisher extends PageWikiListener {

  public static final String WIKI_APP_ID         = "ks-wiki:spaces";

  public static final String ACTIVITY_TYPE_KEY   = "act_key";

  public static final String PAGE_ID_KEY         = "page_id";

  public static final String PAGE_TYPE_KEY       = "page_type";

  public static final String PAGE_OWNER_KEY      = "page_owner";

  public static final String PAGE_TITLE_KEY      = "page_name";

  public static final String URL_KEY             = "page_url";

  public static final String PAGE_EXCERPT        = "page_exceprt";

  public static final String VIEW_CHANGE_URL_KEY = "view_change_url";

  public static final String VIEW_CHANGE_ANCHOR  = "#CompareRevision/changes";

  public static final String WIKI_PAGE_NAME      = "wiki";

  public static final String WIKI_PAGE_VERSION   = "version";

  private static final int   EXCERPT_LENGTH      = 500;

  private static final Log   LOG                 = ExoLogger.getExoLogger(WikiSpaceActivityPublisher.class);

  private NoteService        noteService;

  private IdentityManager    identityManager;

  private ActivityManager    activityManager;

  private SpaceService       spaceService;

  public WikiSpaceActivityPublisher(NoteService noteService,
                                    IdentityManager identityManager,
                                    ActivityManager activityManager,
                                    SpaceService spaceService) {
    this.noteService = noteService;
    this.identityManager = identityManager;
    this.activityManager = activityManager;
    this.spaceService = spaceService;
  }

  private ExoSocialActivityImpl createNewActivity(String ownerId) {
    ExoSocialActivityImpl activity = new ExoSocialActivityImpl();
    activity.setUserId(ownerId);
    activity.setBody("body");
    activity.setType(WIKI_APP_ID);
    return activity;
  }

  private ExoSocialActivity generateActivity(Identity ownerStream, // NOSONAR
                                             Identity ownerIdentity,
                                             String wikiType,
                                             String wikiOwner,
                                             String pageId,
                                             Page page,
                                             String spaceUrl,
                                             PageUpdateType activityType) throws WikiException {
    // Get activity
    ExoSocialActivity activity = null;
    boolean isNewActivity = true;
    if (page.getActivityId() != null) {
      activity = activityManager.getActivity(page.getActivityId());
      isNewActivity = (activity == null);
    }

    if (isNewActivity && !page.isToBePublished()) {
      return null;
    }

    if (isNewActivity) {
      if (page.isMinorEdit()) {
        return null;
      }
      activity = createNewActivity(ownerIdentity.getId());
    }
    activity.setTitle(page.getTitle());
    // Add UI params
    Map<String, String> templateParams = new HashMap<>();
    templateParams.put(PAGE_ID_KEY, pageId);
    templateParams.put(ACTIVITY_TYPE_KEY, activityType.toString());
    templateParams.put(PAGE_OWNER_KEY, wikiOwner);
    templateParams.put(PAGE_TYPE_KEY, wikiType);
    templateParams.put(PAGE_TITLE_KEY, page.getTitle());
    String pageURL = (page.getUrl() == null) ? (spaceUrl != null ? (spaceUrl + "/" + WIKI_PAGE_NAME) : "") : page.getUrl();
    templateParams.put(URL_KEY, pageURL);
    int versionsTotal = 0;
    List<PageHistory> versions = noteService.getVersionsHistoryOfNote(page);
    if (versions != null && !versions.isEmpty()) {
      versionsTotal = versions.size();
    }
    templateParams.put(WIKI_PAGE_VERSION, String.valueOf(versionsTotal));
    activity.setMetadataObjectType(Utils.NOTES_METADATA_OBJECT_TYPE);
    activity.setMetadataObjectId(page.getId());

    // Create page excerpt
    StringBuilder excerpt = new StringBuilder();
    try {
      excerpt.append(page.getContent());
    } catch (Exception e) {
      throw new WikiException("Cannot render page " + page.getWikiType() + ":" + page.getWikiOwner() + page.getName(), e);
    }
    if (excerpt.length() > EXCERPT_LENGTH) {
      excerpt.replace(EXCERPT_LENGTH, excerpt.length(), "...");
    }
    templateParams.put(PAGE_EXCERPT, validateExcerpt(excerpt.toString()));
    templateParams.put(org.exoplatform.social.core.BaseActivityProcessorPlugin.TEMPLATE_PARAM_TO_PROCESS, PAGE_EXCERPT);
    if (!PageUpdateType.ADD_PAGE.equals(activityType)) {
      String verName = null;
      if (versions != null && !versions.isEmpty()) {
        verName = String.valueOf(versions.size() + 1);
      }
      templateParams.put(VIEW_CHANGE_URL_KEY, Utils.getURL(page.getUrl(), verName));
    }

    activity.setTemplateParams(templateParams);

    // Save activity
    if (isNewActivity) {
      activityManager.saveActivityNoReturn(ownerStream, activity);
    } else {
      if (PageUpdateType.MOVE_PAGE.equals(activityType)) {
        activity.setStreamOwner(ownerStream.getRemoteId());
      }
      if (page.isToBePublished()) {
        activity.setUpdated(new Date().getTime());
      }
      activityManager.updateActivity(activity, page.isToBePublished());
    }
    return activity;
  }

  private String validateExcerpt(String excerpt) {
    List<String> lines = Stream.of(excerpt.split("\n")).filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

    Stream<String> sLines = lines.stream();
    StringBuilder result = new StringBuilder();

    //
    sLines.map(new Function<String, String>() {
      @Override
      public String apply(String line) {
        if (line.length() > EXCERPT_LENGTH) {
          line = line.substring(0, EXCERPT_LENGTH) + "...";
        }
        return line;
      }
    }).limit(4).forEach(line -> {
      result.append("<p>");
      result.append(line);
      result.append("</p>");
    });

    if (lines.size() > 4) {
      result.append("...");
    }
    return result.toString();
  }

  protected void saveActivity(String wikiType,
                              String wikiOwner,
                              String pageId,
                              Page page,
                              PageUpdateType activityType) throws WikiException {
    // Not raise the activity in case of user space
    if (!PortalConfig.GROUP_TYPE.equals(wikiType)) {
      return;
    }

    String username = ConversationState.getCurrent().getIdentity().getUserId();
    Identity userIdentity = identityManager.getOrCreateUserIdentity(username);

    Identity ownerStream = null;
    ExoSocialActivity activity = null;
    String spaceUrl = null;
    try {
      Space space = spaceService.getSpaceByGroupId(wikiOwner);
      if (space != null) {
        ownerStream = identityManager.getOrCreateSpaceIdentity(space.getPrettyName());
        spaceUrl = space.getUrl();
      }
    } catch (SpaceStorageException e) {
      if (LOG.isDebugEnabled()) {
        LOG.debug(String.format("Space %s not existed", wikiOwner), e);
      }
    }

    if (ownerStream != null) {
      activity = generateActivity(ownerStream,
                                  userIdentity,
                                  wikiType,
                                  wikiOwner,
                                  pageId,
                                  page,
                                  spaceUrl,
                                  activityType);
      if (activity == null) {
        return;
      }

      // Attach activity id to wiki page
      String activityId = activity.getId();
      if (!StringUtils.isEmpty(activityId)) {
        page.setActivityId(activityId);
        noteService.updateNote(page);
      }
    }
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if (NoteConstants.NOTE_HOME_NAME.equals(pageId) || !page.getWikiType().toUpperCase().equals(WikiType.GROUP.name())|| !page.isToBePublished()) {
      // catch the case of the Wiki Home added as it's created by the system, not by
      // users.
      return;
    }
    saveActivity(wikiType, wikiOwner, pageId, page, PageUpdateType.ADD_PAGE);
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    if (page.getActivityId() != null && StringUtils.isNotEmpty(page.getActivityId())) {
      activityManager.deleteActivity(page.getActivityId());
    }
  }

  @Override
  public void postUpdatePage(String wikiType,
                             String wikiOwner,
                             String pageId,
                             Page page,
                             PageUpdateType wikiUpdateType) throws WikiException {
    // Generate an activity only in the following cases
    if (page != null && wikiUpdateType != null) {
      saveActivity(wikiType, wikiOwner, pageId, page, wikiUpdateType);
    }
  }
}
