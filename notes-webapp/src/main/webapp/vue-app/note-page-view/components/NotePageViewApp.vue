<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

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
  <v-app v-if="canView">
    <v-hover v-slot="{ hover }">
      <v-card
        :class="!edit && 'pa-4'"
        min-width="100%"
        max-width="100%"
        min-height="72"
        class="d-flex flex-column border-box-sizing position-relative"
        color="white"
        flat>
        <note-page-edit
          v-if="edit"
          class="full-width"
          @saved="edit = false"
          @cancel="edit = false" />
        <template v-else>
          <note-page-header
            v-if="$root.initialized && canEdit"
            :hover="hover"
            @edit="edit = true" />
          <note-page-view
            v-if="hasNote"
            class="full-width overflow-hidden" />
        </template>
      </v-card>
    </v-hover>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    edit: false,
  }),
  computed: {
    hasNote() {
      return !!this.$root.pageContent;
    },
    canEdit() {
      return this.$root.canEdit;
    },
    canView() {
      return this.$root.canEdit || (this.$root.initialized && this.hasNote);
    },
  },
  watch: {
    edit() {
      if (this.edit) {
        this.$root.$emit('close-alert-message');
      }
    },
  },
};
</script>
