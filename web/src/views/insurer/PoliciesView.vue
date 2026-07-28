<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPolicy,
  getPolicyFhir,
  listPolicies,
  updatePolicyStatus,
  type CreatePolicyRequest,
  type Policy,
} from '../../api/policies'
import { listOrganizations, type Organization } from '../../api/organizations'
import { formatDate } from '../../lib/formatDate'
import StatusBadge from '../../components/StatusBadge.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const policies = ref<Policy[]>([])
const organizations = ref<Organization[]>([])
const loading = ref(true)
const error = ref('')

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

const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive<CreatePolicyRequest>({
  name: '',
  type: 'FAMILY',
  premium: '',
  startDate: '',
  endDate: '',
})

const acting = ref<string | null>(null)

// End date must be strictly after start date - one day later is the earliest valid pick.
const minEndDate = computed(() => {
  if (!form.startDate) return undefined
  const d = new Date(form.startDate)
  d.setDate(d.getDate() + 1)
  return d.toISOString().slice(0, 10)
})

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

async function onSubmit() {
  formError.value = ''
  if (form.endDate && form.endDate <= form.startDate) {
    formError.value = 'End date must be after the start date.'
    return
  }
  saving.value = true
  try {
    await createPolicy({ ...form, endDate: form.endDate || undefined })
    showForm.value = false
    Object.assign(form, { name: '', type: 'FAMILY', premium: '', startDate: '', endDate: '' })
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to create policy.'
  } finally {
    saving.value = false
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
    <div class="flex items-end justify-between gap-5 flex-wrap mb-6">
      <div>
        <h2 class="font-display text-2xl font-semibold">Policies</h2>
        <p class="text-muted text-sm mt-1">Manage your insurance policies and benefit packages</p>
      </div>
      <button
        class="bg-accent hover:bg-accent-dark text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5"
        @click="showForm = !showForm"
      >
        {{ showForm ? 'Cancel' : 'Create Policy' }}
      </button>
    </div>

    <form
      v-if="showForm"
      class="bg-white border border-line rounded-[10px] p-5 mb-6 grid grid-cols-2 gap-4"
      @submit.prevent="onSubmit"
    >
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Name</label>
        <input v-model="form.name" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Type</label>
        <select v-model="form.type" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="FAMILY">Family</option>
          <option value="INDIVIDUAL">Individual</option>
          <option value="CORPORATE">Corporate</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Annual premium (KES)</label>
        <input v-model="form.premium" type="number" min="0" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Start date</label>
        <input v-model="form.startDate" type="date" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">End date (optional)</label>
        <input
          v-model="form.endDate"
          type="date"
          :min="minEndDate"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>

      <p v-if="formError" class="col-span-2 text-critical text-sm">{{ formError }}</p>

      <div class="col-span-2">
        <button
          type="submit"
          :disabled="saving"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ saving ? 'Saving…' : 'Save policy' }}
        </button>
      </div>
    </form>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Policy</th>
            <th class="px-4 py-3 font-bold">Number</th>
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
            <td colspan="7" class="px-4 py-6 text-center text-muted">No policies yet.</td>
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
