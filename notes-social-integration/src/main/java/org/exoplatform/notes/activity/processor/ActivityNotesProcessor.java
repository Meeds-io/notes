package org.exoplatform.notes.activity.processor;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.BaseActivityProcessorPlugin;
import org.exoplatform.social.core.activity.model.ExoSocialActivity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.utils.Utils;

public class ActivityNotesProcessor extends BaseActivityProcessorPlugin {

  private static final Log LOG = ExoLogger.getLogger(ActivityNotesProcessor.class);

  private NoteService      noteService;

  public ActivityNotesProcessor(NoteService noteService, InitParams initParams) {
    super(initParams);
    this.noteService = noteService;
  }

  @Override
  public void processActivity(ExoSocialActivity activity) {
    if (activity.isComment()
        || activity.getType() == null
        || activity.getTemplateParams() == null
        || !activity.getTemplateParams().containsKey(Utils.PAGE_TYPE_KEY)
        || !activity.getTemplateParams().containsKey(Utils.PAGE_OWNER_KEY)
        || !activity.getTemplateParams().containsKey(Utils.PAGE_ID_KEY)) {
      return;
    }
    String pageOwnerType = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_TYPE_KEY);
    String pageOwner = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_OWNER_KEY);
    String pageId = activity.getTemplateParams().get(WikiSpaceActivityPublisher.PAGE_ID_KEY);
    try {
      Page page = noteService.getNoteOfNoteBookByName(pageOwnerType, pageOwner, pageId);
      if (page != null) {
        activity.setMetadataObjectType(Utils.NOTES_METADATA_OBJECT_TYPE);
        activity.setMetadataObjectId(page.getId());
      }
    } catch (WikiException e) {
      LOG.warn("Error getting notes page {}/{}/{}", pageOwnerType, pageOwner, pageId, e);
    }
  }

}
