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

import static io.meeds.notes.service.NotePageViewService.CMS_CONTENT_TYPE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.ListResourceBundle;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.services.resources.ResourceBundleService;
import org.exoplatform.services.security.Identity;

import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.hidden.model.HiddenApplication;

/**
 * What social's hidden applications learn of a block of content: whether it
 * exists, who may view it, and its title, page and option.
 */
@ExtendWith(MockitoExtension.class)
class NotePageHiddenApplicationPluginTest {

  private static final String             NAME           = "Untitled-5-abc";

  private static final String             PAGE_REFERENCE = "portal::dw::overview";

  @Mock
  private CMSService                      cmsService;

  @Mock
  private NotePageViewService             notePageViewService;

  @Mock
  private ResourceBundleService           resourceBundleService;

  @InjectMocks
  private NotePageHiddenApplicationPlugin plugin;

  @Test
  void getApplicationTypeIsTheBlockSettingType() {
    assertEquals(CMS_CONTENT_TYPE, plugin.getApplicationType());
  }

  @Test
  void getApplicationOfAnUnknownBlockIsNull() {
    assertNull(plugin.getApplication(NAME, "fr"));
  }

  @Test
  void getApplicationGivesTheBlockPageOptionAndDefaultName() {
    when(cmsService.getSetting(CMS_CONTENT_TYPE, NAME)).thenReturn(new CMSSetting(CMS_CONTENT_TYPE, NAME, PAGE_REFERENCE, 0));
    when(notePageViewService.isHideAllowed(NAME)).thenReturn(true);
    when(resourceBundleService.getResourceBundle(eq(NotePageHiddenApplicationPlugin.NOTE_PAGE_VIEW_BUNDLE_NAME),
                                                 any(Locale.class))).thenReturn(bundle(NotePageHiddenApplicationPlugin.DEFAULT_NAME_KEY,
                                                                                       "Block of content"));

    HiddenApplication application = plugin.getApplication(NAME, "fr");

    assertEquals(CMS_CONTENT_TYPE, application.getType());
    assertEquals(NAME, application.getName());
    assertEquals(PAGE_REFERENCE, application.getPageReference());
    assertTrue(application.isAllowHide());
    assertEquals("Block of content", application.getTitle());
  }

  @Test
  void getApplicationTitleIsNullWhenTheBundleHasNoDefaultName() {
    when(cmsService.getSetting(CMS_CONTENT_TYPE, NAME)).thenReturn(new CMSSetting(CMS_CONTENT_TYPE, NAME, PAGE_REFERENCE, 0));
    when(resourceBundleService.getResourceBundle(eq(NotePageHiddenApplicationPlugin.NOTE_PAGE_VIEW_BUNDLE_NAME),
                                                 any(Locale.class))).thenReturn(bundle("other.key", "Other"));

    HiddenApplication application = plugin.getApplication(NAME, null);

    assertNull(application.getTitle());
    assertFalse(application.isAllowHide(), "The option is off until an editor turns it on");
  }

  @Test
  void canViewIsTheAccessPermissionOfTheBlock() {
    Identity reader = new Identity("reader");
    when(cmsService.hasAccessPermission(reader, CMS_CONTENT_TYPE, NAME)).thenReturn(true);

    assertTrue(plugin.canView(NAME, reader));
    assertFalse(plugin.canView(NAME, new Identity("other")));
  }

  private ListResourceBundle bundle(String key, String value) {
    return new ListResourceBundle() {
      @Override
      protected Object[][] getContents() {
        return new Object[][] { { key, value } };
      }
    };
  }

}
