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
          <!--<div class="notes-treeview d-flex pb-2">
            <i class="uiIcon uiTreeviewIcon primary--text me-3"></i>
          <div
            v-for="(node, index) in notesTreeview" 
            :key="index" 
            class="notes-tree-item">
            <span class="caption">{{ node.name }}</span>
            <v-icon v-if="index+1 < notesTreeview.length" size="18">mdi-chevron-right</v-icon>
          </div>
          </div>-->
          <div class="notes-last-update-info">
            <span class="caption text-sub-title font-italic">{{ $t('notes.label.LastModifiedBy', {0: lastNotesUpdatebBy, 1: displayedDate}) }}</span>
          </div>
        </div>
        <v-divider class="my-4" />
        <div class="notes-application-content text-color" v-html="notes.content">
        </div>
      </div>
    </div>
  </v-app>
</template>
<script>
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
      noteTree: [],
      noteTreeElements: []
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
    noteTreeItem() {
      console.warn(this.noteTreeElement);
      return this.noteTreeElement;
    }
  },
  mounted() {
    const urlPath = document.location.pathname;
    if (urlPath.includes('/wiki/')){
      const noteId = urlPath.split('/wiki/')[1];
      this.notesPageName=noteId.split('/')[0];
    }
    this.getNotes();
    this.getNoteTree();
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
    getNotes() {
      return this.$notesService.getNotes(this.noteBookType, this.noteBookOwner , this.notesPageName).then(data => {
        this.notes = data || [];
      });
    },
    getNoteTree() {
      return this.$notesService.getNoteTree(this.noteBookType, this.noteBookOwnerTree , this.notesPageName).then(data => {
        this.noteTree = data && data.jsonList[0] || [];
      });
    },
  }
};
</script>