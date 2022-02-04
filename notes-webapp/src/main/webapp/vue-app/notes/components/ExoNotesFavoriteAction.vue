<template>
  <favorite-button
    :id="favoriteId"
    :favorite="isFavorite"
    :absolute="absolute"
    :top="top"
    :right="right"
    :space-id="spaceId"
    :type="favoriteType"
    :template-params="templateParams"
    :small="false"
    type-label="notes"
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
    spaceId: null,
    templateParams: {},
  }),
  computed: {
    isFavorite() {
      return this.note.metadatas && this.note.metadatas.favorites && this.note.metadatas.favorites.length;
    },
    favoriteType() {
      return this.note.activityId ? 'activity' : 'notes';
    },
    favoriteId() {
      return this.note.activityId ? this.note.activityId : this.note.id;
    }
  },
  watch: {
    note() {
      if (this.note) {
        this.templateParams.page_id = this.note.id;
      }
    }
  },
  methods: {
    removed() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyDeletedFavorite'));
      this.$favoriteService.removeFavorite(this.favoriteType, this.favoriteId)
        .then(() => {
          this.isFavorite = false;
          this.$emit('removed');
        })
        .catch(() => this.$emit('remove-error'));
    },
    removeError() {
      this.displayAlert(this.$t('Favorite.tooltip.ErrorDeletingFavorite', 'note'), 'error');
    },
    added() {
      this.displayAlert(this.$t('Favorite.tooltip.SuccessfullyAddedAsFavorite'));
      this.$favoriteService.addFavorite(this.favoriteType, this.favoriteId)
        .then(() => {
          this.isFavorite = true;
          this.$emit('added');
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