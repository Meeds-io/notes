/*
 * This file is part of the Meeds project (https://meeds.io/).
 * Copyright (C) 2022 Meeds Association
 * contact@meeds.io
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service;

import lombok.Data;

@Data
public class ExportAction {

  public static final String GETTING_NOTES           = "GETTING_NOTES";

  public static final String UPDATING_NOTES_PARENTS  = "UPDATING_NOTES_PARENTS";

  public static final String CREATING_CONTENT_DATA   = "CREATING_CONTENT_DATA";

  public static final String UPDATING_IMAGES_URLS    = "UPDATING_IMAGES_URLS";

  public static final String CREATING_ZIP_FILE       = "CREATING_ZIP_FILE";

  public static final String CLEANING_TEMP_FILE      = "CLEANING_TEMP_FILE";

  public static final String EXPORT_DATA_CREATED     = "EXPORT_DATA_CREATED";

  public static final String PROCESS_FEATURED_IMAGES = "PROCESS_FEATURED_IMAGES";

  private boolean            started                 = false;

  private boolean            notesGetted             = false;

  private boolean            notesPrepared           = false;

  private boolean            jsonCreated             = false;

  private boolean            imageUrlsUpdated        = false;

  private boolean            featuredImagesProcessed = false;

  private boolean            zipCreated              = false;

  private boolean            tempCleaned             = false;

  private boolean            dataCreated             = false;

  private String             action                  = "";

}
