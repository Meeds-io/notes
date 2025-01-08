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
    <v-card-title class="text-h4 font-weight-bold text-color">{{ pageTitle }}</v-card-title>
    <v-card-text v-sanitized-html="pageContentDisplay" />
  </v-card>
</template>

<script>

export default {
  data: () => ({
    lang: eXo.env.portal.language,
    displayed: true,
    page: null
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

