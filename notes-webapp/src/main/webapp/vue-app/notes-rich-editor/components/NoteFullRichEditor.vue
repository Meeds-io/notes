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
      :post-key="postKey + enablePostKeys"
      :draft-saving-status="draftSavingStatus"
      :publish-button-text="publishButtonText"
      :lang-button-tooltip-text="langButtonTooltipText"
      :web-page-url="webPageUrl"
      :editor-icon="editorIcon"
      :save-button-icon="saveButtonIcon"
      :save-button-disabled="saveNoteButtonDisabled"
      :editor-ready="!!editor"
      @editor-closed="editorClosed"
      @post-note="postNote"
      @open-metadata-drawer="openMetadataDrawer" />
    <div class="notes-editor-body-section">
      <div class="notes-editor-body-section-content">
        <form class="notes-content">
          <div class="notes-content-form">
            <div
              v-show="!webPageNote"
              class="formInputGroup title notesTitle white px-5 pt-5 ">
              <input
                id="notesTitle"
                :ref="editorTitleInputRef"
                v-model="noteObject.title"
                :placeholder="titlePlaceholder"
                type="text"
                :maxlength="noteTitleMaxLength + 1"
                class="title text-color ma-0 pa-0"
                @input="waitUserTyping()"
                @keydown.enter.prevent="focusEditor">
            </div>
            <div class="formInputGroup white overflow-auto flex notes-content-wrapper px-5 pb-5">
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
      </div>
    </div>
    <extension-registry-components
      v-if="editorExtensions.length > 0"
      name="NotesRichEditor"
      type="notes-editor-extensions"
      :params="extensionParams" />
    <note-editor-metadata-drawer
      ref="editorMetadataDrawer"
      :has-featured-image="hasFeaturedImage"
      @metadata-updated="metadataUpdated" />
    <note-editor-featured-image-drawer
      ref="featuredImageDrawer"
      :note="noteObject"
      :has-featured-image="hasFeaturedImage" />
    <note-publication-drawer
      v-if="publicationParams"
      ref="editorPublicationDrawer"
      :has-featured-image="hasFeaturedImage"
      :is-publishing="isPublishing"
      :params="publicationParams"
      :edit-mode="editMode"
      @publish="postAndPublishNote"
      @metadata-updated="metadataUpdated"
      @closed="publicationDrawerClosed" />
    <note-publication-target-drawer />
  </div>
</template>

<script>
export default {
  data() {
    return {
      noteObject: null,
      editor: null,
      noteContentInitialized: false,
      instanceReady: false,
      noteTitleMaxLength: 500,
      typingTimer: null,
      isUserTyping: false,
      editorExtensions: [],
      updatingProperties: false,
      enablePostKeys: 0,
      isPublishing: false,
      contentImageUploadProgress: false,
      pendingEditorContent: null,
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
    suggesterSpaceUrl: {
      type: String,
      default: null
    },
    suggesterSpaceId: {
      type: String,
      default: null
    },
    suggesterSpacePrettyName: {
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
    },
    saveButtonDisabled: {
      type: Boolean,
      default: true
    },
    imagesDownloadFolder: {
      type: String,
      default: 'DRIVE_ROOT_NODE/notes/images'
    },
    publicationParams: {
      type: Object,
      default: null
    }
  },
  watch: {
    'noteObject.title': function(newVal, oldVal) {
      this.displayNoteTitleMaxLengthCheckAlert(newVal, oldVal);
      this.updateData();
    },
    'noteObject.content': function () {
      if (this.noteContentInitialized) {
        this.updateData();
      }
    },
    'note.properties': function () {
      this.cloneNoteObject();
    },
    'note.id': function () {
      this.cloneNoteObject();
    },
    'note.lang': function() {
      this.cloneNoteObject();
    },
    'note.title': function() {
      this.noteObject.title = this.note?.title;
    },
    instanceReady() {
      if (this.instanceReady) {
        this.$emit('editor-ready', this.editor);
        setTimeout(() => {
          this.bindNavigationRemoveListener();
        }, 1000);
      }
    }
  },
  computed: {
    newEmptyTranslation() {
      return !!this.note?.lang && !this.note?.title?.length && !this.note?.content?.length;
    },
    entityId() {
      return this.newEmptyTranslation ? null : this.note?.draftPage ? this.note?.id : this.note?.latestVersionId;
    },
    extensionParams() {
      return {
        spaceId: this.getURLQueryParam('spaceId'),
        entityId: this.entityId,
        entityType: this.note.draftPage && 'WIKI_DRAFT_PAGES' || 'WIKI_PAGE_VERSIONS',
        lang: this.note.lang,
        isEmptyNoteTranslation: this.newEmptyTranslation,
        name: this.note?.name
      };
    },
    hasFeaturedImage() {
      return !!this.noteObject?.properties?.featuredImage?.id;
    },
    saveNoteButtonDisabled() {
      return this.updatingProperties || this.saveButtonDisabled || this.isUserTyping;
    },
    newPageDraft() {
      return !this.noteObject?.id || (this.noteObject?.draftPage && !this.noteObject?.targetPageId);
    },
    editMode() {
      return this.noteObject?.id && !this.newPageDraft;
    },
    isTranslation() {
      return !!this.noteObject?.lang;
    },
    isContentImagesUploadProgress() {
      return this.contentImageUploadProgress;
    }
  },
  created() {
    this.cloneNoteObject();
    this.refreshEditorExtensions();
    this.$root.$on('include-page', this.includePage);
    this.$root.$on('update-note-title', this.updateTranslatedNoteTitle);
    this.$root.$on('update-note-content', this.updateTranslatedNoteContent);
    this.$root.$on('update-note-summary', this.updateTranslatedNoteSummary);
    this.$root.$on('close-featured-image-byOverlay', this.closeFeaturedImageDrawerByOverlay);

    document.addEventListener('notes-editor-upload-progress', () => this.contentImageUploadProgress = true);
    document.addEventListener('notes-editor-upload-done', () => this.contentImageUploadProgress = false);
    document.addEventListener('notes-extensions-updated', this.refreshEditorExtensions);
  },
  methods: {
    metadataUpdated(properties) {
      this.updatingProperties = true;
      this.noteObject.properties = properties;
      this.updateData();
      if (this.noteObject?.title?.length) {
        this.autoSave();
        this.waitForNoteMetadataUpdate();
      } else {
        this.updatingProperties = false;
      }
    },
    async refreshEditorExtensions() {
      await this.$utils.includeExtensions('RichEditorExtension');
      await this.$nextTick();
      this.editorExtensions = extensionRegistry.loadComponents('NotesRichEditor') || [];
    },
    editorClosed(){
      this.$emit('editor-closed');
    },
    updateTranslatedNoteTitle(title) {
      this.noteObject.title = title;
    },
    updateTranslatedNoteSummary(summary) {
      this.noteObject.properties.summary = summary;
    },
    updateTranslatedNoteContent(content) {
      this.noteObject.content = content;
      this.setEditorData(content);
    },
    hideTranslationsBar() {
      this.$root.$emit('hide-translations');
    },
    focusEditor() {
      this.editor.focus();
    },
    setEditorData(content) {
      content = this.replaceWithSuggesterClass(content || '');

      if (!this.editor || !this.instanceReady) {
        this.pendingEditorContent = content;
        return;
      }

      this.pendingEditorContent = null;
      this.editor.setData(content);
    },
    cloneNoteObject() {
      this.noteObject = structuredClone(this.note);
    },
    updateData() {
      if (this.instanceReady) {
        this.$emit('update-data', this.noteObject);
        this.hideTranslationsBar();
      }
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
            this.editor.insertHtml(this.createLinkElement(note.url, note.name, 'noteLink'));
          }
          if (editorSelectedElement.getAttribute('class') === 'labelLink') {
            const linkText = editorSelectedElement.getHtml();
            this.editor.getSelection().getStartElement().remove();
            this.editor.insertHtml(this.createLinkElement(note.url, linkText, 'noteLink'));
          }
        } else {
          const linkText = this.editor?.getSelection()?.getSelectedText();
          this.editor.insertHtml(this.createLinkElement(note.url, linkText, 'labelLink'));
        }
      } else {
        this.editor.insertHtml(this.createLinkElement(note.url, note.name, 'noteLink'));
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
    postNote() {
      if (this.publicationParams && !this.isTranslation && !this.editMode) {
        this.openPublicationDrawer(this.noteObject);
        return;
      }
      this.postAndPublishNote();
    },
    postAndPublishNote(publicationSettings, note) {
      if (this.publicationParams) {
        this.noteObject = note;
        this.updateData();
      }
      this.$emit('post-note', publicationSettings);
    },
    resetEditorData() {
      this.noteObject.title = null;
      if (this.noteObject?.properties) {
        this.noteObject.properties.featuredImage = null;
        this.noteObject.properties.summary = '';
      }
      this.editor.setData('');
    },
    async initCKEditor() {
      if (this.editor?.destroy) {
        this.editor.destroy(true);
      }
      await this.refreshEditorExtensions();

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
        typeOfRelation: 'mention_activity_stream',
        spaceURL: self.suggesterSpaceUrl,
        spacePrettyName: self.suggesterSpacePrettyName,
        spaceId: self.suggesterSpaceId,
        spaceGroupId: self.spaceGroupId,
        imagesDownloadFolder: self.imagesDownloadFolder,
        toolbarLocation: 'top',
        extraAllowedContent: 'table[summary];img[style,class,src,referrerpolicy,alt,width,height];span(*)[*]{*}; span[data-atwho-at-query,data-atwho-at-value,contenteditable]; a[*];i[*];',
        removeButtons: '',
        enterMode: CKEDITOR.ENTER_P,
        shiftEnterMode: CKEDITOR.ENTER_BR,
        copyFormatting_allowedContexts: true,
        autoParagraph: false,
        ckEditorType: 'notes',
        objectType: 'notes',
        objectId: this.entityId,
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
            const treeviewParentWrapper =  self.editor.window.$.document.getElementById('note-children-container');
            if ( treeviewParentWrapper ) {
              treeviewParentWrapper.contentEditable = 'false';
            }

            window.setTimeout(() => self.setFocus(), 50);
            self.$root.$applicationLoaded();
            self.instanceReady = true;
            self.$nextTick(() => self.checkPendingContent());
            self.setToolBarEffect();
            let isAttachedKeyListener = false;
            self.editor.on('contentDom', function () {
              isAttachedKeyListener = true;
              const editable = self.editor.editable();
              self.attachKeyListener(editable);
            });
            if (!isAttachedKeyListener) {
              const editable = self.editor.editable();
              self.attachKeyListener(editable);
            }
          },
          change: function (evt) {
            if (!self.noteContentInitialized || self.isContentImagesUploadProgress) {
              // First time setting data
              if (evt.editor.checkDirty()) {
                self.noteContentInitialized = true;
              }
              return;
            }
            self.waitUserTyping(self);
            self.noteObject.content = evt.editor.getData();
            self.autoSave();
            self.bindNavigationRemoveListener();
          },
          paste: function (evt) {
            if (!self.noteContentInitialized) {
              // First time setting data
              self.noteContentInitialized = true;
              return;
            }
            self.noteObject.content = evt.data.dataValue;
            self.autoSave();
            self.bindNavigationRemoveListener();
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
    checkPendingContent() {
      if (this.pendingEditorContent !== null) {
        this.editor.setData(this.pendingEditorContent);
        this.pendingEditorContent = null;
      }
    },
    attachKeyListener(editable) {
      editable.attachListener(editable, 'keydown', function (event) {
        const domEvent = event.data.$;
        if (domEvent.ctrlKey && domEvent.shiftKey && domEvent.keyCode !== 16) {
          const synthetic = new KeyboardEvent('keydown', {
            key: domEvent.key,
            ctrlKey: true,
            shiftKey: true,
            bubbles: true
          });
          window.dispatchEvent(synthetic);
        }
      });
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
    closeFeaturedImageDrawerByOverlay() {
      if (!this.isImageDrawerClosed()) {
        this.$refs.featuredImageDrawer.close();
        return;
      }
      this.$refs.editorMetadataDrawer.close();
      this.$refs.editorPublicationDrawer.close();
    },
    isImageDrawerClosed() {
      return this.$refs.featuredImageDrawer.isClosed();
    },
    openPublicationDrawer() {
      this.$refs.editorPublicationDrawer.open(this.noteObject);
    },
    publicationDrawerClosed() {
      this.enablePostKeys ++;
    },
    openMetadataDrawer() {
      this.$refs.editorMetadataDrawer.open(this.noteObject);
    },
    displayAlert(detail) {
      document.dispatchEvent(new CustomEvent('alert-message', {detail: {
        alertType: detail?.type,
        alertMessage: detail?.message,
      }}));
    },
    displayNoteTitleMaxLengthCheckAlert(newTitle, oldTitle) {
      if (newTitle?.length > this.noteTitleMaxLength) {
        this.noteObject.title = oldTitle;
        this.displayAlert({
          type: 'warning',
          message: this.$t('notes.title.max.length.warning.message', {0: this.noteTitleMaxLength})
        });
      }
    },
    waitForNoteMetadataUpdate() {
      setTimeout(() => {
        this.updatingProperties = false;
      }, 1000);
    },
    setPublishing(publishing) {
      this.isPublishing = publishing;
    },
    getURLQueryParam(paramName) {
      const urlParams = new URLSearchParams(window.location.search);
      if (urlParams.has(paramName)) {
        return urlParams.get(paramName);
      }
    },
    waitUserTyping(component) {
      component ??= this;
      clearTimeout(component.typingTimer);
      component.isUserTyping = true;
      component.typingTimer = setTimeout(function () {
        component.isUserTyping = false;
      }, 1000);
    },
    bindNavigationRemoveListener() {
      const removeTreeviewBtn = this.editor.document.getById('remove-treeview');
      if (!removeTreeviewBtn) {
        return;
      }
      this.editor.editable().attachListener(removeTreeviewBtn, 'click', () => {
        const treeviewParentWrapper = this.editor.document.getById('note-children-container');
        if (treeviewParentWrapper) {
          treeviewParentWrapper.remove();
        }
        const newContent = this.editor.getData().trim();
        const isEmpty = !newContent || /^(<p>(&nbsp;|\s|<br\s*\/?>)*<\/p>)?$/.test(newContent);
        if (isEmpty) {
          this.editor.setData('<p><br>&nbsp;</p>', {
            callback: () => {
              this.editor.setReadOnly(false);
              const editable = this.editor.editable();
              const range = this.editor.createRange();
              const firstElement = editable.getChildren().getItem(0);
              range.moveToPosition(firstElement, CKEDITOR.POSITION_BEFORE_START);
              this.editor.getSelection().selectRanges([range]);
              this.noteObject.content = this.editor.getData();
              this.setFocus();
              this.editor.fire('contentDom', { type: 'contentChanged' });
            }
          });
        } else {
          this.noteObject.content = newContent;
          this.setFocus();
        }
      });
    },
    replaceWithSuggesterClass(message) {
      const tempdiv = $('<div class=\'temp\'/>').html(message || '');
      tempdiv.find('a[href*="/profile"].user-suggester')
        .each(function() {
          $(this).replaceWith(function() {
            return $('<span/>', {
              class: 'atwho-inserted',
              html: `<span class="exo-mention" contenteditable="false">${$(this).text()}<a data-cke-survive href="#" class="remove"><i data-cke-survive class="uiIconClose uiIconLightGray"></i></a></span>`
            }).attr('data-atwho-at-query', '@')
              .attr('data-atwho-at-value', $(this).attr('href').substring($(this).attr('href').lastIndexOf('/')+1))
              .attr('contenteditable', 'false');
          });
        });
      tempdiv.find('a.group-role-mention')
        .each(function() {
          const role = $(this).data('role');
          const identityId = $(this).data('identity-id');
          let icon;
          if (role === 'member') {
            icon = 'fa-users';
          } else if (role === 'manager') {
            icon = 'fa-user-cog';
          } else if (role === 'redactor') {
            icon = 'fa-user-edit';
          } else if (role === 'publisher') {
            icon = 'fa-paper-plane';
          }

          $(this).replaceWith(function() {
            return $('<span/>', {
              class: 'atwho-inserted',
              html: `<span class="exo-mention"><i aria-hidden="true" class="v-icon fa ${icon}" style="font-size: 16px;"></i>${$(this).text()}<a data-cke-survive href="#" class="remove"><i data-cke-survive class="uiIconClose uiIconLightGray"></i></a></span>`
            }).attr('data-atwho-at-query', '@')
              .attr('data-atwho-at-value',`${role}:${identityId}`)
              .attr('contenteditable','false');
          });
        });
      return tempdiv.html();
    },
  }
};
</script>
