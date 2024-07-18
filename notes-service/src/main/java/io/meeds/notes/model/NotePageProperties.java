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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

<<<<<<< HEAD
import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotePageProperties implements Serializable {
=======
@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotePageProperties {
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))

  private long              noteId;

  private String            summary;

  private NoteFeaturedImage featuredImage;

  private boolean           isDraft;
}
