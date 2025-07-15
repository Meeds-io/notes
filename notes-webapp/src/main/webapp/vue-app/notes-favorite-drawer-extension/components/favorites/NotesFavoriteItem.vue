<template>
  <v-list-item
    :href="noteUrl"
    @keydown.enter="setAsViewed"
    @auxclick="setAsViewed"
    @click="setAsViewed">
    <v-list-item-icon class="me-3 my-auto">
      <v-card
        :min-width="iconWidth"
        class="d-flex justify-center no-border-radius"
        color="transparent"
        flat>
        <v-icon :size="iconSize">fa-clipboard</v-icon>
      </v-card>
    </v-list-item-icon>
    <v-list-item-content>
      <v-list-item-title class="text-truncate">{{ noteTitle }}</v-list-item-title>
      <v-list-item-subtitle v-if="expanded" class="d-flex align-center full-width overflow-hidden pt-2px">
        <template v-if="spaceGroupId">
          <favorite-space-avatar
            :space-group-id="spaceGroupId"
            :size="16"
            class="flex-grow-0 flex-shrink-1 text-truncate"
            link-style />
          <v-icon class="flex-grow-0 flex-shrink-0 mx-2" size="2">fa-circle</v-icon>
        </template>
        <date-format class="flex-grow-0 flex-shrink-0" :value="updatedDate" />
        <template v-if="author">
          <v-icon class="flex-grow-0 flex-shrink-0 mx-2" size="2">fa-circle</v-icon>
          <favorite-user-avatar
            :profile-id="author"
            :size="16"
            class="flex-grow-1 flex-shrink-1 text-truncate" />
        </template>
      </v-list-item-subtitle>
    </v-list-item-content>

    <v-list-item-action>
      <favorite-button
        :id="id"
        :favorite="isFavorite"
        :top="top"
        :right="right"
        type="notes"
        type-label="notes"
        @removed="removed"
        @remove-error="removeError" />
    </v-list-item-action>
  </v-list-item>
</template>
<script>
export default {
  props: {
    id: {
      type: String,
      default: () => null,
    },
    clickCallback: {
      type: Function,
      default: null,
    },
    expanded: {
      type: Boolean,
      default: false,
    },
  },
  data: () => ({ 
    isFavorite: true,
    note: {},
  }),
  computed: {
    iconWidth() {
      return this.expanded ? 40 : 30;
    },
    noteTitle() {
      return this.note?.title || '';
    },
    updatedDate() {
      return this.note?.updatedDate?.time || this.note?.createdDate?.time;
    },
    author() {
      return this.note?.lastUpdater || this.note?.author;
    },
    noteUrl() {
      return this.note?.lang && `${this.note?.url}?translation=${this.note.lang}` || `${this.note?.url}?translation=original`;
    },
    spaceGroupId() {
      return this.note?.wikiType === 'group' &&  this.note?.wikiOwner?.startsWith?.('/spaces/') ? this.note?.wikiOwner : null;
    },
    iconSize() {
      return this.expanded ? 34 : 24;
    },
  },
  created() {
    let noteId = this.id;
    let lang = null;
    if (this.id.includes('-')) {
      const parts = this.id.split('-');
      noteId = parts[0];
      lang = parts[1];
    }
    this.$notesService.getNoteById(noteId, lang).then(note => {
      this.note = note;
    });
  },
  methods: {
    removed() {
      this.isFavorite = !this.isFavorite;
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
      this.$emit('removed');
      this.$root.$emit('refresh-favorite-list');
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'note'), 'error');
    },
    displayAlert(message, type) {
      document.dispatchEvent(new CustomEvent('notification-alert', {detail: {
        message,
        type: type || 'success',
      }}));
    },
    setAsViewed(event) {
      if (event.which === 1 || event.which === 2) {
        this.clickCallback('notes', this.id);
      }
    },
  },
};
</script>
