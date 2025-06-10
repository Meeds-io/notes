<template>
  <v-hover v-slot="{ hover }">
    <v-card
      flat
      class="pa-0"
      :aria-label="$t('search.access.to.result', {0 :wikiTitleText})"
      :href="wikiUrl">
      <v-list class="pa-0" :class="hover && 'light-grey-background-color no-border-radius' || ''">
        <v-list-item>
          <v-list-item-icon class="me-4">
            <v-icon size="32" class="icon-default-color mt-2">fas fa-clipboard</v-icon>
          </v-list-item-icon>

          <v-list-item-content>
            <div class="d-flex flex-row full-width align-center">
              <v-list-item-title class="flex-grow-1">
                <h1
                  class="title pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate">
                  {{ wikiTitle }}
                </h1>
              </v-list-item-title>
              <span v-show="hover || isMobile" class="ml-2">
                <note-favorite-action
                  :note="result"
                  @removed="$emit('refresh-favorite')" />
              </span>
            </div>

            <v-list-item-subtitle class="d-flex flex-column">
              <span class="d-flex flex-row mx-auto full-width">
                <span class="d-flex flex-row align-center" v-if="space">
                  <a
                    v-bind="attrs"
                    v-on="on"
                    :href="spaceUrl"
                    class="flex-nowrap flex-shrink-0 d-flex spaceAvatar">
                    <v-avatar
                      :size="18"
                      tile
                      class="my-auto">
                      <img
                        :src="space.avatarUrl"
                        alt=""
                        class="object-fit-cover ma-auto"
                        loading="lazy">
                    </v-avatar>
                    <p class="ms-2 my-auto text-subtitle">{{ space.displayName }}</p>
                  </a>
                  <v-icon size="3" class="icon-default-color mx-3">fas fa-circle</v-icon>
                </span>
                <exo-user-avatar
                  :profile-id="posterUsername"
                  :size="18"
                  small-font-size
                  :avatar="isMobile"
                  :popover="false" />
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
    wikiTitle() {
      return this.result && this.result.title || '';
    },
    wikiTitleText() {
      return $('<div />').html(this.wikiTitle).text();
    },
    posterUsername() {
      return this.result?.poster?.profile?.username;
    },
    space() {
      return this.result?.wikiOwner?.space;
    },
    summary() {
      return this.$utils.htmlToText(this.result?.summary || this.excerpt || this.result.content);
    },
    isMobile() {
      return this.$vuetify?.breakpoint?.smAndDown;
    },
    wikiUpdateDate() {
      return this.result?.updateDate;
    },
    spaceUrl() {
      if (!this.space?.id) {
        return '#';
      }
      return `${eXo.env.portal.context}/s/${this.space?.id}`;
    }
  },
};
</script>
