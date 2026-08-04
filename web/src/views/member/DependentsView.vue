<script setup lang="ts">
import { onMounted, ref, watch } from 'vue'
import { useMemberStore } from '../../stores/member'
import { listDependents, type Dependent } from '../../api/dependents'
import { listPatients, type Patient } from '../../api/patients'
import { formatDate } from '../../lib/formatDate'

const member = useMemberStore()
const dependents = ref<Dependent[]>([])
const patients = ref<Patient[]>([])
const loading = ref(true)
const error = ref('')

function patientFor(dependent: Dependent): Patient | undefined {
  return patients.value.find((p) => p.id === dependent.dependentPatientId)
}

function calculateAge(dob: string): number {
  const match = dob.match(/^(\d{4})-(\d{2})-(\d{2})$/)
  if (!match) return 0
  const year = Number(match[1])
  const month = Number(match[2])
  const day = Number(match[3])
  const today = new Date()
  let age = today.getFullYear() - year
  if (today.getMonth() + 1 < month || (today.getMonth() + 1 === month && today.getDate() < day)) {
    age--
  }
  return age
}

async function load() {
  if (!member.selectedPatientId) return
  loading.value = true
  error.value = ''
  try {
    const [dependentList, patientList] = await Promise.all([
      listDependents(member.selectedPatientId),
      listPatients(),
    ])
    dependents.value = dependentList
    patients.value = patientList
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load dependents.'
  } finally {
    loading.value = false
  }
}

watch(() => member.selectedPatientId, load)
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6 flex items-baseline justify-between gap-3">
      <div>
        <h2 class="font-display text-2xl font-semibold">My Dependents</h2>
        <p class="text-muted text-sm mt-1">Family members covered under your policy.</p>
      </div>
      <span class="text-sm text-muted">{{ dependents.length }} dependents</span>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <template v-else>
      <div class="grid gap-4 sm:grid-cols-2">
        <div
          v-for="dependent in dependents"
          :key="dependent.id"
          class="bg-white border border-line rounded-[10px] p-5"
        >
          <h3 class="font-display text-lg font-semibold">
            {{ patientFor(dependent)?.firstName }} {{ patientFor(dependent)?.lastName }}
          </h3>
          <dl class="grid grid-cols-2 gap-y-2 text-sm mt-3">
            <dt class="text-muted">Relationship</dt>
            <dd class="text-right">{{ dependent.relationship }}</dd>
            <dt class="text-muted">Gender</dt>
            <dd class="text-right">{{ patientFor(dependent)?.gender }}</dd>
            <dt class="text-muted">Date of birth</dt>
            <dd class="text-right font-mono">{{ formatDate(patientFor(dependent)?.dob) ?? '—' }}</dd>
            <dt class="text-muted">Age</dt>
            <dd class="text-right">
              {{ patientFor(dependent)?.dob ? calculateAge(patientFor(dependent)!.dob) : '—' }} years
            </dd>
          </dl>
        </div>
        <p v-if="dependents.length === 0" class="text-muted text-sm col-span-2">No dependents on file.</p>
      </div>

      <p class="text-muted text-sm mt-6">
        <b class="text-ink">Note:</b> All dependents listed above are covered under your family
        policy. Contact your insurer to add or remove dependents.
      </p>
    </template>
  </div>
</template>
