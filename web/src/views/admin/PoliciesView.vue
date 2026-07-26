<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPolicyFhir, listPolicies, updatePolicyStatus, type Policy } from '../../api/policies'
import { listOrganizations, type Organization } from '../../api/organizations'
import { formatDate } from '../../lib/formatDate'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const loading = ref(true)
const error = ref('')
const policies = ref<Policy[]>([])
const organizations = ref<Organization[]>([])
const acting = ref<string | null>(null)

const viewingPolicy = ref<Policy | null>(null)

function insurerName(insurerId: string): string {
  return organizations.value.find((o) => o.id === insurerId)?.name ?? 'Unknown insurer'
}

function policyFields(policy: Policy) {
  return [
    { label: 'Name', value: policy.name },
    { label: 'Policy number', value: policy.policyNumber },
    { label: 'Type', value: policy.type },
    { label: 'Premium', value: `Ksh ${Number(policy.premium).toLocaleString()}` },
    { label: 'Start date', value: formatDate(policy.startDate) ?? policy.startDate },
    { label: 'End date', value: policy.endDate ? (formatDate(policy.endDate) ?? policy.endDate) : null },
    { label: 'Status', value: policy.status },
    { label: 'Insurer', value: insurerName(policy.insurerId) },
    { label: 'Policy ID', value: policy.id },
  ]
}

function resolvePolicyReference(reference: string): string | null {
  const [resourceType, id] = reference.split('/')
  if (resourceType === 'Organization') return insurerName(id)
  return null
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [policyList, organizationList] = await Promise.all([listPolicies(), listOrganizations()])
    policies.value = policyList
    organizations.value = organizationList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load policies.'
  } finally {
    loading.value = false
  }
}

async function toggleStatus(policy: Policy) {
  acting.value = policy.id
  error.value = ''
  try {
    const nextStatus = policy.status === 'SUSPENDED' ? 'ACTIVE' : 'SUSPENDED'
    const updated = await updatePolicyStatus(policy.id, nextStatus)
    policies.value = policies.value.map((p) => (p.id === updated.id ? updated : p))
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to update policy status.'
  } finally {
    acting.value = null
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Policies</h2>
      <p class="text-muted text-sm mt-1">
        Oversight across all insurers — policies are created and managed by insurers via the
        Insurer portal.
      </p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Policy</th>
            <th class="px-4 py-3 font-bold">Number</th>
            <th class="px-4 py-3 font-bold">Insurer</th>
            <th class="px-4 py-3 font-bold">Type</th>
            <th class="px-4 py-3 font-bold">Premium</th>
            <th class="px-4 py-3 font-bold">Period</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="policy in policies" :key="policy.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ policy.name }}</td>
            <td class="px-4 py-3 font-mono">{{ policy.policyNumber }}</td>
            <td class="px-4 py-3">{{ insurerName(policy.insurerId) }}</td>
            <td class="px-4 py-3">{{ policy.type }}</td>
            <td class="px-4 py-3 font-mono">Ksh {{ Number(policy.premium).toLocaleString() }}</td>
            <td class="px-4 py-3 font-mono">
              {{ formatDate(policy.startDate) }} &ndash; {{ policy.endDate ? formatDate(policy.endDate) : 'ongoing' }}
            </td>
            <td class="px-4 py-3"><StatusBadge :status="policy.status" /></td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button
                  v-if="policy.status !== 'EXPIRED'"
                  :disabled="acting === policy.id"
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold disabled:opacity-60"
                  @click="toggleStatus(policy)"
                >
                  {{ policy.status === 'SUSPENDED' ? 'Reactivate' : 'Suspend' }}
                </button>
                <button
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                  @click="viewingPolicy = policy"
                >
                  View
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="policies.length === 0">
            <td colspan="8" class="px-4 py-6 text-center text-muted">No policies yet.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <RecordDetailModal
      v-if="viewingPolicy"
      :title="viewingPolicy.name"
      :fields="policyFields(viewingPolicy)"
      :load-fhir="() => getPolicyFhir(viewingPolicy!.id)"
      :resolve-reference="resolvePolicyReference"
      @close="viewingPolicy = null"
    />
  </div>
</template>
