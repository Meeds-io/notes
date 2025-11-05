<template>
  <div 
    v-if="isDesktop"
    class="note-breadcrumb-wrapper"
    role="navigation" 
    aria-label="Breadcrumbs">
    <div class="notes-tree-items d-flex align-center">
      <template v-for="(breadcrumbItem, index) in noteBreadcrumbList">
        <div 
          v-if="breadcrumbItem.isBreadcrumbItem"
          :key="index"
          :class="breadcrumbItem.class">
          <v-tooltip max-width="300" bottom>
            <template #activator="{ on, filteredAttrs }">
              <a
                :id="breadcrumbItem.id"
                :ref="breadcrumbItem.id"
                :class="breadcrumbItem.classLink"
                :aria-current="breadcrumbItem.id === 'lastBreadcrumbItem' ? 'page' : null"
                v-bind="filteredAttrs"
                v-on="on"
                tabindex="0"
                @click="openNote(noteBreadcrumb[breadcrumbItem.index])"
                @keydown="openNoteOnEnter($event, noteBreadcrumb[breadcrumbItem.index])">{{ breadcrumbItem.title }}
              </a>
            </template>
            <span class="caption">{{ breadcrumbItem.title }}</span>
          </v-tooltip>
          <v-icon
            v-if="breadcrumbItem.isIcon"
            class="flex-grow-1 icon-default-color flex-shrink-0"
            :class="index === 0 && noteEllipsisList.length > 0 ? 'ms-2' : 'mx-2'"
            size="18">
            fas fa-chevron-right
          </v-icon>
        </div>
        <div
          v-if="breadcrumbItem.isEllipsis && noteEllipsisList.length > 0"
          :key="index"
          class="notes-tree-item min-width-content long-path-second-item d-flex">
          <v-tooltip :key="tooltipKey" bottom>
            <template #activator="{ on, attrs }">
              <v-icon
                v-show="noteEllipsisList.length > 0"
                v-bind="attrs"
                v-on="on"
                class="text-sub-title"
                size="18"
                @click="openNote(noteBreadcrumb[noteEllipsisList[noteEllipsisList.length-1].index])"
                @keydown="openNoteOnEnter($event, noteBreadcrumb[noteEllipsisList[noteEllipsisList.length-1].index])">
                fas fa-ellipsis-h
              </v-icon>
            </template>
            <p
              v-for="(noteEllipsisItem) in noteEllipsisList"
              :key="noteEllipsisItem.index"
              class="mb-0">
              <span
                class="caption">
                <v-icon
                  size="18"
                  class="tooltip-chevron">
                  fas fa-chevron-right
                </v-icon>
                {{ noteEllipsisItem.title }}
              </span>
            </p>
          </v-tooltip>
          <v-icon class="clickable me-2" size="18">fas fa-chevron-right</v-icon>
        </div>
      </template>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    noteBreadcrumb: {
      type: Array,
      default: () => null
    },
    actualNoteId: {
      type: String,
      default: ''
    },
  },
  computed: {
    isDesktop() {
      return this.$vuetify.breakpoint.width >= 960;
    },
    filteredAttrs() {
      const attrs = { ...(this.attrs || {}) };
      delete attrs.role;
      return attrs;
    }
  },
  watch: {
    noteBreadcrumb() {
      this.initNoteBreadcrumb();
      this.$forceUpdate();
      this.isMounted = true;
    },
    isMounted() {
      if (this.isMounted) {
        document.fonts.ready.then(() => {
          this.$nextTick(() => {
            if (this.noteEllipsisList.length === 0 || this.noteBreadcrumb.length > 4) {
              this.isLastNoteTruncated();
              this.countRecursive = 0;
            }
          });
        });
        this.isMounted = false;
      }
    },
  },
  data: () => ({
    noteBreadcrumbList: [],
    noteEllipsisList: [],
    isMounted: false,
    tooltipKey: 0,
    countRecursive: 0,
  }),
  methods: {
    openNote(note) {
      if (note.noteId !== this.actualNoteId ) {
        this.noteBreadcrumb = [];
        this.$emit('open-note',note.id);
        document.dispatchEvent(new CustomEvent('note-navigation-updated', {detail: note}));
      }
    },
    openNoteOnEnter(event, note) {
      if (event.key === 'Enter') {
        this.openNote(note);
      }
    },
    initNoteBreadcrumb() {
      const noteBreadcrumbLength = this.noteBreadcrumb.length;
      this.noteBreadcrumbList = [];
      this.noteEllipsisList = [];
      this.noteBreadcrumb.forEach((note, index) => {
        const isFirst = index === 0;
        const isLast = index === noteBreadcrumbLength - 1;
        const isSecondLast = index === noteBreadcrumbLength - 2;
        const isEllipsis = noteBreadcrumbLength > 4 && this.noteEllipsisList.length === 0 && !isFirst;
        const isBreadcrumbItem = isFirst || isLast || isSecondLast || noteBreadcrumbLength <= 4;
        const breadcrumbItem = {
          id: isLast ? 'lastBreadcrumbItem' : null,
          index,
          title: note.title,
          class: isLast ? 'd-flex text-truncate mx-2' : 'd-flex notes-tree-item min-width-content',
          classLink: isLast ? 'caption text-color text-truncate breadCrumb-link' : 'caption text-sub-title path-clickable breadCrumb-link',
          isIcon: isFirst && noteBreadcrumbLength !== 1  || !isLast,
          isEllipsis,
          isBreadcrumbItem,
        };
        if (noteBreadcrumbLength > 4 && !isFirst && !isSecondLast && !isLast) {
          this.noteEllipsisList.push(breadcrumbItem);
        }
        this.noteBreadcrumbList.push(breadcrumbItem);
      });
    },
    getTextWidth(text, font) {
      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      context.font = font;
      return context.measureText(text).width;
    },
    isLastNoteTruncated() {
      document.fonts.ready.then(() => {
        this.$nextTick(() => {
          const lastNoteElement = document.getElementById('lastBreadcrumbItem');
          const noteBreadcrumbListLength = this.noteBreadcrumbList.length;
          if (this.countRecursive === 2){
            this.noteBreadcrumbList[noteBreadcrumbListLength - 1].class = 'd-flex text-truncate min-width-title';
            return;
          }
          if (!lastNoteElement) {
            return;
          }
          const elementWidth = lastNoteElement.clientWidth;
          const text = this.getTruncatedText(lastNoteElement);
          const font = window.getComputedStyle(lastNoteElement).font;
          const textWidth = this.getTextWidth(lastNoteElement.innerText, font);
          if (textWidth > elementWidth && text.length <= 10) {
            if (this.noteBreadcrumb.length < 5 && this.countRecursive === 0) {
              this.getNoteBreadcrumbList();
            } else if (this.countRecursive === 0) {
              const beforeLastIndex = noteBreadcrumbListLength - 2;
              const beforeLastElement = this.noteBreadcrumbList[beforeLastIndex];
              beforeLastElement.isBreadcrumbItem = false;
              beforeLastElement.isIcon = false;
              beforeLastElement.isEllipsis = this.noteEllipsisList.length === 0 ? true : false;
              this.noteEllipsisList.push(beforeLastElement);
              this.noteBreadcrumbList = this.noteBreadcrumbList.map((item, index) => {
                return index === beforeLastIndex ? beforeLastElement : item;
              });
            } else if (this.countRecursive === 1) {
              this.noteBreadcrumbList[0].class = 'd-flex notes-tree-item text-truncate';
              this.noteBreadcrumbList[0].classLink = 'caption text-sub-title text-truncate path-clickable breadCrumb-link';
            }
            this.isLastNoteTruncated();
            this.countRecursive += 1;
          } else {
            this.noteBreadcrumbList[noteBreadcrumbListLength - 1].class = 'd-flex text-truncate min-width-title';
          }
        });
      });
    },
    getTruncatedText(element) {
      const text = element.innerText;
      const style = window.getComputedStyle(element);
      const font = `${style.fontSize} ${style.fontFamily}`;
      const canvas = document.createElement('canvas');
      const context = canvas.getContext('2d');
      context.font = font;
      let truncatedText = '';
      const visibleWidth = element.clientWidth;
      for (let i = 0; i < text.length; i++) {
        const char = text[i];
        const charWidth = context.measureText(truncatedText + char).width;
        if (charWidth > visibleWidth) {
          break;
        }
        truncatedText += char;
      }
      return truncatedText;
    },
    getNoteBreadcrumbList() {
      this.noteBreadcrumbList = [];
      this.noteEllipsisList = [];
      const noteBreadcrumbLength = this.noteBreadcrumb.length;
      this.noteBreadcrumb.forEach((note, index) => {
        const isFirst = index === 0;
        const isLast = index === noteBreadcrumbLength - 1;
        const breadcrumbItem = {
          id: isLast ? 'lastBreadcrumbItem' : null,
          index,
          title: note.title,
          class: isLast ? 'd-flex text-truncate' : 'd-flex notes-tree-item min-width-content',
          classLink: isLast ? 'caption text-color text-truncate breadCrumb-link' : 'caption text-sub-title path-clickable breadCrumb-link',
          isIcon: isFirst,
          isEllipsis: !isFirst && !isLast && this.noteEllipsisList.length === 0,
          isBreadcrumbItem: isFirst || isLast,
        };
        if (!isFirst && !isLast) {
          this.noteEllipsisList.push(breadcrumbItem);
        }
        this.noteBreadcrumbList.push(breadcrumbItem);
      });
      this.tooltipKey += 1;
    },
  }
};
</script>
