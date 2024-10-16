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
    <v-overlay
      id="target-drawer-overlay"
      z-index="2100"
      :value="drawer"
      @click.native="drawer = false" />
    <exo-drawer
      id="publicationTargetsListDrawer"
      ref="publicationTargetsListDrawer"
      v-model="drawer"
      :right="!$vuetify.rtl">
      <template slot="title">
        <div class="d-flex my-auto text-header font-weight-bold text-color">
          <v-btn
            icon
            @click="close">
            <v-icon size="20">
              fas fa-arrow-left
            </v-icon>
          </v-btn>
          <span class="ms-2 my-auto">
            {{ $t('notes.publication.targets.label') }}
          </span>
        </div>
      </template>
      <template slot="content">
        <div class="pa-5">
          <note-publication-target-list
            :targets="targets"
            @unselect="unselectPublicationTarget" />
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
export default {
  data() {
    return {
      drawer: false,
      targets: []
    };
  },
  created() {
    this.$root.$on('open-publication-target-list-drawer', this.open);
    this.$root.$on('close-publication-target-list-drawer', this.close);
  },
  methods: {
    unselectPublicationTarget(targetName) {
      this.$root.$emit('unselect-publication-target', targetName);
    },
    open(targets) {
      this.targets = targets;
      this.$refs.publicationTargetsListDrawer.open();
    },
    close() {
      this.$refs.publicationTargetsListDrawer.close();
    }
  }
};
</script>
