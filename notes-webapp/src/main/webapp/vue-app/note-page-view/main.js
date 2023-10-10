/*
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io
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

import './initComponents.js';
import './services.js';

if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('NotePageView');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const lang = eXo.env.portal.language;
const url = `${eXo.env.portal.context}/${eXo.env.portal.rest}/i18n/bundle/locale.portlet.NotePageView-${lang}.json`;

export function init(appId, name, canEdit) {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
      Vue.createApp({
        data: {
          name,
          canEdit,
          notePage: null,
          language: lang,
          defaultLanguage: 'en',
          initialized: false,
        },
        created() {
          this.init().finally(() => this.initialized = true);
          this.$on('notes-refresh', this.init);
        },
        methods: {
          init() {
            return this.$notePageViewService.getNotePage(this.name, this.language)
              .then(settings => this.settings = settings);
          },
        },
        template: `<note-page-view-app id="${appId}"></note-page-view-app>`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n,
      }, `#${appId}`, `Note Page View - ${name}`);
    });
}