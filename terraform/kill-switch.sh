#!/usr/bin/env bash
# Sourced by setup-gcp.sh — recovers a project the billing kill-switch has unlinked (billing-kill-switch.tf).
#
# Terraform can't do this itself: with billing off, Secret Manager and Cloud Storage refuse even the
# reads Terraform needs to plan (BILLING_DISABLED), so the plan fails before it could link billing again.
# Order matters: the budget is raised first, because a budget notice arrives within minutes and would
# unlink billing again if the limit were still below this month's cost.

# Polls an API that needs billing until it answers, because billing takes a few minutes to propagate
_wait_for_billing() {
  local project="$1" err="" i
  for i in $(seq 1 30); do
    if err=$(gcloud secrets list --project="$project" --limit=1 --format="value(name)" 2>&1 > /dev/null); then
      echo "  ✅ Billing active on $project"
      return 0
    fi
    echo "  ⏳ Waiting for billing to propagate ($i/30)..."
    sleep 20
  done
  echo "❌ Billing still not active on $project after 10 minutes. Last error:" >&2
  echo "$err" >&2
  echo "   Wait a few minutes and re-run the same command." >&2
  return 1
}

# Args: billing account, billing project (for gcloud quota), amount, auto-approve (true/false).
# No-op on first setup (no GCP_PROJECT_ID variable yet) or while billing is still linked.
restore_billing_if_killed() {
  local billing_account="$1" billing_project="$2" amount="$3" auto_approve="$4"
  local project system_name enabled budget budget_units currency answer

  # A missing variable means first setup; any other gh failure (auth, network) must not pass as that
  if ! project=$(gh variable get GCP_PROJECT_ID 2>&1); then
    if [[ "$project" == *"not found"* ]]; then
      echo "  ✅ No existing project yet (first setup)"
      return 0
    fi
    echo "❌ 'gh variable get GCP_PROJECT_ID' failed: $project" >&2
    return 1
  fi
  enabled=$(gcloud billing projects describe "$project" --billing-project="$billing_project" --format="value(billingEnabled)")
  if [[ "$enabled" == "True" ]]; then
    echo "  ✅ Billing linked on $project"
    return 0
  fi

  echo "  ⚠️  Billing is unlinked on $project: the kill-switch has fired (or billing was removed by hand)"
  system_name=$(gh variable get SYSTEM_NAME)
  # Matched here, not with --filter: gcloud's filter doesn't match a display name containing a space
  budget=$(gcloud billing budgets list --billing-account="$billing_account" --billing-project="$billing_project" \
    --format="value(name,displayName)" | awk -F'\t' -v want="${system_name} kill-switch" '$2 == want { print $1; exit }')
  if [[ -z "$budget" ]]; then
    echo "❌ No '${system_name} kill-switch' budget on billing account $billing_account." >&2
    echo "   Link billing by hand: gcloud billing projects link $project --billing-account=$billing_account" >&2
    return 1
  fi
  budget_units=$(gcloud billing budgets describe "$budget" --billing-project="$billing_project" --format="value(amount.specifiedAmount.units)")
  currency=$(gcloud billing budgets describe "$budget" --billing-project="$billing_project" --format="value(amount.specifiedAmount.currencyCode)")

  if (( amount <= budget_units )); then
    echo ""
    echo "⚠️  The limit stays at $amount $currency. If this month's cost is already over it, the next budget notice"
    echo "   unlinks billing again within minutes. Raise it, e.g.: BILLING_KILL_SWITCH_AMOUNT=$((budget_units * 5)) ./setup-gcp.sh"
    echo "   (Keeping it is fine once a new month has started.)"
    if [[ "$auto_approve" != "true" ]]; then
      read -r -p "Link billing again at $amount $currency anyway? (yes/no) " answer
      [[ "$answer" == "yes" ]] || { echo "Aborted."; return 1; }
    fi
  else
    echo "  Raising the kill-switch limit: $budget_units -> $amount $currency..."
    gcloud billing budgets update "$budget" --billing-project="$billing_project" --budget-amount="${amount}${currency}"
  fi

  _link_until_it_holds "$project" "$billing_account" "$billing_project"
}

# A notice sent before the budget raise propagated still carries the old limit, and the function
# unlinks billing on it (seen 2026-11-18: unlinked ~2 min after the re-link). So after linking,
# watch billing for 5 minutes and link again if a late notice removed it.
_link_until_it_holds() {
  local project="$1" billing_account="$2" billing_project="$3" attempt i enabled
  for attempt in 1 2 3; do
    echo "  Linking $project to billing account $billing_account (attempt $attempt/3)..."
    gcloud billing projects link "$project" --billing-account="$billing_account" --billing-project="$billing_project"
    _wait_for_billing "$project" || return 1
    echo "  Watching billing for 5 minutes in case a late budget notice (with the old limit) unlinks it..."
    for i in $(seq 1 10); do
      sleep 30
      enabled=$(gcloud billing projects describe "$project" --billing-project="$billing_project" --format="value(billingEnabled)")
      [[ "$enabled" == "True" ]] || { echo "  ⚠️  A late budget notice unlinked billing again"; continue 2; }
    done
    echo "  ✅ Billing stayed linked on $project"
    return 0
  done
  echo "❌ Billing keeps getting unlinked on $project. Check the kill-switch log:" >&2
  echo "   gcloud logging read 'resource.labels.service_name=\"billing-kill-switch\"' --project=$project --freshness=1h --limit=10" >&2
  echo "   If the notices show the new limit, this month's cost is over it: raise BILLING_KILL_SWITCH_AMOUNT further." >&2
  return 1
}
