import NotesOverview from './components/NotesOverview.vue';
import NoteTreeviewDrawer from './components/NoteTreeviewDrawer.vue';
import NotesActionsMenu from './components/menu/NotesActionsMenu.vue';
import NoteBreadcrumb from './components/NoteBreadcrumb.vue';
import NoteImportDrawer from './components/NoteImportDrawer.vue';
import AttachmentsNotesUploadInput from './components/importNotes/AttachmentsNotesUploadInput.vue';
import AttachmentsUploadedNotes from './components/importNotes/AttachmentsNotesUploaded.vue';
import AttachmentsNotesItem from './components/importNotes/AttachmentsNotesItem.vue';
import NoteFavoriteAction from './components/NoteFavoriteAction.vue';
import NotesTranslationMenu from './components/NotesTranslationMenu.vue';
import NoteContentTableItem from './components/NoteContentTableItem.vue';
import NotesActionMenuItems from './components/menu/NotesActionMenuItems.vue';
import NotesMobileActionMenu from './components/menu/NotesMobileActionMenu.vue';

const components = {
  'notes-overview': NotesOverview,
  'note-treeview-drawer': NoteTreeviewDrawer,
  'notes-actions-menu': NotesActionsMenu,
  'note-breadcrumb': NoteBreadcrumb,
  'note-import-drawer': NoteImportDrawer,
  'attachments-notes-upload-input': AttachmentsNotesUploadInput,
  'attachments-uploaded-notes': AttachmentsUploadedNotes,
  'attachments-notes-item': AttachmentsNotesItem,
  'note-favorite-action': NoteFavoriteAction,
  'notes-translation-menu': NotesTranslationMenu,
  'note-content-table-item': NoteContentTableItem,
  'notes-action-menu-items': NotesActionMenuItems,
  'notes-mobile-action-menu': NotesMobileActionMenu
};

for (const key in components) {
  Vue.component(key, components[key]);
}
