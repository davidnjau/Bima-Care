<script setup lang="ts">
import { nextTick, onMounted, ref, watch } from 'vue'
import { useMemberStore } from '../../stores/member'
import { getPatient, type Patient } from '../../api/patients'

const member = useMemberStore()
const patient = ref<Patient | null>(null)
const loading = ref(false)
const error = ref('')
const qrCanvas = ref<HTMLCanvasElement | null>(null)

async function load() {
  if (!member.selectedPatientId) return
  loading.value = true
  error.value = ''
  try {
    patient.value = await getPatient(member.selectedPatientId)
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Failed to load member.'
  } finally {
    loading.value = false
  }
  // The canvas only mounts once `loading` is false (it's gated by the same
  // v-else-if), so drawing has to wait until after that DOM patch.
  if (patient.value) {
    await nextTick()
    drawQr()
  }
}

function drawQr() {
  const canvas = qrCanvas.value
  if (!canvas) return
  const ctx = canvas.getContext('2d')
  if (!ctx) return
  const n = 15
  const cell = canvas.width / n
  ctx.fillStyle = '#fff'
  ctx.fillRect(0, 0, canvas.width, canvas.height)
  ctx.fillStyle = '#16201c'
  let seed = (patient.value?.nationalId.length ?? 7) * 97 + 13
  const rand = () => {
    seed = (seed * 9301 + 49297) % 233280
    return seed / 233280
  }
  for (let y = 0; y < n; y++) {
    for (let x = 0; x < n; x++) {
      const inFinder = (x < 4 && y < 4) || (x > n - 5 && y < 4) || (x < 4 && y > n - 5)
      if (inFinder) continue
      if (rand() > 0.56) ctx.fillRect(x * cell, y * cell, cell - 1, cell - 1)
    }
  }
  ;[
    [0, 0],
    [n - 4, 0],
    [0, n - 4],
  ].forEach(([fx, fy]) => {
    ctx.fillStyle = '#16201c'
    ctx.fillRect(fx * cell, fy * cell, cell * 4, cell * 4)
    ctx.fillStyle = '#fff'
    ctx.fillRect(fx * cell + cell * 0.7, fy * cell + cell * 0.7, cell * 2.6, cell * 2.6)
    ctx.fillStyle = '#16201c'
    ctx.fillRect(fx * cell + cell * 1.3, fy * cell + cell * 1.3, cell * 1.4, cell * 1.4)
  })
}

watch(() => member.selectedPatientId, load)
onMounted(load)
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">My Card</h2>
      <p class="text-muted text-sm mt-1">Your digital insurance identity.</p>
    </div>

    <p v-if="error" class="text-critical text-sm mb-4">{{ error }}</p>
    <p v-if="loading" class="text-muted text-sm">Loading&hellip;</p>

    <div v-else-if="patient" class="max-w-sm">
      <div
        class="rounded-2xl p-5 text-white relative overflow-hidden"
        style="background: linear-gradient(135deg, #1f4d3d 0%, #122a22 100%)"
      >
        <div class="flex justify-between items-start mb-5">
          <span class="font-display font-bold">BimaCare</span>
          <span
            class="text-[0.68rem] font-bold uppercase tracking-wide px-2.5 py-1 rounded-full"
            :class="patient.isActive ? 'bg-white/20' : 'bg-critical/60'"
          >
            {{ patient.isActive ? 'Active' : 'Suspended' }}
          </span>
        </div>
        <div class="text-lg font-bold mb-0.5">{{ patient.firstName }} {{ patient.lastName }}</div>
        <div class="font-mono text-[0.78rem] text-[#c9d6ce] tracking-wide">
          {{ patient.nationalId }}
        </div>
        <div class="flex justify-between items-end mt-5">
          <div class="text-[0.72rem] text-[#c9d6ce]">
            Gender
            <b class="block text-white text-[0.86rem] font-sans">{{ patient.gender }}</b>
          </div>
          <div class="bg-white p-1.5 rounded-lg leading-none">
            <canvas ref="qrCanvas" width="72" height="72"></canvas>
          </div>
        </div>
      </div>
      <p class="text-muted text-xs mt-2 text-center">Born {{ patient.dob }}</p>
    </div>
  </div>
</template>
