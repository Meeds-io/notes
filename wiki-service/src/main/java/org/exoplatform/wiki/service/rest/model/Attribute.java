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
@XmlType(name = "Attribute")
public class Attribute
                       extends LinkCollection {

  @XmlAttribute(name = "name", required = true)
  protected String name;

  @XmlAttribute(name = "value")
  protected String value;

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
   * Obtient la valeur de la propriété value.
   * 
   * @return possible object is {@link String }
   */
  public String getValue() {
    return value;
  }

  /**
   * Définit la valeur de la propriété value.
   * 
   * @param value allowed object is {@link String }
   */
  public void setValue(String value) {
    this.value = value;
  }

}
