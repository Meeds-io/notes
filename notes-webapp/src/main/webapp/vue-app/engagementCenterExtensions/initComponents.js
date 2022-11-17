import NoteActionValue from './components/NoteActionValue.vue';
const components = {
  'note-action-value': NoteActionValue,
};

for (const key in components) {
  Vue.component(key, components[key]);
}

