/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */
package io.meeds.notes.service;

import java.util.Date;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

public class TermsAndConditionsService {

  private static final String NOTE_TYPE = "terms";

  private static final String NOTE_NAME = "termsAndConditions";

  private final NoteService   noteService;

  private final WikiService   noteBookService;

  private UserACL             userACL;

  public TermsAndConditionsService(NoteService noteService, WikiService noteBookService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
  }

  public Page saveTermsAndConditions(String content, String lang, Identity currentUserAclIdentity) throws IllegalAccessException {
    String username = currentUserAclIdentity.getUserId();
    if (userACL.isAdministrator(currentUserAclIdentity)) {
      throw new IllegalAccessException("User doesn't have enough privileges to update terms and conditions page");
    }
    return saveTermsAndConditions(content, lang, username);
  }

  private Page saveTermsAndConditions(String content, String lang, String username) {
    try {
      Wiki noteBook = getNote();
      Page page = getTermsAndConditions(lang);
      if (page == null) {
        page = new Page(NOTE_NAME, "");
        page.setContent(content);
        page.setCreatedDate(new Date());
        page.setUpdatedDate(new Date());
        page.setOwner(IdentityConstants.SYSTEM);
        page.setLang(lang);
        page.setToBePublished(true);
        page = noteService.createNote(noteBook, noteBook.getWikiHome(), page);
        noteService.createVersionOfNote(page, username);
      }
      return getTermsAndConditions(lang);
    } catch (WikiException e) {
      throw new IllegalStateException("Error retrieving terms and conditions note", e);
    }
  }

  private Wiki getNote() throws WikiException {
    Wiki noteBook = noteBookService.getWikiByTypeAndOwner(NOTE_TYPE, IdentityConstants.SYSTEM);
    if (noteBook == null) {
      return noteBookService.createWiki(NOTE_TYPE, IdentityConstants.SYSTEM);
    } else {
      return noteBook;
    }
  }

  public Page getTermsAndConditions(String lang) {
    try {
      Page page = noteService.getNoteOfNoteBookByName(NOTE_TYPE, IdentityConstants.SYSTEM, NOTE_NAME);
      if (page != null && StringUtils.isNotBlank(lang) && !StringUtils.equals(lang, page.getLang())) {
        Page publishedVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()), lang);
        if (publishedVersion != null) {
          page.setTitle(publishedVersion.getTitle());
          page.setContent(publishedVersion.getContent());
          page.setLang(publishedVersion.getLang());
        }
      }
      return page;
    } catch (WikiException e) {
      throw new IllegalStateException("Error retrieving terms and conditions note", e);
    }
  }
}
