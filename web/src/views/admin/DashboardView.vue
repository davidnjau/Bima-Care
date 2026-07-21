<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { listPatients } from '../../api/patients'
import { listOrganizations } from '../../api/organizations'
import { listCoverages } from '../../api/coverages'

const loading = ref(true)
const error = ref('')
const memberCount = ref(0)
const providerCount = ref(0)
const activeCoverageCount = ref(0)

onMounted(async () => {
  try {
    const [patients, organizations, coverages] = await Promise.all([
      listPatients(),
      listOrganizations(),
      listCoverages(),
    ])
    memberCount.value = patients.length
    providerCount.value = organizations.length
    activeCoverageCount.value = coverages.filter((c) => c.status === 'ACTIVE').length
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load dashboard data.'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Dashboard</h2>
      <p class="text-muted text-sm mt-1">Network overview &middot; live counts from the API</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-4.5">
      <div class="bg-white border border-line rounded-[10px] p-5">
        <span class="text-[0.72rem] uppercase tracking-wider text-muted font-semibold">
          Total members
        </span>
        <div class="font-display text-3xl mt-1.5 tnum">
          {{ loading ? '…' : memberCount }}
        </div>
      </div>
      <div class="bg-white border border-line rounded-[10px] p-5">
        <span class="text-[0.72rem] uppercase tracking-wider text-muted font-semibold">
          Network providers
        </span>
        <div class="font-display text-3xl mt-1.5 tnum">
          {{ loading ? '…' : providerCount }}
        </div>
      </div>
      <div class="bg-white border border-line rounded-[10px] p-5">
        <span class="text-[0.72rem] uppercase tracking-wider text-muted font-semibold">
          Active coverage
        </span>
        <div class="font-display text-3xl mt-1.5 tnum">
          {{ loading ? '…' : activeCoverageCount }}
        </div>
      </div>
    </div>

    <p class="text-muted text-sm mt-6">
      Policies, Claims, and Reports will appear here once their backend services (Phase 2) are
      built.
    </p>
  </div>
</template>
