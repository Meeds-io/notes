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
      @keydown.native="handleKeydown" />
  </v-menu>
</template>
<script>
export default {
  data() {
    return {
      displayActionMenu: false,
      waitTimeUntilCloseMenu: 200
    };
  },
  props: {
    note: {
      type: Object,
      default: () => null,
    }
  },
  watch: {
    displayActionMenu() {
      this.$root.$emit('action-menu-state-changed', this.displayActionMenu);
      if (this.displayActionMenu) {
        this.focusMenuItem(0);
      } else {
        this.restoreFocus();
      }
    }
  },
  created() {
    $(document).on('mousedown', event => {
      if (this.displayActionMenu && !$(event.target).closest('.note-actions-menu').length) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    this.$root.$on('display-action-menu', ( )=> {
      this.displayActionMenu = true;
    });
  },
  methods: {
    getMenuItems() {
      return Array.from(document.querySelectorAll('.note-actions-menu [role="menuitem"]'));
    },
    focusMenuItem(index, attempt = 0) {
      const item = this.getMenuItems()[index];
      if (item) {
        item.focus();
      }
      if (document.activeElement !== item && attempt < 10) {
        window.requestAnimationFrame(() => this.focusMenuItem(index, attempt + 1));
      }
    },
    restoreFocus() {
      const active = document.activeElement;
      if (!active || active === document.body || active.closest('.note-actions-menu')) {
        const button = document.getElementById('noteActionsMenuButton');
        if (button) {
          button.focus();
        }
      }
    },
    handleKeydown(event) {
      const items = this.getMenuItems();
      const current = items.indexOf(document.activeElement);
      let next;
      if (event.key === 'ArrowDown') {
        next = (current + 1) % items.length;
      } else if (event.key === 'ArrowUp') {
        next = current <= 0 ? items.length - 1 : current - 1;
      } else if (event.key === 'Home') {
        next = 0;
      } else if (event.key === 'End') {
        next = items.length - 1;
      } else {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
      if (items[next]) {
        items[next].focus();
      }
    }
  }
};
</script>
