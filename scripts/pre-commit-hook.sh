#!/usr/bin/env bash
#
# Pre-commit hook (dispatcher).
#
# For each project that has staged files, delegates to that project's
# scripts/pre-commit-check.sh, which runs compile then lint.
#
# Projects:
#   system/monolith/java
#   system/monolith/typescript
#   system/monolith/dotnet
#   system/multitier/backend-java
#   system/multitier/backend-clean-java
#   system/multitier/backend-typescript
#   system/multitier/backend-dotnet
#   system/multitier/frontend-react
#
# Bypass with: git commit --no-verify

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
STAGED_FILES="$(git diff --cached --name-only --diff-filter=ACM)"

if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

FAILED_PROJECTS=()

run_project_check() {
    local project_path="$1"
    local check_script="$REPO_ROOT/$project_path/scripts/pre-commit-check.sh"

    if [ ! -f "$check_script" ]; then
        echo "==> [$project_path] SKIP (no scripts/pre-commit-check.sh)"
        return 0
    fi

    echo "==> [$project_path] running pre-commit check"
    if bash "$check_script"; then
        echo "    PASSED: $project_path"
    else
        echo "    FAILED: $project_path"
        FAILED_PROJECTS+=("$project_path")
    fi
}

# Each entry: project_path|staged_path_prefix
PROJECTS=(
    "system/monolith/java|^system/monolith/java/"
    "system/monolith/typescript|^system/monolith/typescript/"
    "system/monolith/dotnet|^system/monolith/dotnet/"
    "system/multitier/backend-java|^system/multitier/backend-java/"
    "system/multitier/backend-clean-java|^system/multitier/backend-clean-java/"
    "system/multitier/backend-typescript|^system/multitier/backend-typescript/"
    "system/multitier/backend-dotnet|^system/multitier/backend-dotnet/"
    "system/multitier/frontend-react|^system/multitier/frontend-react/"
)

for entry in "${PROJECTS[@]}"; do
    project_path="${entry%%|*}"
    path_prefix="${entry##*|}"
    if echo "$STAGED_FILES" | grep -q "$path_prefix"; then
        run_project_check "$project_path"
    fi
done

if [ "${#FAILED_PROJECTS[@]}" -ne 0 ]; then
    echo ""
    echo "Pre-commit hook FAILED for:"
    for p in "${FAILED_PROJECTS[@]}"; do
        echo "  - $p"
    done
    echo ""
    echo "Fix the errors above before committing, or bypass with: git commit --no-verify"
    exit 1
fi

echo ""
echo "Pre-commit hook passed."
exit 0
