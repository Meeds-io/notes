<template>
  <exo-drawer
    ref="breadcrumbDrawer"
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
    activeItem: []
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
    }
  },
  created() {
    this.$root.$on('refresh-treeview-items', (noteChildren, noteBookType, noteBookOwnerTree, openedTreeviewItems )=> {
      this.openNotes = openedTreeviewItems;
      this.activeItem = [this.openNotes[this.openNotes.length-1]];
      this.breadcrumbItems = noteChildren;
      this.noteBookType = noteBookType;
      this.noteBookOwnerTree = noteBookOwnerTree;
    });
  },
  methods: {
    open(noteTreeview, noteBookType, noteBookOwnerTree, openedNotes) {
      if (this.openNotes && !this.openNotes.length) {
        this.openNotes = openedNotes;
      }
      if (this.activeItem && !this.activeItem.length) {
        this.activeItem = [this.openNotes[this.openNotes.length-1]];
      }
      if (this.breadcrumbItems && !this.breadcrumbItems.length) {
        this.breadcrumbItems = noteTreeview;
      }
      this.noteBookType = noteBookType;
      this.noteBookOwnerTree = noteBookOwnerTree;
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
      this.activeItem = [note.id];
      this.$root.$emit('open-note-by-id',note.id);
      this.$refs.breadcrumbDrawer.close();
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
  }
};
</script>