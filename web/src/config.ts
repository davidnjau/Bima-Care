export const KEYCLOAK_URL = import.meta.env.VITE_KEYCLOAK_URL || 'http://localhost:8180'
export const KEYCLOAK_REALM = import.meta.env.VITE_KEYCLOAK_REALM || 'bima-care'
export const KEYCLOAK_CLIENT_ID = import.meta.env.VITE_KEYCLOAK_CLIENT_ID || 'bima-admin-web'
export const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080'

// Mirrors claims-service's DemoProviderIdentityResolver (DEMO_PROVIDER_IDENTITIES env var) -
// the frontend has no /me lookup for a provider's organizationId yet (Provider Keycloak users
// aren't linked to a Practitioner/Organization record anywhere), so Transaction History uses
// this hardcoded map to scope claims to "this facility" per demo account.
export const DEMO_PROVIDER_ORGANIZATION_ID: Record<string, string> = {
  'provider@bimacare.dev': 'fd7cd0f2-0bc9-4e3a-95cf-c28e60b2b8cc',
  'provider2@bimacare.dev': 'cdd1d33a-8464-442b-87f9-577b469c2a5e',
}
