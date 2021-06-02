import NotesOverview from './components/NotesOverview.vue';
import NoteBreadcrumbDrawer from './components/NoteBreadcrumbDrawer.vue';
const components = {
  'notes-overview': NotesOverview,
  'note-breadcrumb-drawer': NoteBreadcrumbDrawer,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
