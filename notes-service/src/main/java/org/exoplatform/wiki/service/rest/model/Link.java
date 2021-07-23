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
@XmlType(name = "Link")
public class Link {

  @XmlAttribute(name = "href")
  protected String href;

  @XmlAttribute(name = "rel")
  protected String rel;

  @XmlAttribute(name = "type")
  protected String type;

  @XmlAttribute(name = "hrefLang")
  protected String hrefLang;

  /**
   * Obtient la valeur de la propriété href.
   * 
   * @return possible object is {@link String }
   */
  public String getHref() {
    return href;
  }

  /**
   * Définit la valeur de la propriété href.
   * 
   * @param value allowed object is {@link String }
   */
  public void setHref(String value) {
    this.href = value;
  }

  /**
   * Obtient la valeur de la propriété rel.
   * 
   * @return possible object is {@link String }
   */
  public String getRel() {
    return rel;
  }

  /**
   * Définit la valeur de la propriété rel.
   * 
   * @param value allowed object is {@link String }
   */
  public void setRel(String value) {
    this.rel = value;
  }

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
   * Obtient la valeur de la propriété hrefLang.
   * 
   * @return possible object is {@link String }
   */
  public String getHrefLang() {
    return hrefLang;
  }

  /**
   * Définit la valeur de la propriété hrefLang.
   * 
   * @param value allowed object is {@link String }
   */
  public void setHrefLang(String value) {
    this.hrefLang = value;
  }

}
