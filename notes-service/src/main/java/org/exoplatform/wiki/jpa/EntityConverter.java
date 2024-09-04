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

package org.exoplatform.wiki.jpa;

import io.meeds.notes.model.NoteFeaturedImage;
import io.meeds.notes.model.NoteMetadataObject;
import io.meeds.notes.model.NotePageProperties;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.exoplatform.commons.file.model.FileInfo;
import org.exoplatform.commons.file.model.FileItem;
import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.social.core.identity.model.Identity;
import org.exoplatform.social.core.identity.provider.OrganizationIdentityProvider;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.social.metadata.MetadataService;
import org.exoplatform.social.metadata.model.MetadataKey;
import org.exoplatform.social.metadata.model.MetadataType;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.jpa.dao.PageDAO;
import org.exoplatform.wiki.jpa.dao.WikiDAO;
import org.exoplatform.wiki.jpa.entity.*;
import org.exoplatform.wiki.model.*;
import org.exoplatform.wiki.service.IDType;
import org.exoplatform.wiki.service.impl.NoteServiceImpl;
import org.exoplatform.wiki.utils.Utils;

import java.io.ByteArrayInputStream;
import java.util.*;

/**
 * Utility class to convert JPA entity objects
 */
public class EntityConverter {

  private static final Log       LOG = ExoLogger.getLogger(EntityConverter.class);

  private static SpaceService    spaceService;

  private static MetadataService metadataService;

  public static final MetadataType NOTES_METADATA_TYPE = new MetadataType(1001, "notes");

  public static final MetadataKey  NOTES_METADATA_KEY   = new MetadataKey(NOTES_METADATA_TYPE.getName(),
                                                                         Utils.NOTES_METADATA_OBJECT_TYPE,
                                                                         0);

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
      wiki.setPermissions(convertPermissionEntitiesToPermissionEntries(wikiEntity.getPermissions(),
              Arrays.asList(PermissionType.ADMINPAGE, PermissionType.ADMINSPACE)));
      // wiki.setDefaultPermissionsInited();
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
      wikiEntity.setPermissions(convertPermissionEntriesToPermissionEntities(wiki.getPermissions()));
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
      page.setPermissions(convertPermissionEntitiesToPermissionEntries(pageEntity.getPermissions(),
              Arrays.asList(PermissionType.VIEWPAGE, PermissionType.EDITPAGE)));
      page.setActivityId(pageEntity.getActivityId());
      page.setDeleted(pageEntity.isDeleted());
      page.setUrl(Utils.getPageUrl(page));
      buildNotePageMetadata(page, page.isDraftPage());
    }
    return page;
  }

  public static void buildNotePageMetadata(Page note, boolean isDraft) {
    if (note == null) {
      return;
    }
    Space space = getSpaceService().getSpaceByGroupId(note.getWikiOwner());
    if (space != null) {
      String noteId = note.getId();
      if (note.getLang() != null) {
        noteId = noteId + "-" + note.getLang();
      }
      NoteMetadataObject noteMetadataObject = new NoteMetadataObject(isDraft ? "noteDraftPage" : "notePage",
                                                                     noteId,
                                                                     note.getParentPageId(),
                                                                     Long.parseLong(space.getId()));
      getMetadataService().getMetadataItemsByObject(noteMetadataObject)
                          .stream()
                          .findFirst()
                          .ifPresent(metadataItem -> {
                            if (!MapUtils.isEmpty(metadataItem.getProperties())) {
                              buildPageProperties(metadataItem.getProperties(), note);
                            }
                          });

    }
  }
  
  private static void buildPageProperties(Map<String, String> properties, Page note) {
    NotePageProperties notePageProperties = new NotePageProperties();
    NoteFeaturedImage noteFeaturedImage = new NoteFeaturedImage();
    notePageProperties.setNoteId(Long.parseLong(note.getId()));
    notePageProperties.setSummary(properties.get(NoteServiceImpl.SUMMARY_PROP));
    noteFeaturedImage.setId(Long.valueOf(properties.getOrDefault(NoteServiceImpl.FEATURED_IMAGE_ID, "0")));
    noteFeaturedImage.setLastUpdated(Long.valueOf(properties.getOrDefault(NoteServiceImpl.FEATURED_IMAGE_UPDATED_DATE, "0")));
    noteFeaturedImage.setAltText(properties.get(NoteServiceImpl.FEATURED_IMAGE_ALT_TEXT));
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
  
  public static List<PermissionEntry> convertPermissionEntitiesToPermissionEntries(List<PermissionEntity> permissionEntities,
                                                                                   List<PermissionType> filteredPermissionTypes) {
    List<PermissionEntry> permissionEntries = new ArrayList<>();
    if(permissionEntities != null) {
      // we fill a map to prevent duplicated entries
      Map<String, PermissionEntry> permissionEntriesMap = new HashMap<>();
      for(PermissionEntity permissionEntity : permissionEntities) {
        // only permission types relevant for pages are used
        if(filteredPermissionTypes.contains(permissionEntity.getPermissionType())) {
          Permission newPermission = new Permission(permissionEntity.getPermissionType(), true);
          if (permissionEntriesMap.get(permissionEntity.getIdentity()) != null) {
            PermissionEntry permissionEntry = permissionEntriesMap.get(permissionEntity.getIdentity());
            Permission[] permissions = permissionEntry.getPermissions();
            // add the new permission only if it does not exist yet
            if (!ArrayUtils.contains(permissions, newPermission)) {
              permissionEntry.setPermissions((Permission[]) ArrayUtils.add(permissions,
                      newPermission));
              permissionEntriesMap.put(permissionEntity.getIdentity(), permissionEntry);
            }
          } else {
            permissionEntriesMap.put(permissionEntity.getIdentity(), new PermissionEntry(
                    permissionEntity.getIdentity(),
                    null,
                    IDType.valueOf(permissionEntity.getIdentityType()),
                    new Permission[]{newPermission}));
          }
        }
      }
      permissionEntries = new ArrayList(permissionEntriesMap.values());

      // fill missing Permission (all PermissionEntry must have all Permission Types with isAllowed to true or false)
      for(PermissionEntry permissionEntry : permissionEntries) {
        for(PermissionType permissionType : filteredPermissionTypes) {
          boolean permissionTypeFound = false;
          for(Permission permission : permissionEntry.getPermissions()) {
            if(permission.getPermissionType().equals(permissionType)) {
              permissionTypeFound = true;
              break;
            }
          }
          if(!permissionTypeFound) {
            Permission newPermission = new Permission(permissionType, false);
            permissionEntry.setPermissions((Permission[])ArrayUtils.add(permissionEntry.getPermissions(), newPermission));
          }
        }
      }
    }
    return permissionEntries;
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
      pageEntity.setPermissions(convertPermissionEntriesToPermissionEntities(page.getPermissions()));
      pageEntity.setActivityId(page.getActivityId());
    }
    return pageEntity;
  }

  public static List<PermissionEntity> convertPermissionEntriesToPermissionEntities(List<PermissionEntry> permissionEntries) {
    List<PermissionEntity> permissionEntities = null;
    if(permissionEntries != null) {
      permissionEntities = new ArrayList<>();
      for (PermissionEntry permissionEntry : permissionEntries) {
        for (Permission permission : permissionEntry.getPermissions()) {
          if (permission.isAllowed()) {
            permissionEntities.add(new PermissionEntity(
                    permissionEntry.getId(),
                    permissionEntry.getIdType().toString(),
                    permission.getPermissionType()
            ));
          }
        }
      }
    }
    return permissionEntities;
  }

  public static Attachment convertAttachmentEntityToAttachment(FileService fileService, AttachmentEntity attachmentEntity, boolean loadContent) throws WikiException {
    Attachment attachment = null;
    FileItem fileItem = null;
    if (attachmentEntity != null) {
      attachment = new Attachment();
      attachment.setFullTitle(attachmentEntity.getFullTitle());
      if (attachmentEntity.getCreatedDate() != null) {
        Calendar createdDate = Calendar.getInstance();
        createdDate.setTime(attachmentEntity.getCreatedDate());
        attachment.setCreatedDate(createdDate);
      }
      try {
        if (loadContent) {
          fileItem = fileService.getFile(attachmentEntity.getAttachmentFileID());
        } else {
          FileInfo fileInfo = fileService.getFileInfo(attachmentEntity.getAttachmentFileID());
          fileItem = new FileItem(fileInfo, null);
        }
      } catch (Exception e) {
        throw new WikiException("Cannot get attachment file ID "+ attachmentEntity.getAttachmentFileID() + " from storage", e.getCause());
      }
      if (fileItem != null) {
        attachment.setName(fileItem.getFileInfo().getName());
        String fullTitle = attachment.getFullTitle();
        if (fullTitle != null && !StringUtils.isEmpty(fullTitle)) {
          int index = fullTitle.lastIndexOf(".");
          if (index != -1) {
            attachment.setTitle(fullTitle.substring(0, index));
          } else {
            attachment.setTitle(fullTitle);
          }
        }
        
        attachment.setContent(fileItem.getAsByte());
        attachment.setMimeType(fileItem.getFileInfo().getMimetype());
        attachment.setWeightInBytes(fileItem.getFileInfo().getSize());
        attachment.setCreator(fileItem.getFileInfo().getUpdater());
        if (fileItem.getFileInfo().getUpdatedDate() != null) {
          Calendar updatedDate = Calendar.getInstance();
          updatedDate.setTime(fileItem.getFileInfo().getUpdatedDate());
          attachment.setUpdatedDate(updatedDate);
        }
      }
    }

    return attachment;
  }

  public static PageAttachmentEntity convertAttachmentToPageAttachmentEntity(FileService fileService, Attachment attachment) throws WikiException {
    PageAttachmentEntity attachmentEntity = null;
    FileItem fileItem = null;

    if (attachment != null) {
      try {
        Date updatedDate;
        if(attachment.getUpdatedDate() != null){
          updatedDate = attachment.getUpdatedDate().getTime();
        }
        else{
          updatedDate = GregorianCalendar.getInstance().getTime();
        }
        long size = 0;
        if(attachment.getContent() != null){
          size = attachment.getContent().length;
        }
        fileItem = new FileItem(null,
                                attachment.getName(),
                                attachment.getMimeType(),
                                JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
                                size,
                                updatedDate,
                                attachment.getCreator(),
                                false,
                                new ByteArrayInputStream(attachment.getContent()));

        fileItem = fileService.writeFile(fileItem);
      } catch (Exception ex) {
        throw new WikiException("Cannot persist page attachment file NAME "+  attachment.getName() + " on file storage", ex.getCause());
      }
      attachmentEntity = new PageAttachmentEntity();
      attachmentEntity.setAttachmentFileID(fileItem.getFileInfo().getId());
      if(attachment.getFullTitle() == null){
        attachmentEntity.setFullTitle(attachment.getName());
      }
      else{
        attachmentEntity.setFullTitle(attachment.getFullTitle());
      }
      if (attachment.getCreatedDate() != null) {
        attachmentEntity.setCreatedDate(attachment.getCreatedDate().getTime());
      }
    }
    return attachmentEntity;
  }

  public static DraftPageAttachmentEntity convertAttachmentToDraftPageAttachmentEntity(FileService fileService,
                                                                                       Attachment attachment) throws WikiException {
    DraftPageAttachmentEntity attachmentEntity = null;
    FileItem fileItem = null;
    if (attachment != null) {
      try {
        Date updatedDate;
        if(attachment.getUpdatedDate() != null){
          updatedDate = attachment.getUpdatedDate().getTime();
        }
        else{
          updatedDate = GregorianCalendar.getInstance().getTime();
        }
        int size =0;
        if(attachment.getContent() != null){
          size = attachment.getContent().length;
        }
        fileItem = new FileItem(null,
                                attachment.getName(),
                                attachment.getMimeType(),
                                JPADataStorage.WIKI_FILES_NAMESPACE_NAME,
                                size,
                                updatedDate,
                                attachment.getCreator(),
                                false,
                                new ByteArrayInputStream(attachment.getContent()));

        fileItem = fileService.writeFile(fileItem);
      } catch (Exception ex) {
        throw new WikiException("Cannot persist draft attachment file NAME "+  attachment.getName() + " on file storage", ex.getCause());
      }
      if (attachment.getUpdatedDate() == null) {
        attachment.setUpdatedDate(GregorianCalendar.getInstance());
      }
      attachmentEntity = new DraftPageAttachmentEntity();
      attachmentEntity.setAttachmentFileID(fileItem.getFileInfo().getId());
      if(attachment.getFullTitle() == null){
        attachmentEntity.setFullTitle(attachment.getName());
      }
      else{
        attachmentEntity.setFullTitle(attachment.getFullTitle());
      }
      if (attachment.getCreatedDate() != null) {
        attachmentEntity.setCreatedDate(attachment.getCreatedDate().getTime());
      }
    }
    return attachmentEntity;
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
      buildNotePageMetadata(draftPage, true);
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
      buildNotePageMetadata(draftPage, true);
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
      buildNotePageMetadata(pageVersion, pageVersion.isDraftPage());
    }
    return pageVersion;
  }

  public static PageHistory convertPageVersionEntityToPageHistory(PageVersionEntity pageVersionEntity) {
    PageHistory pageHistory = null;
    if (pageVersionEntity != null) {
      pageHistory = new PageHistory();
      pageHistory.setId(pageVersionEntity.getId());
      pageHistory.setVersionNumber(pageVersionEntity.getVersionNumber());
      pageHistory.setAuthor(pageVersionEntity.getAuthor());
      pageHistory.setContent(pageVersionEntity.getContent());
      pageHistory.setCreatedDate(pageVersionEntity.getCreatedDate());
      pageHistory.setUpdatedDate(pageVersionEntity.getUpdatedDate());
      pageHistory.setLang(pageVersionEntity.getLang());
    }
    return pageHistory;
  }

  public static PermissionEntry convertPermissionEntityToPermissionEntry(PermissionEntity permissionEntity) {
    PermissionEntry permissionEntry = null;
    if (permissionEntity != null) {
      permissionEntry = new PermissionEntry();
      permissionEntry.setId(permissionEntity.getIdentity());
      permissionEntry.setIdType(IDType.valueOf(permissionEntity.getIdentityType().toUpperCase()));
      permissionEntry.setPermissions(new Permission[] { new Permission(permissionEntity.getPermissionType(), true) });
    }
    return permissionEntry;
  }

  public static List<PageHistory> toPageHistoryVersions(List<PageVersionEntity> pageVersionEntities) {
    IdentityManager identityManager = CommonsUtils.getService(IdentityManager.class);
    
    return pageVersionEntities.stream().map(EntityConverter::convertPageVersionEntityToPageHistory)
                                       .peek(pageHistory -> {
                                         Identity identity = identityManager.getOrCreateIdentity(OrganizationIdentityProvider.NAME,
                                                                                        pageHistory.getAuthor());
                                         if (identity != null && identity.getProfile() != null
                                                             && identity.getProfile().getFullName() != null) {
                                            pageHistory.setAuthorFullName(identity.getProfile().getFullName());
                                         }}).toList();
  }

  public static List<DraftPage> toDraftPages(List<DraftPageEntity> draftPageEntities) {
    return draftPageEntities.stream().map(EntityConverter::convertDraftPageEntityToDraftPage).toList();
  }
}
