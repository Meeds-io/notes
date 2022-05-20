/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2021 Meeds Association contact@meeds.io
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

import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.*;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import java.util.List;
import java.util.Map;

public interface DataStorage {

  public Wiki getWikiByTypeAndOwner(String wikiType, String owner) throws WikiException;

  public List<Wiki> getWikisByType(String wikiType) throws WikiException;

  public Wiki createWiki(Wiki wiki) throws WikiException;

  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException;

  /**
   * Get a wiki page by its unique name in the wiki
   *
   * @param wikiType The wiki type
   * @param wikiOwner The wiki owner
   * @param pageName The unique name of the page in the wiki
   * @return The wiki page
   * @throws WikiException if an error occured
   */
  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException;

  /**
   * Get a wiki page by its unique id
   *
   * @param id The unique id of wiki page
   * @return The wiki page
   * @throws WikiException if an error occured
   */
  public Page getPageById(String id) throws WikiException;

  public DraftPage getDraftPageById(String id) throws WikiException;

  public Page getParentPageOf(Page page) throws WikiException;
  
  /**
   * Get children notes and draft notes of page
   * 
   * @param page the target page to retrieve its children
   * @param userId
   * @param withDrafts if set to true returns the children notes and draft notes
   * @return children notes of page
   * @throws WikiException
   */
  public List<Page> getChildrenPageOf(Page page, String userId, boolean withDrafts) throws WikiException;

  public boolean hasChildren(long noteId) throws WikiException;

  public void deletePage(String wikiType, String wikiOwner, String pageId) throws WikiException;

  public void deleteDraftOfPage(Page page, String username) throws WikiException;

  public void deleteDraftByName(String newDraftPageName, String username) throws WikiException;

  public void renamePage(String wikiType, String wikiOwner, String pageName, String newName, String newTitle) throws WikiException;

  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException;

  public List<PermissionEntry> getWikiPermission(String wikiType, String wikiOwner) throws WikiException;

  public void updateWikiPermission(String wikiType, String wikiOwner, List<PermissionEntry> permissionEntries) throws WikiException;

  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException;

  public Page getRelatedPage(String wikiType, String wikiOwner, String pageId) throws WikiException;

  public void addRelatedPage(Page page, Page relatedPage) throws WikiException;

  public void removeRelatedPage(Page page, Page relatedPage) throws WikiException;

  public Page getExsitedOrNewDraftPageById(String wikiType, String wikiOwner, String pageId, String username) throws WikiException;

  public DraftPage getDraft(WikiPageParams param, String username) throws WikiException;

  public DraftPage getLastestDraft(String username) throws WikiException;

  /**
   * Returns latest draft of given page.
   * 
   * @param targetPage
   * @param username
   * @return
   * @throws WikiException
   */
  DraftPage getLatestDraftOfPage(Page targetPage, String username) throws WikiException;

  public DraftPage getDraft(String draftName, String username) throws WikiException;

  public List<DraftPage> getDraftPagesOfUser(String username) throws WikiException;

  /**
   * Creates a new draft note
   * 
   * @param draftPage The draft note to be created
   * @param username Author name
   * @return Created draft note
   * @throws WikiException
   */
  public DraftPage createDraftPageForUser(DraftPage draftPage, String username) throws WikiException;

  /**
   * Updates a draft note
   * 
   * @param draftPage The draft note to be updated
   * @param username
   * @return Updated draft note
   * @throws WikiException
   */
  public DraftPage updateDraftPageForUser(DraftPage draftPage, String username) throws WikiException;

  public PageList<SearchResult> search(WikiSearchData data) throws WikiException;

  public List<TemplateSearchResult> searchTemplate(TemplateSearchData data) throws WikiException;

  public default List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    return getAttachmentsOfPage(page);
  }

  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException;

  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException;

  public void deleteAttachmentOfPage(String attachmentId, Page page) throws WikiException;

  public Page getHelpSyntaxPage(String syntaxId, boolean fullContent, List<ValuesParam> syntaxHelpParams, ConfigurationManager configurationManager) throws WikiException;

  /**
   * Check if the identity has the given permission type on a page
   * @param page Page
   * @param permissionType Permission type to check
   * @param user Identity of the user
   * @return true if the user has the given permission type on the page
   * @throws WikiException if an error occured
   */
  public boolean hasPermissionOnPage(Page page, PermissionType permissionType, Identity user) throws WikiException;

  /**
   * Check if the identity has the given permission type on a wiki
   * @param wiki Wiki
   * @param permissionType Permission type to check
   * @param identity Identity of the user
   * @return true if the user has the given permission type on the wiki
   * @throws WikiException if an error occured
   */
  public boolean hasPermissionOnWiki(Wiki wiki, PermissionType permissionType, Identity identity) throws WikiException;

  public boolean hasAdminSpacePermission(String wikiType, String owner, Identity user) throws WikiException;

  public boolean hasAdminPagePermission(String wikiType, String owner, Identity user) throws WikiException;

  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException;

  public List<PageHistory> getHistoryOfPage(Page page) throws WikiException;

  public void addPageVersion(Page page, String userName) throws WikiException;

  public void restoreVersionOfPage(String versionName, Page page) throws WikiException;

  public Page updatePage(Page page) throws WikiException;

  public List<String> getPreviousNamesOfPage(Page page) throws WikiException;

  public List<String> getWatchersOfPage(Page page) throws WikiException;

  public void addWatcherToPage(String username, Page page) throws WikiException;

  public void deleteWatcherOfPage(String username, Page page) throws WikiException;

  /**
   * Retrieve the all pages contained in wiki
   * 
   * @param wikiType the wiki type
   * @param wikiOwner the wiki owner
   * @return the List pf pages of the wiki
   */
  public List<Page> getPagesOfWiki(String wikiType, String wikiOwner);
}
