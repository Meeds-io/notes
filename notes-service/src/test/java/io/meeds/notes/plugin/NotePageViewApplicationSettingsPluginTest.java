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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.exoplatform.portal.config.UserACL;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.wiki.model.Page;

import io.meeds.layout.model.ApplicationSettingsContext;
import io.meeds.layout.service.ApplicationSettingsService;
import io.meeds.notes.model.NotePageData;
import io.meeds.notes.service.NotePageViewService;
import io.meeds.social.cms.model.CMSSetting;
import io.meeds.social.cms.service.CMSService;
import io.meeds.social.util.JsonUtils;

import jakarta.annotation.PostConstruct;

@ExtendWith(MockitoExtension.class)
class NotePageViewApplicationSettingsPluginTest {

  private static final String                  USERNAME       = "mary";

  private static final String                  PAGE_REFERENCE = "portal::classic::home";

  private static final String                  SETTING_NAME   = "notePage-7";

  private static final String                  CONTENT_TYPE   = "notePage";

  private static final String                  NOTE_ID        = "12";

  private static final String                  NOT_ALLOWED    = "not allowed: application";

  @Mock
  private NotePageViewService                  notePageViewService;

  @Mock
  private CMSService                           cmsService;

  @Mock
  private UserACL                              userAcl;

  @Mock
  private IdentityManager                      identityManager;

  @Mock
  private LocaleConfigService                  localeConfigService;

  @Mock
  private ApplicationSettingsService           applicationSettingsService;

  @InjectMocks
  private NotePageViewApplicationSettingsPlugin plugin;

  private Identity                             identity;

  @BeforeEach
  void setUp() {
    identity = new Identity(USERNAME);
    lenient().when(userAcl.getUserIdentity(USERNAME)).thenReturn(identity);
    lenient().when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 0)).thenReturn(true);
    defaultLanguage(Locale.ENGLISH);
    org.exoplatform.social.core.identity.model.Identity userIdentity = mock(org.exoplatform.social.core.identity.model.Identity.class);
    lenient().when(userIdentity.getId()).thenReturn("42");
    lenient().when(identityManager.getOrCreateUserIdentity(USERNAME)).thenReturn(userIdentity);
  }

  /**
   * The framework calls {@code init}: the annotation check kills the mutant
   * that removes {@code @PostConstruct}, which a direct call cannot see
   */
  @Test
  void testInitRegistersThePlugin() throws Exception {
    assertTrue(NotePageViewApplicationSettingsPlugin.class.getMethod("init").isAnnotationPresent(PostConstruct.class));
    plugin.init();
    verify(applicationSettingsService).addPlugin(plugin);
  }

  @Test
  void testInitWithoutRegistry() {
    NotePageViewApplicationSettingsPlugin standalone = new NotePageViewApplicationSettingsPlugin();
    assertDoesNotThrow(standalone::init);
  }

  @Test
  @SuppressWarnings("unchecked")
  void testSchemaExposesTheDrawerOptions() {
    assertEquals("NotePageView", plugin.getPortletName());
    assertFalse(plugin.isConfigurableInSiteContainer());
    Map<String, Object> schema = plugin.getSettingsSchema();
    Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
    assertEquals(List.of("content", "lang", "note_id"), List.copyOf(properties.keySet()));
    assertEquals("string", ((Map<String, Object>) properties.get("content")).get("type"));
    assertEquals("string", ((Map<String, Object>) properties.get("lang")).get("type"));
    assertEquals("integer", ((Map<String, Object>) properties.get("note_id")).get("type"));
    assertEquals(false, schema.get("additionalProperties"));
  }

  @Test
  void testReadReturnsTheNoteAndItsDefaultContent() throws Exception {
    assertNull(plugin.read(context(Map.of(), false)));
    verify(notePageViewService, never()).getNotePage(anyString(), any(), any());

    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    assertNull(plugin.read(context));

    storedNote("<p>Hello</p>");
    assertEquals(Map.of("note_id", 12L, "content", "<p>Hello</p>"), plugin.read(context));

    storedNote(null);
    assertEquals(Map.of("note_id", 12L, "content", ""), plugin.read(context));
  }

  @Test
  void testReadOfANoteTheUserMayNotAccessIsRefused() throws Exception {
    when(notePageViewService.getNotePage(SETTING_NAME, null, identity)).thenThrow(new IllegalAccessException("Note page isn't accessible"));
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.read(context(Map.of("name", SETTING_NAME), false)));
    assertEquals(NOT_ALLOWED, e.getMessage());
  }

  @Test
  void testValidateRefusesValuesTheDrawerRefuses() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    assertRefused(context, Map.of("view_template", "List"), "value is not accepted for view_template");
    assertRefused(context, Map.of("content", 42), "value is not accepted for content");
    assertRefused(context, Map.of("lang", 42), "value is not accepted for lang");
    assertRefused(context, Map.of("lang", "xx"), "value is not accepted for lang");
    assertRefused(context, Map.of("lang", "es_ES"), "value is not accepted for lang");
    assertRefused(context, Map.of("note_id", 13), "value is not accepted for note_id");
    assertRefused(context, Map.of("note_id", "12"), "value is not accepted for note_id");
    Map<String, Object> nullContent = new HashMap<>();
    nullContent.put("content", null);
    assertRefused(context, nullContent, "value is not accepted for content");
    Map<String, Object> nullLang = new HashMap<>();
    nullLang.put("lang", null);
    assertRefused(context, nullLang, "value is not accepted for lang");
    Map<String, Object> nullNoteId = new HashMap<>();
    nullNoteId.put("note_id", null);
    assertRefused(context, nullNoteId, "value is not accepted for note_id");
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("content", "", "lang", "fr", "note_id", 12)));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("content", "<p>Hola</p>", "lang", "es-ES")));
  }

  /**
   * A translation needs the default content first: a language other than
   * the platform's default is refused while the window holds no note, and
   * so is a note id, which the read never returned
   */
  @Test
  void testValidateRefusesATranslationOnAWindowWithoutNote() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), false);
    assertRefused(context, Map.of("content", "<p>Bonjour</p>", "lang", "fr"), "value is not accepted for lang");
    assertRefused(context, Map.of("note_id", 12), "value is not accepted for note_id");
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("content", "<p>Hello</p>", "lang", "en")));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("content", "<p>Hello</p>")));

    defaultLanguage(Locale.forLanguageTag("es-ES"));
    assertDoesNotThrow(() -> plugin.validate(context, Map.of("content", "<p>Hola</p>", "lang", "es-ES")));
    assertRefused(context, Map.of("content", "<p>Hello</p>", "lang", "en"), "value is not accepted for lang");
  }

  /**
   * The right comes from the live page of the context, never from the
   * window's {@code name} preference, which any editor of any page can set
   */
  @Test
  void testValidateRefusesAUserWhoMayNotEditThePage() throws Exception {
    when(cmsService.hasEditPermission(identity, PAGE_REFERENCE, 0)).thenReturn(false);
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("content", "<p>Hello</p>")));
    assertEquals(NOT_ALLOWED, e.getMessage());
    assertEquals(NOT_ALLOWED,
                 assertThrows(IllegalAccessException.class, () -> plugin.apply(context, Map.of("content", "<p>Hello</p>"))).getMessage());
    verify(notePageViewService, never()).saveNotePage(anyString(), anyString(), any(), any());
    verify(notePageViewService, never()).getNotePage(anyString(), any(), any());
  }

  @Test
  void testValidateRefusesABlankUserBeforeAnyCheck() {
    lenient().when(userAcl.getUserIdentity("")).thenReturn(identity);
    lenient().when(cmsService.hasEditPermission(any(), anyString(), anyLong())).thenReturn(true);
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    context.setUsername("");
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("content", "<p>Hello</p>")));
    assertEquals(NOT_ALLOWED, e.getMessage());
    verify(cmsService, never()).hasEditPermission(any(), anyString(), anyLong());
  }

  @Test
  void testValidateRefusesAWindowWithoutPage() {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    context.setSiteContainer(true);
    context.setPageReference(null);
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context, Map.of("content", "<p>Hello</p>")));
    assertEquals(NOT_ALLOWED, e.getMessage());
  }

  @Test
  void testValidateRefusesASourceTheUserMayNotAccess() throws Exception {
    when(notePageViewService.getNotePage(SETTING_NAME, null, identity)).thenThrow(new IllegalAccessException("Note page isn't accessible"));
    IllegalAccessException e = assertThrows(IllegalAccessException.class,
                                            () -> plugin.validate(context(Map.of("name", SETTING_NAME), false),
                                                                  Map.of("content", "<p>Hello</p>")));
    assertEquals(NOT_ALLOWED, e.getMessage());
  }

  /**
   * What the read returns is accepted back and writes nothing, whatever the
   * stored content, and so does a call carrying no content
   */
  @Test
  void testReadIsAcceptedBackUnchanged() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    Map<String, Object> read = plugin.read(context);
    plugin.validate(context, read);
    plugin.apply(context, read);

    storedNote(null);
    read = plugin.read(context);
    assertEquals("", read.get("content"));
    plugin.validate(context, read);
    plugin.apply(context, read);

    plugin.validate(context, Map.of("note_id", 12));
    plugin.apply(context, Map.of("note_id", 12));

    verifyNothingWritten();
  }

  @Test
  void testUnchangedTranslationWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    storedVersion("fr", "<p>Bonjour</p>");
    plugin.apply(context, Map.of("content", "<p>Bonjour</p>", "lang", "fr"));
    verifyNothingWritten();
  }

  @Test
  void testApplyWritesInPlaceOnAnOwnedWindow() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    storedNote("<p>Hello</p>");
    storedSetting(SETTING_NAME, PAGE_REFERENCE);
    plugin.apply(context, Map.of("content", "<p>Hi</p>"));
    verify(notePageViewService).saveNotePage(SETTING_NAME, "<p>Hi</p>", null, identity);
    verify(notePageViewService, never()).copyNotePage(anyString(), anyString(), any());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  /**
   * A draft window shares its setting, and so its note, with the live
   * window: the first write creates a setting for it on the live page,
   * copies the note onto it as the user, writes, then marks the window owner
   */
  @Test
  @SuppressWarnings("unchecked")
  void testFirstWriteRekeysAWindowThatDoesNotOwnItsSetting() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    plugin.apply(context, Map.of("content", "<p>Hi</p>"));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Map<String, String>> keys = ArgumentCaptor.forClass(Map.class);
    InOrder inOrder = inOrder(cmsService, notePageViewService, applicationSettingsService);
    inOrder.verify(cmsService).saveSettingName(eq(CONTENT_TYPE), name.capture(), eq(PAGE_REFERENCE), eq(0L), eq(42L));
    inOrder.verify(notePageViewService).copyNotePage(SETTING_NAME, name.getValue(), identity);
    inOrder.verify(notePageViewService).saveNotePage(name.getValue(), "<p>Hi</p>", null, identity);
    inOrder.verify(applicationSettingsService).markOwner(eq(context), keys.capture());
    assertNotEquals(SETTING_NAME, name.getValue());
    assertTrue(name.getValue().startsWith("notePage-45-"));
    assertEquals(Map.of("name", name.getValue()), keys.getValue());
    verify(notePageViewService, never()).saveNotePage(eq(SETTING_NAME), anyString(), any(), any());
  }

  /**
   * The ownership marker alone grants nothing: a marked window whose setting
   * hangs on another page is re-keyed too
   */
  @Test
  void testMarkedWindowWhoseSettingHangsOnAnotherPageIsRekeyed() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    storedNote("<p>Hello</p>");
    storedSetting(SETTING_NAME, "portal::classic::other");
    plugin.apply(context, Map.of("content", "<p>Hi</p>"));
    verify(cmsService).saveSettingName(eq(CONTENT_TYPE), anyString(), eq(PAGE_REFERENCE), eq(0L), eq(42L));
    verify(notePageViewService).copyNotePage(eq(SETTING_NAME), anyString(), eq(identity));
    verify(applicationSettingsService).markOwner(eq(context), anyMap());
  }

  @Test
  void testFirstWriteOnANeverRenderedWindowCreatesItsSetting() throws Exception {
    ApplicationSettingsContext context = context(Map.of(), true);
    when(cmsService.isSettingNameExists(eq(CONTENT_TYPE), anyString())).thenReturn(true, false);
    plugin.apply(context, Map.of("content", "<p>Hi</p>"));
    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(cmsService).saveSettingName(eq(CONTENT_TYPE), name.capture(), eq(PAGE_REFERENCE), eq(0L), eq(42L));
    verify(cmsService, times(2)).isSettingNameExists(eq(CONTENT_TYPE), anyString());
    verify(notePageViewService, never()).copyNotePage(anyString(), anyString(), any());
    verify(notePageViewService).saveNotePage(name.getValue(), "<p>Hi</p>", null, identity);
    verify(applicationSettingsService).markOwner(context, Map.of("name", name.getValue()));
  }

  /**
   * The platform's default language, in its stored form, is the default
   * content while the note holds no version in it: two writes in it leave no
   * translation, with a bare code as with a region-qualified one
   */
  @Test
  void testDefaultLanguageWritesTheDefaultContent() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    storedNote("<p>Hello</p>");
    storedSetting(SETTING_NAME, PAGE_REFERENCE);
    plugin.apply(context, Map.of("content", "<p>Hi</p>", "lang", "en"));
    plugin.apply(context, Map.of("content", "<p>Hi again</p>", "lang", "en"));
    verify(notePageViewService).saveNotePage(SETTING_NAME, "<p>Hi</p>", null, identity);
    verify(notePageViewService).saveNotePage(SETTING_NAME, "<p>Hi again</p>", null, identity);

    defaultLanguage(Locale.forLanguageTag("es-ES"));
    plugin.apply(context, Map.of("content", "<p>Hola</p>", "lang", "es-ES"));
    verify(notePageViewService).saveNotePage(SETTING_NAME, "<p>Hola</p>", null, identity);
    verify(notePageViewService, never()).createNoteTranslation(anyString(), anyString(), anyString(), any());
  }

  @Test
  void testDefaultLanguageWithAVersionWritesThatVersion() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    storedNote("<p>Hello</p>");
    storedSetting(SETTING_NAME, PAGE_REFERENCE);
    storedVersion("en", "<p>Hello there</p>");
    plugin.apply(context, Map.of("content", "<p>Hi</p>", "lang", "en"));
    verify(notePageViewService).createNoteTranslation(SETTING_NAME, "<p>Hi</p>", "en", identity);
    verify(notePageViewService, never()).saveNotePage(anyString(), anyString(), any(), any());
  }

  /**
   * A language other than the default is written as its version, in the
   * stored form of its code, whether the note holds it or not; the default
   * content is never written
   */
  @Test
  void testOtherLanguageWritesItsVersion() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), true);
    storedNote("<p>Hello</p>");
    storedSetting(SETTING_NAME, PAGE_REFERENCE);
    plugin.apply(context, Map.of("content", "<p>Bonjour</p>", "lang", "fr"));
    plugin.apply(context, Map.of("content", "<p>Hola</p>", "lang", "es-ES"));
    storedVersion("fr", "<p>Bonjour</p>");
    plugin.apply(context, Map.of("content", "<p>Salut</p>", "lang", "fr"));
    verify(notePageViewService).createNoteTranslation(SETTING_NAME, "<p>Bonjour</p>", "fr", identity);
    verify(notePageViewService).createNoteTranslation(SETTING_NAME, "<p>Hola</p>", "es_ES", identity);
    verify(notePageViewService).createNoteTranslation(SETTING_NAME, "<p>Salut</p>", "fr", identity);
    verify(notePageViewService, never()).saveNotePage(anyString(), anyString(), any(), any());
  }

  @Test
  void testTranslationOnARekeyedWindowIsWrittenOnTheCopy() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    plugin.apply(context, Map.of("content", "<p>Bonjour</p>", "lang", "fr"));
    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    verify(notePageViewService).copyNotePage(eq(SETTING_NAME), name.capture(), eq(identity));
    verify(notePageViewService).createNoteTranslation(name.getValue(), "<p>Bonjour</p>", "fr", identity);
  }

  /**
   * The platform's default language without a version is the default
   * content, for the unchanged check as for the write: the stored content
   * sent back with that language writes nothing and re-keys nothing, with a
   * bare code as with a region-qualified one
   */
  @Test
  void testDefaultLanguageWithoutVersionSentBackUnchangedWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(Map.of("name", SETTING_NAME), false);
    storedNote("<p>Hello</p>");
    plugin.apply(context, Map.of("content", "<p>Hello</p>", "lang", "en"));
    defaultLanguage(Locale.forLanguageTag("es-ES"));
    plugin.apply(context, Map.of("content", "<p>Hello</p>", "lang", "es-ES"));
    verifyNothingWritten();
    plugin.apply(context, Map.of("content", "<p>Hello</p>", "lang", "fr"));
    verify(notePageViewService).copyNotePage(eq(SETTING_NAME), anyString(), eq(identity));
  }

  /**
   * A window copied with no setting name holds the note's every language in
   * data.init, which the portlet imports at the first render: the read shows
   * its default content, without a note id
   */
  @Test
  void testReadOfAnUnkeyedWindowReturnsItsInitialData() throws Exception {
    assertEquals(Map.of("content", "<p>Hello</p>"), plugin.read(context(initialData(), false)));
    assertEquals(Map.of("content", ""), plugin.read(context(Map.of("data.init", "{\"pages\":{\"fr\":\"<p>Bonjour</p>\"}}"), false)));
    assertNull(plugin.read(context(Map.of("data.init", " "), false)));
    assertNull(plugin.read(context(Map.of("data.init", "{}"), false)));
    verify(notePageViewService, never()).getNotePage(anyString(), any(), any());
  }

  @Test
  void testValidateOnAnUnkeyedWindowReadsItsInitialData() {
    assertDoesNotThrow(() -> plugin.validate(context(initialData(), false), Map.of("content", "<p>Salut</p>", "lang", "fr")));
    assertRefused(context(Map.of("data.init", "{\"pages\":{\"fr\":\"<p>Bonjour</p>\"}}"), false),
                  Map.of("content", "<p>Salut</p>", "lang", "fr"),
                  "value is not accepted for lang");
    assertRefused(context(initialData(), false), Map.of("note_id", 12), "value is not accepted for note_id");
  }

  /**
   * The first write on such a window creates its setting, imports data.init
   * into it as the user through the checked import, never through the
   * super-user one, writes, then marks the window owner and clears data.init
   */
  @Test
  @SuppressWarnings("unchecked")
  void testFirstWriteOnAnUnkeyedWindowImportsItsInitialData() throws Exception {
    ApplicationSettingsContext context = context(initialData(), false);
    plugin.apply(context, Map.of("content", "<p>Salut</p>", "lang", "fr"));

    ArgumentCaptor<String> name = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<NotePageData> data = ArgumentCaptor.forClass(NotePageData.class);
    ArgumentCaptor<Map<String, String>> keys = ArgumentCaptor.forClass(Map.class);
    InOrder inOrder = inOrder(cmsService, notePageViewService, applicationSettingsService);
    inOrder.verify(cmsService).saveSettingName(eq(CONTENT_TYPE), name.capture(), eq(PAGE_REFERENCE), eq(0L), eq(42L));
    inOrder.verify(notePageViewService).importNotePageData(eq(name.getValue()), data.capture(), eq(identity));
    inOrder.verify(notePageViewService).createNoteTranslation(name.getValue(), "<p>Salut</p>", "fr", identity);
    inOrder.verify(applicationSettingsService).markOwner(eq(context), keys.capture());
    assertEquals(Map.of("", "<p>Hello</p>", "fr", "<p>Bonjour</p>"), data.getValue().getPages());
    Map<String, String> expectedKeys = new HashMap<>();
    expectedKeys.put("name", name.getValue());
    expectedKeys.put("data.init", null);
    assertEquals(expectedKeys, keys.getValue());
    verify(notePageViewService, never()).copyNotePage(anyString(), anyString(), any());
    verify(notePageViewService, never()).savePageData(anyString(), any());
    verify(notePageViewService, never()).saveNotePage(anyString(), anyString(), any(), any());
  }

  @Test
  void testInitialDataSentBackUnchangedWritesNothing() throws Exception {
    ApplicationSettingsContext context = context(initialData(), false);
    plugin.apply(context, Map.of("content", "<p>Hello</p>"));
    plugin.apply(context, Map.of("content", "<p>Hello</p>", "lang", "en"));
    plugin.apply(context, Map.of("content", "<p>Bonjour</p>", "lang", "fr"));
    plugin.apply(context, plugin.read(context));
    verifyNothingWritten();
    verify(notePageViewService, never()).savePageData(anyString(), any());
  }

  private Map<String, String> initialData() {
    Map<String, String> pages = new HashMap<>();
    pages.put("", "<p>Hello</p>");
    pages.put("fr", "<p>Bonjour</p>");
    return Map.of("data.init", JsonUtils.toJsonString(new NotePageData(pages)));
  }

  private void storedNote(String content) throws Exception {
    when(notePageViewService.getNotePage(SETTING_NAME, null, identity)).thenReturn(page(content, null));
  }

  private void storedVersion(String lang, String content) throws Exception {
    when(notePageViewService.getNotePage(SETTING_NAME, lang, identity)).thenReturn(page(content, lang));
  }

  private void storedSetting(String name, String pageReference) {
    CMSSetting setting = new CMSSetting();
    setting.setName(name);
    setting.setPageReference(pageReference);
    when(cmsService.getSetting(CONTENT_TYPE, name)).thenReturn(setting);
  }

  private Page page(String content, String lang) {
    Page page = new Page();
    page.setId(NOTE_ID);
    page.setContent(content);
    page.setLang(lang);
    return page;
  }

  private void verifyNothingWritten() throws Exception {
    verify(notePageViewService, never()).saveNotePage(anyString(), anyString(), any(), any());
    verify(notePageViewService, never()).createNoteTranslation(anyString(), anyString(), anyString(), any());
    verify(notePageViewService, never()).copyNotePage(anyString(), anyString(), any());
    verify(cmsService, never()).saveSettingName(anyString(), anyString(), anyString(), anyLong(), anyLong());
    verify(applicationSettingsService, never()).markOwner(any(), anyMap());
  }

  private void assertRefused(ApplicationSettingsContext context, Map<String, Object> settings, String message) {
    IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> plugin.validate(context, settings));
    assertEquals(message, e.getMessage());
  }

  private ApplicationSettingsContext context(Map<String, String> preferences, boolean owner) {
    ApplicationSettingsContext context = new ApplicationSettingsContext(45,
                                                                        "notes/NotePageView",
                                                                        new HashMap<>(preferences),
                                                                        false,
                                                                        true,
                                                                        null,
                                                                        null,
                                                                        PAGE_REFERENCE,
                                                                        0,
                                                                        USERNAME);
    lenient().when(applicationSettingsService.isOwner(context)).thenReturn(owner);
    return context;
  }

  private void defaultLanguage(Locale locale) {
    LocaleConfig defaultConfig = localeConfig(locale);
    List<LocaleConfig> configs = Arrays.asList(localeConfig(Locale.ENGLISH),
                                               localeConfig(Locale.FRENCH),
                                               localeConfig(Locale.forLanguageTag("es-ES")));
    lenient().when(localeConfigService.getLocalConfigs()).thenReturn(configs);
    lenient().when(localeConfigService.getDefaultLocaleConfig()).thenReturn(defaultConfig);
  }

  private LocaleConfig localeConfig(Locale locale) {
    LocaleConfig config = mock(LocaleConfig.class);
    lenient().when(config.getLocale()).thenReturn(locale);
    return config;
  }

}
