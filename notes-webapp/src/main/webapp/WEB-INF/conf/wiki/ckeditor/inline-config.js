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
};
