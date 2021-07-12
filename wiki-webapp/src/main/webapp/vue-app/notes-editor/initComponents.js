import NotesEditorDashboard from './components/NotesEditorDashboard.vue';
import NoteCustomPlugins from './components/NoteCustomPlugins.vue';


const components = {
  'notes-editor-dashboard': NotesEditorDashboard,
  'note-custom-plugins': NoteCustomPlugins,
};

for (const key in components) {
  Vue.component(key, components[key]);
}