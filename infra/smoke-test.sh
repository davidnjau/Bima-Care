#!/usr/bin/env bash
# Phase 1 exit-criteria smoke test (IMPLEMENTATION_GUIDE.md §9):
# register a patient, onboard a provider + insurer org, verify coverage — all through the gateway.
#
# Prerequisites:
#   - infra/docker-compose.yml stack running (postgres, kafka, keycloak)
#   - iam-service, patient-service, provider-service, organization-service,
#     eligibility-service, and gateway all running locally (see IMPLEMENTATION_GUIDE.md §4)
#   - jq and curl installed
set -euo pipefail

KEYCLOAK_URL="${KEYCLOAK_URL:-http://localhost:8180}"
GATEWAY_URL="${GATEWAY_URL:-http://localhost:8080}"
REALM="bima-care"
CLIENT_ID="bima-gateway"
CLIENT_SECRET="${BIMA_GATEWAY_CLIENT_SECRET:-local-dev-only-changeme}"

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
PATIENT_ID=$(auth -X POST "$GATEWAY_URL/patients" -d '{
  "nationalId": "SMOKE-PAT-001",
  "firstName": "Asha",
  "lastName": "Otieno",
  "phone": "+254700000000",
  "gender": "FEMALE",
  "dob": "1990-05-01"
}' | jq -r '.id')
echo "    patient id: $PATIENT_ID"

echo "==> Onboarding provider"
PRACTITIONER_ID=$(auth -X POST "$GATEWAY_URL/practitioners" -d '{
  "licenseNumber": "SMOKE-LIC-001",
  "firstName": "John",
  "lastName": "Mwangi",
  "phone": "+254711111111",
  "specialty": "General Practice"
}' | jq -r '.id')
echo "    practitioner id: $PRACTITIONER_ID"

echo "==> Onboarding insurer organization"
INSURER_ID=$(auth -X POST "$GATEWAY_URL/organizations" -d '{
  "registrationNumber": "SMOKE-ORG-001",
  "name": "SHA Test Insurer",
  "type": "INSURER",
  "phone": "+254722222222",
  "address": "Nairobi, Kenya"
}' | jq -r '.id')
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
