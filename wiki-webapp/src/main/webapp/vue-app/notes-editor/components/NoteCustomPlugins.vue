<template>
  <exo-drawer
    ref="customPluginsDrawer"
    class="customPluginsDrawer"
    body-classes="hide-scroll decrease-z-index-more"
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
</template>

<script>
export default {
  data: () => ({
    plugins: [
      { id: 'selectImage',title: 'Image', src: '/wiki/images/photo.png' },
      { id: 'video',title: 'Video', src: '/wiki/images/video.png' },
      { id: 'table',title: 'Table', src: '/wiki/images/table.png' },
      { id: 'note',title: 'Note', src: '/wiki/images/notes.png' },
      /*{ id: 'ToC',title: 'ToC', src: '/wiki/images/children.png' },
      { id: 'index',title: 'Index', src: '/wiki/images/index.png' },
      { id: 'iframe',title: 'IFrame', src: '/wiki/images/iframe.png' },
      { id: 'code',title: 'Code', src: '/wiki/images/code.png' },*/
    ],
    defaultImagePlugin: '/wiki/images/defaultPlugin.png'
  }),
  props: {
    instance: {
      type: Object,
      default: () => null,
    },
  },
  created() {
    this.$root.$on('close-drawer', () => {
      this.close();
    });
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
      }
    }
  }
};
</script>