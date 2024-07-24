/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
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
 *
 */
package io.meeds.notes.rest.utils;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NotePageProperties;
import io.meeds.notes.rest.model.DraftPageEntity;
import io.meeds.notes.rest.model.FeaturedImageEntity;
import io.meeds.notes.rest.model.PageEntity;
import io.meeds.notes.rest.model.PagePropertiesEntity;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;

public class EntityBuilder {

  private EntityBuilder() { // NOSONAR
  }

  public static NotePageProperties toNotePageProperties(PagePropertiesEntity pagePropertiesEntity) {
    if (pagePropertiesEntity == null) {
      return null;
    }
    return new NotePageProperties(pagePropertiesEntity.getNoteId(),
                                  pagePropertiesEntity.getSummary(),
                                  toNoteFeaturedImage(pagePropertiesEntity.getFeaturedImage()),
                                  pagePropertiesEntity.isDraft());
  }

  public static NoteFeaturedImage toNoteFeaturedImage(FeaturedImageEntity featuredImageEntity) {
    if (featuredImageEntity == null) {
      return null;
    }
    return new NoteFeaturedImage(featuredImageEntity.getId(),
                                 featuredImageEntity.getBase64Data(),
                                 featuredImageEntity.getMimeType(),
                                 featuredImageEntity.getUploadId(),
                                 featuredImageEntity.getAltText(),
                                 featuredImageEntity.getLastUpdated(),
                                 featuredImageEntity.isToDelete());
  }

  public static Page toPage(PageEntity pageEntity) {
    if (pageEntity == null) {
      return null;
    }
    Page page = new Page();
    page.setId(pageEntity.getId());
    page.setTitle(pageEntity.getTitle());
    page.setName(pageEntity.getName());
    page.setUrl(pageEntity.getUrl());
    page.setAuthor(pageEntity.getAuthor());
    page.setOwner(pageEntity.getOwner());
    page.setSyntax(pageEntity.getSyntax());
    page.setParentPageId(pageEntity.getParentPageId());
    page.setParentPageName(pageEntity.getParentPageName());
    page.setContent(pageEntity.getContent());
    page.setWikiType(pageEntity.getWikiType());
    page.setWikiOwner(pageEntity.getWikiOwner());
    page.setAppName(pageEntity.getAppName());
    page.setLang(pageEntity.getLang());
    page.setToBePublished(pageEntity.isToBePublished());
    page.setProperties(toNotePageProperties(pageEntity.getProperties()));
    return page;
  }
  
  public static DraftPage toDraftPage(DraftPageEntity draftPageEntity) {
    if (draftPageEntity == null) {
      return null;
    }
    DraftPage draftPage = new DraftPage();
    draftPage.setId(draftPageEntity.getId());
    draftPage.setTitle(draftPageEntity.getTitle());
    draftPage.setName(draftPageEntity.getName());
    draftPage.setUrl(draftPageEntity.getUrl());
    draftPage.setAuthor(draftPageEntity.getAuthor());
    draftPage.setOwner(draftPageEntity.getOwner());
    draftPage.setSyntax(draftPageEntity.getSyntax());
    draftPage.setParentPageId(draftPageEntity.getParentPageId());
    draftPage.setParentPageName(draftPageEntity.getParentPageName());
    draftPage.setContent(draftPageEntity.getContent());
    draftPage.setWikiType(draftPageEntity.getWikiType());
    draftPage.setWikiOwner(draftPageEntity.getWikiOwner());
    draftPage.setAppName(draftPageEntity.getAppName());
    draftPage.setToBePublished(draftPageEntity.isToBePublished());
    draftPage.setLang(draftPageEntity.getLang());
    draftPage.setTargetPageId(draftPageEntity.getTargetPageId());
    draftPage.setNewPage(draftPageEntity.isNewPage());
    draftPage.setProperties(toNotePageProperties(draftPageEntity.getProperties()));
    return draftPage;
  }
}
