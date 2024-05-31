/**
* This file is part of the Meeds project (https://meeds.io/).
*
* Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
*
* This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Lesser General Public
* License as published by the Free Software Foundation; either
* version 3 of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
* Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with this program; if not, write to the Free Software Foundation,
* Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
*/

package org.exoplatform.wiki.service;

import java.io.IOException;
import java.util.List;

import org.gatein.api.EntityNotFoundException;

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.NoteToExport;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

/**
 * Provides functions for processing database with notes, including: adding,
 * editing, removing and searching for data.
 */
public interface NoteService {

  /**
   * Create a new note in the given notebook, under the given parent note.
   *
   * @param noteBook Notebook object.
   * @param parentNote parent note.
   * @param note the note to create.
   * @return The new note.
   * @throws WikiException if an error occured
   */
  Page createNote(Wiki noteBook, Page parentNote, Page note) throws WikiException;

  /**
   * Create a new note in the given notebook, under the given parent note.
   *
   * @param noteBook Notebook object.
   * @param parentNoteName parent note name.
   * @param note the note object to create.
   * @param userIdentity user Identity.
   * @return The new note.
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights to the
   *           parent note
   */
  Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException,
                                                                                          IllegalAccessException;

  /**
   * Deletes a note.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteId Id of the note.
   * @return "True" if deleting the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   */
  boolean deleteNote(String noteType, String noteOwner, String noteId) throws WikiException;

  /**
   * Deletes a note.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteName Name of the note.
   * @param userIdentity User identity deleting the note.
   * @return "True" if deleting the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   */
  boolean deleteNote(String noteType, String noteOwner, String noteName, Identity userIdentity) throws WikiException,
                                                                                                IllegalAccessException,
                                                                                                EntityNotFoundException;

  /**
   * Renames a note.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteName Old name of the note.
   * @param newName New name of the note.
   * @param newTitle New title of the note.
   * @return "True" if renaming the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   */
  boolean renameNote(String noteType, String noteOwner, String noteName, String newName, String newTitle) throws WikiException;

  /**
   * Move a note
   *
   * @param currentLocationParams The current location of the note.
   * @param newLocationParams The new location of the note.
   * @throws WikiException if an error occured
   */
  void moveNote(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException;

  /**
   * Move a note
   *
   * @param currentLocationParams The current location of the note.
   * @param newLocationParams The new location of the note.
   * @param userIdentity The user Identity to check permissions.
   * @return "True" if moving the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights on the
   *           note
   * @throws EntityNotFoundException if the the note to move don't exist
   */
  boolean moveNote(WikiPageParams currentLocationParams,
                   WikiPageParams newLocationParams,
                   Identity userIdentity) throws WikiException, IllegalAccessException, EntityNotFoundException;

  /**
   * Gets a note by its unique name in the noteBook.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteName Id of the note.
   * @return The note if the current user has the read permission. Otherwise, it
   *         is "null".
   * @throws WikiException if an error occured
   */
  Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName) throws WikiException;

  /**
   * Gets a note by its unique name in the noteBook.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteName Id of the note.
   * @param userIdentity User identity getting the note.
   * @return The note if the current user has the read permission. Otherwise, it
   *         is "null".
   * @throws WikiException if an error occured
   */
  Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName, Identity userIdentity) throws WikiException,
                                                                                                          IllegalAccessException;

  /**
   * Retrieves a note by note type, owner, name and lang.
   *
   * @param noteType note type
   * @param noteOwner note owner
   * @param noteName note name
   * @param lang note version language
   * @param userIdentity user identity id
   * @return {@link Page}
   * @throws WikiException
   * @throws IllegalAccessException
   */
  Page getNoteOfNoteBookByName(String noteType,
                               String noteOwner,
                               String noteName,
                               String lang,
                               Identity userIdentity) throws WikiException, IllegalAccessException;

  /**
   * Retrieves a note by note type, owner and name.
   *
   * @param noteType note type
   * @param noteOwner note owner
   * @param noteName note name
   * @param userIdentity user identity id
   * @return {@link Page}
   * @throws WikiException
   * @throws IllegalAccessException
   */
  Page getNoteOfNoteBookByName(String noteType,
                               String noteOwner,
                               String noteName,
                               Identity userIdentity,
                               String source) throws WikiException, IllegalAccessException;

  /**
   * Gets a note based on its unique id.
   *
   * @param id Unique id of the note.
   * @return The note.
   * @throws WikiException if an error occured
   */
  Page getNoteById(String id) throws WikiException;

  /**
   * Gets a draft note based on its unique id.
   *
   * @param id Unique id of the draft note.
   * @param userId user id
   * @return The note.
   * @throws WikiException if an error occured
   */
  DraftPage getDraftNoteById(String id, String userId) throws WikiException, IllegalAccessException;


  /**
   * Returns latest draft of given page.
   *
   * @param targetPage
   * @return latest draft of the given page
   * @throws WikiException
   */
  DraftPage getLatestDraftOfPage(Page targetPage) throws WikiException;

  /**
   * Returns latest draft of given page.
   *
   * @param targetPage
   * @param username
   * @return latest draft of the given page
   * @throws WikiException
   */
  @Deprecated 
  //Use {@link getLatestDraftOfPage(Page targetPage)} instead
  DraftPage getLatestDraftOfPage(Page targetPage, String username) throws WikiException;

  /**
   * Gets a note based on its unique id.
   *
   * @param id Unique id of the note.
   * @param userIdentity user identity id getting the note
   * @return The note.
   * @throws WikiException if an error occured
   */
  Page getNoteById(String id, Identity userIdentity) throws IllegalAccessException, WikiException;

  /**
   * Gets a note based on its unique id.
   *
   * @param id Unique id of the note.
   * @param userIdentity user identity id getting the note
   * @param source the source of the note
   * @return The note.
   * @throws WikiException if an error occured
   */
  Page getNoteById(String id, Identity userIdentity, String source) throws IllegalAccessException, WikiException;

  /**
   * Get parent note of a note
   * 
   * @param note note.
   * @return The list of children notes
   * @throws WikiException if an error occured
   */
  Page getParentNoteOf(Page note) throws WikiException;

  /**
   * Get all the children notes of a note
   *
   * @param note note.
   * @param withDrafts if set to true returns the children notes and draft notes
   * @return The list of children notes
   * @throws WikiException if an error occured
   */
  List<Page> getChildrenNoteOf(Page note, boolean withDrafts, boolean withChild) throws WikiException;

  /**
   * Get all the children notes of a note
   *
   * @param note note.
   * @param userId
   * @param withDrafts if set to true returns the children notes and draft notes
   * @return The list of children notes
   * @throws WikiException if an error occured
   */
  @Deprecated
  //Use {@link getChildrenNoteOf(Page note, boolean withDrafts, boolean withChild)} instead
  List<Page> getChildrenNoteOf(Page note, String userId, boolean withDrafts, boolean withChild) throws WikiException;

  /**
   * Gets a list of data which is used for composing the breadcrumb.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The owner.
   * @param noteId Id of the note to which the breadcrumb points.
   * @param isDraftNote
   * @return The list of data.
   * @throws WikiException if an error occured
   */
  List<BreadcrumbData> getBreadCrumb(String noteType,
                                     String noteOwner,
                                     String noteId,
                                     boolean isDraftNote) throws WikiException, IllegalAccessException;

  /**
   * Gets a list of data which is used for composing the breadcrumb.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The owner.
   * @param lang lang to be used to get translated titles.
   * @param userIdentity Current user identity.
   * @param isDraftNote
   * @return The list of data.
   * @throws WikiException if an error occured
   */
  List<BreadcrumbData> getBreadCrumb(String noteType,
                                     String noteOwner,
                                     String noteName,
                                     String lang,
                                     Identity userIdentity,
                                     boolean isDraftNote) throws WikiException, IllegalAccessException;

  /**
   * Checks if a note and its children are duplicated with ones in the target
   * NoteBook or not, then gets a list of duplicated notes if any.
   * 
   * @param parentNote The note to check.
   * @param targetNoteBook The target NoteBook to check.
   * @param resultList The list of duplicated notes.
   * @return The list of duplicated notes.
   * @throws WikiException if an error occured
   */
  List<Page> getDuplicateNotes(Page parentNote, Wiki targetNoteBook, List<Page> resultList) throws WikiException;

  /**
   * Checks if a note and its children are duplicated with ones in the target
   * NoteBook or not, then gets a list of duplicated notes if any.
   * 
   * @param parentNote The note to check.
   * @param targetNoteBook The target NoteBook to check.
   * @param resultList The list of duplicated notes.
   * @param userId
   * @return The list of duplicated notes.
   * @throws WikiException if an error occured
   */
  @Deprecated
  // Use {@link getDuplicateNotes(Page parentNote, Wiki targetNoteBook, List<Page> resultList)} instead
  List<Page> getDuplicateNotes(Page parentNote, Wiki targetNoteBook, List<Page> resultList, String userId) throws WikiException;

  /**
   * Remove the all Drafts of a note
   *
   * @param param Note location params.
   * @throws WikiException if an error occured
   */
  void removeDraftOfNote(WikiPageParams param) throws WikiException;

  /**
   * Remove the Drafts of a note by language
   *
   * @param param Note location params.
   * @param lang draft lang.
   * @throws WikiException if an error occured
   */
  void removeDraftOfNote(WikiPageParams param, String lang) throws WikiException;

  /**
   * Remove the Drafts of a note
   *
   * @param page Note page.
   * @throws WikiException if an error occured
   */
  void removeDraftOfNote(Page page) throws WikiException;

  /**
   * Remove the Drafts of a note by username
   *
   * @param page Note page.
   * @param username username.
   * @throws WikiException if an error occured
   */
  @Deprecated
  // Use {@link removeDraftOfNote(Page page)} instead
  void removeDraftOfNote(Page page, String username) throws WikiException;

  /**
   * Removes a draft page by its name.
   *
   * @param draftName Name of the draft page.
   * @throws WikiException if an error occured
   */
  void removeDraft(String draftName) throws WikiException;

  /**
   * Removes a draft page by its technical id.
   *
   * @param draftId Technical Id of the draft page.
   * @throws WikiException if an error occured
   */
  void removeDraftById(String draftId) throws WikiException;

  /**
   * Gets all the Histories of the given note
   *
   * @param note The note
   * @param userName the author name
   * @return All the histories of the note
   * @throws WikiException if an error occured
   */
  List<PageHistory> getVersionsHistoryOfNote(Page note, String userName) throws WikiException;

  /**
   * Retrieves the history version of given note by language
   *
   * @param note the target note id
   * @param userName user name
   * @param lang content language
   * @return {@link List} of {@link PageHistory}
   */
  List<PageHistory> getVersionsHistoryOfNoteByLang(Page note, String userName, String lang) throws WikiException;

  /**
   * Creates a version of a note. This method only tag the current note data as
   * a new version, it does not update the note data
   * 
   * @param note The note
   * @param userName the author name
   * @throws WikiException if an error occured
   */
  void createVersionOfNote(Page note, String userName) throws WikiException;

  /**
   * Restores a version of a note
   * 
   * @param versionName The name of the version to restore
   * @param note The note
   * @param userName the other name
   * @throws WikiException if an error occured
   */
  void restoreVersionOfNote(String versionName, Page note, String userName) throws WikiException;

  /**
   * Update the given note.
   * 
   * @param note Updated note
   * @throws WikiException if an error occured
   * @return updated note
   */
  Page updateNote(Page note) throws WikiException;

  /**
   * Update the given note. This does not automatically create a new version. If
   * a new version must be created it should be explicitly done by calling
   * createVersionOfNote(). The second parameter is the type of update done
   * (title only, content only, both, move, ...).
   *
   * @param note Updated note
   * @param type Type of update
   * @param userIdentity user Identity
   * @return The updated note
   * @throws WikiException if an error occure
   * @throws IllegalAccessException if the user don't have edit rights on the
   *           note
   * @throws EntityNotFoundException if the the note to update don't exist
   */
  Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws WikiException,
                                                                         IllegalAccessException,
                                                                         EntityNotFoundException;

  /**
   * Update the given note. This does not automatically create a new version. If
   * a new version must be created it should be explicitly done by calling
   * createVersionOfNote(). The second parameter is the type of update done
   * (title only, content only, both, move, ...).
   * 
   * @param note Updated note
   * @param type Type of update
   * @return The updated note
   * @throws WikiException
   */
  Page updateNote(Page note, PageUpdateType type) throws WikiException;

  /**
   * Get previous names of a note
   * 
   * @param note The note
   * @return List of all the previous names of the note
   * @throws WikiException if an error occured
   */
  List<String> getPreviousNamesOfNote(Page note) throws WikiException;

  /**
   * Retrieve the all notes contained in noteBook
   * 
   * @param noteType the notebook Type It can be Portal, Group, or User.
   * @param noteOwner the notebook owner
   * @return List of pages
   */
  List<Page> getNotesOfWiki(String noteType, String noteOwner);

  /**
   * Check if the given note is existing
   * 
   * @param noteBookType the notebook Type It can be Portal, Group, or User.
   * @param noteBookOwner the notebook owner
   * @param noteId the note id
   * @return true if the note is existing
   */
  boolean isExisting(String noteBookType, String noteBookOwner, String noteId) throws WikiException;

  /**
   * Update draft note for an existing page
   *
   * @param draftNoteToUpdate The draft note to be updated
   * @param targetNote The target note of the draft
   * @param revision The revision which is used for creating the draft page. If
   *          "null", this will be the last revision.
   * @param currentTimeMillis
   * @param userName The author name
   * @return Updated draft
   * @throws WikiException
   */
  DraftPage updateDraftForExistPage(DraftPage draftNoteToUpdate,
                                    Page targetNote,
                                    String revision,
                                    long currentTimeMillis,
                                    String userName) throws WikiException;

  /**
   * Update draft note for a new page
   *
   * @param draftNoteToUpdate the draft note to be updated
   * @param currentTimeMillis
   * @return Updated draft
   * @throws WikiException
   */
  DraftPage updateDraftForNewPage(DraftPage draftNoteToUpdate, long currentTimeMillis) throws WikiException;

  /**
   * Creates a draft for an existing page
   *
   * @param draftNoteToSave The draft note to be created
   * @param targetNote The target note of the draft
   * @param revision The revision which is used for creating the draft page. If
   *          "null", this will be the last revision.
   * @param currentTimeMillis
   * @param username The author name
   * @return Created draft
   * @throws WikiException
   */
  DraftPage createDraftForExistPage(DraftPage draftNoteToSave,
                                    Page targetNote,
                                    String revision,
                                    long currentTimeMillis,
                                    String username) throws WikiException;

  /**
   * Creates a draft for a new page
   *
   * @param draftNoteToSave The draft note to be created
   * @param currentTimeMillis
   * @return Created draft
   * @throws WikiException
   */
  DraftPage createDraftForNewPage(DraftPage draftNoteToSave, long currentTimeMillis) throws WikiException;

  /**
   * Return the list of children of the note to export
   *
   * @param note The Note to export
   * @return the list of children of the note to export
   * @throws WikiException
   */
  List<NoteToExport> getChildrenNoteOf(NoteToExport note) throws WikiException;

  /**
   * Return the list of children of the note to export
   *
   * @param note The Note to export
   * @param userId the current user Id
   * @return the list of children of the note to export
   * @throws WikiException
   */
  @Deprecated
  // Use {@link getChildrenNoteOf(NoteToExport note)} instead
  List<NoteToExport> getChildrenNoteOf(NoteToExport note, String userId) throws WikiException;

  /**
   * Return the Parent of the note to export
   *
   * @param note The Note to export
   * @return the parent of the note to export
   * @throws WikiException
   */
  NoteToExport getParentNoteOf(NoteToExport note) throws WikiException;

  /**
   * Return the content of the note to be rendred
   *
   * @param note The Note
   * @return Content to be rendred
   */
  String getNoteRenderedContent(Page note);

  /**
   * Import Notes from a zip file location
   *
   * @param zipLocation the zip file location path
   * @param parent The parent page where notes will be impoprted
   * @param conflict import strategy ( can be
   *          "overwrite","replaceAll","duplicate" or "duplicate")
   * @param userIdentity current user Identity
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights on the
   *           note
   * @throws IOException if can't read zip file
   */
  void importNotes(String zipLocation, Page parent, String conflict, Identity userIdentity) throws WikiException,
                                                                                            IllegalAccessException,
                                                                                            IOException;

  /**
   * Import Notes from a list of files
   *
   * @param files the list of files
   * @param parent The parent page where notes will be imported
   * @param conflict import strategy ( can be
   *          "overwrite","replaceAll","duplicate" or "duplicate")
   * @param userIdentity current user Identity
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights on the
   *           note
   * @throws IOException if can't read files
   */
  void importNotes(List<String> files, Page parent, String conflict, Identity userIdentity) throws WikiException,
                                                                                            IllegalAccessException,
                                                                                            IOException;

  /**
   * Searches in all wiki pages.
   *
   * @param data The data to search.
   * @return Search results.
   * @throws WikiException if an error occured if an error occured
   */
  PageList<SearchResult> search(WikiSearchData data) throws WikiException;

  /**
   * Gets a wiki page regardless of the current user's permission.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageId Id of the wiki page.
   * @return The wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  Page getNoteByRootPermission(String wikiType, String wikiOwner, String pageId) throws WikiException;

  /**
   * Checks if the given user has the permission on a page
   * 
   * @param user the userName
   * @param page the wiki page object
   * @param permissionType permission Type
   * @return true if user has permissions
   * @throws WikiException if an error occured
   */
  boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException;

  /**
   * Retrieves note page by its id and content lang
   * 
   * @param pageId page id
   * @param userIdentity user identity id
   * @param source source
   * @param lang content language
   * @return {@link Page}
   * @throws WikiException
   * @throws IllegalAccessException
   */
  Page getNoteByIdAndLang(Long pageId, Identity userIdentity, String source, String lang) throws WikiException,
                                                                                          IllegalAccessException;

  /**
   * Retrieves note page by its id and content lang
   * 
   * @param pageId page id
   * @param userIdentity user identity id
   * @return {@link Page}
   */
  Page getNoteByIdAndLang(Long pageId, String lang);

  /**
   * Retrieves published note version page by its page id and content lang
   *
   * @param pageId page id
   * @param lang content language
   * @return {@link PageVersion}
   */
  PageVersion getPublishedVersionByPageIdAndLang(Long pageId, String lang);

  /**
   * Retrieves list of available translations languages of a page
   *
   * @param pageId page id
   * @param withDrafts if set to true returns languages draft notes
   * @return {@link List} of {@link String}
   */
  List<String> getPageAvailableTranslationLanguages(Long pageId, boolean withDrafts) throws WikiException;

  /**
   * Retrieves list of available translations languages of a page
   *
   * @param pageId page id
   * @param userId owner username
   * @param withDrafts if set to true returns languages draft notes
   * @return {@link List} of {@link String}
   *             
   */
  @Deprecated
  // Use {@link getPageAvailableTranslationLanguages(Long pageId, boolean withDrafts)} instead
  List<String> getPageAvailableTranslationLanguages(Long pageId, String userId, boolean withDrafts) throws WikiException;

  /**
   * Retrieves latest draft of a specific page by target page id and content
   * language
   *
   * @param targetPageId target page id
   * @param lang content language
   * @return {@link DraftPage}
   */
  DraftPage getLatestDraftPageByTargetPageAndLang(Long targetPageId, String lang);

  /**
   * Retrieves latest draft of a specific page by target page id and content
   * language and owner username
   *
   * @param targetPageId target page id
   * @param username owner username
   * @param lang content language
   * @return {@link DraftPage}
   */
  @Deprecated
  // Use {@link getLatestDraftPageByTargetPageAndLang(Long targetPageId, String lang)} instead
  DraftPage getLatestDraftPageByUserAndTargetPageAndLang(Long targetPageId, String username, String lang);

  /**
   * Deletes a list of versions of note by language.
   *
   * @param noteId Id of the note.
   * @param lang language.
   * @throws WikiException if an error occured
   */
  void deleteVersionsByNoteIdAndLang(Long noteId, String lang) throws WikiException;

  /**
   * Deletes a list of versions of note by language.
   *
   * @param noteId Id of the note.
   * @param lang language.
   * @param username owner username
   * @throws WikiException if an error occured
   */
  @Deprecated
  // Use {@link deleteVersionsByNoteIdAndLang(Long noteId, String lang)} instead
  void deleteVersionsByNoteIdAndLang(Long noteId, String username, String lang) throws WikiException;


  /**
   * Remove all children drafts of a parent page without existing target
   *
   * @param parentPageId Note parent page id
   */
  void removeOrphanDraftPagesByParentPage(long parentPageId);
}
