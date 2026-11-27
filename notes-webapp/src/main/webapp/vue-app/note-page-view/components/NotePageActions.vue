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
  <div
    v-if="canEdit || $root.allowHide"
    :class="{
      'r-0': !$vuetify.rtl,
      'l-0': $vuetify.rtl,
    }"
    class="note-page-actions position-absolute z-index-two t-0 d-flex align-center">
    <!-- always rendered for an editor, so the keyboard reaches it: hidden until the block is hovered or has the focus -->
    <v-btn
      v-if="canEdit"
      :title="$t('notePageView.label.openSettings')"
      :aria-label="$t('notePageView.label.openSettings')"
      :class="!hover && 'd-sr-only-focusable'"
      class="note-page-settings-button ma-1"
      small
      icon
      @click="$emit('open-settings')">
      <v-icon size="16" class="icon-default-color">fas fa-cog</v-icon>
    </v-btn>
    <hide-application-button
      v-if="$root.allowHide"
      :name="$root.name"
      :label="$t('notePageView.label.hideBlock')"
      :error-label="$t('notePageView.label.hideError')"
      type="notePage"
      class="note-page-hide-button ma-1"
      @hidden="$root.hidden = true" />
  </div>
</template>
<script>
export default {
  props: {
    canEdit: {
      type: Boolean,
      default: false,
    },
    hover: {
      type: Boolean,
      default: false,
    },
  },
};
</script>
