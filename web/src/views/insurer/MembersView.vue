<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import {
  createPatient,
  getPatientFhir,
  listPatients,
  type CreatePatientRequest,
  type Patient,
} from '../../api/patients'
import { createCoverage, listCoverages, type Coverage } from '../../api/coverages'
import { listPolicies, type Policy } from '../../api/policies'
import { createDependent } from '../../api/dependents'
import { formatDate } from '../../lib/formatDate'
import StatusChip from '../../components/StatusChip.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const loading = ref(true)
const error = ref('')
const patients = ref<Patient[]>([])
const coverages = ref<Coverage[]>([])
// listPolicies() already resolves to this insurer's own organizationId server-side, so
// everything built from this list (the picker, the "own coverage" check below) is
// automatically scoped to policies this insurer actually issued.
const policies = ref<Policy[]>([])

const viewingPatient = ref<Patient | null>(null)

function patientFields(patient: Patient) {
  return [
    { label: 'First name', value: patient.firstName },
    { label: 'Last name', value: patient.lastName },
    { label: 'National ID', value: patient.nationalId },
    { label: 'Phone', value: patient.phone },
    { label: 'Email', value: patient.email },
    { label: 'Gender', value: patient.gender },
    { label: 'Date of birth', value: formatDate(patient.dob) ?? patient.dob },
    { label: 'Status', value: patient.isActive ? 'Active' : 'Inactive' },
    { label: 'Patient ID', value: patient.id },
  ]
}

// A member is only eligible for a dependent under THIS insurer specifically if their active
// coverage references one of this insurer's own policies - a coverage from a different
// insurer doesn't give this insurer anything to attach a dependent's coverage to.
function ownActiveCoverage(patientId: string): Coverage | undefined {
  return coverages.value.find(
    (c) => c.patientId === patientId && c.status === 'ACTIVE' && policies.value.some((p) => p.id === c.policyId),
  )
}

const rows = computed(() =>
  patients.value.map((patient) => {
    const coverage = coverages.value.find((c) => c.patientId === patient.id)
    const policy = coverage?.policyId ? policies.value.find((p) => p.id === coverage.policyId) : null
    return {
      patient,
      policyLabel: policy ? policy.name : (coverage?.planTier ?? null),
      hasCoverage: !!coverage,
      ownCoverage: ownActiveCoverage(patient.id),
    }
  }),
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [patientList, coverageList, policyList] = await Promise.all([
      listPatients(),
      listCoverages(),
      listPolicies(),
    ])
    patients.value = patientList
    coverages.value = coverageList
    policies.value = policyList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load members.'
  } finally {
    loading.value = false
  }
}

// --- Add member ---

const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive<CreatePatientRequest>({
  nationalId: '',
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  gender: 'FEMALE',
  dob: '',
})

function resetForm() {
  Object.assign(form, {
    nationalId: '',
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    gender: 'FEMALE',
    dob: '',
  })
}

async function onSubmit() {
  formError.value = ''
  saving.value = true
  try {
    // Reuse the existing patient if this national ID is already registered (e.g. by another
    // insurer or Admin) - the server's own find-or-409 logic is still the real safety net for
    // races, this just avoids an unnecessary error in the common case.
    const existing = patients.value.find((p) => p.nationalId === form.nationalId)
    if (!existing) await createPatient({ ...form, email: form.email || undefined })
    showForm.value = false
    resetForm()
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to add member.'
  } finally {
    saving.value = false
  }
}

// --- Attach to policy ---

const attachingPatient = ref<Patient | null>(null)
const attaching = ref(false)
const attachError = ref('')
const attachForm = reactive({ policyId: '', planTier: '', startDate: '', endDate: '' })

function openAttachForm(patient: Patient) {
  attachingPatient.value = patient
  attachError.value = ''
  Object.assign(attachForm, { policyId: '', planTier: '', startDate: '', endDate: '' })
}

async function onAttachSubmit() {
  if (!attachingPatient.value) return
  const policy = policies.value.find((p) => p.id === attachForm.policyId)
  if (!policy) {
    attachError.value = 'Select a policy.'
    return
  }
  attachError.value = ''
  attaching.value = true
  try {
    await createCoverage({
      patientId: attachingPatient.value.id,
      insurerId: policy.insurerId,
      status: 'ACTIVE',
      startDate: attachForm.startDate,
      endDate: attachForm.endDate || undefined,
      planTier: attachForm.planTier,
      policyId: policy.id,
    })
    attachingPatient.value = null
    await load()
  } catch (e) {
    attachError.value = e instanceof Error ? e.message : 'Failed to attach member to policy.'
  } finally {
    attaching.value = false
  }
}

// --- Add dependent ---

const addingDependentFor = ref<Patient | null>(null)
const addingDependent = ref(false)
const dependentError = ref('')
const dependentForm = reactive({
  nationalId: '',
  firstName: '',
  lastName: '',
  phone: '',
  email: '',
  gender: 'FEMALE',
  dob: '',
  relationship: 'CHILD',
  planTier: '',
  startDate: '',
  endDate: '',
})

function openDependentForm(patient: Patient) {
  addingDependentFor.value = patient
  dependentError.value = ''
  const primaryCoverage = ownActiveCoverage(patient.id)
  Object.assign(dependentForm, {
    nationalId: '',
    firstName: '',
    lastName: '',
    phone: '',
    email: '',
    gender: 'FEMALE',
    dob: '',
    relationship: 'CHILD',
    planTier: primaryCoverage?.planTier ?? '',
    startDate: primaryCoverage?.startDate ?? '',
    endDate: primaryCoverage?.endDate ?? '',
  })
}

async function onDependentSubmit() {
  const primary = addingDependentFor.value
  if (!primary) return
  const primaryCoverage = ownActiveCoverage(primary.id)
  if (!primaryCoverage) {
    dependentError.value = 'This member has no active coverage under one of your policies.'
    return
  }
  dependentError.value = ''
  addingDependent.value = true
  try {
    const existing = patients.value.find((p) => p.nationalId === dependentForm.nationalId)
    const dependentPatient =
      existing ??
      (await createPatient({
        nationalId: dependentForm.nationalId,
        firstName: dependentForm.firstName,
        lastName: dependentForm.lastName,
        phone: dependentForm.phone,
        email: dependentForm.email || undefined,
        gender: dependentForm.gender,
        dob: dependentForm.dob,
      }))
    await createDependent(primary.id, {
      dependentPatientId: dependentPatient.id,
      relationship: dependentForm.relationship,
    })
    await createCoverage({
      patientId: dependentPatient.id,
      insurerId: primaryCoverage.insurerId,
      status: 'ACTIVE',
      startDate: dependentForm.startDate,
      endDate: dependentForm.endDate || undefined,
      planTier: dependentForm.planTier,
      policyId: primaryCoverage.policyId ?? undefined,
    })
    addingDependentFor.value = null
    await load()
  } catch (e) {
    dependentError.value = e instanceof Error ? e.message : 'Failed to add dependent.'
  } finally {
    addingDependent.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-end justify-between gap-5 flex-wrap mb-6">
      <div>
        <h2 class="font-display text-2xl font-semibold">Members</h2>
        <p class="text-muted text-sm mt-1">Enroll members and dependents under your policies</p>
      </div>
      <button
        class="bg-accent hover:bg-accent-dark text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5"
        @click="showForm = !showForm"
      >
        {{ showForm ? 'Cancel' : 'Add member' }}
      </button>
    </div>

    <form
      v-if="showForm"
      class="bg-white border border-line rounded-[10px] p-5 mb-6 grid grid-cols-2 gap-4"
      @submit.prevent="onSubmit"
    >
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">National ID</label>
        <input v-model="form.nationalId" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Phone</label>
        <input v-model="form.phone" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Email (optional)</label>
        <input
          v-model="form.email"
          type="email"
          placeholder="For account-credential delivery"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">First name</label>
        <input v-model="form.firstName" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Last name</label>
        <input v-model="form.lastName" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Gender</label>
        <select v-model="form.gender" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="FEMALE">Female</option>
          <option value="MALE">Male</option>
          <option value="OTHER">Other</option>
          <option value="UNKNOWN">Unknown</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Date of birth</label>
        <input v-model="form.dob" type="date" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>

      <p v-if="formError" class="col-span-2 text-critical text-sm">{{ formError }}</p>

      <div class="col-span-2">
        <button
          type="submit"
          :disabled="saving"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ saving ? 'Saving…' : 'Save member' }}
        </button>
      </div>
    </form>

    <form
      v-if="attachingPatient"
      class="bg-white border border-line rounded-[10px] p-5 mb-6 grid grid-cols-2 gap-4"
      @submit.prevent="onAttachSubmit"
    >
      <p class="col-span-2 text-sm">
        Attach <b>{{ attachingPatient.firstName }} {{ attachingPatient.lastName }}</b> to a policy
      </p>
      <div class="flex flex-col gap-1.5 col-span-2">
        <label class="text-xs font-semibold text-muted">Policy</label>
        <select v-model="attachForm.policyId" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="" disabled>Select a policy&hellip;</option>
          <option v-for="policy in policies" :key="policy.id" :value="policy.id">
            {{ policy.name }} ({{ policy.policyNumber }})
          </option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Plan tier</label>
        <input v-model="attachForm.planTier" required placeholder="e.g. Gold" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Start date</label>
        <input v-model="attachForm.startDate" type="date" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">End date (optional)</label>
        <input v-model="attachForm.endDate" type="date" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>

      <p v-if="attachError" class="col-span-2 text-critical text-sm">{{ attachError }}</p>

      <div class="col-span-2 flex gap-2">
        <button
          type="submit"
          :disabled="attaching"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ attaching ? 'Saving…' : 'Attach to policy' }}
        </button>
        <button
          type="button"
          class="border border-line-strong rounded-[7px] px-4.5 py-2.5 text-sm font-semibold"
          @click="attachingPatient = null"
        >
          Cancel
        </button>
      </div>
    </form>

    <form
      v-if="addingDependentFor"
      class="bg-white border border-line rounded-[10px] p-5 mb-6 grid grid-cols-2 gap-4"
      @submit.prevent="onDependentSubmit"
    >
      <p class="col-span-2 text-sm">
        Add a dependent for <b>{{ addingDependentFor.firstName }} {{ addingDependentFor.lastName }}</b>
      </p>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">National ID</label>
        <input v-model="dependentForm.nationalId" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Relationship</label>
        <select v-model="dependentForm.relationship" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="SPOUSE">Spouse</option>
          <option value="CHILD">Child</option>
          <option value="OTHER">Other</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">First name</label>
        <input v-model="dependentForm.firstName" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Last name</label>
        <input v-model="dependentForm.lastName" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Phone</label>
        <input
          v-model="dependentForm.phone"
          required
          placeholder="Guardian's phone if the dependent has none"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Email (optional)</label>
        <input
          v-model="dependentForm.email"
          type="email"
          placeholder="For account-credential delivery"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Gender</label>
        <select v-model="dependentForm.gender" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="FEMALE">Female</option>
          <option value="MALE">Male</option>
          <option value="OTHER">Other</option>
          <option value="UNKNOWN">Unknown</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Date of birth</label>
        <input v-model="dependentForm.dob" type="date" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Plan tier</label>
        <input v-model="dependentForm.planTier" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Coverage start date</label>
        <input v-model="dependentForm.startDate" type="date" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Coverage end date (optional)</label>
        <input v-model="dependentForm.endDate" type="date" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>

      <p v-if="dependentError" class="col-span-2 text-critical text-sm">{{ dependentError }}</p>

      <div class="col-span-2 flex gap-2">
        <button
          type="submit"
          :disabled="addingDependent"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ addingDependent ? 'Saving…' : 'Add dependent' }}
        </button>
        <button
          type="button"
          class="border border-line-strong rounded-[7px] px-4.5 py-2.5 text-sm font-semibold"
          @click="addingDependentFor = null"
        >
          Cancel
        </button>
      </div>
    </form>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading members&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Member</th>
            <th class="px-4 py-3 font-bold">National ID</th>
            <th class="px-4 py-3 font-bold">Contact</th>
            <th class="px-4 py-3 font-bold">Policy</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.patient.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ row.patient.firstName }} {{ row.patient.lastName }}</td>
            <td class="px-4 py-3 font-mono">{{ row.patient.nationalId }}</td>
            <td class="px-4 py-3">{{ row.patient.phone }}</td>
            <td class="px-4 py-3">{{ row.policyLabel ?? '—' }}</td>
            <td class="px-4 py-3"><StatusChip :active="row.patient.isActive" /></td>
            <td class="px-4 py-3">
              <div class="flex gap-2">
                <button
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                  @click="viewingPatient = row.patient"
                >
                  View
                </button>
                <button
                  v-if="!row.hasCoverage"
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                  @click="openAttachForm(row.patient)"
                >
                  Attach to Policy
                </button>
                <button
                  v-if="row.ownCoverage"
                  class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                  @click="openDependentForm(row.patient)"
                >
                  Add Dependent
                </button>
              </div>
            </td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="6" class="px-4 py-6 text-center text-muted">No members yet.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <RecordDetailModal
      v-if="viewingPatient"
      :title="`${viewingPatient.firstName} ${viewingPatient.lastName}`"
      :fields="patientFields(viewingPatient)"
      :load-fhir="() => getPatientFhir(viewingPatient!.id)"
      @close="viewingPatient = null"
    />
  </div>
</template>
