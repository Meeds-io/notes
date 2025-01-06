import './services.js';

export function init() {
  extensionRegistry.registerExtension('notesEditor', 'snackbar-extension', {
    type: 'termsAndConditions',
    options: {
      name: 'termsAndConditions',
      displayMessage: (vm, page) => {
        document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
          alertMessage: vm.$t('notes.content.save.success.message'),
          alertType: 'success',
          alertLinkText: vm.$t('notes.publication.publish.save.label'),
          alertLinkCallback: () => this.updatePublishedSetting(vm, page),
        }}));
      },
    },
  });
}

export function updatePublishedSetting(vm, page) {
  const settings = {
    published: true,
    publishedDate: Date.now(),
    latestVersionId: page.latestVersionId,
  };
  return Vue.prototype.$termsAndConditionsService.updateTermsAndConditionsSettings(settings, eXo.env.portal.language || 'en')
    .then(() => {
      document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
        alertMessage: vm.$t('notes.content.publish.success.message'),
        alertType: 'success',
      }}));
    });
}
