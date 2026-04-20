#!/usr/bin/env bash
set -euo pipefail

echo "=== Verifying devcontainer tools ==="
echo ""

check() {
  local name="$1"
  local cmd="$2"
  if command -v "$name" >/dev/null 2>&1; then
    printf "  %-12s %s\n" "$name" "$(eval "$cmd" 2>&1 | head -1)"
  else
    printf "  %-12s MISSING\n" "$name"
    return 1
  fi
}

check gh        "gh --version"
check gcloud    "gcloud --version | head -1"
check terraform "terraform version | head -1"
check docker    "docker --version"
check node      "node --version"

echo ""
echo "=== Next steps ==="
echo ""
echo "1. Authenticate:"
echo "     gh auth login"
echo "     gcloud auth login"
echo ""
echo "2. Run GCP setup:"
echo "     ./setup-gcp.sh"
echo ""
echo "3. Trigger workflow:"
echo "     gh workflow run monolith-typescript-acceptance-stage-cloud.yml"
echo ""
