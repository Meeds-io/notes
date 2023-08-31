import NotesEditorDashboard from './components/NotesEditorDashboard.vue';
import NoteCustomPlugins from './components/NoteCustomPlugins.vue';
import NoteTablePluginsDrawer from './components/NoteTablePluginsDrawer.vue';
import TranslationsEditBar from './components/TranslationsEditBar.vue';
import NoteTreeviewDrawer from '../notes/components/NoteTreeviewDrawer.vue';


const components = {
  'notes-editor-dashboard': NotesEditorDashboard,
  'note-custom-plugins': NoteCustomPlugins,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'note-table-plugins-drawer': NoteTablePluginsDrawer,
  'note-translation-edit-bar': TranslationsEditBar,
};

for (const key in components) {
  Vue.component(key, components[key]);
}