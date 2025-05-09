<template>
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      @click="openWiki">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="me-4">
            <v-icon size="32" class="icon-default-color mt-2">fas fa-clipboard</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <div class="d-flex flex-row full-width align-center">
              <v-list-item-title class="flex-grow-1" :title="wikiTitle">
                <p
                  :title="wikiTitleText"
                  class="title font-weight-bold pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                  v-sanitized-html="wikiTitle"></p>
              </v-list-item-title>
              <span v-show="hover || isMobile" class="ml-2">
                <note-favorite-action
                  :note="result"
                  @removed="$emit('refresh-favorite')" />
              </span>
            </div>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row mx-auto full-width">
                <exo-space-avatar
                  :space-id="spaceId"
                  size="18"
                  text-truncate-class="text-truncate text-sub-title"
                  small-font-size
                  subtitle-new-line-class
                  :avatar="isMobile"
                  popover />
                <v-icon size="3" class="icon-default-color mx-3">fas fa-circle</v-icon>
                <exo-user-avatar
                  :profile-id="posterUsername"
                  :size="18"
                  small-font-size
                  :avatar="isMobile"
                  :popover="!isMobile" />
                <v-icon
                  v-if="wikiUpdateDate"
                  size="3"
                  class="icon-default-color mx-3">fas fa-circle</v-icon>
                <v-icon
                  v-if="wikiUpdateDate"
                  size="12"
                  class="icon-default-color">fas fa-clock</v-icon>
                <date-format class="ms-1 my-auto" :value="wikiUpdateDate" />
              </span>
              <div
                class="pt-2 text-wrap text-body text-break"
                :title="summaryText"
                :class="isMobile && 'text-truncate-2' || 'text-truncate-3'"
                v-sanitized-html="summary"></div>
            </v-list-item-subtitle>
          </v-list-item-content>
        </v-list-item>
      </v-list>
    </v-card>
  </v-hover>
</template>

<script>
export default {
  props: {
    term: {
      type: String,
      default: null,
    },
    result: {
      type: Object,
      default: null,
    },
  },
  computed: {
    wikiUrl() {
      return this.result?.lang && this.result?.url || `${this.result?.url}?translation=original`;
    },
    excerpt() {
      return this.result?.excerpt;
    },
    summaryText() {
      return this.excerpt && $('<div />').html(this.excerpt).text() || $('<div />').html(this.summary).text();
    },
    wikiTitle() {
      return this.result && this.result.title || '';
    },
    wikiTitleText() {
      return $('<div />').html(this.wikiTitle).text();
    },
    posterUsername() {
      return this.result?.poster?.profile?.username;
    },
    spaceId() {
      return this.result?.wikiOwner?.space?.id;
    },
    summary() {
      return this.result?.summary || this.excerpt || this.result.content;
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
    wikiUpdateDate() {
      return this.result?.updateDate;
    }
  },
  methods: {
    openWiki() {
      if (this.wikiUrl) {
        window.location.href = this.wikiUrl; // same tab
      }
    },
  }
};
</script>
