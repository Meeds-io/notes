<template>
  <v-app class="transparent" flat>
    <div>
      <div class="notes-application white border-radius ma-3 py-3 px-6">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between">
            <span class=" title text-color">{{ notes.title }}</span>
            <div class="notes-header-icons">
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

              <v-tooltip bottom>
                <template v-slot:activator="{ on, attrs }">
                  <v-icon
                    size="19"
                    class="clickable"
                    v-bind="attrs"
                    v-on="on">
                    mdi-dots-vertical
                  </v-icon>
                </template>
                <span class="caption">{{ $t('notes.label.openMenu') }}</span>
              </v-tooltip>
            </div>
          </div>
          <div class="notes-treeview d-flex pb-2">
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
            <div
              v-for="(note, index) in notes.breadcrumb" 
              :key="index" 
              class="notes-tree-item">
              <a
                v-if="index+1 < notes.breadcrumb.length"
                @click="getNoteById(note.id)"
                class="caption text-color" 
                :class="index+1 === notes.breadcrumb.length && 'primary--text font-weight-bold' || ''">{{ note.title }}</a>
              <span v-else class="caption primary--text font-weight-bold">{{ note.title }}</span>
              <v-icon v-if="index+1 < notes.breadcrumb.length" size="18">mdi-chevron-right</v-icon>  
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
    </div>
    <note-breadcrumb-drawer 
      ref="notesBreadcrumb" />
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
      noteBookType: eXo.env.portal.spaceName ? 'group' : 'portal',
      noteBookOwner: eXo.env.portal.spaceName ? `/spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      noteBookOwnerTree: eXo.env.portal.spaceName ? `spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
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
    notesPageName() {
      if (!(notesConstants.PORTAL_BASE_URL.includes('/wiki/'))) {
        return;
      } else {
        const noteId = notesConstants.PORTAL_BASE_URL.split('/wiki/')[1];
        if (noteId) {
          return noteId.split('/')[0];
        } else {
          return 'WikiHome';
        }
      }
    }
  },
  created() {
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
    retrieveUserInformations(userName) {
      this.$userService.getUser(userName).then(user => {
        this.lastUpdatedUser =  user.fullname;
      });
    },
    getNotes(noteBookType,noteBookOwner,notesPageName) {
      return this.$notesService.getNotes(noteBookType, noteBookOwner , notesPageName).then(data => {
        this.notes = data || [];
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
    }
  }
};
</script>