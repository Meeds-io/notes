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
    name = "Attachment",
    propOrder = {
        "id",
        "name",
        "size",
        "version",
        "pageId",
        "pageVersion",
        "mimeType",
        "author",
        "date",
        "xwikiRelativeUrl",
        "xwikiAbsoluteUrl"
    }
)
@XmlRootElement(name = "attachment")
public class Attachment
                        extends LinkCollection {

  @XmlElement(required = true)
  protected String   id;

  @XmlElement(required = true)
  protected String   name;

  protected int      size;

  @XmlElement(required = true)
  protected String   version;

  @XmlElement(required = true)
  protected String   pageId;

  @XmlElement(required = true)
  protected String   pageVersion;

  @XmlElement(required = true)
  protected String   mimeType;

  @XmlElement(required = true)
  protected String   author;

  @XmlElement(required = true, type = String.class)
  @XmlJavaTypeAdapter(Adapter1.class)
  @XmlSchemaType(name = "dateTime")
  protected Calendar date;

  @XmlElement(required = true)
  protected String   xwikiRelativeUrl;

  @XmlElement(required = true)
  protected String   xwikiAbsoluteUrl;

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
   * Obtient la valeur de la propriété size.
   */
  public int getSize() {
    return size;
  }

  /**
   * Définit la valeur de la propriété size.
   */
  public void setSize(int value) {
    this.size = value;
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
   * Obtient la valeur de la propriété pageVersion.
   * 
   * @return possible object is {@link String }
   */
  public String getPageVersion() {
    return pageVersion;
  }

  /**
   * Définit la valeur de la propriété pageVersion.
   * 
   * @param value allowed object is {@link String }
   */
  public void setPageVersion(String value) {
    this.pageVersion = value;
  }

  /**
   * Obtient la valeur de la propriété mimeType.
   * 
   * @return possible object is {@link String }
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Définit la valeur de la propriété mimeType.
   * 
   * @param value allowed object is {@link String }
   */
  public void setMimeType(String value) {
    this.mimeType = value;
  }

  /**
   * Obtient la valeur de la propriété author.
   * 
   * @return possible object is {@link String }
   */
  public String getAuthor() {
    return author;
  }

  /**
   * Définit la valeur de la propriété author.
   * 
   * @param value allowed object is {@link String }
   */
  public void setAuthor(String value) {
    this.author = value;
  }

  /**
   * Obtient la valeur de la propriété date.
   * 
   * @return possible object is {@link String }
   */
  public Calendar getDate() {
    return date;
  }

  /**
   * Définit la valeur de la propriété date.
   * 
   * @param value allowed object is {@link String }
   */
  public void setDate(Calendar value) {
    this.date = value;
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

}
