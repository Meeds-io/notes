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
          <img
            :src="srcImageNote"
            :alt="formTitle">
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
            id="notesUpdateAndPost"
            class="btn btn-primary primary px-2 py-0"
            :key="postKey"
            :aria-label="publishButtonText"
            @click.once="postNote(false)">
            {{ publishButtonText }}
            <v-icon
              v-if="!webPageNote && enablePublishAndPost"
              id="notesPublichAndPost"
              dark
              @click.stop.prevent="openPublishAndPost">
              mdi-menu-down
            </v-icon>
          </button>
          <v-menu
            v-if="!webPageNote"
            v-model="publishAndPost"
            :attach="'#notesUpdateAndPost'"
            transition="scroll-y-transition"
            content-class="publish-and-post-btn width-full"
            offset-y
            left>
            <v-list-item
              @click.stop="postNote(true)"
              class="px-2">
              <v-icon
                size="16"
                class="primary--text clickable pr-2">
                mdi-arrow-collapse-up
              </v-icon>
              <span class="body-2 text-color">
                {{ publishAndPostButtonText }}
              </span>
            </v-list-item>
          </v-menu>
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
      srcImageNote: '/notes/images/wiki.png',
      showTranslationBar: false,
      publishAndPost: false,
      waitTimeUntilCloseMenu: 200
    };
  },
  props: {
    note: {
      type: Object,
      default: null
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
    publishAndPostButtonText: {
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
    enablePublishAndPost: {
      type: Boolean,
      default: false
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
    this.initPublishAndPost();
  },
  methods: {
    initPublishAndPost() {
      $(document).on('mousedown', () => {
        if (this.publishAndPost) {
          window.setTimeout(() => {
            this.publishAndPost = false;
          }, this.waitTimeUntilCloseMenu);
        }
      });
    },
    openPublishAndPost() {
      this.publishAndPost = !this.publishAndPost;
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
