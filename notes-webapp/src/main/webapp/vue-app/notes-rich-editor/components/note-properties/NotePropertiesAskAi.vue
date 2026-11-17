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
  <v-tooltip v-if="canAskAi" bottom>
    <template #activator="{on, attrs}">
      <!-- a wrapper, so that the tooltip still shows while the button is disabled -->
      <div
        v-bind="attrs"
        v-on="on">
        <v-btn
          :aria-label="$t('notes.metadata.askAi.label')"
          :loading="opening"
          :disabled="!hasNoteText"
          class="note-properties-ask-ai ms-4"
          max-width="36"
          max-height="36"
          icon
          @click.prevent.stop="askAi">
          <v-icon size="18" class="icon-default-color">fa-concierge-bell</v-icon>
        </v-btn>
      </div>
    </template>
    <span class="text-subtitle-font-size">{{ $t('notes.metadata.askAi.label') }}</span>
  </v-tooltip>
</template>

<script>
import {
  PROPERTIES_KINDS,
  getArticleObjectId,
  getBindings,
  isSpaceSharedWithAssistant,
  openAssistant
} from '../../js/NoteAssistant.js';

/**
 * Reads a file as a data URL.
 *
 * @param {File} file the file to read
 * @returns {Promise<string>} its content as a data URL; rejected when the
 *          file is missing or cannot be read
 */
function readAsDataUrl(file) {
  return new Promise((resolve, reject) => {
    if (!file) {
      reject(new Error('no file'));
      return;
    }
    const reader = new FileReader();
    reader.onload = () => resolve(reader.result);
    reader.onerror = () => reject(reader.error);
    reader.readAsDataURL(file);
  });
}

/**
 * « Ask AI » in the header of a note's own Properties, or of an article's in
 * Content's editor: opens the assistant with every enabled binding of the
 * Properties' place, on the content as it is on screen. A text answer is inserted in the Summary field, through
 * the drawer, and an image answer opens the featured image's crop window;
 * Save then saves them.
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
    /** Whether the drawer is the note's own Properties, which offers the assistant, as an article's does. */
    fillFromContent: {
      type: Boolean,
      default: false
    },
    /** The kind of content shown, `notes` or `news` (an article), which sets the requests' place. */
    kind: {
      type: String,
      default: 'notes'
    }
  },
  data: () => ({
    bindings: [],
    load: 0,
    opening: false,
  }),
  computed: {
    /**
     * @returns {object} the place, object type and id name of the content
     *          shown, a note's for a kind it does not know
     */
    place() {
      return PROPERTIES_KINDS[this.kind] || PROPERTIES_KINDS.notes;
    },
    /**
     * @returns {boolean} whether the drawer is one the assistant is offered
     *          in: a note's own Properties, which fill from its content, or
     *          an article's, which Content's editor names by its kind while
     *          it leaves the pick among the article's images off
     */
    offered() {
      return this.fillFromContent || this.kind === 'news';
    },
    /**
     * @returns {object} the values the prompts receive besides the content:
     *          the page id, as {note_id} or {news_id}, and for an article its
     *          id for the add-ons that attach data to it, as {article_id};
     *          each empty for content never saved
     */
    params() {
      const params = {[this.place.idParam]: this.pageId || ''};
      if (this.kind === 'news') {
        params.article_id = getArticleObjectId(this.note);
      }
      return params;
    },
    /**
     * @returns {boolean} whether « Ask AI » is offered: the bindings read
     *          last are kept only in a note's or an article's editable Properties, with
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
    /**
     * Reads again what the assistant offers when the kind of content changes.
     *
     * @returns {void}
     */
    kind() {
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
     * Reads the bindings of the Properties' place: none outside a note's or
     * an article's editable Properties, when the assistant is off, or in a space not
     * shared with it, a space that cannot be read or is not known yet
     * included. The space is read only when the place holds a binding. Only
     * the latest read applies.
     *
     * @returns {Promise<void>} resolved once read
     */
    async loadBindings() {
      const load = ++this.load;
      this.bindings = [];
      if (!this.offered || this.readOnly || !this.spaceResolved) {
        return;
      }
      const bindings = await getBindings(this.place.applicationId, this.place.extensionId);
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
     * {note_id}, or {news_id} and {article_id} for an article, empty for
     * content never saved. Tells the drawer once the
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
          objectType: this.place.objectType,
          objectTitle: this.note?.title || this.$t('notes.untitled.title'),
          objectContent: this.note?.content || '',
          params: this.params,
        },
        insertText: text => this.$emit('insert-text', text),
        insertImage: this.insertImage,
      })
        .then(() => this.$emit('assistant-opened'))
        .catch(() => this.$root.$emit('alert-message', this.$t('notes.metadata.assistant.error'), 'error'))
        .finally(() => this.opening = false);
    },
    /**
     * Opens an image the assistant made in the featured image's crop window,
     * as an added image: the crop, its upload and the Properties' Save make
     * it the featured image, and closing the window drops it. The image is
     * handed as a data URL read from the file, as the window already takes
     * the last crop back, so that no object URL outlives a cancelled crop, a
     * closed window or a failed read; its type is left to the crop, as for a
     * picked image, since a type the window reads as animated would skip the
     * crop and its upload.
     *
     * @param {File} file the image the assistant made
     * @returns {Promise<void>} resolved once the window is asked to open
     */
    insertImage(file) {
      return readAsDataUrl(file)
        .then(src => this.$root.$emit('open-featured-image-drawer', {
          src,
          altText: '',
        }))
        .catch(() => this.$root.$emit('alert-message', this.$t('notes.metadata.assistant.imageError'), 'error'));
    },
  },
};
</script>
