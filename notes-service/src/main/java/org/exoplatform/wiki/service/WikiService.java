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
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionEntry;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.impl.SpaceBean;
import org.exoplatform.wiki.service.listener.AttachmentWikiListener;
import org.exoplatform.wiki.service.listener.PageWikiListener;

/**
 * Provides functions for processing database
 * with wikis and pages, including: adding, editing, removing and searching for data.
 *
 * @LevelAPI Provisional
 */
public interface WikiService {

  /**
   * Gets parameters of a wiki page based on the data stored in the breadcrumb.
   *
   * @param data The data in the breadcrumb that identifies the wiki page.
   * @return The parameters identifying the wiki page.
   * @throws WikiException if an error occured if an error occured
   */
  public WikiPageParams getWikiPageParams(BreadcrumbData data) throws WikiException;

  /**
   * Gets a list of Wiki permissions based on its type and owner.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @return The list of Wiki permissions.
   * @throws WikiException if an error occured if an error occured
   */
  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException;

  /**
   * Adds a list of permissions to Wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param permissionEntries The list of permissions.
   * @throws WikiException if an error occured if an error occured
   */
  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException;

  /**
   * Gets a wiki page by its unique name in the wiki.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param wikiOwner The Wiki owner.
   * @param pageName Id of the wiki page.
   * @return The wiki page if the current user has the read permission. Otherwise, it is "null".
   * @throws WikiException if an error occured if an error occured
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException;




  /**
   * Gets Id of a default Wiki syntax.
   *
   * @return The Id.
   */
  public String getDefaultWikiSyntaxId();

  /**
   * Gets attachments of the given page, without loading their content
   * @param page The wiki page
   * @return The attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException;

  /**
   * Gets attachments of the given page,
   * and allow to load their attachment content by setting loadContent to true
   * @param page The wiki page
   * @param loadContent treue if need to load the attachement content
   * @return The attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public default List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    return getAttachmentsOfPage(page);
  }

  /**
   * Get the number of attachment of the given page
   * @param page The wiki page
   * @return The number of attachments of the page
   * @throws WikiException if an error occured if an error occured
   */
  public int getNbOfAttachmentsOfPage(Page page) throws WikiException;

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
   * Get a attachment of a the given page by name,
   * and allow to load the attachment content by setting loadContent to true
   *
   * @param attachmentName The name of the attachment
   * @param page           The wiki page
   * @param loadContent    true to load the attachment content
   * @return attachement
   * @throws WikiException if an error occured if an error occured
   */
  public default Attachment getAttachmentOfPageByName(String attachmentName, Page page, boolean loadContent) throws WikiException {
    return getAttachmentOfPageByName(attachmentName, page);
  }

  /**
   * Add the given attachment to the given page
   * @param attachment The attachment to add
   * @param page The wiki page
   * @throws WikiException if an error occured if an error occured
   */
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException;

  /**
   * Deletes the given attachment of the given page
   * @param attachmentId Id of the attachment
   * @param page The wiki page
   * @throws WikiException if an error occured
   */
  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException;

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
   * @param plugin The component plugin to be registered.
   */
  public void addComponentPlugin(ComponentPlugin plugin);

  /**
   * Gets listeners of all wiki pages that are registered into the Wiki service.
   * @return The list of listeners.
   */
  public List<PageWikiListener> getPageListeners();

  /**
   * Gets attachment listeners that are registered into the Wiki service.
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
   * Gets a space name by a given group Id.
   * 
   * @param groupId The group Id.
   * @return The space name.
   */
  public String getSpaceNameByGroupId(String groupId);
  
  /**
   * Searches for spaces by a given keyword.
   * 
   * @param keyword The keyword to search for spaces.
   * @return The list of spaces matching with the keyword.
   * @throws WikiException if an error occured
   */
  public List<SpaceBean> searchSpaces(String keyword) throws WikiException;
  
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
   * @param wikiType Type of wiki
   * @return Wikis of the given type
   * @throws WikiException if an error occured
   */
  public List<Wiki> getWikisByType(String wikiType) throws WikiException;

  /**
   * Creates a wiki with the given type and owner
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
   * Check if the identity has the given permission type on a wiki
   * @param wiki Wiki
   * @param permissionType Permission type to check
   * @param user Identity of the user
   * @return true if the user has the given permission type on the wiki
   * @throws WikiException if an error occured
   */
  boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity user) throws WikiException;



  /** 
   * Checks if the current user has the admin permission on a space or not.
   *
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the space.
   * @return The returned value is "true" if the current user has the admin permission on the space, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminSpacePermission(String wikiType, String owner) throws WikiException;
  
  /**
   * Checks if the current user has the admin permission on a wiki page.
   * 
   * @param wikiType It can be Portal, Group, or User.
   * @param owner Owner of the wiki page.
   * @return "True" if the current user has the admin permission on the wiki page, or "false" if not.
   * @throws WikiException if an error occured
   */
  public boolean hasAdminPagePermission(String wikiType, String owner) throws WikiException;

  /**
   * Gets a Wiki by its Id.
   * 
   * @param wikiId The Wiki Id.
   * @return The Wiki.
   * @throws WikiException if an error occured
   */
  public Wiki getWikiById(String wikiId) throws WikiException;
  
  /**
   * Gets a Wiki name by its Id.
   *
   * @param wikiId The Wiki Id.
   * @return The Wiki name.
   * @throws WikiException if an error occured
   */
  public String getWikiNameById(String wikiId) throws WikiException;

}
