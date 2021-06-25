<template>
  <v-app class="notesEditor">
    <div>
      <div
        id="notesEditor"
        class="notesEditor width-full">
        <div class="notesActions white">
          <div class="notesFormButtons d-inline-flex flex-wrap width-full pa-3 ma-0">
            <div class="notesFormLeftActions d-inline-flex mr-10">
              <img :src="srcImageNote">
              <input
                ref="autoFocusInput1"
                id="notesTitle"
                class="mb-0 pr-5"
                v-model="notes.title"
                :maxlength="titleMaxLength"
                :placeholder="notesTitlePlaceholder"
                type="text">
            </div>
            <div class="notesFormRightActions pr-7">
              <button
                id="notesUpdateAndPost"
                class="btn btn-primary primary pl-4 pr-4 py-0"
                size="16"
                @click="postNotes">
                {{ $t("btn.post") }}
              </button>
              <button
                class="notesCancel btn mr-2 pl-4 pr-4 py-0"
                @click="closeNotes">
                {{ $t("btn.cancel") }}
              </button>
            </div>
          </div>
        </div>
        <div id="notesTop" class="width-full"></div>
        <div class="formInputGroup white overflow-auto ma-2 pa-2 flex">
          <textarea
            id="notesContent"
            v-model="notes.content"
            :placeholder="notesBodyPlaceholder"
            class="notesFormInput"
            name="notesContent">
            </textarea>
        </div>
      </div>
      <exo-confirm-dialog
        ref="CreateNoteDialog"
        :message="$t('popup.confirmation')"
        :title="$t('popup.msg.confirmation')"
        :ok-label="$t('popup.confirm')"
        :cancel-label="$t('btn.cancel')"
        persistent
        @ok="confirmPostNotes()"
        @dialog-opened="$emit('confirmDialogOpened')"
        @dialog-closed="$emit('confirmDialogClosed')" />
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
        title: '',
        content: '',
        parentPageId: '',
      },
      noteId: '',
      parentPageId: '',
      srcImageNote: '/wiki/images/wiki.png',
      titleMaxLength: 1000,
      notesTitlePlaceholder: `${this.$t('notes.title.placeholderContentInput')}*`,
      notesBodyPlaceholder: `${this.$t('notes.body.placeholderContentInput')}*`,
    };
  },
  mounted() {
    this.initCKEditor();
  },
  created() {
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if ( urlParams.has('noteId') ){
      this.noteId = urlParams.get('noteId');
      this.getNotes(this.noteId);
    } else if (urlParams.has('parentNoteId')){
      this.parentPageId = urlParams.get('parentNoteId');
      this.notes.parentPageId=this.parentPageId;
    }
  },
  methods: {
    getNotes(id) {
      return this.$notesService.getNoteById(id).then(data => {
        this.notes = data || [];
      });
    },
    postNotes(){
      const notes = {
        id: this.notes.id,
        title: this.notes.title,
        name: this.notes.name,
        wikiType: this.notes.wikiType,
        wikiOwner: this.notes.wikiOwner,
        content: this.notes.content,
        parentPageId: this.notes.parentPageId,
      };
      let notePath = '';
      if (this.notes.id){
        this.$notesService.updateNote(notes).then(() => {
          notes.name=notes.title;
          notePath = this.$notesService.getPathByNoteOwner(notes).replace(/ /g, '_');
          window.location.href= notePath;
        }).catch(e => {
          console.error('Error when update note page', e);
        });
      }
      else if (!this.notes.title.length){
        this.confirmCreateNote();
      }
      else {
        this.$notesService.createNote(notes).then(data => {
          notePath = this.$notesService.getPathByNoteOwner(data).replace(/ /g, '_');
          window.location.href = notePath;
        }).catch(e => {
          console.error('Error when adding note page', e);
        });
      }
    },
    confirmPostNotes(){
      const notes = {
        id: this.notes.id,
        title: this.notes.title,
        name: this.notes.name,
        wikiType: this.notes.wikiType,
        wikiOwner: this.notes.wikiOwner,
        content: this.notes.content,
        parentPageId: this.notes.parentPageId,
      };
      if (!notes.title){
        notes.title = this.$t('notes.untitled.title');
      }
      this.$notesService.createNote(notes).then(data => {
        window.location.href=this.$notesService.getPathByNoteOwner(data);
      }).catch(e => {
        console.error('Error when adding note page', e);
      });
    },
    closeNotes(){
      if (this.notes.id){
        window.location.href=this.$notesService.getPathByNoteOwner(this.notes);
      } else {
        this.$notesService.getNoteById(this.parentPageId).then(data => {
          window.location.href=this.$notesService.getPathByNoteOwner(data);
        });
      }
    },
    initCKEditor: function() {
      if (CKEDITOR.instances['notesContent'] && CKEDITOR.instances['notesContent'].destroy) {
        CKEDITOR.instances['notesContent'].destroy(true);
      }
      CKEDITOR.plugins.addExternal('video','/wiki/javascript/eXo/wiki/ckeditor/plugins/video/','plugin.js');
      CKEDITOR.dtd.$removeEmpty['i'] = false;
      let extraPlugins = 'sharedspace,simpleLink,selectImage,font,justify,widget,video';
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
            self.notes.content = evt.editor.getData();
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
    confirmCreateNote: function () {
      this.$refs.CreateNoteDialog.open();
    },
  }
};
</script>
