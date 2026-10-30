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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import org.exoplatform.social.core.space.model.Space;
import org.exoplatform.social.core.space.spi.SpaceService;
import org.exoplatform.wiki.model.Page;
import org.exoplatform.wiki.service.NoteService;

import io.meeds.mcp.server.constant.Origin;
import io.meeds.mcp.server.model.ActingIdentity;
import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.model.SpaceScopeDeclaration;
import io.meeds.mcp.server.model.SpaceScopeRefusedException;
import io.meeds.mcp.server.plugin.SpaceScopeResolver;
import io.meeds.mcp.server.service.McpToolSpaceScopeService;
import io.meeds.mcp.server.util.McpToolUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins the {@code space_scope} declarations the notes add-on ships: which
 * notes tools a call limited to one space may use, on which arguments.
 */
public class NoteToolDefinitionSpaceScopeTest {

  private static final Set<String>         SPACE_TOOLS    = Set.of("get_space_note_tree", "create_space_note", "search_notes");

  private static final Set<String>         OBJECT_TOOLS   = Set.of("get_note",
                                                                   "get_note_versions",
                                                                   "get_note_translations",
                                                                   "create_child_note",
                                                                   "update_note",
                                                                   "publish_note",
                                                                   "delete_note",
                                                                   "restore_note_version",
                                                                   "set_note_summary",
                                                                   "remove_note_cover",
                                                                   "remove_note_translation",
                                                                   "move_note");

  private static final Set<String>         WRITES         = Set.of("create_space_note",
                                                                   "create_child_note",
                                                                   "update_note",
                                                                   "publish_note",
                                                                   "delete_note",
                                                                   "restore_note_version",
                                                                   "set_note_summary",
                                                                   "remove_note_cover",
                                                                   "remove_note_translation",
                                                                   "move_note");

  /** Tools that reach other notes or places than the one their object argument names. */
  private static final Set<String>         LEFT_OUT       = Set.of("create_personal_note", "set_note_cover");

  private static final Map<String, String> OBJECT_ARGS    = Map.of("create_child_note", "parent_note_id");

  private static final JsonMapper          MAPPER         = JsonMapper.builder().build();

  /**
   * Exactly the expected notes tools declare a space scope, well formed for
   * what they are, a read or a write, and the writes need an approval. A tool
   * added, or one that stops declaring, fails here.
   */
  @Test
  public void declaredToolsAreTheExpectedOnes() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    assertEquals(Set.copyOf(Stream.concat(SPACE_TOOLS.stream(), OBJECT_TOOLS.stream()).toList()),
                 declared.keySet());
    declared.forEach((name, tool) -> {
      boolean write = WRITES.contains(name);
      SpaceScopeDeclaration scope = tool.getSpaceScope();
      assertNotNull(name, tool.getAnnotations());
      assertEquals(name, !write, McpToolSpaceScopeService.isReadTool(tool));
      assertEquals(name, write, tool.isRequireApproval());
      assertTrue(name, scope.isWellFormedFor(write));
      assertFalse(name, scope.isUnscopedKind());
    });
  }

  /**
   * Tools that can reach another space than the one their arguments name stay
   * undeclared, so a limited call refuses them: the personal notebook and a
   * cover fetched from a URL.
   */
  @Test
  public void leftOutToolsStayUndeclared() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    LEFT_OUT.forEach(name -> assertFalse(name, declared.containsKey(name)));
    Set<String> shipped = allTools().keySet();
    assertTrue(shipped.containsAll(LEFT_OUT));
    assertEquals(shipped.size(), declared.size() + LEFT_OUT.size());
  }

  /**
   * The space tools name the {@code space_id} argument; only the read that
   * searches may leave it out, to read the call's space; the write names it
   * and has no {@code when_absent}.
   */
  @Test
  public void spaceToolsNameTheSpaceArgument() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    SPACE_TOOLS.forEach(name -> {
      SpaceScopeDeclaration scope = declared.get(name).getSpaceScope();
      assertTrue(name, scope.isSpaceKind());
      assertEquals(name, "space_id", scope.getSpaceArg());
      assertEquals(name, "search_notes".equals(name), scope.isUseScopeWhenAbsent());
    });
    assertEquals(List.of("is_favorites"), declared.get("search_notes").getSpaceScope().getFalseOnlyArgs());
    assertEquals(List.of("query", "offset", "limit"), allowed(declared.get("search_notes")));
    assertEquals(List.of("title", "summary", "html_content"), allowed(declared.get("create_space_note")));
    assertEquals(List.of(), allowed(declared.get("get_space_note_tree")));
  }

  /**
   * The note tools reach their note through the {@value NoteSpaceScopeResolver#NAME}
   * resolver, by the argument that names the note they change or read: the
   * parent for a child note, the note otherwise. Each tool accepts nothing
   * that names another note or a space, but the move: its new parent is a
   * second note, which {@code NoteMcpTool#moveNote} accepts in a limited call
   * only when it belongs to the call's space.
   */
  @Test
  public void noteToolsResolveTheirNote() {
    Map<String, SimpleToolDefinition> declared = declaredTools();

    OBJECT_TOOLS.forEach(name -> {
      SpaceScopeDeclaration scope = declared.get(name).getSpaceScope();
      assertTrue(name, scope.isObjectKind());
      assertEquals(name, OBJECT_ARGS.getOrDefault(name, "note_id"), scope.getObjectArg());
      assertEquals(name, NoteSpaceScopeResolver.NAME, scope.getResolver());
      assertNull(name, scope.getSpaceArg());
      assertTrue(name, scope.getFalseOnlyArgs() == null || scope.getFalseOnlyArgs().isEmpty());
      allowed(declared.get(name)).stream()
                                 .filter(argument -> !("move_note".equals(name) && "target_parent_note_id".equals(argument)))
                                 .forEach(argument -> assertFalse(name + " takes " + argument, argument.endsWith("_id")));
    });
    assertEquals(List.of("language"), allowed(declared.get("get_note")));
    assertEquals(List.of(), allowed(declared.get("delete_note")));
    assertEquals(List.of(), allowed(declared.get("publish_note")));
    assertEquals(List.of("title", "html_content", "language"), allowed(declared.get("update_note")));
    assertEquals(List.of("target_parent_note_id"), allowed(declared.get("move_note")));
  }

  /**
   * Every argument a limited call may carry is a property of the tool's input
   * schema, so a renamed argument can't leave a declaration accepting
   * nothing, or something else.
   */
  @Test
  public void acceptedArgumentsAreSchemaProperties() {
    declaredTools().forEach((name, tool) -> {
      JsonNode properties = MAPPER.readTree(tool.getInputSchema()).path("properties");
      SpaceScopeDeclaration scope = tool.getSpaceScope();
      String own = scope.isSpaceKind() ? scope.getSpaceArg() : scope.getObjectArg();
      assertTrue(name + " takes " + own, properties.has(own));
      allowed(tool).forEach(argument -> assertTrue(name + " takes " + argument, properties.has(argument)));
      if (scope.getFalseOnlyArgs() != null) {
        scope.getFalseOnlyArgs().forEach(argument -> assertTrue(name + " takes " + argument, properties.has(argument)));
      }
    });
  }

  /**
   * Through the real space scope check and the real
   * {@value NoteSpaceScopeResolver#NAME} resolver, an approved proposal of a
   * run limited to space 12 moves a note of space 12, naming its new parent;
   * the move of a note of space 13 or of a personal note is refused, and so
   * is a call that isn't a proposal or carries an argument the declaration
   * doesn't accept. The new parent itself is checked by {@code NoteMcpTool}.
   * Mutants: the declaration dropped, its resolver or object argument
   * changed, or the new parent left out of the accepted arguments.
   *
   * @throws Exception never, the services are mocks
   */
  @Test
  @SuppressWarnings("unchecked")
  public void moveIsEnforcedUnderASpaceScope() throws Exception {
    NoteService noteService = mock(NoteService.class);
    SpaceService spaceService = mock(SpaceService.class);
    NoteSpaceScopeResolver resolver = new NoteSpaceScopeResolver();
    ReflectionTestUtils.setField(resolver, "noteService", noteService);
    ReflectionTestUtils.setField(resolver, "spaceService", spaceService);
    Space alpha = mock(Space.class);
    when(alpha.getId()).thenReturn("12");
    Space beta = mock(Space.class);
    when(beta.getId()).thenReturn("13");
    when(spaceService.getSpaceByGroupId("/spaces/alpha")).thenReturn(alpha);
    when(spaceService.getSpaceByGroupId("/spaces/beta")).thenReturn(beta);
    Page inSpace = note("group", "/spaces/alpha");
    Page otherSpace = note("group", "/spaces/beta");
    Page personal = note("user", "root");
    when(noteService.getNoteById("5")).thenReturn(inSpace);
    when(noteService.getNoteById("6")).thenReturn(otherSpace);
    when(noteService.getNoteById("4")).thenReturn(personal);
    ObjectProvider<SpaceScopeResolver> provider = mock(ObjectProvider.class);
    when(provider.orderedStream()).thenAnswer(invocation -> Stream.of(resolver));
    McpToolSpaceScopeService service = new McpToolSpaceScopeService(provider);
    ActingIdentity proposal = ActingIdentity.agent("support-desk", "SUPPORT_DESK", Origin.PROPOSAL, 12L);
    ActingIdentity run = ActingIdentity.agent("support-desk", "SUPPORT_DESK", Origin.MENTION, 12L);
    SimpleToolDefinition tool = declaredTools().get("move_note");

    String move = "{\"note_id\":5,\"target_parent_note_id\":7}";
    assertEquals(move, service.enforce("move_note", tool, move, proposal));
    assertThrows(SpaceScopeRefusedException.class, () -> service.enforce("move_note", tool, move, run));
    for (String refused : List.of("{\"note_id\":6,\"target_parent_note_id\":7}",
                                  "{\"note_id\":4,\"target_parent_note_id\":7}",
                                  "{\"note_id\":5,\"target_parent_note_id\":7,\"space_id\":13}")) {
      assertThrows(refused, SpaceScopeRefusedException.class, () -> service.enforce("move_note", tool, refused, proposal));
    }
  }

  /**
   * @param wikiType the type of the note's notebook
   * @param owner its owner, a group id for a space
   * @return a note of that notebook
   */
  private static Page note(String wikiType, String owner) {
    Page note = mock(Page.class);
    when(note.getWikiType()).thenReturn(wikiType);
    when(note.getWikiOwner()).thenReturn(owner);
    return note;
  }

  /**
   * @param tool a tool declaring a space scope
   * @return its allowed arguments, empty when it names none
   */
  private static List<String> allowed(SimpleToolDefinition tool) {
    List<String> allowed = tool.getSpaceScope().getAllowedArgs();
    return allowed == null ? List.of() : allowed;
  }

  /**
   * @return the shipped notes tools that declare a space scope, by name
   */
  private static Map<String, SimpleToolDefinition> declaredTools() {
    return allTools().entrySet()
                     .stream()
                     .filter(entry -> entry.getValue().getSpaceScope() != null)
                     .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
  }

  /**
   * @return every shipped notes tool, by name
   */
  private static Map<String, SimpleToolDefinition> allTools() {
    try {
      URL classes = NoteMcpTool.class.getProtectionDomain().getCodeSource().getLocation();
      URL url = new File(new File(classes.toURI()), "ai-tool-definitions.json").toURI().toURL();
      return McpToolUtils.parseToolDefinitions(url)
                         .stream()
                         .collect(Collectors.toMap(SimpleToolDefinition::getName, tool -> tool));
    } catch (Exception e) {
      throw new IllegalStateException("The notes tool definitions couldn't be read", e);
    }
  }

}
