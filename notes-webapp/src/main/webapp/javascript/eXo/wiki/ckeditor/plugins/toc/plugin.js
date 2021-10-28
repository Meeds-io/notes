CKEDITOR.plugins.add( 'toc', {

  icons: 'toc',

  init: function( editor ) {

    var pluginDirectory = this.path;
    editor.addContentsCss(pluginDirectory + 'toc.css');

    editor.addCommand( 'ToC', {

      exec: function( editor , childrenList) {
        if ( childrenList.length ) {
          var div = editor.document.createElement('div');
          var listChildItems = '<ul class="note-manual-child">';
          for (var j = 0; j < childrenList.length; j++){
            if (childrenList[j].hasChild) {
              listChildItems += '<li class="note-child-item has-child"><a href="'+childrenList[j].id+'" class="noteLink">'+childrenList[j].name+'<a></li>';
            } else {
              listChildItems += '<li class="note-child-item"><a href="'+childrenList[j].id+'" class="noteLink">'+childrenList[j].name+'<a></li>';
            }
          }
          listChildItems += '</ul>';
          div.setHtml(listChildItems);
          editor.insertElement( div );
        }
      }
    });
  }
});
