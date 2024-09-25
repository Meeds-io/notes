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

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.localization.LocaleContextInfoUtils;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PermissionType;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.NoteService;
import org.exoplatform.wiki.service.WikiPageParams;
import org.exoplatform.wiki.tree.*;
import org.exoplatform.wiki.utils.NoteConstants;
import org.exoplatform.wiki.utils.Utils;

public class TreeUtils {
  
  private static ResourceBundleService resourceBundleService;

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
  public static TreeNode getDescendants(WikiPageParams params, Map<String, Object> context, String userId) throws Exception {
    TreeNode treeNode = getTreeNode(params);
    treeNode.pushDescendants(context, userId);
    return treeNode;
  }
  
  public static List<JsonNodeData> tranformToJson(TreeNode treeNode,
                                                  Map<String, Object> context,
                                                  Identity identity,
                                                  Locale locale) throws Exception {
    NoteService noteService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(NoteService.class);

    int counter = 1;
    Boolean showExcerpt = false;
    Page currentPage = null;
    String currentPath = null;
    Boolean canEdit = false;
    if (context != null) {
      currentPath = (String) context.get(TreeNode.CURRENT_PATH);
      currentPage = (Page) context.get(TreeNode.CURRENT_PAGE);
      showExcerpt = (Boolean) context.get(TreeNode.SHOW_EXCERPT);
      canEdit = (Boolean)context.get(TreeNode.CAN_EDIT);
    }
    
    List<JsonNodeData> children = new ArrayList<>();
    for (TreeNode child : treeNode.getChildren()) {
      boolean isLastNode = counter >= treeNode.getChildren().size();
      JsonNodeData jsonNodeData = toJsonNodeData(child,
                                                 currentPath,
                                                 currentPage,
                                                 context,
                                                 canEdit,
                                                 isLastNode,
                                                 identity,
                                                 locale,
                                                 noteService);
      encodeWikiTreeNode(jsonNodeData, locale, identity, noteService);
      String excerpt = null;
      if (showExcerpt != null && showExcerpt) {
        WikiPageParams params = getPageParamsFromPath(child.getPath());
        // FIXME Migration - Remove excerpt support ?
        // excerpt = ExcerptUtils.getExcerpts(params);
        excerpt = "";
      }
      jsonNodeData.setExcerpt(excerpt);
      children.add(jsonNodeData);
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
    Map<String, List<JsonNodeData>> childrenByTarget = children.stream()
                                                               .filter(jsonNodeData -> jsonNodeData.getTargetPageId() != null)
                                                               .collect(Collectors.groupingBy(JsonNodeData::getTargetPageId));

    List<JsonNodeData> cleanedChildren = children.stream()
                                                 .filter(jsonNodeData -> !jsonNodeData.isDraftPage()
                                                     || StringUtils.isEmpty(jsonNodeData.getTargetPageId()))
                                                 .collect(Collectors.toList());

    for (Map.Entry<String, List<JsonNodeData>> entry : childrenByTarget.entrySet()) {
      String target = entry.getKey();
      List<JsonNodeData> subJsonNodeDataList = entry.getValue();

      if (StringUtils.isNotEmpty(target) && subJsonNodeDataList.size() > 1) {
        JsonNodeData currentLangDraft = null;
        JsonNodeData originalLangDraft = null;
        JsonNodeData anyLangDraft = null;

        for (JsonNodeData nodeData : subJsonNodeDataList) {
          if (StringUtils.isEmpty(nodeData.getLang())) {
            originalLangDraft = nodeData;
          } else if (nodeData.getLang().equals(locale.getLanguage())) {
            currentLangDraft = nodeData;
          } else if (anyLangDraft == null
              || getLocatedLangDisplayName(localesList,
                                           locale,
                                           nodeData.getLang()).compareToIgnoreCase(getLocatedLangDisplayName(localesList, locale, anyLangDraft.getLang())) < 0) {
            anyLangDraft = nodeData;
          }
        }

        cleanedChildren.add(Optional.ofNullable(currentLangDraft)
                                    .orElse(Optional.ofNullable(originalLangDraft).orElse(anyLangDraft)));
      } else {
        cleanedChildren.addAll(subJsonNodeDataList);
      }
    }
    return cleanedChildren;
  }

  public static String getLocatedLangDisplayName(List<Locale> localesList, Locale currentLocale, String lang) {
    for (Locale locale : localesList) {
      if (locale.getLanguage().equals(lang)) {
        return locale.getDisplayName(currentLocale);
      }
    }
    return lang;
  }
  
  public static JsonNodeData toJsonNodeData(TreeNode node,
                                            String path,
                                            Page currentPage,
                                            Map<String, Object> context,
                                            Boolean canEdit,
                                            Boolean isLastNode,
                                            Identity identity,
                                            Locale locale,
                                            NoteService noteService) throws Exception {
    boolean isSelectable = true;
    if (node.getNodeType().equals(TreeNodeType.WIKI)) {
      isSelectable = false;
    } else if (node.getNodeType().equals(TreeNodeType.PAGE)) {
      Page page = ((PageTreeNode) node).getPage();
      if (((currentPage != null) && (currentPage.equals(page) || Utils.isDescendantPage(page, currentPage)))) {
        isSelectable = false;
      }

      if (!noteService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
        isSelectable = false;
        node.setRetricted(true);
      }
      if (BooleanUtils.isTrue(canEdit)
          && !noteService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())) {
        isSelectable = false;
        node.setRetricted(true);
      }
    } else if (node.getNodeType().equals(TreeNodeType.WIKIHOME)) {
      Page page = ((WikiHomeTreeNode) node).getWikiHome();
      if (!noteService.hasPermissionOnPage(page, PermissionType.VIEWPAGE, ConversationState.getCurrent().getIdentity())) {
        isSelectable = false;
        node.setRetricted(true);
      }

      if (BooleanUtils.isTrue(canEdit)
          && !noteService.hasPermissionOnPage(page, PermissionType.EDITPAGE, ConversationState.getCurrent().getIdentity())) {
        isSelectable = false;
        node.setRetricted(true);
      }

    }
    JsonNodeData jsonNodeData = new JsonNodeData(node, isLastNode, isSelectable, path, "", context, identity, locale);
    encodeWikiTreeNode(jsonNodeData, locale, identity, noteService);
    return jsonNodeData;
  }

  private static void encodeWikiTreeNode(JsonNodeData nodeData, Locale locale, Identity identity, NoteService noteService) throws Exception {
    ResourceBundle resourceBundle = getResourceBundleService().getResourceBundle(Utils.WIKI_RESOUCE_BUNDLE_NAME, locale);
    String untitledLabel = resourceBundle.getString("Page.Untitled");
    if (StringUtils.isBlank(nodeData.getName())) {
      nodeData.setName(untitledLabel);
    } else {
      if (!nodeData.isDraftPage()) {
        Page page = noteService.getNoteByIdAndLang(Long.valueOf(nodeData.getNoteId()), identity, "", locale.getLanguage());
        if (page != null) {
          nodeData.setName(page.getTitle());
        }
      }
    }
  }

  private static ResourceBundleService getResourceBundleService() {
    if (resourceBundleService == null) {
      resourceBundleService = ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(ResourceBundleService.class);
    }
    return resourceBundleService;
  }
}
