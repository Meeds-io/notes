// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)
const oldEditorConfigFn = CKEDITOR.editorConfig;
CKEDITOR.editorConfig = function (config) {

  oldEditorConfigFn(config);

  // style inside the editor
  config.contentsCss = [];
  document.querySelectorAll('[skin-type=portal-skin]').forEach(link => config.contentsCss.push(link.href));
  config.contentsCss.push('/notes/ckeditorCustom/contents.css'); // load last

  CKEDITOR.plugins.addExternal('insertOptions','/notes/javascript/eXo/wiki/ckeditor/plugins/insertOptions/','plugin.js');
  CKEDITOR.plugins.addExternal('toc','/notes/javascript/eXo/wiki/ckeditor/plugins/toc/','plugin.js');
  CKEDITOR.plugins.addExternal('linkBalloon', '/social-portlet/js/ckeditorPlugins/linkBalloon/', 'plugin.js');


  let extraPlugins = 'simpleLink,a11ychecker,balloonpanel,indent,indentblock,indentlist,codesnippet,sharedspace,copyformatting,table,tabletools,embedsemantic,' +
        'autolink,colordialog,emoji,link,font,justify,widget,insertOptions,contextmenu,tabletools,tableresize,toc,editorplaceholder,formatOption,linkBalloon';

  let removePlugins = 'image,confirmBeforeReload,maximize,resize,autoembed,tagSuggester,attachImage';

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
    if (notesEditorExtensions?.length) {
      notesEditorExtensions.forEach(notesEditorExtension => {
        if (notesEditorExtension.extraPlugin) {
          extraPlugins = `${extraPlugins},${notesEditorExtension.extraPlugin}`;
        }
        if (notesEditorExtension.removePlugin) {
          removePlugins = `${extraPlugins},${notesEditorExtension.removePlugin}`;
        }
        if (notesEditorExtension.extraToolbarItem) {
          toolbar[0].push(notesEditorExtension.extraToolbarItem);
        }
      });
    }
  });

  config.extraPlugins = extraPlugins;
  config.removePlugins = removePlugins;
  config.toolbar = [
    ['formatOption', 'Bold', 'Italic', 'BulletedList', 'NumberedList', 'Blockquote', 'emoji'],
  ];

  config.autoGrow_onStartup = true;
  config.autoGrow_minHeight = 165;
  config.height = 165;
  config.format_tags = 'p;h1;h2;h3';
};