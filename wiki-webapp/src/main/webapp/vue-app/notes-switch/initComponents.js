import notesFeatureSwitch from './components/NotesFeatureSwitch.vue';
import notesOverView from '../notes/components/NotesOverview.vue';

const components = {
  'notes-feature-switch': notesFeatureSwitch,
  'notes-overview': notesOverView,
};

for (const key in components) {
  Vue.component(key, components[key]);
}