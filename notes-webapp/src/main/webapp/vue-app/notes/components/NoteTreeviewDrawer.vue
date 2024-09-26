<template>
  <div>
    <v-overlay
      z-index="1031"
      :value="drawer"
      @click.native="drawer = false" />

    <exo-drawer
      ref="breadcrumbDrawer"
      class="breadcrumbDrawer"
      :confirm-close="exporting"
      :confirm-close-labels="confirmCloseLabels"
      allow-expand
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
      <template v-else-if="exportNotes" slot="title">
        {{ $t('notes.label.exportNotesTitle') }}
      </template>
      <template v-else slot="title">
        {{ $t('notes.label.breadcrumbTitle') }}
      </template>
      <template v-if="exporting" slot="content">
        <v-card flat class="pa-2 mt-10">            
          <v-list>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.started">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.started"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.started') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>

            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.notesGetted">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.notesGetted"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title v-if="exportStatus.exportedNotesCount>0"> {{ $t('notes.export.status.label.preparingNotes') }} ({{ exportStatus.exportedNotesCount }} notes)</v-list-item-title>
                  <v-list-item-title v-else> {{ $t('notes.export.status.label.preparingNotes') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>

            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.notesPrepared">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.notesPrepared"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.updatingNoteParents') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.jsonCreated">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.jsonCreated"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.creatingJson') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.imageUrlsUpdated">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.imageUrlsUpdated"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.updatingImages') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.featuredImagesProcessed">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.featuredImagesProcessed"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.processingFeaturedImages') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.zipCreated">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.zipCreated"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.creatingZip') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.tempCleaned">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.tempCleaned"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.cleaningTemps') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
            <v-list-item>
              <template>
                <v-list-item-action class="mr-3">
                  <v-icon
                    color="success"
                    size="18"
                    v-if="exportStatus.action.dataCreated">
                    fa-check
                  </v-icon>
                  <v-progress-circular
                    v-if="!exportStatus.action.dataCreated"
                    color="primary"
                    indeterminate
                    size="18" />
                </v-list-item-action>
                <v-list-item-content>
                  <v-list-item-title>{{ $t('notes.export.status.label.done') }}</v-list-item-title>
                </v-list-item-content>
              </template>
            </v-list-item>
          </v-list>
        </v-card>
      </template>
      <template slot="content" v-if="!exporting">
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
          <div v-if="!exportNotes && !movePage" class="d-flex mb-4">
            <div class="flex-grow-1 me-2 d-flex content-box-sizing">
              <v-text-field
                v-model="search"
                :placeholder=" $t('notes.label.filter') "
                class="search mt-auto"
                clearable
                hide-details
                font-size="18"
                prepend-inner-icon="fa-filter" />
            </div>
            <div class="filter">
              <select
                v-model="filter"
                :disabled="isLoading"
                class="selectSpacesFilter my-auto width-auto me-2 subtitle-1 ignore-vuetify-classes d-none d-sm-inline">
                <option
                  v-for="filterOption in filterOptions"
                  :key="filterOption"
                  :value="filterOption">
                  {{ filterOption }}
                </option>
              </select>
            </div>
          </div>
          <template v-if="home && !exportNotes && resultSearch && !search">
            <v-list-item @click.prevent="openNote(event,home)" class="ma-0 border-box-sizing">
              <v-list-item-content>
                <v-list-item-title class="body-2 treeview-home-link">
                  <span v-if="filter === $t('notes.filter.label.drafts')" :style="{color: 'rgba(0, 0, 0, 0.38)!important', cursor: 'default'}">{{ home.name==='Home'?$t('notes.label.noteHome'):home.name }}</span>
                  <a v-else :href="home.noteId">{{ home.name==='Home'?$t('notes.label.noteHome'):home.name }}</a>
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <template v-if="items?.length && exportNotes">
            <v-checkbox
              v-model="checkbox"
              :label="selectExportLabel"
              class="checkbox mt-0 pl-3" />
            <v-treeview
              v-if="reload"
              ref="treeSearch"
              v-model="selectionNotes"
              :items="items"
              :open.sync="openLevel"
              :selection-type="selectionType"
              :load-children="loadChildren"
              class="treeview-item"
              item-key="noteId"
              hoverable
              selectable
              activatable
              open-on-click
              transition />
          </template>
          <template v-else-if="!exportNotes">
            <v-treeview
              v-if="reload"
              ref="treeSearch"
              :items="items"
              :active="active"
              :open="openedItems"
              class="ps-1 notes-custom-treeview treeview-item"
              item-key="noteId"
              expand-icon=""
              open-on-click
              hoverable
              activatable
              transition
              dense>
              <template #prepend="{ item, open }">
                <v-btn
                  v-if="item.hasChild"
                  :loading="item.isLoading"
                  class="me-n3"
                  width="30"
                  height="30"
                  min-width="30"
                  icon
                  @click="fetchChildren(item, $refs.treeSearch)">
                  <v-icon
                    size="16">
                    {{ open ? 'fas fa-caret-down' : 'fas fa-caret-right' }}
                  </v-icon>
                </v-btn>
              </template>
              <template #label="{ item }">
                <v-list-item-title class="body-2">
                  <div
                    v-if="isDraftFilter && !item.draftPage"
                    class="not-clickable">
                    {{ item.name }}
                  </div>
                  <a
                    v-else
                    :href="item.draftPage ? `${item.noteId}/draft` : item.noteId"
                    :class="{'text-color': (isDraftFilter && item.draftPage) || !item.draftPage}"
                    @click.prevent="openNote($event,item)">
                    {{ item.name }}
                  </a>
                </v-list-item-title>
              </template>
            </v-treeview>
          </template>
          <template v-if="!resultSearch">
            <div class="note-not-found-wrapper text-center mt-6">
              <v-img
                :src="noteNotFountImage"
                class="mx-auto"
                max-height="85"
                max-width="90"
                contain
                eager />
              <p class="mt-3 text-light-color">{{ $t('notes.label.noteSearchNotFound') + search }}</p>
            </div>
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
      <template v-if="exportNotes" slot="footer">
        <div class="d-flex">
          <v-spacer />
          <v-btn
            @click="close"
            class="btn ml-2">
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            :disabled="exporting"
            @click="exportNotesToZip()"
            class="btn btn-primary ml-2">
            {{ $t('notes.button.export') }}
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
    spaceDisplayName: eXo.env.portal.spaceDisplayName,
    breadcrumb: [],
    destinationNote: {},
    displayArrow: true,
    render: true,
    closeAll: true,
    drawer: false,
    filter: '',
    filterOptions: [],
    checkbox: false,
    showTree: true,
    search: '',
    noteNotFountImage: '/notes/skin/images/notes_not_found.png',
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
    selection: 'independent',
  }),
  props: {
    selectedTranslation: {
      type: String,
      default: () => null,
    }
  },
  computed: {
    confirmCloseLabels() {
      return {
        title: this.$t('notes.confirmCancelExport.title'),
        message: this.$t('notes.confirmCancelExport'),
        ok: this.$t('notes.button.yes'),
        cancel: this.$t('notes.button.no'),
      };
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
    currentBreadcrumb() {
      return this.breadcrumb;
    },
    reload() {
      return this.render;
    },
    resultSearch() {
      return this.showTree;
    },
    selectExportLabel() {
      if (this.checkbox === true) {
        return this.$t('notes.label.export.deselectAll');
      } else {
        return this.$t('notes.label.export.selectAll');
      }
    },
    openLevel() {
      return [this.home.noteId];
    },
    selectionType() {
      return this.selection;
    },
    isDraftFilter() {
      return this.filter === this.$t('notes.filter.label.drafts');
    }
  },
  watch: {
    isLoading() {
      if (this.isLoading) {
        this.$refs.breadcrumbDrawer.startLoading();
      } else {
        this.$refs.breadcrumbDrawer.endLoading();
      }
    },
    search() {
      this.showTree = true;
      if (this.search) {
        this.items = this.active;
        this.items.forEach(item => {
          item.children = null;
        });
        this.showTree = !!this.active.length;
      } else {
        this.retrieveNoteTree(this.note.wikiType, this.note.wikiOwner, this.note.name);
      }
    },
    checkbox() {
      this.selectAllItems(this.checkbox);
    },
    filter() {
      if (this.note && this.note.id) {
        if (this.note.draftPage) {
          this.getDraftNote(this.note.id);
        } else {
          this.getNoteById(this.note.id);
        }
      }
    },
  },
  created() {
    this.$root.$on('refresh-treeView-items', (note)=> {
      if (note.draftPage) {
        this.getDraftNote(note.id);
      } else {
        this.getNoteById(note.id);
      }
    });
    this.$root.$on('close-note-tree-drawer', () => {
      this.close();
    });
    this.$root.$on('display-treeview-items', () => {
      this.closeAll = true;
    });
  },
  mounted() {
    this.filterOptions = [
      this.$t('notes.filter.label.published.notes')
    ];
    this.filter = this.filterOptions[0];
  },
  methods: {
    selectAllItems(select) {
      this.$refs.treeSearch.selectionType = 'leaf';
      this.$refs.treeSearch.updateSelected(this.home.noteId, select);
    },
    open(note, source, includeDisplay,filter) {
      this.render = false;
      if (note.draftPage) {
        this.filter = filter === 'published' && this.filterOptions[0] || this.filterOptions[1];
        this.getDraftNote(note.id);
      } else {
        this.filter = filter === 'draft' && this.filterOptions[1] || this.filterOptions[0];
        this.getNoteById(note.id);
      }
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
        this.render = true;
        this.$refs.breadcrumbDrawer.open();
      });
      const draftFilterValue = this.$t('notes.filter.label.drafts');
      if ((this.note.canManage || note.canManage) && !this.filterOptions.includes(draftFilterValue)) {
        this.filterOptions.push(draftFilterValue);
      }
    },
    backToPlugins() {
      this.closeAll = false;
    },
    openNote(event, note) {
      const canOpenNote = (this.filter !== this.$t('notes.filter.label.drafts') || this.filter === this.$t('notes.filter.label.drafts') && note.draftPage) && note.noteId !== this.note.id;
      if (canOpenNote) {
        //reinitialize filter
        this.filter = this.filterOptions[0];
        this.activeItem = [note.noteId];
        if (this.includePage) {
          this.$root.$emit('include-page', note);
          this.$refs.breadcrumbDrawer.close();
        } else if (this.movePage) {
          if (note.noteId !== this.note.id) {
            this.$notesService.getNoteById(note.noteId,'', '', '', '', true).then(data => {
              this.breadcrumb = data && data.breadcrumb || [];
              this.breadcrumb[0].name = this.$t('notes.label.noteHome');
              this.destinationNote = data;
            });
          } else {
            Object.assign(this.destinationNote, this.note);
          }
        } else {
          const noteName = note.draftPage ? note.noteId : note.path.split('%2F').pop();
          this.$root.$emit('open-note-by-name', noteName, note.draftPage);
          this.$refs.breadcrumbDrawer.close();
        }
      } else {
        this.$refs.breadcrumbDrawer.close();
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
    getDraftNote(id) {
      if (id) {
        return this.$notesService.getDraftNoteById(id).then(data => {
          this.note = data || [];
          this.note.breadcrumb[0].title = this.$t('notes.label.noteHome');
          this.breadcrumb = this.note.breadcrumb;
        }).then(() => {
          if (this.note.wikiType === 'group') {
            this.note.wikiOwner = this.note.wikiOwner.substring(1);
          }
          this.retrieveNoteTree(this.note.wikiType, this.note.wikiOwner, this.note.parentPageName);
        });
      }
    },
    loadChildren(item) {
      return this.$notesService.getNoteTreeLevel(item.path).then(data => {
        item.children = this.mapItems(data?.jsonList);
      });
    },
    fetchChildren(item, treeview) {
      if (item?.isOpen || item.expanded) {
        this.closeItem(item, treeview);
        return;
      }
      if (this.isDraftFilter) {
        this.openItem(item, treeview);
        return;
      }
      item.isLoading = true;
      this.$notesService.getNoteTreeLevel(item.path).then(data => {
        item.children = data?.jsonList;
        this.openItem(item, treeview);
        item.isLoading = false;
      });
    },
    openItem(item, treeview) {
      treeview.updateOpen(item.noteId, true);
      item.isOpen = true;
      item.expanded = item.isOpen;

    },
    closeItem(item, treeview) {
      treeview.updateOpen(item.noteId, false);
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
      const noteType = this.isDraftFilter && 'drafts' || 'published';
      this.isLoading = true;
      this.items = [];
      this.$notesService.getNoteTree(noteBookType, noteOwner, noteName, treeType, noteType).then(data => {
        if (data?.jsonList?.length) {
          this.home = data.jsonList[0];
          this.items = this.exportNotes && [this.home] || data.jsonList[0].children;
          if (this.exportNotes) {
            if (!this.items[0].hasChild) {
              delete this.items[0].children;
            } else {
              this.mapItems(this.items[0]?.children);
            }
          }
          if (this.isDraftFilter) {
            this.naturalSort(this.items);
          }
          this.allItems = data.treeNodeData;
          this.allItemsHome = data.jsonList[0].children;
        }
        this.isLoading = false;
        const openedTreeViewItems = this.getOpenedTreeViewItems(this.note.breadcrumb);
        this.openNotes = openedTreeViewItems;
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
    moveNote() {
      if (this.note.id !== this.destinationNote.id && this.note.parentPageId !== this.destinationNote.id) {
        this.$root.$emit('move-page', this.note, this.destinationNote);
      }
    },
    exportNotesToZip(){
      this.$root.$emit('export-notes',this.selectionNotes,this.checkbox,this.home.noteId);
      this.exporting=true;
    },
    resetImport(){
      this.checkbox = false;
      this.selectionNotes = [];
    },
    close() {
      this.render = false;
      this.$refs.breadcrumbDrawer.close();
      if (this.exporting){
        this.$root.$emit('cancel-export-notes');
        this.exportStatus = {};
        this.exporting = false; 
        this.resetImport();
      }
    },
    closeAllDrawer() {
      this.checkbox = false;
      $('.spaceButtomNavigation').removeClass('hidden');
      this.search = '';
      if (this.exporting){
        this.$root.$emit('cancel-export-notes');
        this.exportStatus = {};
        this.exporting = false;
        this.resetImport();
      }
      if (this.closeAll) {
        this.$emit('closed');
      }
    },
    setExportStaus(exportStatus) {
      this.exportStatus = exportStatus;
      if (!this.exportStatus.status || this.exportStatus.status==='ZIP_CREATED'){
        this.exporting = false;
        this.$nextTick().then(() => this.close());
      }
    }
  }
};
</script>
