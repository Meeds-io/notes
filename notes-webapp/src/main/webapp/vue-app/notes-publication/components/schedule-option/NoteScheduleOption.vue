<!--
 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2024 Meeds Association contact@meeds.io

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
  <div>
    <p
      v-if="editMode && !noSavedSchedule"
      class="text-header text-header-color mb-7">
      {{ $t('notes.publication.date.label') }}
    </p>
    <v-radio-group
      v-if="editMode && savedScheduleSettings?.scheduled"
      v-model="editScheduleOption"
      :multiple="isMultipleSelectionOption"
      class="d-flex ms-n1 mt-0 pt-0">
      <v-radio
        v-if="fromExternalPage && !hasSavedUnpublishSchedule"
        :label="$t('notes.publication.externalPage.publish.cancel.label')"
        :value="CANCEL_SCHEDULE_OPTION" />
      <v-radio
        v-else-if="hasSavedUnpublishSchedule"
        :label="$t('notes.publication.publish.cancel.label')"
        :value="CANCEL_PUBLICATION_OPTION" />
      <v-radio
        v-else
        :label="$t('notes.publication.schedule.cancel.label')"
        :value="CANCEL_SCHEDULE_OPTION" />
      <v-tooltip
        v-if="canPublish"
        :disabled="!isMultipleSelectionOption"
        bottom>
        <template #activator="{ on, attrs }">
          <span
            v-bind="attrs"
            v-on="on"
            class="mb-2">
            <v-radio
              v-if="!hasSavedUnpublishSchedule"
              :disabled="isMultipleSelectionOption"
              :label="$t('notes.publication.publish.now.label')"
              value="publish_now" />
          </span>
        </template>
        {{ $t('notes.publication.schedule.publish.now.tooltip') }}
      </v-tooltip>
      <v-radio
        :label="$t('notes.publication.schedule.label')"
        value="schedule" />
    </v-radio-group>
    <div
      v-else
      class="d-flex">
      <v-switch
        v-model="schedule"
        :disabled="isPublishing"
        :aria-label="$t('notes.publication.schedule.label')"
        :ripple="false"
        color="primary"
        class="mt-n1 me-1" />
      <p class="mb-0">
        {{ $t('notes.publication.schedule.label') }}
      </p>
    </div>
    <div
      v-if="schedule"
      :class="{'d-flex': !showStartDate && expanded}"
      class="width-fit-content mt-5">
      <div
        :class="{'d-flex': !publish || expanded}"
        class="justify-start">
        <div
          :class="{'mb-2': !expanded && publish}"
          class="my-auto me-4">
          <span
            v-if="!publish">
            {{ $t('notes.publication.schedule.from.label') }}
          </span>
          <span
            v-else-if="(schedule && noSavedSchedule) || hasSavedUnpublishSchedule">
            {{ $t('notes.publication.schedule.until.label') }}
          </span>
          <v-select
            v-else
            ref="scheduleType"
            v-model="selectedScheduleType"
            :items="scheduleTypes"
            class="pt-0 flex-grow-0 width-fit-content"
            item-text="label"
            item-value="value"
            return-object
            attach
            dense
            outlined
            @blur="$refs.scheduleType.blur()" />
        </div>
        <div
          class="d-flex">
          <v-icon
            v-if="!isUntilScheduleType || expanded || !publish"
            color="primary"
            size="24">
            fas fa-calendar-check
          </v-icon>
          <div
            v-if="showStartDate"
            class="d-flex">
            <v-menu
              v-model="startDateMenu"
              :close-on-content-click="true"
              :nudge-right="40"
              transition="scale-transition"
              content-class="ms-n10"
              offset-y
              min-width="auto">
              <template #activator="{ on, attrs }">
                <v-text-field
                  v-model="formattedStartDate"
                  v-bind="attrs"
                  v-on="on"
                  :aria-label="$t('notes.publication.startDate.label')"
                  class="pt-0 ms-4 border-box-sizing flex-grow-0"
                  readonly
                  outlined
                  dense />
              </template>
              <v-date-picker
                v-model="startDate"
                :min="minStartDate"
                :locale="locale"
                @input="updateMinStartTime" />
            </v-menu>
            <time-picker
              v-model="startTime"
              :min="minStartTime"
              :aria-label="$t('notes.publication.startTime.label')"
              format="ampm"
              type="time"
              class="mb-1 " />
          </div>
        </div>
      </div>
      <div
        v-if="showEndDate"
        :class="{'mt-2': showStartDate}"
        class="d-flex justify-end">
        <v-icon
          v-if="isUntilScheduleType && !expanded"
          color="primary"
          size="24">
          fas fa-calendar-check
        </v-icon>
        <v-menu
          v-model="endDateMenu"
          :close-on-content-click="true"
          :nudge-right="40"
          transition="scale-transition"
          content-class="ms-n10"
          offset-y
          min-width="auto">
          <template #activator="{ on, attrs }">
            <v-text-field
              v-model="formattedEndDate"
              v-bind="attrs"
              v-on="on"
              :aria-label="$t('notes.publication.endDate.label')"
              class="pt-0 ms-4 border-box-sizing flex-grow-0"
              readonly
              outlined
              dense />
          </template>
          <v-date-picker
            v-model="endDate"
            :min="minEndDate"
            :locale="locale"
            @input="updateEndMinTime" />
        </v-menu>
        <time-picker
          v-model="endTime"
          :min="minEndTime"
          :aria-label="$t('notes.publication.endTime.label')"
          format="ampm"
          type="time"
          class="mb-1 " />
      </div>
    </div>
  </div>
</template>

<script>
export const SCHEDULE_OPTION = 'schedule';
export const PUBLISH_NOW_OPTION = 'publish_now';
export const CANCEL_SCHEDULE_OPTION = 'cancel_schedule';
export const CANCEL_PUBLICATION_OPTION = 'cancel_unpublish';
export default {
  data() {
    const { startDate, minStartDate, endDate } = this.initDateValues();
    const betweenScheduleType = {label: this.$t('notes.publication.schedule.between.label'), value: 'between'};
    const untilScheduleType = {label: this.$t('notes.publication.schedule.until.label'), value: 'until'};
    const fromScheduleType = {label: this.$t('notes.publication.schedule.from.label'), value: 'from'};
    return {
      CANCEL_SCHEDULE_OPTION,
      CANCEL_PUBLICATION_OPTION,
      editScheduleOption: null,
      schedule: false,
      betweenScheduleType: betweenScheduleType,
      untilScheduleType: untilScheduleType,
      fromScheduleType: fromScheduleType,
      selectedScheduleType: betweenScheduleType,
      scheduleTypes: [
        betweenScheduleType,
        fromScheduleType,
        untilScheduleType
      ],
      startDate: startDate,
      minStartDate: minStartDate,
      minEndDate: startDate,
      endDate: endDate,
      startTime: '08:00',
      endTime: '18:00',
      minStartTime: '',
      minEndTime: '',
      startDateMenu: false,
      endDateMenu: false,
      locale: eXo?.env?.portal?.language
    };
  },
  props: {
    isPublishing: {
      type: Boolean,
      default: false
    },
    publish: {
      type: Boolean,
      default: false
    },
    expanded: {
      type: Boolean,
      default: false
    },
    savedScheduleSettings: {
      type: Object,
      default: null
    },
    isActivityPosted: {
      type: Boolean,
      default: false
    },
    editMode: {
      type: Boolean,
      default: false
    },
    canPublish: {
      type: Boolean,
      default: false
    },
    fromExternalPage: {
      type: Boolean,
      default: false
    }
  },
  watch: {
    schedule() {
      this.resetDateValues();
      this.emitUpdatedSettings();
    },
    selectedScheduleType() {
      this.resetDateValues();
      this.emitUpdatedSettings();
    },
    startDate() {
      this.emitUpdatedSettings();
    },
    endDate() {
      this.emitUpdatedSettings();
    },
    startTime() {
      this.emitUpdatedSettings();
    },
    endTime() {
      this.emitUpdatedSettings();
    },
    editScheduleOption() {
      this.handleEditScheduleOptionUpdate();
      this.emitUpdatedSettings();
    },
    isMultipleSelectionOption() {
      this.handleMultiSelectionUpdate();
    },
    showStartDate() {
      this.emitUpdatedSettings();
    },
    showEndDate() {
      this.emitUpdatedSettings();
    }
  },
  computed: {
    isMultipleSelectionOption() {
      return this.schedule && this.isUntilScheduleType && !this.hasSavedUnpublishSchedule;
    },
    formattedStartDate() {
      return this.startDate && this.formatDate(this.startDate) || '';
    },
    formattedEndDate() {
      return this.endDate && this.formatDate(this.endDate) || '';
    },
    showEndDate() {
      return ['between', 'until'].includes(this.selectedScheduleType?.value) && this.publish;
    },
    showStartDate() {
      return (this.selectedScheduleType?.value !== 'until' && this.publish) || !this.publish;
    },
    isUntilScheduleType() {
      return this.selectedScheduleType?.value === 'until';
    },
    hasSavedUnpublishSchedule() {
      return this.editMode && this.savedScheduleSettings?.scheduled
                           && !this.savedScheduleSettings?.postDate
                           && !!this.savedScheduleSettings?.unpublishDate;
    },
    hasSavedPostSchedule() {
      return !this.noSavedSchedule && !this.hasSavedUnpublishSchedule;
    },
    noSavedSchedule() {
      return this.editMode && !this.savedScheduleSettings.scheduled;
    }
  },
  created() {
    this.initSettings();
    this.updateMinStartTime();
  },
  methods: {
    handleMultiSelectionUpdate() {
      if (this.isMultipleSelectionOption) {
        this.editScheduleOption = [SCHEDULE_OPTION, PUBLISH_NOW_OPTION];
      } else if (this.isUntilScheduleType) {
        this.editScheduleOption = CANCEL_SCHEDULE_OPTION;
      } else {
        this.editScheduleOption = this.editScheduleOption[0];
      }
    },
    handleEditScheduleOptionUpdate() {
      this.schedule = this.editScheduleOption === SCHEDULE_OPTION || (this.editScheduleOption?.includes(SCHEDULE_OPTION)
          && !this.editScheduleOption.includes(CANCEL_SCHEDULE_OPTION));
    },
    resetDateValues() {
      if (this.schedule && this.noSavedSchedule) {
        const {startDate, minStartDate, endDate} = this.initDateValues();
        this.startDate = startDate;
        this.minStartDate = minStartDate;
        this.endDate = endDate;
        this.endTime = '18:00';
        this.minEndDate = minStartDate;
        this.scheduleTypes = [this.untilScheduleType];
        this.selectedScheduleType = this.scheduleTypes[0];
      } else if (this.hasSavedPostSchedule) {
        const {endDate} = this.initDateValues();
        this.endDate = endDate;
        this.endTime = '18:00';
      } else if (this.isUntilScheduleType) {
        this.minEndDate = new Date().toISOString().split('T')[0];
      } else {
        this.minEndDate = this.startDate;
      }
    },
    initDateValues() {
      const today = new Date();
      const tomorrow = new Date(today);
      const nextWeek = new Date(today);
      tomorrow.setDate(today.getDate() + 1);
      nextWeek.setDate(today.getDate() + 15);
      const startDate = tomorrow.toISOString().split('T')[0];
      const minStartDate = today.toISOString().split('T')[0];
      const endDate = nextWeek.toISOString().split('T')[0];
      return { startDate, minStartDate, endDate };
    },
    initSettings() {
      if (!this.editMode) {
        this.resetSettings();
        return;
      }
      this.schedule = this.savedScheduleSettings.scheduled;
      const startDate = this.savedScheduleSettings?.postDate && new Date(this.savedScheduleSettings?.postDate) || null;
      const endDate = this.savedScheduleSettings?.unpublishDate && new Date(this.savedScheduleSettings?.unpublishDate) || null;
      this.selectedScheduleType = this.getUsedScheduleType(startDate, endDate);
      this.editScheduleOption = this.getUsedScheduleOption();
      this.startDate = startDate?.toISOString()?.split('T')[0];
      this.endDate = endDate?.toISOString()?.split('T')[0];
      this.startTime = startDate && `${startDate.getHours().toString().padStart(2, '0')}:${startDate.getMinutes().toString().padStart(2, '0')}` || null;
      this.endTime = endDate && `${endDate.getHours().toString().padStart(2, '0')}:${endDate.getMinutes().toString().padStart(2, '0')}` || null;
    },
    getUsedScheduleOption() {
      if (this.schedule && this.isUntilScheduleType && !this.hasSavedUnpublishSchedule) {
        return [SCHEDULE_OPTION, PUBLISH_NOW_OPTION];
      } else if (this.schedule) {
        return SCHEDULE_OPTION;
      }
      return null;
    },
    getUsedScheduleType(postDate, unpublishDate) {
      if (postDate && unpublishDate) {
        return this.betweenScheduleType;
      } else if (postDate) {
        return this.fromScheduleType;
      }
      return this.untilScheduleType;
    },
    getEditScheduleAction() {
      return this.isMultipleSelectionOption && SCHEDULE_OPTION || this.editScheduleOption;
    },
    emitUpdatedSettings() {
      this.$emit('updated', {
        schedule: this.schedule,
        editScheduleAction: this.getEditScheduleAction(),
        postDate: this.computePostDate(),
        unpublishDate: this.computeUnpublishDate()
      });
    },
    computePostDate() {
      return this.showStartDate && this.computeDateTime(this.startDate, this.startTime) || null;
    },
    computeUnpublishDate() {
      return this.showEndDate && this.computeDateTime(this.endDate, this.endTime) || null;
    },
    computeDateTime(date, time) {
      if (!date || !time || !this.schedule) {
        return null;
      }
      const computedDate = new Date(date);
      let hours;
      let minutes;
      if (time instanceof Date) {
        const dateTime = new Date(time);
        hours = dateTime.getHours();
        minutes = dateTime.getMinutes();
      } else {
        [hours, minutes] = time.split(':').map(Number);
      }
      computedDate.setHours(hours);
      computedDate.setMinutes(minutes);
      computedDate.setSeconds(0);
      return computedDate.toISOString();
    },
    formatDate(date) {
      const options = {year: 'numeric', month: 'long', day: 'numeric'};
      return date && new Intl.DateTimeFormat(this.locale, options).format(new Date(date));
    },
    checkEndDateComparedToStartDate() {
      const startDate = new Date(this.startDate);
      let endEndDate = new Date(this.endDate);
      if (startDate > endEndDate) {
        endEndDate = startDate;
        endEndDate.setDate(startDate.getDate() + 14);
        this.endDate = endEndDate.toISOString().split('T')[0];
        this.endTime = '18:00';
      }
    },
    updateMinStartTime() {
      const selectedDate = new Date(this.startDate);
      const today = new Date();
      if (selectedDate.toDateString() === today.toDateString()) {
        const roundedMinutes = Math.ceil(today.getMinutes() / 15) * 15;
        const hours = today.getHours() + Math.floor(roundedMinutes / 60);
        const adjustedMinutes = roundedMinutes % 60;
        this.minStartTime = `${hours.toString().padStart(2, '0')}:${adjustedMinutes.toString().padStart(2, '0')}`;
      }
      this.startDateMenu = false;
      this.updateEndMinTime();
      this.checkEndDateComparedToStartDate();
      this.minEndDate = this.startDate;
    },
    updateEndMinTime() {
      const selectedDate = new Date(this.endDate);
      const startDate = new Date(this.startDate);
      if (!this.isUntilScheduleType && selectedDate.toDateString() === startDate.toDateString()) {
        const [hours, minutes] = this.startTime.split(':').map(Number);
        this.minEndTime = `${hours.toString().padStart(2, '0')}:${(minutes + 15).toString().padStart(2, '0')}`;
      } else if (this.isUntilScheduleType) {
        const today = new Date();
        if (selectedDate.toDateString() === today.toDateString()) {
          const roundedMinutes = Math.ceil(today.getMinutes() / 15) * 15;
          const hours = today.getHours() + Math.floor(roundedMinutes / 60);
          const adjustedMinutes = roundedMinutes % 60;
          this.minEndTime = `${hours.toString().padStart(2, '0')}:${adjustedMinutes.toString().padStart(2, '0')}`;
        }
      }
      this.endDateMenu = false;
    },
    cancelChanges() {
      this.initSettings();
    },
    resetSettings() {
      this.schedule = false;
      this.savedScheduleSettings = {};
      const {startDate, minStartDate, endDate} = this.initDateValues();
      this.startDate = startDate;
      this.endDate = endDate;
      this.minStartDate = minStartDate;
      this.minEndDate = minStartDate;
      this.endTime = '18:00';
      this.startTime= '08:00';
    }
  }
};
</script>
