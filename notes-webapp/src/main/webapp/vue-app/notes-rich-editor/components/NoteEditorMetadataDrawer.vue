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
<<<<<<< HEAD
        <div class="d-flex my-auto text-header font-weight-bold text-color">
=======
        <div class="d-flex my-auto font-weight-bold text-color">
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
          {{ $t('notes.metadata.properties.label') }}
        </div>
      </template>
      <template slot="content">
        <div class="pa-5">
          <v-form>
            <label for="image-area">
<<<<<<< HEAD
              <p class="text-color text-body mb-3">
                {{ $t('notes.metadata.featuredImage.label') }}
              </p>
              <v-file-input
                id="featuredImageInput"
                ref="featuredImageInput"
                accept="image/*"
                class="position-absolute hidden"
                @change="handleUpload" />
=======
              <p class="text-color xlarge-font-size mb-3">
                {{ $t('notes.metadata.featuredImage.label') }}
              </p>
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
              <v-btn
                v-if="!canShowFeaturedImagePreview"
                name="image-area"
                class="btn add-image-area d-flex"
<<<<<<< HEAD
                height="206"
                width="100%"
                text
                @click="uploadFeaturedImage">
=======
                height="95"
                width="100%"
                text
                @click="openFeaturedImageDrawer">
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
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
=======
                height="48">
                <v-hover v-slot="{ hover }">
                  <div>
                    <v-img
                      height="48"
                      :lazy-src="featuredImageLink"
                      :alt="savedFeaturedImageAltText"
                      :src="featuredImageLink" />
                    <div
                      v-if="hover && canShowFeaturedImagePreview"
                      class="width-fit-content ms-auto featured-image-controls me-2">
                      <v-btn
                        :loading="isRemovingFeaturedImage"
                        class="feature-image-button me-1"
                        icon
                        @click.stop="removeNoteFeaturedImage">
                        <v-icon
                          class="feature-image-trash-icon white--text"
                          size="20">
                          fa-solid fa-trash
                        </v-icon>
                      </v-btn>
                      <v-btn
                        class="feature-image-button"
                        icon
                        @click="openFeaturedImageDrawer">
                        <v-icon
                          class="feature-image-file-icon white--text"
                          size="20">
                          fa-regular fa-file-image
                        </v-icon>
                      </v-btn>
                    </div>
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
                  </div>
                </v-hover>
              </v-sheet>
            </label>
            <label for="summaryInputEditor">
              <div class="mt-5">
<<<<<<< HEAD
                <p class="text-color text-body mb-3">
=======
                <p class="text-color xlarge-font-size mb-3">
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
                  {{ $t('notes.metadata.summary.label') }}
                </p>
                <v-textarea
                  v-model="summaryContent"
                  name="summaryInputEditor"
<<<<<<< HEAD
                  class="summary-metadata-input pt-0 overflow-auto"
=======
                  class="summary-metadata-input pt-0"
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
            @click="cancelChanges">
=======
            @click="close">
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            :disabled="saveDisabled"
<<<<<<< HEAD
=======
            :loading="isSaving"
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
      maxFileSize: 20 * 1024 * 1024,
=======
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
      removeFeaturedImage: false,
      isUploading: false
=======
      isSaving: false,
      isRemovingFeaturedImage: false
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
      return this.noteObject?.properties.featuredImage?.featuredImageAltText;
    },
    saveDisabled() {
      return (!this.propertiesChanged && !this.imageData && !this.removeFeaturedImage) || this.isUploading;
=======
      return this.noteObject?.properties?.featuredImageAltText;
    },
    saveDisabled() {
      return !this.propertiesChanged && !this.imageData;
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
    langParam() {
      return this.noteObject?.lang && `&lang=${this.noteObject?.lang}` || '';
=======
    lang() {
      return this.noteObject?.lang || '';
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
    },
    isDraft() {
      return this.noteObject?.draftPage;
    },
    noteFeatureImageUpdatedDate() {
<<<<<<< HEAD
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
=======
      return this.noteObject?.properties?.featuredImageUpdatedDate || 0;
    },
    featuredImageLink() {
      const langParam = this.lang && `&lang=${this.lang}` || '';
      return this.imageData || this.hasFeaturedImageValue
                            && `${this.illustrationBaseUrl}${this.notedId}?v=${this.noteFeatureImageUpdatedDate}&isDraft=${this.isDraft}${langParam}` || '';
    },
  },
  watch: {
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
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
=======
    resetProperties() {
      this.noteObject.properties = this.currentNoteProperties;
      this.featureImageUpdated = false;
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
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
<<<<<<< HEAD
      this.removeFeaturedImage = false;
      this.$refs.metadataDrawer.open();
    },
    cancelChanges() {
      this.close();
      this.noteObject.properties = structuredClone(this.currentNoteProperties || {});
      this.summaryContent = this.currentNoteProperties?.summary || '';
      this.hasFeaturedImageValue = this.hasFeaturedImage;
      this.imageData = null;
      this.uploadId = null;
    },
    close() {
      this.$refs.metadataDrawer.close();
    },
    save() {
      const savedFeaturedImageId = this.noteObject?.properties?.featuredImage?.id;
      const properties = {
        noteId: this.noteObject?.id,
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
=======
      this.$refs.metadataDrawer.open();
    },
    close() {
      this.$refs.metadataDrawer.close();
    },
    displayMessage(message) {
      document.dispatchEvent(new CustomEvent('alert-message', {
        detail: {
          alertType: message.type,
          alertMessage: message.text
        }
      }));
    },
    save() {
      const properties = {
        noteId: this.isDraft && this.noteObject?.targetPageId
                             || this.noteObject?.id,
        summary: this.summaryContent,
        featuredImage: {
          uploadId: this.uploadId,
          base64Data: this.imageData,
          mimeType: this.mimeType,
          altText: this.featuredImageAltText
        },
        draft: this.isDraft
      };
      this.isSaving = true;
      return this.$notesService.saveNoteMetadata(properties, this.lang).then((properties) => {
        this.hasFeaturedImageValue = !!properties?.featuredImageId;
        this.noteObject.properties = properties;
        this.currentNoteProperties = structuredClone(properties);
        this.$emit('metadata-updated');
        this.displayMessage({
          type: 'success',
          text: this.$t('notes.metadata.saved.success.message')
        });
      }).catch(() => {
        this.displayMessage({
          type: 'error',
          text: this.$t('notes.metadata.saved.error.message')
        });
      }).finally(() => {
        this.isSaving = false;
        this.close();
      });
    },
    removeNoteFeaturedImage() {
      if (!this.hasFeaturedImage) {
        this.imageData = null;
        this.hasFeaturedImageValue = false;
        return;
      }
      this.isRemovingFeaturedImage = true;
      return this.$notesService.removeNoteFeaturedImage(this.notedId, this.noteObject?.draftPage, this.lang).then(() => {
        this.hasFeaturedImageValue = false;
        this.imageData = null;
        this.$root.$emit('reset-featured-image-data');
        this.displayMessage({
          type: 'success',
          text: this.$t('notes.featuredImage.remove.success.message')
        });
      }).catch(() => {
        this.displayMessage({
          type: 'error',
          text: this.$t('notes.featuredImage.remove.error.message')
        });
      }).finally(() => this.isRemovingFeaturedImage = false);
>>>>>>> 5f49af63c (feat: Implement note editor metadata drawer - EXO-71928,EXO-71929,EXO-71930 - Meeds-io/MIPs#128 (#1039))
    }
  }
};
</script>
