<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <v-card class="application-body" flat>
    <v-toolbar
      class="border-box-sizing"
      flat>
      <v-btn
        icon
        height="36"
        width="36"
        @click="$emit('back')">
        <v-icon size="20">
          {{ $vuetify.rtl && 'mdi-arrow-right' || 'mdi-arrow-left' }}
        </v-icon>
      </v-btn>
      <v-toolbar-title class="ps-0 text-title">
        {{ $t('termsAndConditions.label.termsAndConditions') }}
      </v-toolbar-title>
      <v-spacer />
    </v-toolbar>
    <div class="px-4">
      <v-img
        v-if="hasFeaturedImage"
        :lazy-src="featuredImageLink"
        :alt="featuredImageAltText"
        :src="featuredImageLink"
        contain
        class="mb-5 mt-2"
        width="100%"
        max-height="400" />
      <v-card-title class="text-h4 font-weight-bold text-color px-0">{{ pageTitle }}</v-card-title>
      <p
        v-if="hasSummary"
        class="note-summary text-break text-sub-title mt-4 mb-0">
        {{ noteSummary }}
      </p>
      <v-card-text class="px-0" v-sanitized-html="pageContentDisplay" />
    </div>
  </v-card>
</template>

<script>

export default {
  data: () => ({
    lang: eXo.env.portal.language,
    displayed: true,
    page: null,
    illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`
  }),
  computed: {
    pageContent() {
      return this.page?.content;
    },
    pageContentDisplay() {
      return this.pageContent && this.$noteUtils.getContentToDisplay(this.pageContent) || '';
    },
    pageTitle() {
      return this.page?.title;
    },
    hasSummary() {
      return this.page?.properties?.summary?.length;
    },
    noteSummary() {
      return this.page?.properties?.summary;
    },
    noteFeatureImageUpdatedDate() {
      return this.page?.properties?.featuredImage?.lastUpdated || 0;
    },
    hasFeaturedImage() {
      return !!this.page?.properties?.featuredImage?.id;
    },
    featuredImageAltText() {
      return this.page?.properties?.featuredImage?.altText;
    },
    featuredImageLink() {
      return `${this.illustrationBaseUrl}${this.page?.id}?v=${this.noteFeatureImageUpdatedDate}&isDraft=false&lang=en&size=0x400`;
    },
  },
  created() {
    this.retrieveTerms();
    document.addEventListener('hideSettingsApps', (id) => {
      if (this.id !== id) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => {
      this.displayed = true;
    });
  },
  methods: {
    retrieveTerms() {
      return this.$termsAndConditionsService.getTermsAndConditions(this.lang).then(page => {
        this.page = page;
      });
    },
  }
};
</script>

