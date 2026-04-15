#!/usr/bin/env bash
set -euo pipefail

# Tear down Google Cloud Run deployment infrastructure
# Deletes the GCP project and removes GitHub repo variables/secrets

echo "========================================="
echo " Cloud Run Deployment Teardown"
echo "========================================="
echo ""

# Check prerequisites
if ! command -v gcloud &> /dev/null; then
  echo "❌ gcloud CLI not found."
  exit 1
fi

if ! command -v gh &> /dev/null; then
  echo "❌ gh CLI not found."
  exit 1
fi

# Read config
if [[ ! -f deploy-config.yml ]]; then
  echo "❌ deploy-config.yml not found. Nothing to tear down."
  exit 1
fi

PROJECT_ID=$(grep 'project-id:' deploy-config.yml | awk '{print $2}')

if [[ -z "$PROJECT_ID" ]]; then
  echo "❌ Could not read project-id from deploy-config.yml"
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

# Delete GCP project
echo "Step 1/3: Deleting GCP project..."
gcloud projects delete "$PROJECT_ID" --quiet 2>/dev/null && \
  echo "  ✅ Project '$PROJECT_ID' scheduled for deletion" || \
  echo "  ⚠️  Could not delete project (may already be deleted)"
echo ""

# Remove GitHub variables and secrets
echo "Step 2/3: Removing GitHub repo variables and secrets..."
gh variable delete GCP_PROJECT_ID --yes 2>/dev/null || true
gh variable delete GCP_REGION --yes 2>/dev/null || true
gh variable delete GCP_WORKLOAD_IDENTITY_PROVIDER --yes 2>/dev/null || true
gh variable delete GCP_SERVICE_ACCOUNT --yes 2>/dev/null || true
gh secret delete NEON_DATABASE_URL --yes 2>/dev/null || true
echo "  ✅ GitHub variables and secrets removed"
echo ""

# Remove local config
echo "Step 3/3: Removing local config..."
rm -f deploy-config.yml
echo "  ✅ deploy-config.yml removed"
echo ""

echo "========================================="
echo " Teardown Complete!"
echo "========================================="
echo ""
echo "  All Cloud Run resources have been removed."
echo "  Your GitHub Actions will fall back to Docker deployment."
echo ""
