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
  <v-list-item-title class="body-2">
    <div
      v-if="isDraftFilter && !draftPage"
      class="not-clickable">
      {{ noteLabel }}
    </div>
    <a
      v-else
      :href="draftPage ? `${noteId}/draft` : noteId"
      :class="{'text-color': (isDraftFilter && draftPage) || !draftPage}"
      :aria-current="(noteId === activeItem[0] && !draftPage) ? 'page' : null"
      @click.prevent="$emit('open-note', $event, note)">
      {{ noteLabel }}
    </a>
  </v-list-item-title>
</template>

<script>
export default {
  props: {
    note: {
      type: Object,
      default: null
    },
    isDraftFilter: {
      type: Boolean,
      default: false
    },
    spaceNote: {
      type: Boolean,
      default: false
    },
    activeItem: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    noteId() {
      return this.note.noteId;
    },
    noteName() {
      return this.note.name;
    },
    noteLabel() {
      return this.isHomePage ? this.noteHomeTitle : this.noteName;
    },
    draftPage() {
      return this.note.draftPage;
    },
    isHomePage() {
      return this.note.nodeType === 'WIKIHOME';
    },
    noteHomeTitle() {
      return this.spaceNote ? this.$t('notes.label.noteHome') : this.$t('notes.label.myNotes');
    },
  }
};
</script>