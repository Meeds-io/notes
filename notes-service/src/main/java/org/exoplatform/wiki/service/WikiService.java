/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service;

import java.util.List;

import org.exoplatform.container.component.ComponentPlugin;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionEntry;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;

/**
 * Provides functions for processing database with wikis and pages, including:
 * adding, editing, removing and searching for data.
 *
 * @LevelAPI Provisional
 */
public interface WikiService {

  /**
   * Gets Id of a default Wiki syntax.
   *
   * @return The Id.
   */
  public String getDefaultWikiSyntaxId();

  /**
   * Get a attachment of a the given page by name, without loading its content
   *
   * @param attachmentName The name of the attachment
   * @param page The wiki page
   * @return Attachment
   * @throws WikiException if an error occured if an error occured
   */
  public Attachment getAttachmentOfPageByName(String attachmentName, Page page) throws WikiException;

  /**
   * Get a attachment of a the given page by name, and allow to load the
   * attachment content by setting loadContent to true
   *
   * @param attachmentName The name of the attachment
   * @param page The wiki page
   * @param loadContent true to load the attachment content
   * @return attachement
   * @throws WikiException if an error occured if an error occured
   */
  public default Attachment getAttachmentOfPageByName(String attachmentName,
                                                      Page page,
                                                      boolean loadContent) throws WikiException {
    return getAttachmentOfPageByName(attachmentName, page);
  }

  /**
   * Gets a list of Wiki default permissions.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki default permissions.
   * @throws WikiException if an error occured
   */
  public List<PermissionEntry> getWikiDefaultPermissions(String wikiType, String wikiOwner) throws WikiException;

  /**
   * Registers a component plugin into the Wiki service.
   * 
   * @param plugin The component plugin to be registered.
   */
  public void addComponentPlugin(ComponentPlugin plugin);

  /**
   * Gets listeners of all wiki pages that are registered into the Wiki service.
   * 
   * @return The list of listeners.
   */
  public List<PageWikiListener> getPageListeners();

  /**
   * Gets attachment listeners that are registered into the Wiki service.
   * 
   * @return The list of attachment listeners.
   */
  public List<AttachmentWikiListener> getAttachmentListeners();

  /**
   * Gets a user Wiki. If it does not exist, the new one will be created.
   * 
   * @param username Name of the user.
   * @return The user Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getOrCreateUserWiki(String username) throws WikiException;

  /**
   * Gets a Wiki which is defined by its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @return The Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException;

  /**
   * Gets all wikis of the given type
   * 
   * @param wikiType Type of wiki
   * @return Wikis of the given type
   * @throws WikiException if an error occured
   */
  public List<Wiki> getWikisByType(String wikiType) throws WikiException;

  /**
   * Creates a wiki with the given type and owner
   * 
   * @param wikiType It can be Portal, Group, or User.
   * @param owner The Wiki owner.
   * @return Wiki created
   * @throws WikiException if an error occured
   */
  public Wiki createWiki(String wikiType, String owner) throws WikiException;

  /**
   * Gets a Wiki webapp URI.
   * 
   * @return The Wiki webapp URI.
   */
  public String getWikiWebappUri();

  /**
   * Gets a Wiki by its Id.
   * 
   * @param wikiId The Wiki Id.
   * @return The Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getWikiById(String wikiId) throws WikiException;

}
