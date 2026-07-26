<script setup lang="ts">
import { formatDate } from '../lib/formatDate'

const props = defineProps<{
  resource: Record<string, unknown>
  // Given a FHIR reference string like "Patient/072efe26-...", returns a human name
  // if resolvable. When omitted or returning nothing, only the resource type is
  // shown - the id itself is never displayed.
  resolveReference?: (reference: string) => string | null | undefined
}>()

function humanize(key: string | number): string {
  return String(key)
    .replace(/([a-z])([A-Z])/g, '$1 $2')
    .replace(/^./, (c) => c.toUpperCase())
    .trim()
}

function isPlainObject(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value)
}

function isEmpty(value: unknown): boolean {
  return value === null || value === undefined || value === ''
}

function isReference(value: unknown): value is { reference: string } {
  return isPlainObject(value) && typeof value.reference === 'string' && /^[A-Za-z]+\/.+/.test(value.reference)
}

function referenceLabel(value: { reference: string }): string {
  const [resourceType] = value.reference.split('/')
  return props.resolveReference?.(value.reference) || resourceType
}

function displayValue(value: unknown): string {
  if (typeof value === 'string') return formatDate(value) ?? value
  return String(value)
}
</script>

<template>
  <div>
    <template v-for="(value, key) in resource" :key="key">
      <template v-if="key !== 'resourceType' && !isEmpty(value)">
        <div v-if="isReference(value)" class="flex justify-between gap-4 py-1.5 border-b border-line/60 text-sm">
          <span class="text-muted">{{ humanize(key) }}</span>
          <span class="font-medium text-right">{{ referenceLabel(value) }}</span>
        </div>

        <div v-else-if="Array.isArray(value)" class="py-2">
          <div class="text-[0.68rem] font-bold uppercase tracking-wide text-muted mb-1.5">
            {{ humanize(key) }}
          </div>
          <div class="bg-paper rounded-[7px] p-3 space-y-2">
            <div v-for="(item, i) in value" :key="i">
              <FhirResourceView v-if="isPlainObject(item)" :resource="item" :resolve-reference="resolveReference" />
              <span v-else class="text-sm">{{ displayValue(item) }}</span>
            </div>
          </div>
        </div>

        <div v-else-if="isPlainObject(value)" class="py-2">
          <div class="text-[0.68rem] font-bold uppercase tracking-wide text-muted mb-1.5">
            {{ humanize(key) }}
          </div>
          <div class="bg-paper rounded-[7px] p-3">
            <FhirResourceView :resource="value" :resolve-reference="resolveReference" />
          </div>
        </div>

        <div v-else class="flex justify-between gap-4 py-1.5 border-b border-line/60 text-sm">
          <span class="text-muted">{{ humanize(key) }}</span>
          <span class="font-medium text-right">{{ displayValue(value) }}</span>
        </div>
      </template>
    </template>
  </div>
</template>
