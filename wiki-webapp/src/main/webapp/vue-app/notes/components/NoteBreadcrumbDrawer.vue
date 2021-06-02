<template>
  <exo-drawer
    ref="breadcrumbDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template slot="title">
       title 
    </template>
    <template slot="content">
     <p>test</p>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data: () => ({
  }),
  methods: {
    open() {
      this.$nextTick().then(() => {
        this.$refs.breadcrumbDrawer.open();
      });
    },
  }
};
</script>