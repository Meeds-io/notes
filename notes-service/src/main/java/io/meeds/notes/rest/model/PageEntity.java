package io.meeds.notes.rest.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PageEntity implements Serializable {

  private String               id;

  private String               name;

  private String               title;

  private String               content;

  private Long                 pageParentId;

  private String               parentPageName;

  private String               parentPageId;

  private String               wikiType;

  private String               wikiOwner;

  private String               owner;

  private String               author;

  private boolean              toBePublished;

  private String               url;

  private String               syntax;

  private String               appName;

  private String               lang;

  private PagePropertiesEntity properties;
}
