package org.exoplatform.wiki.jpa.search;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.mow.api.DraftPage;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.listener.PageWikiListener;

/**
 * Listener on pages creation/update/deletion to index them
 */
public class PageIndexingListener extends PageWikiListener {

  private IndexingService indexingService;


  public PageIndexingListener(IndexingService indexingService) {
    this.indexingService = indexingService;
  }

  @Override
  public void postAddPage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.index(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postUpdatePage(String wikiType, String wikiOwner, String pageId, Page page, PageUpdateType wikiUpdateType) throws WikiException {
    if (!page.isDraftPage()) {
      indexingService.reindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
    }
  }

  @Override
  public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {
    indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, page.getId());
  }

  @Override
  public void postgetPagefromTree(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

  @Override
  public void postgetPagefromBreadCrumb(String wikiType, String wikiOwner, String pageId, Page page) throws WikiException {

  }

}
