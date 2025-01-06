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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;

import static org.exoplatform.commons.api.settings.data.Context.USER;

public class TermsAndConditionsService {

  public static final String                       ERROR_RETRIEVING_TERMS_AND_CONDITIONS_NOTE =
                                                                                              "Error retrieving terms and conditions note";

  private static final String                      NOTE_TYPE                                  = "terms";

  private static final String                      NOTE_NAME                                  = "termsAndConditions";

  public static final Scope                        SETTINGS_APP_SCOPE                         =
                                                                      Scope.APPLICATION.id("TERMS_AND_CONDITIONS");

  public static final String                       SETTINGS_KEY                               =
                                                                "TERMS_AND_CONDITIONS_ACCEPTED_VERSION";

  public static final String                       PUBLISHED                                  = "published";

  public static final String                       TERMS_AND_CONDITIONS_ADDED                 = "termsAndConditionsAdded";

  public static final String                       TERMS_AND_CONDITIONS_UPDATED               = "termsAndConditionsUpdated";

  public static final String                       LATEST_VERSION_ID                          = "latestVersionId";

  private final NoteService                        noteService;

  private final WikiService                        noteBookService;

  private final SettingService                     settingService;

  private final TermsAndConditionsWebSocketService termsAndConditionsWebSocketService;

  private final UserACL                            userACL;

  public TermsAndConditionsService(NoteService noteService,
                                   WikiService noteBookService,
                                   SettingService settingService,
                                   TermsAndConditionsWebSocketService termsAndConditionsWebSocketService,
                                   UserACL userACL) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.settingService = settingService;
    this.termsAndConditionsWebSocketService = termsAndConditionsWebSocketService;
    this.userACL = userACL;
  }

  public Page saveTermsAndConditions(String content, String lang, Identity currentUserAclIdentity) throws IllegalAccessException {
    String username = currentUserAclIdentity.getUserId();
    if (!userACL.isAdministrator(currentUserAclIdentity)) {
      throw new IllegalAccessException("User doesn't have enough privileges to create terms and conditions page");
    }
    return saveTermsAndConditions(content, lang, username);
  }

  public Page updateTermsAndConditionsSettings(Map<String, String> settings,
                                               String lang,
                                               Identity currentUserAclIdentity) throws IllegalAccessException {
    if (!userACL.isAdministrator(currentUserAclIdentity)) {
      throw new IllegalAccessException("User doesn't have enough privileges to update terms and conditions settings");
    }
    try {
      Page page = getTermsAndConditions(lang);
      String latestPublishedVersionId = MapUtils.isNotEmpty(page.getSettings()) ? page.getSettings().get(LATEST_VERSION_ID) : "";
      if (MapUtils.isNotEmpty(settings)) {
        page.setSettings(settings);
        page.setUpdatedDate(new Date());
        noteService.updateNote(page);
        if (settings.get(PUBLISHED) != null && settings.get(PUBLISHED).equals("true")
            && !Objects.equals(settings.get(LATEST_VERSION_ID), latestPublishedVersionId)) {
          termsAndConditionsWebSocketService.sendMessage(page.getLatestVersionId().equals("2") ? TERMS_AND_CONDITIONS_ADDED
                                                                                               : TERMS_AND_CONDITIONS_UPDATED);
        }
      }
      return getTermsAndConditions(lang);
    } catch (WikiException e) {
      throw new IllegalStateException(ERROR_RETRIEVING_TERMS_AND_CONDITIONS_NOTE, e);
    }
  }

  public Page getTermsAndConditions(String lang) {
    try {
      Page page = noteService.getNoteOfNoteBookByName(NOTE_TYPE, IdentityConstants.SYSTEM, NOTE_NAME);
      if (page != null && StringUtils.isNotBlank(lang) && !StringUtils.equals(lang, page.getLang())) {
        Page publishedVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()), lang);
        publishedVersion = publishedVersion != null ? publishedVersion
                                                    : noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()) , Locale.ENGLISH.getLanguage());
        if (publishedVersion != null) {
          page.setTitle(publishedVersion.getTitle());
          page.setContent(publishedVersion.getContent());
          page.setLang(publishedVersion.getLang());
          page.setLatestVersionId(publishedVersion.getId());
        }
      }
      return page;
    } catch (WikiException e) {
      throw new IllegalStateException(ERROR_RETRIEVING_TERMS_AND_CONDITIONS_NOTE, e);
    }
  }

  public void markTermsAsAcceptedForUser(String userId, String lang) {
    Page terms = getTermsAndConditions(lang);
    if (terms != null) {
      settingService.set(USER.id(userId), SETTINGS_APP_SCOPE, SETTINGS_KEY, SettingValue.create(terms.getLatestVersionId()));
    }
  }

  public boolean isTermsAcceptedForUser(String userId, String lang) {
    Page terms = getTermsAndConditions(lang);
    if (terms != null && MapUtils.isNotEmpty(terms.getSettings()) && terms.getSettings().get(PUBLISHED) != null
        && terms.getSettings().get(PUBLISHED).equals("true")) {
      SettingValue<?> acceptedVersion = settingService.get(USER.id(userId), SETTINGS_APP_SCOPE, SETTINGS_KEY);
      String acceptedVersionValue = acceptedVersion == null
          || acceptedVersion.getValue() == null ? null : acceptedVersion.getValue().toString();

      return acceptedVersionValue != null && acceptedVersionValue.equals(terms.getLatestVersionId());
    }
    return true;
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
        page.setAuthor(username);
        page.setOwner(IdentityConstants.SYSTEM);
        page.setLang(lang);
        page.setToBePublished(true);
        page = noteService.createNote(noteBook, noteBook.getWikiHome(), page);
        noteService.createVersionOfNote(page, username);
      }
      return getTermsAndConditions(lang);
    } catch (WikiException e) {
      throw new IllegalStateException(ERROR_RETRIEVING_TERMS_AND_CONDITIONS_NOTE, e);
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
}
