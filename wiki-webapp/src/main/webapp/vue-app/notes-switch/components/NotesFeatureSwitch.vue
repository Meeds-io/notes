<template>
  <v-app class="white">
    <div :class="notesApplicationClass">
    <div class="white my-3 py-2 primary--text" >
      <v-btn
        link
        text
        class="primary--text font-weight-bold text-capitalize"
        @click="switchNotesApp">
        <v-icon class="me-3" size="16">far fa-window-restore</v-icon>
        {{ buttonText }}
      </v-btn>
    </div>
    <div v-if="useNewApp" class="white d-flex flex-column text-center pb-4">
      <v-img
        src="/wiki/images/comingSoon.png"
        width="450px"
        max-width="100%"
        class="mx-auto mt-10"
        @load="displayText" />
      <h3 v-if="imageLoaded" class="font-weight-bold">{{ $t('notes.comingSoon') }}</h3>
    </div>
  </div>
  </v-app>
</template>
<script>
export default {
  data: () => ({
    useNewApp: false,
    imageLoaded: false,
    notesApplicationClass: 'wikiPortlet'
  }),
  computed: {
    buttonText() {
      if (this.useNewApp) {
        return this.$t('notes.switchToOldApp');
      } else {
        return this.$t('notes.switchToNewApp');
      }
    },
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
    },
    displayText() {
      window.setTimeout(() => this.imageLoaded = true, 200);
    },
  },
};
</script> 