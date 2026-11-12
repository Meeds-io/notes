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
package org.exoplatform.wiki.model;

/**
 * One page of a notebook's tree, as stored: no content, no title, no
 * version, nothing a reader needs to display the page. It answers where a
 * page sits and who owns it, for a caller that walks a whole notebook.
 *
 * @param id the page id
 * @param parentId the id of the parent page, null for the notebook's Home
 * @param name the page name, {@code WikiHome} for the Home
 * @param owner the page owner, blank for a page that has none
 */
public record PageTreeEntry(long id, Long parentId, String name, String owner) {
}
