<template>
  <v-app
    class="transparent"
    role="main"
    flat>
    <div>
      <div
        v-if="isAvailableNote"
        class="notes-application white card-border-radius pa-5"
        ref="content">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between pb-4 ps-1">
            <span
              ref="noteTitle"
              class="title text-color mt-n1">
              {{ noteTitle }}
            </span>
            <div
              id="note-actions-menu"
              v-show="loadData && !hideElementsForSavingPDF"
              class="notes-header-icons text-right">
              <div
                class="d-inline-flex">
                <v-tooltip bottom v-if="!isMobile && !hasDraft && isManager">
                  <template #activator="{ on, attrs }">
                    <v-btn
                      v-on="on"
                      v-bind="attrs"
                      class="pa-0 mt-0"
                      @click="addNote"
                      icon>
                      <v-icon
                        size="16"
                        class="clickable add-note-click">
                        fas fa-plus
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.addPage') }}</span>
                </v-tooltip>
              </div>
              <div
                class="d-inline-flex">
                <v-tooltip bottom v-if="isManager && !isMobile">
                  <template #activator="{ on, attrs }">
                    <v-btn
                      icon
                      v-on="on"
                      v-bind="attrs"
                      class="pa-0 mt-0"
                      @click="editNote">
                      <v-icon
                        size="16"
                        class="clickable edit-note-click">
                        fas fa-edit
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.editPage') }}</span>
                </v-tooltip>
              </div>

              <note-favorite-action
                :note="note"
                :activity-id="note.activityId" />
              <div
                class="d-inline-flex">
                <v-tooltip bottom>
                  <template #activator="{ on, attrs }">
                    <v-btn
                      v-on="on"
                      @click="$root.$emit('display-action-menu')"
                      v-bind="attrs"
                      class="pa-0 mt-0"
                      icon>
                      <v-icon
                        size="16"
                        class="clickable">
                        fas fa-ellipsis-v
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.openMenu') }}</span>
                </v-tooltip>
              </div>
            </div>
          </div>
          <div v-if="!hideElementsForSavingPDF" class="notes-treeview d-flex flex-inline">
            <v-tooltip bottom>
              <template #activator="{ on, attrs }">
                <v-btn
                  @click="$refs.notesBreadcrumb.open(note, 'displayNote')"
                  v-on="on"
                  class="pa-0"
                  min-width="24"
                  v-bind="attrs"
                  text>
                  <i
                    class="uiIcon uiTreeviewIcon primary--text"></i>
                </v-btn>
              </template>
              <span class="caption">{{ $t('notes.label.noteTreeview.tooltip') }}</span>
            </v-tooltip>
            <note-breadcrumb
              class="pt-2 pe-1 pl-1"
              :note-breadcrumb="notebreadcrumb"
              :actual-note-id="note.id"
              @open-note="getNoteByName($event, 'breadCrumb', true)" />
          </div>
          <div v-show="!hideElementsForSavingPDF" class="notes-last-update-info">
            <notes-translation-menu
              :note="note"
              :translations="translations"
              :selected-translation="selectedTranslation"
              @change-translation="changeTranslation" />
            <span
              v-show="lastNoteVersion"
              class="note-version border-radius primary my-auto px-2 font-weight-bold me-2 caption clickable"
              @click="openNoteVersionsHistoryDrawer(noteVersions, isManager)">V{{ lastNoteVersion }}</span>
            <span class="caption text-sub-title font-italic">{{ $t('notes.label.LastModifiedBy', {0: lastNoteUpdatedBy, 1: displayedDate}) }}</span>
          </div>
        </div>
        <v-divider class="my-4" />
        <div class="note-content" v-if="!hasEmptyContent && !isHomeNoteDefaultContent">
          <div
            class="notes-application-content text-color">
            <component :is="notesContentProcessor" />
          </div>
        </div>
        <div v-else-if="!hasChildren || hasDraft && hasEmptyContent">
          <div v-if="isManager" class="notes-application-content d-flex flex-column justify-center text-center">
            <v-img
              :src="emptyNoteNoManager"
              class="mx-auto mb-4"
              max-height="150"
              max-width="250"
              contain
              eager />
            <div>
              <p v-if="!isMobile" class="notes-welcome-patragraph">
                <span>{{ $t('notes.label.no-content-no-redactor.content.first') }}</span>
                <v-tooltip bottom>
                  <template #activator="{ on, attrs }">
                    <v-btn
                      class="pa-0"
                      icon
                      v-on="on"
                      v-bind="attrs"
                      @click="editNote">
                      <v-icon
                        size="16"
                        class="clickable edit-note-click">
                        mdi-square-edit-outline
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.editPage') }}</span>
                </v-tooltip>
                <span v-if="!hasDraft">{{ $t('notes.label.no-content.no-redactor.content.last') }}</span>
                <v-tooltip bottom v-if="!hasDraft">
                  <template #activator="{ on, attrs }">
                    <v-btn
                      class="pa-0"
                      v-on="on"
                      v-bind="attrs"
                      @click="addNote"
                      icon>
                      <v-icon
                        size="16"
                        class="clickable add-note-click">
                        fas fa-plus
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.addPage') }}</span>
                </v-tooltip>
              </p>
            </div>
          </div>
          <div v-else class="notes-application-content d-flex flex-column justify-center text-center text-color">
            <v-img
              :src="emptyNoteWithManager"
              class="mx-auto mb-4"
              max-height="150"
              max-width="250"
              contain
              eager />
            <div>
              <h4 class="notes-welcome-title font-weight-bold text-color">
                {{ $t('notes.label.no-content-redactor-title').replace('{0}', spaceDisplayName) }}
              </h4>
              <p class="notes-welcome-patragraph">
                <span>{{ $t('notes.label.no-content.redactor.content.first') }}</span>
                <a :href="spaceMembersUrl" class="text-decoration-underline">{{ $t('notes.label.no-content-manager') }}</a>
                <span>{{ $t('notes.label.or') }}</span>
                <a :href="spaceMembersUrl" class="text-decoration-underline">{{ $t('notes.label.no-content-redactor') }}</a>
                <span>{{ $t('notes.label.no-content.redactor.content.last') }}</span>
              </p>
            </div>
          </div>
        </div>

        <div v-else class="notes-application-content">
          <v-treeview
            v-if="noteChildren && noteChildren[0]"
            dense
            :items="noteAllChildren"
            class="ps-1"
            item-key="noteId">
            <template #label="{ item }">
              <note-content-table-item :note="item" />
            </template>
          </v-treeview>
        </div>
      </div>
      <div v-else class="note-not-found-wrapper text-center mt-6">
        <v-img
          :src="noteNotFountImage"
          class="mx-auto"
          max-height="150"
          max-width="250"
          contain
          eager />
        <p class="title mt-3 text-light-color">{{ $t('notes.label.noteNotFound') }}</p>
        <a
          class="btn btn-primary"
          :href="defaultPath">
          {{ $t('notes.label.noteNotFound.button') }}
        </a>
      </div>
    </div>
    <notes-actions-menu
      :note="note"
      :default-path="defaultPath"
      @open-treeview="$refs.notesBreadcrumb.open(note, 'movePage')"
      @export-pdf="createPDF(note)"
      @open-history="openNoteVersionsHistoryDrawer()"
      @open-treeview-export="$refs.notesBreadcrumb.open(note.id, 'exportNotes')"
      @open-import-drawer="$refs.noteImportDrawer.open()" />
    <note-treeview-drawer
      ref="notesBreadcrumb" />
    <version-history-drawer
      :versions="noteVersionsArray"
      :can-manage="this.note.canManage"
      :show-load-more="hasMoreVersions"
      @open-version="displayVersion($event)"
      @restore-version="restoreVersion($event)"
      @load-more="loadMoreVersions"
      ref="noteVersionsHistoryDrawer" />
    <note-import-drawer
      ref="noteImportDrawer" />
    <exo-confirm-dialog
      ref="DeleteNoteDialog"
      :message="confirmMessage"
      :title="hasDraft ? $t('popup.confirmation.delete.draft') : $t('popup.confirmation.delete')"
      :ok-label="$t('notes.button.ok')"
      :cancel-label="$t('notes.button.cancel')"
      persistent
      @ok="deleteNote()"
      @dialog-opened="$emit('confirmDialogOpened')"
      @dialog-closed="$emit('confirmDialogClosed')" />
  </v-app>
</template>
<script>

import { notesConstants } from '../../../javascript/eXo/wiki/notesConstants.js';
import html2canvas from 'html2canvas';
import JSPDF from 'jspdf';

export default {
  data() {
    return {
      versionsPageSize: null,
      note: {},
      lastUpdatedTime: '',
      lang: eXo.env.portal.language,
      dateTimeFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      },
      dateTimeFormatZip: {
        year: 'numeric',
        month: 'numeric',
        day: 'numeric',
      },
      confirmMessage: '',
      spaceDisplayName: eXo.env.portal.spaceDisplayName,
      spaceId: eXo.env.portal.spaceId,
      noteBookType: eXo.env.portal.spaceName ? 'group' : 'user',
      noteBookOwner: eXo.env.portal.spaceGroup ? `/spaces/${eXo.env.portal.spaceGroup}` : eXo.env.portal.profileOwner,
      noteNotFountImage: '/notes/skin/images/notes_not_found.png',
      emptyNoteWithManager: '/notes/images/no-content-with-manager.png',
      emptyNoteNoManager: '/notes/images/no-content-no-manager.png',
      defaultPath: 'Home',
      existingNote: true,
      currentPath: window.location.pathname,
      currentNoteBreadcrumb: [],
      loadData: false,
      openTreeView: false,
      hideElementsForSavingPDF: false,
      noteVersions: [],
      actualVersion: {},
      noteContent: '',
      displayLastVersion: true,
      noteChildren: [],
      isDraft: false,
      noteTitle: '',
      spaceMembersUrl: `${eXo.env.portal.context}/g/:spaces:${eXo.env.portal.spaceGroup}/${eXo.env.portal.spaceUrl}/members`,
      childNodes: [],
      exportStatus: '',
      exportId: 0,
      popStateChange: false,
      iframelyOriginRegex: /^https?:\/\/if-cdn.com/,
      selectedTranslation: {value: eXo.env.portal.language},
      translations: [],
      languages: [],
      slectedLanguage: null,
      originalVersion: { value: null, text: this.$t('notes.label.translation.originalVersion') },
    };
  },
  watch: {
    note() {
      if (!this.note.draftPage) {
        this.getNoteVersionByNoteId(this.note.id);
      }
      if ( this.note && this.note.breadcrumb && this.note.breadcrumb.length ) {
        this.note.breadcrumb[0].title = this.getHomeTitle(this.note.breadcrumb[0].title);
        this.currentNoteBreadcrumb = this.note.breadcrumb;
      }
      this.noteTitle = !this.note.parentPageId && this.note.title==='Home' ? `${this.$t('notes.label.noteHome')}` : this.note.title;
      this.noteContent = this.note.content;
      this.retrieveNoteTreeById();
    },
    actualVersion() {
      if (!this.isDraft && this.actualVersion) {
        this.noteContent = this.actualVersion.content;
        this.displayLastVersion = false;
      }
    },
    exportStatus(){
      if (this.exportStatus.status==='ZIP_CREATED'){
        this.stopGetStatus();
        this.getExportedZip();
        this.exportStatus={};
      }
      if (this.exportStatus.status===null){
        this.stopGetStatus();
        this.exportStatus={};
      }
    }
  },
  computed: {
    notesContentProcessor() {
      return {
        template: `<div class='rich-editor-content extended-rich-content'>${this.$noteUtils.getContentToDisplay(this.noteContent, this.note?.id, this.noteBookType, this.noteBookOwner, true)}</div>`,
        data() {
          return {
            vTreeComponent: {
              template: `
                <v-treeview
                  dense
                  :items="noteChildItems"
                  class="ps-1"
                  item-key="noteId">
                  <template #label="{ item }">
                    <note-content-table-item :note="item" />
                  </template>
                </v-treeview >`,
              props: {
                noteId: 0,
                source: '',
                noteBookType: '',
                noteBookOwner: ''
              },
              data: function () {
                return {
                  noteChildItems: [],
                  selectedTranslation: {value: eXo.env.portal.language},
                  note: null
                };
              },
              created: function () {
                this.$nextTick().then(() => {
                  this.getNodeById(this.noteId, this.source, this.noteBookType, this.noteBookOwner);
                });
              },
              methods: {
                getNodeById(noteId, source, noteBookType, noteBookOwner) {
                  return this.$notesService.getNoteById(noteId,this.selectedTranslation.value, source, noteBookType, noteBookOwner).then(data => {
                    this.note = data || {};
                    this.$notesService.getFullNoteTree(data.wikiType, data.wikiOwner, data.name, false,this.selectedTranslation.value).then(data => {
                      if (data && data.jsonList.length) {
                        const allNotesTreeview = data.jsonList;
                        this.noteChildItems = allNotesTreeview.filter(note => note.name === this.note.title)[0]?.children;
                      }
                    });
                  }).catch(e => {
                    console.error('Error when getting note', e);
                  });
                },
                openNoteChild(item) {
                  const noteName = item.path.split('%2F').pop();
                  this.$root.$emit('open-note-by-name', noteName);
                },
              }
            }
          };
        }
      };
    },
    noteVersionsArray() {
      return this.noteVersions.slice(0, this.versionsPageSize);
    },
    allNoteVersionsCount() {
      return this.noteVersions.length;
    },
    hasMoreVersions() {
      return this.allNoteVersionsCount > this.versionsPageSize;
    },
    noteVersionContent() {
      return this.note.content && this.noteContent && this.$noteUtils.getContentToDisplay(this.noteContent, this.note?.id, this.noteBookType, this.noteBookOwner, true);
    },
    isHomeNoteDefaultContent() {
      return !this.note.parentPageId && ( this.noteContent===`<h1> Welcome to Space ${this.spaceDisplayName} Notes Home </h1>` || this.noteContent === '');
    },
    lastNoteVersion() {
      return this.noteVersions && this.noteVersions[0] && this.noteVersions[0].versionNumber;

    },
    NoteTranslations() {
      return this.translations && this.noteVersions[0] && this.noteVersions[0].versionNumber;

    },
    lastNoteUpdatedBy() {
      if (this.isDraft) {
        return this.note.authorFullName;
      } else {
        if (this.displayLastVersion) {
          return this.noteVersions && this.noteVersions[0] && this.noteVersions[0].authorFullName;
        } else {
          return this.actualVersion.authorFullName;
        }
      }
    },
    noteAllChildren() {
      return this.noteChildren && this.noteChildren.length && this.noteChildren[0].children;
    },
    displayedDate() {
      if (this.isDraft && this.note && this.note.updatedDate && this.note.updatedDate.time) {
        return this.$dateUtil.formatDateObjectToDisplay(new Date(this.note.updatedDate.time), this.dateTimeFormat, this.lang) || '';
      } else {
        if (this.displayLastVersion) {
          return this.noteVersions && this.noteVersions[0] && this.noteVersions[0].updatedDate.time && this.$dateUtil.formatDateObjectToDisplay(new Date(this.noteVersions[0].updatedDate.time), this.dateTimeFormat, this.lang) || '';
        } else {
          return this.$dateUtil.formatDateObjectToDisplay(new Date(this.actualVersion.updatedDate.time), this.dateTimeFormat, this.lang) || '';
        }
      }
    },
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs';
    },

    isAvailableNote() {
      return this.existingNote;
    },
    notebreadcrumb() {
      return this.currentNoteBreadcrumb;
    },
    hasDraft(){
      return !!this.note?.draftPage;
    },
    hasEmptyContent(){
      return !this.note?.content;
    },
    hasChildren(){
      return this.noteChildren && this.noteChildren[0] && this.noteChildren[0].children?.length > 0;
    },
    isManager(){
      return this.note?.canManage;
    },
    notesPageName() {
      if (this.currentPath.endsWith(eXo.env.portal.selectedNodeUri)||this.currentPath.endsWith(`${eXo.env.portal.selectedNodeUri}/`)){
        return 'homeNote';
      } else {
        const noteId = this.currentPath.split(`${eXo.env.portal.selectedNodeUri}/`)[1];
        if (noteId) {
          return noteId;
        } else {
          return 'homeNote';
        }

      }
    },
    noteId() {
      const pathParams = this.currentPath.split('/');
      const noteId = this.isDraft ? this.currentPath.split('/')[pathParams.length - 2] : this.currentPath.split('/')[pathParams.length - 1];
      if (!isNaN(noteId)) {
        return noteId;
      } else {
        return 0;
      }
    },
    appName() {
      const uris = eXo.env.portal.selectedNodeUri.split('/');
      return uris[uris.length - 1];
    },
  },
  created() {
    this.getAvailableLanguages();
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if (urlParams.has('translation')) {
      this.updateSelectedTranslation({value: urlParams.get('translation')});
    } 
    if (this.currentPath.endsWith('draft')) {
      this.isDraft = true;
    }
    this.$root.$on('open-note-by-name', (noteName, isDraft) => {
      if (!isDraft) {
        this.noteId = noteName;
        this.getNoteByName(noteName,'tree',true);
      } else {
        this.getDraftNote(noteName,true);
      }
    });
    this.$root.$on('confirmDeleteNote', () => {
      this.confirmDeleteNote();
    });
    this.$root.$on('show-alert', this.displayMessage);
    this.$root.$on('delete-note', () => {
      this.confirmDeleteNote();
    });
    this.$root.$on('move-page', (note, newParentNote) => {
      this.moveNote(note, newParentNote);
    });
    this.$root.$on('export-notes', (notesSelected,importAll,homeNoteId) => {
      this.exportNotes(notesSelected,importAll,homeNoteId);
    });
    this.$root.$on('cancel-export-notes', () => {
      this.cancelExportNotes();
    });
    this.$root.$on('import-notes', (uploadId,overrideMode) => {
      this.importNotes(uploadId,overrideMode);
    });
    window.addEventListener('popstate', () => {
      this.currentPath = window.location.pathname;
      this.popStateChange = true;
      this.handleChangePages();
    });
    window.addEventListener('message', (event) => {
      if (this.iframelyOriginRegex.exec(event.origin)) {
        const data = JSON.parse(event.data);
        if (data.method === 'open-href') {
          window.open(data.href, '_blank');
        }
      }
    });
    this.$root.$on('update-note-title', this.updateNoteTitle);
    this.$root.$on('update-note-content', this.updateNoteContent);
    this.$root.$on('update-selected-translation', this.updateSelectedTranslation);

  },
  mounted() {
    this.handleChangePages();
  },
  methods: {
    updateNoteTitle(title) {
      this.noteTitle = title;
    },
    updateNoteContent(content) {
      this.noteContent = content;
    },
    updateSelectedTranslation(translation) {
      this.selectedTranslation = translation;
    },
    getNoteLink(noteId) {
      const baseUrl = window.location.href;
      return `${baseUrl.substring(0, baseUrl.lastIndexOf('/') + 1)}${noteId}`;
    },
    loadMoreVersions() {
      this.versionsPageSize += this.versionsPageSize;
    },
    handleChangePages() {
      if (this.noteId) {
        if (this.isDraft) {
          this.getDraftNote(this.noteId, true);
        } else {
          this.getNoteById(this.noteId,'',true);
        }
      } else {
        this.getNoteByName(this.notesPageName,'',true);
      }
    },
    getHomeTitle(title) {
      return title === 'Home' && this.$t('notes.label.noteHome') || title;
    },
    addNote() {
      if (!this.hasDraft) {
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?spaceId=${eXo.env.portal.spaceId}&parentNoteId=${this.note.id}&spaceGroupId=${eXo.env.portal?.spaceGroup}&spaceName=${eXo.env.portal?.spaceDisplayName}&appName=${this.appName}&showMaxWindow=true&hideSharedLayout=true`, '_blank');
      }
    },
    editNote() {
      let translation = '';
      if (this.selectedTranslation.value){
        translation = `&translation=${this.selectedTranslation.value}`;
      }
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?noteId=${this.note.id}&parentNoteId=${this.note.parentPageId ? this.note.parentPageId : this.note.id}&spaceGroupId=${eXo.env.portal?.spaceGroup}&spaceName=${eXo.env.portal?.spaceDisplayName}&appName=${this.appName}&isDraft=${this.isDraft}&showMaxWindow=true&hideSharedLayout=true${translation}`, '_blank');
    },
    deleteNote() {
      if (this.hasDraft) {
        this.$notesService.deleteDraftNote(this.note).then(() => {
          this.getNoteByName(this.notebreadcrumb[this.notebreadcrumb.length - 2].id);
        }).catch(e => {
          console.error('Error when deleting draft note', e);
        });
      } else {
        this.$notesService.deleteNotes(this.note).then(() => {
          this.getNoteByName(this.notebreadcrumb[this.notebreadcrumb.length - 2].id);
        }).catch(e => {
          console.error('Error when deleting note', e);
        });
      }
    },
    moveNote(note, newParentNote){
      note.parentPageId=newParentNote.id;
      this.$notesService.moveNotes(note, newParentNote).then(() => {
        this.getNoteByName(note.name);
        this.$root.$emit('close-note-tree-drawer');
        this.$root.$emit('show-alert', {type: 'success',message: this.$t('notes.alert.success.label.noteMoved')});
      }).catch(e => {
        console.error('Error when move note page', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      });
    },
    exportNotes(notesSelected, exportAll, homeNoteId) {
      const maxExportId = 10000;
      this.exportId = Math.floor(Math.random() * maxExportId);
      if (exportAll) {
        notesSelected = homeNoteId;
      }
      this.$notesService.exportNotes(notesSelected, exportAll,this.exportId);
      this.getExportStatus();
    },
    cancelExportNotes() {
      this.stopGetStatus();
      this.$notesService.cancelExportNotes(this.exportId).then(() => {
        this.$root.$emit('show-alert', {type: 'success', message: this.$t('notes.alert.success.label.export.canceled')});
      }).catch(e => {
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      });
    },
    getExportedZip() {
      const date = this.$dateUtil.formatDateObjectToDisplay(Date.now(), this.dateTimeFormatZip, this.lang);
      this.$notesService.getExportedZip(this.exportId).then((transfer) => {
        return transfer.blob();
      }).then((bytes) => {
        const elm = document.createElement('a');
        elm.href = URL.createObjectURL(bytes);
        elm.setAttribute('download', `${date}_notes_${this.spaceDisplayName}.zip`);
        elm.click();
        this.exportId=0;
        this.$root.$emit('show-alert', {type: 'success', message: this.$t('notes.alert.success.label.exported')});
      }).catch(e => {
        console.error('Error when export note page', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      });
    },
    getExportStatus() {
      this.intervalId = window.setInterval(() =>{
        return this.$notesService.getExportStatus(this.exportId).then(data => {
          this.exportStatus = data;
          this.$refs.notesBreadcrumb.setExportStaus(this.exportStatus);
        }).catch(() => {
          this.stopGetStatus();
        });
      }, 500);
    },
    stopGetStatus(){
      clearInterval(this.intervalId);
    },
    getNoteById(noteId, source, viewNote) {
      return this.$notesService.getNoteById(noteId,this.selectedTranslation.value, source, this.noteBookType, this.noteBookOwner).then(data => {
        this.note = data || {};
        this.isDraft = data.draftPage;
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        this.getNoteLanguages(noteId);
        if (!this.note.lang || this.note.lang === ''){
          this.updateSelectedTranslation(this.originalVersion);
        }
        this.updateURL();
        if (viewNote){
          this.viewNoteStatistics(this.note);
        }
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
        this.existingNote = false;
      }).finally(() => {
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeView-items', this.note);
      });
    },
    importNotes(uploadId,overrideMode){
      this.$notesService.importZipNotes(this.note.id,uploadId,overrideMode).then(() => {
        this.$root.$emit('close-note-tree-drawer');
        this.$root.$emit('show-alert', {type: 'success',message: this.$t('notes.alert.success.label.notes.imported')});
      }).catch(e => {
        console.error('Error when import notese', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });
      });
    },
    getNoteByName(noteName, source, viewNote) {
      return this.$notesService.getNote(this.noteBookType, this.noteBookOwner, noteName, source, this.selectedTranslation.value).then(data => {
        this.note = data || {};
        this.isDraft = data.draftPage;
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        this.getNoteLanguages(this.note.id);
        if (!this.note.lang || this.note.lang === '') {
          this.updateSelectedTranslation(this.originalVersion);
        }
        this.updateURL();
        if (viewNote){
          this.viewNoteStatistics(this.note);
        }
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
        this.existingNote = false;
      }).finally(() => {
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeView-items', this.note);
      });
    },
    getDraftNote(noteId, viewNote) {
      return this.$notesService.getDraftNoteById(noteId,this.selectedTranslation.value).then(data => {
        this.note = {};
        this.note = data || {};
        this.isDraft = true;
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        this.updateURL();
        if (viewNote){
          this.viewNoteStatistics(this.note);
        }
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
        this.existingNote = false;
      }).finally(() => {
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeView-items', this.note);
      });
    },
    confirmDeleteNote: function () {
      let parentsBreadcrumb = '';
      for (let index = 0; index < this.notebreadcrumb.length - 1; index++) {
        parentsBreadcrumb = parentsBreadcrumb.concat(this.notebreadcrumb[index].title);
        if (index < this.notebreadcrumb.length - 2) {
          parentsBreadcrumb = parentsBreadcrumb.concat('>');
        }
      }
      this.confirmMessage = `${this.note.draftPage ? this.$t('popup.msg.confirmation.DeleteDraftInfo1', { 0: `<b>${this.note && this.note.title}</b>` }) :
        this.$t('popup.msg.confirmation.DeleteInfo1', { 0: `<b>${this.note && this.note.title}</b>` })}`
          + `<p>${this.$t('popup.msg.confirmation.DeleteInfo2')}</p>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo4')}</li>`
          + `<li>${this.note.draftPage ? this.$t('popup.msg.confirmation.DeleteDraftInfo5', {
            0: `<b>${parentsBreadcrumb}</b>`
          }) : this.$t('popup.msg.confirmation.DeleteInfo5', {
            0: `<b>${parentsBreadcrumb}</b>`
          })}</li>`;
      this.$refs.DeleteNoteDialog.open();
    },
    createPDF(note) {
      this.hideElementsForSavingPDF = true;
      const title = `${this.noteTitle}`;
      if (note.title !== title) {
        this.noteTitle = note.title;
      }
      const self = this;
      this.$nextTick(() => {
        const element = this.$refs.content;
        this.hideElementsForSavingPDF = false;
        html2canvas(element, {
          useCORS: true
        }).then(function (canvas) {
          if (note.title !== title) {
            self.noteTitle = title;
          }
          const pdf = new JSPDF('p', 'mm', 'a4');
          const ctx = canvas.getContext('2d');
          const a4w = 170;
          const a4h = 257;
          const imgHeight = Math.floor(a4h * canvas.width / a4w);
          let renderedHeight = 0;

          while (renderedHeight < canvas.height) {
            const page = document.createElement('canvas');
            page.width = canvas.width;
            page.height = Math.min(imgHeight, canvas.height - renderedHeight);

            page.getContext('2d').putImageData(ctx.getImageData(0, renderedHeight, canvas.width, Math.min(imgHeight, canvas.height - renderedHeight)), 0, 0);
            pdf.addImage(page.toDataURL('image/jpeg', 1.0), 'JPEG', 10, 10, a4w, Math.min(a4h, a4w * page.height / page.width));
            renderedHeight += imgHeight;
            if (renderedHeight < canvas.height) {
              pdf.addPage();
            }
          }
          const filename = `${note.title}.pdf`;
          pdf.save(filename);
        }).catch(e => {
          this.displayMessage({
            type: 'error',
            message: this.$t('notes.message.export.error')
          });
          console.error('Error when exporting note: ', e);
        });
      });
    },
    displayMessage(message) {
      this.$root.$emit('alert-message', message?.message, message?.type || 'success');
    },
    getNoteVersionByNoteId(noteId) {
      this.noteVersionsArray = [];
      this.noteVersions = [];
      return this.$notesService.getNoteVersionsByNoteId(noteId,this.selectedTranslation.value).then(data => {
        this.noteVersions = data && data.reverse() || [];
        if (this.noteVersions.length>0){
          this.displayVersion(this.noteVersions[0]);
          this.$root.$emit('version-restored', this.noteVersions[0]);
        }
      });
    },
    displayVersion(version) {
      this.actualVersion = version;
      this.actualVersion.current = true;
      this.note.content = version.content;
    },
    restoreVersion(version) {
      const note = {
        id: this.note.id,
        title: this.note.title,
        content: version.content,
        updatedDate: version.updatedDate,
        owner: version.author
      };
      this.note.content = version.content;
      this.$notesService.restoreNoteVersion(note,version.versionNumber)
        .catch(e => {
          console.error('Error when restore note version', e);
          this.$root.$emit('version-restore-error');
        })
        .finally(() => {
          this.getNoteVersionByNoteId(this.note.id);
        });
    },
    openNoteVersionsHistoryDrawer() {
      if (!this.isDraft) {
        if ( this.note.canManage ) {
          this.versionsPageSize = Math.round((window.innerHeight-79)/80);
        } else {
          this.versionsPageSize = Math.round((window.innerHeight-79)/60);
        }
        this.$refs.noteVersionsHistoryDrawer.open();
      }
    },
    retrieveNoteTreeById() {
      this.note.wikiOwner = this.note.wikiOwner.substring(1);
      if (!this.note.draftPage) {
        this.$notesService.getFullNoteTree(this.note.wikiType, this.note.wikiOwner , this.note.name, false).then(data => {
          if (data && data.jsonList.length) {
            const allnotesTreeview = data.jsonList;
            this.noteChildren = allnotesTreeview.filter(note => note.name === this.note.title);
          }
        });
      }
    },
    openNoteChild(item) {
      const noteName = item.path.split('%2F').pop();
      this.$root.$emit('open-note-by-name', noteName);
    },
    updateNote(noteParam) {
      const note = {
        id: noteParam.id,
        title: noteParam.title,
        name: noteParam.name,
        wikiType: noteParam.wikiType,
        wikiOwner: noteParam.wikiOwner,
        content: '',
        parentPageId: null,
      };
      if (note.id) {
        this.$notesService.updateNoteById(note).catch(e => {
          console.error('Error when update note page', e);
        });
      }
    },
    updateURL(){
      const charsToRemove = notesConstants.PORTAL_BASE_URL.length-notesConstants.PORTAL_BASE_URL.lastIndexOf(`/${this.appName}`);
      let translation = '';
      if (this.selectedTranslation.value) {
        translation = `?translation=${this.selectedTranslation.value}`;
      }
      notesConstants.PORTAL_BASE_URL = `${notesConstants.PORTAL_BASE_URL.slice(0,-charsToRemove)}/${this.appName}/${this.note.id}${translation}`;
      if (!this.popStateChange) {
        window.history.replaceState(window.history.state, window.document.title, notesConstants.PORTAL_BASE_URL);
      }
      this.popStateChange = false;
    },
    getNoteLanguages(noteId){
      this.translations = [];
      return this.$notesService.getNoteLanguages(noteId).then(data => {
        this.translations =  data || [];
        if (this.translations.length>0) {
          this.translations = this.languages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.translations.sort((a, b) => a.text.localeCompare(b.text));
        }
        this.translations.unshift(this.originalVersion);
      });
    },
    getAvailableLanguages(){
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
      });
    },
    changeTranslation(translation){
      this.selectedTranslation = translation;
      return this.$notesService.getNoteById(this.note.id,this.selectedTranslation.value).then(data => {
        const note = data || {};
        if (note) {
          this.note.content = note.content;
          this.note.metadatas = note.metadatas;
          this.note.lang = note.lang;
          this.noteContent = note.content;
          this.note.title = note.title;
          this.noteTitle = !this.note.parentPageId && this.note.title==='Home' ? `${this.$t('notes.label.noteHome')}` : this.note.title;
        }
        this.updateURL();
        this.getNoteVersionByNoteId(this.note.id);
        this.viewNoteStatistics(this.note);
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
      });
    },
    viewNoteStatistics(note) {
      document.dispatchEvent(new CustomEvent('exo-statistic-message', {
        detail: {
          module: 'contents',
          subModule: 'contents',
          userId: eXo.env.portal.userIdentityId,
          userName: eXo.env.portal.userName,
          operation: 'viewContent',
          parameters: {
            contentId: note.id,
            contentTitle: note.title,
            contentType: 'Note',
            contentLanguage: note.lang?note.lang:this.$t('notes.label.translation.originalVersion'),
            spaceId: eXo.env.portal.spaceId,
          },
          timestamp: Date.now()
        }
      }));
    },

  }
};
</script>
