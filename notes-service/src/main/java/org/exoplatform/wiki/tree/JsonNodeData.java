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

import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.tree.utils.TreeUtils;

@Data
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

  private String             targetPageId;

  private Boolean hasDraftDescendant;


  private String             lang;
  
  private String             url;

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
    if (!this.children.isEmpty()) {
      this.isExpanded = true;
    }
    if (treeNode.getNodeType().equals(TreeNodeType.PAGE)) {
      Page page = ((PageTreeNode) treeNode).getPage();
      this.isDraftPage = page.isDraftPage();
      this.parentPageId = page.getParentPageId();
      this.lang = page.getLang();
      this.url = page.getUrl();
      this.lang = page.getLang();
      boolean withDrafts = context.containsKey(TreeNode.WITH_DRAFTS) && (boolean) context.get(TreeNode.WITH_DRAFTS);
      if (withDrafts) {
        this.disabled = !this.isDraftPage;
      }
      if (this.isDraftPage) {
        this.targetPageId = ((DraftPage) page).getTargetPageId();
      }
    }
  }

  public Boolean isHasDraftDescendant() {
    return hasDraftDescendant;
  }

  public void setHasDraftDescendant(Boolean hasDraftDescendant) {
    this.hasDraftDescendant = hasDraftDescendant;
  }

  public String getLang() {
    return lang;
  }

  public void setLang(String lang) {
    this.lang = lang;
  }

  public String getTargetPageId() {
    return targetPageId;
  }

  public void setTargetPageId(String targetPageId) {
    this.targetPageId = targetPageId;
  }
}
