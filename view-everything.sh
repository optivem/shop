#!/usr/bin/env bash
set -uo pipefail

# Show EVERYTHING the current accounts can see, not just shop — the same scope as teardown-everything.sh, in more detail:
#   - every GCP project visible to the gcloud account: Cloud Run services, Artifact Registry, secrets, buckets
#   - GCP projects awaiting deletion (restorable for 30 days)
#   - every Neon project in every Neon organization, with its branches and databases
#   - this repo's GitHub variables and secret names
# Read-only: it creates, changes and deletes nothing, and never prints secret values.
# Exits non-zero if any section could not be read, so "(none)" always means none.
# Usage: ./view-everything.sh

UNREADABLE=0

# Runs a listing command and prints its output indented; a failure is reported, never shown as empty
show() {
  local label="$1" out
  shift
  echo "  $label:"
  if ! out=$("$@" < /dev/null 2> /dev/null); then
    echo "     ⚠️  could not read"
    UNREADABLE=$((UNREADABLE + 1))
  elif [[ -n "$out" ]]; then
    sed 's/^/     /' <<< "$out"
  else
    echo "     (none)"
  fi
}

# Like show, but skips the section when the project hasn't enabled the API it needs (listing would prompt to enable it)
show_if_enabled() {
  local api="$1" label="$2"
  shift 2
  if ! grep -qx "$api" <<< "$ENABLED_APIS"; then
    echo "  $label: (API not enabled)"
    return
  fi
  show "$label" "$@"
}

neon_databases() {
  neon_cli databases list --project-id "$1" --branch "$2" -o json | neon_items databases | jq -r '.[] | "\(.name)  (owner: \(.owner_name))"'
}

gh_secret_names() { gh secret list --json name --jq '.[].name'; }

echo "========================================="
echo " View EVERYTHING (all GCP + all Neon)"
echo "========================================="
echo ""

for cmd in gcloud jq; do
  command -v "$cmd" &> /dev/null || { echo "❌ $cmd not found."; exit 1; }
done

# shellcheck source=terraform/neon.sh
source "$(dirname "$0")/terraform/neon.sh"
neon_check_auth || exit 1

gcloud_account=$(gcloud auth list --filter=status:ACTIVE --format="value(account)")
[[ -n "$gcloud_account" ]] || { echo "❌ Run: gcloud auth login --no-launch-browser"; exit 1; }
echo "  ✅ Google Cloud authenticated as: $gcloud_account"
echo ""

# GCP projects
echo "========== GCP projects =========="
if ! GCP_PROJECTS=$(gcloud projects list --filter="lifecycleState:ACTIVE" --format="value(projectId)" < /dev/null 2> /dev/null); then
  echo "⚠️  could not read"
  UNREADABLE=$((UNREADABLE + 1))
elif [[ -z "$GCP_PROJECTS" ]]; then
  echo "(none)"
fi
for id in $(sort <<< "$GCP_PROJECTS"); do
  echo ""
  echo "--- $id ---"
  if ! ENABLED_APIS=$(gcloud services list --enabled --project="$id" --format="value(config.name)" < /dev/null 2> /dev/null); then
    echo "  ⚠️  could not read enabled APIs; skipping its resources"
    UNREADABLE=$((UNREADABLE + 1))
    continue
  fi
  show_if_enabled run.googleapis.com "Cloud Run services" \
    gcloud run services list --project="$id" --format="table[no-heading](metadata.name,metadata.labels['cloud.googleapis.com/location'],status.url)"
  show_if_enabled artifactregistry.googleapis.com "Artifact Registry repositories" \
    gcloud artifacts repositories list --project="$id" --format="table[no-heading](name.basename(),format,sizeBytes.size())"
  show_if_enabled secretmanager.googleapis.com "Secrets (names only)" \
    gcloud secrets list --project="$id" --format="value(name)"
  show_if_enabled storage.googleapis.com "Storage buckets" \
    gcloud storage buckets list --project="$id" --format="value(name)"
done
echo ""

echo "========== GCP projects awaiting deletion =========="
show "Restorable with: gcloud projects undelete <id>" \
  gcloud projects list --filter="lifecycleState:DELETE_REQUESTED" --format="table[no-heading](projectId,name)"
echo ""

# Neon projects: every organization, or the personal account when there are none (with orgs, an org-less
# 'projects list' prompts for one instead of printing JSON; stdin is closed so neonctl never waits)
echo "========== Neon projects =========="
NEON_PROJECTS="[]"
if ! orgs=$(neon_cli orgs list -o json < /dev/null 2> /dev/null); then
  echo "⚠️  could not read organizations"
  UNREADABLE=$((UNREADABLE + 1))
fi
org_ids=$(neon_items organizations <<< "${orgs:-[]}" | jq -r '.[].id')
for org in ${org_ids:-""}; do
  args=()
  [[ -n "$org" ]] && args=(--org-id "$org")
  if ! found=$(neon_cli projects list ${args[@]+"${args[@]}"} -o json < /dev/null 2> /dev/null); then
    echo "⚠️  could not read projects${org:+ of organization $org}"
    UNREADABLE=$((UNREADABLE + 1))
    continue
  fi
  NEON_PROJECTS=$(jq -c -s 'add | unique_by(.id)' <<< "$NEON_PROJECTS $(neon_items projects <<< "$found")")
done
[[ "$(jq length <<< "$NEON_PROJECTS")" -gt 0 ]] || echo "(none)"
while IFS=$'\t' read -r id name region; do
  [[ -n "$id" ]] || continue
  echo ""
  echo "--- $name ($id, $region) ---"
  if ! branches=$(neon_cli branches list --project-id "$id" -o json < /dev/null 2> /dev/null); then
    echo "  ⚠️  could not read branches"
    UNREADABLE=$((UNREADABLE + 1))
    continue
  fi
  while IFS=$'\t' read -r branch_id branch_name; do
    show "Branch $branch_name: databases" neon_databases "$id" "$branch_id"
  done < <(neon_items branches <<< "$branches" | jq -r '.[] | [.id, .name] | @tsv')
done < <(jq -r '.[] | [.id, .name, .region_id] | @tsv' <<< "$NEON_PROJECTS")
echo ""

echo "========== GitHub repo =========="
if command -v gh &> /dev/null && REPO=$(gh repo view --json nameWithOwner -q '.nameWithOwner' 2> /dev/null); then
  echo "  $REPO"
  show "Variables" gh variable list
  show "Secrets (names only)" gh_secret_names
else
  echo "  ⚠️  could not read (gh missing or not authenticated)"
  UNREADABLE=$((UNREADABLE + 1))
fi
echo ""

echo "========================================="
if [[ "$UNREADABLE" -eq 0 ]]; then
  echo " Done. Nothing was changed."
else
  echo " Done, but $UNREADABLE section(s) could not be read. Nothing was changed."
fi
echo "========================================="
[[ "$UNREADABLE" -eq 0 ]]
