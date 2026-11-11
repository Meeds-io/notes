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
      right
      @expand-updated="expanded = $event"
      @closed="resetProperties">
      <template slot="title">
        <div class="d-flex my-auto text-header font-weight-bold text-color">
          {{ $t('notes.metadata.properties.label') }}
        </div>
      </template>
      <template slot="content">
        <div
          class="pa-5"
          :class="{'d-flex flex-wrap': expanded}">
          <div
            id="notePropertiesMainPane"
            :class="{'col-12 col-md-4 pa-0 pe-md-5': expanded}">
            <note-metadata-properties-form
              ref="propertiesForm"
              :note="noteObject"
              :current-properties="currentNoteProperties"
              :has-featured-image="hasFeaturedImage"
              :summary-max-length="summaryMaxLength"
              :read-only="readOnly"
              @properties-updated="propertiesUpdated" />
          </div>
          <div
            id="notePropertiesSectionsPane"
            :class="{'col-12 col-md-8 pa-0': expanded}">
            <div id="notePropertiesDrawerSections">
              <extension-registry-components
                v-if="noteObject"
                name="NotesPropertiesDrawer"
                type="notes-properties-sections"
                :params="sectionsParams" />
            </div>
          </div>
        </div>
      </template>
      <template
        v-if="!readOnly"
        slot="footer">
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
            {{ $t('notes.button.save') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
import {newNoteCreation} from '../../js/NoteCreation.js';

export default {
  data() {
    return {
      noteObject: null,
      drawer: false,
      expanded: false,
      currentNoteProperties: {},
      propertiesToSave: null,
      summaryMaxLength: 1300,
      creation: newNoteCreation(),
      createdPageId: null,
    };
  },
  props: {
    hasFeaturedImage: {
      type: Boolean,
      default: false
    },
    readOnly: {
      type: Boolean,
      default: false
    }
  },
  computed: {
    pageId() {
      const pageId = this.noteObject?.draftPage ? this.noteObject?.targetPageId : this.noteObject?.id;
      return pageId || this.createdPageId || null;
    },
    sectionsParams() {
      return {
        note: this.noteObject,
        pageId: this.pageId,
        parentPageId: this.noteObject?.parentPageId || null,
        lang: this.noteObject?.lang || null,
        editable: !this.readOnly,
        expanded: this.expanded,
        creation: this.creation,
      };
    },
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
      // a copy: the reset on close would otherwise revert the caller's note after it saved
      this.noteObject = structuredClone(note);
      this.createdPageId = null;
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
    /**
     * Seals the new note's creation while the note is being created, and has
     * its sections check what they hold; a refused value never blocks the
     * creation, it stays on its field.
     *
     * @returns {void}
     */
    startCreation() {
      this.creation.seal();
      this.creation.validate();
    },
    /**
     * Has the sections write what they hold on the note just created, then
     * shows the note's own values in the drawer.
     *
     * @param {string|number} noteId the page id of the note just created
     * @returns {Promise<Array<object>>} what the sections could not write, each with its label
     */
    commitCreation(noteId) {
      const creation = this.creation;
      return creation.commit(noteId)
        .then(notSaved => {
          this.createdPageId = noteId;
          return notSaved;
        })
        .finally(() => creation.unseal());
    },
    /**
     * Unseals the creation when the note could not be created: the sections
     * keep what they hold for the next attempt.
     *
     * @returns {void}
     */
    abortCreation() {
      this.creation.unseal();
    },
    /**
     * Gives back what the sections hold for a new note that is dropped, and
     * starts a fresh creation for the next one.
     *
     * @returns {void}
     */
    disposeCreation() {
      this.creation.dispose();
      this.creation = newNoteCreation();
      this.createdPageId = null;
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
