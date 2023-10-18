<template>
  <v-menu
    v-if="translations.length > 1 || menuExtensions.length"
    v-model="translationsMenu"
    offset-y
    bottom>
    <template #activator="{ on, attrs }">
      <v-icon
        size="22"
        :class="langButtonColor"
        class="remove-focus my-auto pa-0  pe-1"
        v-bind="attrs"
        v-on="on">
        fa-language
      </v-icon>
    </template>
    <v-list class="px-2" dense>
      <v-list-item
        v-for="(item, i) in translations"
        :key="i"
        class="translation-chips">
        <v-chip
          small
          :outlined="item.value!==selectedTranslation.value"
          color="primary"
          close-label="translation remove button"
          @click="changeTranslation(item)"
          class="my-auto mx-1">
          {{ item.text }}
        </v-chip>
      </v-list-item>
      <v-list-item
        v-for="(extension, i) in menuExtensions"
        :key="i"
        class="translation-chips">
        <extension-registry-component
          :component="extension"
          :params="params"
          element="div" />
      </v-list-item>
    </v-list>
  </v-menu>
</template>

<script>
export default {
  data() {
    return {
      menuExtensions: null,
      translationsMenu: false,
    };
  },
  props: {
    note: {
      type: Object,
      default: () => {
        return {};
      }
    },
    translations: {
      type: Array,
      default: () => {
        return [];
      }
    },
    selectedTranslation: {
      type: Object,
      default: () => {
        return {};
      }
    }
  },
  computed: {
    params() {
      return {
        note: this.note,
        selectedTranslation: this.selectedTranslation,
      };
    },
    langButtonColor(){
      return this.selectedTranslation.value!=='' ? 'primary--text':'';
    },
  },
  created() {
    this.refreshTranslationExtensions();
    document.addEventListener('automatic-translation-extensions-updated', () => {
      this.refreshTranslationExtensions();
    });
  },
  mounted() {
    $(document).on('click', () => {
      this.translationsMenu = false;
    });
  },
  methods: {
    refreshTranslationExtensions() {
      this.menuExtensions = extensionRegistry.loadExtensions('notes', 'translation-menu-extension');
    },
    changeTranslation(item) {
      this.$emit('change-translation', item);
    }
  }
};
</script>