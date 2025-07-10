export function initNotesExtensions() {
  extensionRegistry.registerExtension('favorite', 'favorite-type', {
    rank: 30,
    id: 'notes',
    icon: 'fa-clipboard',
  });
  extensionRegistry.registerComponent('favorite-notes', 'favorite-drawer-item', {
    id: 'notes',
    vueComponent: Vue.options.components['notes-favorite-item'],
  });
}