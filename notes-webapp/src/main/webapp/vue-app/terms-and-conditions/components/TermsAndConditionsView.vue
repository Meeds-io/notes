<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
  <v-app v-if="pageContent" class="application-body">
    <v-card flat>
      <v-sheet height="12" class="primary no-border-bottom" />
      <v-card-title class="text-h4 font-weight-bold text-color">{{ pageTitle }}</v-card-title>
      <v-card-text
        v-sanitized-html="pageContentDisplay" />
      <v-card-actions class="px-4">
        <div v-if="!isMobile" :style="brandingLogoDisplay">
          <v-img
            :src="brandingLogoUrl"
            width="9em"
            max-width="150px"
            max-height="6em"
            role="presentation"
            contain
            eager />
        </div>
        <div v-if="!alreadyAccepted" class="d-flex flex-column ms-auto">
          <v-checkbox
            v-model="accepted"
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
      loading: false
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
  },
  created() {
    this.retrieveTerms();
    this.isTermsAccepted();
  },
  methods: {
    retrieveTerms() {
      return this.$termsAndConditionsService.getTermsAndConditions(this.lang).then(page => {
        this.page = page;
      });
    },
    accept() {
      this.loading = true;
      return this.$termsAndConditionsService.acceptTermsAndConditions(this.lang).then(() => {
        window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.engagementSiteName}`;
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