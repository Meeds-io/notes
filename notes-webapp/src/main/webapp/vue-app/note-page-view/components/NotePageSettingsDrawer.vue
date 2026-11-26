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
  <exo-drawer
    ref="drawer"
    class="note-page-settings-drawer"
    right>
    <template #title>
      {{ $t('notePageView.label.settingsTitle') }}
    </template>
    <template #content>
      <div class="d-flex align-center pa-4">
        <div class="d-flex align-center flex-grow-1 flex-shrink-1 text-truncate text-color">
          {{ $t('notePageView.label.allowHide') }}
        </div>
        <v-switch
          v-model="allowHide"
          :aria-label="$t('notePageView.label.allowHide')"
          class="note-page-allow-hide-switch my-0 me-n2"
          dense
          hide-details />
      </div>
    </template>
    <template #footer>
      <div class="d-flex justify-end">
        <v-btn
          :disabled="saving"
          class="btn me-2"
          @click="close">
          {{ $t('notePageView.label.cancel') }}
        </v-btn>
        <v-btn
          :loading="saving"
          :disabled="!modified"
          class="btn btn-primary"
          @click="save">
          {{ $t('notePageView.label.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    allowHide: false,
    saving: false,
  }),
  computed: {
    modified() {
      return this.allowHide !== !!this.$root.allowHide;
    },
  },
  methods: {
    open() {
      this.allowHide = !!this.$root.allowHide;
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    save() {
      this.saving = true;
      const allowHide = this.allowHide;
      return this.$notePageViewService.saveHideAllowed(this.$root.name, allowHide)
        .then(() => {
          // Turned off, the option shows the block again to every user
          this.$root.hidden = allowHide && this.$root.hidden;
          this.$root.allowHide = allowHide;
          this.close();
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('notePageView.label.errorSavingSettings'), 'error'))
        .finally(() => this.saving = false);
    },
  },
};
</script>
