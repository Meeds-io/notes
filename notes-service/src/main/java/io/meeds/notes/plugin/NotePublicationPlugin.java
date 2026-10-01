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

import org.exoplatform.services.security.Identity;
import org.exoplatform.wiki.model.Page;

/**
 * Extension point letting an addon that depends on Notes take over the
 * publication of an existing note in its space activity stream, so that a
 * publication made outside the Notes UI (an MCP client for instance) follows
 * the same business rules as the UI publication drawer.
 * <p>
 * Contributions are collected from every Spring context: an implementation
 * must be a non-final {@code @Service} bean. When no contribution handles the
 * note, the caller falls back to the legacy Notes publication. An exception
 * thrown by a contribution is final: the caller doesn't fall back, so a
 * contribution refusing the publication decides who can publish the note.
 */
public interface NotePublicationPlugin {

  /**
   * Publishes the note in its space activity stream.
   *
   * @param note the existing note to publish, already checked as editable by
   *          the given identity
   * @param identity the identity of the user publishing the note
   * @return the identifier of the activity displaying the published note, or
   *         null when this plugin doesn't handle the publication of the note
   * @throws IllegalAccessException when the user isn't allowed to publish the
   *           note
   * @throws Exception when the publication fails
   */
  String publishNote(Page note, Identity identity) throws Exception; // NOSONAR

}
