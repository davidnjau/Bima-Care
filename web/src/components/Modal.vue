<script setup lang="ts">
function onBackdropClick(event: MouseEvent) {
  if (event.target === event.currentTarget) emit('close')
}

const emit = defineEmits<{ close: [] }>()
</script>

<template>
  <div
    class="fixed inset-0 z-50 bg-black/40 flex items-center justify-center p-4"
    @click="onBackdropClick"
    @keydown.esc="emit('close')"
  >
    <div class="bg-white rounded-[10px] max-w-2xl w-full max-h-[85vh] overflow-y-auto p-6 relative">
      <button
        class="absolute top-4 right-4 text-muted hover:text-critical text-xl leading-none"
        aria-label="Close"
        @click="emit('close')"
      >
        &times;
      </button>
      <slot />
    </div>
  </div>
</template>
