<template>
  <v-app 
    class="transparent"
    role="main"
    flat>
    <div>
      <div
        v-if="isAvailableNote"
        class="notes-application white border-radius pa-6"
        ref="content">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between pb-4">
            <span
              ref="noteTitle"
              class="title text-color mt-n1">
              {{ noteTitle }}
            </span>
            <div
              id="note-actions-menu"
              v-show="loadData && !hideElementsForSavingPDF"
              class="notes-header-icons text-right">
              <v-tooltip bottom v-if="!isMobile && !note.draftPage && note.canManage">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    class="pa-0"
                    v-on="on"
                    v-bind="attrs"
                    @click="addNote"
                    icon>
                    <v-icon
                      size="22"
                      class="clickable add-note-click">
                      mdi-plus
                    </v-icon>
                  </v-btn>
                </template>
                <span class="caption">{{ $t('notes.label.addPage') }}</span>
              </v-tooltip>
              <v-tooltip bottom v-if="note.canManage && !isMobile">
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    class="pa-0"
                    icon
                    v-on="on"
                    v-bind="attrs"
                    @click="editNote">
                    <v-icon
                      size="19"
                      class="clickable edit-note-click">
                      mdi-square-edit-outline
                    </v-icon>
                  </v-btn>
                </template>
                <span class="caption">{{ $t('notes.label.editPage') }}</span>
              </v-tooltip>

              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-btn
                    class="pa-0"
                    v-on="on"
                    @click="$root.$emit('display-action-menu')"
                    v-bind="attrs"
                    icon>
                    <v-icon
                      size="19"
                      class="clickable">
                      mdi-dots-vertical
                    </v-icon>
                  </v-btn>
                </template>
                <span class="caption">{{ $t('notes.label.openMenu') }}</span>
              </v-tooltip>
            </div>
          </div>
          <div v-if="!hideElementsForSavingPDF" class="notes-treeview d-flex flex-inline">
            <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
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
              @open-note="getNoteByName($event, 'breadCrumb')" />
          </div>
          <div v-show="!hideElementsForSavingPDF" class="notes-last-update-info">
            <span class="note-version border-radius primary px-2 font-weight-bold me-2 caption clickable" @click="openNoteVersionsHistoryDrawer(noteVersions, note.canManage)">V{{ lastNoteVersion }}</span>
            <span class="caption text-sub-title font-italic">{{ $t('notes.label.LastModifiedBy', {0: lastNoteUpdatedBy, 1: displayedDate}) }}</span>
          </div>
        </div>
        <v-divider class="my-4" />
        <div class="note-content" v-if="note.content && !isHomeNoteDefaultContent">
          <div v-if="showManualChild" id="showManualChild"> 
            <v-treeview
              dense
              :items="noteAllChildren"
              item-key="noteId">
              <template v-slot:label="{ item }">
                <v-list-item-title @click="openNoteChild(item)" class="body-2 clickable primary--text">
                  <span>{{ item.name }}</span>
                </v-list-item-title>
              </template>
            </v-treeview>
          </div>
          <div
            class="notes-application-content text-color"
            v-html="isDraft ? note.content : noteVersionContent">
          </div>
        </div>
        <div v-else-if="noteChildren && noteChildren[0] && !noteChildren[0].hasChild">
          <div v-if="note.canManage" class="notes-application-content d-flex flex-column justify-center text-center">
            <v-img
              :src="emptyNoteNoManager"
              class="mx-auto mb-4"
              max-height="150"
              max-width="250"
              contain
              eager />
            <div>
              <p class="notes-welcome-patragraph">
                <span>{{ $t('notes.label.no-content-no-redactor.content.first') }}</span>
                <v-tooltip bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <v-btn
                      class="pa-0"
                      icon
                      v-on="on"
                      v-bind="attrs"
                      @click="editNote">
                      <v-icon
                        size="19"
                        class="clickable edit-note-click">
                        mdi-square-edit-outline
                      </v-icon>
                    </v-btn>
                  </template>
                  <span class="caption">{{ $t('notes.label.editPage') }}</span>
                </v-tooltip>
                <span>{{ $t('notes.label.no-content.no-redactor.content.last') }}</span>
                <v-tooltip bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <v-btn
                      class="pa-0"
                      v-on="on"
                      v-bind="attrs"
                      @click="addNote"
                      icon>
                      <v-icon
                        size="22"
                        class="clickable add-note-click">
                        mdi-plus
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
            item-key="noteId">
            <template v-slot:label="{ item }">
              <v-list-item-title @click="openNoteChild(item)" class="body-2 clickable primary--text">
                <span>{{ item.name }}</span>
              </v-list-item-title>
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
      @open-history="$refs.noteVersionsHistoryDrawer.open(noteVersions,note.canManage)"
      @open-treeview-export="$refs.notesBreadcrumb.open(note, 'exportNotes')"
      @open-import-drawer="$refs.noteImportDrawer.open()" />
    <note-treeview-drawer
      ref="notesBreadcrumb" />
    <note-history-drawer
      ref="noteVersionsHistoryDrawer"
      @open-version="displayVersion($event)"
      @restore-version="restoreVersion($event)" />
    <note-import-drawer
      ref="noteImportDrawer" />
    <exo-confirm-dialog
      ref="DeleteNoteDialog"
      :message="confirmMessage"
      :title="note.draftPage ? $t('popup.confirmation.delete.draft') : $t('popup.confirmation.delete')"
      :ok-label="$t('notes.button.ok')"
      :cancel-label="$t('notes.button.cancel')"
      persistent
      @ok="deleteNote()"
      @dialog-opened="$emit('confirmDialogOpened')"
      @dialog-closed="$emit('confirmDialogClosed')" />
    <v-alert
      v-model="alert"
      :class="alertMessageClass"
      :type="type"
      :icon="type === 'warning' ? 'mdi-alert-circle' : ''"
      @input="onclose"
      dismissible>
      {{ message }}
    </v-alert>
  </v-app>
</template>
<script>

import { notesConstants } from '../../../javascript/eXo/wiki/notesConstants.js';
import html2canvas from 'html2canvas';
import JSPDF from 'jspdf';

export default {
  data() {
    return {
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
      alert: false,
      type: '',
      message: '',
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
      hasManualChildren: false,
      childNodes: []
    };
  },
  watch: {
    note() {
      if (!this.note.draftPage) {
        this.getNoteVersionByNoteId(this.note.id);
      }
      setTimeout(() => this.hasManualChildren = false, 100);
      if ( this.note && this.note.breadcrumb && this.note.breadcrumb.length ) {
        this.note.breadcrumb[0].title = this.$t('notes.label.noteHome');
        this.currentNoteBreadcrumb = this.note.breadcrumb;
      }
      this.noteTitle = !this.note.parentPageId ? `${this.$t('note.label.home')} ${this.spaceDisplayName}` : this.note.title;
      this.noteContent = this.note.content;
      this.retrieveNoteTreeById();
    },
    hasManualChildren () {
      if (this.hasManualChildren) {
        window.setTimeout(() => {
          const oldContainer = document.getElementById('showManualChild');
          const newContainers = document.getElementById('note-children-container');
          if (oldContainer && !newContainers.childNodes.length) {
            newContainers.append(...oldContainer.childNodes);
          }
        }, 100);
      }
    },
    actualVersion() {
      if (!this.isDraft) {
        this.noteContent = this.actualVersion.content;
        this.displayLastVersion = false;
      }
    },
  },
  computed: {
    showManualChild() {
      return this.hasManualChildren;
    },
    noteVersionContent() {
      return this.note.content && this.noteContent && this.formatContent(this.noteContent);
    },
    isHomeNoteDefaultContent() {
      return !this.note.parentPageId && this.noteContent.includes(`Welcome to Space ${this.spaceDisplayName} Notes Home`);
    },
    lastNoteVersion() {
      if ( this.displayLastVersion ) {
        return this.noteVersions && this.noteVersions[0] && this.noteVersions[0].versionNumber;
      } else {
        return this.actualVersion.versionNumber;
      }
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
      if (this.isDraft) {
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
    alertWarningDisplayed(){
      return (localStorage.getItem(`displayAlertSpaceId-${this.spaceId}`) === 'already_display');
    },
    alertMessageClass(){
      return  this.message.replace(/<[^>]*>/g, '').replace(/&nbsp;/g, '').trim().length > 45 ? 'lengthyAlertMessage' : '';
    }
  },
  created() {
    if (this.currentPath.endsWith('draft')) {
      this.isDraft = true;
    }
    this.$root.$on('open-note-by-name', (noteName, isDraft) => {
      if (!isDraft) {
        this.noteId = noteName;
        this.getNoteByName(noteName,'tree');
      } else {
        this.getDraftNote(noteName);
      }
    });
    this.$root.$on('confirmDeleteNote', () => {
      this.confirmDeleteNote();
    });
    this.$root.$on('show-alert', message => {
      this.displayMessage(message);
    });
    this.$root.$on('delete-note', () => {
      this.confirmDeleteNote();
    });
    this.$root.$on('move-page', (note, newParentNote) => {
      this.moveNote(note, newParentNote);
    });
    this.$root.$on('export-notes', (notesSelected,importAll,homeNoteId) => {
      this.exportNotes(notesSelected,importAll,homeNoteId);
    });
    this.$root.$on('import-notes', (uploadId,overrideMode) => {
      this.importNotes(uploadId,overrideMode);
    });

    
  },
  mounted() {
    if (this.noteId) {
      if (this.isDraft) {
        this.getDraftNote(this.noteId);
      } else {
        this.getNoteById(this.noteId);
      }
    } else {
      this.getNoteByName(this.notesPageName);
    }
  },
  methods: {
    addNote() {
      if (!this.isDraft) {
        window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?spaceId=${eXo.env.portal.spaceId}&parentNoteId=${this.note.id}&appName=${this.appName}`, '_blank');
      }
    },
    editNote() {
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?noteId=${this.note.id}&parentNoteId=${this.note.parentPageId ? this.note.parentPageId : this.note.id}&appName=${this.appName}&isDraft=${this.isDraft}`, '_blank');
    },
    deleteNote() {
      if (this.note.draftPage) {
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
    exportNotes(notesSelected,importAll,homeNoteId){
      let exportChildren =false;
      if (importAll === true) {
        exportChildren = true;
        notesSelected = homeNoteId;
      }
      const date=this.$dateUtil.formatDateObjectToDisplay(Date.now(), this.dateTimeFormatZip, this.lang);
      this.$notesService.exportNotes(notesSelected,exportChildren).then((transfer) => {
        return transfer.blob();
      }).then((bytes) => {
        const elm = document.createElement('a');  
        elm.href = URL.createObjectURL(bytes);
        elm.setAttribute('download', `${date}_notes_${this.spaceDisplayName}.zip`);
        elm.click();                             
        this.$root.$emit('close-note-tree-drawer');
        this.$root.$emit('show-alert', {type: 'success',message: this.$t('notes.alert.success.label.exported')});
      }).catch(e=> {
        console.error('Error when export note page', e);
        this.$root.$emit('show-alert', {
          type: 'error',
          message: this.$t(`notes.message.${e.message}`)
        });          
      });
    },
    getNoteById(noteId, source) {
      return this.$notesService.getNoteById(noteId, source, this.noteBookType, this.noteBookOwner).then(data => {
        this.note = {};
        this.note = data || {};
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        notesConstants.PORTAL_BASE_URL = `${notesConstants.PORTAL_BASE_URL.split(this.appName)[0]}${this.appName}/${this.note.id}`;
        window.history.pushState('notes', '', notesConstants.PORTAL_BASE_URL);
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
        this.existingNote = false;
      }).finally(() => {
        if (!this.note.canManage && !this.alertWarningDisplayed){
          const messageObject = {
            type: 'warning',
            message: `${this.$t('notes.alert.warning.label.notification')}`
          };
          this.displayMessage(messageObject, true);
        }
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
    getNoteByName(noteName, source) {
      return this.$notesService.getNote(this.noteBookType, this.noteBookOwner, noteName, source).then(data => {
        this.note = data || {};
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        notesConstants.PORTAL_BASE_URL = `${notesConstants.PORTAL_BASE_URL.split(this.appName)[0]}${this.appName}/${this.note.id}`;
        window.history.pushState('notes', '', notesConstants.PORTAL_BASE_URL);
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting note', e);
        this.existingNote = false;
      }).finally(() => {
        if (!this.note.canManage && !this.alertWarningDisplayed){
          const messageObject = {
            type: 'warning',
            message: `${this.$t('notes.alert.warning.label.notification')}`
          };
          this.displayMessage(messageObject, true);
        }
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeView-items', this.note);
      });
    },
    getDraftNote(noteId) {
      return this.$notesService.getDraftNoteById(noteId).then(data => {
        this.note = {};
        this.note = data || {};
        this.isDraft = true;
        this.loadData = true;
        this.currentNoteBreadcrumb = this.note.breadcrumb;
        notesConstants.PORTAL_BASE_URL = `${notesConstants.PORTAL_BASE_URL.split(this.appName)[0]}${this.appName}/${this.note.id}/draft`;
        window.history.pushState('notes', '', notesConstants.PORTAL_BASE_URL);
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
          const messageObject = {
            type: 'error',
            message: this.$t('notes.message.export.error')
          };
          this.displayMessage(messageObject);
          console.error('Error when exporting note: ', e);
        });
      });
    },
    displayMessage(message, keepAlert) {
      this.message=message.message;
      this.type=message.type;
      this.alert = true;
      if (!keepAlert) {
        window.setTimeout(() => this.alert = false, 5000);
      }
    },
    onclose() {
      localStorage.setItem(`displayAlertSpaceId-${this.spaceId}`, 'already_display');
    },
    getNoteVersionByNoteId(noteId) {
      return this.$notesService.getNoteVersionsByNoteId(noteId).then(data => {
        this.noteVersions = data && data.reverse() || [];
        this.displayVersion(this.noteVersions[0]);
        this.$root.$emit('refresh-versions-history', this.noteVersions );
      });
    },
    displayVersion(version) {
      this.actualVersion = version;
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
        })
        .finally(() => {
          this.getNoteVersionByNoteId(this.note.id);
        });
    },
    formatContent (content) {
      const internal = location.host + eXo.env.portal.context;
      const domParser = new DOMParser();
      const docElement = domParser.parseFromString(content, 'text/html').documentElement;
      const contentChildren = docElement.getElementsByTagName('body')[0].children;
      const links = docElement.getElementsByTagName('a');
      const tables = docElement.getElementsByTagName('table');
      for (const link of links) {
        let href = link.href.replace(/(^\w+:|^)\/\//, '');
        if (href.endsWith('/')) {
          href = href.slice(0, -1);
        }
        if (href !== location.host && !href.startsWith(internal)) {
          link.setAttribute('target', '_blank');
          link.setAttribute('rel', 'noopener noreferrer');
        }
      }
      for (const table of tables) {
        if (!table.hasAttribute('role')) {
          table.setAttribute('role', 'presentation');
        }
        if (!table.hasAttribute('summary')) {
          const customId = table.parentElement.id.split('-').pop();
          const tableSummary = document.getElementById(`summary-${customId}`);
          if ( tableSummary !== null ) {
            table.setAttribute('summary', tableSummary.innerText);
          }
        }
      }
      contentChildren.forEach( (child) =>  {
        if (child.classList.value.includes('navigation-img-wrapper')) {
          child.innerHTML = '';
          window.setTimeout(() => {this.hasManualChildren = true;},100);
        }
      });
      return docElement.innerHTML;
    },
    openNoteVersionsHistoryDrawer() {
      if (!this.isDraft) {
        this.$refs.noteVersionsHistoryDrawer.open(this.noteVersions, this.note.canManage);
      }
    },
    retrieveNoteTreeById() {
      this.note.wikiOwner = this.note.wikiOwner.substring(1);
      this.$notesService.getFullNoteTree(this.note.wikiType, this.note.wikiOwner , this.note.name).then(data => {
        if (data && data.jsonList.length) {
          const allnotesTreeview = data.jsonList;
          this.noteChildren = allnotesTreeview.filter(note => note.name === this.note.title);
        }
      });
    },
    openNoteChild(item) {
      const noteName = item.path.split('%2F').pop();
      this.$root.$emit('open-note-by-name', noteName);
    },
  }
};
</script>
