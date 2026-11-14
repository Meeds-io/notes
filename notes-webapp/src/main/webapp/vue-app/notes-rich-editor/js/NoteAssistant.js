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

/**
 * The assistant's entries Notes offers in a note's Properties, reached
 * through the assistant's own modules at run time: Notes does not depend on
 * the assistant, and offers nothing when it is absent or off.
 */

/** The place of the note's Properties, as its bindings declare it. */
export const PROPERTIES_APPLICATION_ID = 'notes';
export const PROPERTIES_EXTENSION_ID = 'notes-properties';

/** The bindings of the page, by place, read once whatever the forms asking them. */
const BINDINGS = new Map();

/**
 * Loads one of the assistant's shared modules. A module whose script failed
 * to load is forgotten, so that a later call loads it again instead of
 * waiting on the failed one for good.
 *
 * @param {string} moduleName the module, as `SHARED/<name>`
 * @returns {Promise<object>} the module; rejected when it cannot be loaded
 */
function loadModule(moduleName) {
  return new Promise((resolve, reject) => {
    window.require([moduleName], resolve, error => {
      window.require.undef?.(moduleName);
      reject(error);
    });
  });
}

/**
 * Reads the enabled bindings of a place, once per page.
 *
 * @param {string} applicationId the place's application
 * @param {string} extensionId the place's extension
 * @returns {Promise<Array<object>>} the bindings the user may run there;
 *          none when the assistant is off or its module cannot be loaded
 */
export function getBindings(applicationId, extensionId) {
  if (!eXo.env.portal.aiConciergeEnabled) {
    return Promise.resolve([]);
  }
  const key = `${applicationId}/${extensionId}`;
  if (!BINDINGS.has(key)) {
    const bindings = loadModule('SHARED/AiAgentCommon')
      .then(() => Vue.prototype.$aiUxBindingService?.getUxBindings(applicationId, extensionId))
      .then(result => result || [])
      .catch(() => {
        BINDINGS.delete(key);
        return [];
      });
    BINDINGS.set(key, bindings);
  }
  return BINDINGS.get(key);
}

/**
 * Tells whether the assistant may be offered in a space: everywhere outside
 * spaces, and in a space only once it is read as shared with the assistant
 * (not sovereign). Nothing on the server refuses a note's content handed to
 * the assistant in a sovereign space, so a space that cannot be read counts
 * as not shared.
 *
 * @param {number|string} spaceId the note's space, none outside spaces
 * @returns {Promise<boolean>} whether the assistant may be offered
 */
export async function isSpaceSharedWithAssistant(spaceId) {
  if (!spaceId) {
    return true;
  }
  if (!Vue.prototype.$spaceService) {
    return false;
  }
  try {
    const space = await Vue.prototype.$spaceService.getSpaceById(spaceId);
    return space?.sovereign === false;
  } catch (e) {
    return false;
  }
}

/**
 * Opens the assistant with the given options.
 *
 * @param {object} options the chat's options: its bindings, the object it
 *          is about and what « Insert » calls
 * @returns {Promise<void>} resolved once the chat is opened; rejected when
 *          the assistant's module cannot be loaded
 */
export function openAssistant(options) {
  return loadModule('SHARED/AiAgentChat')
    .then(drawer => drawer.openDrawer(options));
}
