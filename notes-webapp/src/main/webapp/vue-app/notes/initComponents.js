import NotesOverview from './components/NotesOverview.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NotesActionsMenu from './components/NotesActionsMenu.vue';
import NoteBreadcrumb from './components/NoteBreadcrumb.vue';
import NoteHistoryDrawer from './components/NoteHistoryDrawer.vue';

const components = {
  'notes-overview': NotesOverview,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'notes-actions-menu': NotesActionsMenu,
  'note-breadcrumb': NoteBreadcrumb,
  'note-history-drawer': NoteHistoryDrawer
};

for (const key in components) {
  Vue.component(key, components[key]);
}
