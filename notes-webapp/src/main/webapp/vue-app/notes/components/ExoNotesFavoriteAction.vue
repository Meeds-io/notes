<template>
  <favorite-button
    :id="note.activityId"
    :favorite="isFavorite"
    :absolute="absolute"
    :top="top"
    :right="right"
    type="activity"
    @removed="removed"
    @remove-error="removeError"
    @added="added"
    @add-error="addError" />
</template>

<script>
export default {
  props: {
    note: {
      type: Object,
      default: null,
    },
    absolute: {
      type: Boolean,
      default: false,
    },
    top: {
      type: Number,
      default: () => 0,
    },
    right: {
      type: Number,
      default: () => 0,
    },
  },
  data: () => ({
    isFavorite: false,
  }),
  created() {
    this.$activityService.getActivityById(this.note.activityId)
      .then(fullActivity => {
        this.isFavorite = fullActivity && fullActivity.metadatas && fullActivity.metadatas.favorites && fullActivity.metadatas.favorites.length;
      });
  },
  methods: {
    removed() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
      this.$favoriteService.removeFavorite('notes', this.note.id)
        .then(() => {
          this.isFavorite = false;
        })
        .catch(() => this.$emit('remove-error'));
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'note'), 'error');
    },
    added() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyAddedAsFavorite'));
      this.$favoriteService.addFavorite('notes', this.note.id)
        .then(() => {
          this.isFavorite = true;
        })
        .catch(() => this.$emit('add-error'));
    },
    addError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorAddingAsFavorite', 'note'), 'error');
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