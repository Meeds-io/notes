 /**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.wiki.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.utils.TreeUtils;
import org.exoplatform.wiki.utils.Utils;

public class PageTreeNode extends TreeNode {
  private static final Log log = ExoLogger.getLogger(PageTreeNode.class);

  @Setter
  @Getter
  private Page page;

  private NoteService noteService;
  
  private SpaceService spaceService;

  public PageTreeNode(Page page) throws Exception {
    super(page.getTitle(), TreeNodeType.PAGE);

    this.noteService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NoteService.class);
    
    this.spaceService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(SpaceService.class);

    this.page = page;
    this.id = page.getId();
    this.path = buildPath();
    this.hasChild = !page.isDraftPage() && noteService.hasChildren(Long.parseLong(page.getId()));
  }

  @Override
  protected void addChildren(Map<String, Object> context, String userId) throws Exception {
    boolean withDrafts = context.containsKey(TreeNode.WITH_DRAFTS) && (boolean) context.get(TreeNode.WITH_DRAFTS);
    Collection<Page> pages = noteService.getChildrenNoteOf(page, withDrafts, false);
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

  public PageTreeNode getChildByName(String name) throws Exception {
    for (TreeNode child : children) {
      if (child.getName().equals(name))
        return (PageTreeNode) child;
    }
    return null;
  }
  
  @Override
  public String buildPath() {
    try {
      WikiPageParams params = new WikiPageParams(page.getWikiType(), page.getWikiOwner(), page.getName());
      return TreeUtils.getPathFromPageParams(params);
    } catch (Exception e) {
      log.error("Cannot build path of wiki page " + page.getWikiType() + ":" + page.getWikiOwner() + ":"
              + page.getName() + " - Cause : " + e.getMessage(), e);
      return null;
    }
  }

}
