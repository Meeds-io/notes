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
                  :target="plugin.title">
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
      { id: 'Image',title: 'Image', src: '' },
      { id: 'Video',title: 'Video', src: '' },
      { id: 'Table',title: 'Table', src: '' },
      { id: 'Note',title: 'Note', src: '/wiki/images/wiki.png' },
      { id: 'ToC',title: 'ToC', src: '' },
      { id: 'Index',title: 'Index', src: '' },
      { id: 'IFrame',title: 'IFrame', src: '' },
      { id: 'Code',title: 'Code', src: '' },
    ],
    defaultImagePlugin: '/wiki/images/defaultPlugin.png'
  }),
  methods: {
    open() {
      this.$refs.customPluginsDrawer.open();
    },
    close() {
      this.$refs.customPluginsDrawer.close();
    },
  }
};
</script>