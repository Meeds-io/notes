/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.FileUtils;
import org.picocontainer.Startable;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

import org.exoplatform.services.security.Identity;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;

public class NotesExportService implements Startable {

  private static final List<ExportResource> exportResourceList = new ArrayList<>();

  private final NoteService                 noteService;

  private final WikiService                 wikiService;

  private final HTMLUploadImageProcessor    htmlUploadImageProcessor;

  private final ExecutorService                   exportThreadPool;

  public NotesExportService(NoteService noteService, WikiService wikiService, HTMLUploadImageProcessor htmlUploadImageProcessor) {
    this.noteService = noteService;
    this.wikiService = wikiService;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
    this.exportThreadPool = Executors.newCachedThreadPool(new ThreadFactoryBuilder().setNameFormat("Notes-Export-File-%d").build());
  }

  @Override
  public void start() {
    // Nothing to start
  }

  @Override
  public void stop() {
    if (exportThreadPool != null) {
      exportThreadPool.shutdownNow();
    }
  }

  public static void cleanUp(File file) throws IOException {
    if (Files.exists(file.toPath())) {
      Files.delete(file.toPath());
    }
  }

  public void startExportNotes(int exportId, String[] notesToExportIds, boolean exportAll, Identity identity) {
    ExportResource exportResource = new ExportResource();
    exportResource.setExportId(exportId);
    exportResource.setStatus(ExportStatus.STARTED.name());
    exportResource.setAction(new ExportAction());
    exportResourceList.add(exportResource);

    CompletableFuture.runAsync(new ExportThread(noteService,
                                                wikiService,
                                                this,
                                                htmlUploadImageProcessor,
                                                new ExportData(exportId, notesToExportIds, exportAll, identity)),
                               exportThreadPool);
  }

  public void cancelExportNotes(int exportId) {
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

  public byte[] getExportedNotes(int exportId) throws IOException {
    ExportResource exportResource = getExportRessourceById(exportId);
    if (exportResource != null) {
      File zipped = exportResource.getZipFile();
      byte[] filesBytes = FileUtils.readFileToByteArray(zipped);
      cleanUp(zipped);
      exportResource.setStatus(ExportStatus.DONE.name());
      exportResourceList.remove(exportResource);
      return filesBytes;
    } else
      return null; // NOSONAR
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
