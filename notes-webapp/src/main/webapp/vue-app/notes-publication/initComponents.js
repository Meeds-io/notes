import NotePublicationDrawer from './components/NotePublicationDrawer.vue';
import NoteMetadataPropertiesForm from '../notes-rich-editor/components/note-properties/NoteMetadataPropertiesForm.vue';
import NotePublishOption from './components/options/NotePublishOption.vue';
import NotePublicationTargetDrawer from './components/options/NotePublicationTargetDrawer.vue';
import NotePublicationTargetList from './components/options/NotePublicationTargetList.vue';

const components = {
  'note-publication-drawer': NotePublicationDrawer,
  'note-metadata-properties-form': NoteMetadataPropertiesForm,
  'note-publish-option': NotePublishOption,
  'note-publication-target-drawer': NotePublicationTargetDrawer,
  'note-publication-target-list': NotePublicationTargetList
};

for (const key in components) {
  Vue.component(key, components[key]);
}
