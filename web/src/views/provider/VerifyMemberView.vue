<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { listPatients, type Patient } from '../../api/patients'
import { verifyEligibility, type EligibilityResult } from '../../api/eligibility'

const loading = ref(true)
const error = ref('')
const patients = ref<Patient[]>([])
const query = ref('')

const checking = ref(false)
const checkError = ref('')
const selected = ref<Patient | null>(null)
const result = ref<EligibilityResult | null>(null)

const matches = computed(() => {
  const q = query.value.trim().toLowerCase()
  if (!q) return []
  return patients.value
    .filter(
      (p) =>
        p.nationalId.toLowerCase().includes(q) ||
        `${p.firstName} ${p.lastName}`.toLowerCase().includes(q),
    )
    .slice(0, 8)
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    patients.value = await listPatients()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load member list.'
  } finally {
    loading.value = false
  }
}

async function selectPatient(patient: Patient) {
  selected.value = patient
  query.value = ''
  result.value = null
  checkError.value = ''
  checking.value = true
  try {
    result.value = await verifyEligibility(patient.id)
  } catch (e) {
    checkError.value = e instanceof Error ? e.message : 'Failed to verify eligibility.'
  } finally {
    checking.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Verify Member</h2>
      <p class="text-muted text-sm mt-1">
        Confirm cover status and plan before treatment.
      </p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>

    <div class="bg-white border border-line rounded-[10px] p-6 max-w-xl">
      <label class="text-xs font-semibold text-muted">National ID or member name</label>
      <input
        v-model="query"
        :disabled="loading"
        placeholder="e.g. SMOKE-PAT-001 or Asha"
        class="mt-1.5 w-full border border-line-strong rounded-[7px] px-3 py-2.5 text-sm focus:outline-2 focus:outline-accent focus:border-accent"
      />

      <ul v-if="matches.length" class="mt-2 border border-line rounded-[7px] divide-y divide-line overflow-hidden">
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
      <p v-else-if="query.trim()" class="text-muted text-sm mt-2">No matching members.</p>

      <div v-if="checking" class="text-muted text-sm mt-5">Checking coverage&hellip;</div>

      <p v-if="checkError" class="text-critical text-sm mt-5">{{ checkError }}</p>

      <div v-if="!checking && result && selected" class="mt-5">
        <div
          v-if="result.eligible"
          class="bg-success-soft border border-success/30 rounded-[10px] p-4"
        >
          <p class="font-semibold text-success">Coverage confirmed</p>
          <p class="text-sm mt-1">
            {{ selected.firstName }} {{ selected.lastName }} is actively covered under
            <b>{{ result.coverage?.planTier }}</b>.
          </p>
          <p class="text-xs text-muted mt-2 font-mono">
            {{ result.coverage?.startDate }} &ndash; {{ result.coverage?.endDate ?? 'ongoing' }}
          </p>
        </div>
        <div v-else class="bg-critical-soft border border-critical/30 rounded-[10px] p-4">
          <p class="font-semibold text-critical">Not covered</p>
          <p class="text-sm mt-1">
            {{ selected.firstName }} {{ selected.lastName }} has no active coverage on file.
          </p>
        </div>
      </div>
    </div>
  </div>
</template>
