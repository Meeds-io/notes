/**
 * Copyright (c) 2014-2021, CKSource - Frederico Knabben. All rights reserved.
 * Licensed under the terms of the MIT License (see LICENSE.md).
 *
 *
 * Created out of the CKEditor Plugin SDK:
 * https://ckeditor.com/docs/ckeditor4/latest/guide/plugin_sdk_intro.html
 */

// Register the plugin within the editor.
CKEDITOR.plugins.add( 'insertOptions', {

  // Register the icons. They must match command names.
  icons: 'insertOptions',
  lang : ['en','fr'],

  // The plugin initialization logic goes inside this method.
  init: function( editor ) {

    // Define the editor command that inserts a timestamp.
    editor.addCommand( 'insertOptions', {

      // Define the function that will be fired when the command is executed.
      exec: function( editor ) {
        document.dispatchEvent(new CustomEvent('note-custom-plugins'));
      }
    });

    // Create the toolbar button that executes the above command.
    editor.ui.addButton( 'InsertOptions', {
      label: editor.lang.insertOptions.buttonTooltip,
      command: 'insertOptions',
      toolbar: 'insert'
    });
  }
});