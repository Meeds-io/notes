<template>
  <v-app class="white">
    <div :class="notesApplicationClass">
      <div class="white my-3 py-2 primary--text">
        <v-btn
          link
          text
          class="primary--text font-weight-bold text-capitalize"
          @click="switchNotesApp">
          <v-icon class="me-3" size="16">far fa-window-restore</v-icon>
          {{ buttonText }}
        </v-btn>
      </div>
      <div v-if="useNewApp" class="d-flex flex-column pb-4 notes-wrapper">
        <notes-overview />
      </div>
    </div>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    useNewApp: false,
    imageLoaded: false,
    notesApplicationClass: 'wikiPortlet',
    notesPageName: '',
  }),
  computed: {
    buttonText() {
      if (this.useNewApp) {
        return this.$t('notes.switchToOldApp');
      } else {
        return this.$t('notes.switchToNewApp');
      }
    }
  },
  watch: {
    useNewApp() {
      if (this.useNewApp) {
        this.notesApplicationClass='notesApplication';
        $('.uiWikiPortlet').hide();
      } else {
        this.notesApplicationClass='WikiPortlet';
        $('.uiWikiPortlet').show();
      }
    },
  },
  methods: {
    switchNotesApp() {
      this.useNewApp = !this.useNewApp;
      let toApp = 'old';
      if (this.useNewApp){
        toApp = 'new';
      }
      this.$notesService.switchNoteApp(toApp).then(() => {
        if (!this.useNewApp) {
          window.location.reload();
        }
      }).catch(() => {
        if (!this.useNewApp) {
          window.location.reload();
        }
      });

    },
    displayText() {
      window.setTimeout(() => this.imageLoaded = true, 200);
    },
  },
};
</script> 