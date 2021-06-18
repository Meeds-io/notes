<template>
  <v-app class="transparent" flat>
    <div>
      <div v-if="isAvailableNote" class="notes-application white border-radius pa-6">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between">
            <span class="title text-color mt-n1">{{ notes.title }}</span>
            <div class="notes-header-icons text-right">
              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="22"
                    class="clickable"
                    @click="addNotes"
                    v-bind="attrs"
                    v-on="on">
                    mdi-plus
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.addPage') }}</span>
              </v-tooltip>

              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="19"
                    class="clickable"
                    @click="editNotes"
                    v-bind="attrs"
                    v-on="on">
                    mdi-square-edit-outline
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.editPage') }}</span>
              </v-tooltip>

              <v-menu
                v-model="displayActionMenu"
                transition="slide-x-reverse-transition"
                offset-y
                left>
                <template v-slot:activator="{ on: menu, attrs }">
                  <v-tooltip bottom>
                    <template v-slot:activator="{ on: tooltip }">
                      <v-icon
                        size="19"
                        class="clickable"
                        v-bind="attrs"
                        v-on="{ ...tooltip, ...menu }"
                        @click="displayActionMenu = true">
                        mdi-dots-vertical
                      </v-icon>
                    </template>
                    <span class="caption">{{ $t('notes.label.openMenu') }}</span>
                  </v-tooltip>
                </template>
                <v-list>
                  <v-list-item
                    class="draftButton"
                    :key="notes.id"
                    @click="confirmDeleteNote">
                    <v-list-item-title class="subtitle-2">
                      <i class="uiIcon uiIconTrash pr-1"></i>
                      <span>{{ $t('notes.delete') }}</span>
                    </v-list-item-title>
                  </v-list-item>
                </v-list>
              </v-menu>
            </div>
          </div>
          <div class="notes-treeview d-flex flex-wrap pb-2">
            <v-tooltip bottom>
              <template v-slot:activator="{ on, attrs }">
                <i 
                  class="uiIcon uiTreeviewIcon primary--text me-3"
                  v-bind="attrs"
                  v-on="on" 
                  @click="getNoteTree()"></i>
              </template>
              <span class="caption">{{ $t('notes.label.noteTreeview.tooltip') }}</span>
            </v-tooltip>
            <div v-if="notes.breadcrumb.length <= 4" class="notes-tree-items d-flex">
              <div
                v-for="(note, index) in notes.breadcrumb"
                :key="index"
                :class="notes.breadcrumb.length === 1 && 'single-path-element' || ''"
                class="notes-tree-item d-flex text-truncate"
                :style="`max-width: ${100 / (notes.breadcrumb.length)}%`">
                <v-tooltip max-width="300" bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <a 
                      v-bind="attrs"
                      v-on="on"
                      @click="getNoteById(note.id)"
                      class="caption text-truncate "
                      :class="index < notes.breadcrumb.length-1 && 'path-clickable text-color' || 'text-sub-title not-clickable'">{{ note.title }}</a>
                  </template>
                  <span class="caption">{{ note.title }}</span>
                </v-tooltip>
                <v-icon v-if="index < notes.breadcrumb.length-1" size="18">mdi-chevron-right</v-icon>
              </div>
            </div>
            <div v-else class="notes-tree-items notes-long-path d-flex align-center">
              <div class="notes-tree-item long-path-first-item d-flex text-truncate">
                <v-tooltip max-width="300" bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <a
                      class="caption text-color text-truncate path-clickable"
                      v-bind="attrs"
                      v-on="on"
                      @click="getNoteById(notes.breadcrumb[0].id)">{{ notes.breadcrumb[0].title }}</a>
                  </template>
                  <span class="caption">{{ notes.breadcrumb[0].title }}</span>
                </v-tooltip>
                <v-icon size="18">mdi-chevron-right</v-icon>
              </div>
              <div class="notes-tree-item long-path-second-item d-flex">
                <v-tooltip bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <v-icon
                      v-bind="attrs"
                      v-on="on"
                      size="24">
                      mdi-dots-horizontal
                    </v-icon>
                  </template>
                  <p
                    v-for="(note, index) in notes.breadcrumb"
                    :key="index"
                    class="mb-0">
                    <span v-if="index > 0 && index <notes.breadcrumb.length-2" class="caption"><v-icon size="18" class="tooltip-chevron">mdi-chevron-right</v-icon> {{ note.title }}</span>
                  </p>
                </v-tooltip>
                <v-icon class="clickable" size="18">mdi-chevron-right</v-icon>
              </div>
              <div class="notes-tree-item long-path-third-item d-flex text-truncate">
                <v-tooltip max-width="300" bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <a
                      class="caption text-color text-truncate path-clickable"
                      v-bind="attrs"
                      v-on="on"
                      @click="getNoteById(notes.breadcrumb[notes.breadcrumb.length-2].id)">{{ notes.breadcrumb[notes.breadcrumb.length-2].title }}</a>
                  </template>
                  <span class="caption">{{ notes.breadcrumb[notes.breadcrumb.length-2].title }}</span>
                </v-tooltip>
                <v-icon size="18">mdi-chevron-right</v-icon>
              </div>
              <div class="notes-tree-item d-flex text-truncate">
                <v-tooltip max-width="300" bottom>
                  <template v-slot:activator="{ on, attrs }">
                    <a
                      class="caption text-color text-truncate text-sub-title"
                      v-bind="attrs"
                      v-on="on"
                      @click="getNoteById(notes.breadcrumb[notes.breadcrumb.length-1].id)">{{ notes.breadcrumb[notes.breadcrumb.length-1].title }}</a>
                  </template>
                  <span class="caption">{{ notes.breadcrumb[notes.breadcrumb.length-1].title }}</span>
                </v-tooltip>
              </div>
            </div>
          </div>
          <div class="notes-last-update-info">
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
    <note-breadcrumb-drawer 
      ref="notesBreadcrumb" />
    <exo-confirm-dialog
      ref="DeleteNoteDialog"
      :message="confirmMessage"
      :title="$t('popup.confirmation.delete')"
      :ok-label="$t('popup.ok')"
      :cancel-label="$t('btn.cancel')"
      persistent
      @ok="deleteNotes()"
      @dialog-opened="$emit('confirmDialogOpened')"
      @dialog-closed="$emit('confirmDialogClosed')" />
  </v-app>
</template>
<script>
import { notesConstants } from '../../../javascript/eXo/wiki/notesConstants.js';
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
      breadcrumbNotes: '',
      noteBookType: eXo.env.portal.spaceName ? 'group' : 'portal',
      noteBookOwner: eXo.env.portal.spaceName ? `/spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      noteBookOwnerTree: eXo.env.portal.spaceName ? `spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      noteNotFountImage: '/wiki/skin/images/notes_not_found.png',
      defaultPath: 'WikiHome',
      existingNote: false,
      currentPath: window.location.pathname
    };
  },
  watch: {
    notes() {
      this.lastUpdatedUser = this.retrieveUserInformations(this.notes.author);
      this.lastUpdatedTime = this.notes.updatedDate.time && this.$dateUtil.formatDateObjectToDisplay(new Date(this.notes.updatedDate.time), this.dateTimeFormat, this.lang) || '';
    }
  },
  computed: {
    lastNotesUpdatebBy() {
      return this.lastUpdatedUser;
    },
    displayedDate() {
      return this.lastUpdatedTime;
    },

    isAvailableNote() {
      return this.existingNote;
    },
    notesPageName() {
      if (this.currentPath.endsWith('/wiki')){
        return 'WikiHome';
      } else {
        if (!(this.currentPath.includes('/wiki/'))) {
          return;
        } else {
          const noteId = this.currentPath.split('/').pop();
          if (noteId) {
            return noteId;
          } else {
            return 'WikiHome';
          }
        }
      }
    }
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    this.$root.$on('open-note', notePath => {
      const noteName = notePath.split('%2F').pop();
      this.getNotes(this.noteBookType, this.noteBookOwner , noteName);
      const value = notesConstants.PORTAL_BASE_URL.substring(notesConstants.PORTAL_BASE_URL.lastIndexOf('/') + 1);
      notesConstants.PORTAL_BASE_URL = notesConstants.PORTAL_BASE_URL.replace(value, noteName);
      window.location.pathname = notesConstants.PORTAL_BASE_URL;
    });
    this.$root.$on('open-note-by-id', noteId => {
      this.getNoteById(noteId);
    });
  },
  mounted() {
    this.getNotes(this.noteBookType, this.noteBookOwner , this.notesPageName);
  },
  methods: {
    addNotes(){
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?parentNoteId=${this.notes.id}`,'_blank');
    },
    editNotes(){
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?noteId=${this.notes.id}`,'_blank');
    },
    deleteNotes(){
      this.$notesService.deleteNotes(this.notes).then(() => {
        this.refreshNote();
      }).catch(e => {
        console.error('Error when deleting notes', e);
      });
    },
    refreshNote(){
      this.$notesService.getNoteById('1').then(data => {
        window.location.href=this.$notesService.getPathByNoteOwner(data);
      });
    },
    getParentsNotes(){
      this.breadcrumbNotes = '';
      for (let index = 0; index < this.notes.breadcrumb.length-1; index++) {
        this.parentPageName=this.notes.breadcrumb[index].id;
        this.breadcrumbNotes = this.breadcrumbNotes.concat(this.notes.breadcrumb[index].title,' > ');
      }
      return this.breadcrumbNotes;
    },
    retrieveUserInformations(userName) {
      this.$userService.getUser(userName).then(user => {
        this.lastUpdatedUser =  user.fullname;
      });
    },
    getNotes(noteBookType,noteBookOwner,notesPageName) {
      return this.$notesService.getNotes(noteBookType, noteBookOwner , notesPageName).then(data => {
        this.notes = data || [];
        this.existingNote = true;
      });
    },
    getNoteTree() {
      return this.$notesService.getNoteTree(this.noteBookType, this.noteBookOwnerTree , this.notesPageName,'ALL').then(data => {
        this.noteTree = data && data.jsonList || [];
        this.$refs.notesBreadcrumb.open(this.makeNoteChildren(this.noteTree), this.noteBookType, this.noteBookOwnerTree, this.getOpenedTreeviewItems(this.notes.breadcrumb));
      });
    },
    getNoteById(noteId) {
      this.getNotes(this.noteBookType,this.noteBookOwner, noteId);
      const value = notesConstants.PORTAL_BASE_URL.substring(notesConstants.PORTAL_BASE_URL.lastIndexOf('/') + 1);
      notesConstants.PORTAL_BASE_URL = notesConstants.PORTAL_BASE_URL.replace(value, noteId);
      window.history.pushState('wiki', '', notesConstants.PORTAL_BASE_URL);
    },
    makeNoteChildren(childrenArray) {
      const treeviewArray = [];
      childrenArray.forEach(child => {
        if ( child.hasChild ) {
          treeviewArray.push ({
            id: child.path.split('%2F').pop(),
            hasChild: child.hasChild,
            name: child.name,
            children: this.makeNoteChildren(child.children)
          });
        } else {
          treeviewArray.push({
            id: child.path.split('%2F').pop(),
            hasChild: child.hasChild,
            name: child.name
          });
        }
      });
      return treeviewArray;
    },
    getOpenedTreeviewItems(breadcrumArray) {
      const activatedNotes = [];
      for (let index = 1; index < breadcrumArray.length; index++) {
        activatedNotes.push(breadcrumArray[index].id);
      }
      return activatedNotes;
    },
    confirmDeleteNote: function () {
      this.getParentsNotes();
      this.confirmMessage = `${this.$t('popup.msg.confirmation.DeleteInfo1', {
        0: `<b>${this.notes && this.notes.title}</b>`,
      })
      }`
          + `<p>${this.$t('popup.msg.confirmation.DeleteInfo2')}</p>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo3')}</li>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo4')}</li>`
          + `<li>${this.$t('popup.msg.confirmation.DeleteInfo5', {
            1: `<b>${this.breadcrumbNotes}</b>`,
          })}</li>`;
      this.$refs.DeleteNoteDialog.open();
    },
  }
};
</script>