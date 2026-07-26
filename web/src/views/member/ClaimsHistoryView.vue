<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useMemberStore } from '../../stores/member'
import { getClaimFhir, listClaims, type Claim } from '../../api/claims'
import { getPatient } from '../../api/patients'
import { listOrganizations, type Organization } from '../../api/organizations'
import { claimFields } from '../../lib/claimFields'
import { formatDate } from '../../lib/formatDate'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const member = useMemberStore()
const claims = ref<Claim[]>([])
const organizations = ref<Organization[]>([])
const ownName = ref('')
const loading = ref(true)
const error = ref('')
const viewingClaim = ref<Claim | null>(null)

function organizationName(id: string): string {
  return organizations.value.find((o) => o.id === id)?.name ?? 'Unknown provider'
}

function viewFields(claim: Claim) {
  return claimFields(claim, ownName.value || 'You', organizationName(claim.organizationId))
}

function resolveClaimReference(reference: string): string | null {
  const [resourceType, id] = reference.split('/')
  if (resourceType === 'Patient') return ownName.value || null
  if (resourceType === 'Organization') return organizationName(id)
  return null
}

async function load() {
  if (!member.selectedPatientId) return
  loading.value = true
  error.value = ''
  try {
    const [claimList, organizationList, patient] = await Promise.all([
      listClaims({ patientId: member.selectedPatientId }),
      listOrganizations(),
      getPatient(member.selectedPatientId),
    ])
    claims.value = claimList
    organizations.value = organizationList
    ownName.value = `${patient.firstName} ${patient.lastName}`
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
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="claim in claims" :key="claim.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ claim.serviceType }}</td>
            <td class="px-4 py-3 font-mono">{{ formatDate(claim.submittedAt) }}</td>
            <td class="px-4 py-3 text-muted">{{ claim.diagnosisCode }}</td>
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
