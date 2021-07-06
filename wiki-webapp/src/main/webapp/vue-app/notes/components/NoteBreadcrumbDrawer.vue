<template>
  <exo-drawer
    ref="breadcrumbDrawer"
    class="breadcrumbDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template slot="title">
      {{ $t('notes.label.breadcrumbTitle') }}
    </template>
    <template slot="content">
      <v-layout column>
        <template v-if="wikiHome" class="ma-0 border-box-sizing">
          <v-list-item  @click="openNote(event,wikiHome)">
            <v-list-item-content>
              <v-list-item-title>{{ wikiHome.name }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </template>
        <template v-if="items && items.length">
          <v-treeview
            :items="items[0].children"
            :open="openedItems"
            :active="active"
            :load-children="fetchNoteChildren"
            item-key="id"
            hoverable
            open-on-click
            transition>
            <template v-slot:label="{ item }">
              <v-list-item-title @click="openNote(event,item)">{{ item.name }}</v-list-item-title>
            </template>
          </v-treeview>
        </template>
      </v-layout>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data: () => ({
    note: {},
    breadcrumbItems: [],
    breadcrumbItemChild: [],
    noteBookType: '',
    noteBookOwnerTree: '',
    openNotes: [],
    activeItem: [],
    isIncludePage: false
  }),
  computed: {
    items() {
      return this.breadcrumbItems;
    },
    wikiHome() {
      return this.breadcrumbItems && this.breadcrumbItems.length && this.breadcrumbItems[0];
    },
    openedItems() {
      return this.openNotes;
    },
    active() {
      return this.activeItem;
    },
    includePage () {
      return this.isIncludePage;
    }
  },
  created() {
    this.$root.$on('refresh-treeview-items', (noteId)=> {
      this.getNoteById(noteId);
    });
  },
  methods: {
    open(noteId, source) {
      this.getNoteById(noteId);
      if (source === 'includePages') {
        this.isIncludePage = true;
      } else {
        this.isIncludePage = false;
      }
      this.$nextTick().then(() => {
        this.$refs.breadcrumbDrawer.open();
      });
    },
    fetchNoteChildren(childItem) {
      if ( !childItem.hasChild ) 
      {return;}
      return this.$notesService.getNoteTree(this.noteBookType,this.noteBookOwnerTree , childItem.id,'CHILDREN').then(data => {
        if (data && data.jsonList) {
          const noteChildTree = data.jsonList;
          const temporaryNoteChildren = [];
          noteChildTree.forEach(noteChildren => {
            this.makeChildren(noteChildren,temporaryNoteChildren);
          });
          childItem.children.push(...temporaryNoteChildren);
        }
      });
    },
    openNote(event, note) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      if (!this.includePage ) {
        this.activeItem = [note.id];
        this.$root.$emit('open-note-by-id',note.id);
        this.$refs.breadcrumbDrawer.close();
      } else {
        this.$root.$emit('include-page',note);
        document.dispatchEvent(new CustomEvent ('test'));
      }
      
    },
    makeChildren(noteChildren, childrenArray) {
      if ( noteChildren.hasChild ) {
        childrenArray.push ({
          id: noteChildren.path.split('%2F').pop(),
          hasChild: noteChildren.hasChild,
          name: noteChildren.name,
          children: []
        });
      } else {
        childrenArray.push({
          id: noteChildren.path.split('%2F').pop(),
          hasChild: noteChildren.hasChild,
          name: noteChildren.name
        });
      }
    },
    getNoteById(id,source) {
      return this.$notesService.getNoteById(id).then(data => {
        this.note = data || [];
        this.$notesService.getNotes(this.note.wikiType, this.note.wikiOwner , this.note.name,source).then(data => {
          this.note.breadcrumb = data && data.breadcrumb || [];
        });
      }).then(() => {
        this.note.wikiOwner =  this.note.wikiOwner.substring(1);
        this.retrieveNoteTree(this.note.wikiType, this.note.wikiOwner , this.note.name);
      });
    },
    retrieveNoteTree(noteType, noteOwner, noteName) {
      this.$notesService.getNoteTree(noteType, noteOwner , noteName,'ALL').then(data => {
        this.noteTree = data && data.jsonList || [];
        const noteChildren = this.makeNoteChildren(this.noteTree);
        const openedTreeviewItem = this.getOpenedTreeviewItems(this.note.breadcrumb);
        this.openNotes = openedTreeviewItem;
        this.activeItem = [openedTreeviewItem[openedTreeviewItem.length-1]];
        this.breadcrumbItems = noteChildren;
        this.noteBookType = noteType;
        this.noteBookOwnerTree = noteOwner;
      });
    },
    getOpenedTreeviewItems(breadcrumArray) {
      const activatedNotes = [];
      for (let index = 1; index < breadcrumArray.length; index++) {
        activatedNotes.push(breadcrumArray[index].id);
      }
      return activatedNotes;
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
  }
};
</script>