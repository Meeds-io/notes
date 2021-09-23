<template>
  <div>
    <exo-drawer
      ref="noteVersionsDrawer"
      class="noteHistoryDrawer"
      v-model="drawer"
      show-overlay
      right>
      <template slot="title">
        {{ $t('notes.label.historyVersions') }}
      </template>
      <template slot="content">
        <v-list class="ma-3">
          <v-list-item-group
            active-class="bg-active"
            v-model="model">
            <v-list-item
              v-for="version in noteVersions"
              :key="version"
              class="history-line mb-2 justify-space-between border-color border-radius clickable"
              @click="$emit('open-version', version)">
              <div class="version-author">
                <span class="note-version border-radius primary px-2 font-weight-bold me-2">V{{ version.versionNumber }}</span>
                <span class="font-weight-bold text-truncate">{{ version.authorFullName }}</span>
              </div>
              <div class="version-update-date">
                <date-format
                  class="text-light-color text-truncate caption"
                  :value="version.updatedDate.time"
                  :format="dateTimeFormat" />
              </div>
            </v-list-item>
          </v-list-item-group>
        </v-list>
      </template>
    </exo-drawer>
  </div>
</template>
<script>
export default {
  data: () => ({
    noteVersions: [],
    dateTimeFormat: {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit'
    },
    model: 0
  }),
  methods: {
    open(noteVersions) {
      this.noteVersions = noteVersions;
      this.$refs.noteVersionsDrawer.open();
    },
  }
};
</script>