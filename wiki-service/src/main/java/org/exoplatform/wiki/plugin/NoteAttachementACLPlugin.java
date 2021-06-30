package org.exoplatform.wiki.plugin;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.services.attachments.plugin.AttachmentACLPlugin;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.wiki.service.NoteService;

public class NoteAttachementACLPlugin extends AttachmentACLPlugin {
  
  private static final Log    LOG                  = ExoLogger.getLogger(NoteAttachementACLPlugin.class.getName());

  private static final String NOTE_ATTACHMENT_TYPE = "note";

  private NoteService         noteService;

  public NoteAttachementACLPlugin(NoteService noteService) {
    this.noteService = noteService;  }

  @Override
  public String getEntityType() {
    return NOTE_ATTACHMENT_TYPE;
  }

  @Override
  public boolean canView(long userIdentityId, String entityType, String entityId) {
    if (!entityType.equals(NOTE_ATTACHMENT_TYPE)) {
      throw new IllegalArgumentException("Entity type must be" + NOTE_ATTACHMENT_TYPE);
    }

    if (StringUtils.isEmpty(entityId)) {
      throw new IllegalArgumentException("Entity id must not be Empty");
    }

    if (userIdentityId < 0) {
      throw new IllegalArgumentException("User identity must not be null");
    }

    boolean canView = true;
    return canView;
  }

  @Override
  public boolean canDelete(long userIdentityId, String entityType, String entityId) {
    if (!entityType.equals(NOTE_ATTACHMENT_TYPE)) {
      throw new IllegalArgumentException("Entity type must be" + NOTE_ATTACHMENT_TYPE);
    }

    if (StringUtils.isEmpty(entityId)) {
      throw new IllegalArgumentException("Entity id must not be Empty");
    }

    if (userIdentityId <= 0) {
      throw new IllegalArgumentException("User identity must be positive");
    }

    boolean canDelete = true;

    return canDelete;
  }
}
