<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { getClaimFhir, listClaims, type Claim } from '../../api/claims'
import { listPatients, type Patient } from '../../api/patients'
import { listOrganizations, type Organization } from '../../api/organizations'
import { useAuthStore } from '../../stores/auth'
import { DEMO_PROVIDER_ORGANIZATION_ID } from '../../config'
import { claimFields } from '../../lib/claimFields'
import { formatDate } from '../../lib/formatDate'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const auth = useAuthStore()
const claims = ref<Claim[]>([])
const patients = ref<Patient[]>([])
const organizations = ref<Organization[]>([])
const loading = ref(true)
const error = ref('')
const viewingClaim = ref<Claim | null>(null)

function patientName(id: string): string {
  const patient = patients.value.find((p) => p.id === id)
  return patient ? `${patient.firstName} ${patient.lastName}` : 'Unknown member'
}

function organizationName(id: string): string {
  return organizations.value.find((o) => o.id === id)?.name ?? 'Unknown provider'
}

function viewFields(claim: Claim) {
  return claimFields(claim, patientName(claim.patientId), organizationName(claim.organizationId))
}

function resolveClaimReference(reference: string): string | null {
  const [resourceType, id] = reference.split('/')
  if (resourceType === 'Patient') return patientName(id)
  if (resourceType === 'Organization') return organizationName(id)
  return null
}

const summary = computed(() => {
  const total = claims.value.length
  const approved = claims.value.filter((c) => c.status === 'APPROVED' || c.status === 'PARTIALLY_APPROVED').length
  const totalValue = claims.value.reduce((sum, c) => sum + Number(c.approvedAmount ?? c.requestedAmount), 0)
  return { total, approved, totalValue }
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    const organizationId = auth.username ? DEMO_PROVIDER_ORGANIZATION_ID[auth.username] : undefined
    const [claimList, patientList, organizationList] = await Promise.all([
      listClaims(organizationId ? { organizationId } : {}),
      listPatients(),
      listOrganizations(),
    ])
    claims.value = claimList
    patients.value = patientList
    organizations.value = organizationList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load transaction history.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Transaction History</h2>
      <p class="text-muted text-sm mt-1">Claims submitted by this facility.</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <template v-else>
      <div class="grid grid-cols-3 gap-4 mb-6 max-w-xl">
        <div class="bg-white border border-line rounded-[10px] p-4">
          <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Total claims</span>
          <div class="font-display text-2xl mt-1 tnum">{{ summary.total }}</div>
        </div>
        <div class="bg-white border border-line rounded-[10px] p-4">
          <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Approved</span>
          <div class="font-display text-2xl mt-1 tnum">{{ summary.approved }}</div>
        </div>
        <div class="bg-white border border-line rounded-[10px] p-4">
          <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Total value</span>
          <div class="font-display text-2xl mt-1 tnum">Ksh {{ summary.totalValue.toLocaleString() }}</div>
        </div>
      </div>

      <div class="border border-line rounded-[10px] overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
              <th class="px-4 py-3 font-bold">Claim ID</th>
              <th class="px-4 py-3 font-bold">Service</th>
              <th class="px-4 py-3 font-bold">Requested</th>
              <th class="px-4 py-3 font-bold">Approved</th>
              <th class="px-4 py-3 font-bold">Submitted</th>
              <th class="px-4 py-3 font-bold">Status</th>
              <th class="px-4 py-3 font-bold">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="claim in claims" :key="claim.id" class="border-t border-line">
              <td class="px-4 py-3 font-mono">{{ claim.id.slice(0, 8) }}</td>
              <td class="px-4 py-3">{{ claim.serviceType }}</td>
              <td class="px-4 py-3 font-mono">Ksh {{ Number(claim.requestedAmount).toLocaleString() }}</td>
              <td class="px-4 py-3 font-mono">
                {{ claim.approvedAmount ? `Ksh ${Number(claim.approvedAmount).toLocaleString()}` : '—' }}
              </td>
              <td class="px-4 py-3 font-mono">{{ formatDate(claim.submittedAt) }}</td>
              <td class="px-4 py-3"><StatusBadge :status="claim.status" /></td>
              <td class="px-4 py-3">
                <button
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                  @click="viewingClaim = claim"
                >
                  View
                </button>
              </td>
            </tr>
            <tr v-if="claims.length === 0">
              <td colspan="7" class="px-4 py-6 text-center text-muted">No claims submitted yet.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <RecordDetailModal
      v-if="viewingClaim"
      :title="`Claim ${viewingClaim.id.slice(0, 8)}`"
      :fields="viewFields(viewingClaim)"
      :load-fhir="() => getClaimFhir(viewingClaim!.id)"
      :resolve-reference="resolveClaimReference"
      @close="viewingClaim = null"
    />
  </div>
</template>
