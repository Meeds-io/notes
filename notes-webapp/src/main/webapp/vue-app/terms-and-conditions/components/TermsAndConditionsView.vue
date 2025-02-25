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
  <v-app v-if="initialized" class="application-body">
    <v-card flat>
      <v-sheet height="12" class="primary no-border-bottom" />
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
        <v-card-text
          v-if="pageContentDisplay"
          v-sanitized-html="pageContentDisplay"
          class="px-0" />
      </div>
      <v-card-actions class="px-4 align-end">
        <div v-if="!isMobile" :style="brandingLogoDisplay">
          <v-img
            :src="brandingLogoUrl"
            width="6em"
            max-width="150px"
            max-height="6em"
            role="presentation"
            contain
            eager />
        </div>
        <div v-if="!alreadyAccepted || isPreviewMode" class="d-flex flex-column ms-auto">
          <v-checkbox
            v-model="accepted"
            :class="isPreviewMode && 'not-clickable-link'"
            class="mb-2"
            dense>
            <template #label>
              <span class="text--color font-weight-bold">
                {{ $t('termsAndConditions.label.readAndConsent') }}
              </span>
            </template>
          </v-checkbox>
          <v-btn
            :disabled="!accepted"
            :loading="loading"
            class="width-fit-content align-self-end"
            color="primary"
            depressed
            @click="accept">
            {{ $t('termsAndConditions.label.accept') }}
          </v-btn>
        </div>
      </v-card-actions>
    </v-card>
  </v-app>
</template>
<script>
export default {
  data() {
    return {
      dialog: false,
      accepted: false,
      alreadyAccepted: false,
      page: null,
      lang: eXo.env.portal.language,
      loading: false,
      initialized: false,
      isPreviewMode: false,
      illustrationBaseUrl: `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes/illustration/`,
    };
  },
  computed: {
    pageContent() {
      return this.page?.content;
    },
    pageTitle() {
      return this.page?.title;
    },
    pageContentDisplay() {
      return this.pageContent && this.$noteUtils.getContentToDisplay(this.pageContent) || '';
    },
    brandingLogoDisplay() {
      return this.$vuetify.breakpoint.width >= this.$vuetify.breakpoint.thresholds.xl ? 'position: fixed; right: 3%;bottom: 3%' : '';
    },
    isMobile() {
      return this.$vuetify.breakpoint.width < this.$vuetify.breakpoint.thresholds.sm;
    },
    brandingLogoUrl() {
      return `/portal/rest/v1/platform/branding/logo?v=${Date.now()}`;
    },
    hasSummary() {
      return this.page?.properties?.summary?.length;
    },
    noteSummary() {
      return this.page?.properties?.summary;
    },
    noteLang() {
      return this.page?.lang || 'en';
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
      return `${this.illustrationBaseUrl}${this.page?.id}?v=${this.noteFeatureImageUpdatedDate}&isDraft=false&lang=${this.noteLang}&size=0x400`;
    },
  },
  created() {
    this.isPreviewMode = new URLSearchParams(window.location.search).has('preview');
    if (this.isPreviewMode) {
      this.accepted = false;
    }
    this.retrieveTerms();
    if (!this.isPreviewMode) {
      this.isTermsAccepted();
    }
  },
  methods: {
    async retrieveTerms() {
      try {
        this.page = await this.$termsAndConditionsService.getTermsAndConditions(this.lang);
      } finally {
        this.initialized = true;
      }
    },
    accept() {
      this.loading = true;
      return this.$termsAndConditionsService.acceptTermsAndConditions(this.lang)
        .then(() => {
          const urlParams = new URLSearchParams(window.location.search);
          const redirectUrl = urlParams.get('redirect');
          window.location.href = redirectUrl || `${eXo.env.portal.context}/${eXo.env.portal.engagementSiteName}`;
        });
    },
    isTermsAccepted() {
      return this.$termsAndConditionsService.isTermsAcceptedForUser(this.lang).then((accepted) => {
        this.alreadyAccepted = accepted;
      }).finally(() => this.loading = false);
    }
  },
};
</script>