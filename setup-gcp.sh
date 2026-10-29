#!/usr/bin/env bash
set -euo pipefail

# Setup Google Cloud infrastructure using Terraform
# Prerequisite: gcloud and gh CLIs authenticated

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
if [[ -z "${gcloud_active:-}" ]] && [[ "$PREREQ_OK" == "true" ]]; then
  echo "❌ Not authenticated to Google Cloud. Run: gcloud auth login"
  PREREQ_OK=false
elif [[ -n "${gcloud_active:-}" ]]; then
  GCP_ACCOUNT=$(echo "$gcloud_active" | head -1)
  echo "  ✅ Google Cloud authenticated as: $GCP_ACCOUNT"
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
echo "Initializing Terraform..."
cd "$(dirname "$0")/terraform"
terraform init

# Plan
echo ""
echo "Planning infrastructure..."
terraform plan -var="github_repo=$REPO" -var="billing_account_id=$BILLING_ACCOUNT" -out=tfplan

# Apply
echo ""
echo "⚠️  Review the plan above. Proceed with applying? (yes/no)"
read -r PROCEED

if [[ "$PROCEED" != "yes" ]]; then
  echo "Cancelled. Run './setup-gcp.sh' again to continue."
  rm -f tfplan
  exit 0
fi

echo ""
echo "Applying infrastructure..."
terraform apply tfplan
rm -f tfplan

# Get outputs
echo ""
echo "✅ Infrastructure created!"
echo ""
echo "Retrieving outputs..."
PROJECT_ID=$(terraform output -raw project_id)
REGION=$(terraform output -raw region)
WIF_PROVIDER=$(terraform output -raw workload_identity_provider)
SA_EMAIL=$(terraform output -raw service_account_email)

# Configure GitHub repo with variables
echo ""
echo "Configuring GitHub repository..."
gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --body "$WIF_PROVIDER"
gh variable set GCP_SERVICE_ACCOUNT --body "$SA_EMAIL"

echo "  ✅ GCP_PROJECT_ID = $PROJECT_ID"
echo "  ✅ GCP_REGION = $REGION"
echo "  ✅ GCP_WORKLOAD_IDENTITY_PROVIDER"
echo "  ✅ GCP_SERVICE_ACCOUNT = $SA_EMAIL"

# Prompt for optional database URL
echo ""
echo "Optional: Database connection (Neon, Cloud SQL, etc.)"
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
echo "  Next steps:"
echo "    - Trigger workflow: gh workflow run monolith-typescript-acceptance-stage-cloud.yml"
echo "    - To tear down:    terraform destroy -var=\"github_repo=$REPO\" -var=\"billing_account_id=$BILLING_ACCOUNT\""
echo ""
