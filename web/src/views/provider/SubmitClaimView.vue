<script setup lang="ts">
import { reactive, ref } from 'vue'
import { serviceTypes } from '../../mocks/providerMocks'

const form = reactive({
  memberId: '',
  serviceType: serviceTypes[0],
  diagnosis: '',
  treatmentDetails: '',
  amount: '',
})

const submitted = ref(false)

function onSubmit() {
  submitted.value = true
  Object.assign(form, {
    memberId: '',
    serviceType: serviceTypes[0],
    diagnosis: '',
    treatmentDetails: '',
    amount: '',
  })
}
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Submit Claim</h2>
      <p class="text-muted text-sm mt-1">Submit a claim for a verified member's treatment.</p>
    </div>

    <p v-if="submitted" class="bg-success-soft text-success text-sm rounded-[7px] px-4 py-3 mb-5">
      Claim captured. This is a preview form — nothing was sent yet; claims submission goes live
      once the Claims service (Phase 2) exists.
    </p>

    <form
      class="bg-white border border-line rounded-[10px] p-6 max-w-xl grid grid-cols-2 gap-4"
      @submit.prevent="onSubmit"
    >
      <div class="flex flex-col gap-1.5 col-span-2">
        <label class="text-xs font-semibold text-muted">Member ID</label>
        <input
          v-model="form.memberId"
          required
          placeholder="e.g. KIC-MEM-2024-001234"
          class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
        />
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
          v-model="form.diagnosis"
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
          class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5"
        >
          Submit Claim
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
