#!/usr/bin/env bash
set -euo pipefail

# Named volumes are created root-owned; the login state in them must be writable by vscode
sudo chown -R "$(id -u):$(id -g)" "$HOME/.config/gcloud" "$HOME/.config/gh"
# ~/.claude/commands is a read-only bind mount from the host; skip it
sudo find "$HOME/.claude" -path "$HOME/.claude/commands" -prune -o -exec chown "$(id -u):$(id -g)" {} +
# The claude-code feature installs as root at build time, so auto-update fails with no_permissions.
# Chown the @anthropic-ai scope folder too, not just the package: npm writes the new version next to it.
# The bin/claude symlink as well, so the updater can repoint it.
sudo chown -R "$(id -u):$(id -g)" "$(npm root -g)/@anthropic-ai"
CLAUDE_BIN="$(npm config get prefix)/bin/claude"
if [[ -L "$CLAUDE_BIN" ]]; then sudo chown -h "$(id -u):$(id -g)" "$CLAUDE_BIN"; fi

# npm's global prefix is nvm's, which vscode can write to, so this needs no sudo.
# terraform/neon.sh holds the one pin for neonctl (NEONCTL_VERSION)
# shellcheck source=terraform/neon.sh
source terraform/neon.sh
npm install -g --no-audit --no-fund "neonctl@${NEONCTL_VERSION}"

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
check neonctl   "neonctl --version"
check docker    "docker --version"
check node      "node --version"
check java      "java -version"
check dotnet    "dotnet --version"
check pwsh      "pwsh --version"
check jq        "jq --version"
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
echo "     neonctl login   (only to deploy; or export NEON_API_KEY=<key>)"
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
