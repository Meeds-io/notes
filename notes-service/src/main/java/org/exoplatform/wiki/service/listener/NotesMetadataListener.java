package org.exoplatform.wiki.service.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.MetadataActivityProcessor;
import org.exoplatform.social.metadata.favorite.FavoriteService;
import org.exoplatform.social.metadata.favorite.model.Favorite;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

import java.util.Map;

public class NotesMetadataListener extends Listener<Long, MetadataItem> {

  private final IndexingService indexingService;

  private final FavoriteService favoriteService;

  private final IdentityManager identityManager;

  private final NoteService     noteService;

  private final ActivityManager activityManager;

  private static final String   METADATA_CREATED = "social.metadataItem.created";

  private static final String   METADATA_DELETED = "social.metadataItem.deleted";

  public NotesMetadataListener(IndexingService indexingService,
                               FavoriteService favoriteService,
                               IdentityManager identityManager,
                               NoteService noteService,
                               ActivityManager activityManager) {
    this.indexingService = indexingService;
    this.favoriteService = favoriteService;
    this.identityManager = identityManager;
    this.noteService = noteService;
    this.activityManager = activityManager;
  }

  @Override
  public void onEvent(Event<Long, MetadataItem> event) throws Exception {
    ConversationState conversationstate = ConversationState.getCurrent();
    Identity currentIdentity = conversationstate == null
        || conversationstate.getIdentity() == null ? null : conversationstate.getIdentity();
    MetadataItem metadataItem = event.getData();
    String objectType = event.getData().getObjectType();
    String objectId = metadataItem.getObjectId();
    if (StringUtils.equals(objectType, Utils.NOTES_METADATA_OBJECT_TYPE)) {
      indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, objectId);
    } else if (StringUtils.equals(objectType, MetadataActivityProcessor.ACTIVITY_METADATA_OBJECT_TYPE)) {
      ExoSocialActivity activity = activityManager.getActivity(objectId);
      if (activity.getType().equals(Utils.WIKI_APP_ID) && currentIdentity != null) {
        Map<String, String> templateParams = activity.getTemplateParams();
        String pageId = templateParams.get(Utils.PAGE_ID_KEY);
        String pageOwner = templateParams.get(Utils.PAGE_OWNER_KEY);
        String pageType = templateParams.get(Utils.PAGE_TYPE_KEY);

        Page note = noteService.getNoteOfNoteBookByName(pageType, pageOwner, pageId, currentIdentity);

        org.exoplatform.social.core.identity.model.Identity userIdentity =
                                                                         identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                                             currentIdentity.getUserId());
        Favorite favorite =
                          new Favorite(Utils.NOTES_METADATA_OBJECT_TYPE, note.getId(), "", Long.parseLong(userIdentity.getId()));
        if (note.getMetadatas().isEmpty() && event.getEventName().equals(METADATA_CREATED)) {
          favoriteService.createFavorite(favorite);
        } else if (!note.getMetadatas().isEmpty() && event.getEventName().equals(METADATA_DELETED)) {
          favoriteService.deleteFavorite(favorite);
        }
        indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, note.getId());
      }
    }
  }
}
