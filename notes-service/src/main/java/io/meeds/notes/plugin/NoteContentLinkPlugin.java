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
package io.meeds.notes.plugin;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.search.SearchResult;
import org.exoplatform.wiki.service.search.WikiSearchData;

import io.meeds.social.cms.model.ContentLinkExtension;
import io.meeds.social.cms.model.ContentLinkSearchResult;
import io.meeds.social.cms.plugin.ContentLinkPlugin;
import io.meeds.social.cms.service.ContentLinkPluginService;

import jakarta.annotation.PostConstruct;
import lombok.SneakyThrows;

@Component
public class NoteContentLinkPlugin implements ContentLinkPlugin {

  public static final String                OBJECT_TYPE = NotePermanentLinkPlugin.OBJECT_TYPE;

  private static final String               TITLE_KEY   = "contentLink.note";

  private static final String               ICON        = "fa fa-clipboard";

  private static final String               COMMAND     = "note";

  private static final ContentLinkExtension EXTENSION   = new ContentLinkExtension(OBJECT_TYPE,
                                                                                   TITLE_KEY,
                                                                                   ICON,
                                                                                   COMMAND);

  @Autowired
  private ContentLinkPluginService          contentLinkPluginService;

  @Autowired
  private NoteService                       noteService;

  @PostConstruct
  public void init() {
    contentLinkPluginService.addPlugin(this);
  }

  @Override
  public ContentLinkExtension getExtension() {
    return EXTENSION;
  }

  @Override
  @SneakyThrows
  public List<ContentLinkSearchResult> search(String keyword, Identity identity, Locale locale, int offset, int limit) {
    WikiSearchData data = new WikiSearchData(keyword, identity == null ? null : identity.getUserId());
    data.setLimit(limit + offset);
    data.setWikiType("group");
    @SuppressWarnings("deprecation")
    List<SearchResult> results = noteService.search(data).getAll();
    return results.stream()
                  .map(searchResult -> toContentLink(searchResult, identity, locale))
                  .filter(Objects::nonNull)
                  .toList();
  }

  @Override
  @SneakyThrows
  public String getContentTitle(String objectId, Locale locale) {
    Page note = noteService.getNoteByIdAndLang(Long.parseLong(objectId), locale == null ? null : locale.toLanguageTag());
    if (note == null) {
      note = noteService.getNoteById(objectId);
    }
    return note == null || note.isDeleted() ? null : note.getTitle();
  }

  @SneakyThrows
  private ContentLinkSearchResult toContentLink(SearchResult searchResult,
                                                Identity identity,
                                                Locale locale) {
    Page page = noteService.getNoteByIdAndLang(searchResult.getId(), searchResult.getLang());
    if (page == null || !noteService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, identity)) {
      return null;
    } else {
      if (!StringUtils.equals(locale.toLanguageTag(), searchResult.getLang())) {
        Page pageWithLang = noteService.getNoteByIdAndLang(searchResult.getId(), locale.toLanguageTag());
        if (pageWithLang != null) {
          page = pageWithLang;
        }
      }
      return new ContentLinkSearchResult(OBJECT_TYPE, String.valueOf(searchResult.getId()), page.getTitle(), EXTENSION.getIcon());
    }
  }

}
