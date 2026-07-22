<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useMemberStore } from '../../stores/member'
import { listClaims, type Claim } from '../../api/claims'
import StatusBadge from '../../components/StatusBadge.vue'

const member = useMemberStore()
const claims = ref<Claim[]>([])
const loading = ref(true)
const error = ref('')

async function load() {
  if (!member.selectedPatientId) return
  loading.value = true
  error.value = ''
  try {
    claims.value = await listClaims({ patientId: member.selectedPatientId })
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load claims history.'
  } finally {
    loading.value = false
  }
}

watch(() => member.selectedPatientId, load)
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Claims History</h2>
      <p class="text-muted text-sm mt-1">Your past claims.</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Service</th>
            <th class="px-4 py-3 font-bold">Submitted</th>
            <th class="px-4 py-3 font-bold">Diagnosis</th>
            <th class="px-4 py-3 font-bold">Requested</th>
            <th class="px-4 py-3 font-bold">Approved</th>
            <th class="px-4 py-3 font-bold">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="claim in claims" :key="claim.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ claim.serviceType }}</td>
            <td class="px-4 py-3 font-mono">{{ claim.submittedAt.slice(0, 10) }}</td>
            <td class="px-4 py-3 text-muted">{{ claim.diagnosisCode }}</td>
            <td class="px-4 py-3 font-mono">Ksh {{ Number(claim.requestedAmount).toLocaleString() }}</td>
            <td class="px-4 py-3 font-mono">
              {{ claim.approvedAmount ? `Ksh ${Number(claim.approvedAmount).toLocaleString()}` : '—' }}
            </td>
            <td class="px-4 py-3"><StatusBadge :status="claim.status" /></td>
          </tr>
          <tr v-if="claims.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-muted">No claims yet.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
