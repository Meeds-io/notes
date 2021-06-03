import notesFeatureSwitch from './components/NotesFeatureSwitch.vue';
import notesOverView from '../notes/components/NotesOverview.vue';
import NoteBreadcrumbDrawer from '../notes/components/NoteBreadcrumbDrawer.vue';

const components = {
  'notes-feature-switch': notesFeatureSwitch,
  'notes-overview': notesOverView,
  'note-breadcrumb-drawer': NoteBreadcrumbDrawer,
};

for (const key in components) {
  Vue.component(key, components[key]);
}