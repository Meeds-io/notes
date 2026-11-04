<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2026 Meeds Association contact@meeds.io

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
  <v-treeview
    ref="noteTreeview"
    :items="noteChildItems"
    class="ps-1 notes-custom-treeview"
    item-key="noteId"
    expand-icon=""
    dense>
    <template #prepend="{ item, open }">
      <note-treeview-item-prepend
        :item="item"
        :open="open"
        @click="handleFetchChildren(item, $refs.noteTreeview)" />
    </template>
    <template #label="{ item }">
      <note-content-table-item
        :note="item"
        @open-note="handleOpenNoteChild" />
    </template>
  </v-treeview>
</template>
<script>
export default {
  props: {
    noteId: {
      type: [Number, String],
      default: 0
    },
    source: {
      type: String,
      default: ''
    },
    noteBookType: {
      type: String,
      default: ''
    },
    noteBookOwner: {
      type: String,
      default: ''
    }
  },
  inject: ['fetchChildren', 'getNoteLanguages', 'openNoteChild'],
  data() {
    return {
      noteChildItems: [],
      selectedTranslation: { value: eXo.env.portal.language },
      note: null
    };
  },
  created() {
    this.$nextTick().then(() => {
      this.getNodeById(this.noteId, this.source, this.noteBookType, this.noteBookOwner);
    });
  },
  methods: {
    handleOpenNoteChild(item) {
      if (typeof this.openNoteChild === 'function') {
        return this.openNoteChild(item);
      }
    },
    handleFetchChildren(item, treeview) {
      if (typeof this.fetchChildren === 'function') {
        return this.fetchChildren(item, treeview);
      }
    },
    handleGetNoteLanguages(noteId) {
      if (typeof this.getNoteLanguages === 'function') {
        return this.getNoteLanguages(noteId);
      }
    },
    getNodeById(noteId, source, noteBookType, noteBookOwner) {
      return this.$notesService
        .getNoteById(noteId, this.selectedTranslation.value, source, noteBookType, noteBookOwner)
        .then(data => {
          this.note = data || {};
          this.handleGetNoteLanguages(noteId);
          return this.$notesService.getNoteTree(data.wikiType, data.wikiOwner, data.name, 'children');
        })
        .then(data => {
          if (data?.jsonList?.length) {
            this.noteChildItems = data.jsonList;
          }
        }).catch(e => {
          console.error('Error when getting note', e);
        });
    }
  }
};
</script>
