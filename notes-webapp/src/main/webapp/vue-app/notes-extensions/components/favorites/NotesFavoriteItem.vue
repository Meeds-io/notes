<template>
  <v-list-item class="clickable" :href="noteUrl">
    <v-list-item-icon class="me-3 my-auto">
      <v-icon size="22" class="icon-default-color"> fas fa-clipboard </v-icon>
    </v-list-item-icon>

    <v-list-item-content>
      <v-list-item-title class="text-color body-2">{{ noteTitle }}</v-list-item-title>
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
  },
  data: () => ({
    noteTitle: '',
    noteUrl: '', 
    isFavorite: true
  }),
  created() {
    this.$notesService.getNoteById(this.id).then(note => {
      const noteSpace = note.wikiOwner.split('/')[2];
      this.noteTitle = note.title;
      this.noteUrl = `${eXo.env.portal.context}/g/:spaces:${noteSpace}/${noteSpace}/notes/${note.id}`;
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
      this.$root.$emit('notes-notification-alert', {
        message,
        type: type || 'success',
      });
    },
  },
};
</script>
