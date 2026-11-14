<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2026 Meeds Association contact@meeds.io

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
  <v-btn
    v-if="canAskAi"
    :aria-label="$t('notes.metadata.askAi.label')"
    :title="$t('notes.metadata.askAi.label')"
    :loading="opening"
    :disabled="!hasNoteText"
    class="note-properties-ask-ai ms-auto"
    small
    text
    @click.prevent.stop="askAi">
    <v-icon
      class="icon-default-color me-1"
      size="16">
      fa-concierge-bell
    </v-icon>
    <span class="text-body text-color">{{ $t('notes.metadata.askAi.label') }}</span>
  </v-btn>
</template>

<script>
import {
  PROPERTIES_APPLICATION_ID,
  PROPERTIES_EXTENSION_ID,
  getBindings,
  isSpaceSharedWithAssistant,
  openAssistant
} from '../../js/NoteAssistant.js';

/**
 * « Ask AI » in the header of a note's own Properties: opens the assistant
 * with every enabled binding of the Properties' place, on the note's content
 * as it is on screen. A text answer is inserted in the Summary field, through
 * the drawer; Save then saves it.
 */
export default {
  props: {
    /** The note the drawer shows, with the content being written. */
    note: {
      type: Object,
      default: null
    },
    /** The note's page id, none for a note never saved. */
    pageId: {
      type: [Number, String],
      default: null
    },
    /** The note's space, none outside spaces. */
    spaceId: {
      type: [Number, String],
      default: null
    },
    /** Whether the caller knows the note's space; false while it is read or when it could not be. */
    spaceResolved: {
      type: Boolean,
      default: true
    },
    /** Whether the drawer opened read-only. */
    readOnly: {
      type: Boolean,
      default: false
    },
    /** Whether the drawer is the note's own Properties, the only one that offers the assistant. */
    fillFromContent: {
      type: Boolean,
      default: false
    }
  },
  data: () => ({
    bindings: [],
    load: 0,
    opening: false,
  }),
  computed: {
    /**
     * @returns {boolean} whether « Ask AI » is offered: the bindings read
     *          last are kept only in the note's own editable Properties, with
     *          the space shared with the assistant, and cleared at once when
     *          any of these changes
     */
    canAskAi() {
      return this.bindings.length > 0;
    },
    /**
     * @returns {boolean} whether the note holds text for the assistant to
     *          read: an empty note would hand it nothing, so the button
     *          stays disabled until something is written
     */
    hasNoteText() {
      return !!this.$utils.htmlToText(this.note?.content || '').trim();
    },
  },
  watch: {
    /**
     * Reads again what the assistant offers when the note's space changes.
     *
     * @returns {void}
     */
    spaceId() {
      this.loadBindings();
    },
    /**
     * Reads again what the assistant offers once the note's space is known.
     *
     * @returns {void}
     */
    spaceResolved() {
      this.loadBindings();
    },
    /**
     * Reads again what the assistant offers when the drawer turns editable
     * or read-only.
     *
     * @returns {void}
     */
    readOnly() {
      this.loadBindings();
    },
    /**
     * Reads again what the assistant offers when the caller turns it on or
     * off.
     *
     * @returns {void}
     */
    fillFromContent() {
      this.loadBindings();
    },
  },
  /**
   * Reads what the assistant offers when the header is first shown.
   *
   * @returns {void}
   */
  created() {
    this.loadBindings();
  },
  methods: {
    /**
     * Reads the bindings of the Properties' place: none outside the note's
     * own editable Properties, when the assistant is off, or in a space not
     * shared with it, a space that cannot be read or is not known yet
     * included. The space is read only when the place holds a binding. Only
     * the latest read applies.
     *
     * @returns {Promise<void>} resolved once read
     */
    async loadBindings() {
      const load = ++this.load;
      this.bindings = [];
      if (!this.fillFromContent || this.readOnly || !this.spaceResolved) {
        return;
      }
      const bindings = await getBindings(PROPERTIES_APPLICATION_ID, PROPERTIES_EXTENSION_ID);
      if (load !== this.load || !bindings.length) {
        return;
      }
      const shared = await isSpaceSharedWithAssistant(this.spaceId);
      if (load === this.load && shared) {
        this.bindings = bindings;
      }
    },
    /**
     * Opens the assistant with every binding of the place, on the content on
     * screen, saved or not: the chat gets the content, never the note's id,
     * whose stored version may be older. The page id reaches the prompts as
     * {note_id}, empty for a note never saved. Tells the drawer once the
     * chat is open.
     *
     * @returns {void}
     */
    askAi() {
      if (!this.hasNoteText) {
        return;
      }
      this.opening = true;
      openAssistant({
        uxBindings: this.bindings,
        properties: {
          objectType: 'notes',
          objectTitle: this.note?.title || this.$t('notes.untitled.title'),
          objectContent: this.note?.content || '',
          params: {note_id: this.pageId || ''},
        },
        insertText: text => this.$emit('insert-text', text),
      })
        .then(() => this.$emit('assistant-opened'))
        .catch(() => this.$root.$emit('alert-message', this.$t('notes.metadata.assistant.error'), 'error'))
        .finally(() => this.opening = false);
    },
  },
};
</script>
