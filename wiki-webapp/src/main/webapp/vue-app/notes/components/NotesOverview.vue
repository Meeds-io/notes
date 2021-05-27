<template>
  <v-app class="transparent" flat>
    <div class="notes-application border-radius ma-3 py-3 px-6">
      <div class="notes-application-header">
        <div class="notes-title d-flex justify-space-between">
          <span class=" title text-color">UX Ideation Notes</span>
          <div class="notes-header-icons">
            <v-icon
              size="22"
              class="clickable"
              :title="$t('notes.label.addPage')">
              mdi-plus
            </v-icon>
            <v-icon
              size="19"
              class="clickable px-1"
              :title="$t('notes.label.editPage')">
              mdi-square-edit-outline
            </v-icon>
            <v-icon
              size="19"
              class="clickable"
              :title="$t('notes.label.openMenu')">
              mdi-dots-vertical
            </v-icon>
          </div>
        </div>
        <div class="notes-treeview d-flex pb-2">
          <i class="uiIcon uiTreeviewIcon"></i>
          <div
            v-for="(node, index) in notesTreeview" 
            :key="index" 
            class="notes-tree-item">
            <span class="caption">{{ node.name }}</span>
            <v-icon v-if="index+1 < notesTreeview.length" size="18">mdi-chevron-right</v-icon>
          </div>
        </div>
        <div class="notes-last-update-info">
          <span class="caption grey-light-color font-italic">Lorem ipsum dolor si amet lorem, lorem ipsum dolor</span>
        </div>
      </div>
      <v-divider class="my-4" />
      <div class="notes-application-content">
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
        <p class="text-color">Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.</p>
      </div>
    </div>
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