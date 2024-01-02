import * as notesService from '../../javascript/eXo/wiki/notesService.js';

if (!Vue.prototype.$notesService) {
  window.Object.defineProperty(Vue.prototype, '$notesService', {
    value: notesService,
  });
}

export function init() {
  extensionRegistry.registerExtension('engagementCenterActions', 'user-actions', {
    type: 'note',
    options: {
      rank: 70,
      icon: 'fas fa-clipboard',
      match: (actionLabel) => ['addWikiPage', 'updateWikiPage'].includes(actionLabel),
      getLink: (realization) => {
        if (realization?.objectType === 'notes') {
          return window?.eXo?.env?.portal?.userName?.length && Vue.prototype.$notesService.getNoteById(realization?.objectId)
            .then(note => {
              if (note?.wikiType === 'group' && note?.wikiOwner?.includes?.('/spaces/')) {
                realization.link = note.url;
                return realization.link;
              }
            }) || null;
        }
      },
    },
  });
}