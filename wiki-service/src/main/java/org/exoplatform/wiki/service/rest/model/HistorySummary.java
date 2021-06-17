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

import java.util.Calendar;

import javax.xml.bind.annotation.*;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "HistorySummary",
    propOrder = {
        "pageId",
        "wiki",
        "space",
        "name",
        "version",
        "majorVersion",
        "minorVersion",
        "modified",
        "modifier",
        "language"
    }
)
public class HistorySummary
                            extends LinkCollection {

  @XmlElement(required = true)
  protected String   pageId;

  @XmlElement(required = true)
  protected String   wiki;

  @XmlElement(required = true)
  protected String   space;

  @XmlElement(required = true)
  protected String   name;

  @XmlElement(required = true)
  protected String   version;

  protected int      majorVersion;

  protected int      minorVersion;

  @XmlElement(required = true, type = String.class)
  @XmlJavaTypeAdapter(Adapter1.class)
  @XmlSchemaType(name = "dateTime")
  protected Calendar modified;

  @XmlElement(required = true)
  protected String   modifier;

  @XmlElement(required = true)
  protected String   language;

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
   * Obtient la valeur de la propriété version.
   * 
   * @return possible object is {@link String }
   */
  public String getVersion() {
    return version;
  }

  /**
   * Définit la valeur de la propriété version.
   * 
   * @param value allowed object is {@link String }
   */
  public void setVersion(String value) {
    this.version = value;
  }

  /**
   * Obtient la valeur de la propriété majorVersion.
   */
  public int getMajorVersion() {
    return majorVersion;
  }

  /**
   * Définit la valeur de la propriété majorVersion.
   */
  public void setMajorVersion(int value) {
    this.majorVersion = value;
  }

  /**
   * Obtient la valeur de la propriété minorVersion.
   */
  public int getMinorVersion() {
    return minorVersion;
  }

  /**
   * Définit la valeur de la propriété minorVersion.
   */
  public void setMinorVersion(int value) {
    this.minorVersion = value;
  }

  /**
   * Obtient la valeur de la propriété modified.
   * 
   * @return possible object is {@link String }
   */
  public Calendar getModified() {
    return modified;
  }

  /**
   * Définit la valeur de la propriété modified.
   * 
   * @param value allowed object is {@link String }
   */
  public void setModified(Calendar value) {
    this.modified = value;
  }

  /**
   * Obtient la valeur de la propriété modifier.
   * 
   * @return possible object is {@link String }
   */
  public String getModifier() {
    return modifier;
  }

  /**
   * Définit la valeur de la propriété modifier.
   * 
   * @param value allowed object is {@link String }
   */
  public void setModifier(String value) {
    this.modifier = value;
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

}
