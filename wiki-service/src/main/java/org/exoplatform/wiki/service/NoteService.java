/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.service;

import java.util.List;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.*;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.gatein.api.EntityNotFoundException;

/**
 * Provides functions for processing database with notes, including:
 * adding, editing, removing and searching for data.
 *
 */
public interface NoteService {

  /**
   * Create a new note in the given notebook, under the given parent note.
   *
   * @param noteBook Notebook object.
   * @param parentNote  parent note.
   * @param note  the note to create.
   * @return The new note.
   * @throws WikiException if an error occured
   */
  public Page createNote(Wiki noteBook, Page parentNote, Page note) throws WikiException;

  /**
   * Create a new note in the given notebook, under the given parent note.
   *
   * @param noteBook Notebook object.
   * @param parentNoteName  parent note name.
   * @param note  the note object to create.
   * @param userIdentity  user Identity.
   * @return The new note.
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights to the parent note
   */
  Page createNote(Wiki noteBook, String parentNoteName, Page note, Identity userIdentity) throws WikiException, IllegalAccessException;

  /**
   * Deletes a note.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteId Id of the note.
   * @return "True" if deleting the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean deleteNote(String noteType, String noteOwner, String noteId) throws WikiException;

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
  public boolean renameNote(String noteType,
                            String noteOwner,
                            String noteName,
                            String newName,
                            String newTitle) throws WikiException;

  /**
   * Move a note
   *
   * @param currentLocationParams The current location of the note.
   * @param newLocationParams The new location of the note.
   * @throws WikiException if an error occured
   */
  public void moveNote(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException;

  /**
   * Move a note
   *
   * @param currentLocationParams The current location of the note.
   * @param newLocationParams The new location of the note.
   * @param userIdentity The user Identity to check permissions.
   * @return "True" if moving the note is successful, or "false" if not.
   * @throws WikiException if an error occured
   * @throws IllegalAccessException if the user don't have edit rights on the note
   * @throws EntityNotFoundException if the the note to move don't exist
   */
  boolean moveNote(WikiPageParams currentLocationParams, WikiPageParams newLocationParams, Identity userIdentity) throws WikiException, IllegalAccessException, EntityNotFoundException;

  /**
   * Gets a note by its unique name in the noteBook.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The NoteBook owner.
   * @param noteName Id of the note.
   * @return The note if the current user has the read permission. Otherwise,
   *         it is "null".
   * @throws WikiException if an error occured
   */
  public Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName) throws WikiException;


  Page getNoteOfNoteBookByName(String noteType, String noteOwner, String noteName, Identity userIdentity) throws WikiException, IllegalAccessException;

  /**
   * Gets a note based on its unique id.
   *
   * @param id Unique id of the note.
   * @return The note.
   * @throws WikiException if an error occured
   */
  public Page getNoteById(String id) throws WikiException;


  Page getNoteById(String id, Identity userIdentity) throws IllegalAccessException, WikiException;

  /**
   * Get parent note of a note
   * 
   * @param note note.
   * @return The list of children notes
   * @throws WikiException if an error occured
   */
  public Page getParentNoteOf(Page note) throws WikiException;

  /**
   * Get all the children notes of a note
   * 
   * @param note note.
   * @return The list of children notes
   * @throws WikiException if an error occured
   */
  public List<Page> getChildrenNoteOf(Page note) throws WikiException;

  /**
   * Gets a list of data which is used for composing the breadcrumb.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param noteOwner The owner.
   * @param noteId Id of the note to which the breadcrumb points.
   * @return The list of data.
   * @throws WikiException if an error occured
   */
  public List<BreadcrumbData> getBreadcumb(String noteType, String noteOwner, String noteId) throws WikiException;

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
  public List<Page> getDuplicateNotes(Page parentNote, Wiki targetNoteBook, List<Page> resultList) throws WikiException;


  void removeDraftOfNote(WikiPageParams param) throws WikiException;

  /**
   * Checks if the given user has the permission on a note
   * 
   * @param user the user
   * @param note the note to check
   * @param permissionType type of permissions to chack
   * @return true if user has permissions on the note
   * @throws WikiException if an error occured
   */
  public boolean hasPermissionOnNote(Page note, PermissionType permissionType, Identity user) throws WikiException;

  /**
   * Checks if the current user has the admin permission on a space or not.
   *
   * @param noteType It can be Portal, Group, or User.
   * @param owner Owner of the space.
   * @param user Identity of current user.
   * @return The returned value is "true" if the current user has the admin
   *         permission on the space, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminSpacePermission(String noteType, String owner, Identity user) throws WikiException;

  /**
   * Checks if the current user has the admin permission on a note.
   * 
   * @param noteType It can be Portal, Group, or User.
   * @param owner Owner of the noteBook.
   * @param user Identity of current user.
   * @return "True" if the current user has the admin permission on the note,
   *         or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminNotePermission(String noteType, String owner, Identity user) throws WikiException;

  /**
   * Check if the given user can update the note
   * 
   * @param currentNote The note to update
   * @param currentIdentity The identity of user user that needs to update the note
   * @return true if the user can update the note
   * @throws WikiException if an error occured
   */
  public boolean canModifyNotePermission(Page currentNote, Identity currentIdentity) throws WikiException;

  /**
   * Check if the given user can public or restrict the note
   * 
   * @param currentNote the note to chack permissions
   * @param currentIdentity The identity of user
   * @return true if the current user has EditNote permission or admin note or
   *         admin space
   * @throws WikiException if an error occured
   */
  public boolean canPublicAndRetrictNote(Page currentNote , Identity currentIdentity) throws WikiException;

  /**
   * Gets all the versions of the given note
   * 
   * @param note The note
   * @return All the versions of the note
   * @throws WikiException if an error occured
   */
  public List<PageVersion> getVersionsOfNote(Page note) throws WikiException;

  /**
   * Gets a specific version by name of the given note
   * 
   * @param versionName The name of the version
   * @param note The note
   * @return The version of the note
   * @throws WikiException if an error occured
   */
  public PageVersion getVersionOfNoteByName(String versionName, Page note) throws WikiException;

  /**
   * Creates a version of a note. This method only tag the current note data as a
   * new version, it does not update the note data
   * 
   * @param note The note
   * @throws WikiException if an error occured
   */
  public void createVersionOfNote(Page note) throws WikiException;

  /**
   * Restores a version of a note
   * 
   * @param versionName The name of the version to restore
   * @param note The note
   * @throws WikiException if an error occured
   */
  public void restoreVersionOfNote(String versionName, Page note) throws WikiException;


  /**
   * Update the given note.
   * 
   * @param note Updated note
   * @throws WikiException if an error occured
   */
  public void updateNote(Page note) throws WikiException;

  /**
   * Update the given note. This does not automatically create a new version. If a
   * new version must be created it should be explicitly done by calling
   * createVersionOfNote(). The second parameter is the type of update done (title
   * only, content only, both, move, ...).
   *
   * @param note Updated note
   * @param type Type of update
   * @param userIdentity user Identity
   * @return The updated note
   * @throws  WikiException if an error occure
   * @throws IllegalAccessException if the user don't have edit rights on the note
   * @throws EntityNotFoundException if the the note to update don't exist
   */
  Page updateNote(Page note, PageUpdateType type, Identity userIdentity) throws WikiException, IllegalAccessException, EntityNotFoundException;

  /**
   * Get previous names of a note
   * 
   * @param note The note
   * @return List of all the previous names of the note
   * @throws WikiException if an error occured
   */
  public List<String> getPreviousNamesOfNote(Page note) throws WikiException;

  /**
   * Retrieve the all notes contained in noteBook
   * 
   * @param noteType the notebook Type It can be Portal, Group, or User.
   * @param noteOwner the notebook owner
   * @return List of pages
   */
  public List<Page> getNotesOfWiki(String noteType, String noteOwner);

  boolean isExisting(String noteBookType, String noteBookOwner, String noteId) throws WikiException;

  Page getNoteByRootPermission(String noteBookType, String noteBookOwner, String noteId) throws WikiException;
}
