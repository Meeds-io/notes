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
  <div v-if="hoverEdit" class="d-flex align-center">
    <v-btn
      :title="$t('notePageView.label.editText')"
      :class="{
        'r-0': !$vuetify.rtl,
        'l-0': $vuetify.rtl,
      }"
      class="position-absolute z-index-two t-0"
      small
      icon
      @click="$emit('edit')">
      <v-icon size="18">fa-edit</v-icon>
    </v-btn>
  </div>
  <div v-else-if="!hasPage && canEdit" class="d-flex align-center justify-center">
    <v-btn
      class="primary"
      elevation="0"
      outlined
      border
      @click="$emit('edit')">
      {{ $t('notePageView.label.addTextButton') }}
    </v-btn>
  </div>
</template>
<script>
export default {
  props: {
    canEdit: {
      type: Boolean,
      default: false,
    },
    hasPage: {
      type: Boolean,
      default: false,
    },
    hover: {
      type: Boolean,
      default: false,
    },
  },
  computed: {
    hoverEdit() {
      return this.hasPage && this.canEdit && this.hover;
    },
  },
};
</script>