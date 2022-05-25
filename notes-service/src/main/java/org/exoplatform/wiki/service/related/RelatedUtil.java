/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2022 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

package org.exoplatform.wiki.service.related;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;

public final class RelatedUtil {
  private RelatedUtil() {}
  
  public static List<JsonRelatedData> pageToJson(List<Page> pages) {
    List<JsonRelatedData> jsonObjs = new ArrayList<>();
    for (Page page : pages) {
      String name = page.getName();
      String title = page.getTitle();
      String path = TreeUtils.getPathFromPageParams(Utils.getWikiPageParams(page));
      JsonRelatedData dataObj = new JsonRelatedData(name, title, path);
      jsonObjs.add(dataObj);
    }
    return jsonObjs;
  }
  
  /**
   * convert wiki page info to path string. <br>
   * The format: [wiki type]/[wiki owner]/[page id]
   * @param params the wiki page params (type, owner and page name)
   * @return the page path
   */
  public static String getPath(WikiPageParams params) {
    StringBuilder sb = new StringBuilder();
    if (params.getType() != null) {
      sb.append(params.getType());
    }
    if (params.getOwner() != null) {
      sb.append("/").append(Utils.validateWikiOwner(params.getType(), params.getOwner()));
    }
    if (params.getPageName() != null) {
      sb.append("/").append(params.getPageName());
    }
    return sb.toString();
  }
  
  /**
   * get wiki page params from the path made by {@link #getPath(WikiPageParams)} 
   * @param path made by {@link #getPath(WikiPageParams)}
   * @throws Exception if an error occurs.
   * @return Wiki page params
   */
  public static WikiPageParams getPageParams(String path) throws Exception {
    return TreeUtils.getPageParamsFromPath(path);
  }
}
