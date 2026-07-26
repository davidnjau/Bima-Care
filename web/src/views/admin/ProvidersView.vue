<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import {
  createOrganization,
  getOrganizationFhir,
  listOrganizations,
  type CreateOrganizationRequest,
  type Organization,
} from '../../api/organizations'
import StatusChip from '../../components/StatusChip.vue'
import RecordDetailModal from '../../components/RecordDetailModal.vue'

const loading = ref(true)
const error = ref('')
const organizations = ref<Organization[]>([])

const viewingOrg = ref<Organization | null>(null)

function orgFields(org: Organization) {
  return [
    { label: 'Facility name', value: org.name },
    { label: 'Registration number', value: org.registrationNumber },
    { label: 'Type', value: org.type },
    { label: 'Phone', value: org.phone },
    { label: 'Address', value: org.address },
    { label: 'Status', value: org.isActive ? 'Active' : 'Inactive' },
    { label: 'Organization ID', value: org.id },
  ]
}

const showForm = ref(false)
const saving = ref(false)
const formError = ref('')
const form = reactive<CreateOrganizationRequest>({
  registrationNumber: '',
  name: '',
  type: 'HOSPITAL',
  phone: '',
  address: '',
})

async function load() {
  loading.value = true
  error.value = ''
  try {
    organizations.value = await listOrganizations()
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load providers.'
  } finally {
    loading.value = false
  }
}

async function onSubmit() {
  formError.value = ''
  saving.value = true
  try {
    await createOrganization({ ...form })
    showForm.value = false
    Object.assign(form, {
      registrationNumber: '',
      name: '',
      type: 'HOSPITAL',
      phone: '',
      address: '',
    })
    await load()
  } catch (e) {
    formError.value = e instanceof Error ? e.message : 'Failed to add provider.'
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
        <h2 class="font-display text-2xl font-semibold">Healthcare providers</h2>
        <p class="text-muted text-sm mt-1">Manage network hospitals, clinics, and pharmacies</p>
      </div>
      <button
        class="bg-accent hover:bg-accent-dark text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5"
        @click="showForm = !showForm"
      >
        {{ showForm ? 'Cancel' : 'Add provider' }}
      </button>
    </div>

    <form
      v-if="showForm"
      class="bg-white border border-line rounded-[10px] p-5 mb-6 grid grid-cols-2 gap-4"
      @submit.prevent="onSubmit"
    >
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Registration number</label>
        <input v-model="form.registrationNumber" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Facility name</label>
        <input v-model="form.name" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Type</label>
        <select v-model="form.type" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
          <option value="HOSPITAL">Hospital</option>
          <option value="CLINIC">Clinic</option>
          <option value="PHARMACY">Pharmacy</option>
          <option value="LAB">Lab</option>
          <option value="INSURER">Insurer</option>
        </select>
      </div>
      <div class="flex flex-col gap-1.5">
        <label class="text-xs font-semibold text-muted">Phone</label>
        <input v-model="form.phone" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>
      <div class="flex flex-col gap-1.5 col-span-2">
        <label class="text-xs font-semibold text-muted">Address</label>
        <input v-model="form.address" required class="border border-line-strong rounded-[7px] px-3 py-2 text-sm" />
      </div>

      <p v-if="formError" class="col-span-2 text-critical text-sm">{{ formError }}</p>

      <div class="col-span-2">
        <button
          type="submit"
          :disabled="saving"
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
        >
          {{ saving ? 'Saving…' : 'Save provider' }}
        </button>
      </div>
    </form>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading providers…</p>

    <div v-else class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Facility</th>
            <th class="px-4 py-3 font-bold">Registration</th>
            <th class="px-4 py-3 font-bold">Type</th>
            <th class="px-4 py-3 font-bold">Address</th>
            <th class="px-4 py-3 font-bold">Phone</th>
            <th class="px-4 py-3 font-bold">Status</th>
            <th class="px-4 py-3 font-bold">Action</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="org in organizations" :key="org.id" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ org.name }}</td>
            <td class="px-4 py-3 font-mono">{{ org.registrationNumber }}</td>
            <td class="px-4 py-3">{{ org.type }}</td>
            <td class="px-4 py-3">{{ org.address }}</td>
            <td class="px-4 py-3">{{ org.phone }}</td>
            <td class="px-4 py-3"><StatusChip :active="org.isActive" /></td>
            <td class="px-4 py-3">
              <button
                class="border border-line-strong rounded-[7px] px-3 py-1.5 text-xs font-semibold"
                @click="viewingOrg = org"
              >
                View
              </button>
            </td>
          </tr>
          <tr v-if="organizations.length === 0">
            <td colspan="7" class="px-4 py-6 text-center text-muted">No providers yet.</td>
          </tr>
        </tbody>
      </table>
    </div>

    <RecordDetailModal
      v-if="viewingOrg"
      :title="viewingOrg.name"
      :fields="orgFields(viewingOrg)"
      :load-fhir="() => getOrganizationFhir(viewingOrg!.id)"
      @close="viewingOrg = null"
    />
  </div>
</template>
