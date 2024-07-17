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
  <div v-if="spaces.length">
    <div class="text-header mb-2">
      {{ $t('gamification.event.display.goThere') }}
    </div>
    <v-progress-linear
      v-if="!initialized"
      indeterminate
      height="2"
      color="primary" />
    <meeds-stream-event-space-item
      v-for="space in spaces"
      :key="space.spaceId"
      :space="space" />
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
      activity: null,
      spaces: [],
      initialized: false
    };
  },
  computed: {
    spaceIds() {
      return this.properties?.spaceIds;
    },
    activityId() {
      return this.properties?.activityId;
    },
    activityTitle() {
      return this.activity?.poll?.question || this.activity?.title;
    },
    activityUrl() {
      return this.activityId && `${eXo.env.portal.context}/${eXo.env.portal.metaPortalName}/activity?id=${this.activityId}`;
    },
  },
  created() {
    if (this.activityId) {
      this.loadActivity();
    } else if (this.spaceIds) {
      this.loadSpaces();
    }
  },
  methods: {
    loadActivity() {
      return this.$activityService.getActivityById(this.activityId)
        .then(fullActivity => {
          this.activity = fullActivity;
        });
    },
    loadSpaces() {
      this.spaceIds.split(',').forEach((spaceId, index) => {
        this.$spaceService.getSpaceById(spaceId)
          .then((space) => {
            this.spaces.push(space);
          }).finally(() => {
            if (index === this.spaceIds.split(',').length - 1) {
              this.initialized = true;
            }
          });
      });
    },
  }
};
</script>