<template>
  <div
    v-if="showTranslationbar"
    id="translationBar"
    class="d-flex flex-nowrap text-truncate px-1 pt-4 pb-3">
    <v-btn
      id="translationBarBackButton"
      class="px-4  my-auto "
      small
      icon
      @click="hide">
      <v-icon size="22" class="icon-default-color">fa-arrow-left</v-icon>
    </v-btn>
    <div class="px-4 height-auto left-separator right-separator d-flex flex-nowrap">
      <div class=" d-flex flex-wrap my-auto  height-auto translation-chips">
        <div v-if="!isMobile" class=" d-flex flex-wrap my-auto  height-auto">
          <v-chip
            color="primary"
            class="ma-1"
            small
            :outlined="''!==selectedTranslation.value"
            @click="changeTranslation({value: ''})">
            {{ $t('notes.label.translation.originalVersion') }}
          </v-chip>
          <div v-if="!isMobile" class="ps-2 ma-2">{{ $t('notes.label.translations') }}</div>
          <div v-if="!translations || translations.length===0" class="ps-2 ma-2 text-sub-title">{{ $t('notes.label.noTranslations') }}</div>
          <v-chip
            v-for="(translation, i) in translationToShow"
            :key="i"
            close
            small
            :outlined="translation.value!==selectedTranslation.value"
            color="primary"
            @click:close="removeTranslation(translation)"
            @click="changeTranslation(translation)"
            class="ma-1">
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
              v-bind="attrs"
              v-on="on">
              <v-avatar
                size="
              32"
                class="notDisplayedIdentitiesOverlay">
                <div class="notDisplayedIdentities d-flex align-center justify-center">
                  +{{ moreTranslations.length }}
                </div>
              </v-avatar>
            </v-btn>
            <v-chip
              v-show="isMobile"
              color="primary"
              class="ma-1"
              small
              v-bind="attrs"
              v-on="on">
              {{ selectedTranslation.value!==''?selectedTranslation.text:$t('notes.label.translation.originalVersion') }}
            </v-chip>
          </template>

          <v-list class="pa-0">
            <v-list-item
              v-for="(item, i) in moreTranslations"
              :key="i"
              class="pa-0 translation-chips">
              <v-chip
                close
                small
                :outlined="item.value!==selectedTranslation.value"
                color="primary"
                @click:close="removeTranslation(item)"
                @click="changeTranslation(item)"
                class="ma-2">
                {{ item.text }}
              </v-chip>
            </v-list-item>
          </v-list>
        </v-menu>
      </div>
    </div>
    <div class="px-2  my-auto  height-auto width-auto  d-flex flex-nowrap">
      <div v-if="!isMobile" class="ma-2">{{ $t('notes.label.translation.add') }}</div>
      <select
        id="translationBarFilterSelect"
        v-model="selectedLang"
        class="ignore-vuetify-classes py-2 height-auto width-auto text-truncate my-auto mx-2">
        <option
          v-for="item in languages"
          :key="item.value"
          :value="item">
          {{ item.text }}
        </option>
      </select>
      <v-btn
        class="my-auto "
        small
        icon
        @click="add()">
        <v-icon size="18" class="icon-default-color py-3">fa-plus</v-icon>
      </v-btn>
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
  },

  data: () => ({
    moreTranslationsMenu: false,
    showTranslationbar: false,
    languages: [],
    selectedLang: {},
    selectedTranslation: {value: ''},
    displayActionMenu: true,
    noteLanguages: [],
    translations: null,
  }),

  mounted() {
    $(document).on('click', () => {
      this.moreTranslationsMenu = false;
    });

  },

  created() {
    this.getAvailableLanguages();
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
    isMobile() {
      return this.$vuetify.breakpoint.name === 'xs';
    },
    limitTranslationsToShow() {
      return this.isMobile?1:3;
    }

  },

  methods: {
    show() {
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
      this.translations.unshift(this.selectedLang);
      this.selectedTranslation=this.selectedLang;
      this.languages = this.languages.filter(item => item.value !== this.selectedLang.value);
      this.$root.$emit('add-translation', this.selectedLang.value);
      this.selectedLang={value: '',text: this.$t('notes.label.chooseLangage')};
      
    },
    changeTranslation(translation){
      if (translation.value!=='') {
        this.translations=this.translations.filter(item => item.value !== translation.value);
        this.translations.unshift(translation);  
      }
      this.selectedTranslation=translation;
      this.$root.$emit('lang-translation-changed', this.selectedTranslation.value);
    },
    getAvailableLanguages(){
      return this.$notesService.getAvailableLanguages().then(data => {
        this.languages = data || [];
        this.languages.unshift({value: '',text: this.$t('notes.label.chooseLangage')});
        if (this.translations){
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }       
      });
    },
    getNoteLanguages(){
      return this.$notesService.getNoteLanguages(this.noteId).then(data => {
        this.translations =  data || [];
        if (this.translations.length>0) {
          this.translations = this.languages.filter(item1 => this.translations.some(item2 => item2 === item1.value));
          this.languages = this.languages.filter(item1 => !this.translations.some(item2 => item2.value === item1.value));
        }
        if (this.isMobile) {
          this.translations.unshift({value: '',text: this.$t('notes.label.translation.originalVersion')});
        }
      });
    },
    removeTranslation(translation){
      return this.$notesService.deleteNoteTranslation(this.noteId,translation.value).then(() => {
        this.translations=this.translations.filter(item => item.value !== translation.value);
      });
    },
  }
};
</script>