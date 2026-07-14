<template>
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      :aria-label="$t('search.access.to.result', {0 :pageTitleText})"
      :href="pageUrl">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="me-4">
            <v-icon size="32" class="icon-default-color mt-2">fas fa-file-alt</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <v-list-item-title>
              <h1
                class="title primary--text pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                v-sanitized-html="pageTitle">
              </h1>
            </v-list-item-title>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row mx-auto full-width" v-if="pageAuthor || pageDate">
                <exo-user-avatar
                  v-if="pageAuthor"
                  :profile-id="pageAuthor"
                  :size="18"
                  small-font-size
                  :popover="false" />
                <v-icon
                  v-if="pageAuthor && pageDate"
                  size="3"
                  class="icon-default-color mx-3">fas fa-circle</v-icon>
                <v-icon
                  v-if="pageDate"
                  size="12"
                  class="icon-default-color">fas fa-clock</v-icon>
                <date-format v-if="pageDate" class="ms-1 my-auto" :value="pageDate" />
              </span>
              <div
                class="pt-2 text-wrap text-body-2 text-color text-break text-truncate-3"
                v-sanitized-html="excerpt"></div>
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
    pageUrl() {
      return this.result?.url || null;
    },
    excerpt() {
      return this.result?.excerpt;
    },
    pageTitle() {
      if (!this.result) {
        return '';
      }
      return this.result.siteName ? `${this.result.title} - ${this.result.siteName}` : this.result.title;
    },
    pageTitleText() {
      return $('<div />').html(this.pageTitle).text();
    },
    pageAuthor() {
      return this.result?.author;
    },
    pageDate() {
      return this.result?.date;
    },
  },
};
</script>
