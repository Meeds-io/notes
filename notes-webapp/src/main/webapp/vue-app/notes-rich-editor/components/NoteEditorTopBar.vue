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
  <div class="notes-topbar">
    <div
      v-if="!showTranslationBar"
      class="notesActions white">
      <div class="notesFormButtons d-inline-flex flex-wrap width-full py-3 px-5 ma-0">
        <div class="notesFormLeftActions d-inline-flex align-center me-10">
          <v-icon
            class="icon-default-color editor-icon"
            size="24">
            {{ editorIcon }}
          </v-icon>
          <span class="notesFormTitle my-auto ms-3 me-5">{{ formTitle }}</span>
          <v-tooltip bottom v-if="translationOptionEnabled">
            <template #activator="{ on, attrs }">
              <v-btn
                v-on="on"
                v-bind="attrs"
                width="36"
                min-width="36"
                height="36"
                class="pa-0 my-auto"
                icon
                :disabled="!noteIdParam || !editorReady"
                @click="showTranslations">
                <v-icon
                  :class="{'primary--text': !!selectedLanguage}"
                  :aria-label="$t('notes.label.button.translations.options')"
                  size="20"
                  class="pa-0 translation-button-icon my-auto icon-default-color">
                  fa-language
                </v-icon>
              </v-btn>
            </template>
            <span class="caption">
              {{ langButtonTooltipText }}
            </span>
          </v-tooltip>
          <v-btn
            v-if="editorMetadataDrawerEnabled"
            width="36"
            min-width="36"
            height="36"
            class="pa-0 my-auto "
            icon
            :disabled="!editorReady"
            @click="openMetadataDrawer">
            <v-icon
              size="20"
              class="pa-0 metadata-button-icon my-auto icon-default-color">
              fas fa-th-list
            </v-icon>
          </v-btn>
        </div>
        <div class="notesFormRightActions pe-5">
          <p class="draftSavingStatus my-auto me-3">{{ draftSavingStatus }}</p>
          <v-btn
            v-if="!isMobile"
            id="notesUpdateAndPost"
            class="btn btn-primary primary px-2 py-0 me-5"
            height="34"
            :key="postKey"
            :disabled="saveButtonDisabled"
            :aria-label="publishButtonText"
            @click.once="postNote(false)">
            {{ publishButtonText }}
          </v-btn>
          <div
            v-else>
            <v-btn
              class="btn-primary primary pa-0 me-2"
              width="42"
              height="36"
              min-width="42"
              text
              :key="postKey"
              :disabled="saveButtonDisabled"
              :aria-label="publishButtonText"
              @click.once="postNote(false)">
              <v-icon
                class="text--white save-button-icon"
                size="20">
                {{ saveButtonIcon }}
              </v-icon>
            </v-btn>
            <v-btn
              class="me-2"
              width="36"
              min-width="36"
              height="36"
              icon
              @click="closeEditor">
              <v-icon
                class="icon-default-color"
                size="20">
                fas fa-times
              </v-icon>
            </v-btn>
          </div>
        </div>
      </div>
    </div>
    <note-translation-edit-bar
      ref="translationsEditBar"
      :note="note"
      :languages="languages"
      :translations="translations"
      :is-mobile="isMobile"
      @translations-hidden="showTranslationBar = false" />
    <div id="notesTop" class="width-full darkComposerEffect"></div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      showTranslationBar: false,
    };
  },
  props: {
    note: {
      type: Object,
      default: null
    },
    editorIcon: {
      type: String,
      default: 'fas fa-clipboard'
    },
    saveButtonIcon: {
      type: String,
      default: 'fas fa-save'
    },
    noteIdParam: {
      type: String,
      default: null
    },
    postKey: {
      type: Number,
      default: 1
    },
    formTitle: {
      type: String,
      default: null
    },
    selectedLanguage: {
      type: Object,
      default: null
    },
    isMobile: {
      type: Boolean,
      default: false
    },
    webPageNote: {
      type: Boolean,
      default: false
    },
    languages: {
      type: Array,
      default: () => []
    },
    translations: {
      type: Array,
      default: () => []
    },
    draftSavingStatus: {
      type: String,
      default: null
    },
    publishButtonText: {
      type: String,
      default: null
    },
    langButtonTooltipText: {
      type: String,
      default: null
    },
    webPageUrl: {
      type: Boolean,
      default: false
    },
    saveButtonDisabled: {
      type: Boolean,
      default: true
    },
    translationOptionEnabled: {
      type: Boolean,
      default: true
    },
    editorReady: {
      type: Boolean,
      default: false
    },
  },
  computed: {
    editorMetadataDrawerEnabled() {
      return eXo?.env?.portal?.notesEditorMetadataDrawerEnabled &&!this.isMobile && !this.webPageNote;
    }
  },
  created() {
    this.$root.$on('hide-translations', this.hideTranslations);
  },
  methods: {
    closeEditor() {
      this.$emit('editor-closed');
    },
    postNote(toPublish) {
      this.$emit('post-note', toPublish);
    },
    showTranslations() {
      if (this.noteIdParam) {
        this.showTranslationBar = true;
        this.$refs.translationsEditBar.show(this.selectedLanguage);
      }
    },
    hideTranslations() {
      this.showTranslationBar = false;
      this.$refs.translationsEditBar.hide();
    },
    openMetadataDrawer() {
      this.$emit('open-metadata-drawer');
    }
  }
};
</script>
