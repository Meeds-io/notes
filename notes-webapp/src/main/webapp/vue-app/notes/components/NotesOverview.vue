<template>
  <v-app class="transparent" flat>
    <div>
      <div
        v-if="isAvailableNote"
        class="notes-application white border-radius pa-6"
        ref="content">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between">
            <span class="title text-color mt-n1">{{ noteTitle }}</span>
            <div
              id="note-actions-menu"
              v-show="loadData && !hideActions"
              class="notes-header-icons text-right">
              <v-tooltip bottom v-if="!isMobile">
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="22"
                    class="clickable add-note-click"
                    @click="addNotes"
                    v-bind="attrs"
                    v-on="on">
                    mdi-plus
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.addPage') }}</span>
              </v-tooltip>

              <v-tooltip bottom v-if="notes.canEdit && !isMobile">
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="19"
                    class="clickable edit-note-click"
                    @click="editNotes"
                    v-bind="attrs"
                    v-on="on">
                    mdi-square-edit-outline
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.editPage') }}</span>
              </v-tooltip>

              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="19"
                    class="clickable"
                    v-bind="attrs"
                    v-on="on"
                    @click="$root.$emit('display-action-menu')">
                    mdi-dots-vertical
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.openMenu') }}</span>
              </v-tooltip>
            </div>
          </div>
          <div class="notes-treeview d-flex flex-wrap pb-2">
            <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <i 
                  class="uiIcon uiTreeviewIcon primary--text me-3"
                  v-bind="attrs"
                  v-on="on" 
                  @click="$refs.notesBreadcrumb.open(notes.id, 'displayNote')"></i>
              </template>
              <span class="caption">{{ $t('notes.label.noteTreeview.tooltip') }}</span>
            </v-tooltip>
            <note-breadcrumb :note-breadcrumb="notebreadcrumb" @open-note="getNoteByName($event, 'breadCrumb')" />
          </div>
          <div class="notes-last-update-info">
            <span class="note-version border-radius primary px-2 font-weight-bold me-2 caption clickable" @click="$refs.noteVersionsHistoryDrawer.open()">V{{ lastNoteVersion }}</span>
            <span class="caption text-sub-title font-italic">{{ $t('notes.label.LastModifiedBy', {0: lastNotesUpdatebBy, 1: displayedDate}) }}</span>
          </div>
        </div>
        <v-divider class="my-4" />
        <div
          v-if="notes.content"
          class="notes-application-content text-color"
          v-html="notes.content">
        </div>
        <div v-else class="notes-application-content">
          <p class="body-2 font-italic">
            {{ $t('notes.label.no-content') }}
          </p>
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
      :note="notes" 
      :default-path="defaultPath" 
      @open-treeview="$refs.notesBreadcrumb.open(notes.id, 'movePage')" 
      @export-pdf="createPDF(notes)"
      @open-history="$refs.noteVersionsHistoryDrawer.open(notes.id)" />
    <note-treeview-drawer
      ref="notesBreadcrumb" />
    <note-history-drawer
      ref="noteVersionsHistoryDrawer"
      :note-versions="noteVersions" />
    <exo-confirm-dialog
      ref="DeleteNoteDialog"
      :message="confirmMessage"
      :title="$t('popup.confirmation.delete')"
      :ok-label="$t('notes.button.ok')"
      :cancel-label="$t('notes.button.cancel')"
      persistent
      @ok="deleteNotes()"
      @dialog-opened="$emit('confirmDialogOpened')"
      @dialog-closed="$emit('confirmDialogClosed')" />
    <v-alert
      v-model="alert"
      :type="type"
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
      notes: {},
      lastUpdatedUser: '',
      lastUpdatedTime: '',
      lang: eXo.env.portal.language,
      dateTimeFormat: {
        year: 'numeric',
        month: 'long',
        day: 'numeric',
        hour: '2-digit',
        minute: '2-digit',
      },
      displayActionMenu: false,
      parentPageName: '',
      confirmMessage: '',
      noteBookType: eXo.env.portal.spaceName ? 'group' : 'user',
      noteBookOwner: eXo.env.portal.spaceGroup ? `/spaces/${eXo.env.portal.spaceGroup}` : eXo.env.portal.profileOwner,
      noteBookOwnerTree: eXo.env.portal.spaceGroup ? `spaces/${eXo.env.portal.spaceGroup}` : eXo.env.portal.profileOwner,
      noteNotFountImage: '/notes/skin/images/notes_not_found.png',
      defaultPath: 'Home',
      existingNote: true,
      currentPath: window.location.pathname, 
      currentNoteBreadcrumb: [],
      alert: false,
      type: '',
      message: '',
      loadData: false,
      openTreeView: false,
      hideActions: false,
      noteVersions: [],
    };
  },
  watch: {
    notes() {
      this.lastUpdatedUser = this.retrieveUserInformations(this.notes.author);
      if ( this.notes && this.notes.breadcrumb && this.notes.breadcrumb.length ) {
        this.notes.breadcrumb[0].title = this.$t('notes.label.noteHome');
        this.currentNoteBreadcrumb = this.notes.breadcrumb;
      }
      this.lastUpdatedTime = this.notes.updatedDate.time && this.$dateUtil.formatDateObjectToDisplay(new Date(this.notes.updatedDate.time), this.dateTimeFormat, this.lang) || '';
    }
  },
  computed: {
    lastNoteVersion() {
      return this.noteVersions && this.noteVersions[0].versionNumber;
    },
    lastNotesUpdatebBy() {
      return this.lastUpdatedUser;
    },
    displayedDate() {
      return this.lastUpdatedTime;
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
    noteTitle() {
      if ( this.noteId === 1) {
        return this.$t('notes.label.noteHome');
      } else {
        return this.notes.title;
      }
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
      const nId = this.currentPath.split(`${eXo.env.portal.selectedNodeUri}/`)[1];
      if (!isNaN(nId)) {
        return nId;
      } else {
        return 0;
      }
        
    },
    appName() {
      const uris = eXo.env.portal.selectedNodeUri.split('/');
      return uris[uris.length - 1];
    }
  },
  created() {
    this.$root.$on('open-note-by-id', noteId => {
      this.noteId = noteId;
      this.getNoteByName(noteId,'tree');
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
      this.moveNotes(note, newParentNote);
    });
    
  },
  mounted() {
    if (this.noteId){
      this.getNotesById(this.noteId);
      this.getNoteVersionByNoteId(this.noteId);
    } else {
      this.getNoteByName(this.notesPageName);
    }
    this.currentNoteBreadcrumb = this.notes.breadcrumb;
  },
  methods: {
    addNotes(){
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?spaceId=${eXo.env.portal.spaceId}&parentNoteId=${this.notes.id}&appName=${this.appName}`,'_blank');
    },
    editNotes(){
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?noteId=${this.notes.id}&appName=${this.appName}`,'_blank');
    },
    deleteNotes(){
      this.$notesService.deleteNotes(this.notes).then(() => {
        this.getNoteByName(this.notebreadcrumb[ this.notebreadcrumb.length-2].id);
      }).catch(e => {
        console.error('Error when deleting notes', e);
      });
    },
    moveNotes(note, newParentNote){
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
    retrieveUserInformations(userName) {
      this.$userService.getUser(userName).then(user => {
        this.lastUpdatedUser =  user.fullname;
      });
    },
    getNotesById(noteId,source) {
      return this.$notesService.getNoteById(noteId,source,this.noteBookType, this.noteBookOwner).then(data => {
        const note = data || [];
        this.getNoteByName(note.name, source);
      }).catch(e => {
        console.error('Error when getting notes', e);
        this.existingNote = false;
      }).finally(() => {
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeview-items',this.notes.id);
      });
    },
    getNoteByName(noteName,source) {
      return this.$notesService.getNotes(this.noteBookType, this.noteBookOwner, noteName,source).then(data => {
        this.notes = data || [];
        this.loadData = true;
        notesConstants.PORTAL_BASE_URL = `${notesConstants.PORTAL_BASE_URL.split(this.appName)[0]}${this.appName}/${this.notes.id}`;
        window.history.pushState('notes', '', notesConstants.PORTAL_BASE_URL);
        return this.$nextTick();
      }).catch(e => {
        console.error('Error when getting notes', e);
        this.existingNote = false;
      }).finally(() => {
        this.$root.$applicationLoaded();
        this.$root.$emit('refresh-treeview-items',this.notes.id);
      });
    },
    confirmDeleteNote: function () {
      let parentsBreadcrumb = '';
      for (let index = 0; index < this.notebreadcrumb.length-1; index++) {
        parentsBreadcrumb = parentsBreadcrumb.concat(this.notebreadcrumb[index].title);
        if (index < this.notebreadcrumb.length-2) {
          parentsBreadcrumb = parentsBreadcrumb.concat('>');
        }
      }
      this.confirmMessage = `${this.$t('popup.msg.confirmation.DeleteInfo1', {
        0: `<b>${this.notes && this.notes.title}</b>`,
      })
      }`
          + `<p>${this.$t('popup.msg.confirmation.DeleteInfo2')}</p>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo4')}</li>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo5', {
            1: `<b>${parentsBreadcrumb}</b>`,
          })}</li>`;
      this.$refs.DeleteNoteDialog.open();
    },
    createPDF(note) {
      this.hideActions = true;
      setTimeout(() => {
        const element = this.$refs.content;
        html2canvas(element, {
          useCORS: true
        }).then(function (canvas) {
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
        this.hideActions = false;
      }, 100);
    },
    displayMessage(message) {
      this.message=message.message;
      this.type=message.type;
      this.alert = true;
      window.setTimeout(() => this.alert = false, 5000);
    },
    getNoteVersionByNoteId(noteId) {
      return this.$notesService.getNoteVersionsByNoteId(noteId).then(data => {
        this.noteVersions = data && data.reverse() || [];
      });
    }
  }
};
</script>
