<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { adjudicateClaim, getClaimFhir, listClaims, type Claim } from '../../api/claims'
import { listPatients, type Patient } from '../../api/patients'
import { listOrganizations, type Organization } from '../../api/organizations'
import { claimDocuments, claimFields } from '../../lib/claimFields'
import { formatDate } from '../../lib/formatDate'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const tab = ref<'pending' | 'processed'>('pending')
const claims = ref<Claim[]>([])
const patients = ref<Patient[]>([])
const organizations = ref<Organization[]>([])
const loading = ref(true)
const error = ref('')
const notice = ref('')
const acting = ref<string | null>(null)
const viewingClaim = ref<Claim | null>(null)

const pendingClaims = computed(() => claims.value.filter((c) => c.status === 'SUBMITTED'))
const processedClaims = computed(() => claims.value.filter((c) => c.status !== 'SUBMITTED'))

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

// Patient/Organization references resolve to real names; Practitioner and Coverage
// have no lookup available in this view, so the FHIR viewer falls back to showing
// just the resource type - never the raw id.
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

async function act(
  claim: Claim,
  decision: 'APPROVED' | 'PARTIALLY_APPROVED' | 'REJECTED',
) {
  notice.value = ''
  error.value = ''
  let approvedAmount: string | undefined
  if (decision === 'PARTIALLY_APPROVED') {
    const input = window.prompt(`Approved amount for claim ${claim.id.slice(0, 8)} (KES):`, claim.requestedAmount)
    if (!input) return
    approvedAmount = input
  }
  acting.value = claim.id
  try {
    const updated = await adjudicateClaim(claim.id, { decision, approvedAmount })
    claims.value = claims.value.map((c) => (c.id === updated.id ? updated : c))
    notice.value = `Claim ${updated.id.slice(0, 8)} marked ${updated.status}.`
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to adjudicate claim.'
  } finally {
    acting.value = null
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Claims</h2>
      <p class="text-muted text-sm mt-1">{{ pendingClaims.length }} pending</p>
    </div>

    <p v-if="notice" class="bg-success-soft text-success text-sm rounded-[7px] px-4 py-3 mb-5">
      {{ notice }}
    </p>
    <p v-if="error" class="bg-critical-soft text-critical text-sm rounded-[7px] px-4 py-3 mb-5">
      {{ error }}
    </p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <template v-else>
      <div class="flex gap-1 border-b border-line mb-5">
        <button
          class="px-4 py-2.5 text-sm border-b-2 -mb-px"
          :class="tab === 'pending' ? 'border-accent text-brand font-semibold' : 'border-transparent text-muted'"
          @click="tab = 'pending'"
        >
          Pending Review
        </button>
        <button
          class="px-4 py-2.5 text-sm border-b-2 -mb-px"
          :class="tab === 'processed' ? 'border-accent text-brand font-semibold' : 'border-transparent text-muted'"
          @click="tab = 'processed'"
        >
          Processed
        </button>
      </div>

      <div v-if="tab === 'pending'" class="grid gap-4">
        <div
          v-for="claim in pendingClaims"
          :key="claim.id"
          class="bg-white border border-line rounded-[10px] p-5"
        >
          <div class="flex items-start justify-between gap-4 flex-wrap">
            <div>
              <div class="font-mono text-sm text-muted">{{ claim.id.slice(0, 8) }}</div>
              <h3 class="font-display text-lg font-semibold mt-0.5">{{ patientName(claim.patientId) }}</h3>
              <p class="text-sm text-muted mt-1">
                {{ organizationName(claim.organizationId) }} &middot; {{ claim.serviceType }}
              </p>
            </div>
            <div class="text-right">
              <div class="font-mono font-semibold">Ksh {{ Number(claim.requestedAmount).toLocaleString() }}</div>
              <div class="text-xs text-muted mt-0.5">{{ formatDate(claim.submittedAt) }}</div>
            </div>
          </div>
          <p class="text-sm mt-3">{{ claim.diagnosisCode }} &mdash; {{ claim.treatmentDetails }}</p>
          <div class="flex gap-2 mt-4">
            <button
              :disabled="acting === claim.id"
              class="bg-success text-white font-semibold text-xs rounded-[7px] px-3.5 py-2 disabled:opacity-60"
              @click="act(claim, 'APPROVED')"
            >
              Approve
            </button>
            <button
              :disabled="acting === claim.id"
              class="bg-warning text-white font-semibold text-xs rounded-[7px] px-3.5 py-2 disabled:opacity-60"
              @click="act(claim, 'PARTIALLY_APPROVED')"
            >
              Partial
            </button>
            <button
              :disabled="acting === claim.id"
              class="bg-critical text-white font-semibold text-xs rounded-[7px] px-3.5 py-2 disabled:opacity-60"
              @click="act(claim, 'REJECTED')"
            >
              Reject
            </button>
            <button
              class="border border-line-strong rounded-[7px] px-3.5 py-2 text-xs font-semibold"
              @click="viewingClaim = claim"
            >
              View
            </button>
          </div>
        </div>
        <div v-if="pendingClaims.length === 0" class="border border-line rounded-[10px] p-6 text-center text-muted text-sm">
          No claims pending review.
        </div>
      </div>

      <div v-else class="border border-line rounded-[10px] overflow-x-auto">
        <table class="w-full text-sm">
          <thead>
            <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
              <th class="px-4 py-3 font-bold">Claim ID</th>
              <th class="px-4 py-3 font-bold">Member</th>
              <th class="px-4 py-3 font-bold">Provider</th>
              <th class="px-4 py-3 font-bold">Requested</th>
              <th class="px-4 py-3 font-bold">Approved</th>
              <th class="px-4 py-3 font-bold">Status</th>
              <th class="px-4 py-3 font-bold">Action</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="claim in processedClaims" :key="claim.id" class="border-t border-line">
              <td class="px-4 py-3 font-mono">{{ claim.id.slice(0, 8) }}</td>
              <td class="px-4 py-3 font-semibold">{{ patientName(claim.patientId) }}</td>
              <td class="px-4 py-3">{{ organizationName(claim.organizationId) }}</td>
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
            <tr v-if="processedClaims.length === 0">
              <td colspan="7" class="px-4 py-6 text-center text-muted">No processed claims yet.</td>
            </tr>
          </tbody>
        </table>
      </div>
    </template>

    <RecordDetailModal
      v-if="viewingClaim"
      :title="`Claim ${viewingClaim.id.slice(0, 8)}`"
      :fields="viewFields(viewingClaim)"
      :documents="claimDocuments(viewingClaim)"
      :load-fhir="() => getClaimFhir(viewingClaim!.id)"
      :resolve-reference="resolveClaimReference"
      @close="viewingClaim = null"
    />
  </div>
</template>
