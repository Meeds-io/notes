/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.model.PageContentBlock;
import io.meeds.social.cms.plugin.PageContentBlockPlugin;
import io.meeds.social.cms.service.PageContentBlockPluginService;

import jakarta.annotation.PostConstruct;

/**
 * Extracts the content of a Notes "Single Note View" content block so that
 * Social's generic {@code PageContentIndexingConnector} can index it,
 * without Social depending on Notes.
 */
@Component
public class NotePageContentBlockPlugin implements PageContentBlockPlugin {

  @Autowired
  private NotePageViewService        notePageViewService;

  @Autowired
  private PageContentBlockPluginService pluginService;

  @PostConstruct
  public void init() {
    pluginService.addPlugin(this);
  }

  @Override
  public String getContentType() {
    return NotePageViewService.CMS_CONTENT_TYPE;
  }

  @Override
  public PageContentBlock getContent(CMSSetting setting) {
    Map<String, Page> pages = notePageViewService.getNotePages(setting.getName());
    Page defaultPage = pages.get("");
    if (defaultPage == null) {
      return null;
    }
    Map<String, String> content = new HashMap<>();
    pages.forEach((lang, page) -> content.put(lang, Utils.html2text(page.getContent())));
    Date date = defaultPage.getUpdatedDate() != null ? defaultPage.getUpdatedDate() : defaultPage.getCreatedDate();
    return new PageContentBlock(defaultPage.getAuthor(), date, content);
  }

}
