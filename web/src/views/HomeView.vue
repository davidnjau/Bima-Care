<script setup lang="ts">
import BrandDot from '../components/BrandDot.vue'

const portals = [
  {
    key: 'member',
    label: 'Member',
    description: 'Preview a digital insurance card, coverage, and benefits for any member.',
    cta: 'Open member preview',
    to: '/member',
  },
  {
    key: 'provider',
    label: 'Provider',
    description: 'Verify member eligibility and coverage before treatment.',
    cta: 'Open provider portal',
    to: '/login',
  },
  {
    key: 'admin',
    label: 'Admin',
    description: 'Manage members, providers, and the network.',
    cta: 'Open admin console',
    to: '/login',
  },
]
</script>

<template>
  <div class="min-h-screen bg-paper flex flex-col items-center px-4 py-20">
    <div class="text-center mb-14 max-w-lg">
      <h1 class="font-display text-3xl font-bold text-brand mb-3">
        Bima<BrandDot />Care
      </h1>
      <p class="text-muted">
        Digital cover, verified in seconds &mdash; one platform connecting members, providers,
        and insurers.
      </p>
    </div>

    <div class="grid grid-cols-1 sm:grid-cols-3 gap-5 w-full max-w-4xl">
      <div
        v-for="portal in portals"
        :key="portal.key"
        class="bg-white border border-line rounded-[10px] p-6 flex flex-col gap-3"
      >
        <h3 class="font-display text-lg font-semibold">{{ portal.label }}</h3>
        <p class="text-muted text-sm flex-1">{{ portal.description }}</p>
        <router-link
          :to="portal.to"
          class="bg-accent hover:bg-accent-dark text-white text-sm font-semibold rounded-[7px] px-4 py-2.5 text-center"
        >
          {{ portal.cta }}
        </router-link>
      </div>
    </div>
  </div>
</template>
