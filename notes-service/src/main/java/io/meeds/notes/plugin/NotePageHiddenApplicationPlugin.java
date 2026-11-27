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

import java.util.Locale;
import java.util.ResourceBundle;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;
import io.meeds.social.hidden.plugin.HiddenApplicationPlugin;

/**
 * Lets each user hide a block of content whose editor allowed it. A block has
 * no header: its title in the user settings is the name of the application.
 */
@Service("notePageHiddenApplicationPlugin")
public class NotePageHiddenApplicationPlugin implements HiddenApplicationPlugin {

  public static final String    NOTE_PAGE_VIEW_BUNDLE_NAME = "locale.portlet.NotePageView";

  public static final String    DEFAULT_NAME_KEY           = "notePageView.label.defaultName";

  @Autowired
  private CMSService            cmsService;

  @Autowired
  private NotePageViewService   notePageViewService;

  @Autowired
  private ResourceBundleService resourceBundleService;

  @Override
  public String getApplicationType() {
    return NotePageViewService.CMS_CONTENT_TYPE;
  }

  @Override
  public HiddenApplication getApplication(String name, String language) {
    CMSSetting setting = cmsService.getSetting(NotePageViewService.CMS_CONTENT_TYPE, name);
    if (setting == null) {
      return null;
    }
    return new HiddenApplication(getApplicationType(),
                                 name,
                                 getTitle(language),
                                 setting.getPageReference(),
                                 notePageViewService.isHideAllowed(name));
  }

  @Override
  public boolean canView(String name, Identity identity) {
    return cmsService.hasAccessPermission(identity, NotePageViewService.CMS_CONTENT_TYPE, name);
  }

  private String getTitle(String language) {
    Locale locale = StringUtils.isBlank(language) ? Locale.ENGLISH : Locale.forLanguageTag(language);
    ResourceBundle bundle = resourceBundleService.getResourceBundle(NOTE_PAGE_VIEW_BUNDLE_NAME, locale);
    return bundle != null && bundle.containsKey(DEFAULT_NAME_KEY) ? bundle.getString(DEFAULT_NAME_KEY) : null;
  }

}
