#!/usr/bin/env bash
set -euo pipefail

# Maps the permanent acceptance (stub) and E2E (real) environments of the reference variant
# (multitier-java) to custom domains, and sets the repo variables the acceptance stage reads.
# See docs/environments.md.
#
#   ACC_DOMAIN=shop-acc.optivem.com E2E_DOMAIN=shop-e2e.optivem.com ./setup-custom-domains.sh
#
# setup-gcp.sh calls this when ACC_DOMAIN or E2E_DOMAIN is set; it can also run on its own.
# Either domain may be left unset to skip it. Idempotent: re-run it until both say "ready".
#
# Runs as you, not as the CI deployer: Cloud Run only maps a domain for a verified owner of it
# (Search Console), and the deployer service account isn't one. The mapping is made once and
# survives every redeploy, because each acceptance run deploys a new revision of the same service.
#
# Prerequisites (one-time, by the domain owner):
#   - the parent domain verified in Search Console for your gcloud account (gcloud domains list-user-verified)
#   - a CNAME <domain> -> ghs.googlehosted.com
#   - one cloud acceptance run of multitier-java, so the services to map exist
#
# The repo variable for a domain is set only once its mapping is Ready (certificate issued), so the
# acceptance stage never targets a domain that doesn't serve yet. Optional overrides: PROJECT_ID,
# REGION, SYSTEM (default: the GCP_PROJECT_ID, GCP_REGION and SYSTEM_NAME repo variables).

PROJECT_ID="${PROJECT_ID:-$(gh variable get GCP_PROJECT_ID)}"
REGION="${REGION:-$(gh variable get GCP_REGION)}"
SYSTEM="${SYSTEM:-$(gh variable get SYSTEM_NAME)}"
PENDING=0

# Service names must match _acceptance-stage.yml (deploy-stack): <system>-<architecture>-<language>-<stack>-frontend
ensure_mapping() {
  local var="$1" domain="$2" service="$3" mapped ready
  if [[ -z "$domain" ]]; then
    echo "  (skipped $var: not set)"
    return 0
  fi
  if ! gcloud run services describe "$service" --region "$REGION" --project "$PROJECT_ID" --format='value(metadata.name)' &> /dev/null; then
    echo "  ⏳ $domain: service $service doesn't exist yet."
    echo "     Run the cloud acceptance stage once (gh workflow run multitier-java-acceptance-stage.yml), then re-run this script."
    PENDING=1
    return 0
  fi
  mapped=$(gcloud beta run domain-mappings describe --domain "$domain" --region "$REGION" --project "$PROJECT_ID" \
    --format='value(spec.routeName)' 2> /dev/null || true)
  if [[ -z "$mapped" ]]; then
    echo "  Creating domain mapping $domain -> $service..."
    if ! gcloud beta run domain-mappings create --domain "$domain" --service "$service" \
        --region "$REGION" --project "$PROJECT_ID" --quiet > /dev/null; then
      echo "  ❌ Could not map $domain. Cloud Run needs you to be a verified owner of the domain:"
      echo "     verify its parent domain at https://search.google.com/search-console for $(gcloud config get-value account 2> /dev/null),"
      echo "     check with 'gcloud domains list-user-verified --project=$PROJECT_ID', then re-run this script."
      exit 1
    fi
  elif [[ "$mapped" != "$service" ]]; then
    echo "  ❌ $domain is already mapped to $mapped, not $service. Fix or remove that mapping by hand:"
    echo "     gcloud beta run domain-mappings describe --domain $domain --region $REGION --project $PROJECT_ID"
    exit 1
  fi
  ready=$(gcloud beta run domain-mappings describe --domain "$domain" --region "$REGION" --project "$PROJECT_ID" \
    --format=json 2> /dev/null | jq -r '.status.conditions[]? | select(.type == "Ready") | .status' || true)
  if [[ "$ready" == "True" ]]; then
    gh variable set "$var" --body "$domain"
    echo "  ✅ $domain -> $service (ready); $var = $domain"
  else
    echo "  ⏳ $domain -> $service: certificate not issued yet (usually 15-60 min after the CNAME resolves)."
    echo "     DNS needed: $domain CNAME ghs.googlehosted.com. Re-run this script later to set $var."
    PENDING=1
  fi
}

echo "Mapping custom domains (Cloud Run domain mappings, project $PROJECT_ID, $REGION)..."
ensure_mapping ACC_DOMAIN "${ACC_DOMAIN:-}" "${SYSTEM}-multitier-java-stub-frontend"
ensure_mapping E2E_DOMAIN "${E2E_DOMAIN:-}" "${SYSTEM}-multitier-java-real-frontend"
if (( PENDING )); then
  echo "  Not finished yet; re-run ./setup-custom-domains.sh (same variables) to complete it."
fi
