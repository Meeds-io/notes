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

@JsonInclude(value = Include.NON_EMPTY)
public record NoteModel(
                        @JsonProperty("note_id")
                        long noteId,
                        String title,
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
                        List<NoteBreadcrumbModel> breadcrumb) {
}
