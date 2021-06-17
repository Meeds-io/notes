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
    name = "XWiki",
    propOrder = {
        "version",
        "syntaxes"
    }
)
@XmlRootElement(name = "xwiki")
public class Xwiki extends LinkCollection {

  @XmlElement(required = true)
  protected String   version;

  @XmlElement(required = true)
  protected Syntaxes syntaxes;

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
   * Obtient la valeur de la propriété syntaxes.
   * 
   * @return possible object is {@link Syntaxes }
   */
  public Syntaxes getSyntaxes() {
    return syntaxes;
  }

  /**
   * Définit la valeur de la propriété syntaxes.
   * 
   * @param value allowed object is {@link Syntaxes }
   */
  public void setSyntaxes(Syntaxes value) {
    this.syntaxes = value;
  }

}
