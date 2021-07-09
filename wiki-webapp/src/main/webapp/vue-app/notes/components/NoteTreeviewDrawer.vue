<template>
  <exo-drawer
    ref="breadcrumbDrawer"
    class="breadcrumbDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template v-if="isIncludePage" slot="title">
      <div class="d-flex">
        <v-icon size="19" @click="close()">mdi-arrow-left</v-icon>
        <span class="ps-2">{{ $t('notes.label.includePageTitle') }}</span>
      </div>
    </template>
    <template v-else-if="movePage" slot="title">
      {{ $t('notes.label.movePageTitle') }}
    </template>
    <template v-else slot="title">
      {{ $t('notes.label.breadcrumbTitle') }}
    </template>
    <template slot="content">
      <v-layout v-if="movePage" column>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="font-weight-bold text-color">{{ note.name }}</v-list-item-title>
          </v-list-item-content>
        </v-list-item>
        <v-list-item>
          <div class="d-flex align-center">
            <div class="pr-4"><span class="font-weight-bold text-color">{{ $t('notes.label.movePageSpace') }}</span></div>
            <div class="identitySuggester no-border mt-0">
              <v-chip
                class="identitySuggesterItem me-2 mt-2">
                <span class="text-truncate">
                  {{ spaceDisplayName }}
                </span>
              </v-chip>
            </div>
          </div>
        </v-list-item>
        <v-list-item>
          <div class="py-2">
            <span class="font-weight-bold text-color  pb-2">{{ $t('notes.label.movePageCurrentPosition') }}</span>
            <note-breadcrumb :note-breadcrumb="note.breadcrumb" />
          </div>
        </v-list-item>
        <v-list-item>
          <div class="py-2">
            <span class="font-weight-bold text-color pb-2">{{ $t('notes.label.movePageDestination') }}</span>
            <note-breadcrumb :note-breadcrumb="currentBreadcrumb" />
          </div>
        </v-list-item>
      </v-layout>
      <v-layout column>
        <template v-if="wikiHome" class="ma-0 border-box-sizing">
          <v-list-item @click="openNote(event,wikiHome)">
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
            class="treeview-item"
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
    <template v-if="movePage" slot="footer">
      <div class="d-flex">
        <v-spacer />
        <v-btn
          class="btn ml-2">
          {{ $t('notes.button.cancel') }}
        </v-btn>
        <v-btn
          @click="moveNote()"
          class="btn btn-primary ml-2">
          {{ $t('notes.menu.label.movePage') }}
        </v-btn>
      </div>
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
    isIncludePage: false,
    movePage: false,
    spaceDisplayName: eXo.env.portal.spaceDisplayName,
    breadcrumb: [],
    destinationNote: {},
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
    },
    currentBreadcrumb() {
      return this.breadcrumb;
    }
  },
  created() {
    this.$root.$on('refresh-treeview-items', (noteId)=> {
      this.getNoteById(noteId);
    });
    this.$root.$on('close-note-tree-drawer', () => {
      this.close();
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
      if (source === 'movePage') {
        this.movePage = true;
      } else {
        this.movePage = false;
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
      if ( !this.includePage && !this.movePage ) {
        this.activeItem = [note.id];
        this.$root.$emit('open-note-by-id',note.id);
        this.$refs.breadcrumbDrawer.close();
      }
      if (this.includePage) {
        this.activeItem = [note.id];
        this.$root.$emit('include-page',note);
      }
      if (this.movePage) {
        this.$notesService.getNotes(this.note.wikiType, this.note.wikiOwner , note.id).then(data => {
          this.breadcrumb = data && data.breadcrumb || []; 
          this.destinationNote = data;      
        });
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
          this.breadcrumb = this.note.breadcrumb;
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
    moveNote(){
      this.$root.$emit('move-page',this.note,this.destinationNote);
    },
    close(){
      this.$refs.breadcrumbDrawer.close();
    }
  }
};
</script>