package org.exoplatform.wiki.listener;

import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.listener.Event;
import org.exoplatform.services.listener.Listener;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.tag.TagService;
import org.exoplatform.social.metadata.tag.model.TagName;
import org.exoplatform.social.metadata.tag.model.TagObject;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.Page;

import java.util.Set;

public class NotesMetadataListener extends Listener<String, Page> {

  private final IndexingService indexingService;

  private final IdentityManager identityManager;

  private final SpaceService    spaceService;

  private final TagService      tagService;

  private static final String   NOTES_METADATA_OBJECT_TYPE = "notes";

  public NotesMetadataListener(IndexingService indexingService,
                               IdentityManager identityManager,
                               SpaceService spaceService,
                               TagService tagService) {
    this.indexingService = indexingService;
    this.identityManager = identityManager;
    this.spaceService = spaceService;
    this.tagService = tagService;
  }

  @Override
  public void onEvent(Event<String, Page> event) {
    Page note = event.getData();
    String username = event.getSource();
    long creatorId = getPosterId(username);
    long audienceId = getStreamOwnerId(note.getWikiOwner(), username);

    Set<TagName> tagNames = tagService.detectTagNames(note.getContent());
    tagService.saveTags(new TagObject(NOTES_METADATA_OBJECT_TYPE, note.getId(), note.getParentPageId()),
                        tagNames,
                        audienceId,
                        creatorId);
    indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, note.getId());
  }

  private long getStreamOwnerId(String spaceGroupId, String username) {
    Space space = spaceService.getSpaceByGroupId(spaceGroupId);
    return space == null ? getPosterId(username)
                         : Long.parseLong(identityManager.getOrCreateSpaceIdentity(space.getPrettyName()).getId());
  }

  private long getPosterId(String username) {
    return StringUtils.isBlank(username) ? 0 : Long.parseLong(identityManager.getOrCreateUserIdentity(username).getId());
  }
}
