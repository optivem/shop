#!/usr/bin/env bash
set -euo pipefail

# Tear down what setup-gcp.sh creates: the GCP project (Terraform), the Neon project
# and the GitHub repo variables

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

# shellcheck source=terraform/neon.sh
source "$(dirname "$0")/terraform/neon.sh"
neon_check_auth || exit 1

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
PROJECT_ID=""
SYSTEM=""
if [[ -n "$STATE_PROJECT" ]]; then
  init_state_backend "${STATE_PROJECT}-tfstate" "$REPO" > /dev/null
  # -json, not -raw: with no outputs, -raw prints a warning to stdout and still exits 0
  OUTPUTS=$(terraform output -json 2> /dev/null) || OUTPUTS="{}"
  PROJECT_ID=$(jq -r '.project_id.value // empty' <<< "$OUTPUTS")
  SYSTEM=$(jq -r '.system_name.value // empty' <<< "$OUTPUTS")
fi
cd - > /dev/null

# Re-runs are idempotent: with no project in the state (already torn down), still clean
# up whatever is left in Neon and GitHub. Derive the name the way terraform/main.tf does.
if [[ -z "$SYSTEM" ]]; then
  SYSTEM="${SYSTEM_NAME:-$(echo "${REPO#*/}" | tr '[:upper:]' '[:lower:]' | sed 's/[^a-z0-9-]/-/g' | cut -c1-14 | sed 's/^-*//; s/-*$//')}"
fi

GH_VARIABLES=(GCP_PROJECT_ID GCP_REGION GCP_WORKLOAD_IDENTITY_PROVIDER GCP_SERVICE_ACCOUNT SYSTEM_NAME ACC_DOMAIN E2E_DOMAIN)
EXISTING_VARIABLES=$(gh variable list --json name -q '.[].name')
LEFTOVER_VARIABLES=()
for v in "${GH_VARIABLES[@]}"; do
  grep -qx "$v" <<< "$EXISTING_VARIABLES" && LEFTOVER_VARIABLES+=("$v")
done

neon_resolve_org || exit 1
NEON_PROJECT_ID=$(neon_find_project "$SYSTEM") || exit 1

echo "  GCP Project: ${PROJECT_ID:-(none in the Terraform state)}"
echo "  Neon project: ${NEON_PROJECT_ID:-(none named '$SYSTEM')}"
echo "  GitHub repo variables: ${LEFTOVER_VARIABLES[*]:-(none)}"
echo ""

if [[ -z "$PROJECT_ID" && -z "$NEON_PROJECT_ID" && ${#LEFTOVER_VARIABLES[@]} -eq 0 ]]; then
  echo "✅ Nothing to tear down — already clean."
  exit 0
fi

echo "⚠️  This will permanently delete:"
[[ -n "$PROJECT_ID" ]] && echo "     - GCP project '$PROJECT_ID' and ALL its resources"
[[ -n "$NEON_PROJECT_ID" ]] && echo "     - Neon project '$SYSTEM' ($NEON_PROJECT_ID) and ALL its databases"
[[ ${#LEFTOVER_VARIABLES[@]} -gt 0 ]] && echo "     - GitHub repo variables: ${LEFTOVER_VARIABLES[*]}"
echo ""
CONFIRM_VALUE="${PROJECT_ID:-$SYSTEM}"
if [[ -n "$PROJECT_ID" ]]; then
  read -rp "  Are you sure? Type the GCP project ID to confirm: " CONFIRM || CONFIRM=""
else
  read -rp "  Are you sure? Type the system name ($SYSTEM) to confirm: " CONFIRM || CONFIRM=""
fi

if [[ "$CONFIRM" != "$CONFIRM_VALUE" ]]; then
  echo "  Aborted."
  exit 0
fi

echo ""

echo "Step 1/3: Destroying infrastructure with Terraform..."
if [[ -n "$PROJECT_ID" ]]; then
  # Billing account (needed for terraform destroy)
  use_state_project_for_quota "$STATE_PROJECT"
  BILLING_ACCOUNT=$(gcloud billing accounts list --billing-project="$STATE_PROJECT" --format="value(name)" --limit=1) || {
    rc=$?
    echo "  ⚠️  'gcloud billing accounts list' failed (exit $rc) — see stderr above; continuing without billing account" >&2
    BILLING_ACCOUNT=""
  }

  # Terraform destroy
  cd "$(dirname "$0")/terraform"
  terraform destroy \
    -var="github_repo=$REPO" \
    -var="billing_account_id=$BILLING_ACCOUNT" \
    -var="system_name=$SYSTEM" \
    -auto-approve

  echo "  ✅ GCP infrastructure destroyed"
  cd - > /dev/null
else
  echo "  (skipped: no GCP project in the Terraform state)"
fi
echo ""

echo "Step 2/3: Deleting the Neon project..."
if [[ -n "$NEON_PROJECT_ID" ]]; then
  neon_cli projects delete "$NEON_PROJECT_ID" > /dev/null
  echo "  ✅ Neon project $NEON_PROJECT_ID deleted"
else
  echo "  (skipped: no Neon project named '$SYSTEM')"
fi
echo ""

# Remove GitHub variables
echo "Step 3/3: Removing GitHub repo variables..."
# Best-effort deletes — variable/secret may already be gone. Soft-fail but
# surface any non-"not found" stderr so auth/scope issues stay visible.
GH_DELETE_FAILED=0
delete_gh_thing() {
  local kind="$1"  # "variable" or "secret"
  local name="$2"
  local err rc
  err=$(gh "$kind" delete "$name" 2>&1) && return 0
  rc=$?
  if [[ "$err" == *"could not find"* || "$err" == *"not found"* || "$err" == *"HTTP 404"* ]]; then
    echo "  (skipped $kind $name: not present)"
  else
    echo "  ⚠️  failed to delete $kind $name (exit $rc): $err" >&2
    GH_DELETE_FAILED=1
  fi
}
for v in "${LEFTOVER_VARIABLES[@]}"; do
  delete_gh_thing variable "$v"
done
if [[ "$GH_DELETE_FAILED" == 1 ]]; then
  echo "  ⚠️  Some GitHub variables were not removed (see above)"
else
  echo "  ✅ GitHub variables removed"
fi
echo ""

echo "========================================="
if [[ "$GH_DELETE_FAILED" == 1 ]]; then
  echo " Teardown Complete — with warnings"
else
  echo " Teardown Complete!"
fi
echo "========================================="
echo ""
echo "  All GCP and Neon resources have been removed."
if [[ "$GH_DELETE_FAILED" == 1 ]]; then
  echo "  ⚠️  Some GitHub repo variables could not be removed — see the warnings above"
  echo "     and delete them with: gh variable delete <name>"
fi
echo "  The shared Terraform state project ($STATE_PROJECT) is kept for other systems."
echo ""
