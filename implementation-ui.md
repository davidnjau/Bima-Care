# UI Implementation Plan — Page by Page

**Status: Phase 1 of this plan (navigation + dummy data) is done**, verified 21 Jul 2026 by
click-through with Playwright against the running dev server (admin/provider login via the
Keycloak demo users in `infra/keycloak/realm-export.json`, member portal in its existing demo
mode). No console errors; every route below renders with the dummy data described. Real
integration (swapping mocks for API calls) is the remaining work, gated on Phase 2 backend
services per `ROADMAP.md`.

> **Gotcha hit during verification**: the Reports page charts (`h-36`/`h-28` bar heights) rendered
> as invisible/zero-height at first — not a code bug. The Vite dev server had been running for 18+
> hours since a prior session and hadn't picked up newly-introduced Tailwind utility classes.
> Restarting `npm run dev` fixed it immediately. If a newly added page's Tailwind classes don't
> seem to apply, restart the dev server before debugging the component.

Source reference: https://afya-akili-digital.lovable.app/ (Lovable prototype, same domain — Kenyan
digital health insurance). Routes below were pulled from its compiled bundle and each page was
rendered to extract layout, fields, and sample data.

**Approach**: finish out *navigation and layout* for every page first, using hardcoded dummy data
matching the shapes below. No new backend calls in this pass. Once a page's route/shell/dummy data
is approved, swap the dummy data for a real API call — that is a separate, later step per page
(mostly blocked on Phase 2 — Claims service, see `ROADMAP.md`).

Legend for **Status**:
- ✅ **Live** — already wired to a real backend endpoint, do not touch in this pass
- 🧱 **Stub** — route + `StubPanel` exist, needs a real layout with dummy data
- ⛔ **Missing** — no route/view exists yet, needs to be created from scratch

| Portal | Route | Component | Status |
|---|---|---|---|
| Public | `/` | `views/HomeView.vue` | ✅ Live |
| Public | `/login` | `views/LoginView.vue` | ✅ Live |
| Member | `/member/card` | `views/member/MyCardView.vue` | ✅ Live |
| Member | `/member/benefits` | `views/member/PolicyBenefitsView.vue` | ✅ Live |
| Member | `/member/claims` | `views/member/ClaimsHistoryView.vue` | 🟡 Dummy data — done |
| Member | `/member/dependents` | `views/member/DependentsView.vue` | 🟡 Dummy data — done |
| Provider | `/provider/verify` | `views/provider/VerifyMemberView.vue` | ✅ Live |
| Provider | `/provider/claim` | `views/provider/SubmitClaimView.vue` | 🟡 Dummy data — done |
| Provider | `/provider/preauth` | `views/provider/PreAuthorizationView.vue` | 🟡 Dummy data — done |
| Provider | `/provider/history` | `views/provider/TransactionHistoryView.vue` | 🟡 Dummy data — done |
| Admin | `/admin/dashboard` | `views/admin/DashboardView.vue` | ✅ Live |
| Admin | `/admin/members` | `views/admin/MembersView.vue` | ✅ Live |
| Admin | `/admin/providers` | `views/admin/ProvidersView.vue` | ✅ Live |
| Admin | `/admin/policies` | `views/admin/PoliciesView.vue` | 🟡 Dummy data — done |
| Admin | `/admin/claims` | `views/admin/ClaimsView.vue` | 🟡 Dummy data — done |
| Admin | `/admin/reports` | `views/admin/ReportsView.vue` | 🟡 Dummy data — done |

🟡 = navigation/layout finished this pass, still backed by `web/src/mocks/*.ts` rather than a real
API — swap-in is tracked per-page in the sections below and gated on Phase 2 backend services.

Pages marked ✅ Live are excluded from the rest of this plan — they already work end-to-end against
the real `patient`/`provider`/`eligibility` services and shouldn't be regressed.

---

## Mock data convention

One file per portal, plain TS objects/arrays, no API calls:

```
web/src/mocks/memberMocks.ts
web/src/mocks/providerMocks.ts
web/src/mocks/adminMocks.ts
```

Each stub view imports its slice directly (e.g. `import { claims } from '../../mocks/memberMocks'`)
instead of rendering `StubPanel`. When a page graduates to real integration, only that view's
`<script setup>` changes (swap the mock import for an `api/*.ts` call) — templates and mock shapes
stay as the contract.

---

## Member Portal

### `/member/claims` — Claims History
**Replaces**: `StubPanel` in `ClaimsHistoryView.vue`

List of past claims, most recent first. No filters/search/pagination in the reference — keep it
that simple for this pass.

Columns: Provider · Service date · Status badge · Service type · Amount (Ksh) · Diagnosis

Dummy rows:
| Provider | Date | Status | Type | Amount | Diagnosis |
|---|---|---|---|---|---|
| Nairobi Hospital | 15 Mar 2024 | approved | Outpatient | Ksh 8,500 | Upper Respiratory Tract Infection |
| Mediheal Pharmacy — Westlands | 02 Apr 2024 | approved | Pharmacy | Ksh 3,200 | Prescription refill — Hypertension |
| Karen Hospital | 10 Feb 2024 | approved | Outpatient | Ksh 5,500 | Routine checkup |

Use `StatusChip` (already exists) for the status badge; map `approved` → active/green styling.

Later integration: `GET /claims?patientId=` once `claims-service` exists (Phase 2).

### `/member/dependents` — Dependents
**Replaces**: `StubPanel` in `DependentsView.vue`

Header: "My Dependents" with a count ("3 dependents"). Read-only cards — no add/edit/remove in the
reference (dependents are managed by the insurer, not self-service). Keep it read-only here too.

Dummy rows:
| Name | Relationship | Gender | DOB | Age | Member ID |
|---|---|---|---|---|---|
| Grace Kamau | Spouse | Female | 22 Mar 1988 | 38 | KIC-MEM-2024-001235 |
| Brian Kamau | Child | Male | 10 Sep 2015 | 10 | KIC-MEM-2024-001236 |
| Faith Kamau | Child | Female | 05 Apr 2018 | 8 | KIC-MEM-2024-001237 |

Footer note: "All dependents listed above are covered under your family policy. Contact your
insurer to add or remove dependents."

Later integration: `GET /patients/{id}/related-persons` once `patient-service` exposes it.

---

## Provider Portal

### `/provider/claim` — Submit Claim
**Replaces**: `StubPanel` in `SubmitClaimView.vue`

A real form (no submit handler yet — on submit, just show a success toast/banner and reset; don't
POST anywhere in this pass):

- Member ID (text)
- Service Type (select: Outpatient, Inpatient, Pharmacy, Laboratory & Diagnostics, Dental, Optical)
- Diagnosis / ICD-10 Code (text)
- Treatment Details (textarea)
- Claim Amount (KES) (number)
- Submit button: "Submit Claim"

Guidelines block below the form (static text):
- Claims must be submitted within 30 days of service
- Member eligibility verification is required before submission
- Accurate diagnosis and treatment information must be provided

Later integration: `POST /claims` once `claims-service` exists (Phase 2).

### `/provider/preauth` — Pre-Authorization
**Replaces**: `StubPanel` in `PreAuthorizationView.vue`

Form fields:
- Member ID (text)
- Service Type (same select as above)
- Diagnosis / ICD-10 Code (text)
- Treatment Plan (textarea)
- Expected Admission Date (date)
- Expected Discharge Date (date)
- Estimated Amount (KES) (number)
- Submit button: "Request Pre-Authorization"

Guidelines block:
- Pre-authorization is required for all inpatient admissions
- Request must be submitted at least 24 hours before admission (except emergencies)
- Include a detailed treatment plan and cost estimates

Later integration: `POST /preauthorizations` once `claims-service` exists (Phase 2).

### `/provider/history` — Transaction History
**Replaces**: `StubPanel` in `TransactionHistoryView.vue`

Summary strip above the table: 3 total claims · 2 approved · Ksh 26,700 total value.

Columns: Reference · Type (Pre-Auth/Claim) · Member · Service · Amount · Date · Status

Dummy rows:
| Reference | Type | Member | Service | Amount | Date | Status |
|---|---|---|---|---|---|---|
| PA-2024-000001 | Pre-Auth | James Kamau | Inpatient | Ksh 350,000 | 28/04/2024 | approved |
| CLM-2024-000003 | Claim | Grace Kamau | Laboratory | Ksh 15,000 | 20/04/2024 | pending |
| CLM-2024-000002 | Claim | James Kamau | Pharmacy | Ksh 3,200 | 02/04/2024 | approved |
| CLM-2024-000001 | Claim | James Kamau | Outpatient | Ksh 8,500 | 15/03/2024 | approved |

Later integration: `GET /claims?providerId=` + `GET /preauthorizations?providerId=`, merged client-side
or via a combined endpoint (TBD when `claims-service` is designed).

---

## Admin Portal — new pages

These three need a route added to `router/index.ts` under `/admin`, a nav entry in `AdminLayout.vue`,
and a new view file. Follow the existing `MembersView.vue`/`ProvidersView.vue` table pattern
(bg-white card, `border-line` table, `StatusChip` for status).

### `/admin/policies` — Policies
Table columns: Policy name · Policy number · Type (family/individual/corporate) · Premium (Ksh) ·
Members enrolled · Period (start–end) · Status (active/suspended/expired)

"Create Policy" button (no-op modal placeholder or disabled for this pass — no policy-write API yet).

Dummy rows:
| Policy | Number | Type | Premium | Members | Period | Status |
|---|---|---|---|---|---|---|
| Family Comprehensive Plus | KIC-2024-FAM-001234 | Family | Ksh 85,000 | 4 | Jan 2024–Dec 2024 | active |
| Individual Premium | KIC-2024-IND-002001 | Individual | Ksh 45,000 | 1 | Jan 2024–Dec 2024 | active |
| Corporate Gold | KIC-2024-COR-003001 | Corporate | Ksh 2,500,000 | 85 | Jan 2024–Dec 2024 | active |
| Individual Basic | KIC-2024-IND-003012 | Individual | Ksh 25,000 | 1 | Jan 2024–Dec 2024 | suspended |
| Family Essential | KIC-2023-FAM-000456 | Family | Ksh 55,000 | 3 | Jan 2023–Dec 2023 | expired |

Later integration: this is genuinely new backend surface — no `policy`/`coverage`-write endpoints
exist yet. Depends on `eligibility-service` growing a policy CRUD, or a dedicated model — flag for
scoping when Phase 2 planning happens.

### `/admin/claims` — Claims (management)
Two tabs: **Pending Review** (default) and **Processed**. Each pending row gets three actions:
Approve / Partial / Reject (no-op in this pass — just log to console or show a toast).

Columns: Claim ID · Member · Provider · Service type · Service date · Requested amount · Diagnosis ·
Treatment details · Status badge

Dummy rows (Pending Review tab):
| Claim ID | Member | Provider | Type | Date | Amount | Diagnosis |
|---|---|---|---|---|---|---|
| CLM-2024-000101 | Peter Ochieng | Nairobi Hospital | Inpatient | — | Ksh 125,000 | Pneumonia |
| CLM-2024-000102 | Mary Wanjiku | Karen Hospital | Outpatient | — | Ksh 28,000 | Diabetes |
| CLM-2024-000103 | John Maina | Mediheal Pharmacy — Westlands | Pharmacy | — | Ksh 8,500 | — |
| CLM-2024-000003 | Grace Kamau | Karen Hospital | Laboratory | 20/04/2024 | Ksh 15,000 | Routine checkup |

Later integration: `GET /claims?status=pending` + `POST /claims/{id}/adjudicate` once
`claims-service` exists (Phase 2 — this is the core admin adjudication workflow, §11).

### `/admin/reports` — Reports
Four static report cards, each with a description and a "Download" button (no-op — no export
pipeline yet):
1. Claims Summary Report — "Monthly claims overview with approval rates and amounts"
2. Member Utilization Report — "Member benefit usage and claims patterns"
3. Provider Performance Report — "Claims volume and costs by provider"
4. Financial Summary — "Premium collection vs claims payout analysis"

Plus two charts and a leaderboard, all static dummy data (use whatever charting approach the
Dashboard already uses, if any — otherwise simple static bar/line via CSS or a lightweight chart lib
already in `package.json`; don't add a new charting dependency just for this):
- **Claims Trend (6 months)**: Total Claims vs Approved, Jan–Jun
- **Premium Collection Trend (6 months)**: Ksh 0–14M scale
- **Top Providers by Claims Volume**: Nairobi Hospital, Karen Hospital, Aga Khan, MP Shah, Gertrude's

"Export All Reports" button at the top (no-op).

Later integration: aggregation endpoints don't exist on any service yet — likely a read-model built
on top of `claims-service` events post-Phase 2, out of scope until then.

---

## Out of scope for this pass

- `/admin/dashboard`, `/admin/members`, `/admin/providers`, `/member/card`, `/member/benefits`,
  `/provider/verify` — already live, don't touch.
- Any real write/submit behavior (claim submission, pre-auth request, policy create, claim
  adjudication, report export) — forms should render and validate client-side only; actions can
  show a toast/banner but must not call an API that doesn't exist yet.
- Home page portal-selection copy and public marketing content — matches already, no changes needed.

## Suggested build order

1. Admin nav + routes for Policies/Claims/Reports (scaffolding, no content yet)
2. `mocks/*.ts` files
3. Member: Claims History, Dependents
4. Provider: Submit Claim, Pre-Authorization, Transaction History
5. Admin: Policies, Claims, Reports
6. Manual click-through of every route in both authenticated portals to confirm nav highlighting,
   layout, and no console errors — then this doc gets checked off and Phase 2 integration work
   starts page-by-page against it.
