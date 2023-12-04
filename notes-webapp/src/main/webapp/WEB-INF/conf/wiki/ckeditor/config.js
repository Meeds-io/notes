// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)
const oldEditorConfigFn = CKEDITOR.editorConfig;
CKEDITOR.editorConfig = function (config) {

  oldEditorConfigFn(config);

  // style inside the editor
  config.contentsCss = [];
  document.querySelectorAll('[skin-type=portal-skin]')
    .forEach(link => config.contentsCss.push(link.href));
  config.contentsCss.push(document.querySelector('#brandingSkin').href);
  config.contentsCss.push('/notes/ckeditorCustom/contents.css'); // load last

  const urlParams = new URLSearchParams(window.location.search);
  const webPageNote = urlParams.get('webPageNote') === 'true';

  if (!webPageNote) {
    CKEDITOR.plugins.addExternal('insertOptions','/notes/javascript/eXo/wiki/ckeditor/plugins/insertOptions/','plugin.js');
  }
  CKEDITOR.plugins.addExternal('toc','/notes/javascript/eXo/wiki/ckeditor/plugins/toc/','plugin.js');
  CKEDITOR.plugins.addExternal('linkBalloon', '/social-portlet/js/ckeditorPlugins/linkBalloon/', 'plugin.js');

  const blocksToolbarGroup = [
    'Blockquote',
    'tagSuggester',
    'emoji',
    'selectImage',
    'Table',
    'EmbedSemantic',
    'CodeSnippet',
    'InsertOptions'
  ];
  if (webPageNote) {
    blocksToolbarGroup.splice(blocksToolbarGroup.indexOf('tagSuggester'), 1);
    blocksToolbarGroup.splice(blocksToolbarGroup.indexOf('InsertOptions'), 1);
  }
  const toolbar = [
    {name: 'accessibility', items: ['A11ychecker']},
    {name: 'format', items: ['Format']},
    {name: 'fontsize', items: ['FontSize']},
    {
      name: 'basicstyles',
      groups: ['basicstyles', 'cleanup'],
      items: ['Bold', 'Italic', 'Underline', 'Strike', 'TextColor', 'RemoveFormat', 'CopyFormatting']
    },
    {
      name: 'paragraph',
      groups: ['align', 'list', 'indent'],
      items: ['JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock', 'NumberedList', 'BulletedList', 'Outdent', 'Indent'],
    },
    {name: 'links', items: ['Link', 'Anchor']},
    {
      name: 'blocks',
      items: blocksToolbarGroup
    },
  ];
  let extraPlugins = `a11ychecker,balloonpanel,indent,indentblock,indentlist,codesnippet,sharedspace,copyformatting,table,tabletools,embedsemantic,autolink,colordialog${!webPageNote && ',tagSuggester' || ''},emoji,link,font,justify,widget,${!webPageNote && ',insertOptions' || ''},contextmenu,tabletools,tableresize,toc,linkBalloon`;
  let removePlugins = `image,confirmBeforeReload,maximize,resize,autoembed${webPageNote && ',tagSuggester' || ''}`;

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
        if (notesEditorExtension.extraToolbarItem) {
          toolbar[0].push(notesEditorExtension.extraToolbarItem);
        }
      });
    }
  });

  config.extraPlugins = extraPlugins;
  config.removePlugins = removePlugins;
  config.toolbar = toolbar;
  config.toolbarGroups = [
    { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
    { name: 'paragraph', groups: ['align', 'list', 'indent', ] },
    { name: 'links'},
    { name: 'blocks'},
  ];

  config.autoGrow_minHeight = 500;
  config.height = 'auto'
  config.format_tags = 'p;h1;h2;h3';
};