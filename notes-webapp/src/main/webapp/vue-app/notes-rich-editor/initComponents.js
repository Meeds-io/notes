import NoteCustomPlugins from '../notes-rich-editor/components/NoteCustomPlugins.vue';
import TranslationsEditBar from '../notes-rich-editor/components/TranslationsEditBar.vue';
import NoteTreeviewDrawer from '../notes/components/NoteTreeviewDrawer.vue';
import NoteEditorTopBar from '../notes-rich-editor/components/NoteEditorTopBar.vue';
import NoteFullRichEditor from './components/NoteFullRichEditor.vue';
import NoteEditorMetadataDrawer from './components/NoteEditorMetadataDrawer.vue';
import NoteRichEditorInput from './components/NoteRichEditorInput.vue';

const components = {
  'note-custom-plugins': NoteCustomPlugins,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-translation-edit-bar': TranslationsEditBar,
  'note-editor-top-bar': NoteEditorTopBar,
  'note-full-rich-editor': NoteFullRichEditor,
  'note-editor-metadata-drawer': NoteEditorMetadataDrawer,
  'note-rich-editor-input': NoteRichEditorInput,
};

for (const key in components) {
  Vue.component(key, components[key]);
}


import * as notesService from '../../javascript/eXo/wiki/notesService.js';
import * as noteUtils from './js/Utils.js';

if (!Vue.prototype.$notesService) {
  window.Object.defineProperty(Vue.prototype, '$notesService', {
    value: notesService,
  });
}

if (!Vue.prototype.$noteUtils) {
  window.Object.defineProperty(Vue.prototype, '$noteUtils', {
    value: noteUtils,
  });
}
