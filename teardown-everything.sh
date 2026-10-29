#!/usr/bin/env bash
set -euo pipefail

# Delete EVERYTHING the current accounts can see, not just shop:
#   - every ACTIVE GCP project visible to the gcloud account (including the Terraform state project)
#   - every Neon project in every Neon organization (and the personal account)
# Lists everything first and asks for a typed confirmation before deleting anything.
# GCP projects go to DELETE_REQUESTED: billing stops at once, and they can be restored for 30 days
# (gcloud projects undelete <id>). Neon project deletion is immediate.
# Usage: ./teardown-everything.sh

echo "========================================="
echo " Teardown EVERYTHING (all GCP + all Neon)"
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
GCP_PROJECTS=$(gcloud projects list --filter="lifecycleState:ACTIVE" --format="value(projectId)" | sort)

# Neon projects: every organization, plus the personal account (stdin closed so neonctl never prompts)
NEON_PROJECTS="[]"
org_ids=$(neon_cli orgs list -o json < /dev/null 2> /dev/null | neon_items organizations | jq -r '.[].id') || org_ids=""
for org in $org_ids ""; do
  args=()
  [[ -n "$org" ]] && args=(--org-id "$org")
  found=$(neon_cli projects list ${args[@]+"${args[@]}"} -o json < /dev/null 2> /dev/null | neon_items projects) || found="[]"
  NEON_PROJECTS=$(jq -c -s 'add | unique_by(.id)' <<< "$NEON_PROJECTS $found")
done

echo "GCP projects to delete:"
if [[ -n "$GCP_PROJECTS" ]]; then
  gcloud projects list --filter="lifecycleState:ACTIVE" --format="table[no-heading](projectId,name)" | sed 's/^/     /'
else
  echo "     (none)"
fi
echo ""
echo "Neon projects to delete:"
if [[ "$(jq length <<< "$NEON_PROJECTS")" -gt 0 ]]; then
  jq -r '.[] | "     \(.id)  \(.name)"' <<< "$NEON_PROJECTS"
else
  echo "     (none)"
fi
echo ""

if [[ -z "$GCP_PROJECTS" && "$(jq length <<< "$NEON_PROJECTS")" -eq 0 ]]; then
  echo "Nothing to delete."
  exit 0
fi

echo "⚠️  This permanently deletes ALL of the above, including projects unrelated to shop"
echo "    and the shared Terraform state project. Neon data cannot be recovered."
read -rp "  Type 'delete everything' to confirm: " CONFIRM
if [[ "$CONFIRM" != "delete everything" ]]; then
  echo "  Aborted."
  exit 0
fi
echo ""

FAILED=()

echo "Deleting Neon projects..."
for id in $(jq -r '.[].id' <<< "$NEON_PROJECTS"); do
  if neon_cli projects delete "$id" < /dev/null > /dev/null; then
    echo "  ✅ Neon project $id deleted"
  else
    echo "  ❌ Neon project $id failed"
    FAILED+=("neon:$id")
  fi
done
echo ""

echo "Deleting GCP projects..."
for id in $GCP_PROJECTS; do
  if gcloud projects delete "$id" --quiet 2> /dev/null; then
    echo "  ✅ GCP project $id deleted (restorable for 30 days)"
  else
    echo "  ❌ GCP project $id failed (liens? check: gcloud alpha resource-manager liens list --project=$id)"
    FAILED+=("gcp:$id")
  fi
done
echo ""

echo "========================================="
if [[ ${#FAILED[@]} -eq 0 ]]; then
  echo " Teardown Complete!"
else
  echo " Teardown finished with ${#FAILED[@]} failure(s): ${FAILED[*]}"
fi
echo "========================================="
echo ""
echo "  Not touched: GitHub repo variables/secrets and deploy-config.yml"
echo "  (run ./teardown-gcp.sh first if you want those cleaned up for shop)."
[[ ${#FAILED[@]} -eq 0 ]]
