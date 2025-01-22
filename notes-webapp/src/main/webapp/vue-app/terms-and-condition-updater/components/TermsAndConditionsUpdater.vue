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
  <v-dialog
    ref="dialog"
    v-model="dialog"
    width="500px"
    content-class="uiPopup"
    max-width="100vw">
    <v-card v-if="dialog" class="elevation-12">
      <div class="ignore-vuetify-classes popupHeader ClearFix">
        <span class="ignore-vuetify-classes text-title">{{ $t('termsAndConditions.label.termsAndConditions') }}
        </span>
      </div>
      <v-card-text>{{ termsStatusLabel }}</v-card-text>
      <v-card-text>{{ instruction }}</v-card-text>
      <v-card-actions>
        <v-spacer />
        <v-btn
          class="ignore-vuetify-classes btn btn-primary me-2"
          @click="goToTermsAndConditions">
          {{ $t('termsAndConditions.label.next') }}
        </v-btn>
        <v-spacer />
      </v-card-actions>
    </v-card>
  </v-dialog>
</template>
<script>
export default {
  data: () => ({
    dialog: false,
    termsAndConditionsUrl: `${eXo.env.portal.context}/${eXo.env.portal.engagementSiteName}/terms-and-conditions`,
    eventName: '',
  }),
  computed: {
    termsStatusLabel() {
      return this.eventName === 'termsAndConditionsUpdated' ? this.$t('termsAndConditions.label.termsUpdated') : this.$t('termsAndConditions.label.termsAdded');
    },
    instruction() {
      return this.eventName === 'termsAndConditionsUpdated' ? this.$t('termsAndConditions.label.readAgain') : this.$t('termsAndConditions.label.read');
    }
  },
  created() {
    this.$termsAndConditionsWebSocket.initCometd(this.handleTermsAndConditionsUpdates);
  },
  methods: {
    handleTermsAndConditionsUpdates(updateParams) {
      this.eventName = updateParams?.wsEventName;
      this.dialog = this.eventName !== 'termsAndConditionsAccepted';
    },
    goToTermsAndConditions() {
      const currentPage = window.location.pathname + window.location.search;
      window.location.href = `${eXo.env.portal.context}/${eXo.env.portal.engagementSiteName}/terms-and-conditions?redirect=${encodeURIComponent(currentPage)}`;
    }
  },
};
</script>