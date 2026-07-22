# Frontend ↔ Backend Wiring — Live Implementation

How the Vue SPA (`web/`) actually talks to the running backend today, page by page. Companion to
`implementation-ui.md` (which covers the original dummy-data navigation plan) — this file is the
"what's real now" reference, updated as pages graduate from mock data to live API calls.

**Status: verified live** by full click-through, 22 Jul 2026 — every ✅ Live page below was loaded
in a real browser against the running backend (not just typechecked), including two real
mutations (claim submission, claim adjudication) and a cross-account data-scoping check.

---

## How the wiring works

- **Auth**: `web/src/stores/auth.ts` holds a Keycloak-issued JWT (memory + `sessionStorage`).
  `web/src/api/client.ts` is a single Axios instance (`apiClient`) with a request interceptor that
  attaches `Authorization: Bearer <token>` to every call, and a response interceptor that logs out
  and redirects to `/login` on any `401`.
- **Routing**: every API call goes through the internal `gateway` (`:8080`), never directly to a
  service port. The gateway proxies by path prefix (`/patients` → patient-service, `/claims` →
  claims-service, etc. — see `gateway/src/main/kotlin/care/bima/gateway/Application.kt`'s
  `upstreams()`).
- **API client files** (`web/src/api/*.ts`): one file per backend resource, each a thin
  `axios`-based wrapper (`patients.ts`, `organizations.ts`, `coverages.ts`, `eligibility.ts`,
  `claims.ts`, `payments.ts`, `iam.ts`). Views import functions from these, never call `apiClient`
  directly.
- **Demo identity shortcuts still in effect** (see `implementation-ui.md` / earlier session notes):
  - Member portal has no real login — `member.ensureDemoSession()` silently authenticates as
    `member-demo@bimacare.dev` just to satisfy the gateway's JWT requirement, then lets the user
    pick any patient from a dropdown ("Previewing as").
  - Provider identity (which practitioner/organization a logged-in provider represents) is
    resolved server-side in `claims-service` via `DemoProviderIdentityResolver`, keyed by
    `preferred_username` — not by any real Keycloak-to-Practitioner link.

---

## Page-by-page wiring map

| Portal | Route | Component | Backend calls | Status |
|---|---|---|---|---|
| Public | `/` | `HomeView.vue` | none | static |
| Public | `/login` | `LoginView.vue` | Keycloak direct-grant (`bima-admin-web`) | ✅ Live |
| Member | `/member/card` | `MyCardView.vue` | `listPatients()`, `getPatient(id)` | ✅ Live |
| Member | `/member/benefits` | `PolicyBenefitsView.vue` | `listCoverages()` | ✅ Live |
| Member | `/member/claims` | `ClaimsHistoryView.vue` | `listClaims({ patientId })` | ✅ Live *(wired this session)* |
| Member | `/member/dependents` | `DependentsView.vue` | none — `mocks/memberMocks.ts` | 🟡 Dummy (no backend) |
| Provider | `/provider/verify` | `VerifyMemberView.vue` | `listPatients()`, `verifyEligibility(id)` | ✅ Live |
| Provider | `/provider/claim` | `SubmitClaimView.vue` | `listPatients()`, `submitClaim(...)` | ✅ Live |
| Provider | `/provider/preauth` | `PreAuthorizationView.vue` | none — `mocks/providerMocks.ts` | 🟡 Dummy (no backend) |
| Provider | `/provider/history` | `TransactionHistoryView.vue` | `listClaims({ organizationId })` | ✅ Live |
| Admin | `/admin/dashboard` | `DashboardView.vue` | `listPatients()`, `listOrganizations()`, `listCoverages()` | ✅ Live |
| Admin | `/admin/members` | `MembersView.vue` | `listPatients()`, `createPatient(...)`, `listCoverages()` | ✅ Live |
| Admin | `/admin/providers` | `ProvidersView.vue` | `listOrganizations()`, `createOrganization(...)` | ✅ Live |
| Admin | `/admin/claims` | `ClaimsView.vue` | `listClaims()`, `adjudicateClaim(...)`, `listPatients()`, `listOrganizations()` | ✅ Live |
| Admin | `/admin/policies` | `PoliciesView.vue` | none — `mocks/adminMocks.ts` | 🟡 Dummy (no backend) |
| Admin | `/admin/reports` | `ReportsView.vue` | none — `mocks/adminMocks.ts` | 🟡 Dummy (no backend) |

The three 🟡 Dummy pages are unchanged from `implementation-ui.md` — no backend exists for
Pre-Authorization, Policies, Reports, or Dependents (all would be new architectural scope, not a
gap-fill — see that file's "Open decisions" section).

---

## Verification pass results (22 Jul 2026)

Full click-through as `admin@bimacare.dev`, `provider@bimacare.dev`, `provider2@bimacare.dev`, and
the member demo dropdown. 21 checks, all pass after one fix (below).

| Check | Result |
|---|---|
| Admin Dashboard | Real counts: 9 members, 9 providers, 6 active coverages |
| Admin Members / Providers | 9 real rows each |
| Admin Claims | Approved a real pending claim live — moved to Processed, success banner shown |
| Admin Policies / Reports | Render correctly with dummy data, as designed |
| Provider Verify Member | Real eligibility check, "Coverage confirmed" |
| Provider Submit Claim | Real claim submitted end-to-end (see issue below — initially failed) |
| Provider Transaction History (provider@) | 6 real rows, all Approved, Ksh 40,900 total |
| Provider Transaction History (provider2@) | Correctly empty — different facility, no claims yet |
| Provider Pre-Authorization | Renders correctly with dummy data, as designed |
| Member Card / Benefits / Claims | Real data for Asha Otieno (SMOKE-PAT-001) |
| Member Dependents | Renders correctly with dummy data, as designed |

No console errors on any page.

### Issue found and fixed: `encounter-service` hung on `POST /encounters`

Provider Submit Claim failed with a `500` — `claims-service`'s log showed
`HttpRequestTimeoutException` calling `encounter-service` at the claim-submission step (claims
create an encounter automatically as part of submission). Direct `GET /encounters` against
`encounter-service` also hung.

This is the **fifth instance this session** of the same root cause: a long-running local JVM
process degrading after the host machine sleeps/wakes repeatedly (its own logs showed repeated
`HikariPool - Thread starvation or clock leap detected` warnings, some over 20 minutes). `/health`
stayed responsive throughout, which is why it wasn't caught by a health check — only an actual
request exposed it. Fixed by restarting `encounter-service`; re-ran the failed check and it passed
in 300ms with no errors.

**Pattern going forward**: if a page that previously worked suddenly 500s or hangs, check the
relevant service's log for `HttpRequestTimeoutException` or `clock leap detected` before assuming
a code bug — restart that service first. This has now hit `provider-service`, `eligibility-service`,
`patient-service`, `encounter-service`, and (differently) Keycloak and Kafka. All are local dev
processes with no persistence/orchestration to auto-recover — there's no production analog to this
issue, since a real deployment would run these behind a process supervisor with health-check-based
restarts.

---

## Known gaps (unchanged from earlier sessions, still true)

- `PaymentNotice` isn't consent-gated in `fhir-gateway` (only reachable via `Claim` reference)
- `provider-service`/`organization-service` never emit Kafka events — no audit trail for onboarding
- FHIR search support in `fhir-gateway` is minimal (a handful of translated params only)
