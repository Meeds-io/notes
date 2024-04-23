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

package org.exoplatform.wiki.jpa.organization;

import org.exoplatform.commons.search.index.IndexingService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupEventListener;
import org.exoplatform.wiki.jpa.EntityConverter;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.TemplateDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.TemplateEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.jpa.search.WikiPageIndexingServiceConnector;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.NoteService;

import java.util.List;

/**
 * Created by The eXo Platform SAS
 * Author : Thibault Clement
 * tclement@exoplatform.com
 * 2/24/16
 */
public class WikiGroupEventListener extends GroupEventListener {

  private static final Log LOG = ExoLogger.getLogger(WikiGroupEventListener.class);

  private WikiDAO wikiDAO;
  private PageDAO pageDAO;
  private TemplateDAO templateDAO;
  private IndexingService indexingService;
  private NoteService noteService;

  public WikiGroupEventListener(WikiDAO wikiDAO, PageDAO pageDAO, TemplateDAO templateDAO, IndexingService indexingService,
                                NoteService noteService) {
    this.wikiDAO = wikiDAO;
    this.pageDAO = pageDAO;
    this.templateDAO = templateDAO;
    this.indexingService = indexingService;
    this.noteService = noteService;
  }

  @Override
  public void postDelete(Group group) throws Exception {

    LOG.info("Removing all wiki data of the group "+group.getId());


    //First remove and unindex all Wiki Pages (include wikiHome and deleted pages)
    List<PageEntity> pages = pageDAO.getAllPagesOfWiki(WikiType.GROUP.toString().toLowerCase(), group.getId());
    if (pages != null) {
      for (PageEntity page : pages) {
        indexingService.unindex(WikiPageIndexingServiceConnector.TYPE, String.valueOf(page.getId()));
        noteService.removeDraftOfNote(EntityConverter.convertPageEntityToPage(page));
        noteService.removeOrphanDraftPagesByParentPage(page.getId());
      }
      pageDAO.deleteAll(pages);
    }

    //Then remove template
    List<TemplateEntity> templates = templateDAO.getTemplatesOfWiki(WikiType.GROUP.toString().toLowerCase(), group.getId());
    if (templates != null) {
      templateDAO.deleteAll(templates);
    }

    //Finally remove the group wiki
    WikiEntity wikiGroup = wikiDAO.getWikiByTypeAndOwner(WikiType.GROUP.toString().toLowerCase(), group.getId());
    if (wikiGroup != null) {
      wikiDAO.delete(wikiGroup);
    }

  }

}

