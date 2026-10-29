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
package io.meeds.notes.util;

import static io.meeds.layout.service.ApplicationSettingsService.VALUE_NOT_ACCEPTED;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.exoplatform.services.resources.LocaleConfigService;

/**
 * Reads and checks the values of an application settings object as the
 * layout tools pass them: JSON values, every refusal being
 * {@code value is not accepted for <option>}
 */
public class ApplicationSettingsUtils {

  private ApplicationSettingsUtils() {
    // Utility class
  }

  public static IllegalArgumentException notAccepted(String option) {
    return new IllegalArgumentException(String.format(VALUE_NOT_ACCEPTED, option));
  }

  public static void requireKnownOptions(Map<String, Object> settings, Collection<String> options) {
    for (String option : settings.keySet()) {
      if (!options.contains(option)) {
        throw notAccepted(option);
      }
    }
  }

  public static String toText(Object value, String option) {
    if (value instanceof String text) {
      return text;
    }
    throw notAccepted(option);
  }

  /**
   * @return the language tags the platform offers
   */
  public static Set<String> getLanguages(LocaleConfigService localeConfigService) {
    return localeConfigService.getLocalConfigs()
                              .stream()
                              .map(config -> config.getLocale().toLanguageTag())
                              .collect(Collectors.toSet());
  }

  public static Map<String, Object> objectSchema(Map<String, Object> properties) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", "object");
    schema.put("properties", properties);
    schema.put("additionalProperties", false);
    return schema;
  }

  public static Map<String, Object> stringSchema(String description) {
    return schema("string", description);
  }

  public static Map<String, Object> integerSchema(String description) {
    return schema("integer", description);
  }

  private static Map<String, Object> schema(String type, String description) {
    Map<String, Object> schema = new LinkedHashMap<>();
    schema.put("type", type);
    schema.put("description", description);
    return schema;
  }

}
