<template>
  <div class="note-breadcrumb-wrapper">
    <div v-if="noteBreadcrumb && noteBreadcrumb.length <= 4" class="notes-tree-items d-flex">
      <div
        v-for="(note, index) in noteBreadcrumb"
        :key="index"
        :class="noteBreadcrumb.length === 1 && 'single-path-element' || ''"
        class="notes-tree-item d-flex text-truncate"
        :style="`max-width: ${100 / (noteBreadcrumb.length)}%`">
        <v-tooltip max-width="300" bottom>
          <template v-slot:activator="{ on, attrs }">
            <a 
              v-bind="attrs"
              v-on="on"
              @click="$emit('open-note',note.id)"
              class="caption text-truncate breadCrumb-link"
              :class="index < noteBreadcrumb.length-1 && 'path-clickable text-color' || 'text-sub-title not-clickable'">{{ note.title }}</a>
          </template>
          <span class="caption">{{ note.title }}</span>
        </v-tooltip>
        <v-icon v-if="index < noteBreadcrumb.length-1" size="18">mdi-chevron-right</v-icon>
      </div>
    </div>
    <div v-else class="notes-tree-items notes-long-path d-flex align-center">
      <div class="notes-tree-item long-path-first-item d-flex text-truncate">
        <v-tooltip max-width="300" bottom>
          <template v-slot:activator="{ on, attrs }">
            <a
              class="caption text-color text-truncate path-clickable breadCrumb-link"
              v-bind="attrs"
              v-on="on"
              @click="$emit('open-note',noteBreadcrumb[0].id)">{{ noteBreadcrumb[0].title }}</a>
          </template>
          <span class="caption">{{ noteBreadcrumb[0].title }}</span>
        </v-tooltip>
        <v-icon size="18">mdi-chevron-right</v-icon>
      </div>
      <div class="notes-tree-item long-path-second-item d-flex">
        <v-tooltip bottom>
          <template v-slot:activator="{ on, attrs }">
            <v-icon
              v-bind="attrs"
              v-on="on"
              size="24">
              mdi-dots-horizontal
            </v-icon>
          </template>
          <p
            v-for="(note, index) in noteBreadcrumb"
            :key="index"
            class="mb-0">
            <span v-if="index > 0 && index < noteBreadcrumb.length-2" class="caption"><v-icon size="18" class="tooltip-chevron">mdi-chevron-right</v-icon> {{ note.title }}</span>
          </p>
        </v-tooltip>
        <v-icon class="clickable" size="18">mdi-chevron-right</v-icon>
      </div>
      <div class="notes-tree-item long-path-third-item d-flex text-truncate">
        <v-tooltip max-width="300" bottom>
          <template v-slot:activator="{ on, attrs }">
            <a
              class="caption text-color text-truncate path-clickable breadCrumb-link"
              v-bind="attrs"
              v-on="on"
              @click="$emit('open-note',noteBreadcrumb[noteBreadcrumb.length-2].id)">{{ noteBreadcrumb[noteBreadcrumb.length-2].title }}</a>
          </template>
          <span class="caption">{{ noteBreadcrumb[noteBreadcrumb.length-2].title }}</span>
        </v-tooltip>
        <v-icon size="18">mdi-chevron-right</v-icon>
      </div>
      <div class="notes-tree-item d-flex text-truncate">
        <v-tooltip max-width="300" bottom>
          <template v-slot:activator="{ on, attrs }">
            <a
              class="caption text-color text-truncate text-sub-title breadCrumb-link"
              v-bind="attrs"
              v-on="on"
              @click="$emit('open-note',noteBreadcrumb[noteBreadcrumb.length-1].id)">{{ noteBreadcrumb[noteBreadcrumb.length-1].title }}</a>
          </template>
          <span class="caption">{{ noteBreadcrumb[noteBreadcrumb.length-1].title }}</span>
        </v-tooltip>
      </div>
    </div>
  </div>
</template>
<script>
export default {
  props: {
    noteBreadcrumb: {
      type: Array,
      default: () => null
    }
  },
  data() {
    return {
      //noteBreadcrumb: []
    };
  },
  /*created() {
    this.$root.$on('update-breadcrumb', noteBreadcrumbArray => {
      this.noteBreadcrumb = noteBreadcrumbArray;
    });
  },*/
};
</script>