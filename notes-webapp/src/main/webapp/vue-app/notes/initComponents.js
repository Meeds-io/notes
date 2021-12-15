import NotesOverview from './components/NotesOverview.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NotesActionsMenu from './components/NotesActionsMenu.vue';
import NoteBreadcrumb from './components/NoteBreadcrumb.vue';
import NoteHistoryDrawer from './components/NoteHistoryDrawer.vue';
import NoteImportDrawer from './components/NoteImportDrawer.vue';
import ExoNotesFavoriteAction from './components/ExoNotesFavoriteAction.vue';
import NotesNotificationAlert from './components/NotesNotificationAlert.vue';
import AttachmentsNotesUploadInput from './components/importNotes/AttachmentsNotesUploadInput.vue';
import AttachmentsUploadedNotes from './components/importNotes/AttachmentsNotesUploaded.vue';
import AttachmentsNotesItem from './components/importNotes/AttachmentsNotesItem.vue';

const components = {
  'notes-overview': NotesOverview,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'notes-actions-menu': NotesActionsMenu,
  'note-breadcrumb': NoteBreadcrumb,
  'note-history-drawer': NoteHistoryDrawer,
  'note-import-drawer': NoteImportDrawer,
  'exo-notes-favorite-action': ExoNotesFavoriteAction,
  'notes-notification-alert': NotesNotificationAlert,
  'attachments-notes-upload-input': AttachmentsNotesUploadInput,
  'attachments-uploaded-notes': AttachmentsUploadedNotes,
  'attachments-notes-item': AttachmentsNotesItem
};

for (const key in components) {
  Vue.component(key, components[key]);
}
