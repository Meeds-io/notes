import NotesEditorDashboard from './components/NotesEditorDashboard.vue';

const components = {
  'notes-editor-dashboard': NotesEditorDashboard,
};

for (const key in components) {
  Vue.component(key, components[key]);
}