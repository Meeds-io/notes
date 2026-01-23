/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 */
package org.exoplatform.wiki.storage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataItem;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.DraftPageEntity;
import org.exoplatform.wiki.jpa.entity.PageEntity;
import org.exoplatform.wiki.jpa.entity.PageVersionEntity;
import org.exoplatform.wiki.jpa.entity.WikiEntity;
import org.exoplatform.wiki.model.DraftPage;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.PageHistory;
import org.exoplatform.wiki.model.PageVersion;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.model.WikiPreferences;
import org.exoplatform.wiki.model.WikiPreferencesSyntax;
import org.exoplatform.wiki.service.impl.NoteServiceImpl;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NoteMetadataObject;
import io.meeds.notes.model.NotePageProperties;

/**
 * Utility class to convert JPA entity objects
 */
public class EntityConverter {

  private static SpaceService       spaceService;

  private static MetadataService    metadataService;

  public static final MetadataType  NOTES_METADATA_TYPE        = new MetadataType(1001, "notes");

  public static final MetadataKey   NOTES_METADATA_KEY         = new MetadataKey(NOTES_METADATA_TYPE.getName(),
                                                                                 Utils.NOTES_METADATA_OBJECT_TYPE,
                                                                                 0);

  private static final List<String> ORIGINAL_SHARED_PROPERTIES = List.of("hideReaction", "hideAuthor");

  private EntityConverter() {
    // Utils class
  }

  public static Wiki convertWikiEntityToWiki(WikiEntity wikiEntity) {
    Wiki wiki = null;
    if (wikiEntity != null) {
      wiki = new Wiki();
      wiki.setId(String.valueOf(wikiEntity.getId()));
      wiki.setType(wikiEntity.getType());
      wiki.setOwner(wikiEntity.getOwner());
      PageEntity wikiHomePageEntity = wikiEntity.getWikiHome();
      if (wikiHomePageEntity != null) {
        wiki.setWikiHome(convertPageEntityToPage(wikiHomePageEntity));
      }
      WikiPreferences wikiPreferences = new WikiPreferences();
      WikiPreferencesSyntax wikiPreferencesSyntax = new WikiPreferencesSyntax();
      wikiPreferencesSyntax.setDefaultSyntax(wikiEntity.getSyntax());
      wikiPreferencesSyntax.setAllowMultipleSyntaxes(wikiEntity.isAllowMultipleSyntax());
      wikiPreferences.setWikiPreferencesSyntax(wikiPreferencesSyntax);
      wiki.setPreferences(wikiPreferences);
    }
    return wiki;
  }

  public static WikiEntity convertWikiToWikiEntity(Wiki wiki, WikiDAO wikiDAO) {
    WikiEntity wikiEntity = null;
    if (wiki != null) {
      wikiEntity = new WikiEntity();
      wikiEntity.setType(wiki.getType());
      wikiEntity.setOwner(wiki.getOwner());
      wikiEntity.setWikiHome(convertPageToPageEntity(wiki.getWikiHome(), wikiDAO));
      WikiPreferences wikiPreferences = wiki.getPreferences();
      if(wikiPreferences != null) {
        WikiPreferencesSyntax wikiPreferencesSyntax = wikiPreferences.getWikiPreferencesSyntax();
        if(wikiPreferencesSyntax != null) {
          wikiEntity.setSyntax(wikiPreferencesSyntax.getDefaultSyntax());
          wikiEntity.setAllowMultipleSyntax(wikiPreferencesSyntax.isAllowMultipleSyntaxes());
        }
      }
    }
    return wikiEntity;
  }

  public static Page convertPageEntityToPage(PageEntity pageEntity) {
    Page page = null;
    if (pageEntity != null) {
      page = new Page();
      page.setId(String.valueOf(pageEntity.getId()));
      page.setName(pageEntity.getName());
      WikiEntity wiki = pageEntity.getWiki();
      if (wiki != null) {
        page.setWikiId(String.valueOf(wiki.getId()));
        page.setWikiType(wiki.getType());
        page.setWikiOwner(wiki.getOwner());
      }
      if (pageEntity.getParentPage() != null) {
        page.setParentPageId(String.valueOf(pageEntity.getParentPage().getId()));
        page.setParentPageName(pageEntity.getParentPage().getName());
      }
      page.setTitle(pageEntity.getTitle());
      page.setOwner(pageEntity.getOwner());
      page.setAuthor(pageEntity.getAuthor());
      page.setContent(pageEntity.getContent());
      page.setSyntax(pageEntity.getSyntax());
      page.setCreatedDate(pageEntity.getCreatedDate());
      page.setUpdatedDate(pageEntity.getUpdatedDate());
      page.setMinorEdit(pageEntity.isMinorEdit());
      page.setComment(pageEntity.getComment());
      page.setActivityId(pageEntity.getActivityId());
      page.setDeleted(pageEntity.isDeleted());
      page.setUrl(Utils.getPageUrl(page));
      buildNotePageMetadata(page, page.isDraftPage(), String.valueOf(pageEntity.getId()));
    }
    return page;
  }
  
  public static void buildNotePageMetadata(Page note, boolean isDraft, String pageId) {
    if (note == null) {
      return;
    }
    Space space = getSpaceService().getSpaceByGroupId(note.getWikiOwner());
    String spaceId = space != null ? space.getId() : "0";
    String noteId = note.getId();
    String metaDataType = isDraft ? "noteDraftPage" : "notePage";
    if (note instanceof PageHistory) {
      noteId = pageId + "-" + ((PageHistory) note).getVersionNumber();
      metaDataType = "noteVersionPage";
    }
    if (note.getLang() != null) {
      noteId = noteId + "-" + note.getLang();
    }
    Map<String, String> originalNoteSharedProperties = getOriginalNoteSharedProperties(note, spaceId);
    NoteMetadataObject noteMetadataObject = new NoteMetadataObject(metaDataType,
                                                                   noteId,
                                                                   note.getParentPageId(),
                                                                   Long.parseLong(spaceId));
    getMetadataService().getMetadataItemsByMetadataAndObject(NOTES_METADATA_KEY, noteMetadataObject)
                        .stream()
                        .findFirst()
                        .ifPresent(metadataItem -> {
                          if (!MapUtils.isEmpty(metadataItem.getProperties())) {
                            buildPageProperties(metadataItem.getProperties(), originalNoteSharedProperties, note);
                          }
                        });

  }

  public static PageEntity convertPageToPageEntity(Page page, WikiDAO wikiDAO) {
    PageEntity pageEntity = null;
    if (page != null) {
      pageEntity = new PageEntity();
      pageEntity.setName(page.getName());
      if (page.getWikiId() != null) {
        WikiEntity wiki = wikiDAO.find(Long.parseLong(page.getWikiId()));
        if (wiki != null) {
          pageEntity.setWiki(wiki);
        }
      }
      pageEntity.setTitle(page.getTitle());
      pageEntity.setOwner(page.getOwner());
      pageEntity.setAuthor(page.getAuthor());
      pageEntity.setContent(page.getContent());
      pageEntity.setSyntax(page.getSyntax());
      pageEntity.setCreatedDate(page.getCreatedDate());
      pageEntity.setUpdatedDate(page.getUpdatedDate());
      pageEntity.setMinorEdit(page.isMinorEdit());
      pageEntity.setComment(page.getComment());
      pageEntity.setUrl(page.getUrl());
      pageEntity.setActivityId(page.getActivityId());
    }
    return pageEntity;
  }

  public static List<DraftPage> convertDraftPageEntitiesToDraftPages(List<DraftPageEntity> draftPageEntities) {
    if (CollectionUtils.isEmpty(draftPageEntities)) {
      return new ArrayList<>();
    }
    return draftPageEntities.stream().map(EntityConverter::convertDraftPageEntityToDraftPage).toList();
  }
  
  public static DraftPage convertDraftPageEntityToDraftPage(DraftPageEntity draftPageEntity) {
    DraftPage draftPage = null;
    if (draftPageEntity != null) {
      draftPage = new DraftPage();
      draftPage.setId(String.valueOf(draftPageEntity.getId()));
      draftPage.setName(draftPageEntity.getName());

      // Oracle database treat empty string as NULL value. So we need to convert title to empty string
      String title = draftPageEntity.getTitle();
      draftPage.setTitle(title != null ? title : "");

      draftPage.setAuthor(draftPageEntity.getAuthor());
      draftPage.setOwner(draftPageEntity.getAuthor());
      draftPage.setContent(draftPageEntity.getContent());
      draftPage.setLang(draftPageEntity.getLang());
      draftPage.setSyntax(draftPageEntity.getSyntax());
      draftPage.setCreatedDate(draftPageEntity.getCreatedDate());
      draftPage.setUpdatedDate(draftPageEntity.getUpdatedDate());
      draftPage.setNewPage(draftPageEntity.isNewPage());
      PageEntity targetPage = draftPageEntity.getTargetPage();
      if (targetPage != null) {
        draftPage.setTargetPageId(String.valueOf(targetPage.getId()));
        draftPage.setTargetPageRevision(draftPageEntity.getTargetRevision());
        
        WikiEntity wiki = targetPage.getWiki();
        if (wiki != null) {
          draftPage.setWikiType(wiki.getType());
          draftPage.setWikiOwner(wiki.getOwner());
        }
      }
      PageEntity parentPage = draftPageEntity.getParentPage();
      if (parentPage != null) {
        draftPage.setParentPageId(String.valueOf(parentPage.getId()));
        draftPage.setParentPageName(parentPage.getName());
        if (StringUtils.isEmpty(draftPage.getWikiType()) || StringUtils.isEmpty(draftPage.getWikiOwner())) {
          WikiEntity wiki = parentPage.getWiki();
          draftPage.setWikiId(String.valueOf(wiki.getId()));
          draftPage.setWikiOwner(parentPage.getWiki().getOwner());
          draftPage.setWikiType(wiki.getType());
        }
      }
      buildNotePageMetadata(draftPage, true, String.valueOf(draftPageEntity.getId()));
    }
    return draftPage;
  }

  public static DraftPageEntity convertDraftPageToDraftPageEntity(DraftPage draftPage, PageDAO pageDAO) {
    DraftPageEntity draftPageEntity = null;
    if (draftPage != null) {
      draftPageEntity = new DraftPageEntity();
      if (StringUtils.isNotEmpty(draftPage.getId())) {
        draftPageEntity.setId(Long.parseLong(draftPage.getId()));
      }
      draftPageEntity.setName(draftPage.getName());
      draftPageEntity.setTitle(draftPage.getTitle());
      draftPageEntity.setAuthor(draftPage.getAuthor());
      draftPageEntity.setContent(draftPage.getContent());
      draftPageEntity.setLang(draftPage.getLang());
      draftPageEntity.setSyntax(draftPage.getSyntax());
      draftPageEntity.setCreatedDate(draftPage.getCreatedDate());
      draftPageEntity.setUpdatedDate(draftPage.getUpdatedDate());
      draftPageEntity.setNewPage(draftPage.isNewPage());
      String targetPageId = draftPage.getTargetPageId();
      if (StringUtils.isNotEmpty(targetPageId)) {
        draftPageEntity.setTargetPage(pageDAO.find(Long.valueOf(targetPageId)));
      }
      String parentPageId = draftPage.getParentPageId();
      if (StringUtils.isNotEmpty(parentPageId)) {
        draftPageEntity.setParentPage(pageDAO.find(Long.valueOf(parentPageId)));
      }
      draftPageEntity.setTargetRevision(draftPage.getTargetPageRevision());
      buildNotePageMetadata(draftPage, true, draftPage.getId());
    }
    return draftPageEntity;
  }

  public static PageVersion convertPageVersionEntityToPageVersion(PageVersionEntity pageVersionEntity) {
    PageVersion pageVersion = null;
    if (pageVersionEntity != null) {
      pageVersion = new PageVersion();
      pageVersion.setId(String.valueOf(pageVersionEntity.getId()));
      pageVersion.setName(String.valueOf(pageVersionEntity.getVersionNumber()));
      pageVersion.setTitle(pageVersionEntity.getTitle());
      pageVersion.setAuthor(pageVersionEntity.getAuthor());
      pageVersion.setComment(pageVersionEntity.getComment());
      pageVersion.setContent(pageVersionEntity.getContent());
      pageVersion.setCreatedDate(pageVersionEntity.getCreatedDate());
      pageVersion.setUpdatedDate(pageVersionEntity.getUpdatedDate());
      pageVersion.setOwner(pageVersionEntity.getAuthor());
      pageVersion.setParent(convertPageEntityToPage(pageVersionEntity.getPage()));
      pageVersion.setLang(pageVersionEntity.getLang());
      pageVersion.setWikiOwner(pageVersionEntity.getPage().getWiki().getOwner());
      buildNotePageMetadata(pageVersion, pageVersion.isDraftPage(), String.valueOf(pageVersionEntity.getPage().getId()));
    }
    return pageVersion;
  }

  public static PageHistory convertPageVersionEntityToPageHistory(PageVersionEntity pageVersionEntity) {
    PageHistory pageHistory = null;
    if (pageVersionEntity != null) {
      pageHistory = new PageHistory();
      pageHistory.setId(String.valueOf(pageVersionEntity.getId()));
      pageHistory.setVersionNumber(pageVersionEntity.getVersionNumber());
      pageHistory.setAuthor(pageVersionEntity.getAuthor());
      pageHistory.setContent(pageVersionEntity.getContent());
      pageHistory.setCreatedDate(pageVersionEntity.getCreatedDate());
      pageHistory.setUpdatedDate(pageVersionEntity.getUpdatedDate());
      pageHistory.setLang(pageVersionEntity.getLang());
      pageHistory.setTitle(pageVersionEntity.getTitle());
      buildNotePageMetadata(pageHistory, false, String.valueOf(pageVersionEntity.getPage().getId()));
      if (StringUtils.isNotBlank(pageHistory.getAuthor())) {
        Identity identity = ExoContainerContext.getService(IdentityManager.class)
            .getOrCreateUserIdentity(pageHistory.getAuthor());
        if (identity != null
            && identity.getProfile() != null
            && identity.getProfile().getFullName() != null) {
          pageHistory.setAuthorFullName(identity.getProfile().getFullName());
        }
      }
    }
    return pageHistory;
  }

  public static List<PageHistory> toPageHistoryVersions(List<PageVersionEntity> pageVersionEntities) {
    return pageVersionEntities.stream()
                              .map(EntityConverter::convertPageVersionEntityToPageHistory)
                              .toList();
  }

  public static List<DraftPage> toDraftPages(List<DraftPageEntity> draftPageEntities) {
    return draftPageEntities.stream().map(EntityConverter::convertDraftPageEntityToDraftPage).toList();
  }

  private static Map<String, String> getOriginalNoteSharedProperties(Page note, String spaceId) {
    if (note.getLang() == null || note.isDraftPage()) {
      return new HashMap<>();
    }
    NoteMetadataObject originalNoteMetadataObject = new NoteMetadataObject("notePage",
                                                                           note.getId(),
                                                                           note.getParentPageId(),
                                                                           Long.parseLong(spaceId));
    List<MetadataItem> metadataItems = getMetadataService().getMetadataItemsByMetadataAndObject(NOTES_METADATA_KEY,
                                                                                                originalNoteMetadataObject);
    Map<String, String> originalNoteSharedProperties = new HashMap<>();
    if (!CollectionUtils.isEmpty(metadataItems)) {
      originalNoteSharedProperties = metadataItems.getFirst().getProperties();
    }
    return originalNoteSharedProperties != null ? originalNoteSharedProperties.entrySet()
                                                                              .stream()
                                                                              .filter(entry -> ORIGINAL_SHARED_PROPERTIES.contains(entry.getKey()))
                                                                              .collect(Collectors.toMap(Map.Entry::getKey,
                                                                                                        Map.Entry::getValue))
                                                : new HashMap<>();
  }

  private static void buildPageProperties(Map<String, String> properties,
                                          Map<String, String> originalNoteSharedProperties,
                                          Page note) {
    Map<String, String> finalProperties = new HashMap<>(properties);
    finalProperties.putAll(originalNoteSharedProperties);
    NotePageProperties notePageProperties = new NotePageProperties();
    NoteFeaturedImage noteFeaturedImage = new NoteFeaturedImage();
    notePageProperties.setNoteId(Long.parseLong(note.getId()));
    notePageProperties.setSummary(finalProperties.get(NoteServiceImpl.SUMMARY_PROP));
    notePageProperties.setHideAuthor(Boolean.parseBoolean(finalProperties.getOrDefault(NoteServiceImpl.HIDE_AUTHOR_PROP,
                                                                                       "false")));
    notePageProperties.setHideReaction(Boolean.parseBoolean(properties.getOrDefault(NoteServiceImpl.HIDE_REACTION_PROP,
                                                                                    "false")));
    noteFeaturedImage.setId(Long.valueOf(finalProperties.getOrDefault(NoteServiceImpl.FEATURED_IMAGE_ID, "0")));
    noteFeaturedImage.setLastUpdated(Long.valueOf(finalProperties.getOrDefault(NoteServiceImpl.FEATURED_IMAGE_UPDATED_DATE,
                                                                               "0")));
    noteFeaturedImage.setAltText(finalProperties.get(NoteServiceImpl.FEATURED_IMAGE_ALT_TEXT));
    notePageProperties.setDraft(note.isDraftPage());
    notePageProperties.setFeaturedImage(noteFeaturedImage);
    note.setProperties(notePageProperties);
  }

  private static SpaceService getSpaceService() {
    if (spaceService == null) {
      spaceService = CommonsUtils.getService(SpaceService.class);
    }
    return spaceService;
  }

  private static MetadataService getMetadataService() {
    if (metadataService == null) {
      metadataService = CommonsUtils.getService(MetadataService.class);
    }
    return metadataService;
  }

}
