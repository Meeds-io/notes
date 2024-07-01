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
      <div class="notesFormButtons d-inline-flex flex-wrap width-full pa-3 ma-0">
        <div class="notesFormLeftActions d-inline-flex align-center me-10">
          <v-icon
            size="24">
            {{ editorIcon }}
          </v-icon>
          <span class="notesFormTitle ps-2">{{ formTitle }}</span>
          <v-tooltip bottom>
            <template #activator="{ on, attrs }">
              <v-icon
                :aria-label="$t('notes.label.button.translations.options')"
                size="22"
                class="clickable pa-2"
                :class="langButtonColor"
                v-on="on"
                v-bind="attrs"
                @click="showTranslations">
                fa-language
              </v-icon>
            </template>
            <span class="caption">
              {{ langButtonTooltipText }}
            </span>
          </v-tooltip>
        </div>
        <div class="notesFormRightActions pr-7">
          <p class="draftSavingStatus mr-7">{{ draftSavingStatus }}</p>
          <button
            v-if="!isMobile"
            id="notesUpdateAndPost"
            class="btn btn-primary primary px-2 py-0"
            :key="postKey"
            :disabled="saveButtonDisabled"
            :aria-label="publishButtonText"
            @click.once="postNote(false)">
            {{ publishButtonText }}
          </button>
          <div
            v-else>
            <v-btn
              class="btn-primary primary pa-0 me-4"
              width="42"
              height="36"
              min-width="42"
              text
              :key="postKey"
              :disabled="saveButtonDisabled"
              :aria-label="publishButtonText"
              @click.once="postNote(false)">
              <v-icon
                class="text--white"
                size="20">
                {{ saveButtonIcon }}
              </v-icon>
            </v-btn>
            <v-btn
              width="14"
              min-width="14"
              height="21"
              icon
              @click="closeEditor">
              <v-icon
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
      @translations-hidden="showTranslationBar = false"/>
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
    }
  },
  computed: {
    langButtonColor(){
      if (!this.noteIdParam){
        return 'disabled--text not-clickable remove-focus';
      }
      return this.selectedLanguage ? 'primary--text':'';
    },
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
  }
};
</script>
