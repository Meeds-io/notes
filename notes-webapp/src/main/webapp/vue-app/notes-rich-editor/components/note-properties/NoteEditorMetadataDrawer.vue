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
  <div>
    <v-overlay
      z-index="2000"
      :value="drawer"
      @click.native="closeDrawersByOverlay" />
    <exo-drawer
      id="editorMetadataDrawer"
      ref="metadataDrawer"
      v-model="drawer"
      allow-expand
      show-overlay
      right
      @closed="resetProperties">
      <template slot="title">
        <div class="d-flex my-auto text-header font-weight-bold text-color">
          {{ $t('notes.metadata.properties.label') }}
        </div>
      </template>
      <template slot="content">
        <div class="pa-5">
          <note-metadata-properties-form
            ref="propertiesForm"
            :note="noteObject"
            :current-properties="currentNoteProperties"
            :has-featured-image="hasFeaturedImage"
            :summary-max-length="summaryMaxLength"
            @properties-updated="propertiesUpdated" />
        </div>
      </template>
      <template slot="footer">
        <div class="d-flex width-fit-content ms-auto">
          <v-btn
            class="btn me-5"
            @click="cancelChanges">
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            :disabled="saveDisabled"
            class="btn btn-primary"
            @click="save">
            {{ $t('notes.button.publish') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>

export default {
  data() {
    return {
      noteObject: null,
      drawer: false,
      currentNoteProperties: {},
      propertiesToSave: null,
      summaryMaxLength: 1300,
    };
  },
  props: {
    hasFeaturedImage: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    summaryLengthError() {
      return this.noteObject?.properties?.summary?.length > this.summaryMaxLength;
    },
    propertiesChanged() {
      return JSON.stringify(this.noteObject?.properties) !== JSON.stringify(this.currentNoteProperties);
    },
    saveDisabled() {
      return (!this.propertiesChanged && !this.imageData) || this.summaryLengthError;
    }
  },
  methods: {
    propertiesUpdated(properties) {
      if (!this.noteObject?.properties || !Object.keys(this.noteObject?.properties).length) {
        this.noteObject.properties = structuredClone(properties || {});
      }
      if (!this.noteObject.properties?.featuredImage) {
        this.noteObject.properties.featuredImage = {};
      }
      this.updateCurrentNoteObjectProperties(properties);
      this.propertiesToSave = properties;
    },
    resetProperties() {
      this.cancelChanges();
    },
    closeFeaturedImageDrawerByOverlay() {
      this.$root.$emit('close-featured-image-byOverlay');
    },
    closeDrawersByOverlay() {
      this.closeFeaturedImageDrawerByOverlay();
    },
    open(note) {
      this.noteObject = note;
      this.cloneProperties();
      this.$refs.metadataDrawer.open();
      this.$refs.propertiesForm?.initProperties();
    },
    cloneProperties() {
      this.currentNoteProperties = structuredClone(
        this.noteObject?.id &&
          this.noteObject?.properties || {
          noteId: 0,
          summary: '',
          featuredImage: {uploadId: null, mimeType: null, altText: null},
          draft: true
        });
    },
    cancelChanges() {
      this.close();
      setTimeout(() => {
        this.$refs.propertiesForm.cancelChanges();
      }, 1000);
    },
    close() {
      this.$refs.metadataDrawer.close();
    },
    save() {
      this.$emit('metadata-updated', this.propertiesToSave);
      this.close();
    },
    updateCurrentNoteObjectProperties(properties) {
      this.noteObject.properties.noteId = Number(properties.noteId);
      this.noteObject.properties.summary = properties.summary;
      this.noteObject.properties.draft = properties.draft;

      this.noteObject.properties.featuredImage.id = properties?.featuredImage?.id;
      this.noteObject.properties.featuredImage.uploadId = properties?.featuredImage?.uploadId;
      this.noteObject.properties.featuredImage.mimeType = properties?.featuredImage?.mimeType;
      this.noteObject.properties.featuredImage.altText = properties?.featuredImage?.altText;
      this.noteObject.properties.featuredImage.toDelete = properties?.featuredImage?.toDelete;
    }
  }
};
</script>
