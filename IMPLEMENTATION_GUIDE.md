# Bima Care — Implementation Guide

Concrete build plan for the architecture defined in `BIMA CARE.md`. This is an MVP-first plan: build Phase 1 end-to-end before touching Phase 2/3, and defer everything marked **V2** until the MVP is live and validated.

---

## 1. Tech Stack

| Concern | Choice | Why |
| :---- | :---- | :---- |
| Shared domain logic | Kotlin Multiplatform (KMP) | One set of models/business rules shared between backend services and mobile clients — avoids re-implementing FHIR mapping logic twice |
| Backend services | Kotlin + Ktor | Lightweight, same vendor/toolchain as KMP, no unnecessary framework weight for a microservices fleet |
| Database | PostgreSQL, one instance/schema per service | Matches the "each service owns its data" rule (BIMA CARE.md §7); JSONB for FHIR extensions (§6) |
| Messaging | Apache Kafka | Backbone for the event model in §8 |
| FHIR modeling/validation | HAPI FHIR (JVM) structures library | Mature, JVM-native, avoids hand-rolling FHIR R4 validation |
| IAM | Keycloak (OIDC/OAuth2) fronting a thin Ktor adapter | Don't hand-roll auth; Keycloak gives RBAC + OIDC out of the box, satisfies §10 |
| Containerization | Docker + Docker Compose (local), per §9 topology (prod) | |
| Web frontend (member, provider, admin) | Vue 3 (Composition API, `<script setup>`) + Pinia + Tailwind | One responsive web app for all three portals — matches how the reference implementation (afya-akili-digital.lovable.app) actually works, and matches the user's default web stack. Talks only to the API Gateway, never to individual services directly |
| **Deferred to V2** | Google Open Health Stack (Android FHIR SDK/FHIR Engine, Structured Data Capture, Workflow libraries); native KMM mobile app | A native mobile client was the original plan (see `BIMA CARE.md`) but is deferred — the web app covers members/providers/admins for MVP. Revisit native mobile only if offline-first or device-native features (camera-based ID scan, push notifications) become a hard requirement |

---

## 2. Repository Structure

Single monorepo, Gradle multi-module (Kotlin DSL):

```
bima-care/
├── settings.gradle.kts
├── build.gradle.kts
├── shared/                     # KMP modules, consumed by services + mobile client
│   ├── fhir-models/             # FHIR R4 resource wrappers used across services
│   ├── domain/                  # shared business rules (eligibility calc, claim state machine, etc.)
│   └── events/                  # Kafka event envelope schemas + serializers
├── services/
│   ├── iam-service/
│   ├── patient-service/
│   ├── provider-service/
│   ├── organization-service/
│   ├── eligibility-service/
│   ├── encounter-service/
│   ├── claims-service/
│   ├── payments-service/
│   ├── fhir-gateway/
│   ├── consent-service/
│   ├── document-service/
│   ├── audit-service/
│   └── notification-service/
├── gateway/                     # API Gateway (routing, authn/authz enforcement)
├── web/                         # Vue 3 SPA: member, provider, and admin portals — talks only to gateway/
├── infra/
│   ├── docker-compose.yml       # local dev: Postgres, Kafka, Keycloak
│   └── k8s/                     # per-cluster manifests (Cluster A/B/C from §9), added when needed
└── CLAUDE.md
```

Each `services/*` module is an independent Ktor application with its own `Dockerfile` and its own Postgres schema — never a shared DB module.

---

## 3. Service Scaffolding Convention

Every service module follows the same internal layout so any engineer (or Claude) can navigate a new service immediately:

```
<service>/
├── src/main/kotlin/care/bima/<service>/
│   ├── Application.kt        # Ktor entrypoint, DI wiring
│   ├── api/                  # HTTP routes, request/response DTOs
│   ├── domain/               # service-local business logic
│   ├── fhir/                 # mapping between domain model <-> FHIR resource (uses shared/fhir-models)
│   ├── db/                   # Exposed/JDBC tables + repositories (schema per §6 mapping tables)
│   └── events/               # Kafka producers/consumers for this service's topics
├── src/test/kotlin/...
└── Dockerfile
```

**Rule**: a service may only reach another service via its HTTP API or by consuming its Kafka topics — never via direct DB access, per §7.

---

## 4. Local Development Environment

`infra/docker-compose.yml` provides the shared dependencies every service needs locally:

- `postgres` (one container, one DB per service, created via init scripts)
- `kafka` + `kafka-ui` (KRaft mode, no Zookeeper needed)
- `keycloak` (preloaded realm with Admin/Provider/Insurer roles per §10)

Bring up the platform locally:

```bash
docker compose -f infra/docker-compose.yml up -d
./gradlew :services:patient-service:run
```

Each service reads its Postgres/Kafka/Keycloak connection details from environment variables (never hardcoded) — see global secrets policy.

---

## 5. FHIR Handling Strategy

- Use HAPI FHIR's R4 structures (`Patient`, `Coverage`, `Claim`, etc.) as the wire format at service boundaries and in the `FHIR Gateway`.
- Internally, each service persists its own relational schema (per the mapping tables in BIMA CARE.md §6) — FHIR resources are constructed on read and parsed on write, not stored as the primary representation.
- `shared/fhir-models` holds the KMP wrappers/converters so mapping logic isn't duplicated per service.
- `fhir-gateway` is the only service that speaks FHIR externally (to labs, EMRs, mobile apps per §5); internal service-to-service calls can use plain REST/JSON DTOs where FHIR fidelity isn't required.

**V2**: swap in Open Health Stack's FHIR Engine on the mobile client for local FHIR storage + sync, and Structured Data Capture for clinical forms.

---

## 6. Kafka Event Conventions

- One topic per lifecycle event, matching the Core Topics table in §8 (`patient.created`, `coverage.verified`, `claim.submitted`, etc.)
- Event envelope (defined once in `shared/events`): `{ eventId, eventType, resourceId, version, occurredAt, payload }`
- Events are immutable and append-only — never mutate or replay-edit a published event; corrections are new events.
- Every event consumption that changes state must also emit an `AuditEvent` to the Audit service (§10).

---

## 7. Database Conventions

- One Postgres database (or at minimum schema) per service — enforced at the infra level, not just by convention.
- Primary keys are UUIDs matching the FHIR resource `id` where applicable (§6 example: `Patient.id` → `patient_id`).
- FHIR `extension` fields map to a `JSONB` column, never new ad-hoc columns per extension.
- Explicit column selection in all queries; avoid `SELECT *` and N+1s (global SQL defaults apply).

---

## 8. Security & IAM

- Keycloak is the OIDC provider; the `iam-service` module is a thin adapter (token introspection, role sync into the domain model) — do not reimplement OAuth2 flows.
- API Gateway enforces authentication on every route; each downstream service still authorizes per-request (zero-trust — authenticate at the edge, authorize everywhere, per §10).
- `Consent` resources (Consent service) gate data-sharing decisions; check consent before returning any cross-service patient data.
- Every state-changing action logs an `AuditEvent`/`Provenance` entry — this is not optional per §10.

---

## 9. Web Frontend Conventions

- One Vue 3 SPA (`web/`) serves all three portals (member, provider, admin) with client-side routing per portal — not three separate apps. This matches the reference implementation's actual structure.
- The SPA talks **only** to the API Gateway (`gateway/`), never directly to `patient-service`/`organization-service`/etc. — the gateway is the single CORS-enabled, authenticated entry point.
- **Auth**: Keycloak direct-grant (Resource Owner Password) login against a dedicated **public** client (`bima-admin-web` — `publicClient: true`, no secret, since a browser SPA cannot keep a secret). The `bima-gateway` confidential client stays server-to-server only (used by `eligibility-service` for its own service-to-service calls) and must never be used from frontend code.
- Access token is held in a Pinia store (memory + `sessionStorage`, not `localStorage`, to limit XSS blast radius) and attached as `Authorization: Bearer` via an API client interceptor.
- Only build screens backed by a real API. If a reference screen (e.g. Policies, Claims, Reports) has no backing service yet, don't build it with mock data silently — either skip it until its Phase 2/3 backend exists, or clearly label it as a placeholder.

---

## 10. Phase-by-Phase Build Plan

### Phase 1 — Foundational (target: 6–8 weeks, per §11)
1. Stand up `infra/docker-compose.yml` (Postgres, Kafka, Keycloak) and the Gradle monorepo skeleton.
2. Build `shared/fhir-models` and `shared/events` first — every service depends on these.
3. `iam-service` + Keycloak realm config (Admin, Provider, Insurer roles).
4. `patient-service`, `provider-service`, `organization-service` — each with its FHIR mapping (§6) and CRUD API.
5. `eligibility-service` (Coverage/InsurancePlan resources).
6. `gateway` (API Gateway) wiring authn to Keycloak and routing to the above.

**Exit criteria**: a member can be registered, a provider/org can be onboarded, and coverage can be verified end-to-end through the gateway.

### Phase 2 — Transactional (target: 6 weeks)
1. `encounter-service` (Encounter, EpisodeOfCare) — depends on Patient, Provider, Organization.
2. `claims-service` (Claim, ClaimResponse) — depends on Coverage, Encounter.
3. `payments-service` (PaymentNotice, PaymentReconciliation) — depends on ClaimResponse.
4. Wire the full Kafka lifecycle: `claim.submitted → eligibility.checked → claim.adjudicated → payment.released` (§8 example).

**Exit criteria**: a claim can be submitted, adjudicated, and paid, driven entirely by Kafka events with audit entries at each step.

### Phase 3 — Interoperability (target: 4 weeks)
1. `fhir-gateway` — external-facing FHIR R4 API for labs/EMRs/mobile apps.
2. `consent-service` — Consent resource + enforcement hooks in the gateway.
3. `document-service` — DocumentReference/Binary storage (object storage-backed).
4. External integration testing (SHA-style workflows, partner EMRs).

**Exit criteria**: an external system can query/submit FHIR resources through the gateway, subject to consent checks, with full audit traceability.

---

## 11. CI/CD

- GitHub Actions: one workflow that builds/tests only the Gradle modules affected by a given diff (`./gradlew build` scoped via `--project-dir` or a path-filter action).
- Lint: `ktlint` + `detekt` on every PR, no merges with lint failures (global "never skip linting" rule applies).
- Each service builds its own Docker image on merge to main; images are tagged with the commit SHA.

---

## 12. Testing Strategy

- Unit tests: JUnit5 + MockK, colocated per service (`src/test/kotlin`).
- Integration tests: Testcontainers for Postgres/Kafka, one suite per service verifying its FHIR mapping + event emission.
- Contract tests: verify each service's Kafka event payloads match the `shared/events` envelope schema before merge.
- No service reaches "done" for a phase without integration tests covering its exit criteria above.

---

## 13. V2 Roadmap (explicitly out of MVP scope)

- Integrate Google's Open Health Stack: Android FHIR SDK / FHIR Engine (client-side FHIR storage + sync), Structured Data Capture Library (dynamic clinical forms), Workflow Library (care plan / task orchestration).
- Avro + Schema Registry for Kafka payloads (MVP uses JSON).
- Multi-cluster horizontal scaling per §9 topology (Cluster A/B/C split) once single-cluster load requires it.
- AI-driven diagnostics and advanced clinical decision support remain explicitly out of scope per §1 Non-Goals — do not add without a separate product decision.
