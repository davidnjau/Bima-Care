<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ active: boolean }>()

const classes = computed(() =>
  props.active ? 'bg-success-soft text-success' : 'bg-critical-soft text-critical',
)
</script>

<template>
  <span
    :class="classes"
    class="inline-flex items-center gap-1.5 text-[0.7rem] font-bold uppercase tracking-wide px-2.5 py-1 rounded-full before:content-[''] before:w-1.5 before:h-1.5 before:rounded-full before:bg-current"
  >
    {{ active ? 'Active' : 'Suspended' }}
  </span>
</template>
