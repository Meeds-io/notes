/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.model;

import java.util.LinkedList;
import java.util.List;

import io.meeds.notes.model.NotePageProperties;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NoteToExport {

  private String             id;

  private String             name;

  private String             owner;

  private String             author;

  private String             content;

  private String             syntax;

  private String             title;

  private String             comment;

  private String             wikiId;

  private String             wikiType;

  private String             wikiOwner;

  private List<NoteToExport> children;

  private NoteToExport       parent;

  private LinkedList<String> ancestors;

  private NotePageProperties properties;

  public NoteToExport() {
  }

  public NoteToExport(String name) {
    this.name = name;
  }

  public NoteToExport(String id,
                      String name,
                      String owner,
                      String author,
                      String content,
                      String syntax,
                      String title,
                      String comment,
                      String wikiId,
                      String wikiType,
                      String wikiOwner) {
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

  public NoteToExport(NoteToExport noteToExport) {
    this.id = noteToExport.getId();
    this.name = noteToExport.getName();
    this.owner = noteToExport.getOwner();
    this.author = noteToExport.getAuthor();
    this.content = noteToExport.getContent();
    this.syntax = noteToExport.getSyntax();
    this.title = noteToExport.getTitle();
    this.comment = noteToExport.getComment();
    this.wikiId = noteToExport.getWikiId();
    this.wikiType = noteToExport.getWikiType();
    this.wikiOwner = noteToExport.getWikiOwner();
    this.children = noteToExport.getChildren();
    this.parent = noteToExport.getParent();
    this.ancestors = noteToExport.getAncestors();
  }

}
