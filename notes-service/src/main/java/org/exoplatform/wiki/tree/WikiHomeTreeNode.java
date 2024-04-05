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
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.service.WikiService;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;
import org.exoplatform.wiki.utils.NoteConstants;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class WikiHomeTreeNode extends TreeNode {
  private static final Log log = ExoLogger.getLogger(WikiHomeTreeNode.class);

  private Page wikiHome;

  private NoteService noteService;

  private WikiService wikiService;
  
  private SpaceService spaceService;

  public WikiHomeTreeNode(Page wikiHome) throws Exception {
    super(wikiHome.getTitle(), TreeNodeType.WIKIHOME);

    noteService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NoteService.class);
    wikiService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(WikiService.class);
    spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);

    this.wikiHome = wikiHome;
    this.path = this.buildPath();
    this.hasChild = !wikiHome.isDraftPage() && !noteService.getChildrenNoteOf(wikiHome, true, true).isEmpty();
  }

  @Override
  protected void addChildren(HashMap<String, Object> context, String userId) throws Exception {
    boolean withDrafts = context.containsKey(TreeNode.WITH_DRAFTS) && (boolean) context.get(TreeNode.WITH_DRAFTS);
    Collection<Page> pages = noteService.getChildrenNoteOf(wikiHome, withDrafts, false);
    Iterator<Page> childPageIterator = pages.iterator();
    int count = 0;
    int size = getNumberOfChildren(context, pages.size());
    Page currentPage = (Page) context.get(TreeNode.SELECTED_PAGE);
    while (childPageIterator.hasNext() && count < size) {
      Page childPage = childPageIterator.next();
      if (noteService.hasPermissionOnPage(childPage, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())
              ||  (currentPage != null && Utils.isDescendantPage(currentPage, childPage))) {
        Space space = spaceService.getSpaceByGroupId(childPage.getWikiOwner());
        if (!childPage.isDraftPage() || Utils.canManageNotes(userId, space, childPage)) {
          PageTreeNode child = new PageTreeNode(childPage);
          this.children.add(child);
        }
      }
      count++;
    }
    super.addChildren(context, userId);
  }

  public Page getWikiHome() {
    return wikiHome;
  }

  public PageTreeNode getChildByName(String name) throws Exception {
    for (TreeNode child : children) {
      if (child.getName().equals(name))
        return (PageTreeNode)child;
    }
    return null;
  }

  public PageTreeNode findDescendantNodeByName(List<TreeNode> listPageTreeNode, String name) throws Exception {
    for (TreeNode pageTreeNode : listPageTreeNode) {
      if (pageTreeNode.getName().equals(name)) {
        return (PageTreeNode)pageTreeNode;
      } else {
        List<TreeNode> listChildPageTreeNode =  pageTreeNode.getChildren();
        if (listChildPageTreeNode.size() > 0) {
          return (PageTreeNode)findDescendantNodeByName(listChildPageTreeNode, name);
        }
      }
    }
    return null;
  }
  
  @Override
  public String buildPath() {
    try {
      Wiki wiki = wikiService.getWikiByTypeAndOwner(wikiHome.getWikiType(), wikiHome.getWikiOwner());
      WikiPageParams params = new WikiPageParams(wiki.getType(), wiki.getOwner(), NoteConstants.NOTE_HOME_NAME);
      return TreeUtils.getPathFromPageParams(params);
    } catch (Exception e) {
      log.error("Cannot build path of wiki page " + wikiHome.getWikiType() + ":" + wikiHome.getWikiOwner() + ":"
              + wikiHome.getName() + " - Cause : " + e.getMessage(), e);
      return null;
    }
  }

  public void setWikiHome(Page wikiHome) {
    this.wikiHome = wikiHome;
  }
}
