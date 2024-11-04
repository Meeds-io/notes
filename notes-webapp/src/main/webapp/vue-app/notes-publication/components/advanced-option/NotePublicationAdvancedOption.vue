<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2024 Meeds Association contact@meeds.io

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
  <div class="mt-8">
    <p class="mb-0 text-truncate text-color font-weight-bold">
      {{ $t('notes.publication.advanced.option.label') }}
    </p>
    <div class="d-flex mt-4">
      <v-switch
        v-model="hideAuthor"
        :disabled="isPublishing"
        :aria-label="$t('notes.publication.hide.author.label')"
        :ripple="false"
        color="primary"
        class="mt-n1 me-1" />
      <p class="mb-5">
        {{ $t('notes.publication.hide.author.label') }}
      </p>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      hideAuthor: false
    };
  },
  props: {
    isPublishing: {
      type: Boolean,
      default: false
    },
    savedAdvancedSettings: {
      type: Object,
      default: null
    },
    editMode: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    hideAuthor() {
      this.emitAdvancedSettingsUpdate();
    }
  },
  created() {
    this.initSettings();
  },
  methods: {
    emitAdvancedSettingsUpdate() {
      this.$emit('update', {
        hideAuthor: this.hideAuthor
      });
    },
    initSettings() {
      if (!this.editMode) {
        return;
      }
      this.hideAuthor = this.savedAdvancedSettings?.hideAuthor;
    },
    cancelChanges() {
      this.initSettings();
    }
  }
};
</script>
