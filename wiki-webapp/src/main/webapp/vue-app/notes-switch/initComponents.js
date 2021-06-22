import notesFeatureSwitch from './components/NotesFeatureSwitch.vue';
import notesOverView from '../notes/components/NotesOverview.vue';
import NoteBreadcrumbDrawer from '../notes/components/NoteBreadcrumbDrawer.vue';
import NotesActionsMenu from '../notes/components/NotesActionsMenu.vue';

const components = {
  'notes-feature-switch': notesFeatureSwitch,
  'notes-overview': notesOverView,
  'note-breadcrumb-drawer': NoteBreadcrumbDrawer,
  'notes-actions-menu': NotesActionsMenu
};

for (const key in components) {
  Vue.component(key, components[key]);
}