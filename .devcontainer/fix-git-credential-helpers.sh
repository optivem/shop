#!/usr/bin/env bash
set -euo pipefail

# Dev Containers copies the host's ~/.gitconfig into the container. On Windows, `gh auth setup-git`
# writes credential helpers with a Windows path ('C:\Program Files\GitHub CLI\gh.exe'), which doesn't
# exist here, so every push and pull prints "gh.exe: not found". Point those helpers at the container's
# own gh, or, while it isn't logged in, drop them so VS Code's forwarding helper applies.
# Runs on every attach (postAttachCommand), because the copy can bring the Windows entries back.

keys=$(git config --global --get-regexp '^credential\..*\.helper$' 2>/dev/null \
  | grep -iE '[a-z]:\\|\.exe' | cut -d' ' -f1 | sort -u || true)

for key in $keys; do
  git config --global --unset-all "$key"
  if gh auth status >/dev/null 2>&1; then
    # The empty entry first clears any helper inherited from a broader config, as `gh auth setup-git` does
    git config --global --add "$key" ""
    git config --global --add "$key" "!gh auth git-credential"
    echo "Fixed $key: Windows gh path -> container gh"
  else
    echo "Removed $key: Windows gh path (container gh not logged in; VS Code's credential forwarding applies)"
  fi
done
