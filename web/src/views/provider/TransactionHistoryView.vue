<script setup lang="ts">
import { transactions, transactionSummary } from '../../mocks/providerMocks'
import StatusBadge from '../../components/StatusBadge.vue'
</script>

<template>
  <div>
    <div class="mb-6">
      <h2 class="font-display text-2xl font-semibold">Transaction History</h2>
      <p class="text-muted text-sm mt-1">Claims submitted by this facility.</p>
    </div>

    <div class="grid grid-cols-3 gap-4 mb-6 max-w-xl">
      <div class="bg-white border border-line rounded-[10px] p-4">
        <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Total claims</span>
        <div class="font-display text-2xl mt-1 tnum">{{ transactionSummary.totalClaims }}</div>
      </div>
      <div class="bg-white border border-line rounded-[10px] p-4">
        <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Approved</span>
        <div class="font-display text-2xl mt-1 tnum">{{ transactionSummary.approvedClaims }}</div>
      </div>
      <div class="bg-white border border-line rounded-[10px] p-4">
        <span class="text-[0.68rem] uppercase tracking-wider text-muted font-semibold">Total value</span>
        <div class="font-display text-2xl mt-1 tnum">{{ transactionSummary.totalValue }}</div>
      </div>
    </div>

    <div class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Reference</th>
            <th class="px-4 py-3 font-bold">Type</th>
            <th class="px-4 py-3 font-bold">Member</th>
            <th class="px-4 py-3 font-bold">Service</th>
            <th class="px-4 py-3 font-bold">Amount</th>
            <th class="px-4 py-3 font-bold">Date</th>
            <th class="px-4 py-3 font-bold">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="tx in transactions" :key="tx.reference" class="border-t border-line">
            <td class="px-4 py-3 font-mono">{{ tx.reference }}</td>
            <td class="px-4 py-3">{{ tx.type }}</td>
            <td class="px-4 py-3 font-semibold">{{ tx.member }}</td>
            <td class="px-4 py-3">{{ tx.service }}</td>
            <td class="px-4 py-3 font-mono">{{ tx.amount }}</td>
            <td class="px-4 py-3 font-mono">{{ tx.date }}</td>
            <td class="px-4 py-3"><StatusBadge :status="tx.status" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="text-muted text-sm mt-5">
      Preview data for navigation review — real transaction history goes live once the Claims
      service (Phase 2) exists.
    </p>
  </div>
</template>
