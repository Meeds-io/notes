<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2023 Meeds Association contact@meeds.io

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
  <div
    :class="largeToolbar && 'large-toolbar'"
    class="richEditor">
    <v-progress-circular
      v-if="!instanceReady"
      :width="3"
      indeterminate
      class="absolute-all-center z-index-one" />
    <textarea
      ref="editor"
      v-model="content"
      :id="instanceId"
      :placeholder="placeholder"
      :class="!instanceReady && 'filter-blur-3'">
    </textarea>
  </div>
</template>
<script>
export default {
  props: {
    value: {
      type: String,
      default: null,
    },
    instanceId: {
      type: String,
      default: null,
    },
    placeholder: {
      type: String,
      default: null,
    },
    toolbarLocation: {
      type: String,
      default: null,
    },
    largeToolbar: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({
    content: null,
    editor: null,
    instanceReady: false,
    oembedMinWidth: 300,
  }),
  watch: {
    content() {
      if (this.instanceReady) {
        this.$emit('input', this.$noteUtils.getContentToSave(this.instanceId, this.oembedMinWidth));
      }
    },
    instanceReady() {
      if (this.instanceReady) {
        this.focus();
        this.$emit('ready');
      } else {
        this.$emit('unloaded');
      }
    },
  },
  created() {
    // Load CKEditor only when needed
    window.require(['SHARED/jquery', 'SHARED/commons-editor', 'SHARED/suggester', 'SHARED/tagSuggester']);
  },
  mounted() {
    this.content = this.$noteUtils.getContentToEdit(this.value);
    this.init();
  },
  beforeDestroy() {
    if (this.editor?.destroy) {
      return this.editor.destroy();
    }
  },
  methods: {
    init() {
      const self = this;
      window.require(['SHARED/jquery', 'SHARED/commons-editor', 'SHARED/suggester', 'SHARED/tagSuggester'], ($) => {
        CKEDITOR.dtd.$removeEmpty['i'] = false;
        CKEDITOR.on('dialogDefinition', function (e) {
          if (e.data.name === 'link') {
            const informationTab = e.data.definition.getContents('target');
            const targetField = informationTab.get('linkTargetType');
            targetField['default'] = '_self';
            targetField.items = targetField.items.filter(t => ['_self', '_blank'].includes(t[1]));
          }
        });
        CKEDITOR.basePath = '/commons-extension/ckeditor/';
        $(this.$refs.editor).ckeditor({
          customConfig: `${eXo.env.portal.context}/${eXo.env.portal.rest}/richeditor/configuration?type=notePageInline&v=${eXo.env.client.assetsVersion}`,
          allowedContent: true,
          toolbarLocation: this.toolbarLocation,
          extraAllowedContent: 'table[summary];img[style,class,src,referrerpolicy,alt,width,height];span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*];',
          removeButtons: '',
          enterMode: CKEDITOR.ENTER_P,
          shiftEnterMode: CKEDITOR.ENTER_BR,
          copyFormatting_allowedContexts: true,
          isImagePasteBlocked: true,
          hideUploadImageLink: true,
          isImageDragBlocked: true,
          indentBlock: {
            offset: 40,
            unit: 'px'
          },
          format_tags: 'p;h1;h2;h3',
          bodyClass: 'notesContent',
          dialog_noConfirmCancel: true,
          pasteFilter: 'p; a[!href]; strong; i', 
          on: {
            instanceReady: function (evt) {
              self.editor = evt.editor;
              self.instanceReady = true;
            },
            change: function(evt) {
              self.content = evt.editor.getData();
            },
          }
        });
      });
    },
    focus() {
      if (this.editor) {
        this.editor.focus();
      }
    },
  }
};
</script>
