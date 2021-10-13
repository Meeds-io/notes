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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.wiki.mow.api.Page;
import org.exoplatform.wiki.tree.utils.TreeUtils;

public class JsonNodeData {  

  protected String           name;

  protected String           noteId;

  protected String           path;

  protected String           currentPath;

  protected boolean          hasChild;

  protected TreeNodeType     nodeType;

  protected boolean          isDraftPage = false;

  protected boolean          disabled = false;

  protected boolean          isLastNode;

  protected boolean          isSelectable;
  
  protected boolean          isRestricted = false;

  protected boolean          isExpanded   = false;

  protected boolean          isSelected   = false;
  
  protected String           excerpt      = StringUtils.EMPTY;

  public static final String CURRENT_PATH = "currentPath";

  private List<JsonNodeData>  children;
  
  private String parentPageId;
  
  private Boolean hasDraftDescendant;
    
  public JsonNodeData(TreeNode treeNode,
                      boolean isLastNode,
                      boolean isSelectable,
                      String currentPath, String excerpt,
                      HashMap<String, Object> context) throws Exception {
    this.name = treeNode.getName();
    this.noteId = treeNode.getId();
    if (treeNode instanceof WikiHomeTreeNode){
      this.noteId = ((WikiHomeTreeNode) treeNode).getWikiHome().getId();
    }
    if(treeNode.getPath() != null) {
      this.path = URLEncoder.encode(treeNode.getPath(), "utf-8");
    }
    if (currentPath != null) {
      this.currentPath = URLEncoder.encode(currentPath, "utf-8");
    }
    this.hasChild = treeNode.isHasChild();
    this.nodeType = treeNode.getNodeType();
    this.isLastNode = isLastNode;
    this.isSelectable = isSelectable;
    this.excerpt = excerpt;
    this.children = TreeUtils.tranformToJson(treeNode, context);
    this.isSelected = treeNode.isSelected();
    this.isRestricted = treeNode.isRetricted;
    if (this.children.size() > 0) {
      this.isExpanded = true;
    }
    if (treeNode.getNodeType().equals(TreeNodeType.PAGE)) {
      Page page = ((PageTreeNode) treeNode).getPage();
      this.isDraftPage = page.isDraftPage();
      this.parentPageId = page.getParentPageId();
      boolean withDrafts = context.containsKey(TreeNode.WITH_DRAFTS) && (boolean) context.get(TreeNode.WITH_DRAFTS);
      if (withDrafts) {
        this.disabled = !this.isDraftPage;
      }
    }
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  } 

  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public boolean isHasChild() {
    return hasChild;
  }

  public void setHasChild(boolean hasChild) {
    this.hasChild = hasChild;
  }

  public TreeNodeType getNodeType() {
    return nodeType;
  }

  public void setNodeType(TreeNodeType nodeType) {
    this.nodeType = nodeType;
  }

  public boolean isLastNode() {
    return isLastNode;
  }

  public void setLastNode(boolean isLastNode) {
    this.isLastNode = isLastNode;
  }

  public boolean isSelectable() {
    return isSelectable;
  }

  public void setSelectable(boolean isSelectable) {
    this.isSelectable = isSelectable;
  }
  
  public boolean isRestricted() {
    return isRestricted;
  }
  
  public void setRestricted(boolean isRetricted) {
    this.isRestricted = isRetricted;
  }

  public String getCurrentPath() {
    return currentPath;
  }

  public void setCurrentPath(String currentPath) {
    this.currentPath = currentPath;
  }
  
  public boolean isExpanded() {
    return isExpanded;
  }

  public void setExpanded(boolean isExpanded) {
    this.isExpanded = isExpanded;
  }

  public boolean isSelected() {
    return isSelected;
  }

  public void setSelected(boolean isSelected) {
    this.isSelected = isSelected;
  }

  public String getExcerpt() {
    return excerpt;
  }

  public void setExcerpt(String excerpt) {
    this.excerpt = excerpt;
  }

  public List<JsonNodeData> getChildren() {
    return children;
  }

  public void setChildren(List<JsonNodeData> children) {
    this.children = children;
  }

  public String getParentPageId() {
    return parentPageId;
  }

  public void setParentPageId(String parentPageId) {
    this.parentPageId = parentPageId;
  }

  public String getNoteId() {
    return noteId;
  }

  public void setNoteId(String noteId) {
    this.noteId = noteId;
  }

  public boolean isDraftPage() {
    return isDraftPage;
  }

  public void setDraftPage(boolean draftPage) {
    isDraftPage = draftPage;
  }

  public boolean isDisabled() {
    return disabled;
  }

  public void setDisabled(boolean disabled) {
    this.disabled = disabled;
  }

  public Boolean isHasDraftDescendant() {
    return hasDraftDescendant;
  }

  public void setHasDraftDescendant(Boolean hasDraftDescendant) {
    this.hasDraftDescendant = hasDraftDescendant;
  }
}
