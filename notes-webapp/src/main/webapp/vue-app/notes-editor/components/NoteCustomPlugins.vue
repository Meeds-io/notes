<template>
  <div>
    <v-overlay
      z-index="1031"
      :value="drawer"
      @click.native="drawer = false" />
    <exo-drawer
      ref="customPluginsDrawer"
      v-model="drawer"
      show-overlay
      class="customPluginsDrawer"
      right>
      <template slot="title">
        {{ $t('notes.label.customPlugins') }}
      </template>
      <template slot="content">
        <div slot="content" class="content">
          <v-row class="mandatory pluginsContainer d-flex flex-wrap width-full ml-0">
            <v-col v-model="plugins" class="pluginsList d-flex flex-wrap width-full ">
              <div
                v-for="(plugin, index) in plugins"
                :id="'plugin-' + index"
                :key="index"
                class="pluginsItemContainer">
                <div
                  :id="'pluginItem-' + index"
                  class="pluginItem pa-4">
                  <v-tooltip>
                    <template v-slot:activator="{ on, attrs }">
                      <a
                        v-bind="attrs"
                        v-on="on"
                        :id="plugin.id"
                        :target="plugin.title"
                        @click="openPlugin(plugin.id)">
                        <img
                          v-if="plugin.src && plugin.src.length"
                          class="pluginImage bloc"
                          :src="plugin.src">
                        <img
                          v-else
                          class="pluginImage block"
                          :src="defaultImagePlugin">
                        <span
                          class="pluginTitle text-truncate">{{ $t(`notes.label.${plugin.title}`) }}
                        </span>
                      </a>
                    </template>
                    <span class="caption">{{ plugin.tooltip }}</span>
                  </v-tooltip>
                </div>
              </div>
            </v-col>
          </v-row>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>

<script>
export default {
  data: () => ({
    defaultImagePlugin: '/notes/images/defaultPlugin.png',
    drawer: false,
    hideTOC: true,
    noteId: '',
    noteChildren: []
  }),
  computed: {
    plugins() {
      const pluginsList = [
        { id: 'video',title: 'Video', src: '/notes/images/video.png', tooltip: this.$t('notes.label.insertVideo') },
        { id: 'table',title: 'Table', src: '/notes/images/table.png', tooltip: this.$t('notes.label.insertTable') },
        { id: 'note',title: 'Note', src: '/notes/images/notes.png', tooltip: this.$t('notes.label.insertNote')  },
        { id: 'ToC',title: 'ToC', src: '/notes/images/children.png', tooltip: this.$t('notes.label.itoc')  },                                    
      /*{ id: 'index',title: 'Index', src: '/notes/images/index.png' },
      { id: 'iframe',title: 'IFrame', src: '/notes/images/iframe.png' },
      { id: 'code',title: 'Code', src: '/notes/images/code.png' },*/
      ];
      if (eXo.ecm){
        pluginsList.unshift({ id: 'selectImage',title: 'Image', src: '/notes/images/photo.png', tooltip: this.$t('notes.label.insertImage')  });
      }
      if (this.hideTOC ) {
        return pluginsList.filter( plugin => plugin.id !== 'ToC' );
      } else {
        return pluginsList;
      }
    },
  },
  props: {
    instance: {
      type: Object,
      default: () => null,
    }
  },
  created() {
    const queryPath = window.location.search;
    const urlParams = new URLSearchParams(queryPath);
    if (urlParams.has('noteId')) {
      this.noteId = urlParams.get('noteId');
      this.retrieveNoteChildren(this.noteId);
    }
  },
  methods: {
    open() {
      this.$refs.customPluginsDrawer.open();
    },
    close() {
      this.$refs.customPluginsDrawer.close();
    },
    retrieveNoteChildren(noteId) {
      this.$notesService.getChildrensByNoteId(noteId).then(data => {
        this.noteChildren = data || [];
        if (this.noteChildren.length) {
          this.hideTOC = false;
        }
      });
    },
    openPlugin(id){
      if (id==='table'){
        this.$root.$emit('note-table-plugins');
      } else if ( id === 'note') {
        this.$root.$emit('display-treeview-items');
      } else if ( id === 'ToC') {
        this.instance.execCommand(id, this.noteChildren);
      }
      else {
        this.instance.execCommand(id);
      }
    }
  }
};
</script>