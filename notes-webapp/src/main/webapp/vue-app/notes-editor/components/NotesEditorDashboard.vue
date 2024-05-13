<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->
<template>
  <v-app class="notesEditor">
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
                    :aria-label="$t('notes.label.button.translations.options')"
                    size="22"
                    class="clickable pa-2"
                    :class="langBottonColor"
                    v-on="on"
                    v-bind="attrs"
                    @click="showTranslations">
                    fa-language
                  </v-icon>
                </template>
                <span class="caption">{{ langBottonTooltipText }}</span>
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
                  v-if="!webPageNote"
                  id="notesPublichAndPost"
                  dark
                  @click="openPublishAndPost">
                  mdi-menu-down
                </v-icon>
              </button>
              <v-menu
                v-if="!webPageNote"
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
          ref="translationsEditBar"
          :note="note"
          :languages="languages"
          :translations="translations"
          :is-mobile="isMobile" />
        <div id="notesTop" class="width-full darkComposerEffect"></div>
      </div>

      <form class="notes-content">
        <div class="notes-content-form singlePageApplication my-5 mx-auto py-1 px-5">
          <div
            v-show="!webPageNote"
            class="formInputGroup notesTitle white px-5">
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
              ref="notesContent"
              :placeholder="notesBodyPlaceholder"
              name="notesContent"
              class="notesFormInput">
            </textarea>
          </div>
        </div>
      </form>
    </div>
    <note-custom-plugins ref="noteCustomPlugins" :instance="editor" />
    <note-table-plugins-drawer
      ref="noteTablePlugins"
      :instance="editor"
      @closed="closePluginsDrawer()" />
    <note-treeview-drawer 
      ref="noteTreeview"
      @closed="closePluginsDrawer()" />

    <div
      v-for="(extension, i) in noteEditorExtensions"
      :key="i">
      <extension-registry-component
        :component="extension"
        element="div" />
    </div>
  </v-app>
</template>
<script>
export default {
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
      oembedMinWidth: 300,
      showTranslationBar: false,
      selectedLanguage: null,
      translations: null,
      languages: [],
      allLanguages: [],
      newDraft: false,
      spaceDisplayName: null,
      noteEditorExtensions: null,
      editor: null,
      loadedNote: null,
      draftNote: null,
      instanceReady: false,
      initialized: false,
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
    urlParams() {
      return window.location.search && new URLSearchParams(window.location.search) || null;
    },
    webPageNote() {
      return this.urlParams?.get?.('webPageNote') === 'true';
    },
    webPageUrl() {
      return this.urlParams?.get?.('webPageUrl');
    },
    langBottonColor(){
      if (!this.noteId){
        return 'disabled--text not-clickable remove-focus';
      }
      return this.selectedLanguage ? 'primary--text':'';
    },
    isMobile() {
      return this.$vuetify.breakpoint.width < 1280;
    },
    langBottonTooltipText() {
      if (this.noteId) {
        return this.$t('notes.label.button.translations.options');
      } else {
        return this.$t('notes.message.firstVersionShouldBeCreated');
      }
    },

  },
  watch: {
    'note.title'() {
      if (this.note.title && this.note.title !== this.actualNote.title ) {
        this.autoSave();
        this.hideTranslations();
      }
    },
    'note.content'() {
      if (this.note.content && this.note.content !== this.actualNote.content) {
        this.autoSave();
        this.hideTranslations();
      }
    },
    draftNote() {
      if (!this.draftNote) {
        this.$root.$emit('close-alert-message');
      }
    },
    editor() {
      if (this.editor) {
        if (this.draftNote?.id || this.loadedNote?.id) {
          this.fillNote(this.draftNote || this.loadedNote);
          this.displayDraftMessage();
          this.initActualNoteDone = true;
          this.newDraft = !this.draftNote;
        }
        this.setToolBarEffect();
        this.initDone = true;
      }
    },
  },
  created() {
    this.refreshTranslationExtensions();
    document.addEventListener('automatic-translation-extensions-updated', () => {
      this.refreshTranslationExtensions();
    });
    this.getAvailableLanguages();
    window.addEventListener('beforeunload', () => {
      if (!this.postingNote && this.note.draftPage && this.note.id) {
        this.saveDraftFromLocalStorage();
      }
    });
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if (urlParams.has('appName')) {
      this.appName = urlParams.get('appName');
    }
    if (urlParams.has('translation')) {
      this.selectedLanguage = urlParams.get('translation');
    }
    if (urlParams.has('noteId')) {
      this.noteId = urlParams.get('noteId');
      const isDraft = urlParams.has('isDraft') && urlParams.get('isDraft') === 'true';
      if (isDraft) {
        this.getDraftNote(this.noteId);
      } else {
        this.getNote(this.noteId);
      }
    } else {
      this.initActualNoteDone=true;
    }
    if (urlParams.has('parentNoteId')) {
      this.parentPageId = urlParams.get('parentNoteId');
      this.spaceId = urlParams.get('spaceId');
      this.spaceGroupId  = urlParams.get('spaceGroupId');
      this.spaceDisplayName  = urlParams.get('spaceName');
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
    this.$root.$on('show-alert', this.displayMessage);
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
          this.$refs.noteTreeview.open(note, 'includePages', null, filter);
        });
      }
    });
    this.$root.$on('add-translation', lang => {
      this.addTranslation(lang);
    });
    this.$root.$on('lang-translation-changed', lang => {
      this.changeTranslation(lang);
    });
    this.$root.$on('delete-lang-translation', translation => {
      const noteId= !this.note.draftPage?this.note.id:this.note.targetPageId;
      this.deleteTranslation(translation, noteId);
    });
    this.$root.$on('update-note-title', this.updateNoteTitle);
    this.$root.$on('update-note-content', this.updateNoteContent);
    this.$root.$on('include-page', (note) => {
      const editorSelectedElement = this.editor.getSelection().getStartElement();
      if (this.editor.getSelection().getSelectedText()) {
        if (editorSelectedElement.is('a')) {
          if (editorSelectedElement.getAttribute( 'class' ) === 'noteLink') {
            this.editor.getSelection().getStartElement().remove();
            this.editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${note.name}</a>`);
          }
          if (editorSelectedElement.getAttribute( 'class' ) === 'labelLink') {
            const linkText = editorSelectedElement.getHtml();
            this.editor.getSelection().getStartElement().remove();
            this.editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${linkText}</a>`);
          }
        } else {
          this.editor.insertHtml(`<a href='${note.noteId}' class='labelLink'>${this.editor.getSelection().getSelectedText()}</a>`);
        }
      } else {
        this.editor.insertHtml(`<a href='${note.noteId}' class='noteLink'>${note.name}</a>`);
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
    this.init();
  },
  methods: {
    init() {
      this.initCKEditor();
    },
    autoSave() {
      // No draft saving if init not done or in edit mode for the moment
      if (!this.initCompleted || !this.initActualNoteDone) {
        return;
      }
      // if the Note is being posted, no need to autosave anymore
      if (this.postingNote) {
        return;
      }

      // if the Note is not updated, no need to autosave anymore
      if ((this.note.title && this.note.title === this.actualNote.title) && (this.note.content && this.note.content === this.actualNote.content)) {
        return;
      }
     
      clearTimeout(this.saveDraft);
      this.saveDraft = setTimeout(() => {
        this.savingDraft = true;
        this.draftSavingStatus = this.$t('notes.draft.savingDraftStatus');
        this.$nextTick(() => {
          this.saveNoteDraft(true);
        });
      }, this.autoSaveDelay);
    },
    saveDraftFromLocalStorage(){
      const currentDraft = localStorage.getItem(`draftNoteId-${this.note.id}`);
      if (currentDraft) {
        this.removeLocalStorageCurrentDraft();
        const draftToPersist = JSON.parse(currentDraft);
        this.persistDraftNote(draftToPersist, false);
      }
    },
    getNote(id, lang) {
      if (!lang) {
        lang = this.selectedLanguage;
      }
      this.draftNote = null;
      this.loadedNote = null;
      return this.$notesService.getLatestDraftOfPage(id, lang)
        .then(latestDraft => {
          // check if page has a draft
          latestDraft = Object.keys(latestDraft).length !== 0 ? latestDraft : null;
          if (latestDraft?.id) {
            if (this.editor) {
              this.newDraft = false;
              this.fillNote(latestDraft);
              setTimeout(() => {
                this.displayDraftMessage();
              }, this.autoSaveDelay / 2);
              this.initActualNoteDone = true;
            } else {
              this.draftNote = latestDraft;
            }
          } else {
            return this.$notesService.getNoteById(id, lang)
              .then(data => {
                if (this.editor) {
                  this.fillNote(data);
                  this.newDraft = true;
                  this.initActualNoteDone = true;
                } else {
                  this.loadedNote = data;
                }
              });
          }
        });
    },
    getDraftNote(id) {
      return this.$notesService.getDraftNoteById(id,this.selectedLanguage).then(data => {
        this.init();
        this.fillNote(data);
        if (data?.id) {
          this.displayDraftMessage();
        }
        this.initActualNoteDone = true;
      });
    },
    fillNote(data) {
      this.initActualNoteDone = false;
      if (data) {
        data.content = this.$noteUtils.getContentToEdit(data.content);
        data.content= !data.parentPageId && (data.content===`<h1> Welcome to Space ${this.spaceDisplayName} Notes Home </h1>`) ? '' : data.content;
        this.note = data;
        this.selectedLanguage = data.lang;
        this.getNoteLanguages();
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
        if ((this.note.content.trim().length === 0)) {
          const noteId = !this.note.draftPage ? this.note.id : this.note.targetPageId;
          this.$notesService.getNoteById(noteId,this.selectedLanguage,'','','',true)
            .then(note => {
              if (this.selectedLanguage && !note?.lang) {
                return;
              }
              if (note?.children?.length) {
                this.updateNoteContent(childContainer);
                this.setFocus();
              }
            });
        } else {
          this.updateNoteContent(data.content);
        }
      }
      this.initActualNoteDone = true;
    },
    fillDraftNote() {
      const draftNote = {
        id: this.note.draftPage ? this.note.id : '',
        title: this.note.title,
        content: this.$noteUtils.getContentToSave('notesContent', this.oembedMinWidth) || this.note.content,
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
      return draftNote;
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
            content: this.$noteUtils.getContentToSave('notesContent', this.oembedMinWidth) || this.note.content,
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
            content: this.$noteUtils.getContentToSave('notesContent', this.oembedMinWidth) || this.note.content,
            parentPageId: this.parentPageId,
            toBePublished: toPublish,
            appName: this.appName,
          };
        }
        let notePath = '';
        if (note.id) {
          this.$notesService.updateNoteById(note).then(data => {
            this.removeLocalStorageCurrentDraft();
            this.redirectAfterSave(data || note);
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
            const draftNote = JSON.parse(localStorage.getItem(`draftNoteId-${this.note.id}-${this.slectedLanguage}`));
            this.deleteDraftNote(draftNote, notePath);
            this.redirectAfterSave(data || note);
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
    redirectAfterSave(note) {
      if (this.webPageUrl) {
        window.location.href = this.webPageUrl;
      } else {
        const notePath = this.$notesService.getPathByNoteOwner(note, this.appName).replace(/ /g, '_');
        this.draftSavingStatus = '';
        let translation = '';
        if (this.selectedLanguage){
          translation = `?translation=${this.selectedLanguage}`;
        } else {
          translation = '?translation=original';
        }
        window.location.href = `${notePath}${translation}`;
      }
    },
    openPublishAndPost(event) {
      this.publishAndPost = !this.publishAndPost;
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
    },
    saveNoteDraft(update) {
      const draftNote = this.fillDraftNote();
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
            this.persistDraftNote(draftNote,update);
          }
        }
      } else {
        // delete draft
        this.deleteDraftNote();
      }
    },
    persistDraftNote(draftNote,update) {
      clearTimeout(this.saveDraft);
      draftNote.lang=this.selectedLanguage;
      if (this.note.title || this.note.content) {
        if (this.newDraft){
          draftNote.id=null;
        }
        this.$notesService.saveDraftNote(draftNote, this.parentPageId).then(savedDraftNote => {
          if (update){
            this.actualNote = {
              id: savedDraftNote.id,
              name: savedDraftNote.name,
              title: savedDraftNote.title,
              content: savedDraftNote.content,
              author: savedDraftNote.author,
              owner: savedDraftNote.owner,
            };
            this.newDraft=false;
            savedDraftNote.parentPageId = this.parentPageId;
            this.note = savedDraftNote;
            localStorage.setItem(`draftNoteId-${this.note.id}`, JSON.stringify(savedDraftNote));
          } else {
            this.removeLocalStorageCurrentDraft();
          }
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
      if (this.editor?.destroy) {
        this.editor.destroy(true);
      }

      CKEDITOR.dtd.$removeEmpty['i'] = false;
      
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

      $(this.$refs.notesContent).ckeditor({
        customConfig: `${eXo.env.portal.context}/${eXo.env.portal.rest}/richeditor/configuration?type=notes&v=${eXo.env.client.assetsVersion}`,
        allowedContent: true,
        spaceURL: self.spaceURL,
        spaceGroupId: `/spaces/${this.spaceGroupId}`,
        imagesDownloadFolder: 'DRIVE_ROOT_NODE/notes/images',
        toolbarLocation: 'top',
        extraAllowedContent: 'table[summary];img[style,class,src,referrerpolicy,alt,width,height];span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*];',
        removeButtons: '',
        enterMode: CKEDITOR.ENTER_P,
        shiftEnterMode: CKEDITOR.ENTER_BR,
        copyFormatting_allowedContexts: true,
        indentBlock: {
          offset: 40,
          unit: 'px'
        },
        format_tags: 'p;h1;h2;h3',
        bodyClass: 'notesContent',
        dialog_noConfirmCancel: true,
        colorButton_enableMore: true,
        isImagePasteBlocked: this.webPageNote,
        hideUploadImageLink: this.webPageNote,
        isImageDragBlocked: this.webPageNote,
        sharedSpaces: {
          top: 'notesTop'
        },
        on: {
          instanceReady: function (evt) {
            self.editor = evt.editor;
            self.actualNote.content = self.editor.getData();
            self.editor.removeMenuItem('linkItem');
            self.editor.removeMenuItem('selectImageItem');
            $(self.editor.document.$)
              .find('.atwho-inserted')
              .each(function() {
                $(this).on('click', '.remove', function() {
                  $(this).closest('[data-atwho-at-query]').remove();
                });
              });

            const treeviewParentWrapper =  self.editor.window.$.document.getElementById('note-children-container');
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
              });
            }
            window.setTimeout(() => self.setFocus(), 50);
            self.$root.$applicationLoaded();
            self.instanceReady = true;
          },
          change: function (evt) {
            if (!self.initialized) {
              // First time setting data
              self.initialized = true;
              return;
            }
            self.note.content = evt.editor.getData();
            self.autoSave();
            const removeTreeviewBtn =  evt.editor.document.getById( 'remove-treeview' );
            if (removeTreeviewBtn) {
              evt.editor.editable().attachListener(removeTreeviewBtn, 'click', function() {
                const treeviewParentWrapper = evt.editor.document.getById( 'note-children-container' );
                if ( treeviewParentWrapper) {
                  const newLine = treeviewParentWrapper.getNext();
                  treeviewParentWrapper.remove();
                  if ( newLine.$.innerText.trim().length === 0) {
                    newLine.remove();
                  }
                  self.note.content = evt.editor.getData();
                }
              });
            }
          },
          fileUploadResponse: function() {
            /*add plugin fileUploadResponse to handle file upload response ,
              in this method we can get the response from server and update the editor content
              this method is called when file upload is finished*/
            self.editor.once('afterInsertHtml', ()=> {
              window.setTimeout(() => {
                self.editor.fire('mode');
              }, 2000);
            });
          },
          doubleclick: function(evt) {
            const element = evt.data.element;
            if ( element && element.is('a')) {
              const noteId = element.getAttribute( 'href' );
              self.$notesService.getNoteById(noteId,this.selectedLanguage).then(data => {
                const note = data;
                self.$refs.noteTreeview.open(note, 'includePages', 'no-arrow');
              });
            }
          }
        }
      });
    },
    setToolBarEffect() {
      const elementNewTop = document.getElementById('notesTop');
      if (this.editor){
        this.editor.on('contentDom', function () {
          this.document.on('click', function(){
            elementNewTop.classList.add('darkComposerEffect');
          });
        });
        this.editor.on('contentDom', function () {
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
      } else if (this.editor) {
        window.setTimeout(() => {
          this.$nextTick().then(() => this.editor.focus());
        }, 200);
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
    displayDraftMessage() {
      if (!this.draftNote) {
        return;
      }
      let draftMessage = `${this.$t('notes.alert.warning.label.original.draft.drop')}, `;
      if (this.selectedLanguage) {
        draftMessage = `${this.$t('notes.alert.warning.label.draft.drop')}, `;
        draftMessage = draftMessage.replace('{0}', this.getLanguageName(this.note.lang));
      }
      this.displayMessage({
        type: 'warning',
        message: `
          <span class="pe-1">${draftMessage}</span>
          <span>${this.$dateUtil.formatDateObjectToDisplay(new Date(this.note.updatedDate.time), this.dateTimeFormat, this.lang)}</span>
        `,
        linkText: this.$t('notes.label.drop.draft'),
        linkCallback: () => this.dropDraft(),
      });
    },
    displayMessage(message) {
      document.dispatchEvent(new CustomEvent('alert-message-html', {detail: {
        alertMessage: message?.message,
        alertType: message?.type,
        alertLinkText: message?.linkText,
        alertLinkCallback: message?.linkCallback,
      }}));
    },
    closeAlertMessage() {
      document.dispatchEvent(new CustomEvent('close-alert-message'));
    },
    displayFormTitle() {
      const urlParams = new URLSearchParams(window.location.search);
      const webPageName = urlParams.get('pageName');
      if (webPageName) {
        this.noteFormTitle = this.$t('notes.edit.editTextFor', {
          0: webPageName,
        });
      } else if (this.noteId) {
        this.noteFormTitle = this.$t('notes.edit.editNotes');
      } else {
        return this.$spaceService.getSpaceById(this.spaceId).then(space => {
          this.noteFormTitle = this.$t('notes.composer.createNotes').replace('{0}', space.displayName);
        });
      }
    },
    dropDraft() {
      if (this.note.draftPage && this.note.id) {
        this.$notesService.deleteDraftNote(this.note)
          .then(() => {
            this.removeLocalStorageCurrentDraft();
            return this.getNote(this.note.targetPageId, this.selectedLanguage);
          })
          .catch(e => console.error('Error when deleting draft note', e));
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
      if (this.noteId){
        this.showTranslationBar=true;
        this.$refs.translationsEditBar.show(this.selectedLanguage);
      }
    },
    hideTranslations() {
      this.showTranslationBar=false;
      this.$refs.translationsEditBar.hide();
    },
    isDefaultContent(noteContent) {
      const div = document.createElement('div');
      div.innerHTML = noteContent;
      if ( div.childElementCount === 2) {
        const childrenWrapper = this.editor.window.$.document.getElementById('note-children-container');
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
    getNoteLanguages(){
      const noteId= !this.note.draftPage?this.note.id:this.note.targetPageId;
      return this.$notesService.getNoteLanguages(noteId,true).then(data => {
        this.translations =  data || [];
        if (this.translations.length>0) {
          this.translations = this.allLanguages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
          this.languages = this.allLanguages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
        if (this.isMobile) {
          this.translations.unshift({value: null,text: this.$t('notes.label.translation.originalVersion')});
        }
        if (!this.selectedLanguage){
          const lang = this.translations.find(item => item.value === this.selectedLanguage);
          if (lang){
            this.translations=this.translations.filter(item => item.value !== lang.value);
            this.translations.unshift(lang);
          }
        }
      });
    },
    getAvailableLanguages(){
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
        this.languages.sort((a, b) => a.text.localeCompare(b.text));
        this.allLanguages=this.languages;
        this.languages.unshift({value: '',text: this.$t('notes.label.chooseLangage')});
        if (this.translations){
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
      });
    },
    getLanguageName(lang){
      const language = this.allLanguages.find(item => item.value === lang);
      return language?language.text:lang;
    },
    deleteTranslation(translation,noteId){
      return this.$notesService.deleteNoteTranslation(noteId,translation.value).then(() => {
        this.translations=this.translations.filter(item => item.value !== translation.value);
        const messageObject = {
          type: 'success',
          message: this.$t('notes.alert.success.label.translation.deleted')
        };
        this.displayMessage(messageObject);
      });

    },
    addTranslation(lang){
      this.closeAlertMessage();
      if (!this.postingNote && this.note.draftPage && this.note.id) {
        this.saveDraftFromLocalStorage();
      }
      const originNoteContent = {
        title: this.note.title,
        content: this.note.content,
        lang: lang?.value
      };
      this.languages = this.languages.filter(item => item.value !== lang?.value);
      this.selectedLanguage = lang?.value;
      this.translations.unshift(lang);
      if (this.webPageNote) {
        this.note.title = this.selectedLanguage && `${this.note.title}_${this.selectedLanguage}` || this.note.title;
      } else {
        this.note.title = '';
      }
      this.note.content = '';
      this.note.lang = this.selectedLanguage;
      this.newDraft=true;
      this.initCKEditor();
      document.dispatchEvent(new CustomEvent('translation-added',{ detail: originNoteContent }));
    },
    updateNoteTitle(title) {
      this.note.title = title;
    },
    updateNoteContent(content) {
      this.note.content = content;
      if (this.editor) {
        this.editor.setData(content);
      } else {
        this.$refs.notesContent.value = content;
      }
    },
    changeTranslation(lang){
      this.closeAlertMessage();
      if (!this.postingNote && this.note.draftPage && this.note.id) {
        this.saveDraftFromLocalStorage();
      }
      this.selectedLanguage = lang.value;
      if (lang.value || this.isMobile) {
        this.translations=this.translations.filter(item => item.value !== lang.value);
        this.translations.unshift(lang);
      }
      const noteId = !this.note.draftPage ? this.note.id : this.note.targetPageId;
      return this.getNote(noteId, lang.value)
        .finally(() => this.updateUrl());
    },
    updateUrl() {
      const url = new URL(window.location.href);
      const params = new URLSearchParams(url.search);
      params.delete('translation');
      if (this.selectedLanguage) {
        params.append('translation', this.selectedLanguage);
      }
      window.history.pushState('notes', '', `${url.origin}${url.pathname}?${params.toString()}`);
    },
    refreshTranslationExtensions() {
      this.noteEditorExtensions = extensionRegistry.loadExtensions('notesEditor', 'translation-extension');
    },
  }
};
</script>
