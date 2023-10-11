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
  <div>
    <rich-editor
      v-if="initialized"
      ref="notePageInlineEditor"
      v-model="pageContent"
      :placeholder="$t('notePageView.placeholder.editText')"
      :tag-enabled="false"
      :ck-editor-id="ckEditorId"
      :toolbar-position="isMobile && 'bottom' || 'top'"
      ck-editor-type="notePageInline"
      autofocus
      hide-chars-count />
    <div
      :class="{
        'r-0': !$vuetify.rtl,
        'l-0': $vuetify.rtl,
        'position-absolute z-index-two t-0': !isMobile,
      }"
      class="ms-auto me-2 my-2 d-flex align-center justify-end">
      <v-tooltip v-if="!isMobile" bottom>
        <template #activator="{on, bind}">
          <v-btn
            v-on="on"
            v-bind="bind"
            :href="fullPageEditorLink"
            :disabled="saving"
            class="me-3"
            target="_blank"
            icon
            @click="save">
            <v-icon size="20">fa-external-link-alt</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('notePageView.label.openFullPageEditor') }}</span>
      </v-tooltip>
      <v-btn
        :title="$t('notePageView.label.cancel')"
        :loading="saving"
        class="btn me-4"
        @click="$emit('cancel')">
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
  </div>
</template>
<script>
export default {
  data: () => ({
    pageContent: null,
    ckEditorId: `notePageInline${parseInt(Math.random() * 10000)}`,
    saving: false,
    initialized: false,
  }),
  computed: {
    fullPageEditorLink() {
      const formData = new FormData();
      formData.append('noteId', this.$root.page?.id);
      formData.append('parentNoteId', this.$root.page?.parentPageId);
      if (eXo.env.portal?.spaceGroup) {
        formData.append('spaceGroupId', eXo.env.portal?.spaceGroup);
      }
      formData.append('pageName', document.title);
      formData.append('isDraft', 'false');
      formData.append('showMaxWindow', 'true');
      formData.append('hideSharedLayout', 'true');
      formData.append('webPageNote', 'true');
      const urlParams = new URLSearchParams(formData).toString();
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?${urlParams}`;
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
  },
  created() {
    this.init();
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
    save(emitEvent) {
      this.saving = true;
      return this.$notePageViewService.saveNotePage(this.$root.name, this.pageContent, this.$root.language)
        .then(() => {
          this.$root.$emit('notes-refresh');
          if (emitEvent) {
            this.$emit('saved');
            this.$root.$emit('alert-message', this.$t('notePageView.label.savedSuccessfully') , 'success');
          }
        })
        .catch(() => this.$root.$emit('alert-message', this.$t('notePageView.label.errorSavingText') , 'error'))
        .finally(() => this.saving = false);
    },
  }
};
</script>