<template>
  <exo-drawer
    ref="breadcrumbDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template slot="title">
      {{ $t('notes.label.breadcrumbTitle') }}
    </template>
    <template slot="content">
      <v-layout column>
        <template v-if="note" class="ma-0 border-box-sizing">
          <v-list-item @click="$root.$emit('open-note',note.path)">
            <v-list-item-content>
              <v-list-item-title>{{ note.name }}</v-list-item-title>
            </v-list-item-content>
          </v-list-item>
        </template>
        <template v-if="note && note.children && note.children.length">
          <v-treeview 
            ref="treeReference"
            :items="breadcrumbItems"
            :load-children="fetchNoteChildren"
            activatable
            color="warning"
            open-on-click
            transition>
            <template v-slot:label="{ item }">
              <v-list-item-title @click="openNote(event,item)">{{ item.name }}</v-list-item-title>
            </template>
          </v-treeview>
        </template>
      </v-layout>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data: () => ({
    note: {},
    breadcrumbItems: [],
    breadcrumbItemChild: [],
    noteBookType: '',
    noteBookOwnerTree: '',
  }),
  methods: {
    open(note, noteBookType, noteBookOwnerTree) {
      this.breadcrumbItems= [];
      this.note = note;
      this.noteBookType = noteBookType;
      this.noteBookOwnerTree = noteBookOwnerTree;
      if ( this.note && this.note.children && this.note.children.length ) {
        this.note.children.forEach(noteChildren => {
          if ( noteChildren.hasChild ) {
            this.breadcrumbItems.push(
              {
                id: noteChildren.path.split('%2F').pop(),
                hasChild: noteChildren.hasChild,
                name: noteChildren.name,
                children: this.breadcrumbItemChild
              }
            );
          } else {
            this.breadcrumbItems.push(
              {
                id: noteChildren.path.split('%2F').pop(),
                hasChild: noteChildren.hasChild,
                name: noteChildren.name
              }
            );
          }
        });
      }
      this.$nextTick().then(() => {
        this.$refs.breadcrumbDrawer.open();
      });
    },
    fetchNoteChildren(childItem) {
      if ( !childItem.hasChild ) 
      {return;}
      return this.$notesService.getNoteTree(this.noteBookType,this.noteBookOwnerTree , childItem.id,'CHILDREN').then(data => {
        if (data && data.jsonList) {
          const noteChildTree = data.jsonList;
          const temporaryNoteChildren = [];
          noteChildTree.forEach(noteChildren => {
            if ( noteChildren.hasChild ) {
              temporaryNoteChildren.push(
                {
                  id: noteChildren.path.split('%2F').pop(),
                  hasChild: noteChildren.hasChild,
                  name: noteChildren.name,
                  children: []
                }
              );
            } else {
              temporaryNoteChildren.push(
                {
                  id: noteChildren.path.split('%2F').pop(),
                  hasChild: noteChildren.hasChild,
                  name: noteChildren.name
                }
              );
            }
          });
          childItem.children.push(...temporaryNoteChildren);
        }
      });
    },
    openNote(event, note) {
      if (event) {
        event.preventDefault();
        event.stopPropagation();
      }
      this.$root.$emit('open-note-by-id',note.id);
      this.$refs.breadcrumbDrawer.close();
    }
  }
};
</script>