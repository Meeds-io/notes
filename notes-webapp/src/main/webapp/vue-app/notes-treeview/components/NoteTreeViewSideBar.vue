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
    <v-card
      flat
      class="border-right-color no-border-radius full-height">
      <note-treeview-toolbar
        :filter="filter"
        class="border-bottom-color"
        compact-display
        @keyword-changed="search = $event" />
      <v-card-text class="px-0 py-2 notes-tree-container">
        <div v-if="isLoading" class="d-flex justify-center py-4">
          <v-progress-circular
            indeterminate
            color="primary"
            size="32" />
        </div>
        <note-treeview-item-list
          v-else
          :items="items"
          :opened-items="openedItems"
          :active-item="activeItem"
          :space-note="isSpaceNote"
          :note-wiki-owner="noteWikiOwner"
          :space-group-id="spaceGroupId"
          :is-draft-filter="isDraftFilter"
          :show-actions="note && note.canManage"
          :level="0"
          @fetch-children="fetchChildren"
          @open-note="handleOpenNote"
          @toggle-open="handleToggleOpen"
          @reorder="handleReorder" />
      </v-card-text>
    </v-card>
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
    enableMove: false,
    currentPath: window.location.pathname,
    lang: eXo.env.portal.language,
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
      return this.isSpaceNote && this.note?.wikiOwner?.startsWith?.('/spaces/') ? this.note?.wikiOwner : null;
    },
    openedItems() {
      return this.openNotes;
    },
    includePage() {
      return this.isIncludePage;
    },
    isDraftFilter() {
      return this.filter === 'drafts';
    },
    notesPageName() {
      if (this.currentPath?.endsWith(eXo.env.portal.selectedNodeUri) || this.currentPath.endsWith(`${eXo.env.portal.selectedNodeUri}/`)){
        return 'homeNote';
      } else {
        const noteId = this.currentPath?.split(`${eXo.env.portal.selectedNodeUri}/`)[1];
        if (noteId) {
          return noteId;
        } else {
          return 'homeNote';
        }
      }
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
    handleOpenNote(payload) {
      const { event, note } = payload;
      this.openNote(event, note);
    },
    isOpen(item) {
      return this.openNotes.includes(item.noteId);
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
    getNoteById(id) {
      if (id) {
        return this.$notesService.getNoteById(id).then(data => {
          this.note = data || [];
          this.note.breadcrumb[0].title = this.noteHomeTitle;
          this.breadcrumb = this.note.breadcrumb;
        }).then(() => {
          this.retrieveNoteTree(this.noteWikiType, this.noteWikiOwner , this.note.name);
        });
      } else {
        return this.$notesService.getNote(this.$root.noteBookType, this.$root.noteBookOwner, this.notesPageName, '', this.lang).then(data => {
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
    mapItems(itemsArray) {
      if (!itemsArray) {
        return [];
      }
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
          this.items = this.isDraftFilter ? this.home.children : [this.home];
          if (this.isDraftFilter) {
            this.naturalSort(this.items);
            this.filterItems = this.items;
            this.filterItemsDraft = [];
            this.filterItemsForSearch(this.filterItems);
          }
          this.allItems = data.treeNodeData;
          this.allItemsHome = this.items;
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
    },
    handleToggleOpen(payload) {
      const { item, open } = payload;
      if (open) {
        if (!this.openNotes.includes(item.noteId)) {
          this.openNotes = [...this.openNotes, item.noteId];
        }
      } else {
        const index = this.openNotes.indexOf(item.noteId);
        if (index > -1) {
          this.openNotes.splice(index, 1);
        }
      }
    },
    handleReorder(payload) {
      const { event, items, level } = payload;
      if (event.moved) {
        this.onItemMoved(event.moved, items, level);
      } else if (event.added) {
        this.onItemAdded(event.added, items, level);
      } else if (event.removed) {
        this.onItemRemoved(event.removed, items, level);
      }
    },
    onItemMoved() {
      try {
        this.$root.$emit('alert-message', this.$t('notes.reorder.success.message'), 'success');
      } catch (error) {
        console.error('Error reordering note:', error);
        this.refreshTree();
      }
    },
    onItemAdded() {
      // TO DO
    },
    onItemRemoved() {
      // TO DO
    },
    fetchChildren(item) {
      if (!item.hasChild) {return;}
      if (this.openNotes.includes(item.noteId)) {
        this.closeItem(item);
        return;
      }
      if (item.children && item.children.length) {
        this.openItem(item);
        return;
      }
      this.$set(item, 'isLoading', true);

      this.$notesService.getNoteTreeLevel(item.path)
        .then(data => {
          this.$set(item, 'children', data?.jsonList || []);
          this.openItem(item);
        })
        .catch(error => {
          console.error('Error fetching children:', error);
        })
        .finally(() => {
          this.$set(item, 'isLoading', false);
        });
    },
    openItem(item) {
      if (!this.openNotes.includes(item.noteId)) {
        this.openNotes = [...this.openNotes, item.noteId];
      }
    },
    closeItem(item) {
      const index = this.openNotes.indexOf(item.noteId);
      if (index > -1) {
        this.openNotes.splice(index, 1);
      }
    },
    openNote(event, note) {
      if (event.defaultPrevented) {
        return;
      }
      const noteIdParam = new URLSearchParams(window.location.search).get('noteId');
      const isEditDifferentNote = this.isEditMode && noteIdParam !== note.noteId;
      const isNotCurrentNote = (this.note.id !== note.noteId) || (this.note.name !== note.name);
      const canOpenNote = (!this.isDraftFilter || (this.isDraftFilter && note.draftPage))
          && (isEditDifferentNote || isNotCurrentNote);
      if (canOpenNote) {
        this.activeItem = [note.noteId];
        if (this.includePage) {
          this.$root.$emit('include-page', note);
        } else {
          const noteName = note.draftPage ? note.noteId : note.path.split('%2F').pop();
          this.$root.$emit('open-note-by-name', noteName, note.draftPage);
        }
        document.dispatchEvent(new CustomEvent('note-navigation-updated', { detail: note }));
      }
    },
    refreshTree() {
      this.retrieveNoteTree(this.noteWikiType, this.noteWikiOwner, this.note.name);
    }
  }
};
</script>