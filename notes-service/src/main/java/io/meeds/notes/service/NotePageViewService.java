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
package io.meeds.notes.service;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.pom.data.PageKey;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityConstants;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;

import io.meeds.notes.model.NotePageData;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

import lombok.SneakyThrows;

public class NotePageViewService {

  private static final String DEFAULT_CONTENT_LANG = "";

  public static final String  CMS_CONTENT_TYPE     = "notePage";

  private static final Log    LOG                  = ExoLogger.getLogger(NotePageViewService.class);

  private NoteService         noteService;

  private WikiService         noteBookService;

  private CMSService          cmsService;

  private UserACL             userACL;

  private LocaleConfigService localeConfigService;

  public NotePageViewService(NoteService noteService,
                             WikiService noteBookService,
                             CMSService cmsService,
                             UserACL userACL,
                             LocaleConfigService localeConfigService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.cmsService = cmsService;
    this.userACL = userACL;
    this.localeConfigService = localeConfigService;
  }

  public Page getNotePage(String name, String lang, Identity currentUserAclIdentity) throws IllegalAccessException {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    if (setting == null) {
      return null;
    } else {
      if (!cmsService.hasAccessPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, name)) {
        throw new IllegalAccessException("Note page isn't accessible");
      }
      return getNotePage(setting, lang);
    }
  }

  public NotePageData getNotePageData(String name) {
    Map<String, Page> pages = getNotePages(name);
    NotePageData pageData = new NotePageData();
    pageData.setPages(new HashMap<>());
    pages.entrySet()
         .forEach(e -> pageData.getPages()
                               .put(e.getKey(), e.getValue().getContent()));
    return pageData;
  }

  public void savePageData(String name, NotePageData pageData) {
    Map<String, String> pages = pageData.getPages();
    if (MapUtils.isEmpty(pages)) {
      return;
    }
    String pageContent = pages.get(DEFAULT_CONTENT_LANG);
    Page page = saveNotePage(name, pageContent, null, userACL.getSuperUser());
    pages.forEach((lang, content) -> {
      if (!StringUtils.equals(lang, page.getLang())) {
        page.setContent(content);
        page.setLang(lang);
        try {
          noteService.createVersionOfNote(page, userACL.getSuperUser());
        } catch (Exception e) {
          LOG.warn("Error saving SNV {} content for lang {}", name, lang, e);
        }
      }
    });
  }

  @SneakyThrows
  public Map<String, Page> getNotePages(String name) {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    if (setting == null) {
      return Collections.emptyMap();
    } else {
      PageKey pageKey = getPageKey(setting);
      String noteType = pageKey.getType();
      String noteOwner = pageKey.getId();
      Page page = noteService.getNoteOfNoteBookByName(noteType, noteOwner, name);
      if (page == null) {
        return Collections.emptyMap();
      }
      Map<String, Page> pages = new HashMap<>();
      pages.put(DEFAULT_CONTENT_LANG, page);
      long pageId = Long.parseLong(page.getId());
      List<String> languages = noteService.getPageAvailableTranslationLanguages(pageId, false);
      languages.forEach(lang -> {
        Page pageByLang = noteService.getNoteByIdAndLang(pageId, lang);
        if (pageByLang != null) {
          pages.put(lang, pageByLang);
        }
      });
      return pages;
    }
  }

  public Page getNotePage(String name, String lang) {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    if (setting == null) {
      return null;
    } else {
      return getNotePage(setting, lang);
    }
  }

  public void saveNotePage(String name,
                           String content,
                           String lang,
                           Identity currentUserAclIdentity) throws IllegalAccessException, ObjectNotFoundException {
    if (!cmsService.isSettingNameExists(CMS_CONTENT_TYPE, name)) {
      throw new ObjectNotFoundException(String.format("CMS Setting name %s wasn't found", name));
    } else {
      if (!cmsService.hasEditPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, name)) {
        throw new IllegalAccessException("Note page isn't editable");
      }
      String username = currentUserAclIdentity.getUserId();
      saveNotePage(name, content, lang, username);
    }
  }

  private Page saveNotePage(String name,
                            String content,
                            String lang,
                            String username) {
    CMSSetting setting = cmsService.getSetting(CMS_CONTENT_TYPE, name);
    String pageReference = setting.getPageReference();
    PageKey pageKey = PageKey.create(pageReference);
    try {
      Wiki noteBook = getNote(pageKey);
      Page page = getNotePage(pageKey, name, null);
      if (page == null) {
        page = new Page(name, name);
        page.setContent(content);
        page.setCreatedDate(new Date());
        page.setUpdatedDate(new Date());
        page.setOwner(IdentityConstants.SYSTEM);
        noteService.createNote(noteBook, noteBook.getWikiHome(), page);
      } else {
        String defaultLang = getDefaultLanguage();
        Page pageWithLang = getNotePage(pageKey, name, lang);
        if (pageWithLang == null
            || StringUtils.isBlank(pageWithLang.getLang())
            || StringUtils.equals(defaultLang, pageWithLang.getLang())) {
          page.setLang(null);
          page.setContent(content);
          page.setUpdatedDate(new Date());
          page = noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT);
        } else {
          page.setUpdatedDate(new Date());
          page = noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT);
          page.setContent(content);
          page.setLang(pageWithLang.getLang());
        }
        noteService.createVersionOfNote(page, username);
        noteService.removeDraftOfNote(page);
      }
      return getNotePage(pageKey, name, lang);
    } catch (WikiException e) {
      throw new IllegalStateException(String.format("Error retrieving note with name %s referenced in page %s",
                                                    name,
                                                    pageKey),
                                      e);
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

  private Page getNotePage(CMSSetting setting, String lang) {
    PageKey pageKey = getPageKey(setting);
    return getNotePage(pageKey, setting.getName(), lang);
  }

  private Page getNotePage(PageKey pageKey, String name, String lang) {
    try {
      String ownerType = pageKey.getType();
      String ownerName = pageKey.getId();
      Page page = noteService.getNoteOfNoteBookByName(ownerType, ownerName, name);
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
      throw new IllegalStateException(String.format("Error retrieving note with name %s referenced in page %s",
                                                    name,
                                                    pageKey),
                                      e);
    }
  }

  private PageKey getPageKey(CMSSetting setting) {
    String pageReference = setting.getPageReference();
    return PageKey.create(pageReference);
  }

  private String getDefaultLanguage() {
    return localeConfigService.getDefaultLocaleConfig().getLocale().toLanguageTag();
  }

}
