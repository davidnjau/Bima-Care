<script setup lang="ts">
import { onMounted, ref } from 'vue'
import Modal from './Modal.vue'
import FhirResourceView from './FhirResourceView.vue'

const props = defineProps<{
  title: string
  fields: { label: string; value: string | null }[]
  // Omit entirely for records with no real backend (dummy-data views) - the FHIR
  // section only renders when a loader is supplied.
  loadFhir?: () => Promise<unknown>
  resolveReference?: (reference: string) => string | null | undefined
}>()
defineEmits<{ close: [] }>()

const fhirResource = ref<Record<string, unknown> | null>(null)
const fhirLoading = ref(false)
const fhirError = ref('')

onMounted(async () => {
  if (!props.loadFhir) return
  fhirLoading.value = true
  fhirError.value = ''
  try {
    fhirResource.value = (await props.loadFhir()) as Record<string, unknown>
  } catch (e) {
    fhirError.value = e instanceof Error ? e.message : 'Failed to load FHIR resource.'
  } finally {
    fhirLoading.value = false
  }
})
</script>

<template>
  <Modal @close="$emit('close')">
    <h3 class="font-display text-xl font-semibold mb-4">{{ title }}</h3>
    <dl class="grid grid-cols-2 gap-x-6 gap-y-3 text-sm mb-6">
      <template v-for="field in fields" :key="field.label">
        <dt class="text-muted font-semibold">{{ field.label }}</dt>
        <dd>{{ field.value || '—' }}</dd>
      </template>
    </dl>

    <template v-if="loadFhir">
      <h4 class="text-xs font-bold uppercase tracking-wide text-muted mb-2 flex items-center gap-2">
        FHIR resource
        <span v-if="fhirResource" class="normal-case font-mono text-muted/80">
          ({{ fhirResource.resourceType }})
        </span>
      </h4>
      <p v-if="fhirLoading" class="text-muted text-sm">Loading&hellip;</p>
      <p v-if="fhirError" class="text-critical text-sm">{{ fhirError }}</p>
      <div v-if="fhirResource" class="border border-line rounded-[7px] p-4">
        <FhirResourceView :resource="fhirResource" :resolve-reference="resolveReference" />
      </div>
    </template>
  </Modal>
</template>
