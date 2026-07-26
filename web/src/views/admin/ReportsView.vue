<script setup lang="ts">
import { claimsTrend, premiumTrend, topProviders, reports } from '../../mocks/adminMocks'

const claimsTrendMax = Math.max(...claimsTrend.map((m) => m.total))
const premiumTrendMax = Math.max(...premiumTrend.map((m) => m.amount))
const topProvidersMax = Math.max(...topProviders.map((p) => p.volume))

function formatKsh(amount: number): string {
  return `Ksh ${(amount / 1_000_000).toFixed(1)}M`
}
</script>

<template>
  <div>
    <div class="flex items-end justify-between gap-5 flex-wrap mb-6">
      <div>
        <h2 class="font-display text-2xl font-semibold">Reports</h2>
        <p class="text-muted text-sm mt-1">Insights and performance metrics</p>
      </div>
      <button
        disabled
        title="Export requires a reporting/aggregation service — not built yet"
        class="bg-accent text-white font-semibold text-sm rounded-[7px] px-4.5 py-2.5 opacity-50 cursor-not-allowed"
      >
        Export All Reports
      </button>
    </div>

    <div class="grid gap-5 lg:grid-cols-2 mb-6">
      <div class="bg-white border border-line rounded-[10px] p-5">
        <h3 class="text-sm font-semibold mb-4">Claims Trend (6 months)</h3>
        <div class="flex items-end gap-3 h-36">
          <div v-for="m in claimsTrend" :key="m.month" class="flex-1 flex flex-col items-center gap-1.5">
            <div class="w-full flex items-end gap-1 h-28">
              <div
                class="flex-1 bg-brand-tint rounded-t"
                :style="{ height: `${(m.total / claimsTrendMax) * 100}%` }"
                :title="`Total: ${m.total}`"
              ></div>
              <div
                class="flex-1 bg-brand rounded-t"
                :style="{ height: `${(m.approved / claimsTrendMax) * 100}%` }"
                :title="`Approved: ${m.approved}`"
              ></div>
            </div>
            <span class="text-[0.68rem] text-muted">{{ m.month }}</span>
          </div>
        </div>
        <div class="flex gap-4 mt-3 text-xs text-muted">
          <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-sm bg-brand-tint"></span>Total Claims</span>
          <span class="flex items-center gap-1.5"><span class="w-2.5 h-2.5 rounded-sm bg-brand"></span>Approved</span>
        </div>
      </div>

      <div class="bg-white border border-line rounded-[10px] p-5">
        <h3 class="text-sm font-semibold mb-4">Premium Collection Trend (6 months)</h3>
        <div class="flex items-end gap-3 h-36">
          <div v-for="m in premiumTrend" :key="m.month" class="flex-1 flex flex-col items-center gap-1.5">
            <div class="w-full flex items-end h-28">
              <div
                class="w-full bg-accent rounded-t"
                :style="{ height: `${(m.amount / premiumTrendMax) * 100}%` }"
                :title="formatKsh(m.amount)"
              ></div>
            </div>
            <span class="text-[0.68rem] text-muted">{{ m.month }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="bg-white border border-line rounded-[10px] p-5 mb-6">
      <h3 class="text-sm font-semibold mb-4">Top Providers by Claims Volume</h3>
      <div class="flex flex-col gap-3">
        <div v-for="p in topProviders" :key="p.name" class="flex items-center gap-3">
          <span class="w-36 text-sm shrink-0">{{ p.name }}</span>
          <div class="flex-1 bg-brand-tint rounded h-4">
            <div
              class="bg-brand h-4 rounded"
              :style="{ width: `${(p.volume / topProvidersMax) * 100}%` }"
            ></div>
          </div>
          <span class="text-sm font-mono w-12 text-right">{{ p.volume }}</span>
        </div>
      </div>
    </div>

    <div class="grid gap-4 sm:grid-cols-2">
      <div
        v-for="report in reports"
        :key="report.title"
        class="bg-white border border-line rounded-[10px] p-5 flex items-start justify-between gap-4"
      >
        <div>
          <h3 class="font-semibold text-sm">{{ report.title }}</h3>
          <p class="text-muted text-sm mt-1">{{ report.description }}</p>
        </div>
        <button
          disabled
          title="Export requires a reporting/aggregation service — not built yet"
          class="border border-line-strong rounded-[7px] px-3.5 py-2 text-xs font-semibold opacity-50 cursor-not-allowed shrink-0"
        >
          Download
        </button>
      </div>
    </div>
  </div>
</template>
