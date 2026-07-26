<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getPatientFhir, listPatients, type Patient } from '../../api/patients'
import { formatDate } from '../../lib/formatDate'
import StatusChip from '../../components/StatusChip.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const patients = ref<Patient[]>([])
const loading = ref(true)
const error = ref('')
const viewingPatient = ref<Patient | null>(null)

function patientFields(patient: Patient) {
  return [
    { label: 'First name', value: patient.firstName },
    { label: 'Last name', value: patient.lastName },
    { label: 'National ID', value: patient.nationalId },
    { label: 'Phone', value: patient.phone },
    { label: 'Gender', value: patient.gender },
    { label: 'Date of birth', value: formatDate(patient.dob) ?? patient.dob },
    { label: 'Status', value: patient.isActive ? 'Active' : 'Inactive' },
    { label: 'Patient ID', value: patient.id },
  ]
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    patients.value = await listPatients()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load members.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Members</h2>
      <p class="text-muted text-sm mt-1">Members across the network</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Member</th>
            <th class="px-4 py-3 font-bold">National ID</th>
            <th class="px-4 py-3 font-bold">Contact</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="patient in patients" :key="patient.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ patient.firstName }} {{ patient.lastName }}</td>
            <td class="px-4 py-3 font-mono">{{ patient.nationalId }}</td>
            <td class="px-4 py-3">{{ patient.phone }}</td>
            <td class="px-4 py-3"><StatusChip :active="patient.isActive" /></td>
            <td class="px-4 py-3">
              <button
                class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                @click="viewingPatient = patient"
              >
                View
              </button>
            </td>
          </tr>
          <tr v-if="patients.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-muted">No members yet.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="text-muted text-sm mt-5">
      Showing all network members — scoping this to only members covered under your policies is a
      known follow-up (needs a patient &rarr; coverage &rarr; policy join that no endpoint
      supports yet).
    </p>

    <RecordDetailModal
      v-if="viewingPatient"
      :title="`${viewingPatient.firstName} ${viewingPatient.lastName}`"
      :fields="patientFields(viewingPatient)"
      :load-fhir="() => getPatientFhir(viewingPatient!.id)"
      @close="viewingPatient = null"
    />
  </div>
</template>
