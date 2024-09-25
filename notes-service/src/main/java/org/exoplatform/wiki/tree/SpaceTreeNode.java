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

package org.exoplatform.wiki.tree;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiType;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.utils.TreeUtils;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

public class SpaceTreeNode extends TreeNode {

  private WikiService wikiService;

  public SpaceTreeNode(String name) throws Exception {
    super(name, TreeNodeType.SPACE);

    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);

    this.path = buildPath();

    try {
      WikiType wikiType = WikiType.valueOf(name.toUpperCase());
      this.hasChild = wikiService.getWikisByType(wikiType.toString()).size() > 0;
    } catch (IllegalArgumentException ex) {
      this.hasChild = false;
    }
  }  
  
  @Override
  protected void addChildren(Map<String, Object> context, String userId) throws Exception {
    try {
      WikiType wikiType = WikiType.valueOf(name.toUpperCase());
      Collection<Wiki> wikis = wikiService.getWikisByType(wikiType.toString());
      Iterator<Wiki> childWikiIterator = wikis.iterator();
      int count = 0;
      int size = getNumberOfChildren(context, wikis.size());
      while (childWikiIterator.hasNext() && count < size) {
        WikiTreeNode child = new WikiTreeNode(childWikiIterator.next());
        this.children.add(child);
        count++;
      }
      super.addChildren(context, userId);
    } catch (IllegalArgumentException ex) {
      this.hasChild = false;
    }
  }

  public WikiTreeNode getChildByName(String name) throws Exception {
    for (TreeNode child : children) {
      if (child.getName().equals(name))
        return (WikiTreeNode)child;
    }
    return null;
  }
  
  @Override
  public String buildPath() {
    return TreeUtils.getPathFromPageParams(new WikiPageParams(this.name, null, null));
  }
}
