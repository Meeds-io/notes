<!--

 This file is part of the Meeds project (https://meeds.io/).

 Copyright (C) 2020 - 2025 Meeds Association contact@meeds.io

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
  <changes-reminder
    :reminder="reminder">
    <template #default>
      <div>{{ $t('notes.terms.reminder.firstInstruction') }}:</div>
      <div>- {{ $t('notes.terms.reminder.secondInstruction') }}</div>
      <div>- {{ $t('notes.terms.reminder.thirdInstruction') }}</div>
      <div>- {{ $t('notes.terms.reminder.fourthInstruction') }}</div>
      <v-spacer class="py-2" />
      <div>{{ $t('notes.terms.reminder.letsGo') }}</div>
    </template>
  </changes-reminder>
</template>
<script>
export default {
  computed: {
    reminder() {
      return {
        name: 'termsAndConditions',
        type: 'guide',
        title: this.$t('notes.terms.reminder.title'),
        img: '/notes/images/terms.png'
      };
    }
  },
  created() {
    this.$nextTick().then(() => {
      document.dispatchEvent(new CustomEvent('changes-reminder-open'));
    });
  }
};
</script>