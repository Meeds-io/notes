/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package io.meeds.notes.service;

import java.util.Date;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

public class NotePageViewService {

  public static final String CMS_CONTENT_TYPE = "notePage";

  private NoteService        noteService;

  private WikiService        noteBookService;

  private CMSService         cmsService;

  public NotePageViewService(NoteService noteService,
                             WikiService noteBookService,
                             CMSService cmsService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.cmsService = cmsService;
  }

  public Page getNotePage(String name, String lang, Identity currentUserAclIdentity) throws IllegalAccessException {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    if (setting == null) {
      return null;
    } else {
      if (!cmsService.hasAccessPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, name)) {
        throw new IllegalAccessException("Note page isn't accessible");
      }
      String pageReference = setting.getPageReference();
      PageKey pageKey = PageKey.create(pageReference);
      return getNotePage(pageKey, name);
    }
  }

  public void saveNotePage(String name,
                           String content,
                           String lang,
                           Identity currentUserAclIdentity) throws IllegalAccessException, ObjectNotFoundException {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    if (setting == null) {
      throw new ObjectNotFoundException(String.format("CMS Setting name %s wasn't found", name));
    } else {
      if (!cmsService.hasEditPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, name)) {
        throw new IllegalAccessException("Note page isn't editable");
      }
      String pageReference = setting.getPageReference();
      PageKey pageKey = PageKey.create(pageReference);
      try {
        Wiki noteBook = getNote(pageKey);
        Page page = getNotePage(pageKey, name);
        if (page == null) {
          page = new Page(name, name);
          page.setContent(content);
          page.setCreatedDate(new Date());
          page.setUpdatedDate(new Date());
          page.setOwner(IdentityConstants.SYSTEM);
          noteService.createNote(noteBook, noteBook.getWikiHome(), page);
        } else {
          page.setUpdatedDate(new Date());
          noteService.updateNote(page);
        }
      } catch (WikiException e) {
        throw new IllegalStateException(String.format("Error retrieving note with name %s referenced in page %s",
                                                      name,
                                                      pageKey),
                                        e);
      }
    }
  }

  private Wiki getNote(PageKey pageKey) throws WikiException {
    Wiki noteBook = noteBookService.getWikiByTypeAndOwner(pageKey.getType(), pageKey.getId());
    if (noteBook == null) {
      return noteBookService.createWiki(pageKey.getType(), pageKey.getId());
    } else {
      return noteBook;
    }
  }

  private Page getNotePage(PageKey pageKey, String name) {
    try {
      String ownerType = pageKey.getType();
      String ownerName = pageKey.getId();
      return noteService.getNoteOfNoteBookByName(ownerType, ownerName, name);
    } catch (WikiException e) {
      throw new IllegalStateException(String.format("Error retrieving note with name %s referenced in page %s",
                                                    name,
                                                    pageKey),
                                      e);
    }
  }

}
