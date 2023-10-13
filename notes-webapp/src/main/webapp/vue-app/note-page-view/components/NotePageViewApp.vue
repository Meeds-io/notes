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
    v-if="canView"
    :class="edit && 'overflow-hidden'">
    <v-hover v-slot="{ hover }">
      <v-card
        :class="viewMode && 'pa-5'"
        min-width="100%"
        max-width="100%"
        min-height="72"
        class="d-flex flex-column border-box-sizing position-relative card-border-radius"
        color="white"
        flat>
        <note-page-edit
          v-if="edit"
          ref="editor"
          :class="editorBackgroundLoading && 'position-absolute l-0 r-0'"
          :style="editorBackgroundLoading && 'z-index: -1;'"
          class="full-width"
          @saved="closeEditor"
          @cancel="closeEditor" />
        <template v-if="!editorReady || !edit">
          <note-page-header
            v-if="$root.initialized && canEdit"
            :hover="hover || editorLoading"
            :loading="editorLoading"
            @edit="openEditor" />
          <note-page-view
            v-if="hasNote"
            class="full-width overflow-hidden" />
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
  }),
  computed: {
    hasNote() {
      return !!this.$root.pageContent;
    },
    canEdit() {
      return this.$root.canEdit;
    },
    canView() {
      return this.$root.canEdit || (this.$root.initialized && this.hasNote);
    },
    viewMode() {
      return !this.edit || this.editorBackgroundLoading;
    },
    editorBackgroundLoading() {
      return this.editorLoading && this.hasNote;
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
  },
  created() {
    this.$root.$on('notes-editor-ready', this.setEditorReady);
    this.$root.$on('notes-editor-unloaded', this.setEditorNotReady);
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
  },
};
</script>
