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

import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.utils.TreeUtils;

import java.util.Map;

public class WikiTreeNode extends TreeNode {
  private Wiki wiki;

  public WikiTreeNode(Wiki wiki) throws Exception {
    super(wiki.getOwner(), TreeNodeType.WIKI);
    this.wiki = wiki;
    this.path = buildPath();
    this.hasChild = true;
  }

  public WikiHomeTreeNode getWikiHomeTreeNode() {
    return (WikiHomeTreeNode) children.get(0);
  }

  @Override
  protected void addChildren(Map<String, Object> context, String userId) throws Exception {

    this.children.add(new WikiHomeTreeNode(wiki.getWikiHome()));
    super.addChildren(context, userId);
  }

  public Wiki getWiki() {
    return wiki;
  }

  public void setWiki(Wiki wiki) {
    this.wiki = wiki;
  }
  
  @Override
  public String buildPath() { 
    WikiPageParams params = new WikiPageParams(wiki.getType(), wiki.getOwner(), null);
    return TreeUtils.getPathFromPageParams(params);
  }
}
