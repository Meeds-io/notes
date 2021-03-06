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
