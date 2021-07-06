<template>
  <exo-drawer
    ref="customTableDrawer"
    class="customTableDrawer"
    body-classes="hide-scroll decrease-z-index-more"
    right>
    <template slot="title">
      <div class="d-flex">
        <i class="uiIcon uiArrowBAckIcon" @click="close"></i>
        <span class="ps-2">{{ $t('notes.plugin.table') }}</span>
      </div>
    </template>
    <template slot="content">
      <v-container fluid>
        <div class="d-flex">
          <v-subheader class="px-0">
            {{ $t('notes.plugin.table.size') }}
          </v-subheader>
          <v-divider class="spacesOverviewHorizontalSeparator mx-2 ma-auto" />
        </div>

        <v-row align="center">
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.lines') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="lines"
              type="number"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.columns') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="columns"
              type="number"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.width') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="width"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.height') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="height"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
        </v-row>
      </v-container>
      <v-container fluid>
        <div class="d-flex">
          <v-subheader class="px-0">
            {{ $t('notes.plugin.table.adjustment') }}
          </v-subheader>
          <v-divider class="spacesOverviewHorizontalSeparator mx-2 ma-auto" />
        </div>

        <v-row align="center">
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.header') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <select
              v-model="headerSelected"
              name="priority"
              class="input-block-level ignore-vuetify-classes my-3">
              <option
                v-for="item in header"
                :key="item.name"
                :value="item.name">
                {{ $t('label.header.'+item.name.toLowerCase()) }}
              </option>
            </select>
          </v-col>
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.border') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="border"
              type="number"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
          <v-col cols="9">
            <v-subheader>
              {{ $t('notes.plugin.table.spacing') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="spacing"
              type="number"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
          <v-col cols="9">
            <v-subheader>
              {{ $t('notes.plugin.table.internal') }}
            </v-subheader>
          </v-col>

          <v-col cols="3">
            <v-text-field
              v-model="internal"
              type="number"
              :rules="maxRules"
              dense
              min-height
              outlined />
          </v-col>
        </v-row>
      </v-container>
      <v-container fluid>
        <div class="d-flex">
          <v-subheader class="px-0">
            {{ $t('notes.plugin.table.alignment') }}
          </v-subheader>
          <v-divider class="spacesOverviewHorizontalSeparator mx-2 ma-auto" />
        </div>

        <v-row align="center">
          <v-col cols="3">
            <v-subheader>
              {{ $t('notes.plugin.table.alignment') }}
            </v-subheader>
          </v-col>
          <v-col cols="3">
            <select
              v-model="alignmentSelected"
              name="priority"
              class="input-block-level ignore-vuetify-classes my-3">
              <option
                v-for="item in alignment"
                :key="item.name"
                :value="item.name">
                {{ $t('label.alignment.'+item.name.toLowerCase()) }}
              </option>
            </select>
          </v-col>
        </v-row>
      </v-container>
    </template>
    <template slot="footer">
      <div class="d-flex">
        <v-spacer />
        <div class="VuetifyApp d-flex">
          <div class="d-btn">
            <v-btn class="btn mr-2" @click="close">
              <template>
                {{ $t('popup.cancel') }}
              </template>
            </v-btn>

            <v-btn class="btn btn-primary" @click="insertTable">
              <template>
                {{ $t('label.confirm') }}
              </template>
            </v-btn>
          </div>
        </div>
      </div>
    </template>
  </exo-drawer>
</template>

<script>
export default {
  data: () => ({
    limit: 0,
    maxRules: [],
    lines: 1,
    columns: 1,
    width: 500,
    height: 25,
    border: 1,
    spacing: 1,
    internal: 0,
    alignmentSelected: '',
    alignment: [
      {name: ''},{name: 'left'},{name: 'center'},{name: 'right'}
    ],
    headerSelected: '',
    header: [
      {name: ''},{name: 'FIRST.ROW'},{name: 'FIRST.COLUMN'},{name: 'BOTH'}
    ],
  }),
  props: {
    instance: {
      type: Object,
      default: () => null,
    },
  },
  created() {
    this.maxRules = [v => v >= 0];
  },
  methods: {
    open() {
      this.$refs.customTableDrawer.open();
    },
    close() {
      this.$refs.customTableDrawer.close();
    },
    insertTable() {
      this.addTable();
      this.close();
    },
    addTable() {
      const div = document.createElement('DIV');
      const table = document.createElement('TABLE');
      div.appendChild(table);
      table.width = this.width;
      table.setAttribute( 'border', this.border );
      table.setAttribute( 'cellPadding', this.internal );
      table.setAttribute( 'height', this.height );
      table.setAttribute( 'cellSpacing', this.spacing );
      table.setAttribute( 'align', this.alignmentSelected );
      table.setAttribute( 'tHead', this.headerSelected );

      const tableBody = document.createElement('TBODY');
      table.appendChild(tableBody);

      for (let i = 0; i < this.lines; i++) {
        const tr = document.createElement('TR');
        tableBody.appendChild(tr);

        for (let j = 0; j < this.columns; j++) {
          const td = document.createElement('TD');
          td.width = '50';
          td.height = '25';
          tr.appendChild(td);
        }
      }
      this.instance.insertHtml(div.innerHTML);    }
  }
};
</script>
