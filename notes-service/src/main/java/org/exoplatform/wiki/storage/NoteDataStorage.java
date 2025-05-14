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
package org.exoplatform.wiki.storage;

import static org.exoplatform.wiki.storage.EntityConverter.convertAttachmentEntityToAttachment;
import static org.exoplatform.wiki.storage.EntityConverter.convertAttachmentToDraftPageAttachmentEntity;
import static org.exoplatform.wiki.storage.EntityConverter.convertAttachmentToPageAttachmentEntity;
import static org.exoplatform.wiki.storage.EntityConverter.convertDraftPageEntitiesToDraftPages;
import static org.exoplatform.wiki.storage.EntityConverter.convertDraftPageEntityToDraftPage;
import static org.exoplatform.wiki.storage.EntityConverter.convertDraftPageToDraftPageEntity;
import static org.exoplatform.wiki.storage.EntityConverter.convertPageEntityToPage;
import static org.exoplatform.wiki.storage.EntityConverter.convertPageToPageEntity;
import static org.exoplatform.wiki.storage.EntityConverter.convertPageVersionEntityToPageHistory;
import static org.exoplatform.wiki.storage.EntityConverter.convertPageVersionEntityToPageVersion;
import static org.exoplatform.wiki.storage.EntityConverter.convertWikiEntityToWiki;
import static org.exoplatform.wiki.storage.EntityConverter.convertWikiToWikiEntity;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.persistence.ExoTransactional;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.ObjectPageList;
import org.exoplatform.commons.utils.PageList;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.container.configuration.ConfigurationManager;
import org.exoplatform.container.xml.ValuesParam;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.dao.DraftPageAttachmentDAO;
import org.exoplatform.wiki.jpa.dao.DraftPageDAO;
import org.exoplatform.wiki.jpa.dao.PageAttachmentDAO;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.PageMoveDAO;
import org.exoplatform.wiki.jpa.dao.PageVersionDAO;
import org.exoplatform.wiki.jpa.dao.TemplateDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.AttachmentEntity;
import org.exoplatform.wiki.jpa.entity.DraftPageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;
import org.exoplatform.wiki.jpa.entity.PageAttachmentEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageMoveEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;
import org.exoplatform.wiki.jpa.entity.TemplateEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.WikiElasticSearchServiceConnector;
import org.exoplatform.wiki.model.Attachment;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.SearchResultType;
import org.exoplatform.wiki.service.search.TemplateSearchData;
import org.exoplatform.wiki.service.search.TemplateSearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.VersionNameComparatorDesc;

public class NoteDataStorage {

  public static final String     WIKI_TYPE_DRAFT           = "draft";

  public static final String     WIKI_FILES_NAMESPACE_NAME = "wiki";

  private WikiDAO                wikiDAO;

  private PageDAO                pageDAO;

  private PageAttachmentDAO      pageAttachmentDAO;

  private DraftPageAttachmentDAO draftPageAttachmentDAO;

  private DraftPageDAO           draftPageDAO;

  private PageVersionDAO         pageVersionDAO;

  private PageMoveDAO            pageMoveDAO;

  private TemplateDAO            templateDAO;

  private FileService            fileService;

  private UserACL                userACL;

  public NoteDataStorage(WikiDAO wikiDAO,
                     PageDAO pageDAO,
                     PageAttachmentDAO pageAttachmentDAO,
                     DraftPageAttachmentDAO draftPageAttachmentDAO,
                     DraftPageDAO draftPageDAO,
                     PageVersionDAO pageVersionDAO,
                     PageMoveDAO pageMoveDAO,
                     TemplateDAO templateDAO,
                     FileService fileService,
                     UserACL userACL) {
    this.wikiDAO = wikiDAO;
    this.pageDAO = pageDAO;
    this.pageAttachmentDAO = pageAttachmentDAO;
    this.draftPageAttachmentDAO = draftPageAttachmentDAO;
    this.draftPageDAO = draftPageDAO;
    this.pageVersionDAO = pageVersionDAO;
    this.pageMoveDAO = pageMoveDAO;
    this.templateDAO = templateDAO;
    this.fileService = fileService;
    this.userACL = userACL;
  }

  public PageList<SearchResult> search(WikiSearchData wikiSearchData) {
    if (wikiSearchData == null) {
      return new ObjectPageList<>(Collections.emptyList(), 0);
    }
    WikiElasticSearchServiceConnector searchService =
                                                    PortalContainer.getInstance()
                                                                   .getComponentInstanceOfType(WikiElasticSearchServiceConnector.class);

    List<SearchResult> searchResults = searchService.searchWiki(getSearchedText(wikiSearchData),
                                                                wikiSearchData.getUserId(),
                                                                wikiSearchData.getWikiOwner(),
                                                                wikiSearchData.getTagNames(),
                                                                wikiSearchData.isFavorites(),
                                                                wikiSearchData.isNotesTreeFilter(),
                                                                (int) wikiSearchData.getOffset(),
                                                                wikiSearchData.getLimit());

    return new ObjectPageList<>(searchResults, searchResults.size());
  }

  private String getSearchedText(WikiSearchData wikiSearchData) {
    String searchText = "";
    if (StringUtils.isNotBlank(wikiSearchData.getTitle())) {
      searchText = wikiSearchData.getTitle();
    } else if (StringUtils.isNotBlank(wikiSearchData.getContent())) {
      searchText = wikiSearchData.getContent();
    }
    return searchText;
  }

  public Wiki getWikiByTypeAndOwner(String wikiType, String wikiOwner) throws WikiException {
    return convertWikiEntityToWiki(wikiDAO.getWikiByTypeAndOwner(wikiType, wikiOwner));
  }

  public List<Wiki> getWikisByType(String wikiType) throws WikiException {
    List<Wiki> wikis = new ArrayList();
    for (WikiEntity wikiEntity : wikiDAO.getWikisByType(wikiType)) {
      wikis.add(convertWikiEntityToWiki(wikiEntity));
    }
    return wikis;
  }

  @ExoTransactional
  public Wiki createWiki(Wiki wiki) throws WikiException {
    WikiEntity createdWikiEntity = wikiDAO.create(convertWikiToWikiEntity(wiki, wikiDAO));
    Wiki createdWiki = convertWikiEntityToWiki(createdWikiEntity);

    // create Home page
    Page wikiHomePage = new Page();
    wikiHomePage.setWikiType(wiki.getType());
    wikiHomePage.setWikiOwner(wiki.getOwner());
    wikiHomePage.setName(NoteConstants.NOTE_HOME_NAME);
    wikiHomePage.setTitle(NoteConstants.NOTE_HOME_TITLE);
    Date now = Calendar.getInstance().getTime();
    wikiHomePage.setCreatedDate(now);
    wikiHomePage.setUpdatedDate(now);
    wikiHomePage.setContent("");
    // inherit syntax from wiki
    wikiHomePage.setSyntax(createdWiki.getPreferences().getWikiPreferencesSyntax().getDefaultSyntax());
    Page createdWikiHomePage = createPage(createdWiki, null, wikiHomePage);
    createdWiki.setWikiHome(createdWikiHomePage);

    return createdWiki;
  }

  @ExoTransactional
  public Page createPage(Wiki wiki, Page parentPage, Page page) throws WikiException {
    WikiEntity wikiEntity = wikiDAO.getWikiByTypeAndOwner(wiki.getType(), wiki.getOwner());
    if (wikiEntity == null) {
      throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName() +
          " because wiki does not exist.");
    }

    PageEntity parentPageEntity = null;
    if (parentPage != null) {
      parentPageEntity = pageDAO.getPageOfWikiByName(wiki.getType(), wiki.getOwner(), parentPage.getName());
      if (parentPageEntity == null) {
        throw new WikiException("Cannot create page " + wiki.getType() + ":" + wiki.getOwner() + ":" + page.getName() +
            " because parent page " + parentPage.getName() + " does not exist.");
      }
    }
    PageEntity pageEntity = convertPageToPageEntity(page, wikiDAO);
    pageEntity.setWiki(wikiEntity);
    pageEntity.setParentPage(parentPageEntity);

    Date now = GregorianCalendar.getInstance().getTime();
    if (pageEntity.getCreatedDate() == null) {
      pageEntity.setCreatedDate(now);
    }
    if (pageEntity.getUpdatedDate() == null) {
      pageEntity.setUpdatedDate(now);
    }

    pageEntity.setDeleted(false);

    PageEntity createdPageEntity = pageDAO.create(pageEntity);

    // if the page to create is the Home, update the wiki
    if (parentPage == null && NoteConstants.NOTE_HOME_NAME.equals(createdPageEntity.getName())) {
      wikiEntity.setWikiHome(createdPageEntity);
      wikiDAO.update(wikiEntity);
    }

    return convertPageEntityToPage(createdPageEntity);
  }

  public Page getPageOfWikiByName(String wikiType, String wikiOwner, String pageName) throws WikiException {
    // getCurrentNewDraftWikiPage from org.exoplatform.wiki.commons.Utils can
    // call this method with wikiType
    // and wikiOwner null. This will cause an error in the pageDAO
    if (wikiType == null || wikiOwner == null)
      return null;
    if (WIKI_TYPE_DRAFT.equals(wikiType)) {
      return convertDraftPageEntityToDraftPage(draftPageDAO.findDraftPageByName(pageName));
    } else {
      return convertPageEntityToPage(pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName));
    }
  }

  public Page getPageById(String id) {
    return convertPageEntityToPage(pageDAO.find(Long.parseLong(id)));
  }

  public DraftPage getDraftPageById(String id) {
    return convertDraftPageEntityToDraftPage(draftPageDAO.find(Long.parseLong(id)));
  }

  public Page getParentPageOf(Page page) throws WikiException {
    Page parentPage = null;

    PageEntity childPageEntity = null;
    if (page.getId() != null && !page.getId().isEmpty()) {
      childPageEntity = pageDAO.find(Long.parseLong(page.getId()));
    } else {
      childPageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    }

    if (childPageEntity != null) {
      parentPage = convertPageEntityToPage(childPageEntity.getParentPage());
    }

    return parentPage;
  }

  public List<Page> getChildrenPageOf(Page page, boolean withDrafts, boolean withChild) {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    if (pageEntity == null) {
      throw new WikiException("Cannot get children of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    List<Page> childrenPages = new ArrayList<>();
    List<PageEntity> childrenPagesEntities = pageDAO.getChildrenPages(pageEntity);
    if (childrenPagesEntities != null) {
      for (PageEntity childPageEntity : childrenPagesEntities) {
        Page childPage = convertPageEntityToPage(childPageEntity);
        if (withChild) {
          childPage.setHasChild(hasChildren(Long.parseLong(childPage.getId())));
        }
        childrenPages.add(childPage);
      }
    }

    if (withDrafts) {
      List<DraftPageEntity> draftPageEntities;
      draftPageEntities = draftPageDAO.findDraftPagesByParentPage(pageEntity.getId());

      if (!draftPageEntities.isEmpty()) {
        for (DraftPageEntity draftPageEntity : draftPageEntities) {
          childrenPages.add(convertDraftPageEntityToDraftPage(draftPageEntity));
        }
      }
    }

    return childrenPages;
  }

  public boolean hasChildren(long noteId) {
    return pageDAO.countPageChildrenById(noteId) > 0;
  }

  public boolean hasDrafts(long noteId) {
    return draftPageDAO.countDraftPagesByParentPage(noteId) > 0;
  }
  
  @ExoTransactional
  public void deletePage(String pageId) throws WikiException {
    PageEntity pageEntity = pageDAO.find(Long.parseLong(pageId));
    deletePageEntity(pageEntity);
  }

  @ExoTransactional
  public void deletePage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    if (pageEntity == null) {
      throw new WikiException("Cannot delete page " + wikiType + ":" + wikiOwner + ":" + pageName +
          " because page does not exist.");
    }
    deletePageEntity(pageEntity);
  }

  /**
   * Recursively deletes a page and all its children pages
   *
   * @param pageEntity the root page to delete
   */
  private void deletePageEntity(PageEntity pageEntity) {
    List<PageEntity> childrenPages = pageDAO.getChildrenPages(pageEntity);
    if (childrenPages != null) {
      for (PageEntity childPage : childrenPages) {
        deletePageEntity(childPage);
      }
    }

    pageEntity.setDeleted(true);
    pageDAO.update(pageEntity);
  }

  public void deleteDraftOfPage(Page page) throws WikiException {
    draftPageDAO.deleteDraftPagesByTargetPage(Long.parseLong(page.getId()));
  }

  public void deleteAttachmentsOfDraftPage(DraftPage draftPage) {
    deleteAttachmentsOfDraftPage(convertDraftPageToDraftPageEntity(draftPage, pageDAO));
  }

  public void deleteDraftOfPage(Page page, String lang) throws WikiException {
    List<DraftPageEntity> draftPages = draftPageDAO.findDraftPagesByTargetPage(Long.parseLong(page.getId()));
    for (DraftPageEntity draftPage : draftPages) {
      if (draftPage != null && StringUtils.equals(draftPage.getLang(), lang)) {
        deleteAttachmentsOfDraftPage(draftPage);
        draftPageDAO.delete(draftPage);
      }
    }
  }

  public void deleteDraftByName(String draftPageName) throws WikiException {
    DraftPageEntity draftPage = draftPageDAO.findDraftPageByName(draftPageName);
    if (draftPage != null) {
      deleteAttachmentsOfDraftPage(draftPage);
    }
    draftPageDAO.deleteDraftPagesByName(draftPageName);
  }

  public void deleteDraftById(String id) throws WikiException {
    DraftPageEntity draftPageEntity = draftPageDAO.find(Long.parseLong(id));
    draftPageDAO.delete(draftPageEntity);
  }

  @ExoTransactional
  public void renamePage(String wikiType,
                         String wikiOwner,
                         String pageName,
                         String newName,
                         String newTitle) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, pageName);
    if (pageEntity == null) {
      throw new WikiException("Cannot rename page " + wikiType + ":" + wikiOwner + ":" + pageName +
          " because page does not exist.");
    }

    // save the move in the page moves history
    List<PageMoveEntity> pageMoves = pageEntity.getMoves();
    if (pageMoves == null) {
      pageMoves = new ArrayList<>();
    }
    PageMoveEntity move = new PageMoveEntity(wikiType, wikiOwner, pageName, Calendar.getInstance().getTime());
    move.setPage(pageEntity);
    pageMoves.add(move);
    // move must be saved here because of Hibernate bug HHH-6776
    pageMoveDAO.create(move);

    pageEntity.setName(newName);
    pageEntity.setTitle(newTitle);
    pageEntity.setMoves(pageMoves);

    pageDAO.update(pageEntity);
  }

  @ExoTransactional
  public void movePage(WikiPageParams currentLocationParams, WikiPageParams newLocationParams) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(currentLocationParams.getType(),
                                                        currentLocationParams.getOwner(),
                                                        currentLocationParams.getPageName());
    if (pageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":" + currentLocationParams.getOwner() +
          ":" + currentLocationParams.getPageName() + " because page does not exist.");
    }

    PageEntity destinationPageEntity = pageDAO.getPageOfWikiByName(newLocationParams.getType(),
                                                                   newLocationParams.getOwner(),
                                                                   newLocationParams.getPageName());
    if (destinationPageEntity == null) {
      throw new WikiException("Cannot move page " + currentLocationParams.getType() + ":" + currentLocationParams.getOwner() +
          ":" + currentLocationParams.getPageName() + " to page " + newLocationParams.getType() + ":" +
          newLocationParams.getOwner() + ":" + newLocationParams.getPageName() + " because destination page does not exist.");
    }

    // save the move in the page moves history
    List<PageMoveEntity> pageMoves = pageEntity.getMoves();
    if (pageMoves == null) {
      pageMoves = new ArrayList<>();
    }
    PageMoveEntity move = new PageMoveEntity(currentLocationParams.getType(),
                                             currentLocationParams.getOwner(),
                                             currentLocationParams.getPageName(),
                                             Calendar.getInstance().getTime());
    move.setPage(pageEntity);
    // move must be saved here because of Hibernate bug HHH-6776
    pageMoveDAO.create(move);

    pageEntity.setParentPage(destinationPageEntity);
    updateWikiOfPageTree(destinationPageEntity.getWiki(), pageEntity);

    pageMoves.add(move);
    pageEntity.setMoves(pageMoves);

    pageDAO.update(pageEntity);
  }

  /**
   * Recursively update wiki of children pages
   * 
   * @param wikiEntity The new wiki
   * @param pageEntity The page to update
   */
  private void updateWikiOfPageTree(WikiEntity wikiEntity, PageEntity pageEntity) {
    pageEntity.setWiki(wikiEntity);

    List<PageEntity> childrenPages = pageDAO.getChildrenPages(pageEntity);
    if (childrenPages != null) {
      for (PageEntity childrenPageEntity : childrenPages) {
        updateWikiOfPageTree(wikiEntity, childrenPageEntity);
      }
    }
  }

  public List<Page> getRelatedPagesOfPage(Page page) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot get related pages of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    List<Page> relatedPages = new ArrayList<>();
    List<PageEntity> relatedPagesEntities = pageEntity.getRelatedPages();
    if (relatedPagesEntities != null) {
      for (PageEntity relatedPageEntity : relatedPagesEntities) {
        relatedPages.add(convertPageEntityToPage(relatedPageEntity));
      }
    }

    return relatedPages;
  }

  public Page getRelatedPage(String wikiType, String wikiOwner, String pageName) throws WikiException {
    Page relatedPage = null;
    List<PageMoveEntity> pageMoveEntities = pageMoveDAO.findInPageMoves(wikiType, wikiOwner, pageName);
    if (pageMoveEntities != null && !pageMoveEntities.isEmpty()) {
      // take first result
      relatedPage = convertPageEntityToPage(pageMoveEntities.get(0).getPage());
    }
    return relatedPage;
  }

  public void addRelatedPage(Page page, Page relatedPage) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot add related page to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    PageEntity relatedPageEntity = pageDAO.getPageOfWikiByName(relatedPage.getWikiType(),
                                                               relatedPage.getWikiOwner(),
                                                               relatedPage.getName());

    if (relatedPageEntity == null) {
      throw new WikiException("Cannot add related page " + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":" +
          relatedPage.getName() + " of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName() +
          " because related page does not exist.");
    }

    List<PageEntity> relatedPages = pageEntity.getRelatedPages();
    if (relatedPages == null) {
      relatedPages = new ArrayList<>();
    }
    relatedPages.add(relatedPageEntity);
    pageEntity.setRelatedPages(relatedPages);

    pageDAO.update(pageEntity);
  }

  public List<Page> getPagesOfWiki(String wikiType, String wikiOwner) {
    if (StringUtils.isBlank(wikiOwner)) {
      throw new IllegalArgumentException("wikiOwner is mandatory argument");
    }
    if (StringUtils.isBlank(wikiType)) {
      throw new IllegalArgumentException("wikiType is mandatory argument");
    }
    List<PageEntity> pagesOfWiki = pageDAO.getPagesOfWiki(wikiType, wikiOwner, false);
    List<Page> pages = new ArrayList<>();
    for (PageEntity pageEntity : pagesOfWiki) {
      pages.add(convertPageEntityToPage(pageEntity));
    }
    return pages;
  }

  public void removeRelatedPage(Page page, Page relatedPage) throws WikiException {
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());

    if (pageEntity == null) {
      throw new WikiException("Cannot remove related page to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    PageEntity relatedPageEntity = pageDAO.getPageOfWikiByName(relatedPage.getWikiType(),
                                                               relatedPage.getWikiOwner(),
                                                               relatedPage.getName());

    if (relatedPageEntity == null) {
      throw new WikiException("Cannot remove related page " + relatedPage.getWikiType() + ":" + relatedPage.getWikiOwner() + ":" +
          relatedPage.getName() + " of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName() +
          " because related page does not exist.");
    }

    List<PageEntity> relatedPages = pageEntity.getRelatedPages();
    if (relatedPages != null) {
      for (int i = 0; i < relatedPages.size(); i++) {
        if (relatedPages.get(i).getId() == relatedPageEntity.getId()) {
          relatedPages.remove(i);
          break;
        }
      }
      pageEntity.setRelatedPages(relatedPages);
      pageDAO.update(pageEntity);
    }
  }

  public Page getExsitedOrNewDraftPageById(String wikiType,
                                           String wikiOwner,
                                           String pageName,
                                           String username) throws WikiException {

    if (pageName.contains(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE)) {
      String[] pageNameParts = pageName.split(Utils.SPLIT_TEXT_OF_DRAFT_FOR_NEW_PAGE);
      username = pageNameParts[0];
    }
    DraftPageEntity draftPageEntity = draftPageDAO.findDraftPageByName(pageName);
    DraftPage draftPage = convertDraftPageEntityToDraftPage(draftPageEntity);

    if (draftPage == null) {
      Date now = GregorianCalendar.getInstance().getTime();
      // create draft page for non existing draft page
      draftPage = new DraftPage();
      draftPage.setWikiType(PortalConfig.USER_TYPE);
      draftPage.setWikiOwner(username);
      draftPage.setName(pageName);
      draftPage.setAuthor(username);
      draftPage.setNewPage(true);
      draftPage.setCreatedDate(now);
      draftPage.setUpdatedDate(now);

      if (wikiType != null && wikiOwner != null) {
        Page targetPage = getPageOfWikiByName(wikiType, wikiOwner, pageName);
        if (targetPage != null) {
          draftPage.setTargetPageId(targetPage.getId());
          draftPage.setTargetPageRevision("1");
        }
      }

      createDraftPageForUser(draftPage, username);
    }

    return draftPage;
  }

  public DraftPage getDraftOfPageByLang(Page page, String lang) throws WikiException {
    List<DraftPageEntity> draftPages = draftPageDAO.findDraftPagesByTargetPage(Long.parseLong(page.getId()));
    for (DraftPageEntity draftPage : draftPages) {
      if (draftPage != null && StringUtils.equals(draftPage.getLang(), lang)) {
        return convertDraftPageEntityToDraftPage(draftPage);
      }
    }
    return null;
  }

  public List<DraftPage> getDraftsOfPage(Long pageId) {
    return convertDraftPageEntitiesToDraftPages(draftPageDAO.findDraftPagesByTargetPage(pageId));
  }

  public DraftPage getDraft(WikiPageParams wikiPageParams) throws WikiException {
    DraftPage latestDraft = null;

    Page page = getPageOfWikiByName(wikiPageParams.getType(), wikiPageParams.getOwner(), wikiPageParams.getPageName());

    if (page != null) {
      List<DraftPageEntity> draftPages = draftPageDAO.findDraftPagesByTargetPage(Long.valueOf(page.getId()));

      DraftPageEntity latestDraftEntity = null;
      for (DraftPageEntity draft : draftPages) {
        // Compare and get the latest draft
        if ((latestDraftEntity == null) || (latestDraftEntity.getUpdatedDate().getTime() < draft.getUpdatedDate().getTime())) {
          latestDraftEntity = draft;
        }
      }
      latestDraft = convertDraftPageEntityToDraftPage(latestDraftEntity);
    } else {
      throw new WikiException("Cannot get draft of page " + wikiPageParams.getType() + ":" + wikiPageParams.getOwner() + ":" +
          wikiPageParams.getPageName() + " because page does not exist.");
    }

    return latestDraft;
  }

  public DraftPage getLatestDraftOfPage(Page targetPage) {
    DraftPageEntity draftPagEntity = draftPageDAO.findLatestDraftPageByTargetPage(Long.parseLong(targetPage.getId()));
    return convertDraftPageEntityToDraftPage(draftPagEntity);
  }

  public DraftPage createDraftPageForUser(DraftPage draftPage, String username) throws WikiException {
    DraftPageEntity draftPageEntity = convertDraftPageToDraftPageEntity(draftPage, pageDAO);
    if (username != null) {
      draftPageEntity.setAuthor(username);
    }
    draftPage = convertDraftPageEntityToDraftPage(draftPageDAO.create(draftPageEntity));
    return draftPage;
  }

  public DraftPage updateDraftPageForUser(DraftPage draftPage, String username) throws WikiException {
    DraftPageEntity draftPageEntity = convertDraftPageToDraftPageEntity(draftPage, pageDAO);
    draftPageEntity.setAuthor(username);
    draftPage = convertDraftPageEntityToDraftPage(draftPageDAO.update(draftPageEntity));
    return draftPage;
  }

  public List<TemplateSearchResult> searchTemplate(TemplateSearchData templateSearchData) throws WikiException {

    String wikiOwner = templateSearchData.getWikiOwner();
    if (templateSearchData.getWikiType().toUpperCase().equals(WikiType.GROUP.toString())) {
      wikiOwner = templateDAO.validateGroupWikiOwner(wikiOwner);
    }

    List<TemplateEntity> templates = templateDAO.searchTemplatesByTitle(templateSearchData.getWikiType(),
                                                                        wikiOwner,
                                                                        templateSearchData.getTitle());

    List<TemplateSearchResult> searchResults = new ArrayList<>();
    if (templates != null) {
      for (TemplateEntity templateEntity : templates) {
        Calendar createdDateCalendar = null;
        Date createdDate = templateEntity.getCreatedDate();
        if (createdDate != null) {
          createdDateCalendar = Calendar.getInstance();
          createdDateCalendar.setTime(createdDate);
        }
        Calendar updatedDateCalendar = null;
        Date updatedDate = templateEntity.getUpdatedDate();
        if (updatedDate != null) {
          updatedDateCalendar = Calendar.getInstance();
          updatedDateCalendar.setTime(updatedDate);
        }
        TemplateSearchResult templateSearchResult = new TemplateSearchResult(templateEntity.getWiki().getType(),
                                                                             templateEntity.getWiki().getOwner(),
                                                                             templateEntity.getName(),
                                                                             templateEntity.getTitle(),
                                                                             SearchResultType.TEMPLATE,
                                                                             updatedDateCalendar,
                                                                             createdDateCalendar,
                                                                             null);
        searchResults.add(templateSearchResult);
      }
    }

    return searchResults;
  }

  public List<Attachment> getAttachmentsOfPage(Page page) throws WikiException {
    return getAttachmentsOfPage(page, false);
  }

  public List<Attachment> getAttachmentsOfPage(Page page, boolean loadContent) throws WikiException {
    List<AttachmentEntity> attachmentsEntities;
    String wikiType;
    String wikiOwner;
    String pageName;
    if (page.isDraftPage()) {
      DraftPageEntity draftPageEntity = draftPageDAO.findDraftPageByName(page.getName());
      if (draftPageEntity == null) {
        throw new WikiException("Cannot get attachments of draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because draft page does not exist.");
      }
      attachmentsEntities = new ArrayList<>();
      List<DraftPageAttachmentEntity> draftPageAttachmentEntities = draftPageEntity.getAttachments();
      if (draftPageAttachmentEntities != null)
        attachmentsEntities.addAll(draftPageAttachmentEntities);
      if (draftPageEntity.isNewPage()) {
        wikiType = WIKI_TYPE_DRAFT;
        wikiOwner = draftPageEntity.getAuthor();
        pageName = draftPageEntity.getName();
      } else {
        PageEntity targetPage = draftPageEntity.getTargetPage();
        WikiEntity wiki = targetPage.getWiki();
        wikiType = wiki.getType();
        wikiOwner = wiki.getOwner();
        pageName = targetPage.getName();
      }
    } else {
      PageEntity pageEntity = fetchPageEntity(page);
      if (pageEntity == null) {
        throw new WikiException("Cannot get attachments of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because page does not exist.");
      }
      attachmentsEntities = new ArrayList<>();
      List<PageAttachmentEntity> pageAttachmentEntities = pageEntity.getAttachments();
      if (pageAttachmentEntities != null)
        attachmentsEntities.addAll(pageAttachmentEntities);
      WikiEntity wikiEntity = pageEntity.getWiki();
      wikiType = wikiEntity.getType();
      wikiOwner = wikiEntity.getOwner();
      pageName = pageEntity.getName();
    }

    List<Attachment> attachments = new ArrayList<>();
    if (attachmentsEntities != null) {
      for (AttachmentEntity attachmentEntity : attachmentsEntities) {
        Attachment attachment = convertAttachmentEntityToAttachment(fileService, attachmentEntity, loadContent);
        // set title and full title if not there
        if (attachment.getTitle() == null || StringUtils.isEmpty(attachment.getTitle())) {
          int index = attachment.getName().lastIndexOf(".");
          if (index != -1) {
            attachment.setTitle(attachment.getName().substring(0, index));
          } else {
            attachment.setTitle(attachment.getName());
          }
        }
        if (attachment.getFullTitle() == null || StringUtils.isEmpty(attachment.getFullTitle())) {
          attachment.setFullTitle(attachment.getName());
        }
        // build download url
        attachment.setDownloadURL(getDownloadURL(wikiType, wikiOwner, pageName, attachment));
        attachments.add(attachment);
      }
    }

    return attachments;
  }

  @ExoTransactional
  public void addAttachmentToPage(Attachment attachment, Page page) throws WikiException {

    if (page.isDraftPage()) {

      DraftPageAttachmentEntity attachmentEntity = convertAttachmentToDraftPageAttachmentEntity(fileService, attachment);
      Date now = GregorianCalendar.getInstance().getTime();
      if (attachmentEntity.getCreatedDate() == null) {
        attachmentEntity.setCreatedDate(now);
      }

      DraftPageEntity draftPageEntity = draftPageDAO.findDraftPageByName(page.getName());

      if (draftPageEntity == null) {
        throw new WikiException("Cannot add an attachment to draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because draft page does not exist.");
      }

      attachmentEntity.setDraftPage(draftPageEntity);

      // attachment must be saved here because of Hibernate bug HHH-6776
      draftPageAttachmentDAO.create(attachmentEntity);

      List<DraftPageAttachmentEntity> attachmentsEntities = draftPageEntity.getAttachments();
      if (attachmentsEntities == null) {
        attachmentsEntities = new ArrayList<>();
      }
      DraftPageAttachmentEntity draftPageAttachmentEntity = attachmentEntity;
      draftPageAttachmentEntity.setDraftPage(draftPageEntity);
      attachmentsEntities.add(draftPageAttachmentEntity);
      draftPageEntity.setAttachments(attachmentsEntities);
      draftPageDAO.update(draftPageEntity);
    } else {

      PageAttachmentEntity attachmentEntity = convertAttachmentToPageAttachmentEntity(fileService, attachment);
      Date now = GregorianCalendar.getInstance().getTime();
      if (attachmentEntity.getCreatedDate() == null) {
        attachmentEntity.setCreatedDate(now);
      }

      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot add an attachment to page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because page does not exist.");
      }
      attachmentEntity.setPage(pageEntity);

      // attachment must be saved here because of Hibernate bug HHH-6776
      pageAttachmentDAO.create(attachmentEntity);

      List<PageAttachmentEntity> attachmentsEntities = pageEntity.getAttachments();
      if (attachmentsEntities == null) {
        attachmentsEntities = new ArrayList<>();
      }

      PageAttachmentEntity pageAttachmentEntity = attachmentEntity;
      pageAttachmentEntity.setPage(pageEntity);
      attachmentsEntities.add(pageAttachmentEntity);
      pageEntity.setAttachments(attachmentsEntities);
      pageDAO.update(pageEntity);
    }
  }

  @ExoTransactional
  public void deleteAttachmentOfPage(String attachmentName, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);
    DraftPageEntity draftPageEntity = null;
    if (pageEntity == null) {
      draftPageEntity = draftPageDAO.findDraftPageByName(page.getName());
    }

    if (pageEntity == null && draftPageEntity == null) {
      throw new WikiException("Cannot delete an attachment of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    boolean attachmentFound = false;
    if (pageEntity == null && draftPageEntity != null) {
      attachmentFound = deleteDraftPageAttachementEntity(attachmentName, draftPageEntity, attachmentFound);
    } else {
      attachmentFound = deletePageAttachementEntity(attachmentName, pageEntity, attachmentFound);
    }

    if (!attachmentFound) {
      throw new WikiException("Cannot delete the attachment " + attachmentName + " of page " + page.getWikiType() + ":" +
          page.getWikiOwner() + ":" + page.getName() + " because attachment does not exist.");
    }
  }

  private boolean deletePageAttachementEntity(String attachmentName, PageEntity pageEntity, boolean attachmentFound) {
    List<PageAttachmentEntity> attachmentsEntities = pageEntity.getAttachments();
    if (attachmentsEntities != null) {
      for (int i = 0; i < attachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = attachmentsEntities.get(i);
        String name = null;
        if (attachmentEntity.getAttachmentFileID() != null) {
          name = fileService.getFileInfo(attachmentEntity.getAttachmentFileID()).getName();
        }
        if (name != null && name.equals(attachmentName)) {
          attachmentFound = true;
          attachmentsEntities.remove(i);

          fileService.deleteFile(attachmentEntity.getAttachmentFileID());
          pageAttachmentDAO.delete((PageAttachmentEntity) attachmentEntity);

          pageEntity.setAttachments(attachmentsEntities);
          pageDAO.update(pageEntity);
          break;
        }
      }
    }
    return attachmentFound;
  }

  private boolean deleteDraftPageAttachementEntity(String attachmentName,
                                                   DraftPageEntity draftPageEntity,
                                                   boolean attachmentFound) {
    List<DraftPageAttachmentEntity> draftAttachmentsEntities = draftPageEntity.getAttachments();
    if (draftAttachmentsEntities != null) {
      for (int i = 0; i < draftAttachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = draftAttachmentsEntities.get(i);
        String name = null;
        if (attachmentEntity.getAttachmentFileID() != null) {
          name = fileService.getFileInfo(attachmentEntity.getAttachmentFileID()).getName();
        }
        if (name != null && name.equals(attachmentName)) {
          attachmentFound = true;
          draftAttachmentsEntities.remove(i);

          fileService.deleteFile(attachmentEntity.getAttachmentFileID());
          draftPageAttachmentDAO.delete((DraftPageAttachmentEntity) attachmentEntity);

          draftPageEntity.setAttachments(draftAttachmentsEntities);
          draftPageDAO.update(draftPageEntity);
          break;
        }
      }
    }
    return attachmentFound;
  }

  @Deprecated
  public Page getHelpSyntaxPage(String syntaxId,
                                boolean fullContent,
                                List<ValuesParam> syntaxHelpParams,
                                ConfigurationManager configurationManager) throws WikiException {
    return null;
  }

  public List<PageVersion> getVersionsOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get versions of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    List<PageVersion> pageVersions = new ArrayList<>();
    List<PageVersionEntity> pageVersionEntities = pageEntity.getVersions();
    if (pageVersionEntities != null) {
      for (PageVersionEntity pageVersionEntity : pageVersionEntities) {
        pageVersions.add(convertPageVersionEntityToPageVersion(pageVersionEntity));
      }
    }

    Collections.sort(pageVersions, new VersionNameComparatorDesc());

    return pageVersions;
  }

  public List<PageHistory> getHistoryOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get versions of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    List<PageHistory> pageVersionsHistory = new ArrayList<>();
    List<PageVersionEntity> pageVersionEntities = pageEntity.getVersions();
    if (pageVersionEntities != null) {
      for (PageVersionEntity pageVersionEntity : pageVersionEntities) {
        PageHistory pageHistory = convertPageVersionEntityToPageHistory(pageVersionEntity);
        pageVersionsHistory.add(pageHistory);
      }
    }
    return pageVersionsHistory;
  }

  @ExoTransactional
  public PageVersion addPageVersion(Page page, String userName) throws WikiException {
    if (page != null) {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot add version of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because page does not exist.");
      }

      PageVersionEntity pageVersionEntity = new PageVersionEntity();
      Long versionNumber = pageVersionDAO.getLastversionNumberOfPage(pageEntity.getId());
      if (versionNumber == null) {
        versionNumber = 1L;
      } else {
        versionNumber = versionNumber + 1;
      }

      pageVersionEntity.setPage(pageEntity);
      pageVersionEntity.setVersionNumber(versionNumber);
      pageVersionEntity.setName(page.getName());
      pageVersionEntity.setTitle(page.getTitle());
      if (StringUtils.isNotEmpty(userName)) {
        pageVersionEntity.setAuthor(userName);
      } else {
        pageVersionEntity.setAuthor(pageEntity.getAuthor());
      }
      pageVersionEntity.setContent(page.getContent());
      pageVersionEntity.setSyntax(pageEntity.getSyntax());
      pageVersionEntity.setMinorEdit(pageEntity.isMinorEdit());
      pageVersionEntity.setComment(pageEntity.getComment());
      Date now = Calendar.getInstance().getTime();
      pageVersionEntity.setCreatedDate(now);
      pageVersionEntity.setUpdatedDate(now);
      pageVersionEntity.setLang(page.getLang());

      // attachment must be saved here because of Hibernate bug HHH-6776
      pageVersionDAO.create(pageVersionEntity);

      List<PageVersionEntity> pageVersionEntities = pageEntity.getVersions();
      if (pageVersionEntities == null) {
        pageVersionEntities = new ArrayList<>();
      }
      pageVersionEntities.add(pageVersionEntity);
      pageEntity.setVersions(pageVersionEntities);

      pageDAO.update(pageEntity);

      return EntityConverter.convertPageVersionEntityToPageVersion(pageVersionEntity);
    } else {
      throw new WikiException("Cannot create version of a page null");
    }
  }

  public PageVersion restoreVersionOfPage(String versionName, Page page) throws WikiException {
    if (page != null) {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot restore version of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because page does not exist.");
      }

      PageVersionEntity versionToRestore = pageVersionDAO.getPageversionByPageIdAndVersion(Long.parseLong(page.getId()),
                                                                                           Long.parseLong(versionName));
      if (versionToRestore != null) {
        pageEntity.setContent(versionToRestore.getContent());
        pageEntity.setUpdatedDate(Calendar.getInstance().getTime());
        pageDAO.update(pageEntity);
        return EntityConverter.convertPageVersionEntityToPageVersion(versionToRestore);
      } else {
        throw new WikiException("Cannot restore version " + versionName + " of a page " + page.getWikiType() + ":" +
            page.getWikiOwner() + ":" + page.getName() + " because version does not exist.");
      }
    } else {
      throw new WikiException("Cannot restore version of a page null");
    }
  }

  @ExoTransactional
  public Page updatePage(Page page) throws WikiException {
    if (page.isDraftPage()) {
      DraftPageEntity draftPageEntity = draftPageDAO.findDraftPageByName(page.getName());

      if (draftPageEntity == null) {
        throw new WikiException("Cannot add an attachment to draft page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
            page.getName() + " because draft page does not exist.");
      }

      draftPageEntity.setTitle(page.getTitle());
      draftPageEntity.setContent(page.getContent());
      draftPageEntity.setUpdatedDate(page.getUpdatedDate());

      return convertDraftPageEntityToDraftPage(draftPageDAO.update(draftPageEntity));
    } else {
      PageEntity pageEntity = fetchPageEntity(page);

      if (pageEntity == null) {
        throw new WikiException("Cannot update page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" + page.getName() +
            " because page does not exist.");
      }

      pageEntity.setName(page.getName());
      pageEntity.setTitle(page.getTitle());
      pageEntity.setAuthor(page.getAuthor());
      pageEntity.setContent(page.getContent());
      pageEntity.setSyntax(page.getSyntax());
      pageEntity.setCreatedDate(page.getCreatedDate());
      pageEntity.setUpdatedDate(page.getUpdatedDate());
      pageEntity.setMinorEdit(page.isMinorEdit());
      pageEntity.setComment(page.getComment());
      pageEntity.setUrl(page.getUrl());
      pageEntity.setActivityId(page.getActivityId());

      return convertPageEntityToPage(pageDAO.update(pageEntity));
    }
  }

  public List<String> getPreviousNamesOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get previous names of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    List<String> previousPageName = new ArrayList<>();
    List<PageMoveEntity> moves = pageEntity.getMoves();
    if (moves != null) {
      for (PageMoveEntity pageMoveEntity : moves) {
        previousPageName.add(pageMoveEntity.getPageName());
      }
    }

    return previousPageName;
  }

  public List<String> getWatchersOfPage(Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot get watchers of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }
    return pageEntity.getWatchers() == null ? null : new ArrayList<>(pageEntity.getWatchers());
  }

  public void addWatcherToPage(String username, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot add a watcher on page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }
    if (pageEntity.getWatchers() == null) {
      throw new WikiException("Cannot add a watcher on page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because list of watchers is null.");
    }
    pageEntity.getWatchers().add(username);
    pageDAO.update(pageEntity);
  }

  public void deleteWatcherOfPage(String username, Page page) throws WikiException {
    PageEntity pageEntity = fetchPageEntity(page);

    if (pageEntity == null) {
      throw new WikiException("Cannot delete a watcher of page " + page.getWikiType() + ":" + page.getWikiOwner() + ":" +
          page.getName() + " because page does not exist.");
    }

    Set<String> watchers = pageEntity.getWatchers();
    if (watchers != null && watchers.contains(username)) {
      watchers.remove(username);
      pageEntity.setWatchers(watchers);
      pageDAO.update(pageEntity);
    } else {
      throw new WikiException("Cannot remove watcher " + username + " of page " + page.getWikiType() + ":" + page.getWikiOwner() +
          ":" + page.getName() + " because watcher does not exist.");
    }
  }

  @ExoTransactional
  public void deleteAttachmentsOfDraftPage(DraftPageEntity page) {
    List<DraftPageAttachmentEntity> attachmentsEntities = page.getAttachments();
    if (attachmentsEntities != null) {
      for (int i = 0; i < attachmentsEntities.size(); i++) {
        AttachmentEntity attachmentEntity = attachmentsEntities.get(i);
        attachmentsEntities.remove(i);
        fileService.deleteFile(attachmentEntity.getAttachmentFileID());
        draftPageAttachmentDAO.delete((DraftPageAttachmentEntity) attachmentEntity);
      }
      page.setAttachments(attachmentsEntities);
      draftPageDAO.update(page);
    }
  }

  public PageEntity fetchPageEntity(Page page) {
    PageEntity pageEntity;
    Long pageId = null;
    if (page.getId() != null && !page.getId().isEmpty()) {
      try {
        pageId = Long.parseLong(page.getId());
      } catch (NumberFormatException e) {
        pageId = null;
      }
    }
    if (pageId != null) {
      pageEntity = pageDAO.find(Long.parseLong(page.getId()));
    } else {
      pageEntity = pageDAO.getPageOfWikiByName(page.getWikiType(), page.getWikiOwner(), page.getName());
    }
    return pageEntity;
  }

  private String getDownloadURL(String wikiType, String wikiOwner, String pageName, Attachment attachment) {
    StringBuilder sb = new StringBuilder();

    sb.append(Utils.getDefaultRestBaseURI())
      .append("/wiki/attachments/")
      .append(wikiType)
      .append("/")
      .append(Utils.SPACE)
      .append("/")
      .append(wikiOwner)
      .append("/")
      .append(Utils.PAGE)
      .append("/")
      .append(pageName);
    try {
      sb.append("/").append(URLEncoder.encode(attachment.getName(), "UTF-8"));
    } catch (UnsupportedEncodingException e) {
      sb.append("/").append(attachment.getName());
    }

    return sb.toString();
  }

  public List<PageHistory> getPageHistoryVersionsByPageIdAndLang(Long pageId, String lang) {
    if (pageId == null) {
      throw new IllegalArgumentException("pageId argument is null");
    }
    return EntityConverter.toPageHistoryVersions(pageVersionDAO.findPageVersionsByPageIdAndLang(pageId, lang));
  }

  public DraftPage getLatestDraftPageByTargetPageAndLang(Long targetPageId, String lang) {
    if (targetPageId == null) {
      throw new IllegalArgumentException("targetPageId argument is null");
    }
    return EntityConverter.convertDraftPageEntityToDraftPage(draftPageDAO.findLatestDraftPageByTargetPageAndLang(targetPageId,
                                                                                                                 lang));
  }

  public PageVersion getPublishedVersionByPageIdAndLang(Long pageId, String lang) {
    if (pageId == null) {
      throw new IllegalArgumentException("targetPageId argument is null");
    }
    PageVersion pageVersion =
                            convertPageVersionEntityToPageVersion(pageVersionDAO.findLatestVersionByPageIdAndLang(pageId, lang));
    if (pageVersion != null) {
      Page page = pageVersion.getParent();
      page.setLang(lang);
      EntityConverter.buildNotePageMetadata(page, false);
      pageVersion.setProperties(page.getProperties());
      return pageVersion;
    }
    return null;
  }

  public List<String> getPageAvailableTranslationLanguages(Long pageId) {
    if (pageId == null) {
      throw new IllegalArgumentException("pageId argument is null");
    }
    return pageVersionDAO.findPageAvailableTranslationLanguages(pageId);
  }

  @ExoTransactional
  public void deleteVersionsByNoteIdAndLang(Long noteId, String lang) throws WikiException {
    if (noteId == null) {
      throw new IllegalArgumentException("noteId argument is null");
    }
    PageEntity pageEntity = pageDAO.find(noteId);

    if (pageEntity == null) {
      throw new WikiException("Cannot delete versions of page with: " + noteId + "for language:" + lang +
          " because page does not exist.");
    }
    List<PageVersionEntity> history = pageVersionDAO.findPageVersionsByPageIdAndLang(noteId, lang);
    pageVersionDAO.deleteAll(history);
    history = pageEntity.getVersions();
    history.removeIf(version -> (StringUtils.isNotEmpty(version.getLang()) && version.getLang().equals(lang)));
    pageEntity.setVersions(history);
    pageDAO.update(pageEntity);
  }

  public void deleteOrphanDraftPagesByParentPage(long parentPageId) {
    draftPageDAO.deleteOrphanDraftPagesByParentPage(parentPageId);
  }

  public PageVersion getPageVersionById(long versionId) {
    return EntityConverter.convertPageVersionEntityToPageVersion(pageVersionDAO.find(versionId));
  }

  public Page updatePageContent(Page page, String content) {
    PageEntity pageTobeUpdated = fetchPageEntity(page);
    pageTobeUpdated.setContent(content);
    pageTobeUpdated.setUpdatedDate(new Date(System.currentTimeMillis()));
    return convertPageEntityToPage(pageDAO.update(pageTobeUpdated));
  }

  public DraftPage updateDraftContent(long draftId, String content) {
    DraftPageEntity draftPageEntity = draftPageDAO.find(draftId);
    draftPageEntity.setContent(content);
    draftPageEntity.setUpdatedDate(new Date(System.currentTimeMillis()));
    return EntityConverter.convertDraftPageEntityToDraftPage(draftPageDAO.update(draftPageEntity));
  }

  public List<DraftPage> getDraftsOfWiki(String wikiOwner, String wikiType, String wikiHome) {
    // The Note API allows multiple home pages to be created within the wiki.
    // To avoid retrieving drafts from all home pages,
    // we need to specifically fetch all pages under the target home page and
    // search
    // for drafts.
    // Although this approach may seem performance-intensive, it is the only
    // reliable solution.
    PageEntity pageEntity = pageDAO.getPageOfWikiByName(wikiType, wikiOwner, wikiHome);
    List<DraftPageEntity> draftPageEntities = new ArrayList<>();
    getDraftsOfPage(pageEntity, draftPageEntities);
    return convertDraftPageEntitiesToDraftPages(draftPageEntities);
  }

  private void getDraftsOfPage(PageEntity pageEntity, List<DraftPageEntity> drafts) {
    drafts.addAll(draftPageDAO.findDraftPagesByParentPage(pageEntity.getId()));
    List<PageEntity> childrenPages = pageDAO.getChildrenPages(pageEntity);
    for (PageEntity child : childrenPages) {
      getDraftsOfPage(child, drafts);
    }
  }

}
