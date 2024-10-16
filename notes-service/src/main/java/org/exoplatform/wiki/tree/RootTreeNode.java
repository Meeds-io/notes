/*
 * Copyright (C) 2003-2010 eXo Platform SAS.
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
package org.exoplatform.wiki.tree;

import java.util.Map;

public class RootTreeNode extends TreeNode {

  public RootTreeNode() {
    super();
  }

  @Override
  protected void addChildren(Map<String, Object> context, String userId) throws Exception {

    SpaceTreeNode portalNode = new SpaceTreeNode("portal");
    SpaceTreeNode groupNode = new SpaceTreeNode("group");
    SpaceTreeNode userNode = new SpaceTreeNode("user");
    this.children.add(portalNode);
    this.children.add(groupNode);
    this.children.add(userNode);
    super.addChildren(context, userId);
  }

}
