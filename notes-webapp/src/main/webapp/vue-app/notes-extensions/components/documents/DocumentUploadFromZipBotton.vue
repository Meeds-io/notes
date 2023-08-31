<template>
  <div>
    <div v-show="isMobile && !showMobileFilter || !isMobile">
      <v-btn
        icon
        small
        class="me-2"
        @click="openDrawer()">
        <v-icon class="text-sub-title" size="16">
          fas fa-upload
        </v-icon>
      </v-btn>
    </div>
  </div>
</template>
<script>
export default {
  methods: {
    openDrawer() {
      this.$root.$emit('open-upload-zip-drawer');
    },

  },
};
</script>
