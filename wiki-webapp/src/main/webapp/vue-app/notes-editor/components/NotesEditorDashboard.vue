<template>
  <v-app class="notesEditor">
    <v-alert
      v-model="alert"
      :type="type"
      dismissible>
      {{ message }}
    </v-alert>
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
      alert: false,
      type: '',
      message: '',
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
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
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
      if (this.validateForm()){
        const notes = {
          id: this.notes.id,
          title: this.notes.title,
          name: this.notes.name,
          wikiType: this.notes.wikiType,
          wikiOwner: this.notes.wikiOwner,
          content: this.notes.content,
          parentPageId: this.notes.parentPageId,
        };
        if (this.notes.id){
          this.$notesService.updateNote(notes).then(() => {
            notes.name=notes.title;
            window.location.href=this.$notesService.getPathByNoteOwner(notes);
          }).catch(e => {
            console.error('Error when update note page', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t(`notes.message.${e.message}`)
            });
          });
        } else {
          this.$notesService.createNote(notes).then(data => {
            window.location.href=this.$notesService.getPathByNoteOwner(data);
          }).catch(e => {
            console.error('Error when creating note page', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t(`notes.message.${e.message}`)
            });
          });
        }
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
    validateForm() {
      if (!this.notes.title) {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.missingTitle')
        });
        return false;
      }
      else if (this.notes.title.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length < 3 || this.notes.title.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length > this.titleMaxLength) {
        this.validateFor=false;
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.missingLengthTitle')
        });
        return false;
      } else {
        return true;
      }
    },
    displayMessage(message) {
      this.message=message.message;
      this.type=message.type;
      this.alert = true;
      window.setTimeout(() => this.alert = false, 5000);
    }
  }
};
</script>
