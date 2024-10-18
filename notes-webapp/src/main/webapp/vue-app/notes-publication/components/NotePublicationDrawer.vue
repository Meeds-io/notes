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
  <div>
    <v-overlay
      z-index="2000"
      :value="drawer"
      @click.native="closeDrawerByOverlay" />
    <exo-drawer
      id="editorPublicationDrawer"
      ref="publicationDrawer"
      v-model="drawer"
      :right="!$vuetify.rtl"
      allow-expand
      show-overlay
      @expand-updated="expanded = $event"
      @closed="reset">
      <template slot="title">
        <div class="d-flex my-auto text-header font-weight-bold text-color">
          {{ $t('notes.publication.publish.label') }}
        </div>
      </template>
      <template slot="content">
        <div class="pa-5">
          <v-stepper
            v-model="stepper"
            :class="expanded && 'flex-row' || 'flex-column'"
            class="ma-0 d-flex"
            vertical
            flat>
            <div
              v-if="!editMode"
              :class="{
                'col-6': expanded,
                'flex-grow-1': expanded || stepper === 1,
                'flex-grow-0': !expanded && stepper !== 1,
              }"
              class="flex-shrink-0">
              <v-stepper-step
                :step="1"
                :editable="!expanded"
                width="100%"
                class="ma-0 pa-0 position-relative">
                <div class="d-flex">
                  <div class="d-flex align-center flex-grow-1 flex-shrink-1 text-truncate text-header">
                    {{ $t('notes.publication.check.properties.label') }}
                  </div>
                </div>
              </v-stepper-step>
              <v-slide-y-transition>
                <div v-show="expanded || stepper === 1">
                  <div
                    class="d-flex flex-column mt-8">
                    <v-scroll-y-transition hide-on-leave>
                      <div class="mb-2">
                        <note-metadata-properties-form
                          ref="propertiesForm"
                          :note="noteObject"
                          :current-properties="currentNoteProperties"
                          :has-featured-image="hasFeaturedImage"
                          :summary-max-length="summaryMaxLength"
                          @properties-updated="propertiesUpdated" />
                      </div>
                    </v-scroll-y-transition>
                  </div>
                </div>
              </v-slide-y-transition>
            </div>
            <div
              :class="{
                'col-6': expanded && !editMode,
                'col-12': expanded && editMode,
                'mt-8': !expanded && stepper < 2 && !editMode,
                'mt-4': !expanded && stepper === 2 && !editMode,
              }"
              class="flex-grow-0 flex-shrink-0">
              <v-stepper-step
                v-if="!editMode"
                :step="2"
                :editable="!expanded"
                width="100%"
                class="ma-0 pa-0 position-relative">
                <div class="d-flex">
                  <div class="d-flex align-center flex-grow-1 flex-shrink-1 text-truncate text-header">
                    {{ $t('notes.publication.label') }}
                  </div>
                </div>
              </v-stepper-step>
              <v-slide-y-transition>
                <div v-show="expanded || stepper === (2 - editMode)">
                  <div
                    :class="{'mt-8': !editMode}"
                    class="d-flex flex-column">
                    <v-scroll-y-transition hide-on-leave>
                      <div class="mb-2">
                        <div class="d-flex">
                          <v-switch
                            v-model="publicationSettings.post"
                            :disabled="isPublishing"
                            :aria-label="$t('notes.publication.post.in.feed.label')"
                            :ripple="false"
                            color="primary"
                            class="mt-n1 me-1" />
                          <div class="d-flex flex-wrap mb-6">
                            <p class="me-2">
                              {{ $t('notes.publication.post.in.feed.label') }}
                            </p>
                            <exo-space-avatar
                              :space-id="spaceId"
                              size="21"
                              :extra-class="['mb-2 text-truncate', {
                                'post-feed-target': !expanded
                              }]"
                              bold-title
                              popover />
                          </div>
                        </div>
                        <note-publish-option
                          v-if="allowedTargets?.length"
                          ref="publishOption"
                          :allowed-targets="allowedTargets"
                          :is-publishing="isPublishing"
                          :edit-mode="editMode"
                          :expanded="expanded"
                          :saved-settings="{
                            published: publicationSettings?.publish,
                            selectedAudience: publicationSettings?.selectedAudience,
                            selectedTargets: savedTargets(publicationSettings?.selectedTargets)
                          }"
                          @updated="updatedPublicationSettings" />
                      </div>
                    </v-scroll-y-transition>
                  </div>
                </div>
              </v-slide-y-transition>
            </div>
          </v-stepper>
        </div>
      </template>
      <template slot="footer">
        <div class="d-flex width-fit-content ms-auto">
          <v-btn
            class="btn me-5"
            @click="cancel">
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            class="btn btn-primary"
            :disabled="summaryLengthError || !saveEnabled"
            :loading="isPublishing"
            @click="save">
            {{ saveButtonLabel }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>

export default {
  data() {
    return {
      stepper: 1,
      drawer: false,
      noteObject: null,
      currentNoteProperties: {},
      expanded: false,
      summaryMaxLength: 1300,
      publicationSettings: {
        post: true
      },
      currentPublicationSettings: {}
    };
  },
  props: {
    hasFeaturedImage: {
      type: Boolean,
      default: false
    },
    isPublishing: {
      type: Boolean,
      default: false
    },
    editMode: {
      type: Boolean,
      default: false
    },
    params: {
      type: Object,
      default: null
    }
  },
  computed: {
    saveEnabled() {
      return !this.editMode || this.publicationSettingsUpdated;
    },
    publicationSettingsUpdated() {
      return JSON.stringify(this.currentPublicationSettings) !== JSON.stringify(this.publicationSettings);
    },
    saveButtonLabel() {
      return (!this.editMode && !this.expanded && this.stepper === 1) && this.$t('notes.publication.publish.next.label')
                                                  || this.$t('notes.publication.publish.save.label');
    },
    summaryLengthError() {
      return this.noteObject?.properties?.summary?.length > this.summaryMaxLength;
    },
    spaceId() {
      return this.params?.spaceId;
    },
    allowedTargets() {
      return this.params?.allowedTargets;
    }
  },
  watch: {
    expanded() {
      this.stepper = this.expanded && 2 || 1;
    },
    isPublishing() {
      if (!this.isPublishing) {
        this.close();
      }
    }
  },
  methods: {
    updatedPublicationSettings(settings) {
      this.publicationSettings = structuredClone({
        post: this.publicationSettings.post
      });
      this.publicationSettings.publish = settings?.publish;
      this.publicationSettings.selectedTargets = settings?.selectedTargets;
      this.publicationSettings.selectedAudience = settings?.selectedAudience;
    },
    propertiesUpdated(properties) {
      if (!this.noteObject?.properties || !Object.keys(this.noteObject?.properties).length) {
        this.noteObject.properties = structuredClone(properties || {});
      }
      if (!this.noteObject.properties?.featuredImage) {
        this.noteObject.properties.featuredImage = {};
      }
      this.updateCurrentNoteObjectProperties(properties);
      this.propertiesToSave = properties;
    },
    savedTargets(targets) {
      return targets?.map(target => {
        return this.allowedTargets[this.allowedTargets.findIndex(allowedTarget => allowedTarget.name === target)];
      });
    },
    open(noteObject) {
      this.noteObject = noteObject;
      if (this.editMode) {
        this.publicationSettings.post = this.noteObject?.activityPosted;
        this.publicationSettings.publish = this.noteObject?.published;
        this.publicationSettings.selectedTargets = this.noteObject?.targets;
        this.publicationSettings.selectedAudience = this.noteObject?.audience;
      }
      this.currentPublicationSettings = structuredClone(this.publicationSettings);
      this.cloneProperties();
      this.$refs.publicationDrawer.open();
      this.toggleExpand();
      setTimeout(() => {
        this.$refs.publishOption.initSettings();
      }, 200);
      this.$refs.propertiesForm?.initProperties();
    },
    toggleExpand() {
      if (!this.editMode) {
        setTimeout(() => {
          this.$refs.publicationDrawer.toogleExpand();
        }, 50);
      }
    },
    cloneProperties() {
      this.currentNoteProperties = structuredClone(
        this.noteObject?.id &&
          this.noteObject?.properties || {
          noteId: 0,
          summary: '',
          featuredImage: {uploadId: null, mimeType: null, altText: null},
          draft: true
        });
    },
    close() {
      this.stepper = 1;
      this.$refs.publicationDrawer.close();
    },
    cancelChanges() {
      this.$refs?.publishOption?.cancelChanges();
    },
    reset() {
      setTimeout(() => {
        this.cancelChanges();
      }, 1000);
      this.$emit('closed');
    },
    cancel() {
      this.close();
      setTimeout(() => {
        this.$refs.propertiesForm.cancelChanges();
      }, 1000);
    },
    save() {
      if (this.editMode) {
        this.$emit('publish', this.publicationSettings);
        return;
      }
      if (!this.expanded && this.stepper === 1) {
        this.stepper += 1;
        return;
      }
      this.$emit('metadata-updated', this.noteObject.properties);
      this.$emit('publish', this.noteObject, this.publicationSettings);
    },
    updateCurrentNoteObjectProperties(properties) {
      this.noteObject.properties.noteId = Number(properties.noteId);
      this.noteObject.properties.summary = properties.summary;
      this.noteObject.properties.draft = properties.draft;

      this.noteObject.properties.featuredImage.id = properties?.featuredImage?.id;
      this.noteObject.properties.featuredImage.uploadId = properties?.featuredImage?.uploadId;
      this.noteObject.properties.featuredImage.mimeType = properties?.featuredImage?.mimeType;
      this.noteObject.properties.featuredImage.altText = properties?.featuredImage?.altText;
      this.noteObject.properties.featuredImage.toDelete = properties?.featuredImage?.toDelete;
    },
    closeDrawerByOverlay() {
      if (this.editMode) {
        this.drawer = !this.drawer;
        return;
      }
      this.$root.$emit('close-featured-image-byOverlay');
    },
  }
};
</script>
