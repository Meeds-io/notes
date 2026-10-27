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
package io.meeds.notes.analytics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import org.exoplatform.notes.listener.analytics.NotesPageListener;
import org.exoplatform.wiki.ext.impl.WikiSpaceActivityPublisher;

import tools.jackson.databind.ObjectMapper;

/**
 * The collaborative activity declaration analytics reads from this jar: a
 * note counts once, through the content event its publication records; the
 * stream post of a note is left out by its activity type; the name notes
 * recorded before 7.0.0-exo, noteCreated, keeps counting as a legacy name.
 */
class CollaborativeActivityDeclarationTest {

  private static final String DECLARATION_FILE = "analytics-collaborative-activities.json";

  @Test
  @SuppressWarnings("unchecked")
  void theDeclarationCountsThePublicationAndItsLegacyNameAndExcludesTheNoteStreamPost() throws IOException {
    Map<String, Object> declaration;
    try (InputStream content = getClass().getClassLoader().getResourceAsStream(DECLARATION_FILE)) {
      assertNotNull(content, "the declaration ships at the root of notes-service, which lands in /lib");
      declaration = new ObjectMapper().readValue(content, Map.class);
    }

    assertEquals(List.of("contents"), declaration.get("modules"));
    assertEquals(List.of(NotesPageListener.WIKI_ADD_PAGE_OPERATION), declaration.get("operations"));
    assertEquals(List.of("noteCreated"), declaration.get("legacyOperations"), "the name emitted until 7.0.0-exo, never again");
    assertEquals(List.of(WikiSpaceActivityPublisher.WIKI_APP_ID), declaration.get("excludedActivityTypes"));
    assertFalse(declaration.containsKey("browserOperations"));

    Map<String, Object> labels = (Map<String, Object>) declaration.get("labels");
    assertEquals(List.of("notes.collaborativeActivity.help.counted.publishNote",
                         "notes.collaborativeActivity.help.counted.importNotes"),
                 labels.get("counted"));
    assertEquals(List.of("notes.collaborativeActivity.help.notCounted.draftNote",
                         "notes.collaborativeActivity.help.notCounted.editNote"),
                 labels.get("notCounted"),
                 "one entry per user-facing action; the keys are matched by hand against notes-webapp's Portlets_en.properties");
  }

}
