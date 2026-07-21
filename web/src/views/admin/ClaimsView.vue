<script setup lang="ts">
import { ref } from 'vue'
import { pendingClaims, processedClaims } from '../../mocks/adminMocks'

const tab = ref<'pending' | 'processed'>('pending')
const notice = ref('')

function act(action: 'Approve' | 'Partial' | 'Reject', claimId: string) {
  notice.value = `${action} recorded for ${claimId} (preview only — claims adjudication needs the Claims service, Phase 2).`
}
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Claims</h2>
      <p class="text-muted text-sm mt-1">{{ pendingClaims.length }} pending</p>
    </div>

    <p v-if="notice" class="bg-success-soft text-success text-sm rounded-[7px] px-4 py-3 mb-5">
      {{ notice }}
    </p>

    <div class="flex gap-1 border-b border-line mb-5">
      <button
        class="px-4 py-2.5 text-sm border-b-2 -mb-px"
        :class="tab === 'pending' ? 'border-accent text-brand font-semibold' : 'border-transparent text-muted'"
        @click="tab = 'pending'"
      >
        Pending Review
      </button>
      <button
        class="px-4 py-2.5 text-sm border-b-2 -mb-px"
        :class="tab === 'processed' ? 'border-accent text-brand font-semibold' : 'border-transparent text-muted'"
        @click="tab = 'processed'"
      >
        Processed
      </button>
    </div>

    <div v-if="tab === 'pending'" class="grid gap-4">
      <div
        v-for="claim in pendingClaims"
        :key="claim.id"
        class="bg-white border border-line rounded-[10px] p-5"
      >
        <div class="flex items-start justify-between gap-4 flex-wrap">
          <div>
            <div class="font-mono text-sm text-muted">{{ claim.id }}</div>
            <h3 class="font-display text-lg font-semibold mt-0.5">{{ claim.member }}</h3>
            <p class="text-sm text-muted mt-1">{{ claim.provider }} &middot; {{ claim.type }}</p>
          </div>
          <div class="text-right">
            <div class="font-mono font-semibold">{{ claim.amount }}</div>
            <div class="text-xs text-muted mt-0.5">{{ claim.date }}</div>
          </div>
        </div>
        <p class="text-sm mt-3">{{ claim.diagnosis }}</p>
        <div class="flex gap-2 mt-4">
          <button
            class="bg-success text-white font-semibold text-xs rounded-[7px] px-3.5 py-2"
            @click="act('Approve', claim.id)"
          >
            Approve
          </button>
          <button
            class="bg-warning text-white font-semibold text-xs rounded-[7px] px-3.5 py-2"
            @click="act('Partial', claim.id)"
          >
            Partial
          </button>
          <button
            class="bg-critical text-white font-semibold text-xs rounded-[7px] px-3.5 py-2"
            @click="act('Reject', claim.id)"
          >
            Reject
          </button>
        </div>
      </div>
    </div>

    <div v-else class="border border-line rounded-[10px] p-6 text-center text-muted text-sm">
      <template v-if="processedClaims.length === 0">No processed claims yet.</template>
    </div>
  </div>
</template>
