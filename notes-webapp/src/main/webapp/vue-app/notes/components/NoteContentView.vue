<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2026 Meeds Association contact@meeds.io

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
  <div
    v-sanitized-document="contentToDisplay"
    class="rich-editor-content extended-rich-content"></div>
</template>
<script>
import NoteContentChildrenTree from './NoteContentChildrenTree.vue';

// The note body is rendered as sanitized HTML and never compiled as a Vue
// template: nothing an author writes in it is evaluated. The children tree of
// the navigation block is the only component in it, mounted by this component
// in each top-level navigation wrapper, with the page's values as props.
export default {
  props: {
    content: {
      type: String,
      default: '',
    },
    noteId: {
      type: [Number, String],
      default: null,
    },
    noteBookType: {
      type: String,
      default: '',
    },
    noteBookOwner: {
      type: String,
      default: '',
    },
  },
  computed: {
    contentToDisplay() {
      return this.content && this.$noteUtils.getContentToDisplay(this.content, true) || '';
    },
  },
  created() {
    // not reactive: the mounted trees are not part of this component's render
    this.childrenTrees = [];
  },
  mounted() {
    this.mountChildrenTrees();
  },
  updated() {
    // the directive has rendered the new content, and dropped the trees with
    // the elements they were mounted in
    this.mountChildrenTrees();
  },
  beforeDestroy() {
    this.destroyChildrenTrees();
  },
  methods: {
    mountChildrenTrees() {
      this.destroyChildrenTrees();
      const props = {
        noteId: this.noteId,
        noteBookType: this.noteBookType,
        noteBookOwner: this.noteBookOwner,
      };
      // the wrappers $noteUtils.getContentToDisplay emptied
      Array.from(this.$el.children)
        .filter(element => element.classList.value.includes('navigation-img-wrapper'))
        .forEach(wrapper => {
          const tree = new Vue({
            parent: this,
            render: h => h(NoteContentChildrenTree, {props}),
          });
          wrapper.appendChild(tree.$mount().$el);
          this.childrenTrees.push(tree);
        });
    },
    destroyChildrenTrees() {
      this.childrenTrees.forEach(tree => {
        tree.$destroy();
        tree.$el.remove();
      });
      this.childrenTrees = [];
    },
  },
};
</script>
