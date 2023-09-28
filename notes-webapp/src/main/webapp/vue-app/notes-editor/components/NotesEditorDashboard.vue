<template>
  <v-app class="notesEditor">
    <v-alert
      v-model="alert"
      :class="alertMessageClass"
      :type="alertType"
      :icon="alertType === 'warning' ? 'mdi-alert-circle' : ''"
      dismissible>
      {{ message }}
      <a 
        v-if="alertType === 'warning' && note.draftPage" 
        class="dropDraftLink"
        @click="dropDraft">{{ $t('notes.label.drop.draft') }}</a>
    </v-alert>
    <div
      id="notesEditor"
      class="notesEditor width-full">
      <div class="notes-topbar">
        <div
          v-if="!showTranslationBar"
          class="notesActions white">
          <div class="notesFormButtons d-inline-flex flex-wrap width-full pa-3 ma-0">
            <div class="notesFormLeftActions d-inline-flex align-center me-10">
              <img :src="srcImageNote">
              <span class="notesFormTitle ps-2">{{ noteFormTitle }}</span>
              <v-tooltip bottom>
                <template #activator="{ on, attrs }">
                  <v-icon
                    v-if="notesMultilingualActive && noteId"
                    size="22"
                    class="clickable pa-2"
                    :class="langBottonColor"
                    v-on="on"
                    v-bind="attrs"
                    @click="showTranslations">
                    fa-language
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.button.translations.options') }}</span>
              </v-tooltip>
            </div>
            <div class="notesFormRightActions pr-7">
              <p class="draftSavingStatus mr-7">{{ draftSavingStatus }}</p>
              <button
                id="notesUpdateAndPost"
                class="btn btn-primary primary px-2 py-0"
                :key="postKey"
                :aria-label="publishButtonText"
                @click.once="postNote(false)">
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
                  @click.stop="postNote(true)"
                  class="px-2">
                  <v-icon
                    size="16"
                    class="primary--text clickable pr-2">
                    mdi-arrow-collapse-up
                  </v-icon>
                  <span class="body-2 text-color">{{ publishAndPostButtonText }}</span>
                </v-list-item>
              </v-menu>
            </div>
          </div>
        </div>
        <note-translation-edit-bar
          v-if="notesMultilingualActive"
          ref="translationsEditBar"
          :note="note"
          :languages="languages"
          :translations="translations" />
        <div id="notesTop" class="width-full darkComposerEffect"></div>
      </div>

      <form class="notes-content">
        <div class="notes-content-form singlePageApplication py-1 px-5">
          <div class="formInputGroup notesTitle white px-5">
            <input
              id="notesTitle"
              ref="noteTitle"
              v-model="note.title"
              :placeholder="notesTitlePlaceholder"
              type="text"
              class="py-0 px-1 mt-5 mb-0">
          </div>
          <div class="formInputGroup white overflow-auto flex notes-content-wrapper">
            <textarea
              id="notesContent"
              v-model="note.content"
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
      lang: eXo.env.portal.language,
      dateTimeFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      },
      note: {
        id: '',
        title: '',
        content: '',
        parentPageId: '',
      },
      actualNote: {
        id: '',
        title: '',
        content: '',
        parentPageId: '',
      },
      alert: false,
      alertType: '',
      message: '',
      noteId: '',
      parentPageId: '',
      appName: 'notes',
      srcImageNote: '/notes/images/wiki.png',
      titleMaxLength: 1000,
      notesTitlePlaceholder: `${this.$t('notes.title.placeholderContentInput')}*`,
      notesBodyPlaceholder: `${this.$t('notes.body.placeholderContentInput')}`,
      publishAndPost: false,
      spaceId: '',
      noteFormTitle: '',
      postingNote: false,
      savingDraft: false,
      initDone: false,
      initActualNoteDone: false,
      draftSavingStatus: '',
      autoSaveDelay: 1000,
      saveDraft: '',
      postKey: 1,
      navigationLabel: `${this.$t('notes.label.Navigation')}`,
      noteNavigationDisplayed: false,
      spaceGroupId: null,
      showTranslationBar: false,
      slectedLanguage: null,
      translations: null,
      languages: [],
      oembedMinWidth: 300,
    };
  },
  computed: {
    publishAndPostButtonText() {
      if (this.note.id && (this.note.targetPageId || !this.note.draftPage)) {
        return this.$t('notes.button.updateAndPost');
      } else {
        return this.$t('notes.button.publishAndPost');
      }
    },
    publishButtonText() {
      if (this.note.id && (this.note.targetPageId || !this.note.draftPage)) {
        return this.$t('notes.button.update');
      } else {
        return this.$t('notes.button.publish');
      }
    },
    initCompleted() {
      return this.initDone && ((this.initActualNoteDone || this.noteId) || (this.initActualNoteDone || !this.noteId)) ;
    },
    alertMessageClass(){
      return  this.message.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length > 45 ? 'lengthyAlertMessage' : '';
    },
    notesMultilingualActive() {
      return eXo?.env?.portal?.notesMultilingual;
    },
    langBottonColor(){
      return this.translations?.length>0 ? 'primary--text':'';
    }

  },
  watch: {
    'note.title'() {
      if (this.note.title !== this.actualNote.title ) {
        this.autoSave();
        this.hideTranslations();
      }
    },
    'note.content'() {
      if (this.note.content !== this.actualNote.content) {
        this.autoSave();
        this.hideTranslations();
      }
    }
  },
  created() {
    this.getAvailableLanguages();
    window.addEventListener('beforeunload', () => {
      if (!this.postingNote && this.note.draftPage && this.note.id) {
        const currentDraft = localStorage.getItem(`draftNoteId-${this.note.id}`);
        if (currentDraft) {
          this.removeLocalStorageCurrentDraft();
          const draftToPersist = JSON.parse(currentDraft);
          this.persistDraftNote(draftToPersist);
        }
      }
    });
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if (urlParams.has('appName')) {
      this.appName = urlParams.get('appName');
    }
    if (urlParams.has('translation')) {
      this.slectedLanguage = urlParams.get('translation');
    }
    if (urlParams.has('noteId')) {
      this.noteId = urlParams.get('noteId');
      const isDraft = urlParams.has('isDraft') && urlParams.get('isDraft') === 'true';
      if (isDraft) {
        this.getDraftNote(this.noteId);
      } else {
        this.getNote(this.noteId);
      }
      this.getNoteLanguages();
    }
    if (urlParams.has('parentNoteId')) {
      this.parentPageId = urlParams.get('parentNoteId');
      this.spaceId = urlParams.get('spaceId');
      this.spaceGroupId  = urlParams.get('spaceGroupId');
      this.note.parentPageId = this.parentPageId;
    }
    this.displayFormTitle();
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
      this.note.content= data;
    });
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
    this.$root.$on('hide-translations', () => {
      this.showTranslationBar=false;
    });
    this.$root.$on('display-treeview-items', filter => {
      if ( urlParams.has('noteId') ) {
        this.$refs.noteTreeview.open(this.note, 'includePages', null, filter);
      } else if (urlParams.has('parentNoteId')) {
        this.$notesService.getNoteById(this.parentPageId).then(data => {
          const note = data;
          this.$refs.noteTreeview.open(note, 'includePages', null, filter);
        });
      }
    });
    this.$root.$on('add-translation', lang => {
      this.slectedLanguage=lang.value;
      this.translations.unshift(lang);
      this.note.content='';
      this.note.title='';
      this.note.lang=lang.value;
      this.initCKEditor();
    });
    this.$root.$on('lang-translation-changed', lang => {
      const noteId= !this.note.draftPage?this.note.id:this.note.targetPageId;
      this.slectedLanguage=lang.value;
      if (lang.value!=='') {
        this.translations=this.translations.filter(item => item.value !== lang.value);
        this.translations.unshift(lang);
      }
      this.getNote(noteId);
      this.note.lang=lang.value;
      this.initCKEditor();
    });
    this.$root.$on('include-page', (note) => {
      const editor = $('textarea#notesContent').ckeditor().editor;
      const editorSelectedElement = editor.getSelection().getStartElement();
      if (editor.getSelection().getSelectedText()) {
        if (editorSelectedElement.is('a')) {
          if (editorSelectedElement.getAttribute( 'class' ) === 'noteLink') {
            editor.getSelection().getStartElement().remove();
            editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${note.name}</a>`);
          }
          if (editorSelectedElement.getAttribute( 'class' ) === 'labelLink') {
            const linkText = editorSelectedElement.getHtml();
            editor.getSelection().getStartElement().remove();
            editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${linkText}</a>`);
          }
        } else {
          editor.insertHtml(`<a href='${note.noteId}' class='labelLink'>${editor.getSelection().getSelectedText()}</a>`);
        }
      } else {
        editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${note.name}</a>`);
      }
    });

    document.addEventListener('note-navigation-plugin', () => {
      this.$root.$emit('show-alert', {
        type: 'error',
        message: this.$t('notes.message.manualChild')
      });
    });

  },
  mounted() {
    if (this.spaceId) {
      this.init();
    }
  },
  methods: {
    init() {
      setTimeout(() => {
        this.initCKEditor();
        this.setToolBarEffect();
        this.initDone = true;
      },200);
    },
    autoSave() {
      // No draft saving if init not done or in edit mode for the moment
      if (!this.initCompleted) {
        return;
      }
      // if the Note is being posted, no need to autosave anymore
      if (this.postingNote) {
        return;
      }

      // close draft dropping related alert
      if (this.alertType === 'warning' && this.note.draftPage && this.alert) {
        this.alert = false;
      }
      
      clearTimeout(this.saveDraft);
      this.saveDraft = setTimeout(() => {
        this.savingDraft = true;
        this.draftSavingStatus = this.$t('notes.draft.savingDraftStatus');
        this.$nextTick(() => {
          this.saveNoteDraft();
        });
      }, this.autoSaveDelay);
    },
    getNote(id) {
      return this.$notesService.getLatestDraftOfPage(id).then(latestDraft => {
        this.init();
        // check if page has a draft
        latestDraft = Object.keys(latestDraft).length !== 0 ? latestDraft : null;
        if (latestDraft) {
          this.fillNote(latestDraft);
          const messageObject = {
            type: 'warning',
            message: `${this.$t('notes.alert.warning.label.draft.drop')} ${this.$dateUtil.formatDateObjectToDisplay(new Date(this.note.updatedDate.time), this.dateTimeFormat, this.lang)},`
          };
          this.displayMessage(messageObject, true);
          this.initActualNoteDone = true;
        } else {
          this.$notesService.getNoteById(id,this.slectedLanguage).then(data => {
            this.$nextTick(()=> this.fillNote(data));
            this.initActualNoteDone = true;
          });
        }
      });
    },
    getDraftNote(id) {
      return this.$notesService.getDraftNoteById(id).then(data => {
        this.init();
        this.fillNote(data);
      }).finally(() => {
        const messageObject = {
          type: 'warning',
          message: `${this.$t('notes.alert.warning.label.draft.drop')} ${this.$dateUtil.formatDateObjectToDisplay(new Date(this.note.updatedDate.time), this.dateTimeFormat, this.lang)},  `
        };
        this.displayMessage(messageObject, true);
        this.initActualNoteDone = true;
      });
    },
    fillNote(data) {
      this.initActualNoteDone = false;
      if (data) {
        this.note = data;
        this.actualNote = {
          id: this.note.id,
          name: this.note.name,
          title: this.note.title,
          content: this.note.content,
          lang: this.note.lang,
          author: this.note.author,
          owner: this.note.owner,
          breadcrumb: this.note.breadcrumb,
          toBePublished: this.note.toBePublished,
        };
        const childContainer = '<div id="note-children-container" class="navigation-img-wrapper" contenteditable="false"><figure class="image-navigation" contenteditable="false">'
        +'<img src="/notes/images/children.png" role="presentation"/><img src="/notes/images/trash.png" id="remove-treeview" alt="remove treeview"/>'
        +'<figcaption class="note-navigation-label">Navigation</figcaption></figure></div><p></p>';
        if (CKEDITOR.instances['notesContent']) {
          CKEDITOR.instances['notesContent'].setData(data.content);
        }
        if ((this.note.content.trim().length === 0)) {
          this.$notesService.getNoteById(this.noteId,this.slectedLanguage,'','','',true).then(data => {
            if (data && data.children && data.children.length) {
              CKEDITOR.instances['notesContent'].setData(childContainer);
              this.setFocus();
            }
          });
        } 
      }
    },
    postNote(toPublish) {
      this.postingNote = true;
      clearTimeout(this.saveDraft);
      if (this.validateForm()) {
        let note;
        if (this.note.draftPage) {
          note = {
            id: this.note.targetPageId ? this.note.targetPageId : null,
            title: this.note.title,
            name: this.note.name,
            lang: this.note.lang,
            wikiType: this.note.wikiType,
            wikiOwner: this.note.wikiOwner,
            content: this.getBody() || this.note.content,
            parentPageId: this.note.targetPageId === this.parentPageId ? null : this.parentPageId,
            toBePublished: toPublish,
            appName: this.appName,
          };
        } else {
          note = {
            id: this.note.id,
            title: this.note.title,
            name: this.note.name,
            lang: this.note.lang,
            wikiType: this.note.wikiType,
            wikiOwner: this.note.wikiOwner,
            content: this.getBody() || this.note.content,
            parentPageId: this.parentPageId,
            toBePublished: toPublish,
            appName: this.appName,
          };
        }
        let notePath = '';
        if (note.id) {
          this.$notesService.updateNoteById(note).then(data => {
            this.removeLocalStorageCurrentDraft();
            notePath = this.$notesService.getPathByNoteOwner(data, this.appName).replace(/ /g, '_');
            this.draftSavingStatus = '';
            let translation = '';
            if (this.slectedLanguage!==''){
              translation = `?translation=${this.slectedLanguage}`;
            }
            window.location.href = `${notePath}${translation}`;
          }).catch(e => {
            console.error('Error when update note page', e);
            this.enableClickOnce();
            this.$root.$emit('show-alert', {
              type: 'error',
              message: this.$t(`notes.message.${e.message}`)
            });
          });
        } else {
          this.$notesService.createNote(note).then(data => {
            notePath = this.$notesService.getPathByNoteOwner(data, this.appName).replace(/ /g, '_');
            // delete draft note
            const draftNote = JSON.parse(localStorage.getItem(`draftNoteId-${this.note.id}`));
            this.deleteDraftNote(draftNote, notePath);
          }).catch(e => {
            console.error('Error when creating note page', e);
            this.enableClickOnce();
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
    saveNoteDraft() {
      const draftNote = {
        id: this.note.draftPage ? this.note.id : '',
        title: this.note.title,
        content: this.getBody() || this.note.content,
        name: this.note.name,
        lang: this.note.lang,
        appName: this.appName,
        wikiType: this.note.wikiType,
        wikiOwner: this.note.wikiOwner,
        parentPageId: this.parentPageId,
      };
      if (this.note.draftPage && this.note.id) {
        draftNote.targetPageId = this.note.targetPageId;
      } else {
        draftNote.targetPageId = this.note.id ? this.note.id : '';
      }

      if (this.note.title || this.note.content) {
        // if draft page not created persist it only the first time else update it in browser's localStorage
        if (this.note.draftPage && this.note.id) {
          this.note.parentPageId = this.parentPageId;
          localStorage.setItem(`draftNoteId-${this.note.id}`, JSON.stringify(draftNote));
          this.actualNote = {
            name: draftNote.name,
            title: draftNote.title,
            content: draftNote.content,
          };
          setTimeout(() => {
            this.draftSavingStatus = this.$t('notes.draft.savedDraftStatus');
          }, this.autoSaveDelay/2);
        } else {
          if (!this.isDefaultContent(this.note.content)) {
            this.persistDraftNote(draftNote);
          }
        }
      } else {
        // delete draft
        this.deleteDraftNote();
      }
    },
    persistDraftNote(draftNote) {
      if (this.note.title || this.note.content) {
        this.$notesService.saveDraftNote(draftNote, this.parentPageId).then(savedDraftNote => {
          this.actualNote = {
            id: savedDraftNote.id,
            name: savedDraftNote.name,
            title: savedDraftNote.title,
            content: savedDraftNote.content,
            author: savedDraftNote.author,
            owner: savedDraftNote.owner,
          };
          savedDraftNote.parentPageId = this.parentPageId;
          this.note = savedDraftNote;
          localStorage.setItem(`draftNoteId-${this.note.id}`, JSON.stringify(savedDraftNote));
        }).then(() => {
          this.savingDraft = false;
          setTimeout(() => {
            this.draftSavingStatus = this.$t('notes.draft.savedDraftStatus');
          }, this.autoSaveDelay/2);
        }).catch(e => {
          console.error('Error when creating draft note: ', e);
          this.$root.$emit('show-alert', {
            type: 'error',
            message: this.$t(`notes.message.${e.message}`)
          });
        });
      }
    },
    closePluginsDrawer() {
      this.$refs.noteCustomPlugins.close();
    },
    initCKEditor: function() {
      if (CKEDITOR.instances['notesContent'] && CKEDITOR.instances['notesContent'].destroy) {
        CKEDITOR.instances['notesContent'].destroy(true);
      }
      CKEDITOR.plugins.addExternal('insertOptions','/notes/javascript/eXo/wiki/ckeditor/plugins/insertOptions/','plugin.js');
      CKEDITOR.plugins.addExternal('toc','/notes/javascript/eXo/wiki/ckeditor/plugins/toc/','plugin.js');

      CKEDITOR.dtd.$removeEmpty['i'] = false;
      let extraPlugins = 'indent,indentblock,indentlist,codesnippet,sharedspace,copyformatting,table,tabletools,embedsemantic,autolink,colordialog,' +
          'tagSuggester,emoji,link,font,justify,widget,insertOptions,contextmenu,tabletools,tableresize,toc';
      let removePlugins = 'image,confirmBeforeReload,maximize,resize,autoembed';
      const windowWidth = $(window).width();
      const windowHeight = $(window).height();
      if (windowWidth > windowHeight && windowWidth < this.SMARTPHONE_LANDSCAPE_WIDTH) {
        // Disable suggester on smart-phone landscape
        extraPlugins = 'simpleLink';
      }
      const toolbar = [
        { name: 'format', items: ['Format'] },
        { name: 'fontsize', items: ['FontSize'] },
        {
          name: 'basicstyles',
          groups: ['basicstyles', 'cleanup'],
          items: ['Bold', 'Italic', 'Underline', 'Strike', 'TextColor','RemoveFormat', 'CopyFormatting']
        },
        {
          name: 'paragraph',
          groups: ['align','list','indent'],
          items: ['JustifyLeft', 'JustifyCenter', 'JustifyRight', 'JustifyBlock', 'NumberedList', 'BulletedList', 'Outdent', 'Indent'],
        },
        { name: 'links', items: [ 'Link', 'Anchor' ] },
        { name: 'blocks', items: ['Blockquote', 'tagSuggester', 'emoji', 'selectImage', 'Table', 'EmbedSemantic', 'CodeSnippet', 'InsertOptions'] },
      ];

      const ckEditorExtensions = extensionRegistry.loadExtensions('WYSIWYGPlugins', 'image');
      if (ckEditorExtensions && ckEditorExtensions.length) {
        const ckEditorExtraPlugins = ckEditorExtensions.map(ckEditorExtension => ckEditorExtension.extraPlugin).join(',');
        const ckEditorRemovePlugins = ckEditorExtensions.map(ckEditorExtension => ckEditorExtension.removePlugin).join(',');
        if (ckEditorExtraPlugins) {
          extraPlugins = `${extraPlugins},${ckEditorExtraPlugins}`;
        }
        if (ckEditorRemovePlugins) {
          removePlugins = `${removePlugins},${ckEditorRemovePlugins}`;
        }
      }
      const notesEditorExtensions = extensionRegistry.loadExtensions('NotesEditor', 'ckeditor-extensions');
      if (notesEditorExtensions?.length && this.useExtraPlugins) {
        notesEditorExtensions.forEach(notesEditorExtension => {
          if (notesEditorExtension.extraPlugin) {
            extraPlugins = `${extraPlugins},${notesEditorExtension.extraPlugin}`;
          }
          if (notesEditorExtension.removePlugin) {
            removePlugins = `${extraPlugins},${notesEditorExtension.removePlugin}`;
          }
          if (notesEditorExtension.extraToolbarItem) {
            toolbar[0].push(notesEditorExtension.extraToolbarItem);
          }
        });
      }

      CKEDITOR.addCss('h1 { font-size: 28px;margin-top:45px; }');
      CKEDITOR.addCss('h2 { font-size: 23px;margin-top:35px; }');
      CKEDITOR.addCss('h3 { font-size: 18px;margin-top:25px; }');
      CKEDITOR.addCss('h1, h2, h3 { font-weight: 500; line-height:1.2; }');
      CKEDITOR.addCss('p,li, table td { line-height:1.4}');
      CKEDITOR.addCss('p, li, table td, blockquote { font-size: 16px;}');
      CKEDITOR.addCss('ol, ul, dl {margin: 0 0 10px 0px;padding: 0 40px;}');
      CKEDITOR.addCss('ul li {list-style: revert; list-style-type: inherit !important;}');
      CKEDITOR.addCss('table td:not(:has(p)) {padding-bottom: 10px;}');
      CKEDITOR.addCss('blockquote {font-weight: 400; font-style:normal !important; padding: 10px !important; margin: 0 0 10px 0 !important;}');
      CKEDITOR.addCss('table {margin-bottom: 10px !important; margin-top: 0 !important;}');
      CKEDITOR.addCss('td {margin-bottom: 10px !important; margin-top: 0 !important;}');
      CKEDITOR.addCss('img:not(.cke_widget_mask, .cke_widget_drag_handler) { margin: 10px !important;}');
      CKEDITOR.addCss('blockquote p { margin-bottom: 0 !important; line-height: 1.4 !important; font-size: 16px !important;}');
      CKEDITOR.addCss('.cke_editable { font-size: 14px; line-height: 1.4 !important;}');
      CKEDITOR.addCss('.placeholder { color: #5f708a!important;}');
      CKEDITOR.addCss('div[data-widget="embedSemantic"] div {margin-bottom:10px;}');
      CKEDITOR.addCss('ol li {list-style-type: decimal !important;}');
      CKEDITOR.addCss('ol ol li {list-style-type: lower-latin !important;}');
      CKEDITOR.addCss('ol ol ol li {list-style-type: lower-roman !important;}');
      CKEDITOR.addCss('ol ol ol ol li {list-style-type: upper-latin !important;}');
      CKEDITOR.addCss('ol ol ol ol ol li {list-style-type: upper-roman !important;}');
      CKEDITOR.addCss('span[data-cke-display-name="image"] { margin: auto;}');
      CKEDITOR.addCss('span[data-cke-display-name="image"] span.cke_image_resizer { bottom: 0 !important;}');

      CKEDITOR.on('dialogDefinition', function (e) {
        if (e.data.name === 'link') {
          const informationTab = e.data.definition.getContents('target');
          const targetField = informationTab.get('linkTargetType');
          targetField['default'] = '_self';
          targetField.items = targetField.items.filter(t => ['_self', '_blank'].includes(t[1]));
        }
      });

      // this line is mandatory when a custom skin is defined
      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;

      $('textarea#notesContent').ckeditor({
        customConfig: `${eXo.env.portal.context}/${eXo.env.portal.rest}/richeditor/configuration?type=notes&v=${eXo.env.client.assetsVersion}`,
        extraPlugins: extraPlugins,
        removePlugins: removePlugins,
        allowedContent: true,
        spaceURL: self.spaceURL,
        spaceGroupId: `/spaces/${this.spaceGroupId}`,
        imagesDownloadFolder: 'notes/images',
        toolbarLocation: 'top',
        extraAllowedContent: 'table[summary];img[style,class,src,referrerpolicy,alt,width,height];span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*];',
        removeButtons: '',
        enterMode: CKEDITOR.ENTER_P,
        shiftEnterMode: CKEDITOR.ENTER_BR,
        toolbar: toolbar,
        toolbarGroups: [
          { name: 'basicstyles', groups: [ 'basicstyles', 'cleanup' ] },
          { name: 'paragraph', groups: ['align', 'list', 'indent', ] },
          { name: 'links'},
          { name: 'blocks'},
        ],
        copyFormatting_allowedContexts: true,
        indentBlock: {
          offset: 40,
          unit: 'px'
        },
        format_tags: 'p;h1;h2;h3',
        autoGrow_minHeight: self.noteFormContentHeight,
        height: self.noteFormContentHeight,
        bodyClass: 'notesContent',
        dialog_noConfirmCancel: true,
        colorButton_enableMore: true,
        sharedSpaces: {
          top: 'notesTop'
        },
        on: {
          instanceReady: function (evt) {
            self.actualNote.content = evt.editor.getData();
            CKEDITOR.instances['notesContent'].removeMenuItem('linkItem');
            CKEDITOR.instances['notesContent'].removeMenuItem('selectImageItem');
            CKEDITOR.instances['notesContent'].contextMenu.addListener( function( element ) {
              if ( element.getAscendant( 'table', true ) ) {
                return {
                  tableProperties: CKEDITOR.TRISTATE_ON
                };
              }
            });
            $(CKEDITOR.instances['notesContent'].document.$)
              .find('.atwho-inserted')
              .each(function() {
                $(this).on('click', '.remove', function() {
                  $(this).closest('[data-atwho-at-query]').remove();
                });
              });
            
            const treeviewParentWrapper =  CKEDITOR.instances['notesContent'].window.$.document.getElementById('note-children-container');
            if ( treeviewParentWrapper ) {
              treeviewParentWrapper.contentEditable='false';
            }

            const removeTreeviewBtn =  evt.editor.document.getById( 'remove-treeview' );
            if ( removeTreeviewBtn ) {
              evt.editor.editable().attachListener( removeTreeviewBtn, 'click', function() {
                const treeviewParentWrapper = evt.editor.document.getById( 'note-children-container' );
                if ( treeviewParentWrapper) {
                  treeviewParentWrapper.remove();
                  self.note.content = evt.editor.getData();
                }
                self.setFocus();
              } );
            }
            window.setTimeout(() => self.setFocus(), 50);
            self.$root.$applicationLoaded();
          },
          change: function (evt) {
            self.note.content = evt.editor.getData();
            self.autoSave();
            const removeTreeviewBtn =  evt.editor.document.getById( 'remove-treeview' );
            if ( removeTreeviewBtn ) {
              evt.editor.editable().attachListener( removeTreeviewBtn, 'click', function() {
                const treeviewParentWrapper = evt.editor.document.getById( 'note-children-container' );
                if ( treeviewParentWrapper) {
                  const newLine = treeviewParentWrapper.getNext();
                  treeviewParentWrapper.remove();
                  if ( newLine.$.innerText.trim().length === 0) {
                    newLine.remove();
                  }
                  self.note.content = evt.editor.getData();
                }
              } );
            }
          },
          fileUploadResponse: function() {
            /*add plugin fileUploadResponse to handle file upload response ,
              in this method we can get the response from server and update the editor content
              this method is called when file upload is finished*/
            CKEDITOR.instances.notesContent.once('afterInsertHtml', ()=> {
              window.setTimeout(() => {
                CKEDITOR.instances.notesContent.fire('mode');
              }, 2000);
            });
          },
          doubleclick: function(evt) {
            const element = evt.data.element;
            if ( element && element.is('a')) {
              const noteId = element.getAttribute( 'href' );
              self.$notesService.getNoteById(noteId,this.slectedLanguage).then(data => {
                const note = data;
                self.$refs.noteTreeview.open(note, 'includePages', 'no-arrow');
              });
            }
          }
        }
      });
      this.instance =CKEDITOR.instances['notesContent'];
    },
    setToolBarEffect() {
      const element = CKEDITOR.instances['notesContent'] ;
      const elementNewTop = document.getElementById('notesTop');
      if (element){
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
      }
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
      if (!this.noteId) {
        this.$refs.noteTitle.focus();
      } else {
        if (CKEDITOR.instances['notesContent']) {
          CKEDITOR.instances['notesContent'].status = 'ready';
          window.setTimeout(() => {
            this.$nextTick().then(() => CKEDITOR.instances['notesContent']?.focus());
          }, 200);
        }        
      }
    },
    validateForm() {
      if (!this.note.title) {
        this.enableClickOnce();
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.missingTitle')
        });
        return false;
      }
      if (!isNaN(this.note.title)) {
        this.enableClickOnce();
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.numericTitle')
        });
        return false;
      } else if (this.note.title.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length < 3 || this.note.title.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length > this.titleMaxLength) {
        this.enableClickOnce();
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t('notes.message.missingLengthTitle')
        });
        return false;
      } else {
        return true;
      }
    },
    displayMessage(message, keepAlert) {
      this.message = message.message;
      this.alertType = message.type;
      this.alert = true;
      if (!keepAlert) {
        window.setTimeout(() => this.alert = false, 5000);
      }
    },
    displayFormTitle() {
      if (this.noteId) {
        this.noteFormTitle = this.$t('notes.edit.editNotes');
      } else {
        return this.$spaceService.getSpaceById(this.spaceId).then(space => {
          this.noteFormTitle = this.$t('notes.composer.createNotes').replace('{0}', space.displayName);
        });
      }
    },
    dropDraft() {
      if (this.note.draftPage && this.note.id) {
        const targetPageId = this.note.targetPageId;
        this.removeLocalStorageCurrentDraft();
        this.$notesService.deleteDraftNote(this.note).then(() => {
          this.draftSavingStatus = '';
          //re-initialize data
          if (targetPageId) {
            if (this.alertType === 'warning') {
              this.alert = false;
            }
            this.getNote(targetPageId);
          } else {
            const parentNote = {
              id: this.note.parentPageId,
              wikiId: this.note.wikiId,
              wikiOwner: this.note.wikiOwner,
              wikiType: this.note.wikiType,
            };
            window.location.href = this.$notesService.getPathByNoteOwner(parentNote, this.appName).replace(/ /g, '_');
          }
        }).catch(e => {
          console.error('Error when deleting draft note', e);
        });
      }
    },
    deleteDraftNote(draftNote, notePath) {
      if (!draftNote) {
        draftNote = this.note;
      }
      if (this.note.draftPage && this.note.id) {
        this.removeLocalStorageCurrentDraft();
        this.$notesService.deleteDraftNote(draftNote).then(() => {
          this.draftSavingStatus = '';
          //re-initialize data
          if (!notePath) {
            this.note = {
              id: '',
              title: '',
              content: '',
              parentPageId: this.parentPageId,
              draftPage: true,
            };
            this.actualNote = {
              id: '',
              title: '',
              content: '',
              parentPageId: this.parentPageId,
              draftPage: true,
            };
          }
        }).then(() => {
          this.draftSavingStatus = '';
          if (notePath) {
            window.location.href = notePath;
          }
        }).catch(e => {
          console.error('Error when deleting draft note', e);
        });
      }
    },
    removeLocalStorageCurrentDraft() {
      const currentDraft = localStorage.getItem(`draftNoteId-${this.note.id}`);
      if (currentDraft) {
        localStorage.removeItem(`draftNoteId-${this.note.id}`);
      }
    },
    enableClickOnce() {
      this.postingNote = false;
      this.postKey++;
    },
    showTranslations() {
      this.showTranslationBar=true;
      this.$refs.translationsEditBar.show(this.slectedLanguage);
    },
    hideTranslations() {
      this.showTranslationBar=false;
      this.$refs.translationsEditBar.hide();
    },
    isDefaultContent(noteContent) {
      const div = document.createElement('div');
      div.innerHTML = noteContent;
      if ( div.childElementCount === 2) {
        const childrenWrapper = CKEDITOR.instances['notesContent'].window.$.document.getElementById('note-children-container');
        if ( childrenWrapper ) {
          if (childrenWrapper.nextElementSibling.innerText.trim().length === 0) {
            return true;
          } else {
            return false;
          }
        } else {
          return false;
        }
      } else {
        return false;
      }
    },
    getBody: function() {
      const domParser = new DOMParser();
      const newData = CKEDITOR.instances['notesContent'].getData();
      const body = CKEDITOR.instances['notesContent'].document.getBody().$;
      const docElement = domParser.parseFromString(newData, 'text/html').documentElement;
      const iframes = body.querySelectorAll('[data-widget="embedSemantic"] div iframe');
      const oEmbeds = docElement.querySelectorAll('oembed');
      oEmbeds.forEach((oembed, index) => {
        oembed.innerHTML = decodeURIComponent(oembed.innerHTML);
        oembed.dataset.htmlSource = iframes[index]?.parentNode?.innerHTML?.toString();
        const width = iframes[index]?.parentNode?.offsetWidth;
        const height = iframes[index]?.parentNode?.offsetHeight;
        const aspectRatio = width / height;
        const minWidth = parseInt(this.oembedMinWidth) / aspectRatio;
        const style = `
          position: relative;
          display: flex;
          margin: auto;
          min-height: ${minWidth}px;
          min-width: ${this.oembedMinWidth}px;
          width: ${width}px;
          height:${height}px;
          margin-bottom: 10px;
          aspect-ratio: ${aspectRatio};
        `;
        oembed.setAttribute('style', style);
      });
      return docElement?.children[1].innerHTML;
    },
    getNoteLanguages(){
      return this.$notesService.getNoteLanguages(this.noteId).then(data => {
        this.translations =  data || [];
        if (this.translations.length>0) {
          this.translations = this.languages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
        if (this.isMobile) {
          this.translations.unshift({value: '',text: this.$t('notes.label.translation.originalVersion')});
        }
      });
    },
    getAvailableLanguages(){
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
        this.languages.sort((a, b) => a.text.localeCompare(b.text));
        this.languages.unshift({value: '',text: this.$t('notes.label.chooseLangage')});
        if (this.translations){
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
      });
    },
  }
};
</script>
