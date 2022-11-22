import './initComponents.js';
export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'note',
    options: {
      rank: 70,
      vueComponent: Vue.options.components['note-action-value'],
      match: (actionLabel) => ['addWikiPage', 'updateWikiPage'].includes(actionLabel),
    },
  });
}