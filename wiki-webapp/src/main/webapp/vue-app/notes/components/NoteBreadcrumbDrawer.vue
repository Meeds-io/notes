<template>
  <exo-drawer
    ref="breadcrumbDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template slot="title">
      {{ $t('notes.label.breadcrumbTitle') }}
    </template>
    <template slot="content">
      <v-layout>
        <template v-if="note" class="ma-0 border-box-sizing">
          <v-list-item @click="$root.$emit('open-note',note.path)">
            <v-list-item-content>
              <v-list-item-title>{{ note.name }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </template>
      </v-layout>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data: () => ({
    note: {}
  }),
  methods: {
    open(note) {
      this.note = note;
      this.$nextTick().then(() => {
        this.$refs.breadcrumbDrawer.open();
      });
    }
  }
};
</script>