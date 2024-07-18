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
package io.meeds.notes.model;

import org.exoplatform.social.metadata.model.MetadataObject;

public class NoteMetadataObject extends MetadataObject {

  public NoteMetadataObject(String type, String id, String parentId, long spaceId) {
    super(type, id, parentId, spaceId);
  }
}
