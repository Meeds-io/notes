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
    useNewApp: true,
    imageLoaded: false,
    notesApplicationClass: 'notesApplication',
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
  /*   watch: {
    useNewApp() {
      
    },
  }, */
  created() {
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if ( urlParams.has('appView') ){
      const appView = urlParams.get('appView');
      if (appView ==='old'){
        this.useNewApp = false; 
        this.notesApplicationClass='WikiPortlet';
        $('.uiWikiPortlet').show();
        const theURL= new URL(window.location.href);
        theURL.searchParams.delete('appView');
        window.history.pushState('wiki', '', theURL.href);
      }
    }
  },
  methods: {
    switchNotesApp() {
      document.dispatchEvent(new CustomEvent('displayTopBarLoading'));
      this.useNewApp = !this.useNewApp;
      let toApp = 'old';
      if (this.useNewApp){
        toApp = 'new';
      }
      this.$notesService.switchNoteApp(toApp);
      if (!this.useNewApp) {
        const theURL= new URL(window.location.href);
        theURL.searchParams.set('appView', 'old');
        window.location.href=theURL.href;
      } else {
        this.notesApplicationClass='notesApplication';
        $('.uiWikiPortlet').hide();
        const theURL= new URL(window.location.href);
        theURL.searchParams.delete('appView');
        window.history.pushState('wiki', '', theURL.href);
        document.dispatchEvent(new CustomEvent('hideTopBarLoading'));
      }
        
      

    },
    displayText() {
      window.setTimeout(() => this.imageLoaded = true, 200);
    },
  },
};
</script> 