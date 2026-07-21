<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useMemberStore } from '../../stores/member'
import { listCoverages, type Coverage } from '../../api/coverages'
import StatusChip from '../../components/StatusChip.vue'

const member = useMemberStore()
const coverages = ref<Coverage[]>([])
const loading = ref(true)
const error = ref('')

const coverage = computed(() =>
  coverages.value.find((c) => c.patientId === member.selectedPatientId) ?? null,
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    coverages.value = await listCoverages()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load coverage.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Policy & Benefits</h2>
      <p class="text-muted text-sm mt-1">Your active cover.</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else-if="coverage" class="bg-white border border-line rounded-[10px] p-6 max-w-md">
      <div class="flex items-center justify-between mb-4">
        <h3 class="font-display text-lg font-semibold">{{ coverage.planTier }}</h3>
        <StatusChip :active="coverage.status === 'ACTIVE'" />
      </div>
      <dl class="grid grid-cols-2 gap-y-3 text-sm">
        <dt class="text-muted">Coverage period</dt>
        <dd class="font-mono text-right">
          {{ coverage.startDate }} &ndash; {{ coverage.endDate ?? 'ongoing' }}
        </dd>
        <dt class="text-muted">Status</dt>
        <dd class="text-right">{{ coverage.status }}</dd>
      </dl>
    </div>
    <p v-else class="text-muted text-sm">No active coverage on file for this member.</p>
  </div>
</template>
