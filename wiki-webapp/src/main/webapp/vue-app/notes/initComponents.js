import NotesOverview from './components/NotesOverview.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NotesActionsMenu from './components/NotesActionsMenu.vue';
import NoteBreadcrumb from './components/NoteBreadcrumb.vue';

const components = {
  'notes-overview': NotesOverview,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'notes-actions-menu': NotesActionsMenu,
  'note-breadcrumb': NoteBreadcrumb
};

for (const key in components) {
  Vue.component(key, components[key]);
}
