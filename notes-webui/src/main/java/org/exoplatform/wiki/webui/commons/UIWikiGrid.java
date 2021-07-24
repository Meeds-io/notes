/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.wiki.webui.commons;

import org.exoplatform.commons.serialization.api.annotations.Serialized;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.core.UIGrid;

@ComponentConfig(template = "app:/templates/wiki/webui/UIWikiGrid.gtmpl")
@Serialized
public class UIWikiGrid extends UIGrid {
  private String grid_mode = "Template";
  
  public static final String    TEMPLATE        = "Template";
  public static final String    SETTING         = "Setting";
  public void setUIGridMode(String mode) {
  	this.grid_mode = mode;
  }
  public String getUIGridMode() {
  	return this.grid_mode;
  }
  public UIWikiGrid() throws Exception {
    super();
  }
}
