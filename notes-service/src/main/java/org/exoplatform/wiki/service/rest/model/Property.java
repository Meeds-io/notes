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

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(
    name = "Property",
    propOrder = {
        "attributes",
        "value"
    }
)
@XmlRootElement(name = "property")
public class Property
                      extends LinkCollection {

  @XmlElement(name = "attribute")
  protected List<Attribute> attributes;

  @XmlElement(required = true)
  protected String          value;

  @XmlAttribute(name = "name", required = true)
  protected String          name;

  @XmlAttribute(name = "type")
  protected String          type;

  /**
   * Gets the value of the attributes property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the attributes property.
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getAttributes().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link Attribute }
   */
  public List<Attribute> getAttributes() {
    if (attributes == null) {
      attributes = new ArrayList<Attribute>();
    }
    return this.attributes;
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

}
