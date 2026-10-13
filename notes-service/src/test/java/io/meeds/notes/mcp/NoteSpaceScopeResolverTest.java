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
package io.meeds.notes.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.annotation.Profile;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.WikiException;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

/**
 * Pins the space of a note as the space scope sees it: the space whose group
 * owns the notebook of the note, nothing otherwise.
 */
@RunWith(MockitoJUnitRunner.class)
public class NoteSpaceScopeResolverTest {

  private static final String       NOTE_ID = "5";

  @Mock
  private NoteService               noteService;

  @Mock
  private SpaceService              spaceService;

  @InjectMocks
  private NoteSpaceScopeResolver    resolver;

  /**
   * Gives the resolver a space for one group, and none for another.
   */
  @Before
  public void setUp() {
    Space alpha = mock(Space.class);
    lenient().when(alpha.getId()).thenReturn("12");
    lenient().when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/gone")).thenReturn(null);
  }

  /**
   * The resolver is a bean of the profile the notes tools live in, which is
   * how the space scope finds it. Mutant: the {@code @Profile} annotation
   * removed.
   */
  @Test
  public void resolverIsABeanOfTheProfileOfTheNotesTools() {
    assertTrue(NoteSpaceScopeResolver.class.isAnnotationPresent(Profile.class));
    assertEquals("mcp-server", NoteSpaceScopeResolver.class.getAnnotation(Profile.class).value()[0]);
    assertEquals(NoteMcpTool.class.getAnnotation(Profile.class).value()[0],
                 NoteSpaceScopeResolver.class.getAnnotation(Profile.class).value()[0]);
  }

  /**
   * The name is the one the notes tools' declarations give.
   */
  @Test
  public void nameIsTheOneTheDeclarationsGive() {
    assertEquals("note", resolver.getName());
  }

  /**
   * A note of a space notebook belongs to that space.
   */
  @Test
  public void spaceOfSpaceNote() throws Exception {
    givenNote("group", "/spaces/alpha");
    assertEquals(Long.valueOf(12), resolver.spaceOf(NOTE_ID));
  }

  /**
   * A note of a personal notebook belongs to no space, whatever its owner
   * looks like. Mutant: the wiki type isn't looked at.
   */
  @Test
  public void noSpaceForPersonalNote() throws Exception {
    givenNote("user", "/spaces/alpha");
    assertNull(resolver.spaceOf(NOTE_ID));
    verifyNoInteractions(spaceService);
  }

  /**
   * A note of the portal notebook belongs to no space.
   */
  @Test
  public void noSpaceForPortalNote() throws Exception {
    givenNote("portal", "classic");
    assertNull(resolver.spaceOf(NOTE_ID));
  }

  /**
   * A group notebook of a group outside the spaces, or with no owner, belongs
   * to no space. Mutant: the spaces prefix isn't required.
   */
  @Test
  public void noSpaceForGroupOutsideTheSpaces() throws Exception {
    givenNote("group", "/platform/users");
    assertNull(resolver.spaceOf(NOTE_ID));
    givenNote("group", null);
    assertNull(resolver.spaceOf(NOTE_ID));
    verify(spaceService, never()).getSpaceByGroupId(anyString());
  }

  /**
   * A space that no longer exists, or one with no usable id, gives none.
   */
  @Test
  public void noSpaceWhenTheSpaceIsGone() throws Exception {
    givenNote("group", "/spaces/gone");
    assertNull(resolver.spaceOf(NOTE_ID));
    Space noId = mock(Space.class);
    lenient().when(noId.getId()).thenReturn(null);
    lenient().when(spaceService.getSpaceByGroupId("/spaces/noid")).thenReturn(noId);
    givenNote("group", "/spaces/noid");
    assertNull(resolver.spaceOf(NOTE_ID));
  }

  /**
   * A missing note gives none.
   */
  @Test
  public void noSpaceForMissingNote() throws Exception {
    lenient().when(noteService.getNoteById(NOTE_ID)).thenReturn(null);
    assertNull(resolver.spaceOf(NOTE_ID));
  }

  /**
   * An id that isn't decimal digits gives none and reads nothing.
   */
  @Test
  public void noSpaceForAnIdThatIsNotOne() {
    assertNull(resolver.spaceOf(null));
    assertNull(resolver.spaceOf(""));
    assertNull(resolver.spaceOf("-5"));
    assertNull(resolver.spaceOf("abc"));
    verifyNoInteractions(noteService);
  }

  /**
   * A lookup that fails gives none, never an exception: the call is refused.
   * Mutant: the failure is let through, or answered with the notebook's
   * space.
   */
  @Test
  public void noSpaceWhenTheLookupFails() throws Exception {
    lenient().doThrow(new WikiException("boom")).when(noteService).getNoteById(NOTE_ID);
    assertNull(resolver.spaceOf(NOTE_ID));
    lenient().when(noteService.getNoteById("6")).thenThrow(new IllegalStateException("boom"));
    assertNull(resolver.spaceOf("6"));
    givenNote("group", "/spaces/alpha");
    lenient().doThrow(new IllegalStateException("boom")).when(spaceService).getSpaceByGroupId("/spaces/alpha");
    assertNull(resolver.spaceOf(NOTE_ID));
  }

  /**
   * @param wikiType  the type of the notebook of the note
   * @param wikiOwner the owner of that notebook
   */
  private void givenNote(String wikiType, String wikiOwner) throws Exception {
    Page note = new Page();
    note.setWikiType(wikiType);
    note.setWikiOwner(wikiOwner);
    lenient().doReturn(note).when(noteService).getNoteById(NOTE_ID);
  }

}
