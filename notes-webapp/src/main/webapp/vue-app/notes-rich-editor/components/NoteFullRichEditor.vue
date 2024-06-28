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
  <div
    id="notesEditor"
    class="notesEditor width-full">
    <note-editor-top-bar
      :note="note"
      :languages="languages"
      :form-title="formTitle"
      :note-id-param="noteIdParam"
      :web-page-note="webPageNote"
      :selected-language="selectedLanguage"
      :translations="translations"
      :is-mobile="isMobile"
      :post-key="postKey"
      :draft-saving-status="draftSavingStatus"
      :publish-button-text="publishButtonText"
      :lang-button-tooltip-text="langButtonTooltipText"
      :web-page-url="webPageUrl"
      :editor-icon="editorIcon"
      :save-button-icon="saveButtonIcon"
      @editor-closed="editorClosed"
      @post-note="postNote" />
    <form class="notes-content">
      <div class="notes-content-form singlePageApplication my-5 mx-auto py-1 px-5">
        <div
          v-show="!webPageNote"
          class="formInputGroup notesTitle white px-5">
          <input
            id="notesTitle"
            :ref="editorTitleInputRef"
            v-model="noteObject.title"
            :placeholder="titlePlaceholder"
            type="text"
            class="py-0 px-1 mt-5 mb-0">
        </div>
        <div class="formInputGroup white overflow-auto flex notes-content-wrapper">
          <textarea
            :id="editorBodyInputRef"
            :ref="editorBodyInputRef"
            :placeholder="bodyPlaceholder"
            :name="editorBodyInputRef"
            class="notesFormInput">
            </textarea>
        </div>
      </div>
    </form>
    <note-custom-plugins
      ref="noteCustomPlugins"
      :instance="editor" />
  </div>
</template>

<script>
export default {
  data() {
    return {
      noteObject: null,
      editor: null,
    };
  },
  props: {
    note: {
      type: Object,
      default: null
    },
    titlePlaceholder: {
      type: String,
      default: null
    },
    bodyPlaceholder: {
      type: String,
      default: null
    },
    languages: {
      type: Array,
      default: () => []
    },
    translations: {
      type: Array,
      default: () => []
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
    appName: {
      type: String,
      default: null
    },
    draftSavingStatus: {
      type: String,
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
    webPageNote: {
      type: Boolean,
      default: false
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
    editorBodyInputRef: {
      type: String,
      default: 'notesContent'
    },
    editorTitleInputRef: {
      type: String,
      default: 'noteTitle'
    },
    spaceUrl: {
      type: String,
      default: null
    },
    spaceGroupId: {
      type: String,
      default: null
    },
    editorIcon: {
      type: String,
      default: null
    },
    saveButtonIcon: {
      type: String,
      default: null
    }
  },
  watch: {
    'noteObject.title': function() {
      this.updateData();
    },
    'note.title': function () {
      this.noteObject.title = this.note?.title;
    }
  },
  created() {
    this.cloneNoteObject();
    this.$root.$on('include-page', this.includePage);
    this.$root.$on('update-note-title', this.updateTranslatedNoteTitle);
    this.$root.$on('update-note-content', this.updateTranslatedNoteContent);
    document.addEventListener('note-custom-plugins', this.openCustomPluginsDrawer);
  },
  methods: {
    editorClosed(){
      this.$emit('editor-closed');
    },
    updateTranslatedNoteTitle(title) {
      this.noteObject.title = title;
    },
    updateTranslatedNoteContent(content) {
      this.noteObject.content = content;
      this.setEditorData(content);
    },
    hideTranslationsBar() {
      this.$root.$emit('hide-translations');
    },
    setEditorData(content) {
      if (this.editor) {
        this.editor.setData(content);
      } else {
        this.$refs[this.editorBodyInputRef].value = content;
      }
    },
    cloneNoteObject() {
      this.noteObject = structuredClone(this.note);
    },
    updateData() {
      this.$emit('update-data', this.noteObject);
      this.hideTranslationsBar();
    },
    autoSave() {
      this.$emit('auto-save', this.noteObject);
    },
    createLinkElement(link, label, clazz) {
      return `<a href='${link}' class='${clazz}'>${label}</a>`;
    },
    includePage(note) {
      const editorSelectedElement = this.editor?.getSelection()?.getStartElement();
      if (this.editor?.getSelection()?.getSelectedText()) {
        if (editorSelectedElement.is('a')) {
          if (editorSelectedElement?.getAttribute('class') === 'noteLink') {
            this.editor.getSelection()?.getStartElement()?.remove();
            this.editor.insertHtml(this.createLinkElement(note.noteId, note.name, 'noteLink'));
          }
          if (editorSelectedElement.getAttribute('class') === 'labelLink') {
            const linkText = editorSelectedElement.getHtml();
            this.editor.getSelection().getStartElement().remove();
            this.editor.insertHtml(this.createLinkElement(note.noteId, linkText, 'noteLink'));
          }
        } else {
          const linkText = this.editor?.getSelection()?.getSelectedText();
          this.editor.insertHtml(this.createLinkElement(note.noteId, linkText, 'labelLink'));
        }
      } else {
        this.editor.insertHtml(this.createLinkElement(note.noteId, note.name, 'noteLink'));
      }
    },
    setFocus() {
      if (!this.noteIdParam) {
        this.$refs[this.editorTitleInputRef].focus();
      } else if (this.editor) {
        window.setTimeout(() => {
          this.$nextTick().then(() => this.editor.focus());
        }, 200);
      }
    },
    postNote(toPublish) {
      this.$emit('post-note', toPublish);
    },
    resetEditorData() {
      this.noteObject.title = null;
      this.editor.setData('');
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

      $(this.$refs[this.editorBodyInputRef]).ckeditor({
        customConfig: `${eXo?.env?.portal.context}/${eXo?.env?.portal.rest}/richeditor/configuration?type=notes&v=${eXo.env.client.assetsVersion}`,
        allowedContent: true,
        spaceURL: self.spaceURL,
        spaceGroupId: self.spaceGroupId,
        imagesDownloadFolder: 'DRIVE_ROOT_NODE/notes/images',
        toolbarLocation: 'top',
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
        isImagePasteBlocked: this.webPageNote,
        hideUploadImageLink: this.webPageNote,
        isImageDragBlocked: this.webPageNote,
        sharedSpaces: {
          top: 'notesTop'
        },
        on: {
          instanceReady: function (evt) {
            self.editor = evt.editor;
            self.$emit('editor-ready', self.editor);
            $(self.editor.document.$)
              .find('.atwho-inserted')
              .each(function() {
                $(this).on('click', '.remove', function() {
                  $(this).closest('[data-atwho-at-query]').remove();
                });
              });

            const treeviewParentWrapper =  self.editor.window.$.document.getElementById('note-children-container');
            if ( treeviewParentWrapper ) {
              treeviewParentWrapper.contentEditable='false';
            }

            const removeTreeviewBtn =  evt.editor.document.getById( 'remove-treeview' );
            if ( removeTreeviewBtn ) {
              evt.editor.editable().attachListener( removeTreeviewBtn, 'click', function() {
                const treeviewParentWrapper = evt.editor.document.getById( 'note-children-container' );
                if ( treeviewParentWrapper) {
                  treeviewParentWrapper.remove();
                  self.noteObject.content = evt.editor.getData();
                }
                self.setFocus();
              });
            }
            window.setTimeout(() => self.setFocus(), 50);
            self.$root.$applicationLoaded();
            self.instanceReady = true;
            self.setToolBarEffect();
          },
          change: function (evt) {
            if (!self.initialized) {
              // First time setting data
              self.initialized = true;
              return;
            }
            self.noteObject.content = evt.editor.getData();
            self.autoSave();
            const removeTreeviewBtn =  evt.editor.document.getById( 'remove-treeview' );
            if (removeTreeviewBtn) {
              evt.editor.editable().attachListener(removeTreeviewBtn, 'click', function() {
                const treeviewParentWrapper = evt.editor.document.getById( 'note-children-container' );
                if ( treeviewParentWrapper) {
                  const newLine = treeviewParentWrapper.getNext();
                  treeviewParentWrapper.remove();
                  if ( newLine.$.innerText.trim().length === 0) {
                    newLine.remove();
                  }
                  self.noteObject.content = evt.editor.getData();
                }
              });
            }
            self.updateData();
          },
          fileUploadResponse: function() {
            /*add plugin fileUploadResponse to handle file upload response ,
              in this method we can get the response from server and update the editor content
              this method is called when file upload is finished*/
            self.editor.once('afterInsertHtml', ()=> {
              window.setTimeout(() => {
                self.editor.fire('mode');
              }, 2000);
            });
          },
          doubleclick: function(evt) {
            const element = evt.data.element;
            if ( element && element.is('a')) {
              const noteId = element.getAttribute('href');
              self.$emit('open-treeview', noteId, 'includePages', 'no-arrow');
            }
          }
        }
      });
    },
    openCustomPluginsDrawer() {
      this.$refs.noteCustomPlugins.open();
    },
    closePluginsDrawer() {
      this.$refs.noteCustomPlugins.close();
    },
    setToolBarEffect() {
      const elementNewTop = document.getElementById('notesTop');
      if (this.editor) {
        this.editor.on('contentDom', function () {
          this.document.on('click', function () {
            elementNewTop.classList.add('darkComposerEffect');
          });
        });
        this.editor.on('contentDom', function () {
          this.document.on('keyup', function () {
            elementNewTop.classList.add('darkComposerEffect');
          });
        });
      }
      const notesEditor = document.getElementById('notesEditor');
      notesEditor.parentElement.addEventListener('click', () => {
        elementNewTop.classList.remove('darkComposerEffect');
        elementNewTop.classList.add('greyComposerEffect');
      });
      notesEditor.parentElement.addEventListener('keyup', () => {
        elementNewTop.classList.remove('darkComposerEffect');
        elementNewTop.classList.add('greyComposerEffect');
      });
    },
  }
};
</script>
