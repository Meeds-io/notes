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
  <v-menu
    v-if="translations.length > 1 || menuExtensions.length"
    v-model="translationsMenu"
    offset-y
    bottom>
    <template #activator="{ on, attrs }">
      <v-icon
        size="22"
        :class="langButtonColor"
        class="remove-focus mt-n2 pa-0  ms-5"
        v-bind="attrs"
        v-on="on">
        fa-language
      </v-icon>
    </template>
    <v-list class="px-2" dense>
      <v-list-item
        v-for="(item, i) in translations"
        :key="i"
        class="translation-chips">
        <v-chip
          small
          :outlined="item.value!==selectedTranslation.value"
          color="primary"
          close-label="translation remove button"
          @click="changeTranslation(item)"
          class="my-auto mx-1">
          {{ item.text }}
        </v-chip>
      </v-list-item>
      <v-list-item
        v-for="(extension, i) in menuExtensions"
        :key="i"
        class="translation-chips">
        <extension-registry-component
          :component="extension"
          :params="params"
          element="div" />
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script>
export default {
  data() {
    return {
      menuExtensions: null,
      translationsMenu: false,
    };
  },
  props: {
    note: {
      type: Object,
      default: () => {
        return {};
      }
    },
    translations: {
      type: Array,
      default: () => {
        return [];
      }
    },
    selectedTranslation: {
      type: Object,
      default: () => {
        return {};
      }
    }
  },
  computed: {
    params() {
      return {
        note: this.note,
        selectedTranslation: this.selectedTranslation,
      };
    },
    langButtonColor(){
      return this.selectedTranslation.value ? 'primary--text':'';
    },
  },
  created() {
    this.refreshTranslationExtensions();
    document.addEventListener('automatic-translation-extensions-updated', () => {
      this.refreshTranslationExtensions();
    });
  },
  mounted() {
    $(document).on('click', () => {
      this.translationsMenu = false;
    });
  },
  methods: {
    refreshTranslationExtensions() {
      this.menuExtensions = extensionRegistry.loadExtensions('notes', 'translation-menu-extension');
    },
    changeTranslation(item) {
      this.$emit('change-translation', item);
    }
  }
};
</script>
