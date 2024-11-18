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
  <v-list class="py-0 text-no-wrap width-fit-content">
    <v-list-item
      v-for="extension in filteredExtensions"
      :key="extension.id"
      :class="extension.cssClass"
      @click="handleAction(extension)">
      <v-icon
        size="16"
        :class="extension?.iconCssClass"
        class="clickable icon-menu">
        {{ extension?.icon }}
      </v-icon>
      <span class="text-color">
        {{ $t(extension.labelKey) }}
      </span>
    </v-list-item>
  </v-list>
</template>

<script>
export default {
  data() {
    return {
      extensions: []
    };
  },
  props: {
    note: {
      type: Object,
      default: () => null,
    }
  },
  computed: {
    filteredExtensions() {
      return this.extensions.filter(extension => extension.enabled(this.note));
    }
  },
  created() {
    this.refreshMenuExtensions();
  },
  methods: {
    refreshMenuExtensions() {
      this.extensions = extensionRegistry.loadExtensions('NotesMenu', 'menuActionMenu');
    },
    handleAction(extension) {
      if (extension?.action) {
        extension.action(this);
      } else {
        this.$root.$emit(extension.actionEvent, this.note, extension.id);
      }
    }
  },
};
</script>

