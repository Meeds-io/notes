package org.exoplatform.wiki.service;

import lombok.Data;
import org.exoplatform.wiki.mow.api.ExportList;
import org.exoplatform.wiki.mow.api.NoteToExport;

import java.io.File;
import java.util.List;

@Data
public class ExportResource {

  private int exportId;

  private ExportAction action;

  private String status;

  private ExportList notesExport;

  private int exportedNotesCount = 0;

  private int totalNumber;

  private File zipFile;
}
