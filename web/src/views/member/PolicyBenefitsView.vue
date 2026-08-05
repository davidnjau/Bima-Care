<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useMemberStore } from '../../stores/member'
import { listCoverages, type Coverage } from '../../api/coverages'
import { listPolicies, type Policy } from '../../api/policies'
import StatusChip from '../../components/StatusChip.vue'

const member = useMemberStore()
const coverages = ref<Coverage[]>([])
const policies = ref<Policy[]>([])
const loading = ref(true)
const error = ref('')

// A member can hold more than one policy (e.g. one from their employer, one they bought
// individually) - show all of them, not just the first match.
const memberCoverages = computed(() =>
  coverages.value.filter((c) => c.patientId === member.selectedPatientId),
)

function policyFor(coverage: Coverage): Policy | undefined {
  return coverage.policyId ? policies.value.find((p) => p.id === coverage.policyId) : undefined
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [coverageList, policyList] = await Promise.all([listCoverages(), listPolicies()])
    coverages.value = coverageList
    policies.value = policyList
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

    <div v-else-if="memberCoverages.length > 0" class="grid gap-4 sm:grid-cols-2">
      <div
        v-for="coverage in memberCoverages"
        :key="coverage.id"
        class="bg-white border border-line rounded-[10px] p-6"
      >
        <div class="flex items-center justify-between mb-4">
          <div>
            <h3 class="font-display text-lg font-semibold">{{ policyFor(coverage)?.name ?? coverage.planTier }}</h3>
            <p v-if="policyFor(coverage)" class="text-xs text-muted font-mono mt-0.5">
              {{ policyFor(coverage)!.policyNumber }} &middot; {{ coverage.planTier }}
            </p>
          </div>
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
    </div>
    <p v-else class="text-muted text-sm">No active coverage on file for this member.</p>
  </div>
</template>
