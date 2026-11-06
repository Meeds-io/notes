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
  <div class="note-move-buttons">
    <div class="note-move-buttons-inner d-flex align-center">
      <v-btn
        :id="`noteMoveUp-${item.noteId}`"
        :disabled="position <= 0"
        :aria-label="$t('notes.reorder.moveUp', [item.name])"
        icon
        x-small
        @click="$emit('move', { item, direction: -1 })">
        <v-icon size="14" aria-hidden="true">fas fa-arrow-up</v-icon>
      </v-btn>
      <v-btn
        :id="`noteMoveDown-${item.noteId}`"
        :disabled="position >= siblingsCount - 1"
        :aria-label="$t('notes.reorder.moveDown', [item.name])"
        icon
        x-small
        @click="$emit('move', { item, direction: 1 })">
        <v-icon size="14" aria-hidden="true">fas fa-arrow-down</v-icon>
      </v-btn>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    item: {
      type: Object,
      default: () => ({})
    },
    position: {
      type: Number,
      default: 0
    },
    siblingsCount: {
      type: Number,
      default: 0
    }
  }
};
</script>
