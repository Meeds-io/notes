<template>
  <v-menu
    v-model="displayActionMenu"
    :attach="'#note-actions-menu'"
    transition="slide-x-reverse-transition"
    content-class="py-0 note-actions-menu pa-0"
    max-width="100%"
    offset-y
    left>
    <v-list class="py-0 text-center text-no-wrap">
      <v-list-item
        v-if="note?.canView"
        class="px-4 py-1 action-menu-item draftButton"
        @click="copyLink">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-link
        </v-icon>
        <span>{{ $t('notes.menu.label.copyLink') }}</span>
      </v-list-item>
      <v-list-item
        v-if="note?.canView"
        class="px-4 py-1 noteExportPdf action-menu-item draftButton"
        @click="$emit('export-pdf')">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-file-pdf
        </v-icon>
        <span>{{ $t('notes.menu.label.exportPdf') }}</span>
      </v-list-item>
      <v-list-item
        class="px-4 py-1 action-menu-item draftButton"
        @click="$emit('open-history')">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-history
        </v-icon>
        <span>{{ $t('notes.menu.label.noteHistory') }}</span>
      </v-list-item>
      <v-list-item
        v-if="!homePage && note.canManage"
        class="px-4 py-1 text-left action-menu-item draftButton"
        @click="$emit('open-treeview')">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-arrows-alt
        </v-icon>
        <span>{{ $t('notes.menu.label.movePage') }}</span>
      </v-list-item>
      <v-list-item
        v-if="homePage"
        class="px-4 py-1 text-left action-menu-item draftButton"
        @click="$emit('open-treeview-export')">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-file-export
        </v-icon>
        <span>{{ $t('notes.menu.label.export') }}</span>
      </v-list-item>
      <v-list-item
        v-if="homePage && note?.canImport"
        class="px-4 py-1 action-menu-item draftButton"
        @click="$emit('open-import-drawer')">
        <v-icon
          size="14"
          class="clickable pe-2 icon-menu">
          fas fa-file-import
        </v-icon>
        <span>{{ $t('notes.menu.label.import') }}</span>
      </v-list-item>
      <v-list-item
        v-if="!homePage && note?.canManage"
        class="red--text px-4 py-1 action-menu-item draftButton"
        @click="$root.$emit('delete-note')">
        <v-icon
          size="14"
          class="delete-option-color clickable pe-2 icon-menu">
          fas fa-trash
        </v-icon>
        <span class="delete-option-color">
          {{ $t('notes.menu.label.delete') }}
        </span>
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
      default: () => 'Home',
    }
  },
  computed: {
    homePage(){
      return !this.note.parentPageId;
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
  methods: {
    copyLink() {
      const inputTemp = $('<input>');
      const path = window.location.href;
      $('body').append(inputTemp);
      inputTemp.val(path).select();
      document.execCommand('copy');
      inputTemp.remove();
      this.$root.$emit('show-alert', {type: 'success',message: this.$t('notes.alert.success.label.linkCopied')});
    }
  },
};
</script>
