#!/usr/bin/env bash
set -euo pipefail

# Setup Google Cloud infrastructure using Terraform
# Prerequisite: gcloud and gh CLIs authenticated
# Optional: SYSTEM_NAME=<4-14 chars> overrides the name derived from the repo (prefixes the GCP project, services, secrets, databases)
# Usage: ./setup-gcp.sh [--yes]   (--yes applies Terraform plans that change/destroy resources without asking)

AUTO_APPROVE=false
[[ "${1:-}" == "--yes" ]] && AUTO_APPROVE=true

echo "========================================="
echo " GCP Infrastructure Setup (Terraform)"
echo "========================================="
echo ""

# Print the install command for the detected OS (reproducible route: reopen the repo in the devcontainer)
install_hint() {
  local winget_id="$1" brew_pkg="$2" apt_note="$3"
  case "$(uname -s)" in
    MINGW*|MSYS*|CYGWIN*) echo "   Install: winget install --id $winget_id -e   (then restart the terminal)" ;;
    Darwin*)              echo "   Install: brew install $brew_pkg" ;;
    *)                    echo "   Install: $apt_note" ;;
  esac
  echo "   Or reopen the repo in the devcontainer, which has every tool pinned."
}

# Neon is driven by neonctl (pinned; npx fallback, so no global install is needed)
NEONCTL_VERSION=8.13.2
NEON_REGION=aws-us-east-2   # closest Neon region to us-central1
NEON_ROLE=neondb_owner

neon_cli() {
  if command -v neonctl &> /dev/null; then
    neonctl "$@" --no-analytics
  else
    npx -y "neonctl@${NEONCTL_VERSION}" "$@" --no-analytics
  fi
}

# Check prerequisites
echo "Checking prerequisites..."
PREREQ_OK=true

if ! command -v gcloud &> /dev/null; then
  echo "❌ gcloud CLI not found."
  install_hint Google.CloudSDK --cask\ google-cloud-sdk "https://cloud.google.com/sdk/docs/install"
  PREREQ_OK=false
else
  echo "  ✅ gcloud CLI found"
fi

if ! command -v gh &> /dev/null; then
  echo "❌ gh CLI not found. Install from: https://cli.github.com"
  PREREQ_OK=false
else
  echo "  ✅ gh CLI found"
fi

if ! command -v jq &> /dev/null; then
  echo "❌ jq not found. Install from: https://jqlang.org/download"
  PREREQ_OK=false
else
  echo "  ✅ jq found"
fi

if ! command -v terraform &> /dev/null; then
  echo "❌ terraform CLI not found."
  install_hint Hashicorp.Terraform hashicorp/tap/terraform "https://developer.hashicorp.com/terraform/install"
  PREREQ_OK=false
else
  echo "  ✅ terraform CLI found"
fi

gh_auth_err=$(gh auth status 2>&1) && gh_auth_ok=1 || gh_auth_ok=0
if (( gh_auth_ok == 0 )); then
  echo "❌ Not authenticated to GitHub. Run: gh auth login"
  echo "    gh auth status output: $gh_auth_err"
  PREREQ_OK=false
else
  echo "  ✅ GitHub authenticated"
fi

gcloud_active=$(gcloud auth list --filter=status:ACTIVE --format="value(account)") || {
  rc=$?
  echo "❌ 'gcloud auth list' failed (exit $rc) — see stderr above. Run: gcloud auth login"
  PREREQ_OK=false
  gcloud_active=""
}
if [[ -z "${gcloud_active:-}" ]]; then
  echo "❌ Not authenticated to Google Cloud. Run: gcloud auth login"
  PREREQ_OK=false
elif [[ -n "${gcloud_active:-}" ]]; then
  GCP_ACCOUNT=$(echo "$gcloud_active" | head -1)
  echo "  ✅ Google Cloud authenticated as: $GCP_ACCOUNT"
fi

if ! command -v neonctl &> /dev/null && ! command -v npx &> /dev/null; then
  echo "❌ neonctl not found (and no npx to run it). Install Node.js, or: npm i -g neonctl@${NEONCTL_VERSION}"
  PREREQ_OK=false
elif ! neon_cli me -o json &> /dev/null; then
  echo "❌ Not authenticated to Neon. Run: npx -y neonctl@${NEONCTL_VERSION} login"
  echo "    (or export NEON_API_KEY=<key> from Neon console → Account settings → API keys; no account yet: https://console.neon.tech)"
  PREREQ_OK=false
else
  echo "  ✅ Neon authenticated"
fi

# Terraform's google provider prefers Application Default Credentials; without them, hand it a token from
# the 'gcloud auth login' session (valid ~1h, enough for one apply). Avoids the separate ADC login, which
# org policies or the consent screen's per-scope checkboxes can block.
if [[ -n "${GCP_ACCOUNT:-}" ]]; then
  if gcloud auth application-default print-access-token &> /dev/null; then
    echo "  ✅ Terraform credentials: Application Default Credentials"
  elif GOOGLE_OAUTH_ACCESS_TOKEN=$(gcloud auth print-access-token 2> /dev/null); then
    export GOOGLE_OAUTH_ACCESS_TOKEN
    echo "  ✅ Terraform credentials: gcloud session token ($GCP_ACCOUNT)"
  else
    echo "❌ Could not get an access token for Terraform. Run: gcloud auth login --no-launch-browser"
    PREREQ_OK=false
  fi
fi

if [[ "$PREREQ_OK" != "true" ]]; then
  echo ""
  echo "❌ Prerequisites not met. Please fix above and try again."
  exit 1
fi

echo ""

# Get GitHub repo
REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')
echo "  📦 GitHub repo: $REPO"
echo ""

# Get or prompt for billing account
echo "Checking GCP billing account..."
BILLING_ACCOUNT=$(gcloud billing accounts list --format="value(name)" --limit=1) || {
  rc=$?
  echo "❌ 'gcloud billing accounts list' failed (exit $rc) — see stderr above" >&2
  exit "$rc"
}

if [[ -z "$BILLING_ACCOUNT" ]]; then
  echo "❌ No active billing account found."
  echo "   Set up billing at: https://console.cloud.google.com/billing"
  echo "   Then re-run this script."
  exit 1
fi

echo "  ✅ Billing account: $BILLING_ACCOUNT"
echo ""

# Initialize Terraform
echo "Initializing Terraform (remote state in GCS)..."
cd "$(dirname "$0")/terraform"
# shellcheck source=terraform/state-backend.sh
source ./state-backend.sh
STATE_BUCKET=$(ensure_state_backend "$BILLING_ACCOUNT")
echo "  ✅ State: gs://$STATE_BUCKET/$REPO"
init_state_backend "$STATE_BUCKET" "$REPO"

# Plan
echo ""
echo "Planning infrastructure..."
terraform plan -var="github_repo=$REPO" -var="billing_account_id=$BILLING_ACCOUNT" -var="system_name=${SYSTEM_NAME:-}" -out=tfplan

# Apply: additions go ahead; updates/deletes need confirmation (or --yes)
plan_json=$(terraform show -json tfplan)
RISKY=$(jq -r '.resource_changes[]? | select(.change.actions | index("update") or index("delete")) | "    \(.change.actions | join("/")): \(.address)"' <<< "$plan_json")
CREATES=$(jq '[.resource_changes[]? | select(.change.actions == ["create"])] | length' <<< "$plan_json")

if [[ -z "$RISKY" && "$CREATES" -eq 0 ]]; then
  echo ""
  echo "  ✅ Infrastructure already up to date"
elif [[ -n "$RISKY" && "$AUTO_APPROVE" != "true" ]]; then
  echo ""
  echo "⚠️  This plan CHANGES or DELETES existing resources:"
  echo "$RISKY"
  echo "Proceed with applying? (yes/no)"
  read -r PROCEED
  if [[ "$PROCEED" != "yes" ]]; then
    echo "Cancelled. Run './setup-gcp.sh' again to continue."
    rm -f tfplan
    exit 0
  fi
fi

if [[ -n "$RISKY" || "$CREATES" -gt 0 ]]; then
  echo ""
  echo "Applying infrastructure ($CREATES to add${RISKY:+, plus the changes above})..."
  terraform apply tfplan
fi
rm -f tfplan

# Get outputs
echo ""
echo "✅ Infrastructure ready"
echo ""
echo "Retrieving outputs..."
PROJECT_ID=$(terraform output -raw project_id)
REGION=$(terraform output -raw region)
WIF_PROVIDER=$(terraform output -raw workload_identity_provider)
SA_EMAIL=$(terraform output -raw service_account_email)
SYSTEM=$(terraform output -raw system_name)
DB_PREFIX="${SYSTEM//-/_}"

# Configure GitHub repo with variables
echo ""
echo "Configuring GitHub repository..."
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --body "$WIF_PROVIDER"
gh variable set GCP_SERVICE_ACCOUNT --body "$SA_EMAIL"
gh variable set SYSTEM_NAME --body "$SYSTEM"

echo "  ✅ GCP_PROJECT_ID = $PROJECT_ID"
echo "  ✅ GCP_REGION = $REGION"
echo "  ✅ GCP_WORKLOAD_IDENTITY_PROVIDER"
echo "  ✅ GCP_SERVICE_ACCOUNT = $SA_EMAIL"
echo "  ✅ SYSTEM_NAME = $SYSTEM"

# Neon: one project named after the system, one database per stack + Keycloak, all owned by the default role.
# Nothing is clicked in the console and credentials never touch Terraform state: neonctl → Secret Manager.
# neonctl list commands print either a bare array or {<key>: [...]}; normalize to an array
neon_items() { jq -c --arg k "$1" 'if type == "array" then . else (.[$k] // []) end'; }

# Sets db_json ({host, port, user, password}) for the direct (non-pooler) endpoint
ensure_neon() {
  local org_args=() orgs
  if [[ -n "${NEON_ORG_ID:-}" ]]; then
    org_args=(--org-id "$NEON_ORG_ID")
  else
    orgs=$(neon_cli orgs list -o json 2> /dev/null | neon_items organizations) || orgs="[]"
    if [[ "$(jq length <<< "$orgs")" -eq 1 ]]; then
      org_args=(--org-id "$(jq -r '.[0].id' <<< "$orgs")")
    elif [[ "$(jq length <<< "$orgs")" -gt 1 ]]; then
      echo "❌ Your Neon account has several organizations. Pick one and re-run:"
      jq -r '.[] | "     NEON_ORG_ID=\(.id) ./setup-gcp.sh    # \(.name)"' <<< "$orgs"
      exit 1
    fi
  fi

  local project_id matches
  matches=$(neon_cli projects list ${org_args[@]+"${org_args[@]}"} -o json | neon_items projects | jq -c --arg n "$SYSTEM" '[.[] | select(.name == $n)]')
  case "$(jq length <<< "$matches")" in
    0)
      echo "  Creating Neon project '$SYSTEM' ($NEON_REGION)..."
      project_id=$(neon_cli projects create --name "$SYSTEM" --region-id "$NEON_REGION" ${org_args[@]+"${org_args[@]}"} --no-secrets -o json \
        | jq -r '.project.id // .id')
      ;;
    1) project_id=$(jq -r '.[0].id' <<< "$matches") ;;
    *) echo "❌ Several Neon projects are named '$SYSTEM'; delete or rename the extras and re-run."; exit 1 ;;
  esac
  echo "  ✅ Neon project: $SYSTEM ($project_id)"

  local existing db
  existing=$(neon_cli databases list --project-id "$project_id" -o json | neon_items databases | jq -r '.[].name')
  for db in "${DB_PREFIX}_multitier_java_real" "${DB_PREFIX}_multitier_java_stub" "${DB_PREFIX}_keycloak"; do
    if grep -qx "$db" <<< "$existing"; then
      echo "  ✅ Database $db exists"
    else
      neon_cli databases create --project-id "$project_id" --name "$db" --owner-name "$NEON_ROLE" -o json > /dev/null
      echo "  ✅ Created database $db"
    fi
  done

  # Direct endpoint (Flyway and Keycloak need session features the pooler lacks); parse with node so
  # percent-encoded password characters are decoded
  local conn
  conn=$(neon_cli connection-string --project-id "$project_id" --role-name "$NEON_ROLE" \
    --database-name "${DB_PREFIX}_keycloak" | grep -E '^postgres(ql)?://' | tail -1)
  if [[ -z "$conn" ]]; then
    echo "❌ Could not read the Neon connection string for project $project_id."
    exit 1
  fi
  db_json=$(node -e 'const u = new URL(process.argv[1]); console.log(JSON.stringify({host: u.hostname, port: u.port || "5432",
    user: decodeURIComponent(u.username), password: decodeURIComponent(u.password)}))' "$conn")
}

# Secret Manager values for multitier-java (created empty by Terraform; values never go through Terraform state)
secret_has_version() {
  [[ -n "$(gcloud secrets versions list "$1" --project="$PROJECT_ID" --filter=state:ENABLED --limit=1 --format='value(name)')" ]]
}

if secret_has_version "$SYSTEM-multitier-java-acceptance-db" && secret_has_version "$SYSTEM-keycloak-db"; then
  echo ""
  echo "  ✅ Neon credentials already stored ($SYSTEM-multitier-java-acceptance-db, $SYSTEM-keycloak-db)"
else
  echo ""
  echo "Provisioning Neon (neonctl)..."
  ensure_neon
  for s in "$SYSTEM-multitier-java-acceptance-db" "$SYSTEM-keycloak-db"; do
    echo -n "$db_json" | gcloud secrets versions add "$s" --data-file=- --project="$PROJECT_ID" --quiet > /dev/null
    echo "  ✅ Stored $s"
  done
fi

if secret_has_version "$SYSTEM-keycloak-admin-password"; then
  echo "  ✅ Keycloak admin password already stored"
else
  read -rsp "Choose a Keycloak admin password (hidden, Enter = generate one): " KC_ADMIN_PASSWORD
  echo ""
  if [[ -z "$KC_ADMIN_PASSWORD" ]]; then
    KC_ADMIN_PASSWORD=$(openssl rand -base64 24)
    echo "  Generated. Read it back with: gcloud secrets versions access latest --secret=$SYSTEM-keycloak-admin-password"
  fi
  echo -n "$KC_ADMIN_PASSWORD" | gcloud secrets versions add "$SYSTEM-keycloak-admin-password" --data-file=- --project="$PROJECT_ID" --quiet > /dev/null
  echo "  ✅ Stored $SYSTEM-keycloak-admin-password"
fi

# GitHub secrets the cloud workflows need (not created by this script)
echo ""
MISSING_GH_SECRETS=()
repo_secrets=$(gh secret list --json name --jq '.[].name')
for s in GHCR_TOKEN WORKFLOW_TOKEN; do
  if grep -qx "$s" <<< "$repo_secrets"; then
    echo "  ✅ GitHub secret $s exists"
  else
    echo "  ⚠️  GitHub secret $s is missing"
    MISSING_GH_SECRETS+=("$s")
  fi
done

# Legacy single connection string, still read by the monolith/other cloud workflows
echo ""
echo "Optional (other variants only, not multitier-java): single Neon connection string."
echo "Leave blank to skip."
read -rp "Paste Neon connection string (or press Enter): " NEON_URL || true

if [[ -n "$NEON_URL" ]]; then
  echo -n "$NEON_URL" | gcloud secrets versions add db-connection-string --data-file=- --project="$PROJECT_ID" --quiet
  gh secret set NEON_DATABASE_URL --body "$NEON_URL"
  echo "  ✅ Database URL stored"
fi

# Write local config
cat > deploy-config.yml <<EOF
deployment-target: cloud-run
cloud-run:
  project-id: $PROJECT_ID
  region: $REGION
  service-account: $SA_EMAIL
EOF

echo "  ✅ Written deploy-config.yml"
echo ""

echo "========================================="
echo " Setup Complete!"
echo "========================================="
echo ""
echo "  GCP Project:  $PROJECT_ID"
echo "  Region:       $REGION"
echo "  Cost:         \$0/month (free tier)"
echo ""
if (( ${#MISSING_GH_SECRETS[@]} > 0 )); then
  echo "  ⚠️  MANUAL STEP before triggering the workflow — set the missing GitHub secrets:"
  for s in "${MISSING_GH_SECRETS[@]}"; do
    echo "    gh secret set $s"
  done
  echo "    (GHCR_TOKEN: PAT with read:packages; WORKFLOW_TOKEN: PAT with repo + workflow)"
  echo ""
fi
echo "  Next steps:"
echo "    - Trigger workflow: gh workflow run multitier-java-acceptance-stage-cloud.yml"
echo "    - To tear down:    terraform destroy -var=\"github_repo=$REPO\" -var=\"billing_account_id=$BILLING_ACCOUNT\""
echo ""
