/**
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io
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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import io.meeds.mcp.server.tool.model.UserModel;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A note, as {@code get_note} and the note writes return it. A part of a long
 * note carries the part's bounds in its HTML content; a version of the note
 * carries the version's number.
 *
 * @param noteId the note id
 * @param title the note title
 * @param summary the note summary
 * @param htmlContent the note's HTML content, or the part of it read
 * @param url the note's absolute URL
 * @param createdDate the date the note was created
 * @param updatedDate the date the note, or the version read, was updated
 * @param hasChildNotes whether the note has sub-notes
 * @param canEdit whether the current user can edit the note
 * @param author the note author
 * @param lastUpdater the last user who updated the note, or the version's
 *          author
 * @param breadcrumb the note's ancestors, from the notebook's home note
 * @param versionNumber the number of the version read, null for the current
 *          note
 * @param offset the character of the HTML content the part starts at, null
 *          when the whole content is returned
 * @param endOffset the character after the part's last one, null when the
 *          whole content is returned
 * @param totalChars the length of the whole HTML content, null when the whole
 *          content is returned
 * @param nextOffset where the next part starts, null when this part ends the
 *          content or the whole content is returned
 */
@JsonInclude(value = Include.NON_EMPTY)
public record NoteModel(
                        @JsonProperty("note_id")
                        long noteId,
                        String title,
                        String summary,
                        @JsonProperty("html_content")
                        String htmlContent,
                        String url,
                        @JsonProperty("created_date")
                        String createdDate,
                        @JsonProperty("updated_date")
                        String updatedDate,
                        @JsonProperty("has_child_notes")
                        boolean hasChildNotes,
                        @JsonProperty("can_edit")
                        boolean canEdit,
                        UserModel author,
                        @JsonProperty("last_updater")
                        UserModel lastUpdater,
                        List<NoteBreadcrumbModel> breadcrumb,
                        @JsonProperty("version_number")
                        Long versionNumber,
                        Integer offset,
                        @JsonProperty("end_offset")
                        Integer endOffset,
                        @JsonProperty("total_chars")
                        Integer totalChars,
                        @JsonProperty("next_offset")
                        Integer nextOffset) {
}
