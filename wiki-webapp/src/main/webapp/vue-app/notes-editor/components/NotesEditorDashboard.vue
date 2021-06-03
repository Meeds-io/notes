<template>
  <v-app class="notesEditor">
    <div>
      <div
        id="notesEditor"
        class="notesEditor white">
        <div class="notesActions">
          <div class="notesFormButtons d-inline-flex flex-wrap width-full pa-3 ma-0">
            <div class="notesFormLeftActions d-inline-flex mr-10">
              <img :src="srcImageNote">
              <input
                id="notesTitle"
                class="ml-4"
                v-model="notes.title"
                :maxlength="titleMaxLength"
                :placeholder="notesTitlePlaceholder"
                type="text">
            </div>
            <div class="notesFormRightActions pr-7">
              <button
                class="notesCancel btn mr-2"
                @click="confirmCancelNote">
                {{ $t("btn.cancel") }}
              </button>
              <button
                id="notesUpdateAndPost"
                class="btn btn-primary primary"
                @click="postNotes">
                {{ $t("btn.post") }}
              </button>
            </div>
          </div>
        </div>
        <div id="notesTop"></div>
        <div class="formInputGroup flex">
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
      <exo-confirm-dialog
        ref="CancelNoteDialog"
        :message="$t('popup.confirmation.cancel')"
        :title="$t('popup.msg.confirmation')"
        :ok-label="$t('popup.ok')"
        :cancel-label="$t('btn.cancel')"
        persistent
        @ok="closeNotes()"
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
        wikiType: eXo.env.portal.spaceName ? 'group' : 'portal',
        wikiOwner: eXo.env.portal.spaceName ? `/spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
        parentPageName: 'WikiHome',
        content: '',
      },
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
    const urlPath = document.location.pathname;
    if (urlPath.includes('NotesEditor/id')) {
      this.notesPageName = urlPath.split('NotesEditor/id')[1].split('=')[1];
      this.getNotes();
    }
  },

  methods: {
    getNotes() {
      return this.$notesService.getNotes(this.notes.wikiType, this.notes.wikiOwner , this.notesPageName).then(data => {
        this.notes = data || [];
      });
    },
    postNotes(){
      const notes = {
        title: this.notes.title,
        name: this.notesPageName,
        wikiType: this.notes.wikiType,
        wikiOwner: this.notes.wikiOwner,
        parentPageName: 'WikiHome',
        content: this.notes.content,
      };
      const urlPath = document.location.pathname;
      if (this.notes && this.notes.id){
        this.$notesService.updateNote(notes).then(() => {
          window.location.href=`${urlPath.split('NotesEditor')[0]}Notes`;
        }).catch(e => {
          console.error('Error when update note page', e);
        });
      }
      else if (!this.notes.title.length){
        this.confirmCreateNote();
      }
      else {
        this.$notesService.createNote(notes).then(() => {
          window.location.href=`${urlPath.split('NotesEditor')[0]}Notes`;
        }).catch(e => {
          console.error('Error when adding note page', e);
        });
      }
    },
    confirmPostNotes(){
      const notes = {
        title: this.notes.title,
        name: this.notesPageName,
        wikiType: this.notes.wikiType,
        wikiOwner: this.notes.wikiOwner,
        parentPageName: 'WikiHome',
        content: this.notes.content,
      };
      const urlPath = document.location.pathname;
      this.$notesService.createNote(notes).then(() => {
        window.location.href=`${urlPath.split('NotesEditor')[0]}Notes`;
      }).catch(e => {
        console.error('Error when adding note page', e);
      });
    },
    closeNotes(){
      const urlPath = document.location.pathname;
      window.location.href=`${urlPath.split('NotesEditor')[0]}Notes`;
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
    confirmCancelNote: function () {
      this.$refs.CancelNoteDialog.open();
    },
  }
};
</script>
