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
  <div>
    <v-hover
      v-model="hover">
      <v-list-item
        :class="[
          'tree-item rounded mx-1 my-1',
          { 'v-list-item--active primary--text': isActive(item) }
        ]"
        :style="{ paddingLeft: `${level * 20 + 8}px` }"
        :ripple="false"
        dense
        @click="handleOpenNote($event, item)">
        <v-list-item-icon
          v-if="!isDraftFilter && !isHomePage"
          class="drag-handle my-auto mr-1">
          <v-icon
            v-if="hover"
            small
            color="grey lighten-1">
            fas fa-grip-vertical
          </v-icon>
        </v-list-item-icon>
        <v-list-item-icon
          class="my-auto mr-2 expand-icon"
          @click.stop="toggle($event, item)">
          <template v-if="item.hasChild">
            <note-treeview-sidebar-item-prepend
              :note="item"
              :space-note="spaceNote"
              :note-wiki-owner="noteWikiOwner"
              :space-group-id="spaceGroupId"
              :open="isOpen(item.noteId)"
              :is-draft-filter="isDraftFilter"
              class="align-center" />
          </template>
        </v-list-item-icon>
        <v-list-item-content>
          <v-list-item-title
            :class="[
              'text-truncate',
              { 'font-weight-medium primary--text': isActive(item) }
            ]">
            {{ item.name }}
          </v-list-item-title>
        </v-list-item-content>
      </v-list-item>
    </v-hover>
    <v-expand-transition>
      <div
        v-show="isOpen(item.noteId) && item.hasChild"
        class="tree-children">
        <note-treeview-item-list
          v-if="item.children"
          :items="item.children"
          :opened-items="openedItems"
          :active-item="activeItem"
          :space-note="spaceNote"
          :note-wiki-owner="noteWikiOwner"
          :space-group-id="spaceGroupId"
          :parent-id="item.noteId"
          :is-draft-filter="isDraftFilter"
          :level="level + 1"
          @fetch-children="$emit('fetch-children', $event)"
          @open-note="handleChildOpenNote"
          @toggle-open="$emit('toggle-open', $event)"
          @reorder="$emit('reorder', $event)"
          @action="$emit('action', $event)" />
        <v-list-item
          v-else-if="item.isLoading"
          :style="{ paddingLeft: `${(level + 1) * 20 + 8}px` }"
          class="mx-1">
          <v-list-item-icon class="my-auto mr-3">
            <v-progress-circular
              indeterminate
              size="16"
              width="2"
              color="grey" />
          </v-list-item-icon>
        </v-list-item>
      </div>
    </v-expand-transition>
  </div>
</template>

<script>
export default {
  props: {
    item: {
      type: Object,
      default: () => ({})
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
    level: {
      type: Number,
      default: 0
    }
  },
  data() {
    return {
      isDragging: false,
      hover: false,
    };
  },
  computed: {
    isHomePage() {
      return this.item.nodeType === 'WIKIHOME';
    }
  },
  watch: {
    'item.children.length'(newLength) {
      this.$set(this.item, 'hasChild', newLength > 0);
      if (newLength === 0 && this.isOpen(this.item.noteId)) {
        this.$emit('toggle-open', { item: this.item, open: false });
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
  }
};
</script>