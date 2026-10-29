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

# Creates the state project + bucket if missing (idempotent); prints progress to stderr, bucket name to stdout
ensure_state_backend() {
  local billing_account="$1" project bucket
  project=$(find_state_project)
  if [[ -z "$project" ]]; then
    project="tfstate-$(openssl rand -hex 3)"
    echo "  Creating Terraform state project $project..." >&2
    gcloud projects create "$project" --name="Terraform state" --labels="$STATE_PROJECT_LABEL" --quiet >&2
    gcloud billing projects link "$project" --billing-account="$billing_account" --quiet > /dev/null
    gcloud services enable storage.googleapis.com --project="$project" --quiet >&2
  fi
  bucket="${project}-tfstate"
  if ! gcloud storage buckets describe "gs://${bucket}" --project="$project" &> /dev/null; then
    echo "  Creating state bucket gs://${bucket}..." >&2
    gcloud storage buckets create "gs://${bucket}" --project="$project" --location=us-central1 \
      --uniform-bucket-level-access --public-access-prevention --quiet >&2
    gcloud storage buckets update "gs://${bucket}" --versioning --quiet >&2
  fi
  echo "$bucket"
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
