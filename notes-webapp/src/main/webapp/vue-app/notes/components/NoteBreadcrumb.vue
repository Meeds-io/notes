<template>
  <nav
    v-if="isDesktop"
    class="note-breadcrumb-wrapper"
    :aria-label="$t('notes.label.breadcrumbs')">
    <!-- WebKit drops the list role of a list whose bullets are hidden: it is stated again -->
    <!-- eslint-disable-next-line vuejs-accessibility/no-redundant-roles -->
    <ol class="notes-tree-items d-flex align-center pa-0 ma-0" role="list">
      <li
        v-for="item in breadcrumbItems"
        :key="item.key"
        :class="item.current && 'note-breadcrumb-current' || 'notes-tree-item'"
        class="d-flex align-center">
        <v-menu
          v-if="item.hiddenNotes"
          v-model="hiddenNotesMenuOpened"
          content-class="note-breadcrumb-hidden-notes"
          offset-y
          bottom>
          <template #activator="{ on, attrs }">
            <v-btn
              ref="hiddenNotesButton"
              v-bind="attrs"
              v-on="on"
              :aria-label="$t('notes.label.breadcrumb.hiddenParentNotes')"
              :title="$t('notes.label.breadcrumb.hiddenParentNotes')"
              class="text-sub-title breadcrumb-ellipsis-button"
              width="18"
              height="18"
              min-width="18"
              icon>
              <v-icon size="18" aria-hidden="true">
                fas fa-ellipsis-h
              </v-icon>
            </v-btn>
          </template>
          <v-list
            ref="hiddenNotesList"
            dense
            @keydown.native="moveFocusInHiddenNotes">
            <v-list-item
              v-for="note in item.hiddenNotes"
              :key="note.id"
              :href="noteHref(note)"
              @click.prevent="openNote(note)">
              <v-list-item-title class="caption">{{ note.title }}</v-list-item-title>
            </v-list-item>
          </v-list>
        </v-menu>
        <span
          v-else-if="item.current"
          :title="item.note.title"
          class="caption text-color text-truncate breadCrumb-link"
          aria-current="page">{{ item.note.title }}</span>
        <a
          v-else
          :href="noteHref(item.note)"
          :title="item.note.title"
          class="caption text-sub-title text-truncate path-clickable breadCrumb-link"
          @click.prevent="openNote(item.note)">{{ item.note.title }}</a>
        <v-icon
          v-if="!item.current"
          class="icon-default-color flex-shrink-0 mx-2"
          size="18"
          aria-hidden="true">
          fas fa-chevron-right
        </v-icon>
      </li>
    </ol>
  </nav>
</template>
<script>
import { getNoteUrl } from '../../notes-treeview/js/Utils.js';

const MAX_VISIBLE_LEVELS = 4;

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
  data: () => ({
    hiddenNotesMenuOpened: false,
  }),
  computed: {
    isDesktop() {
      return this.$vuetify.breakpoint.width >= 960;
    },
    breadcrumbItems() {
      const notes = this.noteBreadcrumb || [];
      const lastIndex = notes.length - 1;
      const toItem = (note, index) => ({
        key: note.id,
        note,
        current: index === lastIndex,
      });
      if (notes.length <= MAX_VISIBLE_LEVELS) {
        return notes.map(toItem);
      }
      return [
        toItem(notes[0], 0),
        {
          key: 'hiddenParentNotes',
          hiddenNotes: notes.slice(1, lastIndex - 1),
        },
        toItem(notes[lastIndex - 1], lastIndex - 1),
        toItem(notes[lastIndex], lastIndex),
      ];
    },
  },
  watch: {
    hiddenNotesMenuOpened(opened) {
      if (opened) {
        this.focusHiddenNote(0);
      }
    },
  },
  methods: {
    openNote(note) {
      this.hiddenNotesMenuOpened = false;
      if (note.noteId !== this.actualNoteId) {
        this.$emit('open-note', note.id);
        document.dispatchEvent(new CustomEvent('note-navigation-updated', {detail: note}));
      }
    },
    noteHref(note) {
      return getNoteUrl(note.id, false);
    },
    hiddenNoteItems() {
      const list = this.$refs.hiddenNotesList;
      const listElement = Array.isArray(list) ? list[0]?.$el : list?.$el;
      return listElement && Array.from(listElement.querySelectorAll('[role="menuitem"]')) || [];
    },
    focusHiddenNote(index, attempt = 0) {
      const items = this.hiddenNoteItems();
      const item = items.length && items[(index + items.length) % items.length];
      if (item) {
        item.focus();
      }
      // the menu content is shown a frame after it opens: an item cannot take the focus before
      if ((!item || document.activeElement !== item) && attempt < 10) {
        window.requestAnimationFrame(() => this.focusHiddenNote(index, attempt + 1));
      }
    },
    moveFocusInHiddenNotes(event) {
      // Vuetify 2.3 menus only highlight an item on the arrow keys: the focus has to move for a screen reader to read it
      const items = this.hiddenNoteItems();
      const current = items.indexOf(document.activeElement);
      if (event.key === 'ArrowDown') {
        this.focusHiddenNote(current + 1);
      } else if (event.key === 'ArrowUp') {
        this.focusHiddenNote(current - 1);
      } else if (event.key === 'Home') {
        this.focusHiddenNote(0);
      } else if (event.key === 'End') {
        this.focusHiddenNote(items.length - 1);
      } else if (event.key === 'Tab') {
        // the menu content is detached at the end of the page: Tab goes on from the "…" button, not from there
        this.hiddenNotesMenuOpened = false;
        const button = this.$refs.hiddenNotesButton;
        (Array.isArray(button) ? button[0] : button)?.$el?.focus();
        return;
      } else {
        return;
      }
      event.preventDefault();
      event.stopPropagation();
    },
  },
};
</script>
