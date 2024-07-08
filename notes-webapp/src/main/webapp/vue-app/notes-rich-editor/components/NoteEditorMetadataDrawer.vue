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
      @click.native="closeDrawersByOverlay" />
    <exo-drawer
      id="editorMetadataDrawer"
      ref="metadataDrawer"
      v-model="drawer"
      allow-expand
      show-overlay
      right>
      <template slot="title">
        <div class="d-flex my-auto font-weight-bold text-color">
          {{ $t('notes.metadata.properties.label') }}
        </div>
      </template>
      <template slot="content">
        <div class="pa-5">
          <v-form>
            <label for="image-area">
              <p class="text-color mb-3">
                {{ $t('notes.metadata.featuredImage.label') }}
              </p>
              <v-sheet
                name="image-area"
                class="add-image-area d-flex"
                height="95"
                @click="openFeaturedImageDrawer">
                <div class="d-flex width-fit-content mx-auto">
                  <v-icon
                    class="me-15"
                    size="40">
                    fas fa-image
                  </v-icon>
                  <p class="text-header text-color my-auto">
                    {{ $t('notes.metadata.featuredImage.add.label') }}
                  </p>
                </div>
              </v-sheet>
            </label>
            <label for="summaryInputEditor">
              <p class="text-color mb-3 mt-5">
                {{ $t('notes.metadata.featuredImage.label') }}
              </p>
              <v-progress-circular
                v-if="!editor"
                :width="3"
                indeterminate
                class="absolute-all-center z-index-one" />
              <note-rich-editor-input
                ref="summaryInputEditor"
                name="summaryInputEditor"
                toolbar-location="bottom"
                editor-input-ref="noteSummaryEditor"
                :placeholder="$t('notes.metadata.add.summary.placeholder')"
                :custom-config-url="customConfigUrl"
                :editor-events-callback="editorEventsCallback"
                @editor-ready="editorReady"
                @update-data="updateData" />
            </label>
          </v-form>
        </div>
      </template>
      <template slot="footer">
        <div class="d-flex width-fit-content ms-auto">
          <v-btn
            class="btn me-5"
            @click="save">
            {{ $t('notes.button.cancel') }}
          </v-btn>
          <v-btn
            class="btn btn-primary"
            @click="close">
            {{ $t('notes.button.publish') }}
          </v-btn>
        </div>
      </template>
    </exo-drawer>
    <image-crop-drawer
      ref="featuredImageDrawer"
      :drawer-title="$t('notes.metadata.featuredImage.edit.label')"
      :can-upload="true"
      back-icon="fas fa-arrow-left"
      :src="imageCropperSrc"
      :max-file-size="maxFileSize"
      :crop-options="cropOptions"
      @input="uploadId = $event"
      @data="imageData = $event"
      @format="format = $event" />
  </div>
</template>

<script>
export default {
  data() {
    return {
      noteObject: null,
      drawer: false,
      editor: null,
      summaryContent: null,
      customConfigUrl: `${eXo?.env?.portal.context}/${eXo?.env?.portal.rest}/richeditor/configuration?type=notesSummaryInput&v=${eXo.env.client.assetsVersion}`,
      imageItem: null,
      uploadId: null,
      maxFileSize: 20971520,
      format: 'landscape',
      imageData: null,
      cropOptions: {
        aspectRatio: 8,
        viewMode: 1
      }
    };
  },
  computed: {
    imageCropperSrc() {
      let imageSrc = this.imageItem?.src || '';
      if (imageSrc.length) {
        imageSrc = imageSrc.split('?')[0];
      }
      return imageSrc;
    },
  },
  methods: {
    initInputEditor() {
      this.$refs.summaryInputEditor.initCKEditor();
    },
    editorReady(editor) {
      this.editor = editor;
    },
    updateData(content) {
      this.summaryContent =  content;
    },
    editorEventsCallback(component) {
      return {
        instanceReady: function (evt) {
          component.editor = evt.editor;
          component.instanceReady = true;
        },
        change: function (evt) {
          component.content = evt.editor.getData();
        },
      };
    },
    openFeaturedImageDrawer() {
      this.$refs.featuredImageDrawer.open();
    },
    closeFeaturedImageDrawer() {
      this.$refs.featuredImageDrawer.close();
    },
    closeDrawersByOverlay() {
      if (!this.isImageDrawerClosed()) {
        this.closeFeaturedImageDrawer();
        return;
      }
      this.close();
    },
    open(note) {
      this.noteObject = note;
      setTimeout(() => {
        this.initInputEditor();
      }, 50);
      this.$refs.metadataDrawer.open();
    },
    close() {
      this.$refs.metadataDrawer.close();
    },
    isImageDrawerClosed() {
      return this.$refs.featuredImageDrawer.$el.classList.contains('v-navigation-drawer--close');
    },
    save() {
      // TODO
      this.close();
    }
  }
};
</script>
