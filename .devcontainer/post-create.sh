#!/usr/bin/env bash
set -euo pipefail

# Named volumes are created root-owned; the login state in them must be writable by vscode
sudo chown -R "$(id -u):$(id -g)" "$HOME/.config/gcloud" "$HOME/.config/gh"
# ~/.claude/commands is a read-only bind mount from the host; skip it
sudo find "$HOME/.claude" -path "$HOME/.claude/commands" -prune -o -exec chown "$(id -u):$(id -g)" {} +

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
check java      "java -version"
check dotnet    "dotnet --version"
check pwsh      "pwsh --version"
check claude    "claude --version"

echo ""
echo "=== Installing test prerequisites ==="
echo ""

# Chromium's OS libraries; each suite downloads the browser itself
(cd system-test/typescript && npx --yes playwright install-deps chromium)

# gh-optivem needs a GitHub login, so a fresh container installs it after `gh auth login`
if gh auth status >/dev/null 2>&1; then
  gh extension install optivem/gh-optivem || gh extension upgrade optivem/gh-optivem
else
  echo "  Skipped gh-optivem: run 'gh extension install optivem/gh-optivem' after 'gh auth login'"
fi

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
