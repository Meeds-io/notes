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
  <div class="d-flex flex-grow-1">
    <v-btn
      v-if="hasChild"
      :loading="isLoading"
      icon
      @click="$emit('fetch-children', note)">
      <v-icon
        size="24">
        {{ open ? 'mdi-menu-down' : 'mdi-menu-right' }}
      </v-icon>
    </v-btn>
    <template v-if="isHomePage">
      <exo-user-avatar
        v-if="!spaceNote"
        :profile-id="noteWikiOwner"
        :size="24"
        :popover="false"
        extra-class="align-self-center"
        avatar />
    </template>
  </div>
</template>

<script>
export default {
  props: {
    note: {
      type: Object,
      default: null
    },
    noteWikiOwner: {
      type: String,
      default: null,
    },
    spaceGroupId: {
      type: String,
      default: null,
    },
    spaceNote: {
      type: Boolean,
      default: false
    },
    open: {
      type: Boolean,
      default: false
    },
    isDraftFilter: {
      type: Boolean,
      default: false
    }
  },
  data() {
    return {
      hover: false,
    };
  },
  computed: {
    noteId() {
      return this.note.noteId;
    },
    hasChild() {
      return this.note.hasChild;
    },
    isLoading() {
      return this.note.isLoading;
    },
    isHomePage() {
      return this.note.nodeType === 'WIKIHOME';
    }
  }
};
</script>