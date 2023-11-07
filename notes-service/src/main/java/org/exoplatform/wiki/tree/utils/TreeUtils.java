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

package org.exoplatform.wiki.tree.utils;

import java.util.*;
import java.util.stream.Collectors;

import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.*;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

public class TreeUtils {
  
  /**
   * Create a tree node with a given {@link WikiPageParams}
   * 
   * @param params is the wiki page parameters
   * @return <code>TreeNode</code>
   * @throws Exception if an error occured
   */
  public static TreeNode getTreeNode(WikiPageParams params) throws Exception {
    Object wikiObject = Utils.getObjectFromParams(params);
    if (wikiObject instanceof Page) {
      Page page = (Page) wikiObject;
      if(params.getPageName().equals(NoteConstants.NOTE_HOME_NAME)) {
        WikiHomeTreeNode wikiHomeNode = new WikiHomeTreeNode(page);
        return wikiHomeNode;
      } else {
        PageTreeNode pageNode = new PageTreeNode(page);
        return pageNode;
      }
    } else if (wikiObject instanceof Wiki) {
      Wiki wiki = (Wiki) wikiObject;
      WikiTreeNode wikiNode = new WikiTreeNode(wiki);
      return wikiNode;
    } else if (wikiObject instanceof String) {
      SpaceTreeNode spaceNode = new SpaceTreeNode((String) wikiObject);
      return spaceNode;
    }
    return new TreeNode();
  }
  
  /**
   * Create a tree node contain all its descendant with a given {@link WikiPageParams} 
   * 
   * @param params is the wiki page parameters
   * @param context is the page tree context
   * @param userId
   * @return <code>TreeNode</code>
   * @throws Exception if error occured
   */
  public static TreeNode getDescendants(WikiPageParams params, HashMap<String, Object> context, String userId) throws Exception {
    TreeNode treeNode = getTreeNode(params);
    treeNode.pushDescendants(context, userId);
    return treeNode;
  }
  
  public static List<JsonNodeData> tranformToJson(TreeNode treeNode, HashMap<String, Object> context) throws Exception {
    NoteService noteService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NoteService.class);

    int counter = 1;
    Boolean showExcerpt = false;
    Page currentPage = null;
    String currentPath = null;
    Boolean canEdit      = false;
    if (context != null) {
      currentPath = (String) context.get(TreeNode.CURRENT_PATH);
      currentPage = (Page) context.get(TreeNode.CURRENT_PAGE);
      showExcerpt = (Boolean) context.get(TreeNode.SHOW_EXCERPT);
      canEdit     = (Boolean)context.get(TreeNode.CAN_EDIT);
    }
    
    List<JsonNodeData> children = new ArrayList<JsonNodeData>();
    for (TreeNode child : treeNode.getChildren()) {
      boolean isSelectable = true;
      boolean isLastNode = false;
      if (counter >= treeNode.getChildren().size()) {
        isLastNode = true;
      }
      
      if (child.getNodeType().equals(TreeNodeType.WIKI)) {
        isSelectable = false;
      } else if (child.getNodeType().equals(TreeNodeType.PAGE)) {
        Page page = ((PageTreeNode) child).getPage();
        if (((currentPage != null) && (currentPage.equals(page) || Utils.isDescendantPage(page, currentPage)))) {
          isSelectable = false;
        }
        
        if (!noteService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
          isSelectable = false;
          child.setRetricted(true);
        }
        if(BooleanUtils.isTrue(canEdit) && !noteService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())){
          isSelectable = false;
          child.setRetricted(true);
        }
      } else if (child.getNodeType().equals(TreeNodeType.WIKIHOME)) {
        Page page = ((WikiHomeTreeNode) child).getWikiHome();
        if (!noteService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
          isSelectable = false;
          child.setRetricted(true);
        }

        if(BooleanUtils.isTrue(canEdit) && !noteService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())){
          isSelectable = false;
          child.setRetricted(true);
        }

      }
      
      String excerpt = null;
      if (showExcerpt != null && showExcerpt) {
        WikiPageParams params = getPageParamsFromPath(child.getPath());
        // FIXME Migration - Remove excerpt support ?
        //excerpt = ExcerptUtils.getExcerpts(params);
        excerpt = "";
      }
      
      children.add(new JsonNodeData(child, isLastNode, isSelectable, currentPath, excerpt, context));
      counter++;
    }
    return children;
  }
  
  public static WikiPageParams getPageParamsFromPath(String path) throws Exception {
    if (path == null) {
      return null;
    }
    WikiPageParams result = new WikiPageParams();
    path = path.trim();
    if (path.indexOf("/") < 0) {
      result.setType(path);
    } else {
      String[] array = path.split("/");
      result.setType(array[0]);
      if (array.length < 3) {
        result.setOwner(array[1]);
      } else if (array.length >= 3) {
        if (array[0].equals(PortalConfig.GROUP_TYPE)) {
          OrganizationService oService = (OrganizationService) ExoContainerContext.getCurrentContainer()
                                                                                  .getComponentInstanceOfType(OrganizationService.class);
          String groupId = path.substring(path.indexOf("/"));
          if (oService.getGroupHandler().findGroupById(groupId) != null) {
            result.setOwner(groupId);
          } else {
            String pageName = path.substring(path.lastIndexOf("/") + 1);
            if(StringUtils.isBlank(pageName)) {
              pageName = WikiPageParams.WIKI_HOME;
            }
            result.setPageName(pageName);
            String owner = path.substring(path.indexOf("/"), path.lastIndexOf("/"));
            while (oService.getGroupHandler().findGroupById(owner) == null) {
              owner = owner.substring(0,owner.lastIndexOf("/"));
            }
            result.setOwner(owner);
          }
        } else {
          // if (array[0].equals(PortalConfig.PORTAL_TYPE) || array[0].equals(PortalConfig.USER_TYPE))
          result.setOwner(array[1]);
          result.setPageName(array[array.length - 1]);
        }
      }
    }
    return result;
  }
 
  public static String getPathFromPageParams(WikiPageParams param) {
    StringBuilder sb = new StringBuilder();
    if (param.getType() != null) {
      sb.append(param.getType());
    }
    if (param.getOwner() != null) {
      sb.append("/").append(Utils.validateWikiOwner(param.getType(), param.getOwner()));
    }
    if (param.getPageName() != null) {
      sb.append("/").append(param.getPageName());
    }
    return sb.toString();
  }

  public static List<JsonNodeData> cleanDraftChildren(List<JsonNodeData> children, Locale locale) {
    List<Locale> localesList = new ArrayList<>(LocaleContextInfoUtils.getSupportedLocales());
    List<String> targetList = children.stream().map(JsonNodeData::getTargetPageId).distinct().collect(Collectors.toList());
    List<JsonNodeData> cleanedChildren = children.stream()
                                                 .filter(jsonNodeData -> !jsonNodeData.isDraftPage())
            .collect(Collectors.toList());
    for (String target : targetList) {
      if (StringUtils.isNotEmpty(target)) {
        List<JsonNodeData> subJsonNodeDataList =
                                               children.stream()
                                                       .filter(jsonNodeData -> StringUtils.equals(jsonNodeData.getTargetPageId(),
                                                                                                  target))
                                                       .collect(Collectors.toList());
        if (subJsonNodeDataList.size() > 1) {
          JsonNodeData currentLangDraft = null;
          JsonNodeData originalLangDraft = null;
          JsonNodeData anyLangDraft = null;
          List<JsonNodeData> subCleanedChildren = new ArrayList<>();
          for (JsonNodeData nodeData : subJsonNodeDataList) {
            if (StringUtils.isEmpty(nodeData.getLang())) {
              originalLangDraft = nodeData;
            } else if (nodeData.getLang().equals(locale.getLanguage())) {
              currentLangDraft = nodeData;
            } else if (anyLangDraft == null || getLocatedLangDisplayName(localesList, locale, nodeData.getLang()).compareToIgnoreCase(getLocatedLangDisplayName(localesList, locale, anyLangDraft.getLang())) < 0) {
              anyLangDraft = nodeData;
            }
          }
          if (currentLangDraft != null) {
            subCleanedChildren.add(currentLangDraft);
          } else if (originalLangDraft != null) {
            subCleanedChildren.add(originalLangDraft);
          } else {
            subCleanedChildren.add(anyLangDraft);
          }
          cleanedChildren.addAll(subCleanedChildren);
        } else {
          cleanedChildren.addAll(subJsonNodeDataList);
        }
      }
    }
    return cleanedChildren;
  }

  public static String getLocatedLangDisplayName(List<Locale> localesList, Locale currentLocale, String lang) {
    Optional<Locale> opLocal = localesList.stream().filter(local -> local.getLanguage().equals(lang)).findAny();
    if (opLocal.isPresent()) {
      return opLocal.get().getDisplayName(currentLocale);
    }
    return lang;
  }
}
