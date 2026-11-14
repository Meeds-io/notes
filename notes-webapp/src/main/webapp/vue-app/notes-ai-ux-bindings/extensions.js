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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */

/*
 * The Notes section of the AI administration's ready-made requests gains
 * the place of a note's Properties, matched by its applicationId and
 * extensionId: every request of that place, whichever add-on ships it, is a
 * chip of the Properties' « Ask AI », and an administrator sees, turns off,
 * rewords or restores them here. The variables offered are the ones the
 * Properties fill: the note's content as it is on screen, and its title.
 */
extensionRegistry.registerExtension('ai-agent', 'ux-binding', {
  id: 'notesProperties',
  profile: 'notes',
  parentId: 'notes',
  applicationId: 'notes',
  extensionId: 'notes-properties',
  rank: 26,
  nameKey: 'aiAgentAdmin.uxBindings.notesProperties',
  component: 'ai-ux-bindings-list',
  availableVariables: ['content_body', 'content_title'],
});
