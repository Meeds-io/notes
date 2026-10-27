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
package io.meeds.notes.mcp.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A note found by {@code search_notes}: enough to recognise it and read it
 * with {@code get_note}, without its content.
 *
 * @param noteId the note id
 * @param title the note title
 * @param summary the note summary, when it has one
 * @param url the note's absolute URL, when it can be resolved
 * @param updatedDate the date the note was last updated
 * @param space the space whose notebook holds the note, absent for a personal
 *          note
 */
@JsonInclude(value = Include.NON_EMPTY)
public record NoteSearchHitModel(
                                 @JsonProperty("note_id")
                                 long noteId,
                                 String title,
                                 String summary,
                                 String url,
                                 @JsonProperty("updated_date")
                                 String updatedDate,
                                 NoteSpaceModel space) {
}
