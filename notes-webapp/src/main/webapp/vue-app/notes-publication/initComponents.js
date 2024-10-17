import NotePublicationDrawer from './components/NotePublicationDrawer.vue';
import NoteMetadataPropertiesForm from '../notes-rich-editor/components/note-properties/NoteMetadataPropertiesForm.vue';

const components = {
  'note-publication-drawer': NotePublicationDrawer,
  'note-metadata-properties-form': NoteMetadataPropertiesForm
};

for (const key in components) {
  Vue.component(key, components[key]);
}
