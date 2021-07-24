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
    name = "ObjectSummary",
    propOrder = {
        "id",
        "guid",
        "pageId",
        "wiki",
        "space",
        "pageName",
        "className",
        "number",
        "headline"
    }
)
@XmlSeeAlso(
  {
      Object.class
  }
)
public class ObjectSummary
                           extends LinkCollection {

  @XmlElement(required = true)
  protected String id;

  @XmlElement(required = true)
  protected String guid;

  @XmlElement(required = true)
  protected String pageId;

  @XmlElement(required = true)
  protected String wiki;

  @XmlElement(required = true)
  protected String space;

  @XmlElement(required = true)
  protected String pageName;

  @XmlElement(required = true)
  protected String className;

  protected int    number;

  @XmlElement(required = true)
  protected String headline;

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
   * Obtient la valeur de la propriété guid.
   * 
   * @return possible object is {@link String }
   */
  public String getGuid() {
    return guid;
  }

  /**
   * Définit la valeur de la propriété guid.
   * 
   * @param value allowed object is {@link String }
   */
  public void setGuid(String value) {
    this.guid = value;
  }

  /**
   * Obtient la valeur de la propriété pageId.
   * 
   * @return possible object is {@link String }
   */
  public String getPageId() {
    return pageId;
  }

  /**
   * Définit la valeur de la propriété pageId.
   * 
   * @param value allowed object is {@link String }
   */
  public void setPageId(String value) {
    this.pageId = value;
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
   * Obtient la valeur de la propriété pageName.
   * 
   * @return possible object is {@link String }
   */
  public String getPageName() {
    return pageName;
  }

  /**
   * Définit la valeur de la propriété pageName.
   * 
   * @param value allowed object is {@link String }
   */
  public void setPageName(String value) {
    this.pageName = value;
  }

  /**
   * Obtient la valeur de la propriété className.
   * 
   * @return possible object is {@link String }
   */
  public String getClassName() {
    return className;
  }

  /**
   * Définit la valeur de la propriété className.
   * 
   * @param value allowed object is {@link String }
   */
  public void setClassName(String value) {
    this.className = value;
  }

  /**
   * Obtient la valeur de la propriété number.
   */
  public int getNumber() {
    return number;
  }

  /**
   * Définit la valeur de la propriété number.
   */
  public void setNumber(int value) {
    this.number = value;
  }

  /**
   * Obtient la valeur de la propriété headline.
   * 
   * @return possible object is {@link String }
   */
  public String getHeadline() {
    return headline;
  }

  /**
   * Définit la valeur de la propriété headline.
   * 
   * @param value allowed object is {@link String }
   */
  public void setHeadline(String value) {
    this.headline = value;
  }

}
