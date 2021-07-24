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
    name = "SearchResult",
    propOrder = {
        "type",
        "id",
        "pageFullName",
        "wiki",
        "space",
        "pageName",
        "language",
        "className",
        "objectNumber"
    }
)
@XmlRootElement(name = "searchResult")
public class SearchResult
                          extends LinkCollection {

  @XmlElement(required = true)
  protected String  type;

  @XmlElement(required = true)
  protected String  id;

  @XmlElement(required = true)
  protected String  pageFullName;

  @XmlElement(required = true)
  protected String  wiki;

  @XmlElement(required = true)
  protected String  space;

  @XmlElement(required = true)
  protected String  pageName;

  @XmlElement(required = true)
  protected String  language;

  protected String  className;

  protected Integer objectNumber;

  /**
   * Obtient la valeur de la propriété type.
   * 
   * @return possible object is {@link String }
   */
  public String getType() {
    return type;
  }

  /**
   * Définit la valeur de la propriété type.
   * 
   * @param value allowed object is {@link String }
   */
  public void setType(String value) {
    this.type = value;
  }

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
   * Obtient la valeur de la propriété pageFullName.
   * 
   * @return possible object is {@link String }
   */
  public String getPageFullName() {
    return pageFullName;
  }

  /**
   * Définit la valeur de la propriété pageFullName.
   * 
   * @param value allowed object is {@link String }
   */
  public void setPageFullName(String value) {
    this.pageFullName = value;
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
   * Obtient la valeur de la propriété language.
   * 
   * @return possible object is {@link String }
   */
  public String getLanguage() {
    return language;
  }

  /**
   * Définit la valeur de la propriété language.
   * 
   * @param value allowed object is {@link String }
   */
  public void setLanguage(String value) {
    this.language = value;
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
   * Obtient la valeur de la propriété objectNumber.
   * 
   * @return possible object is {@link Integer }
   */
  public Integer getObjectNumber() {
    return objectNumber;
  }

  /**
   * Définit la valeur de la propriété objectNumber.
   * 
   * @param value allowed object is {@link Integer }
   */
  public void setObjectNumber(Integer value) {
    this.objectNumber = value;
  }

}
