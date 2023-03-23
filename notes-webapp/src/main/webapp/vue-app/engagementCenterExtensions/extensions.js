export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'note',
    options: {
      rank: 70,
      icon: 'fas fa-clipboard',
      match: (actionLabel) => ['addWikiPage', 'updateWikiPage'].includes(actionLabel),
      getLink: (realization) => {
        if (realization?.objectId && !realization.link) {
          Vue.prototype.$notesService.getNoteById(realization?.objectId)
            .then(note => Vue.prototype.$set(realization, 'link', note.url));
        }
      },
    },
  });
}