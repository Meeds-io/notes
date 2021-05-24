import NotesOverview from './components/NotesOverview.vue';

const components = {
  'notes-overview': NotesOverview,
};

for (const key in components) {
  Vue.component(key, components[key]);
}
