<script setup lang="ts">
import { policies } from '../../mocks/adminMocks'
import StatusBadge from '../../components/StatusBadge.vue'
</script>

<template>
  <div>
    <div class="flex items-end justify-between gap-5 flex-wrap mb-6">
      <div>
        <h2 class="font-display text-2xl font-semibold">Policies</h2>
        <p class="text-muted text-sm mt-1">Manage insurance policies and benefit packages</p>
      </div>
      <button
        disabled
        title="Policy creation needs a backend policy model — not built yet"
        class="bg-accent text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 opacity-50 cursor-not-allowed"
      >
        Create Policy
      </button>
    </div>

    <div class="border border-line rounded-[10px] overflow-x-auto">
      <table class="w-full text-sm">
        <thead>
          <tr class="bg-brand-tint text-left text-[0.7rem] uppercase tracking-wide text-muted">
            <th class="px-4 py-3 font-bold">Policy</th>
            <th class="px-4 py-3 font-bold">Policy number</th>
            <th class="px-4 py-3 font-bold">Type</th>
            <th class="px-4 py-3 font-bold">Premium</th>
            <th class="px-4 py-3 font-bold">Members</th>
            <th class="px-4 py-3 font-bold">Period</th>
            <th class="px-4 py-3 font-bold">Status</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="policy in policies" :key="policy.number" class="border-t border-line">
            <td class="px-4 py-3 font-semibold">{{ policy.name }}</td>
            <td class="px-4 py-3 font-mono">{{ policy.number }}</td>
            <td class="px-4 py-3">{{ policy.type }}</td>
            <td class="px-4 py-3 font-mono">{{ policy.premium }}</td>
            <td class="px-4 py-3 tnum">{{ policy.members }}</td>
            <td class="px-4 py-3 font-mono">{{ policy.period }}</td>
            <td class="px-4 py-3"><StatusBadge :status="policy.status" /></td>
          </tr>
        </tbody>
      </table>
    </div>

    <p class="text-muted text-sm mt-5">
      Dummy data for navigation review — policy CRUD depends on a backend policy model that
      doesn't exist yet (see <code class="font-mono">implementation-ui.md</code>).
    </p>
  </div>
</template>
