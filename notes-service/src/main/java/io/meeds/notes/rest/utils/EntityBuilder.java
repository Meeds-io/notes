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
import io.meeds.notes.rest.model.FeaturedImageEntity;
import io.meeds.notes.rest.model.PagePropertiesEntity;

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
    return new NoteFeaturedImage(featuredImageEntity.getBase64Data(),
                                 featuredImageEntity.getMimeType(),
                                 featuredImageEntity.getUploadId(),
                                 featuredImageEntity.getAltText());
  }
}
