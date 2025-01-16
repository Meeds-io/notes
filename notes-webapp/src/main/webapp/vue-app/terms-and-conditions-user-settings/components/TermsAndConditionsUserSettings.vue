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
  <v-app>
    <terms-and-conditions-window
      v-if="displayDetails"
      @back="closeTermsAndConditionsDetail" />
    <div v-else class="application-body">
      <v-list two-line>
        <v-list-item>
          <v-list-item-content>
            <v-list-item-title class="text-title">
              {{ $t('termsAndConditions.label.termsAndConditions') }}
            </v-list-item-title>
          </v-list-item-content>
          <v-list-item-action>
            <v-btn
              small
              icon
              @click="openTermsAndConditionsDetail">
              <v-icon size="18" class="icon-default-color">fas fa-eye</v-icon>
            </v-btn>
          </v-list-item-action>
        </v-list-item>
      </v-list>
    </div>
  </v-app>
</template>

<script>

export default {
  data: () => ({
    id: `Settings${parseInt(Math.random() * 10000)
      .toString()
      .toString()}`,
    language: eXo.env.portal.language,
    displayed: true,
    displayDetails: false,
    termsAndConditionsLinkBasePath: `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/settings#terms-and-conditions`,
  }),
  watch: {
    displayed() {
      if (this.displayed) {
        this.$nextTick().then(() => this.$root.$emit('application-cache'));
      }
      this.$root.$updateApplicationVisibility(this.displayed);
    },
  },
  created() {
    document.addEventListener('hideSettingsApps', (event) => {
      if (event && event.detail && this.id !== event.detail) {
        this.displayed = false;
      }
    });
    document.addEventListener('showSettingsApps', () => {
      this.displayed = true;
    });
    this.checkTermsAndConditionsPublished().then(() => {
      this.handleHashChange();
    });
  },

  mounted() {
    this.$nextTick().then(() => this.$root.$applicationLoaded());
    this.$root.$updateApplicationVisibility(this.displayed);
  },

  methods: {
    handleHashChange() {
      if (window.location.hash === '#terms-and-conditions' && !this.displayDetails && this.displayed) {
        setTimeout(() => {
          document.dispatchEvent(new CustomEvent('hideSettingsApps', { detail: this.id }));
        },200);
        this.displayDetails = true;
      } else if (window.location.hash !== '#terms-and-conditions' && this.displayDetails) {
        this.closeTermsAndConditionsDetail();
      }
    },
    openTermsAndConditionsDetail() {
      document.dispatchEvent(new CustomEvent('hideSettingsApps', { detail: this.id }));
      this.displayDetails = true;
      window.history.replaceState(
        null,
        this.$t('termsAndConditions.label.termsAndConditions'),
        this.termsAndConditionsLinkBasePath
      );
    },
    closeTermsAndConditionsDetail() {
      this.displayDetails = false;
      document.dispatchEvent(new CustomEvent('showSettingsApps'));
      if (window.location.hash === '#terms-and-conditions') {
        window.history.replaceState('', document.title, window.location.pathname + window.location.search);
      }
    },
    checkTermsAndConditionsPublished() {
      this.displayed = false;
      return this.$termsAndConditionsService.getTermsAndConditions(this.lang).then(page => {
        this.displayed = page?.published;
      });
    },
  },
};
</script>