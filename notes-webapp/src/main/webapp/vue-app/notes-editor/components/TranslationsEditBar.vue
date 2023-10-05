<template>
  <div
    v-if="showTranslationbar"
    id="translationBar"
    class="d-flex flex-nowrap text-truncate px-1 pt-4 pb-3">
    <v-tooltip bottom>
      <template #activator="{ on, attrs }">
        <v-btn
          id="translationBarBackButton"
          aria-label="translation back button"
          class="mx-4  my-auto "
          small
          icon
          v-on="on"
          v-bind="attrs"
          @click="hide">
          <v-icon size="22">fa-arrow-left</v-icon>
        </v-btn>
      </template>
      <span class="caption">{{ $t('notes.label.button.back') }}</span>
    </v-tooltip>
    <div class="bar-separator my-auto"></div>
    <div class="px-4 height-auto d-flex flex-nowrap">
      <div class=" d-flex flex-wrap my-auto  height-auto translation-chips">
        <div v-if="!isMobile" class=" d-flex flex-wrap my-auto  height-auto">
          <v-chip
            color="primary"
            class="my-auto mx-1"
            small
            :outlined="!!selectedTranslation.value"
            @click="changeTranslation({value: ''})">
            {{ $t('notes.label.translation.originalVersion') }}
          </v-chip>
          <div v-if="!isMobile" class="ps-2 my-auto mx-1">{{ $t('notes.label.translations') }}</div>
          <div v-if="!translations || translations.length===0" class="ps-2 my-auto mx-1 text-sub-title">{{ $t('notes.label.noTranslations') }}</div>
          <v-chip
            v-for="(translation, i) in translationToShow"
            :key="i"
            :close="translation.value!==selectedTranslation.value"
            small
            :outlined="translation.value!==selectedTranslation.value"
            color="primary"
            close-label="translation remove button"
            @click:close="removeTranslation(translation)"
            @click="changeTranslation(translation)"
            class="my-auto mx-1">
            {{ translation.text }}
          </v-chip>
        </div>
        <v-menu
          v-if="moreTranslations.length>0"
          v-model="moreTranslationsMenu"
          class=" ma-1"
          bottom>
          <template #activator="{ on, attrs }">
            <v-btn
              v-show="!isMobile"
              height="32"
              width="32"
              fab
              depressed
              class="my-1"
              v-bind="attrs"
              v-on="on">
              <v-avatar
                size="32"
                class="notDisplayedIdentitiesOverlay">
                <div class="notDisplayedIdentities d-flex align-center justify-center">
                  +{{ moreTranslations.length }}
                </div>
              </v-avatar>
            </v-btn>
            <v-chip
              v-show="isMobile"
              color="primary"
              class="my-auto mx-1"
              small
              v-bind="attrs"
              v-on="on">
              {{ !!selectedTranslation.value ? selectedTranslation.text : $t('notes.label.translation.originalVersion') }}
            </v-chip>
          </template>

          <v-list class="pa-0">
            <v-list-item
              v-for="(item, i) in moreTranslations"
              :key="i"
              class="pa-0 translation-chips">
              <v-chip
                :close="item.value!==selectedTranslation.value"
                small
                :outlined="item.value!==selectedTranslation.value"
                color="primary"
                close-label="translation remove button"
                @click:close="removeTranslation(item)"
                @click="changeTranslation(item)"
                class="my-auto mx-1">
                {{ item.text }}
              </v-chip>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>
    </div>
    <div class="bar-separator my-auto"></div>
    <div class="px-2  my-auto  height-auto width-auto  d-flex flex-nowrap">
      <div v-if="!isMobile" class="ma-2">{{ $t('notes.label.translation.add') }}</div>
      <select
        id="translationBarFilterSelect"
        v-model="selectedLang"
        aria-label="List of traductions"
        class="ignore-vuetify-classes py-2 height-auto width-auto text-truncate my-auto mx-2">
        <option
          v-for="item in languages"
          :key="item.value"
          :value="item">
          {{ item.text }}
        </option>
      </select>
      <v-tooltip bottom>
        <template #activator="{ on, attrs }">
          <v-btn
            aria-label="translation add button"
            class="my-auto "
            small
            icon
            v-on="on"
            v-bind="attrs"
            :disabled="selectedLang.value===''"
            @click="add()">
            <v-icon size="18" class="icon-default-color py-3">fa-plus</v-icon>
          </v-btn>
        </template>
        <span class="caption">{{ $t('notes.label.addTheTranslation') }}</span>
      </v-tooltip>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    note: {
      type: Object,
      default: () => null,
    },
    translations: {
      type: Object,
      default: () => null,
    },
    languages: {
      type: Object,
      default: () => null,
    },
    isMobile: {
      type: Boolean,
      default: false,
    },
  },

  data: () => ({
    moreTranslationsMenu: false,
    showTranslationbar: false,
    selectedLang: {},
    selectedTranslation: {value: ''},
    displayActionMenu: true,
    noteLanguages: [],
  }),

  mounted() {
    $(document).on('click', () => {
      this.moreTranslationsMenu = false;
    });

  },


  computed: {
    moreTranslations(){
      return this.translations && this.translations.length>this.limitTranslationsToShow ? this.translations.slice(this.limitTranslationsToShow,this.translations.length):[];
    }, 
    translationToShow(){
      return this.translations && this.translations.length>this.limitTranslationsToShow ? this.translations.slice(0,this.limitTranslationsToShow) : this.translations;
    },
    noteId(){
      return !this.note.draftPage?this.note.id:this.note.targetPageId;
    },
    limitTranslationsToShow() {
      return this.isMobile?1:3;
    }

  },

  methods: {
    show(lang) {
      this.selectedTranslation={value: lang};
      const translation = this.translations.find(item => item.value === lang);
      if (translation){
        this.selectedTranslation=translation;
      }
      this.selectedLang={value: '',text: this.$t('notes.label.chooseLangage')};
      if (!this.translations && this.note && this.noteId){
        this.getNoteLanguages(this.noteId);
      }
      this.showTranslationbar=true;
    },
    hide() {
      this.showTranslationbar=false;
      this.$root.$emit('hide-translations');
    },
    add(){
      this.$root.$emit('add-translation', this.selectedLang);
      this.selectedTranslation=this.selectedLang;
      this.languages = this.languages.filter(item => item.value !== this.selectedLang.value);
      this.selectedLang={value: '',text: this.$t('notes.label.chooseLangage')};
      
    },
    changeTranslation(translation){
      this.selectedTranslation=translation;
      this.$root.$emit('lang-translation-changed', this.selectedTranslation);
    },
    removeTranslation(translation){
      this.$root.$emit('delete-lang-translation', translation);
    },
  }
};
</script>