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
    name = "Wiki",
    propOrder = {
        "id",
        "name"
    }
)
@XmlRootElement(name = "wiki")
public class Wiki
                  extends LinkCollection {

  @XmlElement(required = true)
  protected String id;

  @XmlElement(required = true)
  protected String name;

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

}
