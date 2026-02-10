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
  <v-list
    dense
    class="py-0 note-tree-list">
    <draggable
      :list="items"
      :group="{ name: 'notes-tree' }"
      :animation="200"
      :disabled="isDraftFilter"
      :data-level="level"
      :move="checkMove"
      ghost-class="ghost-note"
      drag-class="dragging-note"
      chosen-class="chosen-note"
      handle=".drag-handle"
      class="draggable-list"
      @start="onDragStart"
      @end="onDragEnd"
      @change="onDragChange">
      <note-treeview-item
        v-for="item in items"
        :key="item.noteId"
        :item="item"
        :opened-items="openedItems"
        :active-item="activeItem"
        :space-note="spaceNote"
        :parent-id="item.noteId"
        :note-wiki-owner="noteWikiOwner"
        :space-group-id="spaceGroupId"
        :is-draft-filter="isDraftFilter"
        :show-actions="showActions"
        :level="level"
        @fetch-children="$emit('fetch-children', $event)"
        @open-note="handleChildOpenNote"
        @toggle-open="$emit('toggle-open', $event)"
        @reorder="$emit('reorder', $event)"
        @action="$emit('action', $event)" />
    </draggable>
  </v-list>
</template>

<script>
export default {
  props: {
    items: {
      type: Array,
      default: () => []
    },
    openedItems: {
      type: Array,
      default: () => []
    },
    activeItem: {
      type: Array,
      default: () => []
    },
    spaceNote: {
      type: Boolean,
      default: false
    },
    noteWikiOwner: {
      type: String,
      default: ''
    },
    spaceGroupId: {
      type: String,
      default: null
    },
    isDraftFilter: {
      type: Boolean,
      default: false
    },
    showActions: {
      type: Boolean,
      default: false
    },
    level: {
      type: Number,
      default: 0
    },
    parentId: {
      type: String,
      default: null
    },
  },
  data() {
    return {
      isDragging: false
    };
  },
  methods: {
    isOpen(noteId) {
      return this.openedItems && this.openedItems.includes(noteId);
    },
    handleChildOpenNote(payload) {
      this.$emit('open-note', payload);
    },
    checkMove(evt) {
      const fromLevel = parseInt(evt.from.dataset.level || 0, 10);
      const toLevel = parseInt(evt.to.dataset.level || 0, 10);
      if (fromLevel > 0 && toLevel === 0) {
        return false;
      }
      return !(fromLevel === 0 && toLevel > 0);
    },
    onDragStart() {
      this.isDragging = true;
      document.body.classList.add('is-dragging-note');
    },
    onDragEnd() {
      this.isDragging = false;
      document.body.classList.remove('is-dragging-note');
    },
    onDragChange(evt) {
      if (evt.added || evt.moved || evt.removed) {
        this.$emit('reorder', {
          event: evt,
          items: this.items,
          level: this.level,
          parentId: this.parentId
        });
      }
    }
  }
};
</script>