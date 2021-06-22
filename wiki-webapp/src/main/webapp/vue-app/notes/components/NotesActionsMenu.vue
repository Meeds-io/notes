<template>
  <v-menu
    v-model="displayActionMenu"
    :attach="'#note-actions-menu'"
    transition="slide-x-reverse-transition"
    content-class="note-actions-menu"
    offset-y
    left>
    <v-list>
      <v-list-item
        v-if="note.name !== defaultPath"
        class="draftButton"
        :key="note.id"
        @click="$root.$emit('delete-note')">
        <v-list-item-title class="subtitle-2">
          <i class="uiIcon uiIconTrash pr-1"></i>
          <span>{{ $t('notes.delete') }}</span>
        </v-list-item-title>
      </v-list-item>
    </v-list>
  </v-menu>
</template>
<script>
export default {
  data() {
    return {
      displayActionMenu: false,
    };
  },
  props: {
    note: {
      type: Object,
      default: () => null,
    },
    defaultPath: {
      type: String,
      default: () => 'WikiHome',
    }
  },
  created() {
    $(document).on('mousedown', () => {
      if (this.displayActionMenu) {
        window.setTimeout(() => {
          this.displayActionMenu = false;
        }, this.waitTimeUntilCloseMenu);
      }
    });
    this.$root.$on('display-action-menu', ( )=> {
      this.displayActionMenu = true;
    });
  },
};
</script>