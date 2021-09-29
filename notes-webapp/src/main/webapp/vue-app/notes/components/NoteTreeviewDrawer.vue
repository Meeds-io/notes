<template>
  <div>
    <v-overlay
      z-index="1031"
      :value="drawer"
      @click.native="drawer = false" />

    <exo-drawer
      ref="breadcrumbDrawer"
      class="breadcrumbDrawer"
      v-model="drawer"
      show-overlay
      @closed="closeAllDrawer()" 
      right>
      <template v-if="isIncludePage && displayArrow" slot="title">
        <div class="d-flex">
          <v-icon size="19" @click="backToPlugins(); close()">mdi-arrow-left</v-icon>
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
            <div class="py-2 width-full">
              <span class="font-weight-bold text-color  pb-2">{{ $t('notes.label.movePageCurrentPosition') }}</span>
              <note-breadcrumb :note-breadcrumb="note ? note.breadcrumb : []" />
            </div>
          </v-list-item>
          <v-list-item>
            <div class="py-2  width-full">
              <span class="font-weight-bold text-color pb-2">{{ $t('notes.label.movePageDestination') }}</span>
              <note-breadcrumb :note-breadcrumb="currentBreadcrumb" />
            </div>
          </v-list-item>
          <v-list-item class="position-title">
            <div class="py-2">
              <span class="font-weight-bold text-color">{{ $t('notes.label.movePagePosition') }}</span>
            </div>
          </v-list-item>
        </v-layout>
        <v-col column>
          <v-row>
            <v-col class="my-auto">
              <v-text-field
                v-model="keyword"
                class="search"
                :placeholder=" $t('notes.label.filter') "
                clearable
                prepend-inner-icon="fa-filter" />
            </v-col>
            <v-col class="filter" cols="4">
              <div class="btn-group">
                <button class="btn dropdown-toggle" data-toggle="dropdown">
                  {{ filter }}
                  <i class="uiIconMiniArrowDown uiIconLightGray"></i><span></span>
                </button>
                <ul class="dropdown-menu">
                  <li><a href="#" @click="filter = filterOptions[0]"> {{ filterOptions[0] }} </a></li>
                  <li><a href="#" @click="filter = filterOptions[1]"> {{ filterOptions[1] }} </a></li>
                </ul>
              </div>
            </v-col>
          </v-row>
          <template v-if="home && filter !== $t('notes.filter.label.drafts')" class="ma-0 border-box-sizing">
            <v-list-item @click="openNote(event,home)">
              <v-list-item-content>
                <v-list-item-title class="body-2 treeview-home-link">
                  <a :href="home.noteId">{{ home.name }}</a>
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <template v-if="items && items.length">
            <v-treeview
              v-if="reload"
              :items="items"
              :open="openedItems"
              :active="active"
              :search="keyword"
              :filter="filterNotes"
              class="treeview-item"
              item-key="noteId"
              hoverable
              open-on-click
              transition>
              <template v-slot:label="{ item }">
                <v-list-item-title @click="openNote(event,item)" class="body-2">
                  <div v-if="filter === $t('notes.filter.label.drafts') && !item.draftPage">{{ item.name }}</div>
                  <span v-else :style="{color: filter === $t('notes.filter.label.drafts') && item.draftPage ? 'var(--allPagesBaseTextColor, #333333)' : ''}">{{ item.name }}</span>
                </v-list-item-title>
              </template>
            </v-treeview>
          </template>
        </v-col>
      </template>
      <template v-if="movePage" slot="footer">
        <div class="d-flex">
          <v-spacer />
          <v-btn
            @click="close"
            class="btn ml-2">
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            @click="moveNote()"
            class="btn btn-primary ml-2">
            {{ $t('notes.button.ok') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
export default {
  data: () => ({
    note: {},
    items: [],
    allItems: [],
    home: {},
    noteBookType: '',
    noteBookOwnerTree: '',
    openNotes: [],
    activeItem: [],
    isIncludePage: false,
    movePage: false,
    spaceDisplayName: eXo.env.portal.spaceDisplayName,
    breadcrumb: [],
    destinationNote: {},
    displayArrow: true,
    render: true,
    closeAll: true,
    drawer: false,
    filter: '',
    filterOptions: [],
    keyword: '',
  }),
  computed: {
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
    },
    reload () {
      return this.render;
    },
    filterNotes() {
      return (item, search, textKey) => item[textKey].toLowerCase().match(search.toLowerCase());
    },
  },
  watch: {
    filter() {
      if (this.note && this.note.id) {
        this.getNoteById(this.note.id);
      }
    }
  },
  created() {
    this.$root.$on('refresh-treeview-items', (noteId)=> {
      this.getNoteById(noteId);
    });
    this.$root.$on('close-note-tree-drawer', () => {
      this.close();
    });
    this.$root.$on('display-treeview-items', () => {
      this.closeAll = true;
    });
  },
  mounted() {
    this.filter = this.$t('notes.filter.label.all.notes');
    this.filterOptions = [
      this.$t('notes.filter.label.all.notes'),
      this.$t('notes.filter.label.drafts'),
    ];
  },
  methods: {
    open(noteId, source, includeDisplay) {
      this.render = false;
      this.getNoteById(noteId);
      if (source === 'includePages') {
        this.isIncludePage = true;
      } else {
        this.isIncludePage = false;
      }
      if (includeDisplay) {
        this.displayArrow =false;
      } else {
        this.displayArrow =true;
      }
      if (source === 'movePage') {
        this.movePage = true;
      } else {
        this.movePage = false;
      }
      this.$nextTick().then(() => {
        this.$forceUpdate();
        this.render = true;
        this.$refs.breadcrumbDrawer.open();
      });
    },
    backToPlugins() {
      this.closeAll = false;
    },
    openNote(event, note) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      const canOpenNote = this.filter === this.$t('notes.filter.label.drafts') && note.draftPage || this.filter !== this.$t('notes.filter.label.drafts');
      if (canOpenNote) {
        this.activeItem = [note.id];
        if ( !this.includePage && !this.movePage ) {
          const noteName = note.path.split('%2F').pop();
          this.$root.$emit('open-note-by-name', noteName);
          this.$refs.breadcrumbDrawer.close();
        }
        if (this.includePage) {
          this.$root.$emit('include-page',note);
          this.$refs.breadcrumbDrawer.close();
        }
        if (this.movePage) {
          this.$notesService.getNotes(this.note.wikiType, this.note.wikiOwner , note.id).then(data => {
            this.breadcrumb = data && data.breadcrumb || [];
            this.breadcrumb[0].name = this.$t('notes.label.noteHome');
            this.destinationNote = data;
          });
        }
      }
    },
    getNoteById(id) {
      if (id) {
        return this.$notesService.getNoteById(id).then(data => {
          this.note = data || [];
          this.note.breadcrumb[0].title = this.$t('notes.label.noteHome');
          this.breadcrumb = this.note.breadcrumb;
        }).then(() => {
          if (this.note.wikiType === 'group'){
            this.note.wikiOwner = this.note.wikiOwner.substring(1);
          }
          this.retrieveNoteTree(this.note.wikiType, this.note.wikiOwner , this.note.name);
        });
      }
    },
    retrieveNoteTree(noteType, noteOwner, noteName) {
      const withDrafts = this.filter === this.$t('notes.filter.label.drafts');
      this.$notesService.getFullNoteTree(noteType, noteOwner , noteName, withDrafts).then(data => {
        if (data && data.jsonList.length) {
          this.home = [];
          this.items = [];
          this.allItems = [];
          this.home = data.treeNodeData[0];
          this.items = data.treeNodeData[0].children;
          this.allItems = data.jsonList;
        }
        const openedTreeViewItems = this.getOpenedTreeViewItems(this.note.breadcrumb);
        this.openNotes = [];
        this.openNotes = openedTreeViewItems;
        this.activeItem = [];
        this.activeItem = [openedTreeViewItems[openedTreeViewItems.length-1]];
        this.noteBookType = noteType;
        this.noteBookOwnerTree = noteOwner;
      });
    },
    getOpenedTreeViewItems(breadCrumbArray) {
      const activatedNotes = [];
      if (this.filter === this.$t('notes.filter.label.drafts')) {
        const nodesToOpen = this.allItems.filter(item => !item.draftPage);
        const nodesToOpenIds = nodesToOpen.map(node => node.noteId);
        
        activatedNotes.push(...nodesToOpenIds);
      } else {
        for (let index = 1; index < breadCrumbArray.length; index++) {
          activatedNotes.push(breadCrumbArray[index].noteId);
        }
      }
      return activatedNotes;
    },
    moveNote() {
      this.$root.$emit('move-page', this.note, this.destinationNote);
    },
    close() {
      this.render = false;
      this.$refs.breadcrumbDrawer.close();
    },
    closeAllDrawer() {
      if (this.closeAll) {
        this.$emit('closed');
      }
    },
  }
};
</script>
