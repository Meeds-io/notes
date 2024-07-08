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
  <textarea
    :id="editorInputRef"
    :ref="editorInputRef"
    :placeholder="placeholder"
    :name="editorInputRef"
    class="notesFormInput">
  </textarea>
</template>

<script>
export default {
  data() {
    return {
      content: null,
      editor: null,
      instanceReady: false,
    };
  },
  props: {
    customConfigUrl: {
      type: String,
      default: null
    },
    editorInputRef: {
      type: String,
      default: null
    },
    placeholder: {
      type: String,
      default: null
    },
    toolbarLocation: {
      type: String,
      default: 'top'
    },
    spaceUrl: {
      type: String,
      default: null
    },
    spaceGroupId: {
      type: String,
      default: null
    },
    isWebPageNote: {
      type: Boolean,
      default: false
    },
    editorEventsCallback: {
      type: Object,
      default: null
    }
  },
  watch: {
    content() {
      this.updateData();
    },
    instanceReady() {
      if (this.instanceReady) {
        this.$emit('editor-ready', this.editor);
      }
    },
  },
  methods: {
    updateData() {
      this.$emit('update-data', this.content);
    },
    resetEditorData() {
      this.editor.setData('');
    },
    setFocus() {
      window.setTimeout(() => {
        this.$nextTick().then(() => this.editor.focus());
      }, 200);
    },
    setEditorData(content) {
      if (this.editor) {
        this.editor.setData(content);
      } else {
        this.$refs[this.editorInputRef].value = content;
      }
    },
    initCKEditor: function() {
      if (this.editor?.destroy) {
        this.editor.destroy(true);
      }

      CKEDITOR.dtd.$removeEmpty['i'] = false;

      CKEDITOR.on('dialogDefinition', function (e) {
        if (e.data.name === 'link') {
          const informationTab = e.data.definition.getContents('target');
          const targetField = informationTab.get('linkTargetType');
          targetField['default'] = '_self';
          targetField.items = targetField.items.filter(t => ['_self', '_blank'].includes(t[1]));
        }
      });

      // this line is mandatory when a custom skin is defined
      CKEDITOR.basePath = '/commons-extension/ckeditor/';
      const self = this;

      $(this.$refs[this.editorInputRef]).ckeditor({
        customConfig: self.customConfigUrl,
        allowedContent: true,
        spaceURL: self.spaceURL,
        spaceGroupId: self.spaceGroupId,
        imagesDownloadFolder: 'DRIVE_ROOT_NODE/notes/images',
        toolbarLocation: self.toolbarLocation,
        extraAllowedContent: 'table[summary];img[style,class,src,referrerpolicy,alt,width,height];span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*];',
        removeButtons: '',
        enterMode: CKEDITOR.ENTER_P,
        shiftEnterMode: CKEDITOR.ENTER_BR,
        copyFormatting_allowedContexts: true,
        indentBlock: {
          offset: 40,
          unit: 'px'
        },
        format_tags: 'p;h1;h2;h3',
        bodyClass: 'notesContent',
        dialog_noConfirmCancel: true,
        colorButton_enableMore: true,
        isImagePasteBlocked: self.isWebPageNote,
        hideUploadImageLink: self.isWebPageNote,
        isImageDragBlocked: self.isWebPageNote,
        sharedSpaces: {
          top: 'notesTop'
        },
        on: self.editorEventsCallback(self)
      });
    },
  }
};
</script>
