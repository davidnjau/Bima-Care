<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useAuthStore } from '../../stores/auth'
import { listOrganizations, type Organization } from '../../api/organizations'
import { uploadDocument } from '../../api/documents'
import { submitReimbursementClaim, type Claim } from '../../api/claims'
import { serviceTypes } from '../../mocks/providerMocks'

const MAX_FILE_BYTES = 10 * 1024 * 1024
const ACCEPTED_TYPES = '.pdf,.jpg,.jpeg,.png'

const auth = useAuthStore()
const canSubmit = computed(() => auth.hasRealSession && auth.isMember && !!auth.patientId)

const today = new Date().toISOString().slice(0, 10)
const earliestServiceDate = new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString().slice(0, 10)

const organizations = ref<Organization[]>([])
const loadError = ref('')

const form = reactive({
  organizationId: '',
  serviceType: serviceTypes[0],
  diagnosisCode: '',
  treatmentDetails: '',
  amount: '',
  dateOfService: today,
})

const claimFormFile = ref<File | null>(null)
const itemizedReceiptFile = ref<File | null>(null)
const etrFile = ref<File | null>(null)

const submitting = ref(false)
const submitError = ref('')
const submitted = ref<Claim | null>(null)

function pickFile(event: Event, target: 'claimForm' | 'itemizedReceipt' | 'etr') {
  const file = (event.target as HTMLInputElement).files?.[0] ?? null
  if (file && file.size > MAX_FILE_BYTES) {
    submitError.value = `${file.name} is larger than 10MB.`
    ;(event.target as HTMLInputElement).value = ''
    return
  }
  submitError.value = ''
  if (target === 'claimForm') claimFormFile.value = file
  if (target === 'itemizedReceipt') itemizedReceiptFile.value = file
  if (target === 'etr') etrFile.value = file
}

function resetForm() {
  Object.assign(form, {
    organizationId: '',
    serviceType: serviceTypes[0],
    diagnosisCode: '',
    treatmentDetails: '',
    amount: '',
    dateOfService: today,
  })
  claimFormFile.value = null
  itemizedReceiptFile.value = null
  etrFile.value = null
}

async function onSubmit() {
  if (!auth.patientId || !claimFormFile.value || !itemizedReceiptFile.value || !etrFile.value) return
  submitError.value = ''
  submitting.value = true
  submitted.value = null
  try {
    const patientId = auth.patientId
    const [claimForm, itemizedReceipt, etr] = await Promise.all([
      uploadDocument({
        patientId,
        title: claimFormFile.value.name,
        category: 'claim-form',
        file: claimFormFile.value,
      }),
      uploadDocument({
        patientId,
        title: itemizedReceiptFile.value.name,
        category: 'itemised-receipt',
        file: itemizedReceiptFile.value,
      }),
      uploadDocument({
        patientId,
        title: etrFile.value.name,
        category: 'etr',
        file: etrFile.value,
      }),
    ])

    submitted.value = await submitReimbursementClaim({
      organizationId: form.organizationId,
      serviceType: form.serviceType,
      diagnosisCode: form.diagnosisCode,
      treatmentDetails: form.treatmentDetails,
      amount: form.amount,
      dateOfService: form.dateOfService,
      claimFormDocumentId: claimForm.id,
      itemizedReceiptDocumentId: itemizedReceipt.id,
      etrDocumentId: etr.id,
    })
    resetForm()
  } catch (e) {
    submitError.value = e instanceof Error ? e.message : 'Failed to submit claim.'
  } finally {
    submitting.value = false
  }
}

onMounted(async () => {
  try {
    organizations.value = await listOrganizations()
  } catch (e) {
    loadError.value = e instanceof Error ? e.message : 'Failed to load providers.'
  }
})
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Submit Claim</h2>
      <p class="text-muted text-sm mt-1">
        Paid cash at a hospital instead of using your card? Submit a reimbursement claim with
        your claim form, itemised receipt, and ETR.
      </p>
    </div>

    <p v-if="!canSubmit" class="bg-warning-soft text-warning text-sm rounded-[7px] px-4 py-3">
      Submitting a reimbursement claim requires a real, individually-linked member login — this
      demo preview can't submit a claim on your behalf.
    </p>

    <template v-else>
      <p v-if="submitted" class="bg-success-soft text-success text-sm rounded-[7px] px-4 py-3 mb-5">
        Claim {{ submitted.id }} submitted — status {{ submitted.status }}. An admin will review
        your documents and adjudicate the claim.
      </p>
      <p v-if="submitError" class="bg-critical-soft text-critical text-sm rounded-[7px] px-4 py-3 mb-5">
        {{ submitError }}
      </p>
      <p v-if="loadError" class="bg-critical-soft text-critical text-sm rounded-[7px] px-4 py-3 mb-5">
        {{ loadError }}
      </p>

      <form
        class="bg-white border border-line rounded-[10px] p-6 max-w-xl grid grid-cols-2 gap-4"
        @submit.prevent="onSubmit"
      >
        <div class="flex flex-col gap-1.5 col-span-2">
          <label class="text-xs font-semibold text-muted">Hospital / provider</label>
          <select
            v-model="form.organizationId"
            required
            class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
          >
            <option value="" disabled>Select the hospital or clinic you visited</option>
            <option v-for="org in organizations" :key="org.id" :value="org.id">{{ org.name }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-muted">Service type</label>
          <select v-model="form.serviceType" class="border border-line-strong rounded-[7px] px-3 py-2 text-sm">
            <option v-for="type in serviceTypes" :key="type" :value="type">{{ type }}</option>
          </select>
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-muted">Date of service</label>
          <input
            v-model="form.dateOfService"
            type="date"
            required
            :min="earliestServiceDate"
            :max="today"
            class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
          />
        </div>
        <div class="flex flex-col gap-1.5">
          <label class="text-xs font-semibold text-muted">Amount paid (KES)</label>
          <input
            v-model="form.amount"
            type="number"
            min="0"
            required
            class="border border-line-strong rounded-[7px] px-3 py-2 text-sm"
          />
        </div>
        <div class="flex flex-col gap-1.5">
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

        <div class="col-span-2 border-t border-line pt-4 mt-1">
          <p class="text-xs font-semibold text-muted mb-3">
            Supporting documents (PDF, JPG, or PNG — max 10MB each)
          </p>
          <div class="grid grid-cols-3 gap-4">
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-semibold text-muted">Claim form</label>
              <input
                :accept="ACCEPTED_TYPES"
                type="file"
                required
                class="text-xs"
                @change="pickFile($event, 'claimForm')"
              />
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-semibold text-muted">Itemised receipt</label>
              <input
                :accept="ACCEPTED_TYPES"
                type="file"
                required
                class="text-xs"
                @change="pickFile($event, 'itemizedReceipt')"
              />
            </div>
            <div class="flex flex-col gap-1.5">
              <label class="text-xs font-semibold text-muted">ETR (tax receipt)</label>
              <input
                :accept="ACCEPTED_TYPES"
                type="file"
                required
                class="text-xs"
                @change="pickFile($event, 'etr')"
              />
            </div>
          </div>
        </div>

        <div class="col-span-2">
          <button
            type="submit"
            :disabled="submitting"
            class="bg-brand text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 disabled:opacity-60"
          >
            {{ submitting ? 'Submitting…' : 'Submit Claim' }}
          </button>
        </div>
      </form>

      <ul class="text-muted text-sm mt-5 list-disc pl-5 space-y-1">
        <li>Claims must be submitted within 30 days of service.</li>
        <li>All three documents are required for a reimbursement claim to be reviewed.</li>
        <li>You must have active coverage on the date of service.</li>
      </ul>
    </template>
  </div>
</template>
