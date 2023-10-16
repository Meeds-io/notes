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
  <div>
    <v-card
      v-if="fixedToolbar"
      :min-height="richEditorToolbarHeight"
      flat />
    <rich-editor
      v-if="initialized"
      ref="richEditor"
      v-model="pageContent"
      :placeholder="$t('notePageView.placeholder.editText')"
      :tag-enabled="false"
      :ck-editor-id="richEditorId"
      :toolbar-position="isSmall && 'bottom' || 'top'"
      :large-toolbar="!isSmall"
      ck-editor-type="notePageInline"
      focus-position="start"
      autofocus
      hide-chars-count
      disable-auto-grow
      oembed
      @ready="$root.$emit('notes-editor-ready')"
      @unloaded="$root.$emit('notes-editor-unloaded')" />
    <div
      ref="extraButtons"
      :class="{
        'r-0': !$vuetify.rtl && !displayFixedToolbar,
        'l-0': $vuetify.rtl && !displayFixedToolbar,
        'position-absolute z-index-two t-0': !isSmall && !displayFixedToolbar,
        'z-index-two': displayFixedToolbar,
      }"
      class="ms-auto me-2 my-2 d-flex align-center justify-end">
      <v-tooltip bottom>
        <template #activator="{on, bind}">
          <v-btn
            v-on="on"
            v-bind="bind"
            :href="fullPageEditorLink"
            :disabled="saving"
            class="me-3"
            target="_blank"
            icon
            @mousedown="openFullPageEditor"
            @click="cancelEditing">
            <v-icon size="20">fa-external-link-alt</v-icon>
          </v-btn>
        </template>
        <span>{{ $t('notePageView.label.openFullPageEditor') }}</span>
      </v-tooltip>
      <v-btn
        :title="$t('notePageView.label.cancel')"
        :loading="saving"
        class="btn me-4"
        @click="$emit('cancel')">
        {{ $t('notePageView.label.cancel') }}
      </v-btn>
      <v-btn
        :title="$t('notePageView.label.save')"
        :loading="saving"
        class="btn primary me-2"
        @click="save(true)">
        {{ $t('notePageView.label.save') }}
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  data: () => ({
    pageContent: null,
    richEditorId: `notePageInline${parseInt(Math.random() * 10000)}`,
    saving: false,
    initialized: false,
    siteBody: document.querySelector('#UISiteBody') || document.querySelector('#UIPageBody'),
    topbarHeight: document.querySelector('#UITopBarContainer')?.offsetHeight || 0,
    checkToolbarPosition: false,
    editorTop: 0,
    editorWidth: 0,
    editorX: 0,
    timeout: null,
    fixedToolbar: false,
  }),
  computed: {
    note() {
      return this.$root.page;
    },
    parentPageId() {
      return this.note?.parentPageId;
    },
    fullPageEditorLink() {
      const formData = new FormData();
      formData.append('noteId', this.$root.page?.id);
      formData.append('parentNoteId', this.parentPageId);
      if (eXo.env.portal?.spaceGroup) {
        formData.append('spaceGroupId', eXo.env.portal?.spaceGroup);
      }
      formData.append('pageName', document.title);
      formData.append('isDraft', 'false');
      formData.append('showMaxWindow', 'true');
      formData.append('hideSharedLayout', 'true');
      formData.append('webPageNote', 'true');
      const urlParams = new URLSearchParams(formData).toString();
      return `${eXo.env.portal.context}/${eXo.env.portal.portalName}/notes-editor?${urlParams}`;
    },
    isSmall() {
      return this.$root.isSmall;
    },
    ckEditorId() {
      return `cke_${this.richEditorId}`;
    },
    richEditorElement() {
      return this.$refs?.richEditor?.$el;
    },
    richEditorToolbarElement() {
      return this.richEditorElement?.querySelector?.('.cke_top');
    },
    richEditorToolbarExtraButtonsElement() {
      return this.$refs?.extraButtons;
    },
    richEditorToolbarHeight() {
      return this.richEditorToolbarExtraButtonsElement?.offsetHeight;
    },
    displayFixedToolbar() {
      return !this.isSmall && this.editorTop <= this.topbarHeight && (this.editorTop + this.editorHeight) > (this.topbarHeight * 2);
    },
  },
  watch: {
    displayFixedToolbar(newVal, oldVal) {
      if (this.richEditorToolbarElement && !this.isSmall && !!newVal === !oldVal) {
        this.setFixedPosition(this.richEditorToolbarElement, newVal);
        this.setFixedPosition(this.richEditorToolbarExtraButtonsElement, newVal, true);
        this.fixedToolbar = this.displayFixedToolbar;
      }
    },
    editorWidth() {
      if (this.displayFixedToolbar) {
        this.setFixedPosition(this.richEditorToolbarElement, true);
        this.setFixedPosition(this.richEditorToolbarExtraButtonsElement, true, true);
        this.fixedToolbar = this.displayFixedToolbar;
      }
    },
    checkToolbarPosition() {
      if (this.checkToolbarPosition) {
        if (this.timeout) {
          window.clearTimeout(this.timeout);
        }
        this.timeout = window.setTimeout(() => {
          this.checkToolbarPosition = false;
          this.computeTopbarPosition();
        }, 50);
      }
    },
  },
  created() {
    this.init();
  },
  mounted() {
    if (!this.isSmall) {
      this.siteBody.addEventListener('scroll', this.controlBodyScrollClass, false);
      window.addEventListener('resize', this.controlBodyScrollClass, false);
      this.$root.$on('notes-editor-ready', this.controlBodyScrollClass);
    }
  },
  beforeDestroy() {
    this.siteBody.removeEventListener('scroll', this.controlBodyScrollClass, false);
    window.removeEventListener('resize', this.controlBodyScrollClass, false);
    this.$root.$off('notes-editor-ready', this.controlBodyScrollClass);
  },
  methods: {
    init() {
      this.pageContent = this.$root.pageContent || '';
      if (this.$root.pageId) {
        this.initialized = true;
      } else {
        return this.save()
          .finally(() => this.initialized = true);
      }
    },
    focus() {
      this.$refs.richEditor?.setFocus?.(true);
    },
    save(emitEvent) {
      this.saving = true;
      return this.$notePageViewService.saveNotePage(this.$root.name, this.pageContent, this.$root.language)
        .then(() => {
          this.$root.$emit('notes-refresh');
          if (emitEvent) {
            this.$emit('saved');
            this.$root.$emit('alert-message', this.$t('notePageView.label.savedSuccessfully') , 'success');
          }
        })
        .catch(() => {
          if (emitEvent) {
            this.$root.$emit('alert-message', this.$t('notePageView.label.errorSavingText') , 'error')
          }
        })
        .finally(() => this.saving = false);
    },
    openFullPageEditor() {
      this.persistDraftNote();
    },
    cancelEditing() {
      window.setTimeout(() => {
        this.$emit('cancel');
      }, 50);
    },
    persistDraftNote() {
      return this.$notesService.saveDraftNote({
        id: this.note.id,
        name: this.note.name,
        title: this.note.title,
        content: this.pageContent,
        wikiType: this.note.wikiType,
        wikiOwner: this.note.wikiOwner,
        parentPageId: this.note.parentPageId,
        targetPageId: this.note.id,
      }, this.note.parentPageId).then(savedDraftNote => {
        localStorage.setItem(`draftNoteId-${this.note.id}`, JSON.stringify(savedDraftNote));
      });
    },
    controlBodyScrollClass() {
      this.checkToolbarPosition = true;
    },
    computeTopbarPosition() {
      if (this.richEditorElement) {
        const clientRect = this.richEditorElement.getClientRects()[0];
        if (clientRect.top !== this.editorTop) {
          this.editorTop = clientRect.top;
        }
        this.editorWidth = clientRect.width;
        this.editorHeight = clientRect.height;
        this.editorX = clientRect.x;
        this.editorY = clientRect.y;
      }
    },
    setFixedPosition(element, fixed, right) {
      if (element) {
        if (fixed) {
          element.style.top = `${this.topbarHeight}px`;
          element.classList.add('white');
          element.classList.add('no-border-radius');
          element.style.position = 'fixed';
          if (right) {
            if (this.$vuetify.rtl) {
              element.style.right = `${(this.editorX + this.editorWidth - 1)}px`;
            } else {
              element.style.left = `${(this.editorX + this.editorWidth - 1)}px`;
            }
            element.style.transform = 'translateX(-100%)';
          } else {
            element.classList.add('elevation-1');
            element.style.width = `${this.editorWidth}px`;
            element.style.transform = 'translateX(-1px)';
          }
        } else {
          element.style.position = '';
          element.style.right = '';
          element.style.left = '';
          element.classList.remove('elevation-1');
          element.classList.remove('no-border-radius');
          element.classList.remove('white');
          element.style.transform = 'initial';
          element.style.top = 'initial';
          element.style.width = 'initial';
        }
      }
    },
  }
};
</script>