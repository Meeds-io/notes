package org.exoplatform.wiki.service;

import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.lang.StringUtils;
import org.picocontainer.Startable;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Context;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.mow.api.ImportList;
import org.exoplatform.wiki.mow.api.Wiki;
import org.exoplatform.wiki.mow.api.WikiType;
import org.exoplatform.wiki.utils.Utils;

public class NotesAutoImportService implements Startable {

  public static final String   NOTES_IMPORT_SCOPE_NAME                    = "NOTES_IMPORT_SCOPE";

  public static final String   NOTES_IMPORT_CONTEXT_NAME                  = "NOTES_IMPORT_CONTEXT";

  public static final Context  NOTES_IMPORT_CONTEXT                       = Context.GLOBAL.id(NOTES_IMPORT_CONTEXT_NAME);

  public static final Scope    NOTES_IMPORT_SCOPE                         = Scope.APPLICATION.id(NOTES_IMPORT_SCOPE_NAME);

  private static final Log     log                                        = ExoLogger.getLogger(NotesAutoImportService.class);

  private static final String  EN_KNOWLEDGE_BASE_SPACE_NAME_PARAM         = "enKnowledgeBaseSpaceName";

  private static final String  FR_KNOWLEDGE_BASE_SPACE_NAME_PARAM         = "frKnowledgeBaseSpaceName ";

  private static final String  EN_KNOWLEDGE_BASE_SPACE_NAME_DISPLAY_PARAM = "enKnowledgeBaseSpaceDispalyName";

  private static final String  FR_KNOWLEDGE_BASE_SPACE_DISPLAY_NAME_PARAM = "frKnowledgeBaseSpaceDispalyName";

  private static final String  EN_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM  = "enKnowledgeBaseSpaceDescription";

  private static final String  FR_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM  = "frKnowledgeBaseSpaceDescription";

  private static final String  IMPORT_ENABLED_PARAM                       = "importEnabled";

  private static final String  IMPORT_CONFLICT_MODE_PARAM                 = "importConflictMode";

  private static final String  FR_EXPORT_ZIP_LOCATION                     = "/exports/fr/kb_export_fr.zip";

  private static final String  EN_EXPORT_ZIP_LOCATION                     = "/exports/en/kb_export_en.zip";

  private static final String  SPACE_TEMPLATE                             = "community";

  private final InitParams     initParams;

  private final NoteService    noteService;

  private final SpaceService   spaceService;

  private final WikiService    wikiService;

  private final SettingService settingService;

  private final UserACL        userACL;

  private String               enKnowledgeBaseSpaceName                   = "exo_knowledge_base_en";

  private String               frKnowledgeBaseSpaceName                   = "exo_knowledge_base_fr";

  private String               enKnowledgeBaseSpaceDispalyName            = "eXo knowledge base";

  private String               frKnowledgeBaseSpaceDispalyName            = "Base de connaissance eXo";

  private String               enKnowledgeBaseSpaceDescription            = "eXo knowledge base space";

  private String               frKnowledgeBaseSpaceDescription            = "Espace pour la base de connaissance eXo";

  private boolean              importEnabled                              = true;

  private String               importConflictMode                         = "replaceAll";

  private Identity             superUserIdentity                          = null;

  public NotesAutoImportService(InitParams initParams,
                                SettingService settingService,
                                NoteService noteService,
                                WikiService wikiService,
                                SpaceService spaceService,
                                UserACL userACL) {
    this.initParams = initParams;
    this.settingService = settingService;
    this.noteService = noteService;
    this.wikiService = wikiService;
    this.spaceService = spaceService;
    this.userACL = userACL;
    if (initParams != null) {
      if (initParams.getValueParam(IMPORT_ENABLED_PARAM) != null) {
        this.importEnabled = initParams.getValueParam(IMPORT_ENABLED_PARAM).getValue().equals("true");
      }
      if (initParams.getValueParam(IMPORT_CONFLICT_MODE_PARAM) != null) {
        this.importConflictMode = initParams.getValueParam(IMPORT_CONFLICT_MODE_PARAM).getValue();
      }
      if (initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_NAME_PARAM) != null) {
        this.enKnowledgeBaseSpaceName = initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_NAME_PARAM).getValue();
      }
      if (initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_NAME_DISPLAY_PARAM) != null) {
        this.enKnowledgeBaseSpaceDispalyName = initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_NAME_DISPLAY_PARAM).getValue();
      }
      if (initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM) != null) {
        this.enKnowledgeBaseSpaceDescription = initParams.getValueParam(EN_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM).getValue();
      }
      if (initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_NAME_PARAM) != null) {
        this.frKnowledgeBaseSpaceName = initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_NAME_PARAM).getValue();
      }
      if (initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_DISPLAY_NAME_PARAM) != null) {
        this.frKnowledgeBaseSpaceDispalyName = initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_DISPLAY_NAME_PARAM).getValue();
      }
      if (initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM) != null) {
        this.frKnowledgeBaseSpaceDescription = initParams.getValueParam(FR_KNOWLEDGE_BASE_SPACE_DESCRIPTION_PARAM).getValue();
      }
    }

  }

  @Override
  public void start() {
    if (importEnabled) {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      try {

        List<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
        membershipEntries.add(new MembershipEntry(userACL.getAdminGroups(), "*"));
        ConversationState.setCurrent(new ConversationState(new Identity(userACL.getSuperUser(), membershipEntries)));
        this.superUserIdentity = ConversationState.getCurrent().getIdentity();
        importNotes(enKnowledgeBaseSpaceName,
                    enKnowledgeBaseSpaceDispalyName,
                    enKnowledgeBaseSpaceDescription,
                    EN_EXPORT_ZIP_LOCATION);
        importNotes(frKnowledgeBaseSpaceName,
                    frKnowledgeBaseSpaceDispalyName,
                    frKnowledgeBaseSpaceDescription,
                    FR_EXPORT_ZIP_LOCATION);
      } catch (Exception e) {
        log.error(" Error occured when trying to import notes for spaces {} and {}", enKnowledgeBaseSpaceName, frKnowledgeBaseSpaceName,e);
      } finally {
        RequestLifeCycle.end();
      }
    }
  }

  @Override
  public void stop() {

  }

  private void importNotes(String spaceName, String spaceDisplayName, String spaceDescription, String zipPath) {

    Space space = spaceService.getSpaceByPrettyName(spaceName);
    if (space == null) {
      space = createSpace(spaceName, spaceDisplayName, spaceDescription, SPACE_TEMPLATE);
    }

    try {
      String folderPath = System.getProperty("java.io.tmpdir");
      List<String> files = new ArrayList<>();
      File destDir = new File(folderPath);
      if (!destDir.exists()) {
        destDir.mkdir();
      }
      String notesFilePath = "";
      InputStream in = getClass().getResourceAsStream(zipPath);
      try (ZipInputStream zipIn = new ZipInputStream(in)) {
        ZipEntry entry = zipIn.getNextEntry();
        while (entry != null) {
          String filePath = folderPath + File.separator + entry.getName();
          if (!entry.isDirectory()) {
            Utils.extractFile(zipIn, filePath);
            if (filePath.contains("notesExport_")) {
              notesFilePath = filePath;
            }
            files.add(filePath);
          } else {
            File dir = new File(filePath);
            dir.mkdirs();
          }
          zipIn.closeEntry();
          entry = zipIn.getNextEntry();
        }
      }
      long exportTime = 0;
      if (StringUtils.isNotEmpty(notesFilePath)) {
        ObjectMapper mapper = new ObjectMapper();
        File notesFile = new File(notesFilePath);
        ImportList notes = mapper.readValue(notesFile, new TypeReference<ImportList>() {
        });
        try {
          exportTime = notes.getExportDate();
        } catch (Exception e) {
          exportTime = 0;
        }
      }

      SettingValue<?> settingsValue = settingService.get(NOTES_IMPORT_CONTEXT, NOTES_IMPORT_SCOPE, spaceName);
      String settingsValueString =
                                 settingsValue == null || settingsValue.getValue() == null ? null
                                                                                           : settingsValue.getValue().toString();
      if (exportTime == 0 || settingsValue == null || exportTime != Long.valueOf(settingsValueString)) {
        log.info(" Start import notes for space {}", spaceName);
        if (space != null) {
          Wiki wiki = wikiService.getWikiByTypeAndOwner(WikiType.GROUP.toString().toLowerCase(), space.getGroupId());
          if (wiki == null) {
            wiki = wikiService.createWiki(WikiType.GROUP.toString().toLowerCase(), space.getGroupId());
          }
          if (wiki != null) {
            noteService.importNotes(files, wiki.getWikiHome(), importConflictMode, superUserIdentity);
            settingService.set(NOTES_IMPORT_CONTEXT,
                               NOTES_IMPORT_SCOPE,
                               spaceName,
                               SettingValue.create(String.valueOf(exportTime)));
          }
        }
        log.info(" End import notes for space {}", spaceName);
      } else {
        log.info("No notes to import for space {}", spaceName);
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  private Space createSpace(String prettyName, String displayName, String description, String template) {
    Space space = new Space();
    space.setPriority(Space.INTERMEDIATE_PRIORITY);
    space.setDisplayName(displayName);
    space.setDescription(description);
    space.setPrettyName(prettyName);
    space.setTemplate(template);
    space.setVisibility(Space.HIDDEN);
    space.setRegistration(Space.CLOSE);
    try {
      return spaceService.createSpace(space, superUserIdentity.getUserId());
    } catch (Exception e) {
      return null;
    }
  }

}
