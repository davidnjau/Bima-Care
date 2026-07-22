<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { listPatients, type Patient } from '../../api/patients'
import { submitClaim, type Claim } from '../../api/claims'
import { serviceTypes } from '../../mocks/providerMocks'

const patients = ref<Patient[]>([])
const query = ref('')
const selected = ref<Patient | null>(null)

const matches = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q || selected.value) return []
  return patients.value
    .filter(
      (p) =>
        p.nationalId.toLowerCase().includes(q) ||
        `${p.firstName} ${p.lastName}`.toLowerCase().includes(q),
    )
    .slice(0, 8)
})

function selectPatient(patient: Patient) {
  selected.value = patient
  query.value = `${patient.firstName} ${patient.lastName} — ${patient.nationalId}`
}

function clearSelection() {
  selected.value = null
  query.value = ''
}

const form = reactive({
  serviceType: serviceTypes[0],
  diagnosisCode: '',
  treatmentDetails: '',
  amount: '',
})

const submitting = ref(false)
const submitError = ref('')
const submitted = ref<Claim | null>(null)

async function onSubmit() {
  if (!selected.value) return
  submitError.value = ''
  submitting.value = true
  submitted.value = null
  try {
    submitted.value = await submitClaim({
      patientId: selected.value.id,
      serviceType: form.serviceType,
      diagnosisCode: form.diagnosisCode,
      treatmentDetails: form.treatmentDetails,
      amount: form.amount,
    })
    clearSelection()
    Object.assign(form, { serviceType: serviceTypes[0], diagnosisCode: '', treatmentDetails: '', amount: '' })
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : 'Failed to submit claim.'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  patients.value = await listPatients()
})
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Submit Claim</h2>
      <p class="text-muted text-sm mt-1">Submit a claim for a verified member's treatment.</p>
    </div>

    <p v-if="submitted" class="bg-success-soft text-success text-sm rounded-[7px] px-4 py-3 mb-5">
      Claim {{ submitted.id }} submitted — status {{ submitted.status }}. An encounter and
      eligibility check ran automatically; an admin still needs to adjudicate it.
    </p>
    <p v-if="submitError" class="bg-critical-soft text-critical text-sm rounded-[7px] px-4 py-3 mb-5">
      {{ submitError }}
    </p>

    <form
      class="bg-white border border-line rounded-[10px] p-6 max-w-xl grid grid-cols-2 gap-4"
      @submit.prevent="onSubmit"
    >
      <div class="flex flex-col gap-1.5 col-span-2 relative">
        <label class="text-xs font-semibold text-muted">Member</label>
        <input
          v-model="query"
          :disabled="!!selected"
          placeholder="Search by national ID or name"
          required
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm disabled:bg-paper"
        />
        <button
          v-if="selected"
          type="button"
          class="absolute right-3 top-8 text-xs text-muted hover:text-critical"
          @click="clearSelection"
        >
          Change
        </button>
        <ul
          v-if="matches.length"
          class="border border-line rounded-[7px] divide-y divide-line overflow-hidden"
        >
          <li
            v-for="patient in matches"
            :key="patient.id"
            class="px-3 py-2 text-sm hover:bg-brand-tint cursor-pointer flex justify-between"
            @click="selectPatient(patient)"
          >
            <span class="font-semibold">{{ patient.firstName }} {{ patient.lastName }}</span>
            <span class="font-mono text-muted">{{ patient.nationalId }}</span>
          </li>
        </ul>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Service type</label>
        <select v-model="form.serviceType" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option v-for="type in serviceTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Claim amount (KES)</label>
        <input
          v-model="form.amount"
          type="number"
          min="0"
          required
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>
      <div class="flex flex-col gap-1.5 col-span-2">
        <label class="text-xs font-semibold text-muted">Diagnosis / ICD-10 code</label>
        <input
          v-model="form.diagnosisCode"
          required
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
      </div>
      <div class="flex flex-col gap-1.5 col-span-2">
        <label class="text-xs font-semibold text-muted">Treatment details</label>
        <textarea
          v-model="form.treatmentDetails"
          required
          rows="3"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        ></textarea>
      </div>

      <div class="col-span-2">
        <button
          type="submit"
          :disabled="submitting || !selected"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ submitting ? 'Submitting…' : 'Submit Claim' }}
        </button>
      </div>
    </form>

    <ul class="text-muted text-sm mt-5 list-disc pl-5 space-y-1">
      <li>Claims must be submitted within 30 days of service.</li>
      <li>Member eligibility verification is required before submission.</li>
      <li>Accurate diagnosis and treatment information must be provided.</li>
    </ul>
  </div>
</template>
