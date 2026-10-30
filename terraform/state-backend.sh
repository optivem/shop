#!/usr/bin/env bash
# Sourced by setup-gcp.sh / teardown-gcp.sh — remote Terraform state in GCS.
#
# One state project per Google account, found by label (so every machine and every
# scaffolded system finds the same one), holding one versioned bucket; each repo
# keeps its state under its own prefix (owner/repo).

STATE_PROJECT_LABEL="purpose=terraform-state"

# Prints the state project ID, or nothing if none exists yet
find_state_project() {
  gcloud projects list --filter="labels.${STATE_PROJECT_LABEL}" --format="value(projectId)" --limit=1
}

# Runs before anything else in GCP: a clean account has no project for gcloud to bill API calls to,
# so even 'gcloud billing accounts list' needs this one. Creates the state project if missing and
# enables the Cloud Billing API (idempotent); prints progress to stderr, project ID to stdout
ensure_state_project() {
  local project
  project=$(find_state_project)
  if [[ -z "$project" ]]; then
    project="tfstate-$(openssl rand -hex 3)"
    echo "  Creating Terraform state project $project..." >&2
    gcloud projects create "$project" --name="Terraform state" --labels="$STATE_PROJECT_LABEL" --quiet >&2
  fi
  gcloud services enable cloudbilling.googleapis.com --project="$project" --quiet >&2
  echo "$project"
}

# Links billing, enables storage and creates the bucket where missing (idempotent); prints progress to stderr, bucket name to stdout
ensure_state_backend() {
  local project="$1" billing_account="$2" bucket
  if [[ "$(gcloud billing projects describe "$project" --billing-project="$project" --format="value(billingEnabled)")" != "True" ]]; then
    echo "  Linking billing to $project..." >&2
    gcloud billing projects link "$project" --billing-account="$billing_account" --billing-project="$project" --quiet > /dev/null
  fi
  gcloud services enable storage.googleapis.com --project="$project" --quiet >&2
  bucket="${project}-tfstate"
  if ! gcloud storage buckets describe "gs://${bucket}" --project="$project" &> /dev/null; then
    echo "  Creating state bucket gs://${bucket}..." >&2
    gcloud storage buckets create "gs://${bucket}" --project="$project" --location=us-central1 \
      --uniform-bucket-level-access --public-access-prevention --quiet >&2
    gcloud storage buckets update "gs://${bucket}" --versioning --quiet >&2
  fi
  echo "$bucket"
}

# Bills Terraform's API calls to the state project. Without this they go to the project behind the
# credentials' OAuth client, which isn't ours and may not have the API enabled (403 SERVICE_DISABLED).
# Every API the provider calls must be enabled here too, so run after billing is linked (some require it).
# Not via $(...): the exports must reach the caller's shell
use_state_project_for_quota() {
  local project="$1"
  gcloud services enable cloudbilling.googleapis.com cloudresourcemanager.googleapis.com serviceusage.googleapis.com \
    iam.googleapis.com artifactregistry.googleapis.com secretmanager.googleapis.com --project="$project" --quiet >&2
  export GOOGLE_BILLING_PROJECT="$project" USER_PROJECT_OVERRIDE=true
}

# terraform init against the bucket; migrates a pre-existing local state file into it
init_state_backend() {
  local bucket="$1" repo="$2"
  local args=(-input=false -backend-config="bucket=${bucket}" -backend-config="prefix=${repo}")
  if [[ -s terraform.tfstate ]]; then
    echo "  Migrating local terraform.tfstate to gs://${bucket}/${repo}..."
    terraform init "${args[@]}" -migrate-state -force-copy
  else
    terraform init "${args[@]}" -reconfigure
  fi
}
