<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'

// The bullet in the "Bima•Care" wordmark, shown on every page - red while offline (the
// state that matters most, regardless of auth), green once actually logged in, otherwise
// the plain brand accent color (e.g. the login page, where no one is ever logged in yet).
const auth = useAuthStore()
const isOnline = ref(navigator.onLine)

function updateOnlineStatus() {
  isOnline.value = navigator.onLine
}

onMounted(() => {
  window.addEventListener('online', updateOnlineStatus)
  window.addEventListener('offline', updateOnlineStatus)
})
onUnmounted(() => {
  window.removeEventListener('online', updateOnlineStatus)
  window.removeEventListener('offline', updateOnlineStatus)
})
</script>

<template>
  <span
    :class="{
      'text-critical': !isOnline,
      'text-success': isOnline && auth.hasRealSession,
      'text-accent': isOnline && !auth.hasRealSession,
    }"
    >&bull;</span
  >
</template>
