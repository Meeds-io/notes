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
 * the places of a note's Properties, one per action, each matched by its
 * applicationId and extensionId: a client tells the actions apart by their
 * place, the bindings it receives carrying no name. An administrator sees,
 * turns off, rewords or restores the entries of each. The variables offered
 * are the ones the Properties fill: the note's content as it is on screen,
 * and its title.
 */
extensionRegistry.registerExtension('ai-agent', 'ux-binding', {
  id: 'notesPropertiesSummary',
  profile: 'notes',
  parentId: 'notes',
  applicationId: 'notes',
  extensionId: 'notes-properties-summary',
  rank: 26,
  nameKey: 'aiAgentAdmin.uxBindings.notesPropertiesSummary',
  component: 'ai-ux-bindings-list',
  availableVariables: ['content_body', 'content_title'],
});
