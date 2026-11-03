#!/usr/bin/env bash
set -euo pipefail

# The workspace is shared with the host, and so are the gitignored .NET bin/ and
# obj/ folders. Output from a host-side restore or build breaks the container:
# project.assets.json points at host paths (C:\...), so the C# extension can't
# load the project, and on Windows the files can't be touched (MSB3374).
# Host-created files aren't owned by this user, which is how they're detected.
# Runs on every start because the host can restore between sessions.

stale=()
while IFS= read -r -d '' csproj; do
  dir="$(dirname "$csproj")"
  if [ -n "$(find "$dir/bin" "$dir/obj" -maxdepth 3 ! -user "$(id -un)" -print -quit 2>/dev/null)" ]; then
    rm -rf "$dir/bin" "$dir/obj"
    stale+=("$csproj")
  fi
done < <(find system system-test external-systems \( -name node_modules -o -name bin -o -name obj -o -name build \) -prune -o -name '*.csproj' -print0)

if [ "${#stale[@]}" -eq 0 ]; then
  exit 0
fi

echo "Restoring ${#stale[@]} .NET project(s) with host-created build output"
for csproj in "${stale[@]}"; do
  dotnet restore "$csproj"
done
