import { defineStore } from 'pinia'
import { createPatient, type CreatePatientRequest } from '../api/patients'

const STORAGE_KEY = 'bimacare.offlineQueue.patients'

export interface QueuedPatient extends CreatePatientRequest {
  id: string
  queuedAt: string
  syncError: string | null
}

function loadQueue(): QueuedPatient[] {
  try {
    return JSON.parse(localStorage.getItem(STORAGE_KEY) ?? '[]')
  } catch {
    return []
  }
}

// axios rejects with no `response` for a genuine network failure (offline, DNS, connection
// refused) - a real HTTP error (4xx/5xx) always has one. That's the signal used to decide
// whether a create-patient attempt should fall back to the offline queue.
export function isNetworkError(error: unknown): boolean {
  return typeof error === 'object' && error !== null && 'isAxiosError' in error && !(error as { response?: unknown }).response
}

export const useOfflineQueueStore = defineStore('offlineQueue', {
  state: () => ({
    pending: loadQueue(),
    syncing: false,
  }),

  getters: {
    pendingCount: (state) => state.pending.length,
  },

  actions: {
    persist() {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(this.pending))
    },

    enqueue(request: CreatePatientRequest): QueuedPatient {
      const queued: QueuedPatient = {
        nationalId: request.nationalId,
        firstName: request.firstName,
        lastName: request.lastName,
        phone: request.phone,
        gender: request.gender,
        dob: request.dob,
        id: request.id ?? crypto.randomUUID(),
        queuedAt: new Date().toISOString(),
        syncError: null,
      }
      this.pending.push(queued)
      this.persist()
      return queued
    },

    async syncAll() {
      if (this.syncing) return
      this.syncing = true
      try {
        for (const item of [...this.pending]) {
          try {
            await createPatient({
              id: item.id,
              nationalId: item.nationalId,
              firstName: item.firstName,
              lastName: item.lastName,
              phone: item.phone,
              gender: item.gender,
              dob: item.dob,
            })
            this.pending = this.pending.filter((p) => p.id !== item.id)
            this.persist()
          } catch (e) {
            const message = e instanceof Error ? e.message : 'Sync failed'
            this.pending = this.pending.map((p) => (p.id === item.id ? { ...p, syncError: message } : p))
            this.persist()
          }
        }
      } finally {
        this.syncing = false
      }
    },
  },
})
