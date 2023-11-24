// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)

const oldEditorConfig = CKEDITOR.editorConfig;

CKEDITOR.editorConfig = function(config) {
  if (oldEditorConfig) {
    oldEditorConfig(config);
  }

  config.autoGrow_onStartup = true;
  config.autoGrow_minHeight = 165;
  config.height = 165;
  config.contentsCss.push('/notes/ckeditorCustom/contents.css'); // load last

  CKEDITOR.plugins.addExternal('insertOptions','/notes/javascript/eXo/wiki/ckeditor/plugins/insertOptions/','plugin.js');
  CKEDITOR.plugins.addExternal('toc','/notes/javascript/eXo/wiki/ckeditor/plugins/toc/','plugin.js');

  let extraPlugins = 'a11ychecker,balloonpanel,indent,indentblock,indentlist,codesnippet,sharedspace,copyformatting,table,tabletools,embedsemantic,' +
        'autolink,colordialog,tagSuggester,emoji,link,font,justify,widget,insertOptions,contextmenu,tabletools,tableresize,toc';

  let removePlugins = 'image,confirmBeforeReload,maximize,resize,autoembed';

  require(['SHARED/extensionRegistry'], function(extensionRegistry) {
    const ckEditorExtensions = extensionRegistry.loadExtensions('WYSIWYGPlugins', 'image');
    if (ckEditorExtensions?.length) {
      const ckEditorExtraPlugins = ckEditorExtensions.map(ckEditorExtension => ckEditorExtension.extraPlugin).join(',');
      const ckEditorRemovePlugins = ckEditorExtensions.map(ckEditorExtension => ckEditorExtension.removePlugin).join(',');
      if (ckEditorExtraPlugins) {
        extraPlugins = `${extraPlugins},${ckEditorExtraPlugins}`;
      }
      if (ckEditorRemovePlugins) {
        removePlugins = `${removePlugins},${ckEditorRemovePlugins}`;
      }
    }
    const notesEditorExtensions = extensionRegistry.loadExtensions('NotesEditor', 'ckeditor-extensions');
    if (notesEditorExtensions?.length && this.useExtraPlugins) {
      notesEditorExtensions.forEach(notesEditorExtension => {
        if (notesEditorExtension.extraPlugin) {
          extraPlugins = `${extraPlugins},${notesEditorExtension.extraPlugin}`;
        }
        if (notesEditorExtension.removePlugin) {
          removePlugins = `${extraPlugins},${notesEditorExtension.removePlugin}`;
        }
      });
    }
  });

  config.extraPlugins = extraPlugins;
  config.removePlugins = removePlugins;
  config.autoGrow_minHeight = 500;
  config.height = 'auto'
  config.format_tags = 'p;h1;h2;h3';
};
