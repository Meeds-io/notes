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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    :right="!$vuetify.rtl"
    class="linkSettingDrawer"
    eager
    @opened="stepper = 1"
    @closed="reset"
    @expand-updated="expanded = $event">
    <template #content>
      <rich-editor
        ref="richEditor"
        v-model="pageContent"
        :placeholder="$t('notePageView.placeholder.editText')"
        :tag-enabled="false"
        :ck-editor-id="richEditorId"
        class="ma-4"
        ck-editor-type="notePageInline"
        toolbar-position="bottom"
        focus-position="end"
        autofocus
        hide-chars-count
        disable-auto-grow
        oembed
        @ready="$root.$emit('notes-editor-ready')"
        @unloaded="$root.$emit('notes-editor-unloaded')" />
    </template>
    <template #footer>
      <div class="d-flex align-center justify-end">
        <v-btn
          :title="$t('notePageView.label.cancel')"
          :loading="saving"
          class="btn me-4"
          @click="cancel">
          {{ $t('notePageView.label.cancel') }}
        </v-btn>
        <v-btn
          :title="$t('notePageView.label.save')"
          :loading="saving"
          class="btn primary me-2"
          @click="save(true)">
          {{ $t('notePageView.label.save') }}
        </v-btn>
      </div>
    </template>
  </exo-drawer>
</template>
<script>
export default {
  data: () => ({
    pageContent: null,
    richEditorId: `notePageInline${parseInt(Math.random() * 10000)}`,
    saving: false,
    initialized: false,
  }),
  computed: {
    note() {
      return this.$root.page;
    },
  },
  created() {
    this.init();
  },
  mounted() {
    this.$refs?.drawer?.open?.();
  },
  methods: {
    init() {
      this.pageContent = this.$root.pageContent || '';
      if (this.$root.pageId) {
        this.initialized = true;
      } else {
        return this.save()
          .finally(() => this.initialized = true);
      }
    },
    cancel() {
      this.$refs.drawer.close();
      this.$nextTick().then(() => this.$emit('cancel'));
    },
    save(emitEvent) {
      this.saving = true;
      return this.$notePageViewService.saveNotePage(this.$root.name, this.pageContent, this.$root.language)
        .then(() => {
          this.$root.$emit('notes-refresh');
          if (emitEvent) {
            this.$refs.drawer.close();
            this.$nextTick().then(() => this.$emit('saved'));
            this.$root.$emit('alert-message', this.$t('notePageView.label.savedSuccessfully') , 'success');
          }
        })
        .catch(() => {
          if (emitEvent) {
            this.$root.$emit('alert-message', this.$t('notePageView.label.errorSavingText') , 'error')
          }
        })
        .finally(() => this.saving = false);
    },
  }
};
</script>