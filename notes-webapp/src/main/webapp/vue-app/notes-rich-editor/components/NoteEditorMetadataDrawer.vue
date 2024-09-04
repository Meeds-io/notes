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
          <v-form>
            <label for="image-area">
              <p class="text-color text-body mb-3">
                {{ $t('notes.metadata.featuredImage.label') }}
              </p>
              <v-file-input
                id="featuredImageInput"
                ref="featuredImageInput"
                accept="image/*"
                class="position-absolute hidden"
                @change="handleUpload" />
              <v-btn
                v-if="!canShowFeaturedImagePreview"
                name="image-area"
                class="btn add-image-area d-flex"
                height="206"
                width="100%"
                text
                @click="uploadFeaturedImage">
                <div class="d-flex width-fit-content mx-auto">
                  <v-icon
                    class="me-15 icon-default-color"
                    size="40">
                    fas fa-image
                  </v-icon>
                  <p class="text-header text-color my-auto">
                    {{ $t('notes.metadata.featuredImage.add.label') }}
                  </p>
                </div>
              </v-btn>
              <v-sheet
                v-else
                height="206"
                min-width="48"
                class="card-border-radius image-preview">
                <v-hover v-slot="{ hover }">
                  <div class="d-flex full-height">
                    <v-img
                      width="100%"
                      contain
                      :class="{'image-pre-upload': isUploading}"
                      :lazy-src="featuredImageLink"
                      :alt="savedFeaturedImageAltText"
                      :src="featuredImageLink">
                      <v-row
                        v-if="isUploading"
                        class="fill-height ma-0"
                        align="center"
                        justify="center">
                        <v-progress-circular
                          indeterminate
                          color="grey lighten-5" />
                      </v-row>
                      <div
                        v-if="hover && canShowFeaturedImagePreview"
                        class="width-fit-content full-height ms-auto d-flex me-2">
                        <v-btn
                          class="feature-image-button me-1 mt-2 mb-auto"
                          icon
                          @click.stop="removeNoteFeaturedImage">
                          <v-icon
                            class="feature-image-trash-icon"
                            size="20">
                            fa-solid fa-trash
                          </v-icon>
                        </v-btn>
                        <v-btn
                          class="feature-image-button mt-2 mb-auto"
                          icon
                          @click="uploadFeaturedImage">
                          <v-icon
                            class="feature-image-file-icon"
                            size="20">
                            fa-regular fa-file-image
                          </v-icon>
                        </v-btn>
                      </div>
                    </v-img>
                  </div>
                </v-hover>
              </v-sheet>
            </label>
            <label for="summaryInputEditor">
              <div class="mt-5">
                <p class="text-color text-body mb-3">
                  {{ $t('notes.metadata.summary.label') }}
                </p>
                <v-textarea
                  v-model="summaryContent"
                  name="summaryInputEditor"
                  class="summary-metadata-input pt-0 overflow-auto"
                  :placeholder="$t('notes.metadata.add.summary.placeholder')"
                  rows="17"
                  row-height="8"
                  dense
                  outlined
                  auto-grow />
              </div>
            </label>
          </v-form>
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
      maxFileSize: 20 * 1024 * 1024,
      noteObject: null,
      drawer: false,
      summaryContent: null,
      imageData: null,
      uploadId: null,
      mimeType: null,
      featuredImageAltText: null,
      hasFeaturedImageValue: false,
      featureImageUpdated: false,
      illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`,
      currentNoteProperties: {},
      removeFeaturedImage: false,
      isUploading: false
    };
  },
  props: {
    hasFeaturedImage: {
      type: Boolean,
      default: false
    }
  },
  created() {
    this.$root.$on('image-data', this.imageDataUpdated);
    this.$root.$on('image-uploaded', (uploadId) => this.uploadId = uploadId);
    this.$root.$on('image-alt-text', (altText) => this.featuredImageAltText = altText);
  },
  computed: {
    savedFeaturedImageAltText() {
      return this.noteObject?.properties.featuredImage?.featuredImageAltText;
    },
    saveDisabled() {
      return (!this.propertiesChanged && !this.imageData && !this.removeFeaturedImage) || this.isUploading;
    },
    propertiesChanged() {
      return JSON.stringify(this.noteObject?.properties || {}) !== JSON.stringify(this.currentNoteProperties || {});
    },
    canShowFeaturedImagePreview() {
      return this.hasFeaturedImageValue || this.imageData?.length;
    },
    notedId() {
      return this.noteObject?.id;
    },
    langParam() {
      return this.noteObject?.lang && `&lang=${this.noteObject?.lang}` || '';
    },
    isDraft() {
      return this.noteObject?.draftPage;
    },
    noteFeatureImageUpdatedDate() {
      return this.noteObject?.properties?.featuredImage?.lastUpdated || 0;
    },
    featuredImageLink() {
      return this.imageData || this.hasFeaturedImageValue
                            && `${this.illustrationBaseUrl}${this.notedId}?v=${this.noteFeatureImageUpdatedDate}&isDraft=${this.isDraft}${this.langParam}` || '';
    }
  },
  watch: {
    'noteObject.lang': function () {
      this.imageData = null;
    },
    hasFeaturedImage() {
      this.hasFeaturedImageValue = this.hasFeaturedImage;
    },
    imageData() {
      if (!this.imageData?.length) {
        this.$root.$emit('reset-featured-image-data');
      }
      this.featureImageUpdated = true;
    },
    summaryContent() {
      if (!this.noteObject?.properties) {
        this.noteObject.properties = {};
      }
      this.noteObject.properties.summary = this.summaryContent;
    }
  },
  methods: {
    displayMessage(message) {
      document.dispatchEvent(new CustomEvent('alert-message', {
        detail: {
          alertType: message.type,
          alertMessage: message.text
        }
      }));
    },
    uploadFeaturedImage() {
      document.getElementById('featuredImageInput').click();
    },
    handleUpload(image) {
      if (!image) {
        return;
      }
      if (image.size > this.maxFileSize) {
        this.displayMessage({
          type: 'error',
          text: this.$t('notes.featuredImage.size.error.message')
        });
        return;
      }
      this.isUploading = true;
      const reader = new FileReader();
      reader.onload = e => {
        this.imageData = e.target.result
                        && e.target.result.length > 23
                        && e.target.result || null;
        this.mimeType = image.type;
      };
      reader.readAsDataURL(image);
      this.$uploadService.upload(image).then(uploadId => {
        this.uploadId = uploadId;
        this.removeFeaturedImage = false;
        this.controlUploadStatus(uploadId);
      });
    },
    controlUploadStatus(uploadId) {
      window.setTimeout(() => {
        this.$uploadService.getUploadProgress(uploadId).then(percent => {
          if (Number(percent) < 100) {
            this.controlUploadStatus(uploadId);
          } else {
            this.isUploading = false;
          }
        });
      }, 200);
    },
    resetProperties() {
      this.featureImageUpdated = false;
      this.cancelChanges();
    },
    imageDataUpdated(data, mimeType) {
      this.imageData = data;
      this.mimeType = mimeType;
    },
    openFeaturedImageDrawer() {
      this.$root.$emit('open-featured-image-drawer', {altText: this.savedFeaturedImageAltText});
    },
    closeFeaturedImageDrawerByOverlay() {
      this.$root.$emit('close-featured-image-byOverlay');
    },
    closeDrawersByOverlay() {
      this.closeFeaturedImageDrawerByOverlay();
    },
    open(note) {
      this.noteObject = note;
      this.currentNoteProperties = structuredClone(this.noteObject?.properties || {});
      this.summaryContent = this.currentNoteProperties?.summary || '';
      this.removeFeaturedImage = false;
      this.$refs.metadataDrawer.open();
    },
    cancelChanges() {
      this.noteObject.properties = structuredClone(this.currentNoteProperties || {});
      console.log(this.noteObject?.properties);
      this.summaryContent = this.currentNoteProperties?.summary || '';
      this.hasFeaturedImageValue = this.hasFeaturedImage;
      this.imageData = null;
      this.close();
    },
    close() {
      this.$refs.metadataDrawer.close();
    },
    save() {
      const savedFeaturedImageId = this.noteObject?.properties?.featuredImage?.id;
      const properties = {
        noteId: this.isDraft && this.noteObject?.targetPageId
            || this.noteObject?.id,
        summary: this.summaryContent,
        featuredImage: {
          id: savedFeaturedImageId,
          uploadId: this.uploadId,
          mimeType: this.mimeType,
          altText: this.featuredImageAltText,
          toDelete: this.removeFeaturedImage || (!this.uploadId && !savedFeaturedImageId)
        },
        draft: this.isDraft || !this.notedId
      };
      this.$emit('metadata-updated', properties);
      this.close();
    },
    removeNoteFeaturedImage() {
      this.featuredImage = null;
      this.removeFeaturedImage = true;
      this.hasFeaturedImageValue = false;
      this.imageData = null;
      this.$root.$emit('reset-featured-image-data');
    }
  }
};
</script>
