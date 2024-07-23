package io.meeds.notes.notifications.plugin;

import org.exoplatform.commons.api.notification.NotificationContext;
import org.exoplatform.commons.api.notification.model.ArgumentLiteral;
import org.exoplatform.commons.api.notification.model.NotificationInfo;
import org.exoplatform.commons.api.notification.plugin.BaseNotificationPlugin;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.social.notification.Utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public class MentionInNoteNotificationPlugin extends BaseNotificationPlugin {

  public final static String                  ID                 = "MentionInNoteNotificationPlugin";

  public static final Pattern                 MENTION_PATTERN    = Pattern.compile("@([^\\s]+)|@([^\\s]+)$");

  public static final ArgumentLiteral<String> CURRENT_USER       = new ArgumentLiteral<>(String.class, "CURRENT_USER");

  public static final ArgumentLiteral<String> NOTE_AUTHOR        = new ArgumentLiteral<>(String.class, "NOTE_AUTHOR");

  public static final ArgumentLiteral<String> AUTHOR_AVATAR_URL  = new ArgumentLiteral<>(String.class, "AUTHOR_AVATAR_URL");

  public static final ArgumentLiteral<String> AUTHOR_PROFILE_URL = new ArgumentLiteral<>(String.class, "AUTHOR_PROFILE_URL");

  public static final ArgumentLiteral<String> ACTIVITY_LINK      = new ArgumentLiteral<>(String.class, "ACTIVITY_LINK");

  public static final ArgumentLiteral<Set>    MENTIONED_IDS      = new ArgumentLiteral<Set>(Set.class, "MENTIONED_IDS");

  public static final ArgumentLiteral<String> NOTE_TITLE         = new ArgumentLiteral<>(String.class, "NOTE_TITLE");

  public static final ArgumentLiteral<String> SPACE_ID           = new ArgumentLiteral<>(String.class, "SPACE_ID");

  public MentionInNoteNotificationPlugin(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public boolean isValid(NotificationContext ctx) {
    return true;
  }

  @Override
  protected NotificationInfo makeNotification(NotificationContext ctx) {
    String currentUserName = ctx.value(CURRENT_USER);
    List<String> mentionedIds = new ArrayList<>(ctx.value(MENTIONED_IDS));
    String spaceId = ctx.value(SPACE_ID);
    Set<String> receivers = new HashSet<>();
    String[] mentionnedIdArray = new String[mentionedIds.size()];
    Utils.sendToMentioners(receivers, mentionedIds.toArray(mentionnedIdArray), currentUserName, spaceId);
    return NotificationInfo.instance()
                           .setFrom(currentUserName)
                           .to(new ArrayList<>(receivers))
                           .key(getKey())
                           .with(NOTE_TITLE.getKey(), ctx.value(NOTE_TITLE))
                           .with(NOTE_AUTHOR.getKey(), ctx.value(NOTE_AUTHOR))
                           .with(AUTHOR_AVATAR_URL.getKey(), ctx.value(AUTHOR_AVATAR_URL))
                           .with(AUTHOR_PROFILE_URL.getKey(), ctx.value(AUTHOR_PROFILE_URL))
                           .with(ACTIVITY_LINK.getKey(), ctx.value(ACTIVITY_LINK))
                           .with(MENTIONED_IDS.getKey(), String.valueOf(mentionedIds))
                           .end();
  }
}
