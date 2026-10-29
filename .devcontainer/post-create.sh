#!/usr/bin/env bash
set -euo pipefail

# Named volumes are created root-owned; the login state in them must be writable by vscode
sudo chown -R "$(id -u):$(id -g)" "$HOME/.config/gcloud" "$HOME/.config/gh"

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
echo "     gcloud auth login --no-launch-browser"
echo "     gcloud auth application-default login --no-launch-browser"
echo ""
echo "2. Optional: Set up cloud infrastructure (GCP)"
echo "     cd terraform"
echo "     cp terraform.tfvars.example terraform.tfvars"
echo "     cd .."
echo "     ./setup-gcp.sh"
echo ""
echo "3. Or, run tests locally (no cloud setup needed):"
echo "     See: CONTRIBUTING.md (Running system tests)"
echo ""
