<template>
  <v-app class="notes-application transparent" flat>
    <div class="notes-application-header">
      <h3 class="notes-title">UX Ideation Notes</h3>
      <div class="notes-treeview">
        <i class="uiIcon uiTreeviewIcon"></i>
        <div
          v-for="(node, index) in notesTreeview" 
          :key="index" 
          class="notes-tree-item">
          <span>{{ node.name }}</span>
          <i v-if="index+1 < notesTreeview.length" class="uiIcon uiArrowRightIcon"></i>
        </div>
      </div>
    </div>
    <v-divider class="my-0" />
    <div class="notes-application-content"></div>
  </v-app>
</template>
<script>
export default {
  data() {
    return {
      notesTreeview: [
        {
          name: 'Node 1',
          link: '#'
        },
        {
          name: 'Node 2',
          link: '#'
        },
        {
          name: 'Node 3',
          link: '#'
        }
      ]
    };
  }
};
</script>