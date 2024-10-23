<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2024 Meeds Association contact@meeds.io

 This program is free software; you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public
 License as published by the Free Software Foundation; either
 version 3 of the License, or (at your option) any later version.
 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this program; if not, write to the Free Software Foundation,
 Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
-->

<template>
  <div>
    <div class="d-flex">
      <v-switch
        v-model="publish"
        :disabled="isPublishing"
        :aria-label="$t('notes.publication.publish.in.list.label')"
        :ripple="false"
        color="primary"
        class="mt-n1 me-1" />
      <p class="mb-4">
        {{ $t('notes.publication.publish.in.list.label') }}
      </p>
    </div>
    <div v-if="publish">
      <label
        for="targets"
        class="mb-0 text-font-size">
        {{ $t('notes.publication.where.to.publish.label') }}
        <v-combobox
          v-model="selectedTargets"
          :items="allowedTargets"
          ref="targets"
          name="targets"
          :label="!filterInputFocused && $t('notes.publication.choose.location.label')"
          class="elevation-0 mt-2 pt-0 no-border"
          item-text="label"
          single-line
          return-object
          multiple
          outlined
          dense
          chips
          @update:search-input="TargetsFilterUpdated"
          @change="checkRestrictedAudience"
          @focus="filterInputFocused = true"
          @blur="filterInputFocused = false">
          <template #prepend-item>
            <v-list-item v-if="allowedTargets?.length && !hasInputFilterValue">
              <v-list-item-action>
                <v-simple-checkbox
                  v-model="allTargetsSelected"
                  :indeterminate="allTargetsSelected"
                  :aria-label="$t('notes.publication.targets.select.all.label')"
                  intermediate-icon="fas fa-minus"
                  color="primary"
                  hide-details
                  ripple
                  @mousedown.prevent.stop
                  @click="selectAllTargets" />
              </v-list-item-action>
              <v-list-item-content>
                <v-list-item-title>
                  {{ $t('notes.publication.targets.select.all.label') }}
                </v-list-item-title>
              </v-list-item-content>
            </v-list-item>
          </template>
          <template #append-outer>
            <v-btn
              v-if="hasInputFilterValue"
              class="position-absolute custom-clear-button no-border pa-1 t-2"
              icon
              @mousedown.prevent.stop
              @click="clearInputFilter">
              <v-icon
                class="icon-default-color"
                size="18">
                fas fa-times
              </v-icon>
            </v-btn>
          </template>
          <template #selection="{index}">
            <label
              v-if="!hasInputFilterValue && !index && !filterInputFocused"
              class="text-color v-label position-absolute l-0">
              {{ $t('notes.publication.choose.location.label') }}
            </label>
          </template>
        </v-combobox>
      </label>
      <note-publication-target-list
        :targets="cleanSelectedTargets?.slice(0, 4)"
        class="mt-2"
        @unselect="removeSelectedTarget" />
      <v-btn
        v-if="cleanSelectedTargets?.length > 4"
        class="btn pa-1 mt-3 text-capitalize-first-letter no-border ma-auto d-flex text-sub-title"
        text
        @click="openSelectedTargetsList">
        + {{
          $t(`notes.publication.targets.${remainTargetsLength > 1 && 'others' || 'other'}.label`,
             {0: remainTargetsLength})
        }}
      </v-btn>
      <label
        for="audience"
        class="text-font-size mt-3">
        {{ $t('notes.publication.who.will.see.label') }}
        <v-tooltip bottom>
          <template #activator="{ on, attrs }">
            <div
              v-bind="attrs"
              v-on="hasRestrictedAudience && on">
              <v-select
                ref="audience"
                v-model="selectedAudience"
                :items="audiences"
                :disabled="hasRestrictedAudience"
                class="pt-0 mt-2"
                item-text="label"
                item-value="value"
                return-object
                attach
                dense
                outlined
                @blur="$refs.audience.blur()" />
            </div>
          </template>
          <span>{{ $t('notes.publication.audience.restricted') }}</span>
        </v-tooltip>
        <span
          v-if="isAllUsersAudience"
          class="mt-1 caption justify-center">
          <v-icon
            size="16"
            class="me-3">
            fas fa-exclamation-triangle
          </v-icon>
          {{ $t('notes.publication.all.users.audience.info') }}
        </span>
      </label>
    </div>
  </div>
</template>

<script>
export default {
  data() {
    return {
      publish: false,
      selectedTargets: null,
      defaultAudience: {label: this.$t('notes.publication.only.space.members.label'), value: 'space'},
      selectedAudience: {label: this.$t('notes.publication.only.space.members.label'), value: 'space'},
      audiences: [
        {label: this.$t('notes.publication.only.space.members.label'), value: 'space'},
        {label: this.$t('notes.publication.all.users.label'), value: 'all'}
      ],
      allTargetsSelected: false,
      hasInputFilterValue: false,
      filterInputFocused: false
    };
  },
  props: {
    isPublishing: {
      type: Boolean,
      default: false
    },
    allowedTargets: {
      type: Array,
      default: () => []
    },
    savedPublishSettings: {
      type: Object,
      default: null
    },
    editMode: {
      type: Boolean,
      default: false
    },
    expanded: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    selectedTargets() {
      this.emitUpdatedSettings();
    },
    selectedAudience() {
      this.emitUpdatedSettings();
    },
    publish() {
      this.emitUpdatedSettings();
    },
    expanded() {
      setTimeout(() => {
        this.$refs.targets?.$refs?.menu?.updateDimensions();
      }, 500);
    }
  },
  computed: {
    cleanSelectedTargets() {
      return this.selectedTargets?.filter(target => !!target.name);
    },
    remainTargetsLength() {
      return this.cleanSelectedTargets?.length - 4;
    },
    hasRestrictedAudience() {
      return this.cleanSelectedTargets?.some(target => target.restrictedAudience);
    },
    isAllUsersAudience() {
      return this.selectedAudience?.value === 'all';
    },
    selectedTargetsValues() {
      return this.cleanSelectedTargets?.map(target => target.name);
    }
  },
  created() {
    this.$root.$on('unselect-publication-target', this.removeSelectedTarget);
    if (this.editMode) {
      this.initSettings();
    }
  },
  methods: {
    initSettings() {
      this.publish = this.savedPublishSettings?.published;
      this.selectedTargets = this.savedPublishSettings?.selectedTargets;
      this.selectedAudience = this.savedAudience(this.savedPublishSettings?.selectedAudience) || this.defaultAudience;
      this.checkRestrictedAudience();
    },
    savedAudience(savedAudience) {
      if (!savedAudience) {
        return null;
      }
      return this.audiences[savedAudience === 'all' && 1 || 0];
    },
    TargetsFilterUpdated(key) {
      this.hasInputFilterValue = key?.length;
    },
    emitUpdatedSettings() {
      this.$emit('updated', {
        publish: this.publish,
        selectedTargets: this.selectedTargetsValues,
        selectedAudience: this.selectedAudience?.value
      });
    },
    openSelectedTargetsList() {
      this.$root.$emit('open-publication-target-list-drawer', this.selectedTargets);
    },
    removeSelectedTarget(name) {
      this.selectedTargets.splice(this.selectedTargets.findIndex(target => target.name === name), 1);
    },
    checkRestrictedAudience() {
      if (this.hasRestrictedAudience) {
        this.selectedAudience = this.defaultAudience;
      }
      this.allTargetsSelected = this.selectedTargets?.length === this.allowedTargets?.length;
    },
    selectAllTargets() {
      this.selectedTargets = this.allTargetsSelected && [...this.allowedTargets] || [];
    },
    clearInputFilter() {
      this.$refs.targets.internalSearch = '';
    },
    cancelChanges() {
      this.initSettings();
    }
  }
};
</script>
