/*
 * Copyright (C) 2003-2021 eXo Platform SAS.
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
package org.exoplatform.wiki.service.rest.model;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "PageSummary",
    propOrder = {
        "id",
        "fullName",
        "wiki",
        "space",
        "name",
        "title",
        "parent",
        "parentId",
        "xwikiRelativeUrl",
        "xwikiAbsoluteUrl",
        "translations",
        "syntax"
    }
)
@XmlSeeAlso(
  {
      Page.class
  }
)
public class PageSummary
                         extends LinkCollection {

  @XmlElement(required = true)
  protected String       id;

  @XmlElement(required = true)
  protected String       fullName;

  @XmlElement(required = true)
  protected String       wiki;

  @XmlElement(required = true)
  protected String       space;

  @XmlElement(required = true)
  protected String       name;

  @XmlElement(required = true)
  protected String       title;

  @XmlElement(required = true)
  protected String       parent;

  @XmlElement(required = true)
  protected String       parentId;

  @XmlElement(required = true)
  protected String       xwikiRelativeUrl;

  @XmlElement(required = true)
  protected String       xwikiAbsoluteUrl;

  @XmlElement(required = true)
  protected Translations translations;

  @XmlElement(required = true)
  protected String       syntax;

  /**
   * Obtient la valeur de la propriété id.
   * 
   * @return possible object is {@link String }
   */
  public String getId() {
    return id;
  }

  /**
   * Définit la valeur de la propriété id.
   * 
   * @param value allowed object is {@link String }
   */
  public void setId(String value) {
    this.id = value;
  }

  /**
   * Obtient la valeur de la propriété fullName.
   * 
   * @return possible object is {@link String }
   */
  public String getFullName() {
    return fullName;
  }

  /**
   * Définit la valeur de la propriété fullName.
   * 
   * @param value allowed object is {@link String }
   */
  public void setFullName(String value) {
    this.fullName = value;
  }

  /**
   * Obtient la valeur de la propriété wiki.
   * 
   * @return possible object is {@link String }
   */
  public String getWiki() {
    return wiki;
  }

  /**
   * Définit la valeur de la propriété wiki.
   * 
   * @param value allowed object is {@link String }
   */
  public void setWiki(String value) {
    this.wiki = value;
  }

  /**
   * Obtient la valeur de la propriété space.
   * 
   * @return possible object is {@link String }
   */
  public String getSpace() {
    return space;
  }

  /**
   * Définit la valeur de la propriété space.
   * 
   * @param value allowed object is {@link String }
   */
  public void setSpace(String value) {
    this.space = value;
  }

  /**
   * Obtient la valeur de la propriété name.
   * 
   * @return possible object is {@link String }
   */
  public String getName() {
    return name;
  }

  /**
   * Définit la valeur de la propriété name.
   * 
   * @param value allowed object is {@link String }
   */
  public void setName(String value) {
    this.name = value;
  }

  /**
   * Obtient la valeur de la propriété title.
   * 
   * @return possible object is {@link String }
   */
  public String getTitle() {
    return title;
  }

  /**
   * Définit la valeur de la propriété title.
   * 
   * @param value allowed object is {@link String }
   */
  public void setTitle(String value) {
    this.title = value;
  }

  /**
   * Obtient la valeur de la propriété parent.
   * 
   * @return possible object is {@link String }
   */
  public String getParent() {
    return parent;
  }

  /**
   * Définit la valeur de la propriété parent.
   * 
   * @param value allowed object is {@link String }
   */
  public void setParent(String value) {
    this.parent = value;
  }

  /**
   * Obtient la valeur de la propriété parentId.
   * 
   * @return possible object is {@link String }
   */
  public String getParentId() {
    return parentId;
  }

  /**
   * Définit la valeur de la propriété parentId.
   * 
   * @param value allowed object is {@link String }
   */
  public void setParentId(String value) {
    this.parentId = value;
  }

  /**
   * Obtient la valeur de la propriété xwikiRelativeUrl.
   * 
   * @return possible object is {@link String }
   */
  public String getXwikiRelativeUrl() {
    return xwikiRelativeUrl;
  }

  /**
   * Définit la valeur de la propriété xwikiRelativeUrl.
   * 
   * @param value allowed object is {@link String }
   */
  public void setXwikiRelativeUrl(String value) {
    this.xwikiRelativeUrl = value;
  }

  /**
   * Obtient la valeur de la propriété xwikiAbsoluteUrl.
   * 
   * @return possible object is {@link String }
   */
  public String getXwikiAbsoluteUrl() {
    return xwikiAbsoluteUrl;
  }

  /**
   * Définit la valeur de la propriété xwikiAbsoluteUrl.
   * 
   * @param value allowed object is {@link String }
   */
  public void setXwikiAbsoluteUrl(String value) {
    this.xwikiAbsoluteUrl = value;
  }

  /**
   * Obtient la valeur de la propriété translations.
   * 
   * @return possible object is {@link Translations }
   */
  public Translations getTranslations() {
    return translations;
  }

  /**
   * Définit la valeur de la propriété translations.
   * 
   * @param value allowed object is {@link Translations }
   */
  public void setTranslations(Translations value) {
    this.translations = value;
  }

  /**
   * Obtient la valeur de la propriété syntax.
   * 
   * @return possible object is {@link String }
   */
  public String getSyntax() {
    return syntax;
  }

  /**
   * Définit la valeur de la propriété syntax.
   * 
   * @param value allowed object is {@link String }
   */
  public void setSyntax(String value) {
    this.syntax = value;
  }

}
