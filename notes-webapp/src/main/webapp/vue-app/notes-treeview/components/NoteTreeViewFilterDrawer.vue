<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <exo-drawer
    id="NoteTreeViewFilterDrawer"
    ref="drawer"
    v-model="drawer"
    allow-expand
    right>
    <template #title>
      {{ $t('notes.advanced.filter.drawer.title') }}
    </template>
    <template v-if="drawer" #content>
      <div class="d-flex flex-column">
        <v-radio-group
          v-model="filter"
          class="mt-2 ms-3"
          mandatory
          inset>
          <v-radio
            v-for="noteFilter in notesFilters"
            :key="noteFilter.value"
            :value="noteFilter.value"
            class="mt-0">
            <template #label>
              <div class="text-body">
                {{ noteFilter.text }}
              </div>
            </template>
          </v-radio>
        </v-radio-group>
      </div>
    </template>
    <template #footer>
      <div class="d-flex">
        <v-btn
          class="btn ms-auto me-2"
          @click="resetFilter()">
          <template>
            <i class="fas fa-redo me-3"></i>
            {{ $t('notes.label.resetFilter') }}
          </template>
        </v-btn>
        <v-spacer />
        <v-btn
          class="btn ms-auto me-2"
          @click="close">
          {{ $t('notes.button.cancel') }}
        </v-btn>
        <v-btn
          color="primary"
          elevation="0"
          @click="apply">
          {{ $t('notes.button.apply') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    drawer: false,
    filter: null,
  }),
  computed: {
    notesFilters() {
      return [{
        text: this.$t('notes.filter.label.published.notes'),
        value: 'published',
      },{
        text: this.$t('notes.filter.label.drafts'),
        value: 'drafts',
      }];
    },
  },
  created() {
    this.$root.$on('notes-filter-open', this.open);
  },
  beforeDestroy() {
    this.$root.$off('notes-filter-open', this.open);
  },
  methods: {
    open(filter) {
      this.filter = filter;
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    apply() {
      this.$root.$emit('notes-filter-update', this.filter);
      this.close();
    },
    resetFilter() {
      this.filter = 'published';
      this.apply();
    }
  },
};
</script>