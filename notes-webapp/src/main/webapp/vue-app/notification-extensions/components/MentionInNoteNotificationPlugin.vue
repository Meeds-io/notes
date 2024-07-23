<template>
  <user-notification-template
    :notification="notification"
    :avatar-url="avatarUrl"
    :message="message"
    :url="url"
    user-avatar>
    <template #actions>
      <div class="text-truncate">
        <v-icon size="14" class="me-1 mb-1">fas fa-clipboard</v-icon>
        {{ noteTitle }}
      </div>
    </template>
  </user-notification-template>
</template>
<script>
export default {
  props: {
    notification: {
      type: Object,
      default: null,
    },
  },
  computed: {
    url() {
      return this.notification?.parameters?.ACTIVITY_LINK;
    },
    noteTitle() {
      return this.notification?.parameters?.NOTE_TITLE;
    },
    avatarUrl() {
      return this.notification?.parameters?.AUTHOR_AVATAR_URL;
    },
    message() {
      return this.$t('note.notification.mention.in.note.description');
    }
  }
};
</script>