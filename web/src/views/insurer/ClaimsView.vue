<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getClaimFhir, listClaims, type Claim } from '../../api/claims'
import { listPatients, type Patient } from '../../api/patients'
import { listOrganizations, type Organization } from '../../api/organizations'
import { claimFields } from '../../lib/claimFields'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

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

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [claimList, patientList, organizationList] = await Promise.all([
      listClaims(),
      listPatients(),
      listOrganizations(),
    ])
    claims.value = claimList
    patients.value = patientList
    organizations.value = organizationList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load claims.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Claims</h2>
      <p class="text-muted text-sm mt-1">Claims activity across the network</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Claim ID</th>
            <th class="px-4 py-3 font-bold">Member</th>
            <th class="px-4 py-3 font-bold">Service</th>
            <th class="px-4 py-3 font-bold">Requested</th>
            <th class="px-4 py-3 font-bold">Approved</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="claim in claims" :key="claim.id" class="border-t border-line">
            <td class="px-4 py-3 font-mono">{{ claim.id.slice(0, 8) }}</td>
            <td class="px-4 py-3 font-semibold">{{ patientName(claim.patientId) }}</td>
            <td class="px-4 py-3">{{ claim.serviceType }}</td>
            <td class="px-4 py-3 font-mono">Ksh {{ Number(claim.requestedAmount).toLocaleString() }}</td>
            <td class="px-4 py-3 font-mono">
              {{ claim.approvedAmount ? `Ksh ${Number(claim.approvedAmount).toLocaleString()}` : '—' }}
            </td>
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
            <td colspan="7" class="px-4 py-6 text-center text-muted">No claims yet.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="text-muted text-sm mt-5">
      Showing all network claims — scoping this to only claims covered under your policies is a
      known follow-up (needs a patient &rarr; coverage &rarr; policy join that no endpoint
      supports yet).
    </p>

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
