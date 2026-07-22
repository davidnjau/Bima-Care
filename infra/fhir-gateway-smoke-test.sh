#!/usr/bin/env bash
# Phase 3 exit-criteria smoke test (IMPLEMENTATION_GUIDE.md §10 / ROADMAP.md #24):
# a mock external SHA/EMR partner queries and submits FHIR resources through fhir-gateway,
# gated by consent-service - denied before consent is granted, allowed after, denied again
# after revocation.
#
# Prerequisites:
#   - Everything infra/smoke-test.sh needs, PLUS consent-service, document-service, and
#     fhir-gateway running locally (see IMPLEMENTATION_GUIDE.md §4)
#   - The `bima-fhir-partner` confidential client must exist in Keycloak (client-credentials
#     grant) with the ExternalPartner realm role - see infra/keycloak/realm-export.json
#   - jq and curl installed
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
FHIR_GATEWAY_URL="${FHIR_GATEWAY_URL:-http://localhost:8091}"
REALM="bima-care"

RUN_ID="${SMOKE_RUN_ID:-$(date +%s)}"

ADMIN_USERNAME="${SMOKE_ADMIN_USERNAME:-admin@bimacare.dev}"
ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-local-dev-only-changeme}"
PARTNER_CLIENT_ID="${SMOKE_PARTNER_CLIENT_ID:-bima-fhir-partner}"
PARTNER_CLIENT_SECRET="${SMOKE_PARTNER_CLIENT_SECRET:-local-dev-only-changeme}"

jwt_claim() {
  # $1 = token, $2 = jq filter
  local payload
  payload=$(echo "$1" | cut -d. -f2 | tr '_-' '/+')
  case $(( ${#payload} % 4 )) in
    2) payload="${payload}==" ;;
    3) payload="${payload}=" ;;
  esac
  echo "$payload" | base64 -d 2>/dev/null | jq -r "$2"
}

echo "==> Logging in as $ADMIN_USERNAME"
ADMIN_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=bima-admin-web&username=$ADMIN_USERNAME&password=$ADMIN_PASSWORD" \
  | jq -r '.access_token')
as_admin() { curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" "$@"; }

echo "==> Logging in as external partner ($PARTNER_CLIENT_ID)"
PARTNER_TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=$PARTNER_CLIENT_ID&client_secret=$PARTNER_CLIENT_SECRET" \
  | jq -r '.access_token')
as_partner() { curl -s -H "Authorization: Bearer $PARTNER_TOKEN" -H "Content-Type: application/json" "$@"; }
GRANTEE_ID=$(jwt_claim "$PARTNER_TOKEN" '.sub')
echo "    partner grantee id: $GRANTEE_ID"

echo "==> Registering patient + coverage"
PATIENT_ID=$(as_admin -X POST "$GATEWAY_URL/patients" -d "{
  \"nationalId\": \"FHIRSMOKE-PAT-$RUN_ID\",
  \"firstName\": \"Fatuma\",
  \"lastName\": \"Hassan\",
  \"phone\": \"+254700000001\",
  \"gender\": \"FEMALE\",
  \"dob\": \"1988-02-10\"
}" | jq -r '.id')
INSURER_ID=$(as_admin -X POST "$GATEWAY_URL/organizations" -d "{
  \"registrationNumber\": \"FHIRSMOKE-ORG-$RUN_ID\",
  \"name\": \"SHA Test Insurer\",
  \"type\": \"INSURER\",
  \"phone\": \"+254722222223\",
  \"address\": \"Nairobi, Kenya\"
}" | jq -r '.id')
as_admin -X POST "$GATEWAY_URL/coverages" -d "{
  \"patientId\": \"$PATIENT_ID\",
  \"insurerId\": \"$INSURER_ID\",
  \"status\": \"ACTIVE\",
  \"startDate\": \"2026-01-01\",
  \"endDate\": \"2026-12-31\",
  \"planTier\": \"gold\"
}" >/dev/null
echo "    patient id: $PATIENT_ID"

echo "==> Partner reads Patient via fhir-gateway BEFORE consent (expect 403)"
STATUS=$(as_partner -o /dev/null -w '%{http_code}' "$FHIR_GATEWAY_URL/Patient/$PATIENT_ID")
if [[ "$STATUS" != "403" ]]; then
  echo "FAIL: expected 403 before consent, got $STATUS" >&2
  exit 1
fi
echo "PASS: denied without consent (403)"

echo "==> Admin grants consent to the partner"
CONSENT_ID=$(as_admin -X POST "$GATEWAY_URL/consents" -d "{
  \"patientId\": \"$PATIENT_ID\",
  \"granteeId\": \"$GRANTEE_ID\",
  \"scope\": \"read:*\"
}" | jq -r '.id')
echo "    consent id: $CONSENT_ID"

echo "==> Partner reads Patient via fhir-gateway AFTER consent (expect 200 + FHIR Patient)"
PATIENT_FHIR=$(as_partner "$FHIR_GATEWAY_URL/Patient/$PATIENT_ID")
RESOURCE_TYPE=$(echo "$PATIENT_FHIR" | jq -r '.resourceType')
if [[ "$RESOURCE_TYPE" != "Patient" ]]; then
  echo "FAIL: expected a FHIR Patient resource, got: $PATIENT_FHIR" >&2
  exit 1
fi
echo "PASS: read Patient after consent (resourceType=$RESOURCE_TYPE)"

echo "==> Partner reads Coverage via fhir-gateway (same patient, expect 200)"
COVERAGE_STATUS=$(as_partner -o /dev/null -w '%{http_code}' "$FHIR_GATEWAY_URL/Coverage?patient=$PATIENT_ID")
if [[ "$COVERAGE_STATUS" != "200" ]]; then
  echo "FAIL: expected 200 for Coverage search, got $COVERAGE_STATUS" >&2
  exit 1
fi
echo "PASS: Coverage search returned 200"

echo "==> Partner submits a Claim via fhir-gateway (FHIR Claim -> internal SubmitClaimRequest)"
CLAIM_FHIR=$(as_partner -X POST "$FHIR_GATEWAY_URL/Claim" -d "{
  \"resourceType\": \"Claim\",
  \"patient\": { \"reference\": \"Patient/$PATIENT_ID\" },
  \"item\": [ { \"productOrService\": { \"text\": \"Outpatient\" } } ],
  \"diagnosis\": [ { \"diagnosisCodeableConcept\": { \"text\": \"J45\" } } ],
  \"supportingInfo\": [ { \"sequence\": 1, \"valueString\": \"Asthma review via partner EMR\" } ],
  \"total\": { \"value\": 5200.00 }
}")
CLAIM_RESOURCE_TYPE=$(echo "$CLAIM_FHIR" | jq -r '.resourceType')
if [[ "$CLAIM_RESOURCE_TYPE" != "Claim" ]]; then
  echo "FAIL: expected a FHIR Claim resource back, got: $CLAIM_FHIR" >&2
  exit 1
fi
echo "PASS: submitted Claim via fhir-gateway (resourceType=$CLAIM_RESOURCE_TYPE)"

echo "==> Admin revokes consent"
as_admin -X POST "$GATEWAY_URL/consents/$CONSENT_ID/revoke" -d '{}' >/dev/null

echo "==> Partner reads Patient via fhir-gateway AFTER revocation (expect 403 again)"
STATUS_AFTER_REVOKE=$(as_partner -o /dev/null -w '%{http_code}' "$FHIR_GATEWAY_URL/Patient/$PATIENT_ID")
if [[ "$STATUS_AFTER_REVOKE" != "403" ]]; then
  echo "FAIL: expected 403 after revocation, got $STATUS_AFTER_REVOKE" >&2
  exit 1
fi
echo "PASS: denied again after revocation (403)"

echo ""
echo "ALL PASS: external partner was denied, granted, and re-denied access through fhir-gateway,"
echo "and successfully submitted a Claim - full Phase 3 consent-gated interoperability flow verified."
