#!/usr/bin/env bash
#
# Installs the pre-commit hook by symlinking scripts/pre-commit-hook.sh
# into .git/hooks/pre-commit.
#

set -euo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
HOOK_SOURCE="$REPO_ROOT/scripts/pre-commit-hook.sh"
HOOK_TARGET="$REPO_ROOT/.git/hooks/pre-commit"

if [ ! -f "$HOOK_SOURCE" ]; then
    echo "ERROR: Hook script not found at $HOOK_SOURCE"
    exit 1
fi

# Ensure the hook script is executable
chmod +x "$HOOK_SOURCE"

# Remove existing hook if present
if [ -e "$HOOK_TARGET" ] || [ -L "$HOOK_TARGET" ]; then
    echo "Removing existing pre-commit hook..."
    rm "$HOOK_TARGET"
fi

# Create symlink using a relative path so it works if the repo is moved
ln -s "../../scripts/pre-commit-hook.sh" "$HOOK_TARGET"

echo "Pre-commit hook installed successfully."
echo "  Source: scripts/pre-commit-hook.sh"
echo "  Target: .git/hooks/pre-commit"
