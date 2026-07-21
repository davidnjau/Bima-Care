<script setup lang="ts">
import { claims } from '../../mocks/memberMocks'
import StatusBadge from '../../components/StatusBadge.vue'
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Claims History</h2>
      <p class="text-muted text-sm mt-1">Your past claims.</p>
    </div>

    <div class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Provider</th>
            <th class="px-4 py-3 font-bold">Date</th>
            <th class="px-4 py-3 font-bold">Type</th>
            <th class="px-4 py-3 font-bold">Diagnosis</th>
            <th class="px-4 py-3 font-bold">Amount</th>
            <th class="px-4 py-3 font-bold">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="claim in claims" :key="claim.provider + claim.date" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ claim.provider }}</td>
            <td class="px-4 py-3 font-mono">{{ claim.date }}</td>
            <td class="px-4 py-3">{{ claim.type }}</td>
            <td class="px-4 py-3 text-muted">{{ claim.diagnosis }}</td>
            <td class="px-4 py-3 font-mono">{{ claim.amount }}</td>
            <td class="px-4 py-3"><StatusBadge :status="claim.status" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="text-muted text-sm mt-5">
      Preview data for navigation review — real claims history goes live once the Claims service
      (Phase 2) exists.
    </p>
  </div>
</template>
