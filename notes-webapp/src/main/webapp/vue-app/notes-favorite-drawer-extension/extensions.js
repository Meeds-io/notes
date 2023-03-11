export function initNotesExtensions() {
  extensionRegistry.registerComponent('favorite-notes', 'favorite-drawer-item', {
    id: 'notes',
    vueComponent: Vue.options.components['notes-favorite-item'],
  });
}