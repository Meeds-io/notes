CKEDITOR.plugins.add( 'toc', {

  // The plugin initialization logic goes inside this method.
  init: function( editor ) {

    var pluginDirectory = this.path;
    editor.addContentsCss(pluginDirectory + 'toc.css');

    editor.addCommand( 'ToC', {

      // Define the function that will be fired when the command is executed.
      exec: function( editor ) {
        var listId = 'toc-' + Math.floor(Math.random() * 100);
        editor.insertHtml('<ul class="note-manual-child" id='+listId+'></ul>');
        document.dispatchEvent(new CustomEvent('note-toc-plugin',{detail: listId}));
      }
    });
  }
});

