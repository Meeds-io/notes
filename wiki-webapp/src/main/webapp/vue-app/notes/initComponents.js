import NotesOverview from './components/NotesOverview.vue';
import NoteBreadcrumbDrawer from './components/NoteBreadcrumbDrawer.vue';
import NotesActionsMenu from './components/NotesActionsMenu.vue';

const components = {
  'notes-overview': NotesOverview,
  'note-breadcrumb-drawer': NoteBreadcrumbDrawer,
  'notes-actions-menu': NotesActionsMenu
};

for (const key in components) {
  Vue.component(key, components[key]);
}
