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
  <exo-drawer
    ref="drawer"
    v-model="drawer"
    id="termsAndConditionsDrawer"
    allow-expand
    right>
    <template #title>
      {{ $t('generalSettings.termsAndConditions') }}
    </template>
    <template v-if="drawer" #content>
      <v-card class="pa-4" flat>
        <div>
          {{ $t('generalSettings.termsAndConditions.description') }}
        </div>
        <template v-if="noteId">
          <v-list-item class="px-0" two-line>
            <v-list-item-content>
              <v-list-item-title class="font-weight-bold">
                {{ $t('generalSettings.termsAndConditions.published') }}
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-switch
                v-model="published"
                :loading="loading"
                class="my-auto"
                hide-details />
            </v-list-item-action>
          </v-list-item>
          <v-list-item class="px-0" two-line>
            <v-list-item-content>
              <v-list-item-title class="font-weight-bold">
                {{ $t('generalSettings.termsAndConditions.previewAsUser') }}
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                icon>
                <v-icon size="18" class="icon-default-color">fas fa-eye</v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
          <v-list-item class="px-0" two-line>
            <v-list-item-content>
              <v-list-item-title class="font-weight-bold">
                {{ $t('generalSettings.termsAndConditions.editContent') }}
              </v-list-item-title>
            </v-list-item-content>
            <v-list-item-action>
              <v-btn
                icon
                @click="editTerms">
                <v-icon size="18" class="icon-default-color">fas fa-edit</v-icon>
              </v-btn>
            </v-list-item-action>
          </v-list-item>
        </template>
        <template v-else>
          <div class="d-flex align-center justify-center mt-4">
            <v-btn
              class="btn btn-primary"
              @click="addTerms">
              {{ $t('generalSettings.termsAndConditions.create') }}
            </v-btn>
          </div>
        </template>
      </v-card>
    </template>
  </exo-drawer>
</template>

<script>

export default {
  data: () => ({
    drawer: false,
    note: null,
    published: false,
    loading: false,
    lang: eXo.env.portal.language,
  }),
  computed: {
    parentPageId() {
      return this.note?.parentPageId;
    },
    noteId() {
      return this.note?.id;
    },
  },
  created() {
    this.$root.$on('terms-and-conditions-create', this.open);
    this.retrieveTerms();
  },
  methods: {
    open() {
      this.$refs.drawer.open();
    },
    close() {
      this.$refs.drawer.close();
    },
    retrieveTerms() {
      return this.$termsAndConditionsService.getTermsAndConditions(this.lang).then(note => {
        this.note = note;
      }).finally(() => this.saving = false);
    },
    addTerms() {
      this.saving = true;
      return this.$termsAndConditionsService.saveTermsAndConditions('', this.lang).then(note => {
        this.note = note;
        this.editTerms();
      })
        .finally(() => this.saving = false);
    },
    editTerms() {
      const formData = new FormData();
      formData.append('noteId', this.note?.id);
      formData.append('parentNoteId', this.parentPageId);
      formData.append('pageName', 'termsAndConditions');
      if (eXo.env.portal?.spaceGroup) {
        formData.append('spaceGroupId', eXo.env.portal?.spaceGroup);
      }
      formData.append('isDraft', 'false');
      formData.append('showMaxWindow', 'true');
      formData.append('hideSharedLayout', 'true');
      if (this.note?.lang) {
        formData.append('translation', this.lang);
      }
      const urlParams = new URLSearchParams(formData).toString();
      window.open(`${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/notes-editor?${urlParams}`);
    }
  },
};
</script>

