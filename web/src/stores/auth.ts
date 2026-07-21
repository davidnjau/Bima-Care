import { defineStore } from 'pinia'
import { KEYCLOAK_CLIENT_ID, KEYCLOAK_REALM, KEYCLOAK_URL } from '../config'
import { getMe } from '../api/iam'

const STORAGE_KEY = 'bimacare.auth.token'

interface AuthState {
  token: string | null
  username: string | null
  roles: string[]
  // True when the current token came from the silent Member-preview demo
  // login, not a real sign-in. Route guards must never treat this as
  // access to /admin or /provider.
  isDemoSession: boolean
}

async function fetchToken(username: string, password: string): Promise<string> {
  const body = new URLSearchParams({
    grant_type: 'password',
    client_id: KEYCLOAK_CLIENT_ID,
    username,
    password,
  })

  const response = await fetch(`${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body,
  })

  if (!response.ok) {
    throw new Error('Incorrect username or password.')
  }

  const data = (await response.json()) as { access_token: string }
  return data.access_token
}

export const useAuthStore = defineStore('auth', {
  state: (): AuthState => ({
    token: sessionStorage.getItem(STORAGE_KEY),
    username: sessionStorage.getItem(`${STORAGE_KEY}.username`),
    roles: [],
    isDemoSession: sessionStorage.getItem(`${STORAGE_KEY}.demo`) === 'true',
  }),

  getters: {
    isAuthenticated: (state) => !!state.token,
    isProvider: (state) => state.roles.includes('Provider'),
    // Real, gate-worthy authentication — excludes the silent demo session.
    hasRealSession: (state) => !!state.token && !state.isDemoSession,
  },

  actions: {
    async login(username: string, password: string) {
      const token = await fetchToken(username, password)
      this.token = token
      this.username = username
      this.isDemoSession = false
      sessionStorage.setItem(STORAGE_KEY, token)
      sessionStorage.setItem(`${STORAGE_KEY}.username`, username)
      sessionStorage.setItem(`${STORAGE_KEY}.demo`, 'false')

      const identity = await getMe()
      this.roles = identity.roles
    },

    // Silently obtains a token for the Member-preview portal, which has no
    // real login UI but still needs a valid JWT to call the gateway. Never
    // overwrites a real, already-signed-in session.
    async ensureDemoSession() {
      if (this.isAuthenticated) return
      const token = await fetchToken('member-demo@bimacare.dev', 'local-dev-only-changeme')
      this.token = token
      this.username = 'member-demo@bimacare.dev'
      this.isDemoSession = true
      sessionStorage.setItem(STORAGE_KEY, token)
      sessionStorage.setItem(`${STORAGE_KEY}.username`, this.username)
      sessionStorage.setItem(`${STORAGE_KEY}.demo`, 'true')
    },

    logout() {
      this.token = null
      this.username = null
      this.roles = []
      this.isDemoSession = false
      sessionStorage.removeItem(STORAGE_KEY)
      sessionStorage.removeItem(`${STORAGE_KEY}.username`)
      sessionStorage.removeItem(`${STORAGE_KEY}.demo`)
    },
  },
})
