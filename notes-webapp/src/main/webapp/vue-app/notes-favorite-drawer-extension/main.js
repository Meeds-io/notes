import './initComponents.js';
import {initNotesExtensions} from './extensions.js';

import * as notesService from '../../javascript/eXo/wiki/notesService.js';

if (!Vue.prototype.$notesService) {
  window.Object.defineProperty(Vue.prototype, '$notesService', {
    value: notesService,
  });
}

export function init() {
  initNotesExtensions();
}