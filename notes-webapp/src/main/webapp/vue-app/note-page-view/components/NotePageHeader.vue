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
  <div
    v-if="hoverEdit"
    :class="{
      'r-0': !$vuetify.rtl,
      'l-0': $vuetify.rtl,
    }"
    class="position-absolute z-index-two t-0 d-flex align-center">
    <v-menu
      v-model="menu"
      :left="!$vuetify.rtl"
      :right="$vuetify.rtl"
      :disabled="!hover"
      bottom
      offset-y
      attach>
      <template #activator="{ on, attrs }">
        <v-btn
          :loading="loading"
          :elevation="loading && 2 || 0"
          small
          icon
          class="ma-1"
          v-bind="attrs"
          v-on="on">
          <v-icon size="16" class="icon-default-color">fas fa-ellipsis-v</v-icon>
        </v-btn>
      </template>
      <v-list dense class="white pa-0">
        <v-list-item
          dense
          class="ps-0 pe-4"
          @click="$emit('edit')">
          <v-list-item-icon class="justify-center mx-1">
            <v-icon size="13" class="icon-default-color">fa-edit</v-icon>
          </v-list-item-icon>
          <v-list-item-title class="text-start pl-0">{{ $t('notePageView.label.edit') }}</v-list-item-title>
        </v-list-item>
        <v-list-item
          :loading="saving"
          dense
          class="ps-0 pe-4"
          @click="remove">
          <v-list-item-icon class="justify-center mx-1">
            <v-icon size="13" class="icon-default-color">fa-trash</v-icon>
          </v-list-item-icon>
          <v-list-item-title class="text-start pl-0">{{ $t('notePageView.label.remove') }}</v-list-item-title>
        </v-list-item>
      </v-list>
    </v-menu>
  </div>
  <div v-else-if="!hasNote && !loading" class="d-flex full-width align-center justify-center">
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
    hover: {
      type: Boolean,
      default: false,
    },
    loading: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    menu: false,
    saving: false,
  }),
  computed: {
    hasNote() {
      return !!this.$root.pageContent;
    },
    hoverEdit() {
      return this.hasNote && this.hover;
    },
  },
  watch: {
    hover() {
      if (!this.hover && this.menu) {
        this.menu = false;
      }
    },
  },
  methods: {
    remove() {
      this.saving = true;
      return this.$notePageViewService.saveNotePage(this.$root.name, '', this.$root.language)
        .then(() => {
          this.$root.$emit('notes-refresh');
          this.$emit('removed');
          this.$root.$emit('alert-message', this.$t('notePageView.label.removedSuccessfully') , 'success');
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('notePageView.label.errorRemovingText') , 'error'))
        .finally(() => this.saving = false);
    },
  }
};
</script>