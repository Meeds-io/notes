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
  <image-crop-drawer
    ref="featuredImageDrawer"
    :drawer-title="$t('notes.metadata.featuredImage.edit.label')"
    :can-upload="true"
    back-icon="fas fa-arrow-left"
    :src="featuredImageLink"
    :max-file-size="maxFileSize"
    :crop-options="cropOptions"
    alt
    @input="uploadId = $event"
    @data="imageData = $event"
    @alt-text="featuredImageAltText = $event" />
</template>

<script>
export default {
  data() {
    return {
      uploadId: null,
      maxFileSize: 20971520,
      format: 'landscape',
      imageData: null,
      featuredImageAltText: null,
      hasFeaturedImageValue: false,
      illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`,
      cropOptions: {
        aspectRatio: 8,
        viewMode: 1
      },
    };
  },
  props: {
    note: {
      type: Object,
      default: null
    },
    hasFeaturedImage: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    imageData() {
      if (this.imageData?.length) {
        this.$root.$emit('image-data', this.imageData, this.getMimeType(this.imageData));
      }
    },
    uploadId() {
      if (this.uploadId) {
        this.$root.$emit('image-uploaded', this.uploadId);
      }
    },
    featuredImageAltText() {
      this.$root.$emit('image-alt-text', this.featuredImageAltText);
    },
    hasFeaturedImage() {
      this.hasFeaturedImageValue = this.hasFeaturedImage;
    }
  },
  computed: {
    langParam() {
      return this.note?.lang && `&lang=${this.note.lang}` || '';
    },
    isDraft() {
      return this.note?.draftPage;
    },
    noteFeatureImageUpdatedDate() {
      return this.note?.properties.featuredImage?.lastUpdated;
    },
    featuredImageLink() {
      return this.imageData || this.hasFeaturedImageValue
                            && `${this.illustrationBaseUrl}${this.note?.id}?v=${this.noteFeatureImageUpdatedDate}&isDraft=${this.isDraft}${this.langParam}` || '';
    }
  },
  created() {
    this.$root.$on('open-featured-image-drawer', this.open);
    this.$root.$on('reset-featured-image-data', this.resetData);
  },
  methods: {
    resetData() {
      this.imageData = null;
      this.uploadId = null;
      this.hasFeaturedImageValue = false;
    },
    getMimeType(data) {
      return this.$refs.featuredImageDrawer.getBase64Mimetype(data);
    },
    open(imageItem) {
      this.hasFeaturedImageValue = this.hasFeaturedImage;
      this.$refs.featuredImageDrawer.open(imageItem);
    },
    close() {
      this.$refs.featuredImageDrawer.close();
    },
    isClosed() {
      return this.$refs.featuredImageDrawer.$el.classList.contains('v-navigation-drawer--close');
    },
  }
};
</script>
