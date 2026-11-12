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

import java.util.ArrayList;
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
import org.exoplatform.wiki.model.PageTreeEntry;
import org.exoplatform.wiki.model.Wiki;
import org.exoplatform.wiki.service.listener.PageWikiListener;
import org.exoplatform.wiki.utils.NoteConstants;

import io.meeds.notes.mcp.NoteMcpTool;
import io.meeds.notes.plugin.NotePublicationPlugin;
import io.meeds.portal.permlink.service.PermanentLinkService;
import io.meeds.social.translation.service.TranslationService;

/**
 * The assistant's {@code delete_note} on the kernel container's services: it
 * deletes through the deletion the Notes app runs, so every page listener
 * hears of each deleted page. Named after {@code TestNoteService} so that it
 * runs after it, whose hard-coded page ids a note created earlier would
 * shift.
 */
public class TestNoteServiceMcpDelete extends BaseTest {

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
   * A note deleted by the tool reaches a registered page listener for itself
   * and for each note below it, at every depth, and the three notes are gone.
   * The Home is refused with the tool's sentence and stays.
   * Mutant: the tool calls {@code deleteNote(String)} again (no listener
   * hears anything).
   *
   * @throws Exception when a note call fails
   */
  public void testDeleteReachesThePageListenersForTheNoteAndItsSubNotes() throws Exception {
    WikiService wikiService = getService(WikiService.class);
    RecordingDeleteListener listener = new RecordingDeleteListener();
    wikiService.addComponentPlugin(listener);
    Wiki wiki = getOrCreateWiki(wikiService, PortalConfig.PORTAL_TYPE, PORTAL_NAME);
    Identity root = new Identity("root");
    Page parent = noteService.createNote(wiki, NoteConstants.NOTE_HOME_NAME, new Page("mcpDeleteParent", "Parent"), root);
    Page child = noteService.createNote(wiki, parent.getName(), new Page("mcpDeleteChild", "Child"), root);
    Page grandChild = noteService.createNote(wiki, child.getName(), new Page("mcpDeleteGrandChild", "Grand child"), root);

    List<PageTreeEntry> tree = noteService.getPageTreeOfWiki(PortalConfig.PORTAL_TYPE, PORTAL_NAME);
    assertTrue(tree.contains(new PageTreeEntry(Long.parseLong(child.getId()),
                                               Long.parseLong(parent.getId()),
                                               child.getName(),
                                               noteService.getNoteById(child.getId()).getOwner())));
    assertTrue(tree.stream().anyMatch(entry -> entry.parentId() == null
        && NoteConstants.NOTE_HOME_NAME.equals(entry.name())));

    tool.deleteNote(Long.parseLong(parent.getId()));

    assertTrue(noteService.getPageTreeOfWiki(PortalConfig.PORTAL_TYPE, PORTAL_NAME)
                          .stream()
                          .noneMatch(entry -> entry.id() == Long.parseLong(grandChild.getId())));

    assertEquals(List.of(parent.getName(), child.getName(), grandChild.getName()), listener.deletedPageNames);
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, PORTAL_NAME, parent.getName()));
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, PORTAL_NAME, child.getName()));
    assertNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, PORTAL_NAME, grandChild.getName()));

    Page home = noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, PORTAL_NAME, NoteConstants.NOTE_HOME_NAME);
    long homeId = Long.parseLong(home.getId());
    try {
      tool.deleteNote(homeId);
      fail("The Home must not be deleted");
    } catch (IllegalArgumentException e) {
      assertEquals("The home note of a notebook can't be deleted", e.getMessage());
    }
    assertNotNull(noteService.getNoteOfNoteBookByName(PortalConfig.PORTAL_TYPE, PORTAL_NAME, NoteConstants.NOTE_HOME_NAME));
  }

  /** Records the name of every page whose deletion it hears of. */
  private static class RecordingDeleteListener extends PageWikiListener {
    private final List<String> deletedPageNames = new ArrayList<>();

    /** {@inheritDoc} */
    @Override
    public void postDeletePage(String wikiType, String wikiOwner, String pageId, Page page) {
      deletedPageNames.add(pageId);
    }
  }

}
