package org.exoplatform.wiki.service;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.commons.lang.StringUtils;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.component.RequestLifeCycle;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.common.service.HTMLUploadImageProcessor;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;

public class ExportThread implements Runnable {

  private static final Log               log                          = ExoLogger.getLogger(ExportThread.class);
  private static final String            IMAGE_URL_REPLACEMENT_PREFIX = "//-";
  private static final String            IMAGE_URL_REPLACEMENT_SUFFIX = "-//";
  private static final String            EXPORT_ZIP_EXTENSION         = ".zip";
  private static final String            EXPORT_ZIP_PREFIX            = "exportzip";
  private static final String            TEMP_DIRECTORY_PATH          = "java.io.tmpdir";
  private final NoteService              noteService;
  private final WikiService              wikiService;
  private final NotesExportService       notesExportService;
  private final HTMLUploadImageProcessor htmlUploadImageProcessor;
  private final ExportData               exportData;

  public ExportThread(NoteService noteService,
                      WikiService wikiService,
                      NotesExportService notesExportService,
                      HTMLUploadImageProcessor htmlUploadImageProcessor,
                      ExportData exportData) {
    this.noteService = noteService;
    this.wikiService = wikiService;
    this.notesExportService = notesExportService;
    this.htmlUploadImageProcessor = htmlUploadImageProcessor;
    this.exportData = exportData;
  }

  public static void cleanUp(File file) throws IOException {
    if (Files.exists(file.toPath())) {
      Files.delete(file.toPath());
    }
  }

  public static File zipFiles(String zipFileName,
                              List<File> addToZip,
                              NotesExportService notesExportService,
                              int exportId) throws IOException {

    String zipPath = System.getProperty(TEMP_DIRECTORY_PATH) + File.separator + zipFileName;
    FileOutputStream fos = new FileOutputStream(zipPath);
    ZipOutputStream zipOut = new ZipOutputStream(fos);
    for (File fileToZip : addToZip) {
      ExportResource exportResource = notesExportService.getExportRessourceById(exportId);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        return null;
      }
      try {
        FileInputStream fis = new FileInputStream(fileToZip);
        ZipEntry zipEntry = new ZipEntry(fileToZip.getName());
        zipOut.putNextEntry(zipEntry);
        byte[] bytes = new byte[1024];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
          zipOut.write(bytes, 0, length);
        }
        fis.close();
      } catch (IOException e) {
        log.warn("cannot add the file: {} to the zip", fileToZip.getName());
      }
    }
    zipOut.close();
    fos.close();

    File zip = new File(zipPath);
    if (!zip.exists()) {
      throw new FileNotFoundException("The created zip file could not be found");
    }
    return zip;
  }

  @Override
  public void run() {
    try {
      RequestLifeCycle.begin(PortalContainer.getInstance());
      processExport(exportData.getExportId(),
                    exportData.getNotesToExportIds(),
                    exportData.isExportAll(),
                    exportData.getIdentity());
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      RequestLifeCycle.end();
    }
  }

  public void processExport(int exportId, String[] notesToExportIds, boolean exportAll, Identity identity) throws IOException {

    File zipFile = null;
    ExportResource exportResource = notesExportService.getExportRessourceById(exportId);
    if (exportResource != null) {
      exportResource.setStatus(ExportStatus.IN_PROGRESS.name());
      exportResource.getAction().setStarted(true);
      exportResource.getAction().setAction(ExportAction.GETTING_NOTES);
      log.info("IN_PROGRESS ................... Getting notes to export");
      Page note_ = null;
      List<NoteToExport> noteToExportList = new ArrayList();
      if (exportAll) {
        for (String noteId : notesToExportIds) {
          try {
            Page note = noteService.getNoteById(noteId, identity);
            if (note == null) {
              log.warn("Failed to export note {}: note not find ", noteId);
              continue;
            }
            if(note_ == null) note_ = note;
            NoteToExport noteToExport = getNoteToExport(new NoteToExport(note.getId(),
                                                                         note.getName(),
                                                                         note.getOwner(),
                                                                         note.getAuthor(),
                                                                         note.getContent(),
                                                                         note.getSyntax(),
                                                                         note.getTitle(),
                                                                         note.getComment(),
                                                                         note.getWikiId(),
                                                                         note.getWikiType(),
                                                                         note.getWikiOwner()),
                                                        exportId,
                                                        identity);
            if (noteToExport == null) {
              exportResource = notesExportService.getExportRessourceById(exportId);
              if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
                return;
              }
            }
            noteToExportList.add(noteToExport);
            exportResource.getAction().setNotesGetted(true);
            log.info("IN_PROGRESS ................... notes getted");
          } catch (IllegalAccessException e) {
            log.error("User does not have  permissions on the note {}", noteId, e);
          } catch (Exception ex) {
            log.warn("Failed to export note {} ", noteId, ex);
          }
        }
      } else {
        List<NoteToExport> allNotesToExport = new ArrayList<>();
        int maxAncestors = 0;
        for (String noteId : notesToExportIds) {
          Page note;
          try {
            note = noteService.getNoteById(noteId, identity);
            if (note == null) {
              log.warn("Failed to export note {}: note not find ", noteId);
              continue;
            }
            NoteToExport noteToExport = new NoteToExport(note.getId(),
                                                         note.getName(),
                                                         note.getOwner(),
                                                         note.getAuthor(),
                                                         note.getContent(),
                                                         note.getSyntax(),
                                                         note.getTitle(),
                                                         note.getComment(),
                                                         note.getWikiId(),
                                                         note.getWikiType(),
                                                         note.getWikiOwner());
            noteToExport.setContent(processImagesForExport(note));
            noteToExport.setContent(processNotesLinkForExport(noteToExport));
            LinkedList<String> ancestors = getNoteAncestorsIds(noteToExport.getId());
            noteToExport.setAncestors(ancestors);
            if (ancestors.size() > maxAncestors) {
              maxAncestors = ancestors.size();
            }
            allNotesToExport.add(noteToExport);
            exportResource = notesExportService.getExportRessourceById(exportId);
            if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
              notesExportService.removeExportResource(exportId);
              return;
            }
            exportResource.setExportedNotesCount(exportResource.getExportedNotesCount() + 1);
          } catch (IllegalAccessException e) {
            log.error("User does not have  permissions on the note {}", noteId, e);
          } catch (Exception ex) {
            log.warn("Failed to export note {} ", noteId, ex);
          }
        }
        exportResource.getAction().setNotesGetted(true);
        exportResource.getAction().setAction(ExportAction.UPDATING_NOTES_PARENTS);
        log.info("IN_PROGRESS ................... Updating notes parents");
        for (NoteToExport noteToExport : allNotesToExport) {
          noteToExport.setParent(getParentOfNoteFromExistingNotes(noteToExport.getAncestors(),
                                                                  allNotesToExport,
                                                                  notesToExportIds));
        }
        if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
          notesExportService.removeExportResource(exportId);
          return;
        }
        for (int level = maxAncestors; level >= 0; level--) {
          List<NoteToExport> bottomNotes = getBottomNotesToExport(allNotesToExport, level);
          for (NoteToExport bottomNote : bottomNotes) {
            NoteToExport parent = bottomNote.getParent();
            if (parent != null) {
              List<NoteToExport> children = parent.getChildren();
              if (children != null) {
                children.add(bottomNote);
              } else {
                children = new ArrayList<>(Collections.singletonList(bottomNote));
              }
              for (NoteToExport child : children) {
                NoteToExport currentParent = new NoteToExport(parent);
                currentParent.setChildren(null);
                currentParent.setParent(null);
                child.setParent(currentParent);
              }
              parent.setChildren(children);
              allNotesToExport.remove(bottomNote);
              allNotesToExport.set(allNotesToExport.indexOf(parent), parent);
            }
          }
        }
        noteToExportList.addAll(allNotesToExport);
      }
      exportResource = notesExportService.getExportRessourceById(exportId);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        notesExportService.removeExportResource(exportId);
        return;
      }
      exportResource.getAction().setNotesPrepared(true);
      exportResource.getAction().setAction(ExportAction.CREATING_CONTENT_DATA);
      log.info("IN_PROGRESS ................... CREATING_CONTENT_DATA");
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        notesExportService.removeExportResource(exportId);
        return;
      }
      ExportList notesExport = new ExportList(new Date().getTime(), noteToExportList);
      exportResource.setNotesExport(notesExport);
      List<File> files = new ArrayList<>();
      File temp;
      temp = File.createTempFile("notesExport_" + new Date().getTime(), ".json");
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(notesExport);
      String contentUpdated = json;
      String fileName = "";
      String filePath = "";
      exportResource.getAction().setJsonCreated(true);
      exportResource.getAction().setAction(ExportAction.UPDATING_IMAGES_URLS);
      log.info("IN_PROGRESS ................... UPDATING_IMAGES_URLS");
      while (contentUpdated.contains(IMAGE_URL_REPLACEMENT_PREFIX)) {
        fileName = contentUpdated.split(IMAGE_URL_REPLACEMENT_PREFIX)[1].split(IMAGE_URL_REPLACEMENT_SUFFIX)[0];
        filePath = System.getProperty(TEMP_DIRECTORY_PATH) + File.separator + fileName;
        files.add(new File(filePath));
        contentUpdated = contentUpdated.replace(IMAGE_URL_REPLACEMENT_PREFIX + fileName + IMAGE_URL_REPLACEMENT_SUFFIX, "");
      }
      exportResource = notesExportService.getExportRessourceById(exportId);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        for (File file : files) {
          cleanUp(file);
        }
        notesExportService.removeExportResource(exportId);
        return;
      }
      exportResource.getAction().setImageUrlsUpdated(true);
      try (BufferedWriter bw = new BufferedWriter(new FileWriter(temp))) {
        bw.write(json);
      }
      files.add(temp);
      exportResource = notesExportService.getExportRessourceById(exportId);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        for (File file : files) {
          cleanUp(file);
        }
        notesExportService.removeExportResource(exportId);
        return;
      }
      exportResource.getAction().setAction(ExportAction.CREATING_ZIP_FILE);
      log.info("IN_PROGRESS ................... CREATING_ZIP_FILE");
      String zipName = EXPORT_ZIP_PREFIX + exportId + EXPORT_ZIP_EXTENSION;
      exportResource.setZipFile(zipFile);
      zipFile = zipFiles(zipName, files, notesExportService, exportId);
      exportResource.setZipFile(zipFile);
      exportResource = notesExportService.getExportRessourceById(exportId);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        for (File file : files) {
          cleanUp(file);
        }
        if (zipFile != null) {
          cleanUp(zipFile);
        }
        notesExportService.removeExportResource(exportId);
        return;
      }
      String date = new SimpleDateFormat("dd_MM_yyyy").format(new Date());
      if (note_.getWikiType().toUpperCase().equals(WikiType.GROUP.name())) {
        htmlUploadImageProcessor.uploadSpaceFile(zipFile.getPath(), note_.getWikiOwner(),"notesExport_" + date + ".zip", "Documents/Notes/exports");
      }
      if (note_.getWikiType().toUpperCase().equals(WikiType.USER.name())) {
        htmlUploadImageProcessor.uploadUserFile(zipFile.getPath(), note_.getWikiOwner(),"notesExport_" + date + ".zip", "Documents/Notes/exports");
      }
      exportResource.setStatus(ExportStatus.ZIP_CREATED.name());
      exportResource.getAction().setZipCreated(true);
      exportResource.getAction().setAction(ExportAction.CLEANING_TEMP_FILE);
      log.info("IN_PROGRESS ................... CLEANING_TEMP_FILE");
      for (File file : files) {
        cleanUp(file);
      }
      exportResource.getAction().setAction(ExportAction.EXPORT_DATA_CREATED);
    }
  }

  private List<NoteToExport> getBottomNotesToExport(List<NoteToExport> allNotesToExport, int level) {
    return allNotesToExport.stream().filter(export -> export.getAncestors().size() == level).collect(Collectors.toList());
  }

  private NoteToExport getParentOfNoteFromExistingNotes(LinkedList<String> ancestors,
                                                        List<NoteToExport> exports,
                                                        String[] noteIds) {
    NoteToExport parent = null;
    Iterator<String> descendingIterator = ancestors.descendingIterator();
    String parentId = null;
    boolean parentFound = false;
    while (descendingIterator.hasNext() && !parentFound) {
      String current = descendingIterator.next();
      if (Arrays.asList(noteIds).contains(current)) {
        parentId = current;
        parentFound = true;
      }
    }
    if (parentId != null) {
      String finalParentId = parentId;
      Optional<NoteToExport> parentToExport = exports.stream().filter(export -> export.getId().equals(finalParentId)).findFirst();
      if (parentToExport.isPresent()) {
        parent = parentToExport.get();
      }
    }
    return parent;
  }

  /**
   * Recursive method to build the children and parent of a note
   *
   * @param note get the note details to be exported
   * @return
   * @throws WikiException
   */
  public NoteToExport getNoteToExport(NoteToExport note, int exportId, Identity identity) throws WikiException,
                                                                                          IOException,
                                                                                          InterruptedException {
    try {
      note.setContent(processImagesForExport(noteService.getNoteById(note.getId())));
    } catch (Exception e) {
      log.warn("Cannot process images for note {}", note.getId());
    }
    try {
      note.setContent(processNotesLinkForExport(note));
    } catch (Exception e) {
      log.warn("Cannot process notes link for note {}", note.getId());
    }
    ExportResource exportResource = notesExportService.getExportRessourceById(exportId);
    if (exportResource != null) {
      exportResource.setExportedNotesCount(exportResource.getExportedNotesCount() + 1);
      if (exportResource.getStatus().equals(ExportStatus.CANCELLED.name())) {
        return null;
      }
    }
    List<NoteToExport> children = noteService.getChildrenNoteOf(note, identity.getUserId());
    for (NoteToExport child : children) {
      child.setParent(note);
    }
    note.setChildren(children);
    note.setParent(noteService.getParentNoteOf(note));
    for (NoteToExport child : children) {
      getNoteToExport(child, exportId, identity);
    }
    return note;
  }

  public String processNotesLinkForExport(NoteToExport note) throws WikiException {
    String content = note.getContent();
    String noteLinkprefix = "class=\"noteLink\" href=\"";
    String contentUpdated = content;
    Map<String, String> urlToReplaces = new HashMap<>();
    while (contentUpdated.contains("noteLink")) {
      String check_content = contentUpdated;
      String noteId = contentUpdated.split(noteLinkprefix)[1].split("\"")[0];
      Page linkedNote = null;
      try {
        long id = Long.parseLong(noteId);
        linkedNote = noteService.getNoteById(noteId);
      } catch (NumberFormatException e) {
        Page note_ = noteService.getNoteById(note.getId());
        linkedNote = noteService.getNoteOfNoteBookByName(note_.getWikiType(), note_.getWikiOwner(), noteId);
      }
      if (linkedNote != null) {
        String noteParams = IMAGE_URL_REPLACEMENT_PREFIX + linkedNote.getWikiType() + IMAGE_URL_REPLACEMENT_SUFFIX
            + IMAGE_URL_REPLACEMENT_PREFIX + linkedNote.getWikiOwner() + IMAGE_URL_REPLACEMENT_SUFFIX
            + IMAGE_URL_REPLACEMENT_PREFIX + linkedNote.getName() + IMAGE_URL_REPLACEMENT_SUFFIX;
        urlToReplaces.put(noteLinkprefix + linkedNote.getId() + "\"", noteLinkprefix + noteParams + "\"");
      }
      contentUpdated = contentUpdated.replace(noteLinkprefix + noteId + "\"", "");
      if (contentUpdated.equals(check_content)) {
        break;
      }
    }
    if (!urlToReplaces.isEmpty()) {
      content = replaceUrl(content, urlToReplaces);
    }
    return content;
  }

  public List<File> getFilesfromContent(NoteToExport note, List<File> files, String userId) throws WikiException {
    String contentUpdated = note.getContent();
    String fileName = "";
    String filePath = "";
    while (contentUpdated.contains("//-")) {
      fileName = contentUpdated.split("//-")[1].split("-//")[0];
      filePath = System.getProperty(TEMP_DIRECTORY_PATH) + File.separator + fileName;
      files.add(new File(filePath));
      contentUpdated = contentUpdated.replace("//-" + fileName + "-//", "");
    }
    List<NoteToExport> children = noteService.getChildrenNoteOf(note, userId);
    for (NoteToExport child : children) {
      getFilesfromContent(child, files, userId);
    }
    return files;
  }

  /**
   * Process images by creting images found in the content
   *
   * @param note
   * @return content
   * @throws WikiException
   */
  public String processImagesForExport(Page note) throws WikiException, IOException {
    String content = note.getContent();
    String restUploadUrl = "/portal/rest/wiki/attachments/";
    Map<String, String> urlToReplaces = new HashMap<>();
    while (content.contains(restUploadUrl)) {
      String check_content = content;
      String urlToReplace = content.split(restUploadUrl)[1].split("\"")[0];
      urlToReplace = restUploadUrl + urlToReplace;
      String attachmentId = StringUtils.substringAfterLast(urlToReplace, "/");
      Attachment attachment = wikiService.getAttachmentOfPageByName(attachmentId, note, true);
      if (attachment != null && attachment.getContent() != null) {
        InputStream bis = new ByteArrayInputStream(attachment.getContent());
        File tempFile = new File(System.getProperty("java.io.tmpdir") + File.separator + attachmentId);
        Files.copy(bis, tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        urlToReplaces.put(urlToReplace, IMAGE_URL_REPLACEMENT_PREFIX + tempFile.getName() + IMAGE_URL_REPLACEMENT_SUFFIX);
      }
      content = content.replace(urlToReplace, "");
      if (content.equals(check_content)) {
        break;
      }
    }
    if (!urlToReplaces.isEmpty()) {
      content = replaceUrl(note.getContent(), urlToReplaces);
    }
    return htmlUploadImageProcessor.processImagesForExport(content);
  }

  private void replaceIncludedPages(Page note, Wiki wiki) throws WikiException {
    Page note_ = noteService.getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), note.getName());
    if (note_ != null) {
      String content = note_.getContent();
      if (content.contains("class=\"noteLink\" href=\"//-")) {
        while (content.contains("class=\"noteLink\" href=\"//-")) {
          String linkedParams = content.split("class=\"noteLink\" href=\"//-")[1].split("-//\"")[0];
          String noteBookType = linkedParams.split("-////-")[0];
          String noteBookOwner = linkedParams.split("-////-")[1];
          String NoteName = linkedParams.split("-////-")[2];
          Page linkedNote = null;
          linkedNote = noteService.getNoteOfNoteBookByName(wiki.getType(), wiki.getOwner(), NoteName);
          if (linkedNote != null) {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//",
                                      "\"noteLink\" href=\"" + linkedNote.getId());
          } else {
            content = content.replace("\"noteLink\" href=\"//-" + linkedParams + "-//", "\"noteLink\" href=\"" + NoteName);
          }
          if (content.equals(note_.getContent()))
            break;
        }
        if (!content.equals(note_.getContent())) {
          note_.setContent(content);
          noteService.updateNote(note_);
        }
      }
    }
    if (note.getChildren() != null) {
      for (Page child : note.getChildren()) {
        replaceIncludedPages(child, wiki);
      }
    }
  }

  private String replaceUrl(String body, Map<String, String> urlToReplaces) {
    for (String url : urlToReplaces.keySet()) {
      while (body.contains(url)) {
        body = body.replace(url, urlToReplaces.get(url));
      }
    }
    return body;
  }

  private LinkedList<String> getNoteAncestorsIds(String noteId) throws WikiException {
    return getNoteAncestorsIds(null, noteId);
  }

  private LinkedList<String> getNoteAncestorsIds(LinkedList<String> ancestorsIds, String noteId) throws WikiException {
    if (ancestorsIds == null) {
      ancestorsIds = new LinkedList<>();
    }
    if (noteId == null) {
      return ancestorsIds;
    }
    Page note = noteService.getNoteById(noteId);
    String parentId = note.getParentPageId();

    if (parentId != null) {
      ancestorsIds.push(parentId);
      getNoteAncestorsIds(ancestorsIds, parentId);
    }

    return ancestorsIds;
  }

}
