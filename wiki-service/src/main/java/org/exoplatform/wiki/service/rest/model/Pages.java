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
    name = "",
    propOrder = {
        "pageSummaries"
    }
)
@XmlRootElement(name = "pages")
public class Pages
                   extends LinkCollection {

  @XmlElement(name = "pageSummary")
  protected List<PageSummary> pageSummaries;

  /**
   * Gets the value of the pageSummaries property.
   * <p>
   * This accessor method returns a reference to the live list, not a snapshot.
   * Therefore any modification you make to the returned list will be present
   * inside the JAXB object. This is why there is not a <CODE>set</CODE> method
   * for the pageSummaries property.
   * <p>
   * For example, to add a new item, do as follows:
   * 
   * <pre>
   * getPageSummaries().add(newItem);
   * </pre>
   * <p>
   * Objects of the following type(s) are allowed in the list {@link PageSummary
   * }
   */
  public List<PageSummary> getPageSummaries() {
    if (pageSummaries == null) {
      pageSummaries = new ArrayList<PageSummary>();
    }
    return this.pageSummaries;
  }

}
