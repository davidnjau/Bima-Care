# Bima Care — Roadmap

Step-by-step implementation walkthrough, derived from `IMPLEMENTATION_GUIDE.md`. Each phase has a gate that must pass before moving to the next — don't start Phase N+1 work with Phase N's gate still red.

---

## Phase 0 — Bootstrap (~1 week)

Nothing here is a "service" yet — just the scaffolding everything else depends on.

- [x] 1. Initialize the Gradle monorepo (Kotlin DSL), empty `settings.gradle.kts` with module placeholders.
- [x] 2. Stand up `infra/docker-compose.yml`: Postgres, Kafka (KRaft mode), Keycloak.
- [x] 3. Build `shared/events` — the Kafka envelope schema (`eventId, eventType, resourceId, version, occurredAt, payload`).
- [x] 4. Build `shared/fhir-models` — HAPI FHIR wired in, wrapper types for the resources in BIMA CARE.md §5 (Patient, Coverage, Claim, etc.).
- [x] 5. Set up CI skeleton: GitHub Actions workflow, ktlint + detekt gating, path-filtered builds.
- [x] 6. Configure Keycloak realm: Admin / Provider / Insurer roles, OIDC client for the future API Gateway.

**Gate to Phase 1**: `docker compose up` brings up all three infra deps cleanly; `./gradlew build` passes on the empty skeleton with CI green.

---

## Phase 1 — Foundational (~6–8 weeks)

- [x] 7. `iam-service` — thin Keycloak adapter (token introspection, role sync).
- [x] 8. `gateway` (API Gateway) — routes to nothing yet, but enforces authn against Keycloak.
- [x] 9. `patient-service` — Patient/RelatedPerson CRUD + FHIR mapping (§6 table) + `patient.created` event.
- [x] 10. `provider-service` — Practitioner/PractitionerRole CRUD + FHIR mapping.
- [x] 11. `organization-service` — Organization/Location CRUD + FHIR mapping.
- [x] 12. `eligibility-service` — Coverage/InsurancePlan CRUD, consumes patient/provider/org data via API (not DB), emits `coverage.verified`.
- [x] 13. Wire all four services behind the `gateway`.
- [x] 14. Integration test: register a patient → onboard a provider/org → verify coverage, end-to-end through the gateway, with a Keycloak-issued token.

**Gate to Phase 2**: the Phase 1 exit criteria in `IMPLEMENTATION_GUIDE.md` §9 passes in CI (Testcontainers-backed), not just manually.

---

## Phase 2 — Transactional (~6 weeks)

- [x] 15. `encounter-service` — Encounter/EpisodeOfCare, references Patient/Provider/Organization by ID (no direct DB joins across services).
- [x] 16. `claims-service` — Claim/ClaimResponse, consumes `coverage.verified` + Encounter data, emits `claim.submitted` / `claim.adjudicated`.
- [x] 17. `payments-service` — PaymentNotice/PaymentReconciliation, consumes `claim.adjudicated`, emits `payment.released`.
- [x] 18. Wire the full event chain: `claim.submitted → eligibility.checked → claim.adjudicated → payment.released`, each hop logging an `AuditEvent` (needs a minimal `audit-service` stub here, even though it's formally a Phase 3 service — payments/claims can't ship without an audit trail per §10).
- [x] 19. Integration test: submit a claim against a live encounter → adjudicate → release payment, verifying Kafka events and audit entries at each step.

**Gate to Phase 3**: a claim can go from submission to payment with zero manual intervention and a complete audit trail.

---

## Phase 3 — Interoperability (~4 weeks)

- [x] 20. `consent-service` — Consent resource CRUD + enforcement hook added to the `gateway` (deny cross-service reads without active consent).
- [x] 21. `document-service` — DocumentReference/Binary, object-storage backed.
- [x] 22. `audit-service` — promote the Phase 2 stub into the full AuditEvent/Provenance service, backfill any gaps.
- [x] 23. `fhir-gateway` — the only externally FHIR-speaking service; exposes the platform as a FHIR R4 API to labs/EMRs/partners.
- [x] 24. External integration test with a mock SHA/EMR client hitting `fhir-gateway`, subject to consent checks.

**Gate to "MVP done"**: an external system can query/submit FHIR resources through `fhir-gateway`, gated by Consent, fully audited — matching the Bottom Line in the executive summary.

---

## Phase 4 — V2 (not scheduled yet)

- [ ] Open Health Stack integration on the mobile client (FHIR Engine, Structured Data Capture, Workflow).
- [ ] Avro + Schema Registry for Kafka.
- [ ] Multi-cluster split (Cluster A/B/C per §9) once single-cluster load demands it.
