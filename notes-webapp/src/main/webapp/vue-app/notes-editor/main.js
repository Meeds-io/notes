import './initComponents.js';
import './services.js';

// get overrided components if exists
if (extensionRegistry) {
  const components = extensionRegistry.loadComponents('notesEditor');
  if (components && components.length > 0) {
    components.forEach(cmp => {
      Vue.component(cmp.componentName, cmp.componentOptions);
    });
  }
}

const appId = 'notesEditorApplication';
const lang = window.eXo?.env?.portal?.language || 'en';
const url = `/notes/i18n/locale.portlet.notes.notesPortlet?lang=${lang}`;

export function init() {
  exoi18n.loadLanguageAsync(lang, url)
    .then(i18n => {
      // init Vue app when locale ressources are ready
      Vue.createApp({
        template: `<notes-editor-dashboard id="${appId}" />`,
        vuetify: Vue.prototype.vuetifyOptions,
        i18n
      }, `#${appId}`, 'Notes Editor Dashboard');
    }).finally(() => Vue.prototype.$utils.includeExtensions('NotesEditorExtension'));
}
