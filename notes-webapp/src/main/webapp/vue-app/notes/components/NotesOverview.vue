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
  <v-container fluid class="notes-layout pa-0">
    <div class="row no-gutters">
      <div :class="treeViewExpended ? 'col-3' : ''">
        <note-treeview-sideBar
          :tree-view-expended="treeViewExpended"
          :active-note-id="currentNoteId"
          @note-selected="onSelectNote" />
      </div>
      <div :class="treeViewExpended ? 'col-9' : 'col-12'">
        <notes-page
          :tree-view-expended="treeViewExpended"
          :note-id="currentNoteId" />
      </div>
    </div>
  </v-container>
</template>

<script>
export default {
  data() {
    return {
      currentNoteId: null,
      treeViewExpended: true
    };
  },
  created() {
    this.currentNoteId = this.getNoteIdFromUrl();
    this.$root.$on('sidebar-tree-view-expend', this.extendTreeView);
  },
  mounted() {
    window.addEventListener('popstate', this.onPopState);
  },
  beforeDestroy() {
    window.removeEventListener('popstate', this.onPopState);
  },
  methods: {
    getNoteIdFromUrl() {
      const match = location.pathname.match(/\/notes\/(.+)/);
      return match ? match[1] : null;
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
