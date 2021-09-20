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
                  <a
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
                      v-exo-tooltip.bottom.body="plugin.title"
                      class="pluginTitle text-truncate">
                      {{ $t(`notes.label.${plugin.title}`) }}
                    </span>
                  </a>
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
  }),
  computed: {
    plugins() {
      const pluginsList = [
        { id: 'video',title: 'Video', src: '/notes/images/video.png' },
        { id: 'table',title: 'Table', src: '/notes/images/table.png' },
        { id: 'note',title: 'Note', src: '/notes/images/notes.png' },
      /*{ id: 'ToC',title: 'ToC', src: '/notes/images/children.png' },
      { id: 'index',title: 'Index', src: '/notes/images/index.png' },
      { id: 'iframe',title: 'IFrame', src: '/notes/images/iframe.png' },
      { id: 'code',title: 'Code', src: '/notes/images/code.png' },*/
      ];
      if (eXo.ecm){
        pluginsList.unshift({ id: 'selectImage',title: 'Image', src: '/notes/images/photo.png' });
      }
      return pluginsList;
    },
  },
  props: {
    instance: {
      type: Object,
      default: () => null,
    },
  },
  methods: {
    open() {
      this.$refs.customPluginsDrawer.open();
    },
    close() {
      this.$refs.customPluginsDrawer.close();
    },
    openPlugin(id){
      if (id==='table'){
        this.$root.$emit('note-table-plugins');
      } else if ( id === 'note') {
        this.$root.$emit('display-treeview-items');
      } else {
        this.instance.execCommand(id);
        this.close();
      }
    }
  }
};
</script>