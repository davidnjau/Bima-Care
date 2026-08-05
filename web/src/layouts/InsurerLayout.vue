<script setup lang="ts">
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import BrandDot from '../components/BrandDot.vue'

const auth = useAuthStore()
const router = useRouter()

function logout() {
  auth.logout()
  router.push('/login')
}

const navItems = [
  { to: '/insurer/policies', label: 'Policies' },
  { to: '/insurer/claims', label: 'Claims' },
  { to: '/insurer/members', label: 'Members' },
]
</script>

<template>
  <div class="min-h-screen bg-paper">
    <header class="sticky top-0 z-40 bg-brand text-white border-b border-brand-dark">
      <div class="flex items-center justify-between px-7 py-3.5">
        <span class="font-display text-lg font-bold">
          Bima<BrandDot />Care
        </span>
        <div class="flex items-center gap-3.5 text-sm">
          <div class="flex flex-col items-end leading-tight">
            <b class="text-[0.82rem]">{{ auth.username }}</b>
            <span class="text-[0.68rem] uppercase tracking-wider text-brand-tint/80">
              Insurer Portal
            </span>
          </div>
          <button
            class="border border-white/35 rounded-[7px] px-3.5 py-1.5 text-xs hover:bg-white/10"
            @click="logout"
          >
            Logout
          </button>
        </div>
      </div>
      <nav class="flex gap-1 px-6 bg-white overflow-x-auto">
        <router-link
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="px-4 py-3 text-sm text-muted border-b-2 border-transparent whitespace-nowrap"
          active-class="!border-accent !text-brand font-semibold"
        >
          {{ item.label }}
        </router-link>
      </nav>
    </header>

    <main class="max-w-6xl mx-auto px-7 py-8">
      <router-view />
    </main>
  </div>
</template>
