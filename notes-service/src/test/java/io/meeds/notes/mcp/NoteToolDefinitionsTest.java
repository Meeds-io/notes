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
package io.meeds.notes.mcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.Test;

import io.meeds.mcp.server.model.SimpleToolDefinition;
import io.meeds.mcp.server.util.McpToolUtils;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Pins that the notes add-on's tool methods and its shipped
 * {@code ai-tool-definitions.json} match: mcp-server turns a public method
 * into a tool only when a definition carries its snake_case name, and binds
 * each argument by the camelCase of its {@code input_schema} property to the
 * Java parameter of that name, passing null when none has it. Neither is
 * checked by mcp-server at build time or at boot.
 */
public class NoteToolDefinitionsTest {

  /**
   * Every public tool method has a definition, every definition has a
   * method, and every {@code input_schema} property of a definition names a
   * parameter of its method, once turned to camelCase.
   * Mutants: a definition dropped or renamed, a method renamed, a property
   * (upload_id above all) whose parameter is missing or named otherwise.
   */
  @Test
  public void everyToolMethodAndDefinitionMatch() {
    Map<String, Method> methods = Arrays.stream(NoteMcpTool.class.getDeclaredMethods())
                                        .filter(method -> Modifier.isPublic(method.getModifiers()))
                                        .filter(method -> !Modifier.isStatic(method.getModifiers()))
                                        .filter(method -> !method.isSynthetic())
                                        .collect(Collectors.toMap(method -> toSnakeCase(method.getName()), Function.identity()));
    Map<String, SimpleToolDefinition> definitions = shippedTools();

    assertEquals(methods.keySet(), definitions.keySet());
    definitions.forEach((name, definition) -> {
      Set<String> parameters = Arrays.stream(methods.get(name).getParameters())
                                     .map(Parameter::getName)
                                     .collect(Collectors.toSet());
      JsonNode schema = JsonMapper.builder().build().readTree(definition.getInputSchema());
      Set<String> properties = new HashSet<>();
      schema.path("properties").propertyNames().forEach(property -> properties.add(toCamelCase(property)));
      assertTrue(name + ": " + properties + " not all in " + parameters, parameters.containsAll(properties));
    });
    assertTrue(definitions.get("set_note_cover").getInputSchema().contains("\"upload_id\""));
  }

  /**
   * Turns a Java method name into the tool name mcp-server looks up.
   *
   * @param name the camelCase name
   * @return the snake_case name
   */
  private static String toSnakeCase(String name) {
    return name.replaceAll("([A-Z])", "_$1").toLowerCase(Locale.ROOT);
  }

  /**
   * Turns a snake_case argument name into the Java parameter name it binds
   * to.
   *
   * @param name the snake_case name
   * @return the camelCase name
   */
  private static String toCamelCase(String name) {
    StringBuilder camelCase = new StringBuilder();
    boolean upper = false;
    for (char c : name.toCharArray()) {
      if (c == '_') {
        upper = true;
      } else {
        camelCase.append(upper ? Character.toUpperCase(c) : c);
        upper = false;
      }
    }
    return camelCase.toString();
  }

  /**
   * Reads the definitions the module ships.
   *
   * @return every shipped tool definition, by name
   */
  private static Map<String, SimpleToolDefinition> shippedTools() {
    try {
      URL classes = NoteMcpTool.class.getProtectionDomain().getCodeSource().getLocation();
      URL url = new File(new File(classes.toURI()), "ai-tool-definitions.json").toURI().toURL();
      return McpToolUtils.parseToolDefinitions(url)
                         .stream()
                         .collect(Collectors.toMap(SimpleToolDefinition::getName, Function.identity()));
    } catch (Exception e) {
      throw new IllegalStateException("The tool definitions couldn't be read", e);
    }
  }

}
