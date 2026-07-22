<script setup lang="ts">
const props = defineProps<{ status: string }>()

const tone: Record<string, string> = {
  approved: 'bg-success-soft text-success',
  active: 'bg-success-soft text-success',
  pending: 'bg-warning-soft text-warning',
  submitted: 'bg-warning-soft text-warning',
  partial: 'bg-warning-soft text-warning',
  partially_approved: 'bg-warning-soft text-warning',
  suspended: 'bg-critical-soft text-critical',
  rejected: 'bg-critical-soft text-critical',
  expired: 'bg-critical-soft text-critical',
}

const classes = tone[props.status.toLowerCase()] ?? 'bg-line text-muted'
const label = props.status
  .toLowerCase()
  .split('_')
  .map((word) => word.charAt(0).toUpperCase() + word.slice(1))
  .join(' ')
</script>

<template>
  <span
    :class="classes"
    class="inline-flex items-center gap-1.5 text-[0.7rem] font-bold uppercase tracking-wide px-2.5 py-1 rounded-full before:content-[''] before:w-1.5 before:h-1.5 before:rounded-full before:bg-current"
  >
    {{ label }}
  </span>
</template>
