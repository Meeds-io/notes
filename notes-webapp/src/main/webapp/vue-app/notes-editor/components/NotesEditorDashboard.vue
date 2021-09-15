<template>
  <v-app class="notesEditor">
    <v-alert
      v-model="alert"
      :type="type"
      dismissible>
      {{ message }}
    </v-alert>
    <div
      id="notesEditor"
      class="notesEditor width-full">
      <div class="notes-topbar">
        <div class="notesActions white">
          <div class="notesFormButtons d-inline-flex flex-wrap width-full pa-3 ma-0">
            <div class="notesFormLeftActions d-inline-flex align-center me-10">
              <img :src="srcImageNote">
              <span class="notesFormTitle ps-2">{{ notesFormTitle }}</span>
            </div>
            <div class="notesFormRightActions pr-7">
              <button
                id="notesUpdateAndPost"
                class="btn btn-primary primary px-2 py-0"
                @click="postNotes(false)">
                {{ publishButtonText }}
                <v-icon
                  id="notesPublichAndPost"
                  dark
                  @click="openPublishAndPost">
                  mdi-menu-down
                </v-icon>
              </button>
              <v-menu
                v-model="publishAndPost"
                :attach="'#notesUpdateAndPost'"
                transition="scroll-y-transition"
                content-class="publish-and-post-btn width-full"
                offset-y
                left>
                <v-list-item
                  @click.stop="postNotes(true)"
                  class="px-2">
                  <v-icon
                    size="19"
                    class="primary--text clickable pr-2">
                    mdi-arrow-collapse-up
                  </v-icon>
                  <span class="body-2 text-color">{{ publishAndPostButtonText }}</span>
                </v-list-item>
              </v-menu>
            </div>
          </div>
        </div>
        <div id="notesTop" class="width-full"></div>
      </div>

      <form class="notes-content">
        <div class="notes-content-form px-4">
          <div class="formInputGroup notesTitle mx-3">
            <input
              id="notesTitle"
              v-model="notes.title"
              :placeholder="notesTitlePlaceholder"
              type="text"
              class="py-0 px-1 mt-5 mb-0">
          </div>
          <div class="formInputGroup white overflow-auto flex notes-content-wrapper">
            <textarea
              id="notesContent"
              v-model="notes.content"
              :placeholder="notesBodyPlaceholder"
              class="notesFormInput"
              name="notesContent">
                     </textarea>
          </div>
        </div>
      </form>
    </div>
    <note-custom-plugins ref="noteCustomPlugins" :instance="instance" />
    <note-table-plugins-drawer
      ref="noteTablePlugins"
      :instance="instance"
      @closed="closePluginsDrawer()" />
    <note-treeview-drawer 
      ref="noteTreeview"
      @closed="closePluginsDrawer()" />
  </v-app>
</template>

<script>

export default {
  props: {
    instance: {
      type: Object,
      default: () => null,
    },
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
      appName: 'notes',
      srcImageNote: '/notes/images/wiki.png',
      titleMaxLength: 1000,
      notesTitlePlaceholder: `${this.$t('notes.title.placeholderContentInput')}*`,
      notesBodyPlaceholder: `${this.$t('notes.body.placeholderContentInput')}*`,
      publishAndPost: false,
      spaceId: '',
      notesFormTitle: '',
    };
  },
  mounted() {
    this.initCKEditor();
    const elementNewTop = document.getElementById('notesTop');
    elementNewTop.classList.add('darkComposerEffect');
    this.setToolBarEffect();
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.publishAndPost) {
        window.setTimeout(() => {
          this.publishAndPost = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    document.addEventListener('note-custom-plugins', () => {
      this.$refs.noteCustomPlugins.open();
    });
    this.$root.$on('note-table-plugins', () => {
      this.$refs.noteTablePlugins.open();
    });
    this.$root.$on('updateData', data => {
      this.notes.content= data;
    });
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if ( urlParams.has('appName') ){
      this.appName = urlParams.get('appName');
    }
    if ( urlParams.has('noteId') ){
      this.noteId = urlParams.get('noteId');
      this.getNotes(this.noteId);
    } else if (urlParams.has('parentNoteId')){
      this.parentPageId = urlParams.get('parentNoteId');
      this.spaceId = urlParams.get('spaceId');
      this.notes.parentPageId=this.parentPageId;
    }
    if (urlParams.has('wikiOwner') && !this.notes.wikiOwner){
      this.notes.wikiOwner = urlParams.get('wikiOwner');
    }
    if (urlParams.has('wikiType') && !this.notes.wikiType){
      this.notes.wikiType = urlParams.get('wikiType');
    }
    this.$root.$on('display-treeview-items', () => {
      if ( urlParams.has('noteId') ) {
        this.$refs.noteTreeview.open(this.noteId, 'includePages');
      } else if (urlParams.has('parentNoteId')) {
        this.$refs.noteTreeview.open(this.parentPageId, 'includePages');
      }
    });
    this.$root.$on('include-page', (note) => {
      const editor = $('textarea#notesContent').ckeditor().editor;
      const editorSelectedElement = editor.getSelection().getStartElement();
      if (editor.getSelection().getSelectedText()) {
        if (editorSelectedElement.is('a')) {
          if (editorSelectedElement.getAttribute( 'class' ) === 'noteLink') {
            editor.getSelection().getStartElement().remove();
            editor.insertHtml(`<a href='${note.noteId}' class='noteLink' target='_blank'>${note.name}</a>`);
          }
          if (editorSelectedElement.getAttribute( 'class' ) === 'labelLink') {
            const linkText = editorSelectedElement.getHtml();
            editor.getSelection().getStartElement().remove();
            editor.insertHtml(`<a href='${note.noteId}' class='noteLink' target='_blank'>${linkText}</a>`);
          }
        } else {
          editor.insertHtml(`<a href='${note.noteId}' class='labelLink' target='_blank'>${editor.getSelection().getSelectedText()}</a>`);
        }
      } else {
        editor.insertHtml(`<a href='${note.noteId}' class='noteLink' target='_blank'>${note.name}</a>`);
      }
    });
    this.displayFormTitle();
  },
  computed: {
    publishAndPostButtonText() {
      if (this.notes.id) {
        return this.$t('notes.button.updateAndPost');
      } else {
        return this.$t('notes.button.publishAndPost');
      }
    },
    publishButtonText() {
      if (this.notes.id) {
        return this.$t('notes.button.update');
      } else {
        return this.$t('notes.button.publish');
      }
    },
  }, 
  methods: {
    getNotes(id) {
      return this.$notesService.getNoteById(id).then(data => {
        this.notes = data || [];
      });
    },
    postNotes(toPost){
      if (this.validateForm()){
        const notes = {
          id: this.notes.id,
          title: this.notes.title,
          name: this.notes.name,
          wikiType: this.notes.wikiType,
          wikiOwner: this.notes.wikiOwner,
          content: this.notes.content,
          parentPageId: this.notes.parentPageId,
          toBePublished: toPost,
          appName: this.appName,
        };
        let notePath = '';
        if (this.notes.id){
          this.$notesService.updateNoteById(notes).then(data => {
            notePath = this.$notesService.getPathByNoteOwner(data,this.appName).replace(/ /g, '_');
            window.location.href= notePath;
          }).catch(e => {
            console.error('Error when update note page', e);
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t(`notes.message.${e.message}`)
            });
          });
        } else {
          this.$notesService.createNote(notes).then(data => {
            notePath = this.$notesService.getPathByNoteOwner(data,this.appName).replace(/ /g, '_');
            window.location.href = notePath;
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
    openPublishAndPost(event) {
      this.publishAndPost = !this.publishAndPost;
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
    },
    closePluginsDrawer() {
      this.$refs.noteCustomPlugins.close();
    },
    initCKEditor: function() {
      if (CKEDITOR.instances['notesContent'] && CKEDITOR.instances['notesContent'].destroy) {
        CKEDITOR.instances['notesContent'].destroy(true);
      }
      CKEDITOR.plugins.addExternal('video','/notes/javascript/eXo/wiki/ckeditor/plugins/video/','plugin.js');
      CKEDITOR.plugins.addExternal('insertOptions','/notes/javascript/eXo/wiki/ckeditor/plugins/insertOptions/','plugin.js');

      CKEDITOR.dtd.$removeEmpty['i'] = false;
      let extraPlugins = 'sharedspace,simpleLink,selectImage,font,justify,widget,video,insertOptions,contextmenu,tabletools,tableresize';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < this.SMARTPHONE_LANDSCAPE_WIDTH) {
        // Disable suggester on smart-phone landscape
        extraPlugins = 'simpleLink,selectImage';
      }
      CKEDITOR.addCss('.cke_editable { font-size: 14px;}');
      CKEDITOR.addCss('.placeholder { color: #a8b3c5!important;}');

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
        removeButtons: '',
        toolbar: [
          { name: 'format', items: ['Format'] },
          { name: 'basicstyles', items: [ 'Bold', 'Italic', 'Underline', 'Strike', '-', 'RemoveFormat'] },
          { name: 'paragraph', items: [ 'NumberedList', 'BulletedList', '-', 'Blockquote' ] },
          { name: 'fontsize', items: ['FontSize'] },
          { name: 'colors', items: [ 'TextColor' ] },
          { name: 'align', items: [ 'JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock'] },
          { name: 'insert' },
          { name: 'links', items: [ 'simpleLink','InsertOptions'] },
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
            CKEDITOR.instances['notesContent'].removeMenuItem('linkItem');
            CKEDITOR.instances['notesContent'].removeMenuItem('selectImageItem');


            CKEDITOR.instances['notesContent'].contextMenu.addListener( function( element ) {
              if ( element.getAscendant( 'table', true ) ) {
                CKEDITOR.instances['notesContent'].addCommand('tableProperties', {
                  exec: function() {
                    if (CKEDITOR.instances['notesContent'].elementPath() && CKEDITOR.instances['notesContent'].elementPath().contains( 'table', 1 )){
                      const table=CKEDITOR.instances['notesContent'].elementPath().contains( 'table', 1 ).getAttributes();
                      self.$refs.noteTablePlugins.open(table);
                    }
                  }
                });
                return {
                  tableProperties: CKEDITOR.TRISTATE_ON
                };
              } else {
                const items = CKEDITOR.instances['notesContent'].contextMenu.items;
                CKEDITOR.instances['notesContent'].contextMenu.items = $.grep(items, (item) => item.command !== 'tableProperties');
              }
            });

            $(CKEDITOR.instances['notesContent'].document.$)
              .find('.atwho-inserted')
              .each(function() {
                $(this).on('click', '.remove', function() {
                  $(this).closest('[data-atwho-at-query]').remove();
                });
              });

            self.$root.$applicationLoaded();
            window.setTimeout(() => self.setFocus(), 50);
          },
          change: function (evt) {
            self.notes.content = evt.editor.getData();
          },
          doubleclick: function(evt) {
            const element = evt.data.element;
            if ( element && element.is('a')) {
              const noteId = element.getAttribute( 'href' );
              self.$refs.noteTreeview.open(noteId, 'includePages', 'no-arrow');
            }
          }
        }
      });
      this.instance =CKEDITOR.instances['notesContent'];
    },
    setToolBarEffect() {
      const element = CKEDITOR.instances['notesContent'] ;
      const elementNewTop = document.getElementById('notesTop');
      element.on('contentDom', function () {
        this.document.on('click', function(){
          elementNewTop.classList.add('darkComposerEffect');
        });
      });
      element.on('contentDom', function () {
        this.document.on('keyup', function(){
          elementNewTop.classList.add('darkComposerEffect');
        });
      });
      $('#notesEditor').parent().click(() => {
        elementNewTop.classList.remove('darkComposerEffect');
        elementNewTop.classList.add('greyComposerEffect');
      });
      $('#notesEditor').parent().keyup(() => {
        elementNewTop.classList.remove('darkComposerEffect');
        elementNewTop.classList.add('greyComposerEffect');
      });
    },
    setFocus() {
      if (CKEDITOR.instances['notesContent']) {
        CKEDITOR.instances['notesContent'].status = 'ready';
        window.setTimeout(() => {
          this.$nextTick().then(() => CKEDITOR.instances['notesContent'].focus());
        }, 200);
      }
    },
    validateForm() {
      if (!this.notes.title) {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.missingTitle')
        });
        return false;
      }
      if (!isNaN(this.notes.title)) {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.numericTitle')
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
    },
    displayFormTitle: function() {
      if (this.noteId) {
        this.notesFormTitle = this.$t('notes.edit.editNotes');
      } else {
        return this.$spaceService.getSpaceById(this.spaceId).then(space => {
          this.notesFormTitle = this.$t('notes.composer.createNotes').replace('{0}', space.displayName);
        });
      }
    },
  }
};
</script>
