<template>
  <v-app class="transparent" flat>
    <div>
      <div class="notes-application white border-radius ma-3 py-3 px-6">
        <div class="notes-application-header">
          <div class="notes-title d-flex justify-space-between">
            <span class=" title text-color">{{ notes.title }}</span>
            <div class="notes-header-icons">
              <v-icon
                size="22"
                class="clickable"
                :title="$t('notes.label.addPage')">
                mdi-plus
              </v-icon>
              <v-icon
                size="19"
                class="clickable px-1"
                :title="$t('notes.label.editPage')">
                mdi-square-edit-outline
              </v-icon>
              <v-icon
                size="19"
                class="clickable"
                :title="$t('notes.label.openMenu')">
                mdi-dots-vertical
              </v-icon>
            </div>
          </div>
          <div class="notes-treeview d-flex pb-2">
            <i class="uiIcon uiTreeviewIcon primary--text me-3" @click="getNoteTree()"></i>
            <div
              v-for="(note, index) in notes.breadcrumb" 
              :key="index" 
              class="notes-tree-item">
              <a
                v-if="index+1 < notes.breadcrumb.length"
                @click="getNoteById(note)"
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
        <div class="notes-application-content text-color" v-html="notes.content">
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
      notesPageName: 'WikiHome',
      noteBookType: eXo.env.portal.spaceName ? 'group' : 'portal',
      noteBookOwner: eXo.env.portal.spaceName ? `/spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      noteBookOwnerTree: eXo.env.portal.spaceName ? `spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      //urlPath: document.location.pathname,
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
  },
  created() {
    this.$root.$on('open-note', notePath => {
      const noteName = notePath.split('%2F').pop();
      this.getNotes(this.noteBookType, this.noteBookOwner , noteName);
      const value = notesConstants.PORTAL_BASE_URL.substring(notesConstants.PORTAL_BASE_URL.lastIndexOf('/') + 1);
      notesConstants.PORTAL_BASE_URL = notesConstants.PORTAL_BASE_URL.replace(value, noteName);
      window.location.pathname = notesConstants.PORTAL_BASE_URL;
    });
  },
  mounted() {
    if (notesConstants.PORTAL_BASE_URL.includes('/WikiPortlet/')){
      const noteId = notesConstants.PORTAL_BASE_URL.split('/WikiPortlet/')[1];
      this.notesPageName=noteId.split('/')[0];
    }
    this.getNotes(this.noteBookType, this.noteBookOwner , this.notesPageName);
  },
  methods: {
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
      return this.$notesService.getNoteTree(this.noteBookType, this.noteBookOwnerTree , 'wikiHome').then(data => {
        this.noteTree = data && data.jsonList[0] || [];
        this.$refs.notesBreadcrumb.open(this.noteTree);
      });
    },
    getNoteById(note) {
      this.getNotes(note.wikiType, note.wikiOwner, note.id);
      const value = notesConstants.PORTAL_BASE_URL.substring(notesConstants.PORTAL_BASE_URL.lastIndexOf('/') + 1);
      notesConstants.PORTAL_BASE_URL = notesConstants.PORTAL_BASE_URL.replace(value, note.id);
      window.history.pushState('WikiPortlet', '', notesConstants.PORTAL_BASE_URL); 
    },
  }
};
</script>