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
  <v-app
    v-if="canView">
    <v-hover v-slot="{ hover }">
      <v-card
        :class="{
          'pa-5': viewMode,
          'overflow-hidden': edit,
        }"
        min-width="100%"
        max-width="100%"
        min-height="60"
        class="d-flex flex-column border-box-sizing position-relative card-border-radius"
        color="white"
        flat>
        <template v-if="edit">
          <note-page-edit-drawer
            v-if="$root.isMobile"
            ref="drawer"
            @saved="closeEditor"
            @cancel="closeEditor" />
          <note-page-edit
            v-else
            ref="editor"
            :class="editorBackgroundLoading && 'position-absolute l-0 r-0'"
            :style="editorBackgroundLoading && 'z-index: -1;'"
            class="full-width"
            @saved="closeEditor"
            @cancel="closeEditor" />
        </template>
        <template v-if="!hideViewMode">
          <note-page-header
            v-if="displayEditMode"
            :hover="hover || editorLoading"
            :loading="editorLoading"
            @edit="openEditor" />
          <note-page-view
            v-if="hasNote"
            :class="editorLoading && 'opacity-8 filter-blur-1'"
            class="full-width overflow-hidden pa-1" />
        </template>
      </v-card>
    </v-hover>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    edit: false,
    editorReady: false,
    previewMode: false,
  }),
  computed: {
    hasNote() {
      return !!this.$root.pageContent;
    },
    displayEditMode() {
      return this.$root.initialized && this.canEdit;
    },
    canView() {
      return this.canEdit || (this.$root.initialized && this.hasNote);
    },
    canEdit() {
      return !this.previewMode && this.$root.canEdit;
    },
    viewMode() {
      return this.$root.isMobile || (!this.edit || this.editorBackgroundLoading);
    },
    hideViewMode() {
      return !this.$root.isMobile && this.editorReady && this.edit;
    },
    editorBackgroundLoading() {
      return !this.$root.isMobile && this.editorLoading && this.hasNote;
    },
    editorLoading() {
      return this.edit && !this.editorReady;
    },
  },
  watch: {
    edit() {
      if (this.edit) {
        window.editNoteInProgress = true;
        this.$root.$emit('close-alert-message');
      } else {
        window.editNoteInProgress = false;
      }
    },
    canView() {
      if (this.canView) {
        this.$el.parentElement.closest('.PORTLET-FRAGMENT').classList.remove('hidden');
      } else {
        this.$el.parentElement.closest('.PORTLET-FRAGMENT').classList.add('hidden');
      }
    }
  },
  created() {
    document.addEventListener('cms-preview-mode', this.switchToPreview);
    document.addEventListener('cms-edit-mode', this.switchToEdit);
    this.$root.$on('notes-editor-ready', this.setEditorReady);
    this.$root.$on('notes-editor-unloaded', this.setEditorNotReady);
  },
  beforeDestroy() {
    document.removeEventListener('cms-edit-mode', this.switchToEdit);
    document.removeEventListener('cms-preview-mode', this.switchToPreview);
    this.$root.$off('notes-editor-ready', this.setEditorReady);
    this.$root.$off('notes-editor-unloaded', this.setEditorNotReady);
  },
  methods: {
    openEditor() {
      if (window.editNoteInProgress) {
        this.$root.$emit('alert-message', this.$t('notePageView.label.warningCannotEditTwoNotes'), 'warning');
      } else {
        this.editorReady = false;
        this.$nextTick().then(() => this.edit = true);
      }
    },
    closeEditor() {
      this.editorReady = false;
      this.$nextTick().then(() => this.edit = false);
    },
    setEditorReady() {
      window.setTimeout(() => {
        this.editorReady = true;
      }, 50);
    },
    setEditorNotReady() {
      window.setTimeout(() => {
        this.editorReady = false;
      }, 50);
    },
    switchToPreview() {
      this.previewMode = true;
    },
    switchToEdit() {
      this.previewMode = false;
    },
  },
};
</script>
