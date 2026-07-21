<script setup lang="ts">
defineProps<{
  title: string
  message: string
}>()
</script>

<template>
  <div class="bg-white border border-line rounded-[10px] p-8 text-center max-w-lg mx-auto mt-6">
    <h3 class="font-display text-lg font-semibold mb-2">{{ title }}</h3>
    <p class="text-muted text-sm">{{ message }}</p>
  </div>
</template>
