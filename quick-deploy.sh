#!/usr/bin/env bash
# Trigger the quick-deploy-multitier-typescript workflow and watch it until it finishes.
#
# DELIBERATELY UNTESTED deploy straight to production — see docs/environments.md#quick-deploy.
#
# Usage:
#   ./quick-deploy.sh                 # deploy main, only the parts that changed
#   ./quick-deploy.sh <ref>           # deploy a branch, tag or commit SHA
#   ./quick-deploy.sh <ref> --all     # build and deploy backend, frontend and migrations
#   ./quick-deploy.sh --no-watch ...  # dispatch only, don't wait
set -euo pipefail

WORKFLOW="quick-deploy-multitier-typescript.yml"
REF="main"
DEPLOY_ALL="false"
WATCH="true"

for arg in "$@"; do
  case "$arg" in
    --all) DEPLOY_ALL="true" ;;
    --no-watch) WATCH="false" ;;
    -h|--help) sed -n '2,10p' "$0"; exit 0 ;;
    -*) echo "Unknown option: $arg" >&2; exit 1 ;;
    *) REF="$arg" ;;
  esac
done

cd "$(git rev-parse --show-toplevel)"

# The workflow deploys what is on GitHub, not local commits — warn if main is ahead.
if [ "$REF" = "main" ]; then
  git fetch -q origin main
  ahead="$(git rev-list --count origin/main..main)"
  if [ "$ahead" -gt 0 ]; then
    echo "::warning:: local main is $ahead commit(s) ahead of origin/main; those will NOT be deployed." >&2
  fi
fi

echo "Dispatching $WORKFLOW (ref=$REF, deploy-all=$DEPLOY_ALL)..."
url="$(gh workflow run "$WORKFLOW" --ref main -f ref="$REF" -f deploy-all="$DEPLOY_ALL")"
echo "$url"
run_id="${url##*/}"

if [ "$WATCH" = "true" ] && [ -n "$run_id" ]; then
  gh run watch "$run_id" --interval 120 --exit-status
fi
