<script setup lang="ts">
import { dependents } from '../../mocks/memberMocks'
</script>

<template>
  <div>
    <div class="mb-6 flex items-baseline justify-between gap-3">
      <div>
        <h2 class="font-display text-2xl font-semibold">My Dependents</h2>
        <p class="text-muted text-sm mt-1">Family members covered under your policy.</p>
      </div>
      <span class="text-sm text-muted">{{ dependents.length }} dependents</span>
    </div>

    <div class="grid gap-4 sm:grid-cols-2">
      <div
        v-for="dependent in dependents"
        :key="dependent.memberId"
        class="bg-white border border-line rounded-[10px] p-5"
      >
        <h3 class="font-display text-lg font-semibold">{{ dependent.name }}</h3>
        <dl class="grid grid-cols-2 gap-y-2 text-sm mt-3">
          <dt class="text-muted">Relationship</dt>
          <dd class="text-right">{{ dependent.relationship }}</dd>
          <dt class="text-muted">Gender</dt>
          <dd class="text-right">{{ dependent.gender }}</dd>
          <dt class="text-muted">Date of birth</dt>
          <dd class="text-right font-mono">{{ dependent.dob }}</dd>
          <dt class="text-muted">Age</dt>
          <dd class="text-right">{{ dependent.age }} years</dd>
          <dt class="text-muted">Member ID</dt>
          <dd class="text-right font-mono">{{ dependent.memberId }}</dd>
        </dl>
      </div>
    </div>

    <p class="text-muted text-sm mt-6">
      <b class="text-ink">Note:</b> All dependents listed above are covered under your family
      policy. Contact your insurer to add or remove dependents.
    </p>
    <p class="text-muted text-sm mt-2">
      Preview data for navigation review — Patient has no related-person concept yet, so this
      isn't backed by real data.
    </p>
  </div>
</template>
