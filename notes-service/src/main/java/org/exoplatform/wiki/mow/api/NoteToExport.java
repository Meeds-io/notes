/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.wiki.mow.api;

import java.util.Date;
import java.util.List;

import org.exoplatform.wiki.service.BreadcrumbData;

import lombok.Data;

@Data
public class NoteToExport {

  private String id;

  private String name;

  private String owner;

  private String author;

  private String content;

  private String syntax;

  private String title;

  private String comment;

  private String wikiId;

  private String wikiType;

  private String wikiOwner;

  private List<NoteToExport> children;

  private NoteToExport parent;

  public NoteToExport() {
  }

  public NoteToExport(String name) {
    this.name = name;
  }

  public NoteToExport(String id,String name, String owner, String author, String content, String syntax, String title, String comment, String wikiId, String wikiType, String wikiOwner) {
    this();
    this.id = id;
    this.name = name;
    this.owner = owner;
    this.author = author;
    this.content = content;
    this.syntax = syntax;
    this.title = title;
    this.comment = comment;
    this.wikiId = wikiId;
    this.wikiType = wikiType;
    this.wikiOwner = wikiOwner;
  }


}
