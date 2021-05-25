<template>
  <v-app class="notesEditor">
    <div>
      <div
        id="notesActivityComposer"
        class="notesComposer">
        <div class="notesComposerActions">
          <div class="notesFormButtons">
            <div class="notesFormLeftActions mr-10">
              <img src="/news/images/newsImageDefault.png">
              <input
                id="notesTitle"
                class="ml-4"
                v-model="notes.title"
                :maxlength="titleMaxLength"
                :placeholder="notesTitlePlaceholder"
                type="text">
            </div>
            <div class="notesFormRightActions">
              <button class="notesCancel btn mr-2">
                {{ $t("btn.cancel") }}
              </button>
              <button
                id="notesUpdateAndPost"
                class="btn btn-primary">
                {{ $t("btn.post") }}
              </button>
            </div>
          </div>
        </div>
        <div id="notesTop"></div>
        <div class="formInputGroup">
          <textarea
            id="notesContent"
            v-model="notes.body"
            :placeholder="notesBodyPlaceholder"
            class="notesFormInput"
            name="notesContent">
            </textarea>
        </div>
      </div>
    </div>
  </v-app>
</template>

<script>

export default {
  props: {
  },
  data() {
    return {
      notes: {
        id: '',
        title: 'ddd',
        body: 'ddd',
      },
      titleMaxLength: 1000,
      notesTitlePlaceholder: `${this.$t('notes.title.placeholderContentInput')}*`,
      notesBodyPlaceholder: `${this.$t('notes.body.placeholderContentInput')}*`,
    };
  },
  mounted() {
    this.initCKEditor();
  },

  methods: {
    initCKEditor: function() {
      if (CKEDITOR.instances['notesContent'] && CKEDITOR.instances['notesContent'].destroy) {
        CKEDITOR.instances['notesContent'].destroy(true);
      }
      CKEDITOR.plugins.addExternal('video','/news/js/ckeditor/plugins/video/','plugin.js');
      CKEDITOR.dtd.$removeEmpty['i'] = false;
      let extraPlugins = 'sharedspace,simpleLink,selectImage,suggester,font,justify,widget,video';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < this.SMARTPHONE_LANDSCAPE_WIDTH) {
        // Disable suggester on smart-phone landscape
        extraPlugins = 'simpleLink,selectImage';
      }
      CKEDITOR.addCss('.cke_editable { font-size: 18px; }');

      // this line is mandatory when a custom skin is defined

      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;

      $('textarea#notesContent').ckeditor({
        customConfig: '/commons-extension/ckeditorCustom/config.js',
        extraPlugins: extraPlugins,
        removePlugins: 'image,confirmBeforeReload,maximize,resize',
        allowedContent: true,
        typeOfRelation: 'mention_activity_stream',
        spaceURL: self.spaceURL,
        toolbarLocation: 'top',
        extraAllowedContent: 'img[style,class,src,referrerpolicy,alt,width,height]; span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*]',
        removeButtons: 'Subscript,Superscript,Cut,Copy,Paste,PasteText,PasteFromWord,Undo,Redo,Scayt,Unlink,Anchor,Table,HorizontalRule,SpecialChar,Maximize,Source,Strike,Outdent,Indent,BGColor,About',
        toolbar: [
          { name: 'format', items: ['Format'] },
          { name: 'basicstyles', items: [ 'Bold', 'Italic', 'Underline', 'Strike', '-', 'RemoveFormat'] },
          { name: 'paragraph', items: [ 'NumberedList', 'BulletedList', '-', 'Blockquote' ] },
          { name: 'fontsize', items: ['FontSize'] },
          { name: 'colors', items: [ 'TextColor' ] },
          { name: 'align', items: [ 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock'] },
          { name: 'links', items: [ 'simpleLink', 'selectImage', 'Video'] },
        ],
        format_tags: 'p;h1;h2;h3',
        autoGrow_minHeight: self.notesFormContentHeight,
        height: self.notesFormContentHeight,
        bodyClass: 'notesContent',
        dialog_noConfirmCancel: true,
        sharedSpaces: {
          top: 'notesTop'
        },
        on: {
          instanceReady: function() {
            $(CKEDITOR.instances['notesContent'].document.$)
              .find('.atwho-inserted')
              .each(function() {
                $(this).on('click', '.remove', function() {
                  $(this).closest('[data-atwho-at-query]').remove();
                });
              });
          },
          change: function (evt) {
            self.notes.body = evt.editor.getData();
          }
        }
      });
    },
    initCKEditorData: function(message) {
      if (message) {
        const tempdiv = $('<div class=\'temp\'/>').html(message);
        tempdiv.find('a[href*="/profile"]')
          .each(function() {
            $(this).replaceWith(function() {
              return $('<span/>', {
                class: 'atwho-inserted',
                html: `<span class="exo-mention">${$(this).text()}<a data-cke-survive href="#" class="remove"><i data-cke-survive class="uiIconClose uiIconLightGray"></i></a></span>`
              }).attr('data-atwho-at-query',`@${  $(this).attr('href').substring($(this).attr('href').lastIndexOf('/')+1)}`)
                .attr('data-atwho-at-value',$(this).attr('href').substring($(this).attr('href').lastIndexOf('/')+1))
                .attr('contenteditable','false');
            });
          });
        message = `${tempdiv.html()  }&nbsp;`;
      }
      CKEDITOR.instances['notesContent'].setData(message);
    },
  }
};
</script>
<style>

</style>
