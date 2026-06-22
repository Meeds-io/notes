<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2026 Meeds Association contact@meeds.io

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
  <div class="row no-gutters align-center">
    <application-toolbar
      id="noteTreeviewToolbar"
      :right-text-filter="{
        minCharacters: 3,
        placeholder: $t('notes.label.filter'),
        tooltip: $t('notes.label.filter')
      }"
      :right-filter-button=" {
        text: $t('notes.advanced.filter.button.title'),
        displayText: false,
      }"
      :compact="compactDisplay || $root.isMobile"
      class="px-0 height-auto"
      no-text-truncate
      dense
      @filter-text-input-end-typing="$emit('keyword-changed', $event)"
      @filter-button-click="$root.$emit('notes-filter-open', filter)"
      @filter-expand="filterExpand = $event"
      @loading="$emit('loading', $event)" />
    <v-btn
      v-if="!filterExpand"
      icon
      v-bind="attrs"
      v-on="on"
      @click.stop.prevent="$root.$emit('sidebar-tree-view-expend', false)">
      <img
        alt=""
        :title="$t('notes.tooltip.close.tree')"
        src="/social/images/sidebar.svg"
        class="icon-default-color"
        height="20px"
        width="20px">
    </v-btn>
  </div>
</template>
<script>
export default {
  props: {
    filter: {
      type: String,
      default: null,
    },
    filtersCount: {
      type: Number,
      default: () => 0,
    },
    compactDisplay: {
      type: Boolean,
      default: false
    },
    filterMessage: {
      type: String,
      default: null
    },
    canCreateSpace: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    loading: 0,
    filterExpand: false,
  }),
};
</script>
