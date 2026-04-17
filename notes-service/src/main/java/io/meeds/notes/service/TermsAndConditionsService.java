/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import java.util.*;

import io.meeds.notes.model.TermsAndConditionPage;
import lombok.SneakyThrows;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.api.settings.SettingService;
import org.exoplatform.commons.api.settings.SettingValue;
import org.exoplatform.commons.api.settings.data.Scope;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.listener.ListenerService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataObject;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.wiki.model.*;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import static org.exoplatform.commons.api.settings.data.Context.USER;

@Service
public class TermsAndConditionsService {

  public static final String       TC_NOTE_TYPE            = "terms";

  public static final String       TC_NOTE_NAME            = "termsAndConditions";

  public static final Scope        SETTINGS_APP_SCOPE      = Scope.APPLICATION.id("TERMS_AND_CONDITIONS");

  public static final String       SETTINGS_KEY            = "TERMS_AND_CONDITIONS_ACCEPTED_VERSION";

  public static final String       PUBLISHED               = "published";

  public static final String       PUBLISHED_DATE          = "publishedDate";

  public static final String       PUBLISHED_VERSION_ID    = "latestVersionId";

  public static final String       EVENT_NAME_ADDED        = "terms.and.conditions.added";

  public static final String       EVENT_NAME_UPDATED      = "terms.and.conditions.updated";

  public static final String       EVENT_NAME_ACCEPTED     = "terms.and.conditions.accepted";

  public static final String       LATEST_VERSION_ID       = "latestVersionId";

  public static final String       TC_METADATA_OBJECT_TYPE = "termsAndConditions";

  public static final MetadataType TC_METADATA_TYPE        = new MetadataType(1002, TC_NOTE_NAME);

  public static final MetadataKey  TC_METADATA_KEY         = new MetadataKey(TC_METADATA_TYPE.getName(), TC_NOTE_NAME, 0);

  @Autowired
  private NoteService              noteService;

  @Autowired
  private WikiService              noteBookService;

  @Autowired
  private SettingService           settingService;

  @Autowired
  private ListenerService          listenerService;

  @Autowired
  private MetadataService          metadataService;

  @Autowired
  private UserACL                  userACL;

  public TermsAndConditionPage saveTermsAndConditions(String content,
                                                      String lang,
                                                      Identity currentUserAclIdentity) throws IllegalAccessException {
    String username = currentUserAclIdentity.getUserId();
    if (!userACL.isAdministrator(currentUserAclIdentity)) {
      throw new IllegalAccessException("User doesn't have enough privileges to create terms and conditions page");
    }
    return saveTermsAndConditions(content, lang, username);
  }

  @SneakyThrows
  public TermsAndConditionPage updateTermsAndConditionsSettings(boolean published,
                                                                String lang,
                                                                Identity currentUserAclIdentity) throws IllegalAccessException {
    if (!userACL.isAdministrator(currentUserAclIdentity)) {
      throw new IllegalAccessException("User doesn't have enough privileges to update terms and conditions settings");
    }
    TermsAndConditionPage page = getTermsAndConditions(lang);

    MetadataItem metadataItem = getTermsAndConditionsMetadataItem(page.getId());

    String eventName;
    Map<String, String> settings = new HashMap<>();
    settings.put(PUBLISHED, String.valueOf(published));
    settings.put(PUBLISHED_DATE, published ? String.valueOf(System.currentTimeMillis()) : "");
    settings.put(PUBLISHED_VERSION_ID, String.valueOf(page.getLatestVersionId()));
    if (metadataItem != null) {
      String storedLatestVersionId = metadataItem.getProperties().get(PUBLISHED_VERSION_ID);
      metadataItem.setProperties(settings);
      metadataService.updateMetadataItem(metadataItem, Long.parseLong(page.getId()), false);
      eventName = !page.getLatestVersionId().equals(storedLatestVersionId) ? EVENT_NAME_UPDATED : null;
    } else {
      MetadataObject metadataObject = new MetadataObject(TC_METADATA_OBJECT_TYPE, page.getId());
      metadataService.createMetadataItem(metadataObject, TC_METADATA_KEY, settings, Long.parseLong(page.getId()));
      eventName = EVENT_NAME_ADDED;
    }
    if (published && eventName != null) {
      listenerService.broadcast(eventName, page.getId(), null);
    }
    return getTermsAndConditions(lang);
  }

  @SneakyThrows
  public TermsAndConditionPage getTermsAndConditions(String lang) {
    Page page = noteService.getNoteOfNoteBookByName(TC_NOTE_TYPE, IdentityConstants.SYSTEM, TC_NOTE_NAME);
    if (page != null && !StringUtils.equals(lang, page.getLang())) {
      Page publishedVersion = noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()), lang);
      publishedVersion = publishedVersion != null ? publishedVersion
                                                  : noteService.getPublishedVersionByPageIdAndLang(Long.parseLong(page.getId()),
                                                                                                   lang);
      if (publishedVersion != null) {
        page.setTitle(publishedVersion.getTitle());
        page.setContent(publishedVersion.getContent());
        page.setLang(publishedVersion.getLang());
        page.setProperties(publishedVersion.getProperties());
        page.setLatestVersionId(publishedVersion.getId());
      }
      return buildTermsAndConditionPage(page);

    }
    return null;
  }

  public void markTermsAsAcceptedForUser(String userId, String lang) {
    TermsAndConditionPage terms = getTermsAndConditions(lang);
    if (terms != null) {
      settingService.set(USER.id(userId),
                         SETTINGS_APP_SCOPE,
                         SETTINGS_KEY,
                         SettingValue.create(terms.getLatestPublishedVersionId()));
      listenerService.broadcast(EVENT_NAME_ACCEPTED, userId, null);
    }
  }

  public boolean isTermsAcceptedForUser(String userId, String lang) {
    TermsAndConditionPage terms = getTermsAndConditions(lang);
    if (terms != null && terms.isPublished()) {
      SettingValue<?> acceptedVersion = settingService.get(USER.id(userId), SETTINGS_APP_SCOPE, SETTINGS_KEY);
      String acceptedVersionValue = acceptedVersion == null
          || acceptedVersion.getValue() == null ? null : acceptedVersion.getValue().toString();

      return acceptedVersionValue != null && acceptedVersionValue.equals(terms.getLatestPublishedVersionId());
    }
    return true;
  }

  @SneakyThrows
  private TermsAndConditionPage saveTermsAndConditions(String content, String lang, String username) {
    Wiki noteBook = geTermsAndConditions();
    TermsAndConditionPage termsAndConditionPage = getTermsAndConditions(lang);
    if (termsAndConditionPage == null) {
      Page page = new Page(TC_NOTE_NAME, "");
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
  }

  @SneakyThrows
  private Wiki geTermsAndConditions() {
    Wiki noteBook = noteBookService.getWikiByTypeAndOwner(TC_NOTE_TYPE, IdentityConstants.SYSTEM);
    if (noteBook == null) {
      return noteBookService.createWiki(TC_NOTE_TYPE, IdentityConstants.SYSTEM);
    } else {
      return noteBook;
    }
  }

  private TermsAndConditionPage buildTermsAndConditionPage(Page page) {
    TermsAndConditionPage termsAndConditionPage = new TermsAndConditionPage();

    termsAndConditionPage.setId(page.getId());
    termsAndConditionPage.setParentPageId(page.getParentPageId());
    termsAndConditionPage.setName(page.getName());
    termsAndConditionPage.setTitle(page.getTitle());
    termsAndConditionPage.setContent(page.getContent());
    termsAndConditionPage.setLang(page.getLang());
    termsAndConditionPage.setProperties(page.getProperties());
    termsAndConditionPage.setLatestVersionId(page.getLatestVersionId());
    MetadataItem metadataItem = getTermsAndConditionsMetadataItem(page.getId());

    if (metadataItem != null) {
      boolean published = MapUtils.isNotEmpty(metadataItem.getProperties())
          && Boolean.parseBoolean(metadataItem.getProperties().get(PUBLISHED));
      String publishedDateStr = metadataItem.getProperties().get(PUBLISHED_DATE);
      long publishedDate = StringUtils.isNotBlank(publishedDateStr) ? Long.parseLong(publishedDateStr) : 0;
      String latestPublishedVersionId = MapUtils.isNotEmpty(metadataItem.getProperties())
                                                                                          ? metadataItem.getProperties()
                                                                                                        .get(LATEST_VERSION_ID)
                                                                                          : "";
      termsAndConditionPage.setPublished(published);
      termsAndConditionPage.setPublishedDate(publishedDate);
      termsAndConditionPage.setLatestPublishedVersionId(latestPublishedVersionId);
    }
    return termsAndConditionPage;
  }

  private MetadataItem getTermsAndConditionsMetadataItem(String pageId) {
    MetadataObject metadataObject = new MetadataObject(TC_METADATA_OBJECT_TYPE, pageId);
    return metadataService.getMetadataItemsByMetadataAndObject(TC_METADATA_KEY, metadataObject).stream().findFirst().orElse(null);
  }
}
