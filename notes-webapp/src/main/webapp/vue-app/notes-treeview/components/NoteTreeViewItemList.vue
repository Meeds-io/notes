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
      :list="localItems"
      :group="{ name: 'notes-tree' }"
      :animation="200"
      :disabled="isDraftFilter"
      ghost-class="ghost-note"
      drag-class="dragging-note"
      chosen-class="chosen-note"
      handle=".drag-handle"
      class="draggable-list"
      @start="onDragStart"
      @end="onDragEnd"
      @change="onDragChange">
      <note-treeview-item
        v-for="item in localItems"
        :key="item.noteId"
        :item="item"
        :opened-items="openedItems"
        :active-item="activeItem"
        :space-note="spaceNote"
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
    }
  },
  data() {
    return {
      isDragging: false
    };
  },
  computed: {
    localItems: {
      get() {
        return this.items || [];
      },
      set(value) {
        this.$emit('update:items', value);
      }
    }
  },
  methods: {
    isOpen(noteId) {
      return this.openedItems && this.openedItems.includes(noteId);
    },
    isActive(item) {
      return this.activeItem && this.activeItem.includes(item.noteId);
    },
    toggle(event, item) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      if (!item.hasChild) {return;}

      if (this.isOpen(item.noteId)) {
        this.$emit('toggle-open', { item, open: false });
      } else {
        if (!item.children || !item.children.length) {
          this.$emit('fetch-children', item);
        } else {
          this.$emit('toggle-open', { item, open: true });
        }
      }
    },
    handleOpenNote(event, note) {
      if (event.target.closest('.expand-icon') || event.target.closest('.drag-handle')) {
        return;
      }
      this.$emit('open-note', { event, note });
    },

    handleChildOpenNote(payload) {
      this.$emit('open-note', payload);
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
          items: this.localItems,
          level: this.level
        });
      }
    }
  }
};
</script>