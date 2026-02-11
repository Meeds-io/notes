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
  <v-container fluid class="pa-0">
    <v-row no-gutters class="pa-5 flex-nowrap">
      <v-sheet
        v-if="treeViewExpended && !$root.isMobile"
        :width="sidebarWidth"
        class="overflow-auto">
        <note-treeview-sideBar
          :tree-view-expended="treeViewExpended"
          :active-note-id="currentNoteId"
          @note-selected="onSelectNote" />
      </v-sheet>
      <v-sheet
        v-if="treeViewExpended"
        width="8"
        class="d-flex align-center justify-center"
        style="cursor: col-resize;"
        @mousedown="startResize" />
      <v-col class="pa-0">
        <notes-page
          :tree-view-expended="treeViewExpended"
          :note-id="currentNoteId" />
      </v-col>
    </v-row>
    <note-treeview-filter-drawer />
  </v-container>
</template>

<script>
export default {
  data() {
    return {
      currentNoteId: null,
      treeViewExpended: true,
      sidebarWidth: 320,
      minWidth: 240,
      maxWidth: 480,
      isResizing: false,
      startX: 0,
      startWidth: 0
    };
  },
  created() {
    this.currentNoteId = this.getNoteIdFromUrl();
    this.$root.$on('sidebar-tree-view-expend', this.extendTreeView);
  },
  mounted() {
    window.addEventListener('popstate', this.onPopState);
    window.addEventListener('mousemove', this.resize);
    window.addEventListener('mouseup', this.stopResize);
  },

  beforeDestroy() {
    this.$root.$off('sidebar-tree-view-expend', this.extendTreeView);
    window.removeEventListener('mousemove', this.resize);
    window.removeEventListener('mouseup', this.stopResize);
    window.removeEventListener('popstate', this.onPopState);
  },

  methods: {
    getNoteIdFromUrl() {
      const match = location.pathname.match(/\/notes\/(.+)/);
      return match ? match[1] : null;
    },
    startResize(e) {
      this.isResizing = true;
      this.startX = e.clientX;
      this.startWidth = this.sidebarWidth;
      document.body.style.userSelect = 'none';
      document.body.style.cursor = 'col-resize';
    },
    resize(e) {
      if (!this.isResizing) {
        return;
      }
      const delta = e.clientX - this.startX;
      this.sidebarWidth = Math.min(
        Math.max(this.startWidth + delta, this.minWidth),
        this.maxWidth
      );
    },
    stopResize() {
      if (!this.isResizing) {
        return;
      }
      this.isResizing = false;
      document.body.style.userSelect = '';
      document.body.style.cursor = '';
    },
    onSelectNote(id) {
      history.pushState({}, '', `/notes/${id}`);
      this.currentNoteId = id;
    },
    onPopState() {
      this.currentNoteId = this.getNoteIdFromUrl();
    },
    extendTreeView(value) {
      this.treeViewExpended = value;
      localStorage.setItem('expendedTreeView', value);
    },
  }
};
</script>
