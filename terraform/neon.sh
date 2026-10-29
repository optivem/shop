#!/usr/bin/env bash
# Sourced by setup-gcp.sh / teardown-gcp.sh — Neon Postgres via neonctl.
#
# One Neon project per system, named after it (so setup and teardown find the same one from any machine);
# credentials go from neonctl straight into Secret Manager, never through Terraform state.

NEONCTL_VERSION=8.13.2
NEON_REGION=aws-us-east-2   # closest Neon region to us-central1
NEON_ROLE=neondb_owner
NEON_ORG_ARGS=()

# Pinned neonctl; npx fallback, so no global install is needed
neon_cli() {
  if command -v neonctl &> /dev/null; then
    neonctl "$@" --no-analytics
  else
    npx -y "neonctl@${NEONCTL_VERSION}" "$@" --no-analytics
  fi
}

# neonctl list commands print either a bare array or {<key>: [...]}; normalize to an array
neon_items() { jq -c --arg k "$1" 'if type == "array" then . else (.[$k] // []) end'; }

# Prerequisite check; prints ✅/❌ like the other checks, returns non-zero on ❌
neon_check_auth() {
  if ! command -v neonctl &> /dev/null && ! command -v npx &> /dev/null; then
    echo "❌ neonctl not found (and no npx to run it). Install Node.js, or: npm i -g neonctl@${NEONCTL_VERSION}"
    return 1
  elif ! neon_cli me -o json &> /dev/null; then
    echo "❌ Not authenticated to Neon. Run: npx -y neonctl@${NEONCTL_VERSION} login"
    echo "    (or export NEON_API_KEY=<key> from Neon console → Account settings → API keys; no account yet: https://console.neon.tech)"
    return 1
  fi
  echo "  ✅ Neon authenticated"
}

# Sets NEON_ORG_ARGS from NEON_ORG_ID, or the account's only org; fails if there are several to choose from
neon_resolve_org() {
  local orgs
  if [[ -n "${NEON_ORG_ID:-}" ]]; then
    NEON_ORG_ARGS=(--org-id "$NEON_ORG_ID")
    return 0
  fi
  orgs=$(neon_cli orgs list -o json 2> /dev/null | neon_items organizations) || orgs="[]"
  if [[ "$(jq length <<< "$orgs")" -eq 1 ]]; then
    NEON_ORG_ARGS=(--org-id "$(jq -r '.[0].id' <<< "$orgs")")
  elif [[ "$(jq length <<< "$orgs")" -gt 1 ]]; then
    echo "❌ Your Neon account has several organizations. Pick one and re-run:"
    jq -r --arg cmd "$(basename "$0")" '.[] | "     NEON_ORG_ID=\(.id) ./\($cmd)    # \(.name)"' <<< "$orgs"
    return 1
  fi
}

# Prints the ID of the Neon project named $1, or nothing if none; fails if several share the name
neon_find_project() {
  local matches
  matches=$(neon_cli projects list ${NEON_ORG_ARGS[@]+"${NEON_ORG_ARGS[@]}"} -o json | neon_items projects \
    | jq -c --arg n "$1" '[.[] | select(.name == $n)]')
  if [[ "$(jq length <<< "$matches")" -gt 1 ]]; then
    echo "❌ Several Neon projects are named '$1'; delete or rename the extras and re-run." >&2
    return 1
  fi
  jq -r '.[0].id // empty' <<< "$matches"
}
