#!/usr/bin/env bash
# Phase 1 exit-criteria smoke test (IMPLEMENTATION_GUIDE.md §9), plus a Phase 2 claim
# lifecycle check (ROADMAP.md #19): register a patient, onboard a provider + insurer org,
# verify coverage, then submit -> adjudicate a claim and confirm a payment is auto-released
# — all through the gateway.
#
# Prerequisites:
#   - infra/docker-compose.yml stack running (postgres, kafka, keycloak) - Kafka needs
#     KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 (single-broker cluster) or consumer groups
#     hang forever; this is set in docker-compose.yml, just don't run an older container.
#   - iam-service, patient-service, provider-service, organization-service,
#     eligibility-service, encounter-service, claims-service, payments-service, and gateway
#     all running locally (see IMPLEMENTATION_GUIDE.md §4)
#   - claims-service has DEMO_PROVIDER_IDENTITIES set, mapping provider@bimacare.dev to a
#     real, already-onboarded practitionerId/organizationId pair (see
#     care.bima.claims.identity.DemoProviderIdentityResolver) - this smoke test does not
#     create that pairing itself
#   - jq and curl installed
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
REALM="bima-care"
CLIENT_ID="bima-gateway"
CLIENT_SECRET="${BIMA_GATEWAY_CLIENT_SECRET:-local-dev-only-changeme}"

# Every identifier below is suffixed with this so the script is safely re-runnable -
# patient-service returns a raw 500 (not a handled 409) on a duplicate nationalId.
RUN_ID="${SMOKE_RUN_ID:-$(date +%s)}"

echo "==> Fetching access token from Keycloak"
TOKEN=$(curl -sf -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=client_credentials&client_id=$CLIENT_ID&client_secret=$CLIENT_SECRET" \
  | jq -r '.access_token')

if [[ -z "$TOKEN" || "$TOKEN" == "null" ]]; then
  echo "Failed to obtain access token" >&2
  exit 1
fi

auth() { curl -sf -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" "$@"; }

echo "==> Registering patient"
PATIENT_ID=$(auth -X POST "$GATEWAY_URL/patients" -d "{
  \"nationalId\": \"SMOKE-PAT-$RUN_ID\",
  \"firstName\": \"Asha\",
  \"lastName\": \"Otieno\",
  \"phone\": \"+254700000000\",
  \"gender\": \"FEMALE\",
  \"dob\": \"1990-05-01\"
}" | jq -r '.id')
echo "    patient id: $PATIENT_ID"

echo "==> Onboarding provider"
PRACTITIONER_ID=$(auth -X POST "$GATEWAY_URL/practitioners" -d "{
  \"licenseNumber\": \"SMOKE-LIC-$RUN_ID\",
  \"firstName\": \"John\",
  \"lastName\": \"Mwangi\",
  \"phone\": \"+254711111111\",
  \"specialty\": \"General Practice\"
}" | jq -r '.id')
echo "    practitioner id: $PRACTITIONER_ID"

echo "==> Onboarding insurer organization"
INSURER_ID=$(auth -X POST "$GATEWAY_URL/organizations" -d "{
  \"registrationNumber\": \"SMOKE-ORG-$RUN_ID\",
  \"name\": \"SHA Test Insurer\",
  \"type\": \"INSURER\",
  \"phone\": \"+254722222222\",
  \"address\": \"Nairobi, Kenya\"
}" | jq -r '.id')
echo "    insurer id: $INSURER_ID"

echo "==> Creating coverage"
COVERAGE_ID=$(auth -X POST "$GATEWAY_URL/coverages" -d "{
  \"patientId\": \"$PATIENT_ID\",
  \"insurerId\": \"$INSURER_ID\",
  \"status\": \"ACTIVE\",
  \"startDate\": \"2026-01-01\",
  \"endDate\": \"2026-12-31\",
  \"planTier\": \"gold\"
}" | jq -r '.id')
echo "    coverage id: $COVERAGE_ID"

echo "==> Verifying eligibility"
ELIGIBLE=$(auth "$GATEWAY_URL/coverages/verify/$PATIENT_ID" | jq -r '.eligible')

if [[ "$ELIGIBLE" == "true" ]]; then
  echo "PASS: patient $PATIENT_ID is eligible under coverage $COVERAGE_ID"
else
  echo "FAIL: expected eligible=true, got $ELIGIBLE" >&2
  exit 1
fi

# --- Phase 2: claim submission -> adjudication -> payment release (ROADMAP.md #19) ---

PROVIDER_USERNAME="${SMOKE_PROVIDER_USERNAME:-provider@bimacare.dev}"
PROVIDER_PASSWORD="${SMOKE_PROVIDER_PASSWORD:-local-dev-only-changeme}"
ADMIN_USERNAME="${SMOKE_ADMIN_USERNAME:-admin@bimacare.dev}"
ADMIN_PASSWORD="${SMOKE_ADMIN_PASSWORD:-local-dev-only-changeme}"
PUBLIC_CLIENT_ID="bima-admin-web"

user_token() {
  curl -sf -X POST "$KEYCLOAK_URL/realms/$REALM/protocol/openid-connect/token" \
    -H "Content-Type: application/x-www-form-urlencoded" \
    -d "grant_type=password&client_id=$PUBLIC_CLIENT_ID&username=$1&password=$2" \
    | jq -r '.access_token'
}

echo "==> Logging in as $PROVIDER_USERNAME"
PROVIDER_TOKEN=$(user_token "$PROVIDER_USERNAME" "$PROVIDER_PASSWORD")
as_provider() { curl -sf -H "Authorization: Bearer $PROVIDER_TOKEN" -H "Content-Type: application/json" "$@"; }

echo "==> Submitting claim for patient $PATIENT_ID"
CLAIM=$(as_provider -X POST "$GATEWAY_URL/claims" -d "{
  \"patientId\": \"$PATIENT_ID\",
  \"serviceType\": \"Outpatient\",
  \"diagnosisCode\": \"A09\",
  \"treatmentDetails\": \"Rehydration therapy\",
  \"amount\": \"8500.00\"
}")
CLAIM_ID=$(echo "$CLAIM" | jq -r '.id')
echo "    claim id: $CLAIM_ID"

echo "==> Logging in as $ADMIN_USERNAME"
ADMIN_TOKEN=$(user_token "$ADMIN_USERNAME" "$ADMIN_PASSWORD")
as_admin() { curl -sf -H "Authorization: Bearer $ADMIN_TOKEN" -H "Content-Type: application/json" "$@"; }

echo "==> Adjudicating claim (approve)"
ADJUDICATED_STATUS=$(as_admin -X POST "$GATEWAY_URL/claims/$CLAIM_ID/adjudicate" -d '{"decision":"APPROVED"}' \
  | jq -r '.status')

if [[ "$ADJUDICATED_STATUS" != "APPROVED" ]]; then
  echo "FAIL: expected claim status APPROVED, got $ADJUDICATED_STATUS" >&2
  exit 1
fi

echo "==> Waiting for payments-service to consume claim.adjudicated"
PAYMENT_FOUND="false"
for _ in $(seq 1 10); do
  sleep 1
  COUNT=$(as_admin "$GATEWAY_URL/payments?patientId=$PATIENT_ID" | jq --arg id "$CLAIM_ID" '[.[] | select(.claimId == $id)] | length')
  if [[ "$COUNT" -gt 0 ]]; then
    PAYMENT_FOUND="true"
    break
  fi
done

if [[ "$PAYMENT_FOUND" == "true" ]]; then
  echo "PASS: claim $CLAIM_ID was approved and a payment was released"
else
  echo "FAIL: no payment appeared for claim $CLAIM_ID within 10s" >&2
  exit 1
fi
