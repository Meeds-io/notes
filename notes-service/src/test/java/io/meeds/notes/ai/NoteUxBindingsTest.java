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
package io.meeds.notes.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The assistant entries Notes ships for a note's Properties (EXO-94358 for
 * the summary, EXO-94368 for the illustration): one
 * place, whose every request is a chip of the Properties' « Ask AI », each
 * binding with its name and prompts in Notes' English bundle, and the place
 * in the AI administration. The Properties open the chat with the note's
 * content as it is on screen and no note id, so the chat fills
 * {@code {content_body}} from that content and never {@code {content_id}} or
 * {@code {content_link}}.
 */
class NoteUxBindingsTest {

  private static final Path WEBAPP_RESOURCES = Path.of("..", "notes-webapp", "src", "main", "resources", "locale", "portlet");

  private static final Path CHAT_BUNDLE = WEBAPP_RESOURCES.resolve("AiAgentChat_en.properties");

  private static final Path ADMINISTRATION_BUNDLE = WEBAPP_RESOURCES.resolve("AiAgentAdministration_en.properties");

  private static final Path ADMINISTRATION_EXTENSIONS = Path.of("..",
                                                                "notes-webapp",
                                                                "src",
                                                                "main",
                                                                "webapp",
                                                                "vue-app",
                                                                "notes-ai-ux-bindings",
                                                                "extensions.js");

  /**
   * The summary entry, on the CONTENT agent, at
   * the Properties' place, selecting only {@code get_note}, a tool Notes
   * defines: the summary is written into the Properties' field and saved by
   * their Save, never by a tool. Its version is pinned: a stored binding is
   * re-imported only when its version changes, which replaces an
   * administrator's edits of it (AiUxBindingDataInitializer#importUxBindings),
   * and a version another lineage already shipped is never re-imported.
   *
   * @throws Exception when a definition cannot be read
   */
  @Test
  void theSummaryEntryIsPinnedToItsPlaceAgentAndTool() throws Exception {
    JsonNode bindings = read("/ai-ux-bindings.json").get("bindings");
    Set<String> tools = new HashSet<>();
    read("/ai-tool-definitions.json").get("tools").forEach(tool -> tools.add(tool.get("name").asString()));

    List<String> nameIds = new ArrayList<>();
    bindings.forEach(binding -> nameIds.add(binding.get("nameId").asString()));
    assertEquals(List.of("Note-Summary", "Note-Illustration"), nameIds);

    JsonNode summary = bindings.get(0);
    assertEquals("2-exo-94358", summary.get("version").asString(), "EXO-94358: the summary entry's version, which a changed definition must change to be re-imported");
    assertEquals("notes", summary.get("applicationId").asString());
    assertEquals("notes-properties", summary.get("extensionId").asString());
    assertEquals("CONTENT", summary.get("agentNameId").asString());
    assertTrue(summary.get("enabled").asBoolean());
    assertEquals("[\"*:/platform/users\"]", summary.get("permissions").toString());
    assertEquals("[\"get_note\"]", summary.get("toolNames").toString(), "a binding without toolNames gets its agent's whole list");
    summary.get("toolNames").forEach(tool -> assertTrue(tools.contains(tool.asString()), "Note-Summary selects " + tool));
  }

  /**
   * The illustration entry, on the IMAGE agent, at the Properties' place,
   * selects no tool, as the assistant's own image entry: the image agent has
   * no tool support, so the fallback that hands a binding without toolNames
   * its agent's whole list does not apply. Its image goes into the
   * Properties' crop window and is saved by their Save. Its version is
   * pinned for the same reason as the summary's.
   *
   * @throws Exception when a definition cannot be read
   */
  @Test
  void theIllustrationEntryIsPinnedToThePropertiesPlaceAndTheImageAgent() throws Exception {
    JsonNode illustration = read("/ai-ux-bindings.json").get("bindings").get(1);
    assertEquals("Note-Illustration", illustration.get("nameId").asString());
    assertEquals("2-exo-94368", illustration.get("version").asString(), "EXO-94368 ships the illustration entry at the Properties' one place");
    assertEquals("notes", illustration.get("applicationId").asString());
    assertEquals("notes-properties", illustration.get("extensionId").asString());
    assertEquals("IMAGE", illustration.get("agentNameId").asString());
    assertTrue(illustration.get("enabled").asBoolean());
    assertEquals("[\"*:/platform/users\"]", illustration.get("permissions").toString());
    assertTrue(illustration.path("toolNames").isMissingNode(), "the image agent selects no tool");
  }

  /**
   * Each binding has its name and prompts in the bundle; its prompt names
   * the note's content once, as the chat fills only the first one, and its
   * output prompt, sent after the content, names it not at all. Neither
   * names the note's id or link, which the Properties never give.
   *
   * @throws Exception when a definition cannot be read
   */
  @Test
  void eachBindingHasItsTextsAndNamesTheContentOnce() throws Exception {
    JsonNode bindings = read("/ai-ux-bindings.json").get("bindings");
    Properties bundle = load(CHAT_BUNDLE);
    for (JsonNode binding : bindings) {
      String nameId = binding.get("nameId").asString();
      String prefix = "AiAgentChat.uxBindings." + nameId + ".";
      for (String key : List.of("name", "promptTemplate", "outputPromptTemplate")) {
        assertFalse(bundle.getProperty(prefix + key, "").isBlank(), prefix + key);
      }
      String prompt = bundle.getProperty(prefix + "promptTemplate");
      String outputPrompt = bundle.getProperty(prefix + "outputPromptTemplate");
      assertEquals(1, count(prompt, "{content_body}"), nameId + " names the note's content once");
      assertEquals(0, count(outputPrompt, "{content_body}"), nameId + " names the note's content in its output prompt");
      for (String placeholder : List.of("{content_id}", "{content_link}", "{document_content}")) {
        assertEquals(0, count(prompt + "\n" + outputPrompt, placeholder), nameId + " names " + placeholder);
      }
    }
  }

  private static final Path PROPERTIES_OPENER = Path.of("..",
                                                        "notes-webapp",
                                                        "src",
                                                        "main",
                                                        "webapp",
                                                        "vue-app",
                                                        "notes-rich-editor",
                                                        "js",
                                                        "NoteAssistant.js");

  /**
   * The Properties' « Ask AI » reads the entries at the binding's place: an
   * opener reading another place than the binding declares shows
   * nothing, with nothing failing, since the bindings a client receives
   * carry no name to match them by.
   *
   * @throws Exception when a definition cannot be read
   */
  @Test
  void thePropertiesReadTheEntriesAtTheirBindingsPlace() throws Exception {
    String opener = Files.readString(PROPERTIES_OPENER);
    for (JsonNode binding : read("/ai-ux-bindings.json").get("bindings")) {
      String nameId = binding.get("nameId").asString();
      assertTrue(opener.contains("PROPERTIES_APPLICATION_ID = '" + binding.get("applicationId").asString() + "'"),
                 nameId + ": the opener reads the binding's application");
      assertTrue(opener.contains("PROPERTIES_EXTENSION_ID = '" + binding.get("extensionId").asString() + "'"),
                 nameId + ": the opener reads the binding's place");
    }
  }

  /**
   * Each binding's place is registered in the AI administration under
   * Notes, with its label in the administration bundle, so that an
   * administrator finds, turns off, rewords or restores its entries.
   *
   * @throws Exception when a definition cannot be read
   */
  @Test
  void eachPlaceIsRegisteredInTheAdministration() throws Exception {
    JsonNode bindings = read("/ai-ux-bindings.json").get("bindings");
    String extensions = Files.readString(ADMINISTRATION_EXTENSIONS);
    Properties administration = load(ADMINISTRATION_BUNDLE);
    for (JsonNode binding : bindings) {
      String extensionId = binding.get("extensionId").asString();
      assertTrue(extensions.contains("extensionId: '" + extensionId + "'"), extensionId + " is registered");
    }
    assertTrue(extensions.contains("parentId: 'notes'"));
    assertTrue(extensions.contains("nameKey: 'aiAgentAdmin.uxBindings.notesProperties'"));
    assertFalse(administration.getProperty("aiAgentAdmin.uxBindings.notesProperties", "").isBlank());
  }

  /**
   * Reads a JSON resource of this jar.
   *
   * @param resource the resource's path on the classpath
   * @return the parsed document
   * @throws Exception when the resource cannot be read
   */
  private JsonNode read(String resource) throws Exception {
    try (InputStream input = getClass().getResourceAsStream(resource)) {
      assertNotNull(input, resource);
      return JsonMapper.builder().build().readTree(input);
    }
  }

  /**
   * Loads a properties bundle of the webapp module.
   *
   * @param path the bundle's path, relative to this module
   * @return the bundle's keys
   * @throws Exception when the bundle cannot be read
   */
  private Properties load(Path path) throws Exception {
    Properties properties = new Properties();
    try (InputStream input = Files.newInputStream(path)) {
      properties.load(input);
    }
    return properties;
  }

  /**
   * Counts the occurrences of a placeholder in a text.
   *
   * @param text the text
   * @param placeholder the placeholder, matched literally
   * @return how many times the text names it
   */
  private int count(String text, String placeholder) {
    return text.split(Pattern.quote(placeholder), -1).length - 1;
  }
}
