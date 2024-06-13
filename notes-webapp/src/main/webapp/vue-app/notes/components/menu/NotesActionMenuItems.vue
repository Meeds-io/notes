<!--
 * This file is part of the Meeds project (https://meeds.io/).
 *
 * Copyright (C) 2024 Meeds Association contact@meeds.io
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 -->

<template>
  <v-list class="py-0 text-no-wrap width-fit-content">
    <v-list-item
      v-if="note?.canView"
      class="ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="copyLink">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-link
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.copyLink') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="note?.canView"
      class="ps-2 pe-4 py-1 noteExportPdf action-menu-item draftButton"
      @click="exportPdf">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-file-pdf
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.exportPdf') }}
      </span>
    </v-list-item>
    <v-list-item
      class="ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="openNoteHistory">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-history
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.noteHistory') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="!homePage && note.canManage"
      class="ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="openTreeView('movePage')">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-arrows-alt
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.movePage') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="homePage"
      class="ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="openTreeView('exportNotes')">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-sign-in-alt
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.export') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="homePage && note?.canImport"
      class="ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="openImportDrawer">
      <v-icon
        size="12"
        class="clickable icon-menu">
        fas fa-sign-out-alt
      </v-icon>
      <span class="text-color">
        {{ $t('notes.menu.label.import') }}
      </span>
    </v-list-item>
    <v-list-item
      v-if="!homePage && note?.canManage"
      class="red--text ps-2 pe-4 py-1 action-menu-item draftButton"
      @click="deleteNote">
      <v-icon
        size="12"
        class="delete-option-color clickable icon-menu">
        fas fa-trash
      </v-icon>
      <span class="delete-option-color">
        {{ $t('notes.menu.label.delete') }}
      </span>
    </v-list-item>
  </v-list>
</template>

<script>
export default {
  props: {
    note: {
      type: Object,
      default: () => null,
    },
    defaultPath: {
      type: String,
      default: () => 'Home',
    }
  },
  computed: {
    homePage(){
      return !this.note.parentPageId;
    }
  },
  methods: {
    exportPdf() {
      this.$root.$emit('note-export-pdf', this.note);
    },
    openTreeView(action) {
      this.$root.$emit('open-note-treeview', this.note, action);
    },
    openNoteHistory() {
      this.$root.$emit('open-note-history', this.note);
    },
    openImportDrawer() {
      this.$root.$emit('open-note-import-drawer');
    },
    deleteNote() {
      this.$root.$emit('delete-note', this.note);
    },
    copyLink() {
      const inputTemp = $('<input>');
      const path = window.location.href;
      $('body').append(inputTemp);
      inputTemp.val(path).select();
      document.execCommand('copy');
      inputTemp.remove();
      this.$root.$emit('show-alert', {type: 'success',message: this.$t('notes.alert.success.label.linkCopied')});
      this.$root.$emit('close-action-menu');
    }
  },
};
</script>

