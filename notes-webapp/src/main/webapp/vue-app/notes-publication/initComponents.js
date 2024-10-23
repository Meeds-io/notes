import NotePublicationDrawer from './components/NotePublicationDrawer.vue';
import NoteMetadataPropertiesForm from '../notes-rich-editor/components/note-properties/NoteMetadataPropertiesForm.vue';
import NotePublishOption from './components/publish-option/NotePublishOption.vue';
import NotePublicationTargetDrawer from './components/publish-option/NotePublicationTargetDrawer.vue';
import NotePublicationTargetList from './components/publish-option/NotePublicationTargetList.vue';
import NoteScheduleOption from './components/schedule-option/NoteScheduleOption.vue';

const components = {
  'note-publication-drawer': NotePublicationDrawer,
  'note-metadata-properties-form': NoteMetadataPropertiesForm,
  'note-publish-option': NotePublishOption,
  'note-publication-target-drawer': NotePublicationTargetDrawer,
  'note-publication-target-list': NotePublicationTargetList,
  'note-schedule-option': NoteScheduleOption
};

for (const key in components) {
  Vue.component(key, components[key]);
}
