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
    name = "Comment",
    propOrder = {
        "id",
        "pageId",
        "author",
        "date",
        "highlight",
        "text",
        "replyTo"
    }
)
@XmlRootElement(name = "comment")
public class Comment
                     extends LinkCollection {

  protected int      id;

  @XmlElement(required = true)
  protected String   pageId;

  @XmlElement(required = true)
  protected String   author;

  @XmlElement(required = true, type = String.class)
  @XmlJavaTypeAdapter(Adapter1.class)
  @XmlSchemaType(name = "dateTime")
  protected Calendar date;

  @XmlElement(required = true)
  protected String   highlight;

  @XmlElement(required = true)
  protected String   text;

  @XmlElement(required = true, type = Integer.class, nillable = true)
  protected Integer  replyTo;

  /**
   * Obtient la valeur de la propriété id.
   */
  public int getId() {
    return id;
  }

  /**
   * Définit la valeur de la propriété id.
   */
  public void setId(int value) {
    this.id = value;
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
   * Obtient la valeur de la propriété highlight.
   * 
   * @return possible object is {@link String }
   */
  public String getHighlight() {
    return highlight;
  }

  /**
   * Définit la valeur de la propriété highlight.
   * 
   * @param value allowed object is {@link String }
   */
  public void setHighlight(String value) {
    this.highlight = value;
  }

  /**
   * Obtient la valeur de la propriété text.
   * 
   * @return possible object is {@link String }
   */
  public String getText() {
    return text;
  }

  /**
   * Définit la valeur de la propriété text.
   * 
   * @param value allowed object is {@link String }
   */
  public void setText(String value) {
    this.text = value;
  }

  /**
   * Obtient la valeur de la propriété replyTo.
   * 
   * @return possible object is {@link Integer }
   */
  public Integer getReplyTo() {
    return replyTo;
  }

  /**
   * Définit la valeur de la propriété replyTo.
   * 
   * @param value allowed object is {@link Integer }
   */
  public void setReplyTo(Integer value) {
    this.replyTo = value;
  }

}
