/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io
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
package io.meeds.notes.plugin.layout.renderer;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.portal.config.model.Application;
import org.exoplatform.portal.pom.spi.portlet.Portlet;
import org.exoplatform.portal.pom.spi.portlet.Preference;

import io.meeds.layout.model.PortletInstancePreference;
import io.meeds.layout.plugin.PortletInstancePreferencePlugin;
import io.meeds.notes.model.NotePageData;
import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.util.JsonUtils;

public class NoteSingleViewRenderPlugin implements PortletInstancePreferencePlugin {

  private static final String CMS_SETTING_PREFERENCE_NAME = "name";

  private static final String DATA_INIT_PREFERENCE_NAME   = "data.init";

  private NotePageViewService notePageViewService;

  public NoteSingleViewRenderPlugin(NotePageViewService notePageViewService) {
    this.notePageViewService = notePageViewService;
  }

  @Override
  public String getPortletName() {
    return "NotePageView";
  }

  @Override
  public List<PortletInstancePreference> generatePreferences(Application<Portlet> application, Portlet preferences) {
    String settingName = getCmsSettingName(preferences);
    if (StringUtils.isBlank(settingName)) {
      return Collections.emptyList();
    }
    NotePageData notePageData = notePageViewService.getNotePageData(settingName);
    return Collections.singletonList(new PortletInstancePreference(DATA_INIT_PREFERENCE_NAME,
                                                                   JsonUtils.toJsonString(notePageData)));
  }

  private String getCmsSettingName(Portlet preferences) {
    if (preferences == null) {
      return null;
    }
    Preference settingNamePreference = preferences.getPreference(CMS_SETTING_PREFERENCE_NAME);
    return settingNamePreference == null ? null : settingNamePreference.getValue();
  }

}
