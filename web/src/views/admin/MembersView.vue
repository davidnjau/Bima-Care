<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { createPatient, listPatients, type CreatePatientRequest, type Patient } from '../../api/patients'
import { listCoverages, type Coverage } from '../../api/coverages'
import StatusChip from '../../components/StatusChip.vue'

const loading = ref(true)
const error = ref('')
const patients = ref<Patient[]>([])
const coverages = ref<Coverage[]>([])

const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive<CreatePatientRequest>({
  nationalId: '',
  firstName: '',
  lastName: '',
  phone: '',
  gender: 'FEMALE',
  dob: '',
})

const rows = computed(() =>
  patients.value.map((patient) => ({
    ...patient,
    planTier: coverages.value.find((c) => c.patientId === patient.id)?.planTier ?? null,
  })),
)

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [patientList, coverageList] = await Promise.all([listPatients(), listCoverages()])
    patients.value = patientList
    coverages.value = coverageList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load members.'
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  formError.value = ''
  saving.value = true
  try {
    await createPatient({ ...form })
    showForm.value = false
    Object.assign(form, {
      nationalId: '',
      firstName: '',
      lastName: '',
      phone: '',
      gender: 'FEMALE',
      dob: '',
    })
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to add member.'
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <div class="flex items-end justify-between gap-5 flex-wrap mb-6">
      <div>
        <h2 class="font-display text-2xl font-semibold">Members</h2>
        <p class="text-muted text-sm mt-1">Manage member accounts and policies</p>
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

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading members…</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Member</th>
            <th class="px-4 py-3 font-bold">National ID</th>
            <th class="px-4 py-3 font-bold">Contact</th>
            <th class="px-4 py-3 font-bold">Policy</th>
            <th class="px-4 py-3 font-bold">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="row in rows" :key="row.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ row.firstName }} {{ row.lastName }}</td>
            <td class="px-4 py-3 font-mono">{{ row.nationalId }}</td>
            <td class="px-4 py-3">{{ row.phone }}</td>
            <td class="px-4 py-3">{{ row.planTier ?? '—' }}</td>
            <td class="px-4 py-3"><StatusChip :active="row.isActive" /></td>
          </tr>
          <tr v-if="rows.length === 0">
            <td colspan="5" class="px-4 py-6 text-center text-muted">No members yet.</td>
          </tr>
        </tbody>
      </table>
    </div>
  </div>
</template>
