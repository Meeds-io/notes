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

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.PageUpdateType;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.service.plugin.WikiPageAttachmentPlugin;

import io.meeds.notes.model.NotePageData;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

import lombok.SneakyThrows;

public class NotePageViewService {

  public static final String   CMS_CONTENT_TYPE             = "notePage";

  private static final Pattern NOTES_ATTACHMENT_URI_PATTERN = Pattern.compile("wikiPage/(\\d*)/(\\d*)\"");

  private static final String  DEFAULT_CONTENT_LANG         = "";

  private static final Log     LOG                          = ExoLogger.getLogger(NotePageViewService.class);

  private NoteService          noteService;

  private WikiService          noteBookService;

  private CMSService           cmsService;

  private AttachmentService    attachmentService;

  private IdentityManager      identityManager;

  private UserACL              userACL;

  private LocaleConfigService  localeConfigService;

  public NotePageViewService(NoteService noteService,
                             WikiService noteBookService,
                             CMSService cmsService,
                             AttachmentService attachmentService,
                             IdentityManager identityManager,
                             UserACL userACL,
                             LocaleConfigService localeConfigService) {
    this.noteService = noteService;
    this.noteBookService = noteBookService;
    this.cmsService = cmsService;
    this.attachmentService = attachmentService;
    this.identityManager = identityManager;
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

  @SneakyThrows
  public Page savePageData(String name, NotePageData pageData) {
    Map<String, String> pages = pageData.getPages();
    if (MapUtils.isEmpty(pages)) {
      return null;
    }
    String pageContent = pages.get(DEFAULT_CONTENT_LANG);
    Identity identity = userACL.getUserIdentity(userACL.getSuperUser());
    Page page = persistNotePage(name, pageContent, null, identity);
    String pageId = page.getId(); // NOSONAR
    long superUserIdentityId = getSuperUserIdentityId();
    String pageContentReplacement = clonePageAttachments(pageId, pageContent, (oldPageId, fileId) -> true, superUserIdentityId);
    if (!StringUtils.equals(pageContentReplacement, pageContent)) {
      page.setLang(null);
      page.setContent(pageContentReplacement);
      page.setUpdatedDate(new Date());
      noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT);
    }
    pages.forEach((lang, content) -> {
      if (!StringUtils.equals(lang, page.getLang())) {
        try {
          page.setContent(clonePageAttachments(pageId, content, (oldPageId, fileId) -> true, superUserIdentityId));
          page.setLang(lang);
          noteService.createVersionOfNote(page, userACL.getSuperUser());
        } catch (Exception e) {
          LOG.warn("Error saving SNV {} content for lang {}", name, lang, e);
        }
      }
    });
    return page;
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
        if (!pages.containsKey(lang)) {
          Page pageByLang = noteService.getNoteByIdAndLang(pageId, lang);
          if (pageByLang != null) {
            pages.put(lang, pageByLang);
          }
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

  public Page saveNotePage(String name,
                           String content,
                           String lang,
                           Identity currentUserAclIdentity) throws IllegalAccessException, ObjectNotFoundException {
    requireEditableSetting(name, currentUserAclIdentity);
    return persistNotePage(name, content, lang, currentUserAclIdentity);
  }

  /**
   * Writes the content of the page note in a language as a new published
   * version of that language, attributed to the user, creating the
   * translation when the note holds none in that language. The language is
   * the stored form ({@code Locale.toString()}, as {@code es_ES}), whatever
   * the platform's default language. The default content and the metadata of
   * every language stay as stored; the note's update date moves and every
   * draft of the note is removed, as the drawer's save does.
   *
   * @param  name                    the setting name of the window
   * @param  content                 the content to publish in the language
   * @param  lang                    the language, in its stored form
   * @param  currentUserAclIdentity  the writing user
   * @return                         the note as read in that language
   * @throws ObjectNotFoundException when no setting carries the name
   * @throws IllegalAccessException  when the user may not edit the setting's
   *                                   page
   * @throws IllegalArgumentException when the language is blank, or when the
   *                                    note has no default content yet
   */
  public Page createNoteTranslation(String name,
                                    String content,
                                    String lang,
                                    Identity currentUserAclIdentity) throws IllegalAccessException,
                                                                     ObjectNotFoundException {
    requireEditableSetting(name, currentUserAclIdentity);
    if (StringUtils.isBlank(lang)) {
      throw new IllegalArgumentException("lang is mandatory");
    }
    PageKey pageKey = getPageKey(cmsService.getSetting(CMS_CONTENT_TYPE, name));
    Page page = getNotePage(pageKey, name, null);
    if (page == null) {
      throw new IllegalArgumentException(String.format("Note page %s has no default content yet", name));
    }
    try {
      persistNoteTranslation(page, content, lang, currentUserAclIdentity);
    } catch (WikiException e) {
      throw new IllegalStateException(String.format("Error saving note with name %s referenced in page %s in language %s",
                                                    name,
                                                    pageKey,
                                                    lang),
                                      e);
    }
    return getNotePage(pageKey, name, lang);
  }

  /**
   * Copies the content of a page note, in its default language and in every
   * published translation, onto the note of another setting, attributed to
   * the user: a file the source content names among the source note's
   * attachments is attached to the target note and the content copied names
   * it there. The target's content is replaced language by language; its
   * metadata and the source note stay as stored.
   *
   * @param  sourceName              the setting name of the note to copy
   * @param  newName                 the setting name of the note to write
   * @param  currentUserAclIdentity  the copying user
   * @return                         the target note's default content, null
   *                                 when the source holds no note yet
   * @throws ObjectNotFoundException when either setting does not exist
   * @throws IllegalAccessException  when the user may not read the source
   *                                   or edit the target's page
   */
  public Page copyNotePage(String sourceName,
                           String newName,
                           Identity currentUserAclIdentity) throws IllegalAccessException, ObjectNotFoundException {
    if (!cmsService.isSettingNameExists(CMS_CONTENT_TYPE, sourceName)) {
      throw new ObjectNotFoundException(String.format("CMS Setting name %s wasn't found", sourceName));
    } else if (!cmsService.hasAccessPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, sourceName)) {
      throw new IllegalAccessException("Note page isn't accessible");
    }
    requireEditableSetting(newName, currentUserAclIdentity);
    Map<String, Page> sourcePages = getNotePages(sourceName);
    Page sourcePage = sourcePages.get(DEFAULT_CONTENT_LANG);
    if (sourcePage == null) {
      return null;
    }
    String sourcePageId = sourcePage.getId();
    Map<String, String> pages = new HashMap<>();
    sourcePages.forEach((lang, page) -> pages.put(lang, page.getContent()));
    long superUserIdentityId = getSuperUserIdentityId();
    return writeNotePages(newName,
                          pages,
                          (pageId, content) -> clonePageAttachments(pageId,
                                                                    content,
                                                                    (oldPageId, fileId) -> StringUtils.equals(sourcePageId, oldPageId)
                                                                                           && isAttachment(oldPageId, fileId),
                                                                    superUserIdentityId),
                          currentUserAclIdentity);
  }

  /**
   * Writes the contents of a {@link NotePageData}, the window's
   * {@code data.init} preference, onto the note of a setting, attributed to
   * the user: the default content, then each translation. A file the
   * contents name among the attachments of a note the user can read is
   * attached to the target note by the user and the content written names it
   * there; any other named file is left as named. The preference is one any
   * editor of the page can write, so nothing is written as the super user.
   *
   * @param  name                    the setting name of the note to write
   * @param  pageData                the contents per language, the default
   *                                   content under the empty key
   * @param  currentUserAclIdentity  the importing user
   * @return                         the target note's default content, null
   *                                 when the data holds no default content
   * @throws ObjectNotFoundException when the setting does not exist
   * @throws IllegalAccessException  when the user may not edit the setting's
   *                                   page
   */
  public Page importNotePageData(String name,
                                 NotePageData pageData,
                                 Identity currentUserAclIdentity) throws IllegalAccessException, ObjectNotFoundException {
    requireEditableSetting(name, currentUserAclIdentity);
    if (pageData == null || MapUtils.isEmpty(pageData.getPages())) {
      return null;
    }
    long userIdentityId = Long.parseLong(identityManager.getOrCreateUserIdentity(currentUserAclIdentity.getUserId()).getId());
    return writeNotePages(name,
                          pageData.getPages(),
                          (pageId, content) -> clonePageAttachments(pageId,
                                                                    content,
                                                                    (oldPageId, fileId) -> isReadableAttachment(oldPageId,
                                                                                                                fileId,
                                                                                                                currentUserAclIdentity),
                                                                    userIdentityId),
                          currentUserAclIdentity);
  }

  private Page persistNotePage(String name,
                            String content,
                            String lang,
                            Identity userIdentity) {
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
          page = noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT, userIdentity);
          noteService.createVersionOfNote(page, userIdentity.getUserId());
          noteService.removeDraftOfNote(page);
        } else {
          persistNoteTranslation(page, content, pageWithLang.getLang(), userIdentity);
        }
      }
      return getNotePage(pageKey, name, lang);
    } catch (Exception e) {
      throw new IllegalStateException(String.format("Error retrieving note with name %s referenced in page %s",
                                                    name,
                                                    pageKey),
                                      e);
    }
  }

  /**
   * @param  name              the setting name of the note to write
   * @param  pages             the contents per language, the default under
   *                             the empty key
   * @param  attachmentsCloner maps a content to the one written, given the
   *                             target note id
   * @return                   the target note's default content, null when
   *                           the pages hold no default content
   */
  private Page writeNotePages(String name,
                              Map<String, String> pages,
                              BiFunction<String, String, String> attachmentsCloner,
                              Identity userIdentity) {
    String defaultContent = pages.get(DEFAULT_CONTENT_LANG);
    if (defaultContent == null) {
      return null;
    }
    Page page = persistNotePage(name, defaultContent, null, userIdentity);
    String pageId = page.getId();
    try {
      String content = attachmentsCloner.apply(pageId, defaultContent);
      if (!StringUtils.equals(content, defaultContent)) {
        page.setLang(null);
        page.setContent(content);
        page.setUpdatedDate(new Date());
        page = noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT);
      }
      for (Map.Entry<String, String> translation : pages.entrySet()) {
        if (StringUtils.isNotBlank(translation.getKey()) && translation.getValue() != null) {
          page = persistNoteTranslation(page,
                                        attachmentsCloner.apply(pageId, translation.getValue()),
                                        translation.getKey(),
                                        userIdentity);
        }
      }
    } catch (WikiException e) {
      throw new IllegalStateException(String.format("Error writing the languages of note with name %s", name), e);
    }
    return getNotePage(name, null);
  }

  private Page persistNoteTranslation(Page page, String content, String lang, Identity userIdentity) throws WikiException {
    page.setUpdatedDate(new Date());
    Page updatedPage = noteService.updateNote(page, PageUpdateType.EDIT_PAGE_CONTENT);
    Page version = new Page(updatedPage);
    version.setContent(content);
    version.setLang(lang);
    version.setProperties(null);
    noteService.createVersionOfNote(version, userIdentity.getUserId());
    noteService.removeDraftOfNote(version);
    return updatedPage;
  }

  private void requireEditableSetting(String name, Identity currentUserAclIdentity) throws ObjectNotFoundException,
                                                                                    IllegalAccessException {
    if (!cmsService.isSettingNameExists(CMS_CONTENT_TYPE, name)) {
      throw new ObjectNotFoundException(String.format("CMS Setting name %s wasn't found", name));
    } else if (!cmsService.hasEditPermission(currentUserAclIdentity, CMS_CONTENT_TYPE, name)) {
      throw new IllegalAccessException("Note page isn't editable");
    }
  }

  /**
   * @param  pageId            the note the files are attached to
   * @param  pageContent       the content naming the files, each as a note
   *                             id and a file id
   * @param  cloned            whether a named file, given the note id and
   *                             the file id it is named with, is cloned
   * @param  creatorIdentityId the identity the attachments are created by
   * @return                   the content naming the cloned files on the
   *                           target note
   */
  private String clonePageAttachments(String pageId,
                                      String pageContent,
                                      BiPredicate<String, String> cloned,
                                      long creatorIdentityId) {
    Matcher matcher = NOTES_ATTACHMENT_URI_PATTERN.matcher(pageContent);
    String pageContentReplacement = pageContent;
    while (matcher.find()) {
      String oldPageId = matcher.group(1);
      String fileId = matcher.group(2);
      try {
        if (cloned.test(oldPageId, fileId)) {
          createAttachment(pageId, fileId, creatorIdentityId);
          pageContentReplacement = pageContentReplacement.replace(String.format("wikiPage/%s/%s\"", oldPageId, fileId),
                                                                  String.format("wikiPage/%s/%s\"", pageId, fileId));
        }
      } catch (Exception e) {
        LOG.warn("Error while creating File attachment {} for page with id {}", fileId, pageId);
      }
    }
    return pageContentReplacement;
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

  private boolean isAttachment(String pageId, String fileId) {
    return attachmentService.getAttachment(WikiPageAttachmentPlugin.OBJECT_TYPE, pageId, fileId) != null;
  }

  /**
   * @return whether the user may view the note and the file is one of its
   *         attachments
   */
  private boolean isReadableAttachment(String pageId, String fileId, Identity userAclIdentity) {
    try {
      Page note = noteService.getNoteById(pageId);
      return note != null && noteService.canViewNote(note, userAclIdentity.getUserId()) && isAttachment(pageId, fileId);
    } catch (WikiException e) {
      LOG.debug("Note {} named by an imported content could not be read for {}", pageId, userAclIdentity.getUserId(), e);
      return false;
    }
  }

  @SneakyThrows
  private void createAttachment(String pageId, String fileId, long creatorIdentityId) {
    if (!isAttachment(pageId, fileId)) {
      attachmentService.createAttachment(fileId,
                                         WikiPageAttachmentPlugin.OBJECT_TYPE,
                                         pageId,
                                         null,
                                         creatorIdentityId,
                                         Collections.emptyMap());
    }
  }

  private long getSuperUserIdentityId() {
    return Long.parseLong(identityManager.getOrCreateUserIdentity(userACL.getSuperUser()).getId());
  }

}
