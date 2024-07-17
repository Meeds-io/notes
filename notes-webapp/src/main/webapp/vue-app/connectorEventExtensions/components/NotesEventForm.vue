<!--
This file is part of the Meeds project (https://meeds.io/).

Copyright (C) 2020 - 2024 Meeds Association contact@meeds.io

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
    <v-card-text class="px-0 pb-0">
      {{ $t('gamification.event.detail.notes.label') }}
    </v-card-text>
    <v-radio-group v-model="space" @change="changeSelection">
      <v-radio
        value="ANY"
        :label="$t('gamification.event.detail.anyNote.label')" />
      <v-radio value="ANY_IN_SPACE" :label="$t('gamification.event.detail.noteInSpace.label')" />
      <exo-identity-suggester
        v-if="space === 'ANY_IN_SPACE'"
        ref="spacesSuggester"
        v-model="selected"
        :labels="spaceSuggesterLabels"
        :include-users="false"
        :width="220"
        name="spacesSuggester"
        class="user-suggester mt-n2"
        include-spaces
        multiple />
    </v-radio-group>
  </div>
</template>

<script>
export default {
  props: {
    properties: {
      type: Object,
      default: null
    },
    trigger: {
      type: String,
      default: null
    },
  },
  data() {
    return {
      anySpace: true,
      selected: [],
      space: 'ANY',
    };
  },
  computed: {
    spaceSuggesterLabels() {
      return {
        placeholder: this.$t('activity.composer.audience.placeholder'),
        noDataLabel: this.$t('activity.composer.audience.noDataLabel'),
      };
    },
  },
  watch: {
    selected() {
      if (this.selected?.length) {
        const eventProperties = {
          spaceIds: this.selected.map(space => space.spaceId).toString(),
        };
        document.dispatchEvent(new CustomEvent('event-form-filled', {detail: eventProperties}));
      } else if (this.space === 'ANY_IN_SPACE'){
        document.dispatchEvent(new CustomEvent('event-form-unfilled'));
      }
    },
    trigger() {
      this.space = 'ANY';
      document.dispatchEvent(new CustomEvent('event-form-filled'));
    },
  },
  created() {
    if (this.properties?.spaceIds) {
      this.space = 'ANY_IN_SPACE';
      this.properties?.spaceIds.split(',').forEach(spaceId => {
        this.$spaceService.getSpaceById(spaceId)
          .then(spaceData=> {
            const space = {
              id: `space:${spaceData.prettyName}`,
              profile: {
                avatarUrl: spaceData.avatarUrl,
                fullName: spaceData.displayName,
              },
              providerId: 'space',
              remoteId: spaceData.prettyName,
              spaceId: spaceData.id,
              displayName: spaceData.displayName,
            };
            this.selected.push(space);
          });
      });
    } else if (this.properties?.activity === 'any') {
      this.space = 'ANY';
    } else {
      document.dispatchEvent(new CustomEvent('event-form-filled'));
    }
  },
  methods: {
    changeSelection() {
      this.selected = [];
      if (this.space === 'ANY') {
        document.dispatchEvent(new CustomEvent('event-form-filled'));
      } else {
        document.dispatchEvent(new CustomEvent('event-form-unfilled'));
      }
    },
  }
};
</script>