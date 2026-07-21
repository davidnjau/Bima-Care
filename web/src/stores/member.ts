import { defineStore } from 'pinia'

export const useMemberStore = defineStore('member', {
  state: () => ({
    selectedPatientId: null as string | null,
  }),
  actions: {
    select(patientId: string) {
      this.selectedPatientId = patientId
    },
  },
})
