# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Status

**Phase 0 (bootstrap) and Phase 1 (foundational backend) are built, and verified working.** Phase 2 (Encounter/Claims/Payments) is not started. The Vue web frontend (`web/`) covers all three portals — Admin, Provider, Member — each wired only to real backend endpoints; pages with no backing service (Policies, Claims, Reports, Submit Claim, Pre-Authorization, Transaction History, Claims History, Dependents) show an explicit "not available yet" panel rather than fabricated data. The Member portal has no real login (patients aren't linked to Keycloak accounts) — it's a clearly-labeled demo-mode preview that silently authenticates as a fixed `member-demo@bimacare.dev` account purely to satisfy the backend's blanket JWT requirement; see `IMPLEMENTATION_GUIDE.md` §9.

- `BIMA CARE.md` — full technical architecture spec (microservices landscape, FHIR resource ownership, DB schema mapping, Kafka event model, infra topology, delivery phases, pricing)
- `ONE-PAGE EXECUTIVE & DELIVERY SUMMARY.md` — non-technical summary for leadership
- `IMPLEMENTATION_GUIDE.md` — tech stack, repo layout, conventions, phase-by-phase build order
- `ROADMAP.md` — the working checklist; check it before assuming what's done vs. pending

### Building and running

```bash
./gradlew build                        # compile + detekt + ktlint + tests, all backend modules
docker compose -f infra/docker-compose.yml up -d   # Postgres :5434, Kafka :9092, Keycloak :8180
bash infra/smoke-test.sh               # end-to-end Phase 1 flow through the gateway
```

Each service is run via `java -jar services/<name>/build/libs/<name>-all.jar` with `DB_URL`/`DB_USER`/`DB_PASSWORD`/`KEYCLOAK_ISSUER`/`PORT` env vars (see `IMPLEMENTATION_GUIDE.md` §4). Local port map: gateway `:8080`, patient `:8086` (8081 was taken by another project's container on this dev machine), provider `:8082`, organization `:8083`, eligibility `:8084`, iam `:8085`.

## Tech Stack

- **Backend & shared logic**: Kotlin Multiplatform (KMP) — domain models and business logic shared between backend services (`shared/events`, `shared/fhir-models`, `shared/service-commons`)
- **Backend services**: Kotlin/Ktor microservices, one per bounded context (see Microservices Landscape, BIMA CARE.md §4)
- **Data**: PostgreSQL, one database per service — no cross-service DB access, only API/event references (§7)
- **Messaging**: Kafka for all inter-service state propagation (§8)
- **Containerization**: Docker / Docker Compose locally; per BIMA CARE.md §9 for the production cluster topology
- **FHIR**: HAPI FHIR (JVM) for server-side FHIR R4 resource modeling/validation in the MVP
- **Web frontend**: Vue 3 (Composition API) + Pinia + Tailwind — one SPA for member, provider, and admin portals, talking only to the API Gateway. See `IMPLEMENTATION_GUIDE.md` §9 for auth flow and conventions
- **Deferred to V2**: Google's Open Health Stack libraries (Android FHIR SDK / FHIR Engine, Structured Data Capture, Workflow); native KMM mobile app — do not introduce these during MVP work

See `IMPLEMENTATION_GUIDE.md` for repo structure and the phase-by-phase build order.

## Architectural Rules (from BIMA CARE.md — do not violate)

- Each microservice owns its own database; **no service queries another service's database directly** — only via API calls or Kafka events (§7)
- FHIR R4 is the canonical interoperability model; internal schemas may differ but must map to FHIR at the service boundary (§2, §6)
- All state changes propagate via Kafka events; events are immutable, versioned, and carry resource ID + metadata (§8)
- Security is zero-trust: OAuth2/OIDC, RBAC, consent-driven access via the FHIR `Consent` resource, full audit trail via `AuditEvent`/`Provenance` (§10)

## Delivery Phases (BIMA CARE.md §11)

1. **Phase 1 — Foundational**: IAM, Patient, Provider, Organization, Coverage/Eligibility, API Gateway
2. **Phase 2 — Transactional**: Encounter, Claims, Payments, Kafka orchestration
3. **Phase 3 — Interoperability**: FHIR Gateway, Consent, Documents, external integrations

Build in this order — later services depend on earlier ones (see FHIR Reference Interaction Matrix, §7). MVP scope excludes full EMR replacement, clinical decision support, and AI diagnostics (§1 Non-Goals).
