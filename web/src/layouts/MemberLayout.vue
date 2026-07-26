<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { listPatients, type Patient } from '../api/patients'
import { useMemberStore } from '../stores/member'
import { useAuthStore } from '../stores/auth'

const member = useMemberStore()
const auth = useAuthStore()
const router = useRouter()
const patients = ref<Patient[]>([])
const loading = ref(true)
const error = ref('')

const isRealMemberSession = computed(() => auth.hasRealSession && auth.isMember)

const navItems = [
  { to: '/member/card', label: 'My Card' },
  { to: '/member/benefits', label: 'Policy & Benefits' },
  { to: '/member/claims', label: 'Claims History' },
  { to: '/member/dependents', label: 'Dependents' },
]

function logout() {
  auth.logout()
  router.push('/login')
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (isRealMemberSession.value && auth.patientId) {
      member.select(auth.patientId)
      return
    }
    await auth.ensureDemoSession()
    patients.value = await listPatients()
    if (!member.selectedPatientId && patients.value.length > 0) {
      member.select(patients.value[0].id)
    }
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load members.'
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div class="min-h-screen bg-paper">
    <header class="sticky top-0 z-40 bg-brand text-white border-b border-brand-dark">
      <div class="flex items-center justify-between px-7 py-3.5">
        <router-link to="/" class="font-display text-lg font-bold">
          Bima<span class="text-accent">&bull;</span>Care
        </router-link>
        <div class="flex items-center gap-3.5 text-sm">
          <span class="text-[0.68rem] uppercase tracking-wider text-brand-tint/80">
            {{ isRealMemberSession ? 'Member Portal' : 'Member preview' }}
          </span>
          <button
            v-if="isRealMemberSession"
            class="border border-white/35 rounded-[7px] px-3.5 py-1.5 text-xs hover:bg-white/10"
            @click="logout"
          >
            Logout
          </button>
        </div>
      </div>
      <nav class="flex gap-1 px-6 bg-white overflow-x-auto">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="px-4 py-3 text-sm text-muted border-b-2 border-transparent whitespace-nowrap"
          active-class="!border-accent !text-brand font-semibold"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </header>

    <div
      v-if="!isRealMemberSession"
      class="bg-warning-soft border-b border-warning/20 px-7 py-2.5 text-sm text-warning"
    >
      <b>Demo mode</b> &mdash; pick any member below to preview their record. This is not a real
      login.
    </div>

    <main class="max-w-3xl mx-auto px-7 py-8">
      <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>

      <template v-if="!isRealMemberSession">
        <div class="mb-6">
          <label class="text-xs font-semibold text-muted">Previewing as</label>
          <select
            :disabled="loading"
            :value="member.selectedPatientId"
            class="mt-1.5 block w-full sm:w-auto border border-line-strong rounded-[7px] px-3 py-2 text-sm bg-white"
            @change="member.select(($event.target as HTMLSelectElement).value)"
          >
            <option v-for="patient in patients" :key="patient.id" :value="patient.id">
              {{ patient.firstName }} {{ patient.lastName }} &mdash; {{ patient.nationalId }}
            </option>
          </select>
        </div>
        <p v-if="!loading && patients.length === 0" class="text-muted text-sm">
          No members exist yet — add one from the Admin console first.
        </p>
      </template>

      <router-view v-if="!loading && member.selectedPatientId" />
    </main>
  </div>
</template>
