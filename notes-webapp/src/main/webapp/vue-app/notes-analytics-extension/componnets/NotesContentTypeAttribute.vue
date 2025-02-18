<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2025 Meeds Association contact@meeds.io

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
  <span>
    {{ attributeValue }}
  </span>
</template>

<script>
export default {
  data() {
    return {
      pageName: null,
      pageRefPattern: /^[a-z_]+::[a-z_]+::[a-z_]+$/
    };
  },
  props: {
    attrValue: {
      type: String,
      default: null
    }
  },
  computed: {
    attributeValue() {
      return this.pageName && this.$t('analytics.snv.title', {0: this.pageName}) || this.attrValue;
    }
  },
  async created() {
    if (!this.pageRefPattern.test(this.attrValue)) {
      return;
    }
    const page = await this.getPage();
    if (page?.key?.name) {
      this.pageName = page.key.name;
    }
  },
  methods: {
    async getPage() {
      try {
        const response = await fetch(`/layout/rest/pages/byRef?pageRef=${this.attrValue}`, {
          method: 'GET',
          credentials: 'include',
        });
        if (!response.ok) {
          console.error(`Error retrieving page: ${response.statusText}`);
          return null;
        }
        return await response.json();
      } catch (error) {
        console.error('Fetch error:', error);
        return null;
      }
    }
  }
};
</script>
