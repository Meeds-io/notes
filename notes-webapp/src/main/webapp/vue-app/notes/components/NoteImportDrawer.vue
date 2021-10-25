<template>
  <div>
    <exo-drawer
      ref="importNotesDrawer"
      class="notesImportDrawer"
      v-model="drawer"
      show-overlay
      right>
      <template slot="title">
        {{ $t('notes.label.importNotes') }}
      </template>
      <template slot="content">
        <div>
          <template>
            <v-stepper
              v-model="e6"
              vertical
              flat
              class="ma-0 py-0 me-4">
              <v-stepper-step
                :complete="e6 > 1"
                step="1">
                {{ $t('notes.label.importChoice') }}
              </v-stepper-step>

              <v-stepper-content step="1">
                <attachments-notes-upload-input
                  :attachments="value"
                  :max-files-count="maxFilesCount"
                  :max-files-size="maxFileSize" />

                <attachments-uploaded-notes
                  :attachments="value"
                  :max-files-count="maxFilesCount"
                  :max-files-size="maxFileSize" />

                <v-card-actions class="px-0">
                  <v-spacer />
                  <v-btn
                    class="btn btn-primary"
                    outlined
                    @click="e6 = 2">
                    {{ $t('notes.label.button.continue') }}
                    <v-icon size="18" class="ms-2">
                      {{ $vuetify.rtl && 'fa-caret-left' || 'fa-caret-right' }}
                    </v-icon>
                  </v-btn>
                </v-card-actions>
              </v-stepper-content>

              <v-stepper-step
                :complete="e6 > 2"
                step="2">
                {{ $t('notes.label.importRules') }}
              </v-stepper-step>

              <v-stepper-content step="2">
                <template>
                  <v-container class="mt-n5">
                    <v-radio-group
                      v-model="selected"
                      column
                      >
                      <v-radio
                        :label="$t('notes.label.importRules1')"
                        value="overwrite"
                      ></v-radio>
                      <v-radio
                        :label="$t('notes.label.importRules2')"
                        value="update"
                      ></v-radio>
                      <v-radio
                        :label="$t('notes.label.importRules3')"
                        value="duplicate"
                      ></v-radio>
                    </v-radio-group>
                  </v-container>
                </template>
                <v-card-actions class="mt-4 px-0">
                  <v-btn
                    class="btn"
                    @click="e6 = 1">
                    <v-icon size="18" class="me-2">
                      {{ $vuetify.rtl && 'fa-caret-right' || 'fa-caret-left' }}
                    </v-icon>
                    {{ $t('notes.label.button.back') }}
                  </v-btn>
                </v-card-actions>
              </v-stepper-content>
            </v-stepper>
          </template>
        </div>
      </template>
      <template slot="footer">
        <div class="d-flex">
          <v-spacer />
          <v-btn
            class="btn me-2"
            @click="cancel">
            <template>
              {{ $t('notes.button.cancel') }}
            </template>
          </v-btn>
          <v-btn
            class="btn btn-primary"
            @click="importNotes">
            <template>
              {{ $t('notes.button.import') }}
            </template>
          </v-btn>
        </div>
      </template>
    </exo-drawer>
  </div>
</template>
<script>
export default {
  props: {

    maxFileSize: {
      type: Number,
      default: parseInt(`${eXo.env.portal.maxFileSize}`)
    },
    maxFilesCount: {
      type: Number,
      required: false,
      default: parseInt(`${eXo.env.portal.maxToUpload}`)
    },
  },
  data() {
    return {
      e6: 1,
      selected: 'nothing',
      value: [],
    };
  },
  created() {
    this.$root.$on('add-new-uploaded-file', file => {
      this.value = [];
      this.value.push(file);
    });
    this.$root.$on('delete-uploaded-file', () => {
      this.value = [];
    });
  },
  methods: {
    open() {
      this.$refs.importNotesDrawer.open();
    },
    cancel() {
      this.$refs.importNotesDrawer.close();
    },
    importNotes(){
      this.$root.$emit('import-notes',this.value[0].uploadId,this.selected);
      this.cancel();
    },
  }
};
</script>