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
            <v-slide-y-transition group>
              <v-list-item
                v-for="(version,index) in noteVersionsArray"
                :key="index"
                class="history-line pa-2 mb-2 border-color border-radius d-block" 
                @click="$emit('open-version', version)">
                <div class="author-date-wrapper d-flex justify-space-between ">
                  <div class="version-author">
                    <span class="note-version border-radius primary px-2 font-weight-bold me-2 clickable">V{{ version.versionNumber }}</span>
                    <span class="font-weight-bold text-truncate">{{ version.authorFullName }}</span>
                  </div>
                  <div class="version-update-date">
                    <date-format
                      class="text-light-color text-truncate caption"
                      :value="version.updatedDate.time"
                      :format="dateTimeFormat" />
                  </div>
                </div>
                <div class="description-restore-wrapper d-flex justify-space-between pt-2">
                  <div class="note-version-description"></div>
                  <div v-if="index > 0 && canManage" class="note-version-restore">
                    <v-tooltip bottom>
                      <template #activator="{ on, attrs }">
                        <v-icon
                          v-bind="attrs"
                          v-on="on"
                          size="22"
                          class="primary--text clickable pa-0"
                          @click="$emit('restore-version', version)">
                          mdi-restart
                        </v-icon>
                      </template>
                      <span class="caption">{{ $t('notes.label.restoreVersions') }}</span>
                    </v-tooltip>
                  </div>
                </div>
              </v-list-item>
            </v-slide-y-transition>
          </v-list-item-group>
        </v-list>
      </template>
      <template v-if="hasMore" slot="footer">
        <div class="d-flex mx-4">
          <v-btn
            class="primary--text mx-auto"
            @click="loadMore"
            text>
            {{ $t('notes.button.loadMore') }}
          </v-btn>
        </div>
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
    model: 0,
    pageSize: 0,
    canManage: false
  }),
  computed: {
    noteVersionsArray() {
      return this.noteVersions.slice(0, this.pageSize);
    },
    allNoteVersionsCount() {
      return this.noteVersions.length;
    },
    hasMore() {
      return this.allNoteVersionsCount > this.pageSize;
    },
  },
  created() {
    this.$root.$on('refresh-versions-history', (noteVersions) => {
      this.noteVersions = noteVersions;
    });
  },
  methods: {
    open(noteVersions,canManage) {
      this.canManage = canManage;
      if ( canManage ) {
        this.pageSize = Math.round((window.innerHeight-79)/80);
      } else {
        this.pageSize = Math.round((window.innerHeight-79)/60);
      }
      this.noteVersions = noteVersions;
      this.$refs.noteVersionsDrawer.open();
    },
    loadMore(){
      this.pageSize += this.pageSize;
    },
  }
};
</script>