/*
 * Copyright (C) 2021 eXo Platform SAS
 *
 *  This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <gnu.org/licenses>.
 */

package org.exoplatform.wiki.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.FileUtils;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.wiki.WikiException;

public class NotesExportService {

  private static final Log                  log                = ExoLogger.getLogger(NotesExportService.class);

  private static final List<ExportResource> exportResourceList = new ArrayList<>();

  private final NoteService                 noteService;

  private final WikiService                 wikiService;

  private final HTMLUploadImageProcessor    htmlUploadImageProcessor;

  public NotesExportService(NoteService noteService, WikiService wikiService, HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.noteService = noteService;
    this.wikiService = wikiService;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
  }

  public static void cleanUp(File file) throws IOException {
    if (Files.exists(file.toPath())) {
      Files.delete(file.toPath());
    }
  }

  public void startExportNotes(int exportId, String[] notesToExportIds, boolean exportAll, Identity identity) throws Exception {
    ExportResource exportResource = new ExportResource();
    exportResource.setExportId(exportId);
    exportResource.setStatus(ExportStatus.STARTED.name());
    exportResource.setAction(new ExportAction());
    exportResourceList.add(exportResource);
    Thread exportThread = new Thread(new ExportThread(noteService,
                                                      wikiService,
                                                      this,
                                                      htmlUploadImageProcessor,
                                                      new ExportData(exportId, notesToExportIds, exportAll, identity)));
    exportThread.start();
  }

  public void cancelExportNotes(int exportId) throws Exception {
    ExportResource exportResource = getExportRessourceById(exportId);
    if (exportResource != null) {
      exportResource.setStatus(ExportStatus.CANCELLED.name());
    }
  }

  public void removeExportResource(int exportId) {
    ExportResource exportResource = getExportRessourceById(exportId);
    if (exportResource != null) {
      exportResourceList.remove(exportResource);
    }
  }

  public byte[] getExportedNotes(int exportId) throws IOException, WikiException {
    ExportResource exportResource = getExportRessourceById(exportId);
    if (exportResource != null) {
      File zipped = exportResource.getZipFile();
      byte[] filesBytes = FileUtils.readFileToByteArray(zipped);
      cleanUp(zipped);
      exportResource.setStatus(ExportStatus.DONE.name());
      exportResourceList.remove(exportResource);
      return filesBytes;
    } else
      return null;
  }

  public ExportingStatus getStatus(int exportId) {
    ExportResource exportResource = getExportRessourceById(exportId);
    if (exportResource != null) {
      return new ExportingStatus(exportResource.getStatus(), exportResource.getAction(), exportResource.getExportedNotesCount());
    }
    return new ExportingStatus();
  }

  public ExportResource getExportRessourceById(int id) {
    return exportResourceList.stream().filter(resource -> id == resource.getExportId()).findFirst().orElse(null);
  }

}
