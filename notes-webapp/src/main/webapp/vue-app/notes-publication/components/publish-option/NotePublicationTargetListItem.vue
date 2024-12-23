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
      {{ targetTooTip }}
    </p>
  </v-tooltip>
</template>

<script>
export default {
  data() {
    return {
      lang: eXo.env.portal.language
    };
  },
  props: {
    target: {
      type: Object,
      default: null
    }
  },
  computed: {
    formattedPublishedDate() {
      return new Date(this.target?.publishedDate).toLocaleDateString(this.lang);
    },
    hasPublishedDate() {
      return !!this.target?.publishedDate;
    },
    targetTooTip() {
      return this.hasPublishedDate && `${this.formattedPublishedDate} - ${this.target.tooltipInfo}`
                                   || this.target.tooltipInfo;
    }
  }
};
</script>
