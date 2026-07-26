<script setup lang="ts">
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { postLoginHome } from '../router'

const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const username = ref('')
const password = ref('')
const error = ref('')
const loading = ref(false)

async function onSubmit() {
  error.value = ''
  loading.value = true
  try {
    await auth.login(username.value, password.value)
    const explicitRedirect = route.query.redirect as string | undefined
    router.push(explicitRedirect || postLoginHome())
  } catch (e) {
    error.value = e instanceof Error ? e.message : 'Login failed.'
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="min-h-screen flex items-center justify-center bg-paper px-4">
    <div class="w-full max-w-sm">
      <div class="text-center mb-8">
        <h1 class="font-display text-2xl font-bold text-brand">
          Bima<span class="text-accent">&bull;</span>Care
        </h1>
        <p class="text-muted text-sm mt-1">Sign in</p>
      </div>

      <form
        class="bg-white border border-line rounded-[10px] p-7 flex flex-col gap-4"
        @submit.prevent="onSubmit"
      >
        <div class="flex flex-col gap-1.5">
          <label for="username" class="text-xs font-semibold text-muted">Email</label>
          <input
            id="username"
            v-model="username"
            type="text"
            required
            class="border border-line-strong rounded-[7px] px-3 py-2.5 text-sm focus:outline-2 focus:outline-accent focus:border-accent"
          />
        </div>
        <div class="flex flex-col gap-1.5">
          <label for="password" class="text-xs font-semibold text-muted">Password</label>
          <input
            id="password"
            v-model="password"
            type="password"
            required
            class="border border-line-strong rounded-[7px] px-3 py-2.5 text-sm focus:outline-2 focus:outline-accent focus:border-accent"
          />
        </div>

        <p v-if="error" class="text-critical text-sm">{{ error }}</p>

        <button
          type="submit"
          :disabled="loading"
          class="mt-2 bg-accent hover:bg-accent-dark text-white font-semibold text-sm rounded-[7px] py-2.5 disabled:opacity-60"
        >
          {{ loading ? 'Signing in…' : 'Sign in' }}
        </button>
      </form>
    </div>
  </div>
</template>
