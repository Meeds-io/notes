/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io
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
package org.exoplatform.wiki.jpa.search;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.commons.search.domain.Document;
import org.exoplatform.commons.search.index.impl.ElasticIndexingServiceConnector;
import org.exoplatform.commons.utils.CommonsUtils;
import org.exoplatform.commons.utils.ExpressionUtil;
import org.exoplatform.container.xml.InitParams;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.page.PageKey;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.resources.LocaleContextInfo;
import org.exoplatform.services.resources.ResourceBundleManager;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.utils.Utils;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;

/**
 * Indexes a portal Page as soon as it carries a "content block" (today: the
 * Notes Single Note View portlet, bound through a {@code notePage}
 * {@link CMSSetting}). What is indexed is the content of the block (title,
 * author, date, plain-text content), the Page only provides the
 * "page name - site name" / link context.
 * <p>
 * The document id is the page's numeric storage id (not its
 * {@link PageKey#format()}, which can exceed the 50-character limit of the
 * {@code ES_INDEXING_QUEUE.ENTITY_ID} column) — it is also stable across
 * page/site renames, unlike the formatted key.
 */
public class PageContentIndexingServiceConnector extends ElasticIndexingServiceConnector {

  public static final String TYPE                   = "page";

  private static final String CMS_CONTENT_TYPE       = "notePage";

  private static final Log    LOGGER                 = ExoLogger.getExoLogger(PageContentIndexingServiceConnector.class);

  public PageContentIndexingServiceConnector(InitParams initParams) {
    super(initParams);
  }

  @Override
  public String getMapping() {
    return "{"
        + "  \"properties\" : {"
        + "    \"title\" : {\"type\" : \"text\", \"index_options\": \"offsets\","
        + "      \"fields\": {\"raw\": {\"type\": \"keyword\"}}},"
        + "    \"siteName\" : {\"type\" : \"keyword\"},"
        + "    \"author\" : {\"type\" : \"keyword\"},"
        + "    \"date\" : {\"type\" : \"date\", \"format\": \"epoch_millis\"},"
        + "    \"permissions\" : {\"type\" : \"keyword\"},"
        + "    \"content\" : {\"type\" : \"text\", \"store\": true, \"term_vector\": \"with_positions_offsets\"}"
        + "  }"
        + "}";
  }

  @Override
  public String getConnectorName() {
    return TYPE;
  }

  @Override
  public List<String> getAllIds(int offset, int limit) {
    List<CMSSetting> settings = getCmsService().getSettingsByType(CMS_CONTENT_TYPE);
    return settings.stream()
                   .map(CMSSetting::getPageReference)
                   .filter(StringUtils::isNotBlank)
                   .distinct()
                   .map(this::resolveStorageId)
                   .filter(StringUtils::isNotBlank)
                   .skip(offset)
                   .limit(limit)
                   .toList();
  }

  @Override
  public Document create(String id) {
    if (StringUtils.isBlank(id)) {
      throw new IllegalArgumentException("Id is null");
    }
    try {
      // Read the raw (unrestricted) layout data: indexing must see the page
      // regardless of any user's view permission, the resulting permissions
      // are stored on the Document itself for the search engine to filter on.
      org.exoplatform.portal.config.model.Page page = getLayoutService().getPage(parseStorageId(id));
      if (page == null) {
        LOGGER.warn("Page with storage id {} wasn't found, thus it can't be indexed", id);
        return null;
      }
      PageKey pageKey = page.getPageKey();
      CMSSetting setting = findNotePageSetting(pageKey.format());
      if (setting == null) {
        LOGGER.warn("Page {} doesn't carry any 'notePage' content block anymore, thus it can't be indexed", pageKey);
        return null;
      }
      Page note = getNotePageViewService().getNotePages(setting.getName()).get("");
      if (note == null) {
        LOGGER.warn("No note found for setting {} referenced by page {}, thus it can't be indexed", setting.getName(), pageKey);
        return null;
      }

      Map<String, String> fields = new HashMap<>();
      fields.put("title", page.getTitle());
      fields.put("siteName", getSiteDisplayName(pageKey));
      fields.put("author", note.getAuthor());
      fields.put("date", String.valueOf(note.getUpdatedDate() == null ? note.getCreatedDate().getTime()
                                                                       : note.getUpdatedDate().getTime()));
      fields.put("content", Utils.html2text(note.getContent()));

      Document document = new Document();
      document.setId(id);
      document.setLastUpdatedDate(note.getUpdatedDate());
      document.setPermissions(page.getAccessPermissions() == null ? new HashSet<>()
                                                                    : new HashSet<>(Arrays.asList(page.getAccessPermissions())));
      document.setFields(fields);
      return document;
    } catch (Exception e) {
      LOGGER.warn("Cannot index page with id {}", id, e);
      return null;
    }
  }

  @Override
  public Document update(String id) {
    return create(id);
  }

  private long parseStorageId(String storageId) {
    // PageStorageImpl builds page storage ids as "page_" + <numeric DB id>
    return Long.parseLong(StringUtils.removeStart(storageId, "page_"));
  }

  private String resolveStorageId(String pageReference) {
    try {
      org.exoplatform.portal.config.model.Page page = getLayoutService().getPage(PageKey.parse(pageReference));
      return page == null ? null : page.getStorageId();
    } catch (Exception e) {
      LOGGER.debug("Cannot resolve storage id of page {}", pageReference, e);
      return null;
    }
  }

  private CMSSetting findNotePageSetting(String pageReference) {
    return getCmsService().getSettingsByType(CMS_CONTENT_TYPE)
                          .stream()
                          .filter(setting -> StringUtils.equals(setting.getPageReference(), pageReference))
                          .findFirst()
                          .orElse(null);
  }

  private String getSiteDisplayName(PageKey pageKey) {
    PortalConfig site = getLayoutService().getPortalConfig(pageKey.getSite());
    String label = site == null ? null : site.getLabel();
    if (StringUtils.isBlank(label)) {
      return pageKey.getSite().getName();
    }
    if (ExpressionUtil.isResourceBindingExpression(label)) {
      String resolved = resolveLabelExpression(pageKey, label);
      return StringUtils.isNotBlank(resolved) ? resolved : pageKey.getSite().getName();
    }
    return label;
  }

  private String resolveLabelExpression(PageKey pageKey, String label) {
    try {
      LocaleConfigService localeConfigService = CommonsUtils.getService(LocaleConfigService.class);
      Locale locale = localeConfigService.getDefaultLocaleConfig().getLocale();
      ResourceBundle bundle = CommonsUtils.getService(ResourceBundleManager.class)
                                          .getNavigationResourceBundle(LocaleContextInfo.getLocaleAsString(locale),
                                                                       pageKey.getSite().getTypeName(),
                                                                       pageKey.getSite().getName());
      return bundle == null ? null : ExpressionUtil.getExpressionValue(bundle, label);
    } catch (Exception e) {
      LOGGER.debug("Cannot resolve site label expression {} for site {}", label, pageKey.getSite(), e);
      return null;
    }
  }

  private CMSService getCmsService() {
    return CommonsUtils.getService(CMSService.class);
  }

  private NotePageViewService getNotePageViewService() {
    return CommonsUtils.getService(NotePageViewService.class);
  }

  private LayoutService getLayoutService() {
    return CommonsUtils.getService(LayoutService.class);
  }

}
