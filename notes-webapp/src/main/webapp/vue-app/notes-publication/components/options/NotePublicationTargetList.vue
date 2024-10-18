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
    <div
      v-for="target in targetList"
      :key="target.name"
      class="d-flex align-center">
      <v-tooltip bottom>
        <template #activator="{ on, attrs }">
          <p
            v-on="on"
            v-bind="attrs"
            class="text-truncate mb-0 flex-grow-1 text-start">
            <span
              class="font-weight-bold">
              {{ target.label }}:
            </span>
            {{ target.description }}
          </p>
        </template>
        <p class="caption mb-0">
          {{ target.tooltipInfo }}
        </p>
      </v-tooltip>
      <v-btn
        :aria-label="$t('notes.publication.remove.selected.target.label')"
        class="btn flex-column no-border ms-auto error-color"
        width="28"
        min-width="28"
        height="28"
        icon
        @click="removeSelectedTarget(target.name)">
        <v-icon
          class="error-color"
          size="20">
          fas fa-times
        </v-icon>
      </v-btn>
    </div>
  </div>
</template>

<script>
export default {
  props: {
    targets: {
      type: Array,
      default: () => []
    }
  },
  computed: {
    targetList() {
      return this.targets.filter(target => !!target.name);
    }
  },
  methods: {
    removeSelectedTarget(targetName) {
      this.$emit('unselect', targetName);
    }
  }
};
</script>
