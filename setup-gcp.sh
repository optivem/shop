#!/usr/bin/env bash
set -euo pipefail

# Setup Google Cloud Run deployment infrastructure
# Prerequisites: gcloud CLI, gh CLI, authenticated to both

REGION="${1:-us-central1}"

echo "========================================="
echo " Cloud Run Deployment Setup"
echo "========================================="
echo ""

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v gcloud &> /dev/null; then
  echo "❌ gcloud CLI not found. Install it from: https://cloud.google.com/sdk/docs/install"
  exit 1
fi
echo "  ✅ gcloud CLI found"

if ! command -v gh &> /dev/null; then
  echo "❌ gh CLI not found. Install it from: https://cli.github.com"
  exit 1
fi
echo "  ✅ gh CLI found"

if ! gh auth status &> /dev/null; then
  echo "❌ Not authenticated to GitHub. Run: gh auth login"
  exit 1
fi
echo "  ✅ GitHub authenticated"

REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner')
echo "  📦 GitHub repo: $REPO"
echo ""

# Step 1: Authenticate to Google Cloud
echo "Step 1/7: Authenticating to Google Cloud..."
if ! gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null | head -1 | grep -q .; then
  gcloud auth login
fi
ACCOUNT=$(gcloud auth list --filter=status:ACTIVE --format="value(account)" 2>/dev/null | head -1)
echo "  ✅ Authenticated as: $ACCOUNT"
echo ""

# Step 2: Create GCP project
echo "Step 2/7: Creating GCP project..."
SUFFIX=$(openssl rand -hex 3)
REPO_SHORT=$(echo "$REPO" | sed 's|.*/||' | tr '[:upper:]' '[:lower:]' | tr -cd 'a-z0-9-' | cut -c1-20)
PROJECT_ID="${REPO_SHORT}-${SUFFIX}"

gcloud projects create "$PROJECT_ID" --name="$REPO_SHORT" --quiet
gcloud config set project "$PROJECT_ID" --quiet

# Link billing
BILLING_ACCOUNT=$(gcloud billing accounts list --format="value(name)" --limit=1 2>/dev/null || true)
if [[ -z "$BILLING_ACCOUNT" ]]; then
  echo "❌ No billing account found. Set up billing at: https://console.cloud.google.com/billing"
  echo "   Then re-run this script."
  gcloud projects delete "$PROJECT_ID" --quiet
  exit 1
fi
gcloud billing projects link "$PROJECT_ID" --billing-account="$BILLING_ACCOUNT" --quiet

echo "  ✅ Project created: $PROJECT_ID"
echo "  ✅ Billing linked: $BILLING_ACCOUNT"
echo ""

# Step 3: Enable APIs
echo "Step 3/7: Enabling APIs..."
gcloud services enable \
  run.googleapis.com \
  artifactregistry.googleapis.com \
  secretmanager.googleapis.com \
  iamcredentials.googleapis.com \
  --quiet

echo "  ✅ Cloud Run API enabled"
echo "  ✅ Artifact Registry API enabled"
echo "  ✅ Secret Manager API enabled"
echo "  ✅ IAM Credentials API enabled"
echo ""

# Step 4: Create Artifact Registry repository
echo "Step 4/7: Creating Artifact Registry repository..."
gcloud artifacts repositories create app-images \
  --repository-format=docker \
  --location="$REGION" \
  --quiet 2>/dev/null || echo "  (repository already exists)"

echo "  ✅ Repository 'app-images' ready in $REGION"
echo ""

# Step 5: Set up Workload Identity Federation
echo "Step 5/7: Setting up Workload Identity Federation..."

PROJECT_NUMBER=$(gcloud projects describe "$PROJECT_ID" --format='value(projectNumber)')

# Create Workload Identity Pool
gcloud iam workload-identity-pools create github-actions \
  --location=global \
  --display-name="GitHub Actions" \
  --quiet

# Create OIDC Provider
gcloud iam workload-identity-pools providers create-oidc github \
  --location=global \
  --workload-identity-pool=github-actions \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" \
  --attribute-condition="assertion.repository=='$REPO'" \
  --quiet

# Create Service Account
gcloud iam service-accounts create github-deployer \
  --display-name="GitHub Actions Deployer" \
  --quiet

SA_EMAIL="github-deployer@${PROJECT_ID}.iam.gserviceaccount.com"
WIF_MEMBER="principalSet://iam.googleapis.com/projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-actions/attribute.repository/${REPO}"

# Grant roles
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/run.admin" --quiet
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/iam.serviceAccountUser" --quiet
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/artifactregistry.writer" --quiet
gcloud projects add-iam-policy-binding "$PROJECT_ID" \
  --member="$WIF_MEMBER" --role="roles/secretmanager.secretAccessor" --quiet

gcloud iam service-accounts add-iam-policy-binding "$SA_EMAIL" \
  --member="$WIF_MEMBER" \
  --role="roles/iam.workloadIdentityUser" \
  --quiet

WIF_PROVIDER="projects/${PROJECT_NUMBER}/locations/global/workloadIdentityPools/github-actions/providers/github"

echo "  ✅ Workload Identity Pool created"
echo "  ✅ OIDC Provider linked to: $REPO"
echo "  ✅ Service Account: $SA_EMAIL"
echo "  ✅ Roles granted: run.admin, iam.serviceAccountUser, artifactregistry.writer, secretmanager.secretAccessor"
echo ""

# Step 6: Set up database
echo "Step 6/7: Setting up database..."
echo ""
echo "  This project uses Neon for free PostgreSQL hosting."
echo "  If you don't have a Neon account yet, create one at: https://neon.tech"
echo "  Then create a project and copy the connection string."
echo ""
read -rp "  Paste your Neon connection string: " NEON_URL

if [[ -z "$NEON_URL" ]]; then
  echo "  ⚠️  No connection string provided. You can add it later with:"
  echo "     echo -n 'YOUR_URL' | gcloud secrets create db-connection-string --data-file=- --project=$PROJECT_ID"
  echo "     gh secret set NEON_DATABASE_URL --body 'YOUR_URL'"
else
  echo -n "$NEON_URL" | gcloud secrets create db-connection-string --data-file=- --quiet
  echo "  ✅ Database URL stored in Secret Manager"
fi
echo ""

# Step 7: Configure GitHub repo
echo "Step 7/7: Configuring GitHub repository..."

gh variable set GCP_PROJECT_ID --body "$PROJECT_ID"
gh variable set GCP_REGION --body "$REGION"
gh variable set GCP_WORKLOAD_IDENTITY_PROVIDER --body "$WIF_PROVIDER"
gh variable set GCP_SERVICE_ACCOUNT --body "$SA_EMAIL"

if [[ -n "${NEON_URL:-}" ]]; then
  gh secret set NEON_DATABASE_URL --body "$NEON_URL"
  echo "  ✅ Secret set: NEON_DATABASE_URL"
fi

echo "  ✅ Variable set: GCP_PROJECT_ID = $PROJECT_ID"
echo "  ✅ Variable set: GCP_REGION = $REGION"
echo "  ✅ Variable set: GCP_WORKLOAD_IDENTITY_PROVIDER"
echo "  ✅ Variable set: GCP_SERVICE_ACCOUNT"
echo ""

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
echo "  To deploy, trigger your QA stage workflow."
echo "  To tear down, run: ./teardown-gcp.sh"
echo ""
