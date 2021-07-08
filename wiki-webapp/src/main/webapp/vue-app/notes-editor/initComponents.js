import NotesEditorDashboard from './components/NotesEditorDashboard.vue';
import NoteCustomPlugins from './components/NoteCustomPlugins.vue';
import NoteTablePluginsDrawer from './components/NoteTablePluginsDrawer.vue';
import NoteTreeviewDrawer from '../notes/components/NoteTreeviewDrawer.vue';


const components = {
  'notes-editor-dashboard': NotesEditorDashboard,
  'note-custom-plugins': NoteCustomPlugins,
  'note-table-plugins-drawer': NoteTablePluginsDrawer,
  'note-treeview-drawer': NoteTreeviewDrawer,
};

for (const key in components) {
  Vue.component(key, components[key]);
}