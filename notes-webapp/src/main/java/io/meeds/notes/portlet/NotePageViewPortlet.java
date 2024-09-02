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
package io.meeds.notes.portlet;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javax.portlet.PortletPreferences;

import org.apache.commons.lang3.StringUtils;

import org.exoplatform.container.ExoContainerContext;

import io.meeds.notes.model.NotePageData;
import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.portlet.CMSPortlet;
import io.meeds.social.util.JsonUtils;

import lombok.SneakyThrows;

public class NotePageViewPortlet extends CMSPortlet {

  private static NotePageViewService notePageViewService;

  private static ReentrantLock       dataInitLock = new ReentrantLock();

  @Override
  @SneakyThrows
  protected void postSettingInit(PortletPreferences preferences, String name) {
    boolean locked = dataInitLock.tryLock(10, TimeUnit.SECONDS);
    try {
      String data = preferences.getValue(DATA_INIT_PREFERENCE_NAME, null);
      if (StringUtils.isNotBlank(data)) {
        NotePageData pageData = JsonUtils.fromJsonString(data, NotePageData.class);
        getNotePageViewService().savePageData(name, pageData);
        savePreference(DATA_INIT_PREFERENCE_NAME, null);
      }
    } finally {
      if (locked) {
        dataInitLock.unlock();
      }
    }
  }

  private static NotePageViewService getNotePageViewService() {
    if (notePageViewService == null) {
      notePageViewService = ExoContainerContext.getService(NotePageViewService.class);
    }
    return notePageViewService;
  }

}
