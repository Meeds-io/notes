// force env when using the eXo Android app (the eXo Android app uses a custom user agent which
// is not known by CKEditor and which makes it not initialize the editor)
const oldEditorConfigInputFn = CKEDITOR.editorConfig;
CKEDITOR.editorConfig = function (config) {

    oldEditorConfigInputFn(config);

    // style inside the editor
    config.contentsCss = [];
    document.querySelectorAll('[skin-type=portal-skin]')
        .forEach(link => config.contentsCss.push(link.href));
    config.contentsCss.push(document.querySelector('#brandingSkin').href);
    config.contentsCss.push('/notes/ckeditorCustom/contents.css'); // load last

    const toolbar = [
        {
            name: 'blocks',
            items: ['tagSuggester', 'emoji']
        },
    ];
    let extraPlugins = 'tagSuggester,emoji';
    let removePlugins = 'image,confirmBeforeReload,maximize,resize,autoembed';

    config.toolbar = toolbar;
    config.extraPlugins = extraPlugins;
    config.removePlugins = removePlugins;
    config.toolbarGroups = [{ name: 'blocks'},];

    config.autoGrow_minHeight = 138;
    config.height = 138
    config.format_tags = 'p;h1;h2;h3';
};
