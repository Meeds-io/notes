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
                  class="title primary--text pt-1 mb-0 ps-0 my-auto align-center text-start text-truncate"
                  v-sanitized-html="wikiTitle">
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
                v-if="summary"
                class="pt-2 text-wrap text-body-2 text-color text-break"
                :class="isMobile && 'text-truncate-2' || 'text-truncate-3'"
                v-sanitized-html="summary"></div>
              <div
                v-else-if="contentText"
                class="pt-2 text-wrap text-body-2 text-color text-break"
                :class="isMobile && 'text-truncate-2' || 'text-truncate-3'">
                {{ contentText }}
              </div>
              <div
                v-else-if="navigationItems.length"
                class="pt-2 text-wrap text-body-2 text-color text-break"
                :class="isMobile && 'text-truncate-2' || 'text-truncate-3'">
                <div
                  v-for="(item, index) in navigationItems"
                  :key="index"
                  class="text-truncate">
                  - {{ item }}
                </div>
              </div>
              <div
                v-else-if="!loadingNavigation"
                class="pt-2 text-body-2 text-sub-title text-truncate">
                {{ $t('notes.search.noContent') }}
              </div>
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
  data: () => ({
    navigationItems: [],
    loadingNavigation: false,
  }),
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
      return this.result?.summary || this.excerpt;
    },
    contentText() {
      if (!this.result?.content) {
        return '';
      }
      // An inert document: the navigation macro's images are not fetched
      const body = new DOMParser().parseFromString(this.result.content, 'text/html').body;
      body.querySelectorAll('.navigation-img-wrapper').forEach(macro => macro.remove());
      return body.textContent?.trim() || '';
    },
    hasNavigationMacro() {
      return this.result?.content?.includes('navigation-img-wrapper') || false;
    },
    restUrl() {
      return `${eXo.env.portal.context}/${eXo.env.portal.rest}/notes`;
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
  created() {
    if (!this.summary && !this.contentText && this.hasNavigationMacro) {
      this.retrieveNavigationItems();
    }
  },
  methods: {
    retrieveNavigationItems() {
      this.loadingNavigation = true;
      return this.retrieveNotePath()
        .then(path => fetch(`${this.restUrl}/tree/children/notes?path=${encodeURIComponent(path)}`, {
          credentials: 'include',
        }))
        .then(resp => resp?.ok && resp.json() || null)
        .then(data => this.navigationItems = (data?.jsonList || []).map(node => node.name))
        .catch(() => this.navigationItems = [])
        .finally(() => this.loadingNavigation = false);
    },
    retrieveNotePath() {
      // The search result carries the owner identity of space notes only
      if (this.space?.groupId) {
        return Promise.resolve(`group${this.space.groupId}/${this.result.pageName}`);
      }
      return fetch(`${this.restUrl}/note/${this.result.id}`, {
        credentials: 'include',
      })
        .then(resp => resp?.ok && resp.json() || Promise.reject(new Error('Note not found')))
        .then(note => `${note.wikiType}/${note.wikiOwner.replace(/^\//, '')}/${note.name}`);
    },
  },
};
</script>
