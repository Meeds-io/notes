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
    <note-full-rich-editor
      ref="editor"
      :note="note"
      :draft-saving-status="draftSavingStatus"
      :note-id-param="noteId"
      :post-key="postKey"
      :body-placeholder="notesBodyPlaceholder"
      :title-placeholder="notesTitlePlaceholder"
      :form-title="noteFormTitle"
      :app-name="appName"
      :web-page-note="webPageNote"
      :web-page-url="webPageUrl"
      :languages="languages"
      :translations="translations"
      :selected-language="selectedLanguage"
      :lang-button-tooltip-text="langButtonTooltipText"
      :publish-button-text="$t('notes.button.publish')"
      :editor-icon="editorIcon"
      :space-group-id="`/spaces/${spaceGroupId}`"
      :suggester-space-url="spaceGroupId"
      :save-button-icon="saveButtonIcon"
      :is-mobile="isMobile"
      :save-button-disabled="saveOrUpdateDisabled"
      :editor-body-input-ref="'notesContent'"
      :editor-title-input-ref="'noteTitle'"
      @open-treeview="openTreeView"
      @post-note="postNote"
      @auto-save="autoSave"
      @editor-ready="editorReady"
      @update-data="updateNoteData"
      @editor-closed="editorClosed" />
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
      originalNote: {},
      note: {
        id: '',
        title: '',
        content: '',
        parentPageId: '',
        properties: {}
      },
      actualNote: {
        id: '',
        title: '',
        content: '',
        parentPageId: '',
        properties: {}
      },
      alert: false,
      alertType: '',
      message: '',
      noteId: '',
      parentPageId: '',
      appName: 'notes',
      notesTitlePlaceholder: `${this.$t('notes.title.placeholderContentInput')}*`,
      notesBodyPlaceholder: `${this.$t('notes.body.placeholderContentInput')}`,
      spaceId: '',
      noteFormTitle: '',
      postingNote: false,
      savingDraft: false,
      initDone: false,
      initActualNoteDone: false,
      draftSavingStatus: '',
      autoSaveDelay: 1000,
      titleMaxLength: 1000,
      saveDraft: '',
      postKey: 1,
      navigationLabel: `${this.$t('notes.label.Navigation')}`,
      noteNavigationDisplayed: false,
      spaceGroupId: null,
      oembedMinWidth: 300,
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
      editorIcon: 'fas fa-clipboard',
      saveButtonIcon: 'fas fa-save',
      translationSwitch: false,
    };
  },
  computed: {
    saveOrUpdateDisabled() {
      return (!this.note?.title || this.note?.title?.length < 3
                                || this.note?.title?.length > this.titleMaxLength)
                                || (this.noteNotModified
                                && !this.propertiesModified && !this.draftNote) || this.savingDraft;
    },
    noteNotModified() {
      return this.note?.title === this.originalNote?.title && this.note?.content === this.originalNote?.content;
    },
    propertiesModified() {
      return JSON.stringify(this.note?.properties) !== JSON.stringify(this.originalNote?.properties);
    },
    langButtonTooltipText() {
      if (this.noteId) {
        return this.$t('notes.label.button.translations.options');
      } else {
        return this.$t('notes.message.firstVersionShouldBeCreated');
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
    isMobile() {
      return this.$vuetify.breakpoint.width < 960;
    }
  },
  watch: {
    'note.title'() {
      if (this.note?.title !== this.actualNote.title ) {
        this.autoSave();
      }
    },
    draftNote() {
      if (!this.draftNote) {
        this.$root.$emit('close-alert-message');
      }
    },
    'note.content'() {
      if (!this.$noteUtils.isSameContent(this.note?.content, this.actualNote?.content)) {
        this.autoSave();
      }
    },
  },
  mounted() {
    this.init();
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
    this.displayFormTitle(urlParams);
    this.$root.$on('show-alert', this.displayMessage);
    this.$root.$on('display-treeview-items', filter => {
      if ( urlParams.has('noteId') ) {
        this.openTreeView(urlParams.get('noteId'), 'includePages', null, filter);
      } else if (urlParams.has('parentNoteId')) {
        this.openTreeView(urlParams.get('parentNoteId'), 'includePages', null, filter);
      }
    });
    this.$root.$on('add-translation', this.addTranslation);
    this.$root.$on('lang-translation-changed', this.changeTranslation);
    this.$root.$on('delete-lang-translation', this.deleteTranslation);

    document.addEventListener('note-navigation-plugin', () => {
      this.$root.$emit('show-alert', {
        type: 'error',
        message: this.$t('notes.message.manualChild')
      });
    });
  },
  methods: {
    editorClosed() {
      window.close();
    },
    closePluginsDrawer() {
      this.$refs.editor.closePluginsDrawer();
    },
    openTreeView(noteId, source, includeDisplay, filter) {
      this.$notesService.getNoteById(noteId, this.selectedLanguage).then(note => {
        this.$refs.noteTreeview.open(note, source, includeDisplay, filter);
      });
    },
    init() {
      this.$refs.editor.initCKEditor();
    },
    editorReady(editor) {
      this.editor = editor;
      this.actualNote.content = editor.getData();
      if (this.editor) {
        if (this.draftNote?.id || this.loadedNote?.id) {
          this.fillNote(this.draftNote || this.loadedNote);
          this.displayDraftMessage();
          this.initActualNoteDone = true;
          this.newDraft = !this.draftNote;
        }
        this.initDone = true;
      }
    },
    updateNoteData(noteObject) {
      this.translationSwitch = this.note?.lang !== noteObject?.lang;
      this.note.title = noteObject.title;
      this.note.content = noteObject.content;
      this.note.properties = noteObject.properties;
    },
    postNote(toPublish) {
      this.postingNote = true;
      clearTimeout(this.saveDraft);
      const properties = this.note?.properties;
      if (properties) {
        properties.draft = this.note?.draftPage;
      }
      const note = {
        id: this.note?.draftPage? this.note.targetPageId || null : this.note?.id,
        title: this.note.title,
        name: this.note.name,
        lang: this.note.lang,
        wikiType: this.note.wikiType,
        wikiOwner: this.note.wikiOwner,
        content: this.$noteUtils.getContentToSave('notesContent', this.oembedMinWidth) || this.note.content,
        parentPageId: this.note?.draftPage && this.note?.targetPageId === this.parentPageId ? null : this.parentPageId,
        toBePublished: toPublish,
        appName: this.appName,
        properties: properties
      };
      if (note.id) {
        this.updateNote(note);
      } else {
        this.createNote(note);
      }
      this.draftNote = null;
    },
    addParamToUrl(paramName, paramValue) {
      const url = new URL(window.location.href);
      url.searchParams.set(paramName, paramValue);
      history.pushState({}, null, url.toString());
    },
    updateNote(note) {
      return this.$notesService.updateNoteById(note).then(data => {
        this.note = data;
        this.originalNote = structuredClone(data);
        this.displayMessage({
          type: 'success',
          message: this.$t('notes.save.success.message'),
          linkText: this.$t('notes.view.label'),
          alertLink: this.redirectAfterSaveLink(data || note)
        });
      }).catch(e => {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      }).finally(() => this.enableClickOnce(),this.removeLocalStorageCurrentDraft());
    },
    createNote(note) {
      return this.$notesService.createNote(note).then(data => {
        this.note = data;
        this.noteId = data.id;
        this.addParamToUrl('noteId', this.noteId);
        this.originalNote = structuredClone(data);
        const notePath = this.$notesService.getPathByNoteOwner(data, this.appName).replace(/ /g, '_');
        // delete draft note
        const draftNote = JSON.parse(localStorage.getItem(`draftNoteId-${this.note.id}-${this.slectedLanguage}`));
        this.deleteDraftNote(draftNote, notePath);
        this.displayMessage({
          type: 'success',
          message: this.$t('notes.save.success.message'),
          linkText: this.$t('notes.view.label'),
          alertLink: this.redirectAfterSaveLink(data || note)
        });
      }).catch(e => {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      }).finally(() => this.enableClickOnce(),this.removeLocalStorageCurrentDraft());
    },
    autoSave() {
      if (this.translationSwitch) {
        return;
      }
      // No draft saving if init not done or in edit mode for the moment
      if (!this.initCompleted || !this.initActualNoteDone) {
        return;
      }
      // if the Note is being posted, no need to autosave anymore
      if (this.postingNote) {
        return;
      }

      // if the Note is not updated, no need to autosave anymore
      if ((this.note?.title === this.actualNote.title) && (this.note?.content === this.actualNote.content)
          && (JSON.stringify(this.note?.properties) === JSON.stringify(this.actualNote?.properties))) {
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
        this.originalNote = structuredClone(this.note);
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
          properties: this.note?.properties
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
                this.$refs.editor.setFocus();
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
        properties: this.note?.properties
      };
      if (this.note.draftPage && this.note.id) {
        draftNote.targetPageId = this.note.targetPageId;
      } else {
        draftNote.targetPageId = this.note.id ? this.note.id : '';
      }
      return draftNote;
    },
    redirectAfterSaveLink(note) {
      if (this.webPageUrl) {
        return this.webPageUrl;
      } else {
        const notePath = this.$notesService.getPathByNoteOwner(note, this.appName).replace(/ /g, '_');
        this.draftSavingStatus = '';
        return `${notePath}?translation=${this.selectedLanguage || 'original'}`;
      }
    },
    saveNoteDraft(update) {
      const draftNote = this.fillDraftNote();
      if (this.note.title || this.note.content) {
        // if draft page not created persist it only the first time else update it in browser's localStorage
        if (this.note.draftPage && this.note.id && !this.note?.lang
            && !this.note?.properties?.featuredImage?.uploadId) {
          this.note.parentPageId = this.parentPageId;
          localStorage.setItem(`draftNoteId-${this.note.id}`, JSON.stringify(draftNote));
          this.actualNote = {
            name: draftNote.name,
            title: draftNote.title,
            content: draftNote.content,
            properties: draftNote.properties
          };
          setTimeout(() => {
            this.draftSavingStatus = this.$t('notes.draft.savedDraftStatus');
            this.savingDraft = false;
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
      draftNote.lang = this.selectedLanguage;
      if (this.note.title || this.note.content) {
        if (this.newDraft){
          draftNote.id = null;
        }
        if (draftNote.properties) {
          draftNote.properties.draft = true;
          draftNote.properties.noteId = this.note?.targetPageId || this.note?.id;
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
              properties: savedDraftNote.properties
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
        alertLink: message.alertLink
      }}));
    },
    closeAlertMessage() {
      document.dispatchEvent(new CustomEvent('close-alert-message'));
    },
    displayFormTitle(urlParams) {
      const webPageName = urlParams.get('pageName');
      if (webPageName || this.noteId) {
        this.noteFormTitle = this.$t('notes.edit.editNotes');
      } else {
        this.noteFormTitle = this.$t('notes.composer.createNotes');
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
    getNoteLanguages() {
      const noteId = !this.note.draftPage ? this.note.id : this.note.targetPageId;
      return this.$notesService.getNoteLanguages(noteId, true).then(data => {
        this.translations ??= [];
        this.translations = this.translations.map(translation => translation.value);
        this.translations.push(...data);
        if (this.translations.length > 0) {
          this.translations = this.allLanguages.filter(lang => this.translations.includes(lang.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
          this.languages = this.allLanguages.filter(lang => !this.translations.includes(lang.value));
        }
        if (this.isMobile) {
          this.translations.unshift({value: null, text: this.$t('notes.label.translation.originalVersion')});
        }
        if (!this.selectedLanguage) {
          const lang = this.translations.find(item => item.value === this.selectedLanguage);
          if (lang) {
            this.translations = this.translations.filter(item => item.value !== lang.value);
            this.translations.unshift(lang);
          }
        }
      });
    },
    getAvailableLanguages() {
      this.languages = this.allLanguages = JSON.parse(eXo?.env?.portal?.availableLanguages)
        ?.sort((a, b) => a.text.localeCompare(b.text));
      this.languages.unshift({value: '', text: this.$t('notes.label.chooseLangage')});
      if (this.translations) {
        this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
      }
    },
    getLanguageName(lang){
      const language = this.allLanguages.find(item => item.value === lang);
      return language?language.text:lang;
    },
    deleteTranslation(translation){
      const noteId = !this.note?.draftPage && this.note?.id || this.note?.targetPageId;
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
      if (!this.translations && this.note?.id) {
        this.getNoteLanguages(this.note.id);
      }
      this.closeAlertMessage();
      if (!this.postingNote && this.note.draftPage && this.note.id) {
        this.saveDraftFromLocalStorage();
      }
      const originNoteContent = {
        title: this.note.title,
        content: this.note.content,
        properties: structuredClone(this.note?.properties),
        lang: lang?.value
      };
      this.languages = this.languages.filter(item => item.value !== lang?.value);
      this.selectedLanguage = lang?.value;
      this.translations ??= [];
      this.translations.unshift(lang);
      if (this.webPageNote) {
        this.note.title = this.selectedLanguage && `${this.note.title}_${this.selectedLanguage}` || this.note.title;
      } else {
        this.note.title = '';
      }
      this.note.content = '';
      this.note.lang = this.selectedLanguage;
      this.newDraft = true;
      this.$refs.editor.resetEditorData();
      document.dispatchEvent(new CustomEvent('translation-added',{ detail: originNoteContent }));
    },
    updateNoteContent(content) {
      this.note.content = content;
      this.$refs.editor.setEditorData(content);
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
