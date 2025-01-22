/*
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
import './services.js';
import './initComponents.js';

export function init() {
  extensionRegistry.registerExtension('notesEditor', 'snackbar-extension', {
    type: 'termsAndConditions',
    options: {
      name: 'termsAndConditions',
      displayMessage: (vm) => {
        document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
          alertMessage: vm.$t('notes.content.save.success.message'),
          alertType: 'success',
          alertLinkText: vm.$t('notes.publication.publish.save.label'),
          alertLinkCallback: () => this.updatePublishedSetting(vm),
        }}));
      },
    },
  });
  extensionRegistry.registerComponent('NotesRichEditor', 'notes-editor-extensions', {
    id: 'termsAndConditionsReminder',
    name: 'termsAndConditions',
    vueComponent: Vue.options.components['terms-and-conditions-reminder'],
    isEnabled: (params) => params?.name === 'termsAndConditions',
    rank: 50,
  });
}

export function updatePublishedSetting(vm) {
  return Vue.prototype.$termsAndConditionsService.updateTermsAndConditionsSettings(true, eXo.env.portal.language || 'en')
    .then(() => {
      document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
        alertMessage: vm.$t('notes.content.publish.success.message'),
        alertType: 'success',
      }}));
    });
}
