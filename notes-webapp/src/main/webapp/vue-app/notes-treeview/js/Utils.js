/*
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

// the absolute URL of a note, or of a draft with its trailing `/draft`, under the notes page
export function getNoteUrl(noteId, isDraft) {
  const notePath = `/${noteId}${isDraft ? '/draft' : ''}`;
  const nodeUri = eXo.env.portal.selectedNodeUri;
  if (nodeUri === 'notes-editor') {
    // the notes page the editor names, as NotesEditorDashboard#notePageUri reads it
    // a notePageUri sent from a space's notes page is a node of that space's site
    const urlParams = new URLSearchParams(window.location.search);
    const notePageUri = urlParams.get('notePageUri');
    const spaceGroupId = urlParams.get('spaceGroupId');
    if (notePageUri && spaceGroupId) {
      return `${eXo.env.portal.context}/g/:spaces:${spaceGroupId}/${notePageUri}${notePath}`;
    }
    return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/${notePageUri || 'dashboard/notes'}${notePath}`;
  }
  // the notes page URL is <site path>/<selectedNodeUri>[/<noteId>[/draft]], on a portal site as on a space site
  const path = `${window.location.pathname}/`;
  const pageIndex = path.lastIndexOf(`/${nodeUri}/`);
  const sitePath = pageIndex >= 0 ? path.substring(0, pageIndex) : `${eXo.env.portal.context}/${eXo.env.portal.portalName}`;
  return `${sitePath}/${nodeUri}${notePath}`;
}
