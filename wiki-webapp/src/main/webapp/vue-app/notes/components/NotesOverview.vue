<template>
  <v-app class="transparent" flat>
    <div>
      <div class="notes-application border-radius ma-3 py-3 px-6">
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
            <i class="uiIcon uiTreeviewIcon"></i>
          <!--<div
            v-for="(node, index) in notesTreeview" 
            :key="index" 
            class="notes-tree-item">
            <span class="caption">{{ node.name }}</span>
            <v-icon v-if="index+1 < notesTreeview.length" size="18">mdi-chevron-right</v-icon>
          </div>-->
          </div>
          <div class="notes-last-update-info">
            <span class="caption grey-light-color font-italic">{{ $t('notes.label.LastModifiedBy', {0: lastNotesUpdatebBy, 1: displayedDate}) }}</span>
          </div>
        </div>
        <v-divider class="my-4" />
        <div class="notes-application-content text-color" v-html="notes.content">
        </div>
      </div>
    <!--<notes-editor-dashboard/>-->
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
      notesPageName: 'Note_Zero',
      wikiType: eXo.env.portal.spaceName ? 'group' : 'portal',
      wikiOwner: eXo.env.portal.spaceName ? `/spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      wikiOwnerTree: eXo.env.portal.spaceName ? `spaces/${eXo.env.portal.spaceName}` : `${eXo.env.portal.portalName}`,
      noteTree: [],
      noteTreeElements: []
    };
  },
  watch: {
    notes() {
      this.lastUpdatedUser = this.retrieveUserInformations(this.notes.author);
      this.lastUpdatedTime = this.notes.updatedDate.time && this.$dateUtil.formatDateObjectToDisplay(new Date(this.notes.updatedDate.time), this.dateTimeFormat, this.lang) || '';
      this.noteTreeElement = this.buildNoteTree(this.noteTree, this.notesPageName);
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
    this.getNotes();
    this.getNoteTree();
  },
  methods: {
    retrieveUserInformations(userName) {
      this.$userService.getUser(userName).then(user => {
        this.lastUpdatedUser =  user.fullname;
      });
    },
    getNotes() {
      return this.$notesService.getNotes(this.wikiType, this.wikiOwner , this.notesPageName).then(data => {
        this.notes = data || [];
      });
    },
    getNoteTree() {
      return this.$notesService.getNoteTree(this.wikiType, this.wikiOwnerTree , this.notesPageName).then(data => {
        this.noteTree = data && data.jsonList[0] || [];
      });
    },
    buildNoteTree(arrayObject, stringTest) {
      let result = [];
      if (!arrayObject || typeof arrayObject !== 'object') {return;}
      arrayObject.children.forEach(item => {
        if (item.path.split('%2F').pop() === stringTest) {
          result.push({
            'name': item.name,
            'path': item.path
          });
          result.push({
            'name': arrayObject.name,
            'path': arrayObject.path
          });
          return result;
        }
        return result = this.buildNoteTree(item, stringTest);
      });
      return result;
    },
  }
};
</script>