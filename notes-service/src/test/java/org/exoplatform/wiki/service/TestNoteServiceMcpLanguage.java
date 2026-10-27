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
package org.exoplatform.wiki.service;

import java.util.List;

import org.springframework.beans.factory.support.StaticListableBeanFactory;

import org.exoplatform.commons.file.services.FileService;
import org.exoplatform.portal.config.UserACL;
import org.exoplatform.portal.config.UserPortalConfigService;
import org.exoplatform.portal.config.model.PortalConfig;
import org.exoplatform.portal.mop.service.LayoutService;
import org.exoplatform.services.security.Identity;
import org.exoplatform.social.attachment.AttachmentService;
import org.exoplatform.social.core.manager.ActivityManager;
import org.exoplatform.social.core.manager.IdentityManager;
import org.exoplatform.social.core.processor.I18NActivityProcessor;
import org.exoplatform.social.core.profileproperty.ProfilePropertyService;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.upload.UploadService;
import org.exoplatform.wiki.jpa.BaseTest;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.model.Wiki;

import io.meeds.notes.mcp.NoteMcpTool;
import io.meeds.notes.plugin.NotePublicationPlugin;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

/**
 * Round trips the note MCP tool's language resolution through the HSQLDB test
 * datasource, whose {@code p.lang = :lang} comparison is case-sensitive as on
 * PostgreSQL: a region-qualified code is found as stored, and a wrong-case
 * code reads and writes the translation stored under its other case.
 */
public class TestNoteServiceMcpLanguage extends BaseTest {

  private static final String PORTAL_NAME = "classic";

  private NoteService         noteService;

  private NoteMcpTool         tool;

  /**
   * Builds the tool on the kernel container's services, as root.
   *
   * @throws Exception when the container can't start
   */
  @Override
  public void setUp() throws Exception {
    super.setUp();
    noteService = getService(NoteService.class);
    tool = new NoteMcpTool(getService(WikiService.class),
                           noteService,
                           getService(ActivityManager.class),
                           getService(IdentityManager.class),
                           getService(I18NActivityProcessor.class),
                           getService(SpaceService.class),
                           getService(TranslationService.class),
                           getService(ProfilePropertyService.class),
                           getService(UserACL.class),
                           getService(UserPortalConfigService.class),
                           getService(PermanentLinkService.class),
                           getService(UploadService.class),
                           getService(AttachmentService.class),
                           getService(FileService.class),
                           getService(LayoutService.class),
                           new StaticListableBeanFactory().getBeanProvider(NotePublicationPlugin.class));
    startSessionAs("root");
  }

  /**
   * A translation stored as "es_ES" is read by that code, and one stored as
   * "fr" is read and updated by "FR", which writes no "FR" translation.
   *
   * @throws Exception when a note call fails
   */
  public void testLanguageResolvesToTheStoredTranslationCode() throws Exception {
    Wiki wiki = getOrCreateWiki(getService(WikiService.class), PortalConfig.PORTAL_TYPE, PORTAL_NAME);
    Page note = noteService.createNote(wiki, "Home", new Page("mcpLanguage", "mcpLanguage"), new Identity("root"));
    note = noteService.getNoteById(note.getId());
    note.setLang("es_ES");
    note.setTitle("Título");
    noteService.createVersionOfNote(note, "root");
    note.setLang("fr");
    note.setTitle("Titre");
    noteService.createVersionOfNote(note, "root");
    long noteId = Long.parseLong(note.getId());

    assertEquals("Título", tool.getNote(noteId, "es_ES", null, null, null).title());
    assertEquals("Titre", tool.getNote(noteId, "FR", null, null, null).title());

    tool.updateNote(noteId, "Titre modifié", null, "FR");

    assertEquals("Titre modifié", tool.getNote(noteId, "fr", null, null, null).title());
    List<String> languages = noteService.getPageAvailableTranslationLanguages(noteId, false);
    assertEquals(2, languages.size());
    assertTrue(languages.contains("fr"));
    assertTrue(languages.contains("es_ES"));
    assertFalse(languages.contains("FR"));
  }

}
