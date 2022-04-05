<template>
  <v-list-item class="clickable" :href="noteUrl">
    <v-list-item-icon class="me-3 my-auto">
      <v-icon size="22" class="icon-default-color"> fas fa-clipboard </v-icon>
    </v-list-item-icon>

    <v-list-item-content>
      <v-list-item-title class="text-color body-2">{{ noteTitle }}</v-list-item-title>
    </v-list-item-content>

    <v-list-item-action>
      <v-btn icon>
        <v-icon class="yellow--text text--darken-2" size="18">fa-star</v-icon>
      </v-btn>
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
    noteUrl: ''
  }),
  created() {
    this.$notesService.getNoteById(this.id).then(note => {
      const noteSpace = note.wikiOwner.split('/')[2];
      this.noteTitle = note.title;
      this.noteUrl = `${eXo.env.portal.context}/g/:spaces:${noteSpace}/${noteSpace}/notes/${note.id}`;
    });
  },
};
</script>
