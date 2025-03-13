/*
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
*/

extensionRegistry.registerExtension('AnalyticsSamples', 'SampleItem', {
  type: 'noteContentTitle',
  options: {
    rank: 80,
    vueComponent: Vue.options.components['note-content-type-attribute'],
    match: (fieldName) => {
      return fieldName === 'contentTitle';
    }
  },
});

extensionRegistry.registerExtension('AnalyticsChart', 'FieldValueName', {
  type: 'noteContentTitle',
  match: (fieldName, fieldValue) => {
    return fieldName === 'contentTitle' && /^[a-z_]+::[a-z_]+::[a-z_]+$/.test(fieldValue);
  },
  getLabel: (fieldName, fieldValue) => {
    return exoi18n.i18n.t('analytics.snv.title', {0: fieldValue.split('::')[2]});
  }
});
