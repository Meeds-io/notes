<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.

-->

<template>
  <aside v-if="treeViewExpended" class="sidebar pb-5 ps-5 d-contents">
    <template>
      <v-card
        flat
        class="border-right-color full-height expand-transition-enter-active">
        <note-treeview-toolbar
          :filter="filter"
          class="border-bottom-color"
          compact-display
          @keyword-changed="search = $event" />
        <v-card-text class="px-0">
          <v-treeview
            ref="treeSearch"
            :items="items"
            :active="active"
            :open="openedItems"
            class="px-0 notes-custom-treeview treeview-item"
            item-key="noteId"
            expand-icon=""
            open-on-click
            hoverable
            activatable
            transition
            dense>
            <template #prepend="{ item, open }">
              <note-treeview-sidebar-item-prepend
                :note="item"
                :space-note="isSpaceNote"
                :note-wiki-owner="noteWikiOwner"
                :space-group-id="spaceGroupId"
                :open="open"
                class="me-2 ms-1"
                @fetch-children="fetchChildren" />
            </template>
            <template #label="{ item }">
              <note-treeview-sidebar-item-label
                :note="item"
                :is-draft-filter="isDraftFilter"
                :active-item="activeItem"
                :space-note="isSpaceNote"
                @open-note="openNote" />
            </template>
          </v-treeview>
        </v-card-text>
      </v-card>
    </template>
  </aside>
</template>

<script>
export default {
  props: {
    activeNoteId: {
      type: String,
      default: ''
    },
    treeViewExpended: {
      type: Boolean,
      default: true
    },
  },
  data: () => ({
    note: {},
    items: [],
    allItems: [],
    allItemsHome: [],
    home: {},
    noteBookType: '',
    noteBookOwnerTree: '',
    openNotes: [],
    activeItem: [],
    isIncludePage: false,
    movePage: false,
    exportNotes: false,
    selectionNotes: [],
    spaceId: eXo.env.portal.spaceId,
    spaceDisplayName: eXo.env.portal.spaceDisplayName,
    breadcrumb: [],
    destinationNote: {},
    displayArrow: true,
    closeAll: true,
    drawer: false,
    filter: 'published',
    filterOptions: [],
    checkbox: false,
    showTree: true,
    search: '',
    exportStatus: {status: '', action: {}},
    exporting: false,
    started: false,
    notesGetted: false,
    parentUpdated: false,
    jsonCreated: false,
    imageUrlsUpdated: false,
    zipCreated: false,
    tempCleaned: false,
    dataCreated: false,
    isLoading: false,
    selectionType: 'independent',
    inProgressTreeFetches: [],
    filterItems: [],
    filterItemsDraft: [],
    limit: 20,
    timeout: 1000,
    searchTimer: null,
    enableMove: false
  }),
  computed: {
    homeLabel() {
      return this.home?.name === 'Home' && this.noteHomeTitle || this.home.name;
    },
    isSpaceNote() {
      return this.noteWikiType === 'group';
    },
    noteHomeTitle() {
      return this.spaceId ? this.$t('notes.label.noteHome') : this.$t('notes.label.myNotes');
    },
    noteWikiType() {
      return this.note?.wikiType;
    },
    noteWikiOwner() {
      return this.isSpaceNote ? this.note?.wikiOwner?.substring(1) : this.note?.wikiOwner;
    },
    spaceGroupId() {
      return this.isSpaceNote &&  this.note?.wikiOwner?.startsWith?.('/spaces/') ? this.note?.wikiOwner : null;
    },
    openedItems() {
      return this.openNotes;
    },
    active() {
      return this.search
          && this.allItems
          && this.allItems.filter(item => item.name.toLowerCase().match(this.search.toLowerCase()))
          || this.activeItem;
    },
    includePage() {
      return this.isIncludePage;
    },
    isDraftFilter() {
      return this.filter === 'drafts';
    },
  },
  watch: {
    inProgressTreeFetches() {
      this.isLoading = this.inProgressTreeFetches?.length;
    },
    search() {
      clearTimeout(this.searchTimer);
      this.showTree = true;
      if (this.search) {
        this.searchTerm();
      } else {
        this.retrieveNoteTree(this.noteWikiType, this.noteWikiOwner, this.note.name);
      }
    },
    filter() {
      if (!this.search) {
        this.items = [];
        if (this.note && this.note.id) {
          if (this.note.draftPage) {
            this.getDraftNote(this.note.id);
          } else {
            this.getNoteById(this.note.id);
          }
        }
      } else {
        this.searchTerm();
      }
    },
  },
  created() {
    this.$root.$on('notes-filter-update', this.updateFilter);
    this.getNoteById(this.activeNoteId);
  },
  beforeDestroy() {
    this.$root.$off('notes-filter-update', this.updateFilter);
  },
  methods: {
    updateFilter(filter) {
      this.filter = filter;
    },
    fetchChildrenInParallel(children, level = 0) {
      if (level >= 2) {
        return;
      }
      children.forEach(item => {
        if (item.hasChild && !item?.children?.length) {
          this.inProgressTreeFetches.push(item.noteId);
          return this.$notesService.getNoteTreeLevel(item.path).then((data) => {
            item.children = this.mapItems(data?.jsonList);
            this.fetchChildrenInParallel(item.children, level + 1);
            const inProgressFetch = this.inProgressTreeFetches.indexOf(item.noteId);
            this.inProgressTreeFetches.splice(inProgressFetch, 1);
          });
        }
      });
    },
    selectChildren(item) {
      if (item.hasChild && !item?.children?.length) {
        this.inProgressTreeFetches.push(item.noteId);
        return this.$notesService.getNoteTreeLevel(item.path).then((data) => {
          item.children = this.mapItems(data?.jsonList);
          this.selectionNotes.push(
            ...item.children.map(item => item.noteId).filter(id => !this.selectionNotes.includes(id)));
          item.children.forEach(child => this.selectChildren(child));
          this.openItem(item);
          this.inProgressTreeFetches.splice(this.inProgressTreeFetches.indexOf(item.noteId), 1);
        });
      } else if (item?.children?.length) {
        this.selectionNotes.push(
          ...item.children.map(item => item.noteId).filter(id => !this.selectionNotes.includes(id)));
        this.openItem(item);
        item.children.forEach(item => this.selectChildren(item));
      }
    },
    deselectChildren(item) {
      if (item.children) {
        item.children.forEach((child) => {
          const index = this.selectionNotes.indexOf(child.noteId);
          if (index !== -1) {
            this.selectionNotes.splice(index, 1);
          }
          this.deselectChildren(child);
        });
      }
    },
    open(note, source, includeDisplay,filter) {
      if (note.draftPage) {
        this.filter = filter === 'published';
        this.getDraftNote(note.id);
      } else {
        this.filter = filter === 'drafts';
        this.getNoteById(note.id);
      }
      this.isIncludePage = source === 'includePages';
      this.displayArrow = !includeDisplay;
      if (source === 'movePage') {
        this.enableMove = false;
        this.movePage = true;
        this.exportNotes = false;
      }
      else if (source === 'exportNotes') {
        this.exportNotes = true;
        this.movePage = false;
      } else {
        this.movePage = false;
        this.exportNotes = false;
      }
      this.$nextTick().then(() => {
        this.$forceUpdate();
        this.$refs.breadcrumbDrawer.open();
      });
      const draftFilterValue = this.$t('notes.filter.label.drafts');
      if ((this.note.canManage || note.canManage) && !this.filterOptions.includes(draftFilterValue)) {
        this.filterOptions.push(draftFilterValue);
      }
    },
    openNote(event, note) {
      const noteIdParam = new URLSearchParams(window.location.search).get('noteId');
      const isEditDifferentNote = this.isEditMode && noteIdParam !== note.noteId;
      const isNotCurrentNote = this.note.id !== note.noteId;
      const canOpenNote = (!this.isDraftFilter || this.isDraftFilter && note.draftPage) && isEditDifferentNote || isNotCurrentNote;
      if (canOpenNote) {
        this.activeItem = [note.noteId];
        if (this.includePage) {
          this.$root.$emit('include-page', note);
        } else if (this.movePage) {
          if (note.noteId !== this.note.id && note.noteId !== this.note.parentPageId) {
            this.$notesService.getNoteById(note.noteId,'', '', '', '', true).then(data => {
              this.breadcrumb = data?.breadcrumb || [];
              this.enableMove = true;
              this.breadcrumb[0].name = this.noteHomeTitle;
              this.destinationNote = data;
            });
          } else {
            this.enableMove=false;
            this.breadcrumb = [];
            Object.assign(this.destinationNote, this.note);
          }
        } else {
          const noteName = note.draftPage ? note.noteId : note.path.split('%2F').pop();
          this.$root.$emit('open-note-by-name', noteName, note.draftPage);
        }
        document.dispatchEvent(new CustomEvent('note-navigation-updated', {detail: note}));
      }
    },
    getNoteById(id) {
      if (id) {
        return this.$notesService.getNoteById(id).then(data => {
          this.note = data || [];
          this.note.breadcrumb[0].title = this.noteHomeTitle;
          this.breadcrumb = this.note.breadcrumb;
        }).then(() => {
          this.retrieveNoteTree(this.noteWikiType, this.noteWikiOwner , this.note.name);
        });
      }
    },
    getDraftNote(id) {
      if (id) {
        return this.$notesService.getDraftNoteById(id).then(data => {
          this.note = data || [];
          this.note.breadcrumb[0].title = this.noteHomeTitle;
          this.breadcrumb = this.note.breadcrumb;
        }).then(() => {
          this.retrieveNoteTree(this.noteWikiType, this.noteWikiOwner, this.note.parentPageName);
        });
      }
    },
    fetchChildren(item) {
      if (item?.isOpen || item.expanded) {
        this.closeItem(item);
        return;
      }
      if (this.isDraftFilter) {
        this.openItem(item);
        return;
      }
      item.isLoading = true;
      this.$notesService.getNoteTreeLevel(item.path).then(data => {
        item.children = data?.jsonList;
        this.openItem(item);
        item.isLoading = false;
      });
    },
    openItem(item) {
      this.$refs.treeSearch.updateOpen(item.noteId, true);
      item.isOpen = true;
      item.expanded = item.isOpen;
    },
    closeItem(item) {
      this.$refs.treeSearch.updateOpen(item.noteId, false);
      item.isOpen = false;
      item.expanded = item.isOpen;
      if (!this.isDraftFilter) {
        item.children = [];
      }
    },
    mapItems(itemsArray) {
      for (let i = 0; i < itemsArray.length; i++) {
        const item = itemsArray[i];
        if (!item.hasChild && item.children) {
          delete item.children;
        }
      }
      return itemsArray;
    },
    naturalSort(items) {
      if (items?.length) {
        const collator = new Intl.Collator(eXo.env.portal.language, {numeric: true, sensitivity: 'base'});
        items.sort((a, b) => collator.compare(a.name, b.name));
      }
    },
    retrieveNoteTree(noteBookType, noteOwner, noteName, treeType) {
      if (this.isDraftFilter) {
        noteName = this.note?.breadcrumb[0]?.id;
        treeType = 'all';
      }
      this.isLoading = true;
      this.items = [];

      this.$notesService.getNoteTree(noteBookType, noteOwner, noteName, treeType, this.filter).then(data => {
        if (data?.jsonList?.length) {
          this.home = data.jsonList[0];
          this.home.name = this.homeLabel;
          this.items = [this.home];
          if (this.isDraftFilter) {
            this.naturalSort(this.items);
            this.filterItems = this.items;
            this.filterItemsDraft = [];
            this.filterItemsForSearch(this.filterItems);
          }
          this.allItems = data.treeNodeData;
          this.allItemsHome = [this.home];
        }
        this.isLoading = false;
        const openedTreeViewItems = this.getOpenedTreeViewItems(this.note.breadcrumb);
        this.openNotes = [...new Set([...this.openNotes, ...openedTreeViewItems])];
        this.activeItem = [openedTreeViewItems[openedTreeViewItems.length-1]];
        this.noteBookType = noteBookType;
        this.noteBookOwnerTree = noteOwner;
      });
    },
    getOpenedTreeViewItems(breadCrumbArray) {
      const activatedNotes = [];
      if (this.isDraftFilter) {
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
    searchTerm() {
      this.items = [];
      this.isLoading = true;
      this.searchTimer = setTimeout(() => {
        if (this.isDraftFilter) {
          this.items = this.filterItemsDraft.filter(item => item.name.includes(this.search));
        } else {
          this.$notesService.searchNotes(this.search, `/${this.noteWikiOwner}`, this.limit).then(data => {
            this.items = data?.jsonList.length ? this.toListNotes(data?.jsonList) : [];
            this.showTree = !!this.items.length;
          });
        }
        this.isLoading = false;
      }, this.timeout);
    },
    filterItemsForSearch(filterItems){
      filterItems.forEach(filterItem => {
        if (filterItem.draftPage) {
          this.filterItemsDraft.push(filterItem);
        }
        if (filterItem.hasChild) {
          this.filterItemsForSearch(filterItem.children);
        }
      });
    },
    toListNotes(items) {
      const itemsNotes = [];
      items.forEach(item => itemsNotes.push(this.toNote(item)));
      return itemsNotes;
    },
    toNote(note) {
      return {
        children: [],
        disabled: false,
        draftPage: false,
        name: note.title,
        nodeType: note.type,
        noteId: note.id,
        url: note.url,
        path: note.pageName,
        parentPageId: ''
      };
    }
  }
};
</script>

}