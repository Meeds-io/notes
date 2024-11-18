extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'copyLink',
  labelKey: 'notes.menu.label.copyLink',
  icon: 'fas fa-link',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  rank: 10,
  enabled: (note) => note?.canView,
  action: (vm) => {
    navigator.clipboard.writeText(`${window.location.origin}${vm.note.url}`).then(() => {
      vm.$root.$emit('show-alert', {type: 'success', message: vm.$t('notes.alert.success.label.linkCopied')});
      vm.$root.$emit('close-action-menu');
    });
  }
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'exportPdf',
  labelKey: 'notes.menu.label.exportPdf',
  icon: 'fas fa-file-pdf',
  sortable: true,
  cssClass: 'ps-2 pe-4 noteExportPdf action-menu-item',
  rank: 20,
  actionEvent: 'note-export-pdf',
  enabled: (note) => note?.canView,
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'noteHistory',
  labelKey: 'notes.menu.label.noteHistory',
  icon: 'fas fa-history',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  rank: 40,
  actionEvent: 'open-note-history',
  enabled: () => true,
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'movePage',
  labelKey: 'notes.menu.label.movePage',
  icon: 'fas fa-arrows-alt',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  rank: 50,
  actionEvent: 'open-note-treeview',
  enabled: (note) => note?.parentPageId && note?.canManage
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'exportNotes',
  labelKey: 'notes.menu.label.export',
  icon: 'fas fa-sign-in-alt',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  rank: 50,
  actionEvent: 'open-note-treeview',
  enabled: (note) => !note?.parentPageId
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'importNotes',
  labelKey: 'notes.menu.label.import',
  icon: 'fas fa-sign-out-alt',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  rank: 60,
  actionEvent: 'open-note-import-drawer',
  enabled: (note) => !note?.parentPageId && note?.canImport
});
extensionRegistry.registerExtension('NotesMenu', 'menuActionMenu', {
  id: 'deleteNote',
  labelKey: 'notes.menu.label.delete',
  icon: 'fas fa-trash',
  sortable: true,
  cssClass: 'ps-2 pe-4 action-menu-item',
  iconCssClass: 'delete-option-color',
  rank: 70,
  actionEvent: 'delete-note',
  enabled: (note) => note?.parentPageId && note?.canManage
});
