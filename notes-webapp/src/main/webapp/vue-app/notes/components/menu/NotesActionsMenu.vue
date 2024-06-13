<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 -->

<template>
  <v-menu
    v-model="displayActionMenu"
    :attach="'#note-actions-menu'"
    transition="slide-x-reverse-transition"
    content-class="py-0 note-actions-menu pa-0 overflow-hidden"
    max-width="fit-content"
    max-height="fit-content"
    offset-y
    left>
    <notes-action-menu-items
      :note="note"
      :default-path="defaultPath" />
  </v-menu>
</template>
<script>
export default {
  data() {
    return {
      displayActionMenu: false,
    };
  },
  props: {
    note: {
      type: Object,
      default: () => null,
    },
    defaultPath: {
      type: String,
      default: () => 'Home',
    }
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    this.$root.$on('display-action-menu', ( )=> {
      this.displayActionMenu = true;
    });
  }
};
</script>
