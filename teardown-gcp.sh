#!/usr/bin/env bash
set -euo pipefail

# Tear down Google Cloud infrastructure using Terraform
# Deletes the GCP project and removes GitHub repo variables/secrets

echo "========================================="
echo " GCP Infrastructure Teardown"
echo "========================================="
echo ""

# Check prerequisites
if ! command -v terraform &> /dev/null; then
  echo "❌ terraform CLI not found."
  exit 1
fi

if ! command -v gh &> /dev/null; then
  echo "❌ gh CLI not found."
  exit 1
fi

if ! command -v gcloud &> /dev/null; then
  echo "❌ gcloud CLI not found."
  exit 1
fi

# Terraform credentials: ADC if present, else the 'gcloud auth login' session token
if ! gcloud auth application-default print-access-token &> /dev/null; then
  GOOGLE_OAUTH_ACCESS_TOKEN=$(gcloud auth print-access-token) || { echo "❌ Run: gcloud auth login --no-launch-browser"; exit 1; }
  export GOOGLE_OAUTH_ACCESS_TOKEN
fi

REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')

# Find the project through the remote state (works from any clone/machine)
cd "$(dirname "$0")/terraform"
# shellcheck source=terraform/state-backend.sh
source ./state-backend.sh
STATE_PROJECT=$(find_state_project)
if [[ -z "$STATE_PROJECT" ]]; then
  echo "❌ No Terraform state project found (label $STATE_PROJECT_LABEL). Nothing to tear down."
  exit 1
fi
init_state_backend "${STATE_PROJECT}-tfstate" "$REPO" > /dev/null
PROJECT_ID=$(terraform output -raw project_id 2> /dev/null) || PROJECT_ID=""
cd - > /dev/null

if [[ -z "$PROJECT_ID" ]]; then
  echo "❌ No project in the Terraform state for $REPO. Nothing to tear down."
  exit 1
fi

echo "  GCP Project: $PROJECT_ID"
echo ""
echo "⚠️  This will permanently delete:"
echo "     - GCP project '$PROJECT_ID' and ALL its resources"
echo "     - GitHub repo variables (GCP_PROJECT_ID, GCP_REGION, etc.)"
echo "     - GitHub repo secret (NEON_DATABASE_URL)"
echo ""
read -rp "  Are you sure? Type the project ID to confirm: " CONFIRM

if [[ "$CONFIRM" != "$PROJECT_ID" ]]; then
  echo "  Aborted."
  exit 0
fi

echo ""

# Billing account (needed for terraform destroy)
BILLING_ACCOUNT=$(gcloud billing accounts list --format="value(name)" --limit=1) || {
  rc=$?
  echo "  ⚠️  'gcloud billing accounts list' failed (exit $rc) — see stderr above; continuing without billing account" >&2
  BILLING_ACCOUNT=""
}

# Terraform destroy
echo "Step 1/3: Destroying infrastructure with Terraform..."
cd "$(dirname "$0")/terraform"
terraform destroy \
  -var="github_repo=$REPO" \
  -var="billing_account_id=$BILLING_ACCOUNT" \
  -var="system_name=${SYSTEM_NAME:-}" \
  -auto-approve

echo "  ✅ GCP infrastructure destroyed"
echo ""

# Remove GitHub variables and secrets
echo "Step 2/3: Removing GitHub repo variables and secrets..."
# Best-effort deletes — variable/secret may already be gone. Soft-fail but
# surface any non-"not found" stderr so auth/scope issues stay visible.
delete_gh_thing() {
  local kind="$1"  # "variable" or "secret"
  local name="$2"
  local err rc
  err=$(gh "$kind" delete "$name" --yes 2>&1) && return 0
  rc=$?
  if [[ "$err" == *"could not find"* || "$err" == *"not found"* || "$err" == *"HTTP 404"* ]]; then
    echo "  (skipped $kind $name: not present)"
  else
    echo "  ⚠️  failed to delete $kind $name (exit $rc): $err" >&2
  fi
}
delete_gh_thing variable GCP_PROJECT_ID
delete_gh_thing variable GCP_REGION
delete_gh_thing variable GCP_WORKLOAD_IDENTITY_PROVIDER
delete_gh_thing variable GCP_SERVICE_ACCOUNT
delete_gh_thing variable SYSTEM_NAME
delete_gh_thing secret NEON_DATABASE_URL
echo "  ✅ GitHub variables and secrets removed"
echo ""

# Remove local config
echo "Step 3/3: Removing local config..."
cd - > /dev/null
rm -f deploy-config.yml
echo "  ✅ deploy-config.yml removed"
echo ""

echo "========================================="
echo " Teardown Complete!"
echo "========================================="
echo ""
echo "  All GCP resources have been removed."
echo "  The shared Terraform state project ($STATE_PROJECT) is kept for other systems."
echo "  Your GitHub Actions will fall back to Docker deployment."
echo ""
