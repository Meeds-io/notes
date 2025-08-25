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
package org.exoplatform.wiki.service;

public class BreadcrumbData {
  
  private String id;

  private String path;

  private String title;
  
  private String wikiType;
  
  private String wikiOwner;

  private String noteId;

  public BreadcrumbData(String id, String title, String wikiType, String wikiOwner) {
    this.id = id;
    this.title = title;
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
  }

  public BreadcrumbData(String id, String noteId, String title, String wikiType, String wikiOwner) {
    this.id = id;
    this.title = title;
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
    this.noteId = noteId;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getWikiType() {
    return wikiType;
  }

  public void setWikiType(String wikiType) {
    this.wikiType = wikiType;
  }

  public String getWikiOwner() {
    return wikiOwner;
  }

  public void setWikiOwner(String wikiOwner) {
    this.wikiOwner = wikiOwner;
  }

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }
}
