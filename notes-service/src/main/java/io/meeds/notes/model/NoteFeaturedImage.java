/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
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
package io.meeds.notes.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
<<<<<<< HEAD
import java.io.Serializable;
=======
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))

@Data
@NoArgsConstructor
@AllArgsConstructor
<<<<<<< HEAD
public class NoteFeaturedImage implements Serializable {

  private Long                  id;

  private String                fileName;

  private String                mimeType;

  private long                  fileSize;

  private Long                  lastUpdated;

  private String                uploadId;

  private String                altText;

  private transient InputStream fileInputStream;

  private boolean               toDelete;
=======
public class NoteFeaturedImage {

  private Long        id;

  private String      fileName;

  private String      mimeType;

  private long        fileSize;

  private Long        lastUpdated;

  private String      base64Data;

  private String      uploadId;
  
  private String      altText;

  private InputStream fileInputStream;
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))

  public NoteFeaturedImage(Long id,
                           String fileName,
                           String mimeType,
                           long fileSize,
                           Long lastUpdated,
                           InputStream fileInputStream) {
    this.id = id;
    this.fileName = fileName;
    this.mimeType = mimeType;
    this.fileSize = fileSize;
    this.lastUpdated = lastUpdated;
    this.fileInputStream = fileInputStream;
  }

<<<<<<< HEAD
  public NoteFeaturedImage(Long id,
                           String mimeType,
                           String uploadId,
                           String altText,
                           Long lastUpdated,
                           boolean toDelete) {
    this.id = id;
    this.mimeType = mimeType;
    this.uploadId = uploadId;
    this.altText = altText;
    this.lastUpdated = lastUpdated;
    this.toDelete = toDelete;
=======
  public NoteFeaturedImage(String base64Data, String mimeType, String uploadId, String altText) {
    this.base64Data = base64Data;
    this.mimeType = mimeType;
    this.uploadId = uploadId;
    this.altText = altText;
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
  }
}
