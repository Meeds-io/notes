/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2026 Meeds Association contact@meeds.io
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
package io.meeds.notes.plugin;

import static io.meeds.notes.util.ApplicationSettingsUtils.getLanguages;
import static io.meeds.notes.util.ApplicationSettingsUtils.integerSchema;
import static io.meeds.notes.util.ApplicationSettingsUtils.notAccepted;
import static io.meeds.notes.util.ApplicationSettingsUtils.objectSchema;
import static io.meeds.notes.util.ApplicationSettingsUtils.requireKnownOptions;
import static io.meeds.notes.util.ApplicationSettingsUtils.stringSchema;
import static io.meeds.notes.util.ApplicationSettingsUtils.toText;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import org.exoplatform.commons.ObjectAlreadyExistsException;
import org.exoplatform.commons.exception.ObjectNotFoundException;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.model.Page;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.plugin.ApplicationSettingsPlugin;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.notes.model.NotePageData;
import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;

/**
 * Configures a Block of content window as its settings drawer does: its
 * text is a page note bound to the CMS setting the window's {@code name}
 * preference designates, written in the default language or in a
 * translation through {@link NotePageViewService}'s permission-checked
 * writers, as the user. A draft window shares its setting with the live one,
 * so the first write on a window that does not own its setting creates one
 * for it, keyed on the live page, with a copy of the note in every language.
 * A window copied with no setting name carries the note's every language in
 * its {@code data.init} preference instead, which the portlet imports into
 * the setting it creates at the first render: the first write imports it
 * into the setting it creates through
 * {@link NotePageViewService#importNotePageData}, as the user, since that
 * preference is one any editor of the page can write. The setting hangs on a
 * page: a window of a site container is not configurable.
 */
@Component
@Profile("layout")
public class NotePageViewApplicationSettingsPlugin implements ApplicationSettingsPlugin {

  public static final String         PORTLET_NAME        = "NotePageView";

  public static final String         NAME_PREFERENCE     = "name";

  public static final String         DATA_INIT_PREFERENCE = "data.init";

  public static final String         CONTENT             = "content";

  public static final String         LANG                = "lang";

  public static final String         NOTE_ID             = "note_id";

  private static final String        SETTING_NAME_PREFIX = "notePage";

  private static final String        DEFAULT_LANGUAGE_KEY = "";

  private static final List<String>  OPTIONS             = List.of(CONTENT, LANG, NOTE_ID);

  @Autowired
  private NotePageViewService        notePageViewService;

  @Autowired
  private CMSService                 cmsService;

  @Autowired
  private UserACL                    userAcl;

  @Autowired
  private IdentityManager            identityManager;

  @Autowired
  private LocaleConfigService        localeConfigService;

  @Autowired(required = false)
  private ApplicationSettingsService applicationSettingsService;

  @PostConstruct
  public void init() {
    if (applicationSettingsService != null) {
      applicationSettingsService.addPlugin(this);
    }
  }

  @Override
  public String getPortletName() {
    return PORTLET_NAME;
  }

  @Override
  public Map<String, Object> getSettingsSchema() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put(CONTENT, stringSchema("HTML content, as the drawer's editor saves it; an empty text empties it"));
    properties.put(LANG,
                   stringSchema("Language tag of the translation written, among the platform's languages, such as fr or es-ES;"
                       + " omitted, the default content, shown to every reader without a translation in their language, is written"));
    properties.put(NOTE_ID,
                   integerSchema("Id of the note holding the content, which the notes tools edit; read-only, accepted back unchanged,"
                       + " absent while the window holds no note yet"));
    return objectSchema(properties);
  }

  @Override
  public Map<String, Object> read(ApplicationSettingsContext context) throws IllegalAccessException, ObjectNotFoundException {
    Page page = getReadableSource(context, getIdentity(context));
    Map<String, Object> settings = new LinkedHashMap<>();
    if (page != null) {
      settings.put(NOTE_ID, Long.parseLong(page.getId()));
      settings.put(CONTENT, StringUtils.defaultString(page.getContent()));
      return settings;
    }
    Map<String, String> initialPages = getInitialPages(context);
    if (initialPages == null) {
      return null; // NOSONAR
    }
    settings.put(CONTENT, StringUtils.defaultString(initialPages.get(DEFAULT_LANGUAGE_KEY)));
    return settings;
  }

  @Override
  public void validate(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                         ObjectNotFoundException {
    Identity identity = requireEditor(context);
    requireKnownOptions(settings, OPTIONS);
    Page page = getReadableSource(context, identity);
    if (settings.containsKey(NOTE_ID)
        && (page == null || !(settings.get(NOTE_ID) instanceof Number number) || number.longValue() != Long.parseLong(page.getId()))) {
      throw notAccepted(NOTE_ID);
    }
    if (settings.containsKey(CONTENT)) {
      toText(settings.get(CONTENT), CONTENT);
    }
    String lang = toLang(settings);
    if (lang != null && page == null && !isDefaultLanguage(lang) && !hasInitialDefaultContent(context)) {
      throw notAccepted(LANG);
    }
  }

  @Override
  public void apply(ApplicationSettingsContext context, Map<String, Object> settings) throws IllegalAccessException,
                                                                                      ObjectNotFoundException {
    Identity identity = requireEditor(context);
    if (!settings.containsKey(CONTENT)) {
      return;
    }
    String content = toText(settings.get(CONTENT), CONTENT);
    String lang = toLang(settings);
    String name = context.getPreferences().get(NAME_PREFERENCE);
    Page page = getReadableSource(context, identity);
    Map<String, String> initialPages = page == null ? getInitialPages(context) : null;
    if (page != null ? isStored(name, page, content, lang, identity) : isInitial(initialPages, content, lang)) {
      return;
    }
    if (isOwned(context, name)) {
      write(name, content, lang, identity);
    } else {
      String newName = createSetting(context);
      Map<String, String> keys = new LinkedHashMap<>();
      keys.put(NAME_PREFERENCE, newName);
      if (page != null) {
        notePageViewService.copyNotePage(name, newName, identity);
      } else if (initialPages != null) {
        notePageViewService.importNotePageData(newName, new NotePageData(initialPages), identity);
        keys.put(DATA_INIT_PREFERENCE, null);
      }
      write(newName, content, lang, identity);
      applicationSettingsService.markOwner(context, keys);
    }
  }

  /**
   * The right to configure the window comes from the live page holding it,
   * never from a preference: the window's {@code name} is client-writable.
   */
  private Identity requireEditor(ApplicationSettingsContext context) throws IllegalAccessException {
    if (StringUtils.isBlank(context.getUsername()) || StringUtils.isBlank(context.getPageReference())) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    Identity identity = getIdentity(context);
    if (identity == null || !cmsService.hasEditPermission(identity, context.getPageReference(), context.getSpaceId())) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
    return identity;
  }

  /**
   * @return the note the window's {@code name} preference designates, in its
   *         default language, null when the preference, its setting or its
   *         note does not exist
   * @throws IllegalAccessException when the user may not read it: the
   *                                  preference is client-writable
   */
  private Page getReadableSource(ApplicationSettingsContext context, Identity identity) throws IllegalAccessException {
    String name = context.getPreferences().get(NAME_PREFERENCE);
    if (StringUtils.isBlank(name)) {
      return null;
    }
    try {
      return notePageViewService.getNotePage(name, null, identity);
    } catch (IllegalAccessException e) {
      throw new IllegalAccessException(ApplicationSettingsService.APPLICATION_NOT_ALLOWED);
    }
  }

  /**
   * @return the note's every language as the window's {@code data.init}
   *         preference holds it, keyed as {@link NotePageData#getPages()}
   *         is, null when the window carries no such preference
   */
  private Map<String, String> getInitialPages(ApplicationSettingsContext context) {
    String data = context.getPreferences().get(DATA_INIT_PREFERENCE);
    if (StringUtils.isBlank(data)) {
      return null; // NOSONAR
    }
    NotePageData pageData = JsonUtils.fromJsonString(data, NotePageData.class);
    return pageData == null || pageData.getPages() == null ? null : pageData.getPages();
  }

  private boolean hasInitialDefaultContent(ApplicationSettingsContext context) {
    Map<String, String> initialPages = getInitialPages(context);
    return initialPages != null && initialPages.get(DEFAULT_LANGUAGE_KEY) != null;
  }

  /**
   * @return whether the content is the one stored in the addressed language,
   *         as {@link #write} addresses it: the version's when the note
   *         holds one in that language, else the default content for the
   *         platform's default language
   */
  private boolean isStored(String name, Page page, String content, String lang, Identity identity) throws IllegalAccessException {
    Page version = lang == null ? null : getVersion(name, lang, identity);
    if (version != null) {
      return isSame(version.getContent(), content);
    }
    return (lang == null || isDefaultLanguage(lang)) && isSame(page.getContent(), content);
  }

  /**
   * @return whether the content is the one the window's {@code data.init}
   *         holds in the addressed language, as {@link #isStored} judges it
   */
  private boolean isInitial(Map<String, String> initialPages, String content, String lang) {
    if (initialPages == null) {
      return false;
    } else if (lang != null && initialPages.containsKey(lang)) {
      return isSame(initialPages.get(lang), content);
    }
    return (lang == null || isDefaultLanguage(lang)) && isSame(initialPages.get(DEFAULT_LANGUAGE_KEY), content);
  }

  private static boolean isSame(String stored, String content) {
    return StringUtils.equals(StringUtils.defaultString(stored), content);
  }

  /**
   * @return the note as published in the language, null when it holds no
   *         version in that exact stored code
   */
  private Page getVersion(String name, String lang, Identity identity) throws IllegalAccessException {
    Page page = notePageViewService.getNotePage(name, lang, identity);
    return page != null && StringUtils.equals(page.getLang(), lang) ? page : null;
  }

  /**
   * The window owns its setting when it carries the ownership marker naming
   * itself and the setting hangs on the live page holding the window
   */
  private boolean isOwned(ApplicationSettingsContext context, String name) {
    if (StringUtils.isBlank(name) || !applicationSettingsService.isOwner(context)) {
      return false;
    }
    CMSSetting setting = cmsService.getSetting(NotePageViewService.CMS_CONTENT_TYPE, name);
    return setting != null && StringUtils.equals(setting.getPageReference(), context.getPageReference());
  }

  /**
   * A language the note holds no version in is written as the default
   * content when it is the platform's default language; every other
   * language is written as that language's version
   */
  private void write(String name, String content, String lang, Identity identity) throws IllegalAccessException,
                                                                                   ObjectNotFoundException {
    if (lang == null || (isDefaultLanguage(lang) && getVersion(name, lang, identity) == null)) {
      notePageViewService.saveNotePage(name, content, null, identity);
    } else {
      notePageViewService.createNoteTranslation(name, content, lang, identity);
    }
  }

  private String createSetting(ApplicationSettingsContext context) {
    String name;
    do {
      name = SETTING_NAME_PREFIX + "-" + context.getApplicationId() + "-" + UUID.randomUUID();
    } while (cmsService.isSettingNameExists(NotePageViewService.CMS_CONTENT_TYPE, name));
    try {
      cmsService.saveSettingName(NotePageViewService.CMS_CONTENT_TYPE,
                                 name,
                                 context.getPageReference(),
                                 context.getSpaceId(),
                                 Long.parseLong(identityManager.getOrCreateUserIdentity(context.getUsername()).getId()));
    } catch (ObjectAlreadyExistsException e) {
      throw new IllegalStateException(String.format("Note page setting %s already exists", name), e);
    }
    return name;
  }

  /**
   * @return the language in its stored form, null when none is given
   */
  private String toLang(Map<String, Object> settings) {
    if (!settings.containsKey(LANG)) {
      return null;
    }
    String tag = toText(settings.get(LANG), LANG);
    if (!getLanguages(localeConfigService).contains(tag)) {
      throw notAccepted(LANG);
    }
    return Locale.forLanguageTag(tag).toString();
  }

  private boolean isDefaultLanguage(String lang) {
    return StringUtils.equals(localeConfigService.getDefaultLocaleConfig().getLocale().toString(), lang);
  }

  private Identity getIdentity(ApplicationSettingsContext context) {
    return userAcl.getUserIdentity(context.getUsername());
  }

}
