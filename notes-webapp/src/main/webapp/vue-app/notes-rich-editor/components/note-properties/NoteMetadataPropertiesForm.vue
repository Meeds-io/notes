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
  <v-form>
    <label for="image-area">
      <p class="text-color text-body mb-3">
        {{ $t('notes.metadata.featuredImage.label') }}
      </p>
      <v-btn
        v-if="!canShowFeaturedImagePreview"
        name="image-area"
        class="btn add-image-area d-flex"
        height="206"
        width="100%"
        text
        @click="openFeaturedImageDrawer">
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
              :lazy-src="featuredImageLink"
              :alt="savedFeaturedImageAltText"
              :src="featuredImageLink">
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
                  @click="openFeaturedImageDrawer">
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
        <span
          class="d-flex justify-end me-1"
          :class="{'error-color': summaryLengthError}">
          {{ summaryContent?.length }}/{{ summaryMaxLength }}
          <v-icon
            class="ms-1 my-auto"
            :class="[{ 'success-color': !summaryLengthError }, 'error-color']"
            size="16">
            fas fa-info-circle
          </v-icon>
        </span>
      </div>
    </label>
  </v-form>
</template>

<script>
export default {
  data() {
    return {
      initialized: false,
      noteObject: null,
      summaryContent: null,
      imageData: null,
      uploadId: null,
      mimeType: null,
      featuredImageAltText: null,
      hasFeaturedImageValue: false,
      removeFeaturedImage: false,
      illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`,
    };
  },
  props: {
    note: {
      type: Object,
      default: null
    },
    currentProperties: {
      type: Object,
      default: null
    },
    hasFeaturedImage: {
      type: Boolean,
      default: false
    },
    summaryMaxLength: {
      type: Number,
      default: 1300
    }
  },
  computed: {
    summaryLengthError() {
      return this.summaryContent?.length > this.summaryMaxLength;
    },
    savedFeaturedImageAltText() {
      return this.noteObject?.properties.featuredImage?.altText;
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
    'currentProperties.summary': function () {
      this.summaryContent = this.currentProperties.summary;
    },
    'noteObject.lang': function () {
      this.imageData = null;
    },
    note() {
      this.noteObject = structuredClone(this.note);
    },
    hasFeaturedImage() {
      this.hasFeaturedImageValue = this.hasFeaturedImage;
    },
    imageData() {
      if (!this.imageData?.length) {
        this.$root.$emit('reset-featured-image-data');
      }
      this.propertiesUpdated();
    },
    summaryContent() {
      if (!this.noteObject?.properties) {
        this.noteObject.properties = {};
      }
      this.noteObject.properties.summary = this.summaryContent;
      this.propertiesUpdated();
    },
    uploadId() {
      if (this.uploadId) {
        this.removeFeaturedImage = false;
      }
      this.propertiesUpdated();
    },
  },
  created() {
    this.initProperties();
    this.$root.$on('image-data', this.imageDataUpdated);
    this.$root.$on('image-uploaded', (uploadId) => this.uploadId = uploadId);
    this.$root.$on('image-alt-text', (altText) => this.featuredImageAltText = altText);
  },
  methods: {
    initProperties() {
      this.noteObject = structuredClone(this.note);
      this.hasFeaturedImageValue = this.hasFeaturedImage;
      this.summaryContent = this.currentProperties?.summary || '';
    },
    propertiesUpdated() {
      const savedFeaturedImageId = this.noteObject?.properties?.featuredImage?.id;
      const properties = {
        noteId: this.noteObject?.id,
        summary: this.summaryContent,
        featuredImage: {
          id: savedFeaturedImageId,
          uploadId: this.uploadId,
          mimeType: this.mimeType,
          altText: this.featuredImageAltText
        },
        draft: this.isDraft || !this.notedId
      };
      if (this.notedId) {
        properties.featuredImage.toDelete = this.removeFeaturedImage;
      }
      this.$emit('properties-updated', properties);
    },
    imageDataUpdated(data, mimeType) {
      this.imageData = data;
      this.mimeType = mimeType;
    },
    openFeaturedImageDrawer() {
      this.$root.$emit('open-featured-image-drawer', {
        altText: this.featuredImageAltText || this.savedFeaturedImageAltText,
        src: this.featuredImageLink
      });
    },
    removeNoteFeaturedImage() {
      this.featuredImage = null;
      this.removeFeaturedImage = true;
      this.hasFeaturedImageValue = false;
      this.$root.$emit('reset-featured-image-data');
      this.resetUploadedImage();
      this.propertiesUpdated();
    },
    resetUploadedImage() {
      this.imageData = null;
      this.uploadId = null;
    },
    cancelChanges() {
      this.noteObject.properties = structuredClone(this.currentProperties || {});
      this.summaryContent = this.currentProperties?.summary || '';
      this.featuredImageAltText = this.savedFeaturedImageAltText;
      this.hasFeaturedImageValue = this.hasFeaturedImage;
      this.resetUploadedImage();
    }
  }
};
</script>
