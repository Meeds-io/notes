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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * A page of the notes {@code search_notes} found.
 *
 * @param notes the notes found, at most {@code usedLimit}
 * @param usedLimit the most notes the page holds, after the requested limit
 *          was clamped
 * @param hasMore whether more notes match after this page
 * @param nextOffset the offset of the next page, null when there is none
 */
@JsonInclude(value = Include.NON_NULL)
public record NoteSearchCollectionModel(
                                        List<NoteSearchHitModel> notes,
                                        @JsonProperty("used_limit")
                                        int usedLimit,
                                        @JsonProperty("has_more")
                                        boolean hasMore,
                                        @JsonProperty("next_offset")
                                        Integer nextOffset) {
}
