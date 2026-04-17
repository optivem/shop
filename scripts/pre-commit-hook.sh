#!/usr/bin/env bash
#
# Pre-commit hook: runs linters only for backends whose files are staged.
#
# Detected backends (by staged file paths):
#   TypeScript multitier:  system/multitier/backend-typescript/  -> npm run lint
#   TypeScript monolith:   system/monolith/typescript/           -> npm run lint
#   Java multitier:        system/multitier/backend-java/        -> ./gradlew checkstyleMain
#   Java monolith:         system/monolith/java/                 -> ./gradlew checkstyleMain
#   .NET multitier:        system/multitier/backend-dotnet/      -> dotnet format *.slnx --verify-no-changes
#   .NET monolith:         system/monolith/dotnet/               -> dotnet format *.sln --verify-no-changes

set -uo pipefail

REPO_ROOT="$(git rev-parse --show-toplevel)"
STAGED_FILES="$(git diff --cached --name-only --diff-filter=ACM)"

if [ -z "$STAGED_FILES" ]; then
    exit 0
fi

FAILED=0

run_lint() {
    local label="$1"
    shift
    echo "==> Running lint: $label"
    if "$@"; then
        echo "    PASSED: $label"
    else
        echo "    FAILED: $label"
        FAILED=1
    fi
}

# --- TypeScript backends ---

if echo "$STAGED_FILES" | grep -q "^system/multitier/backend-typescript/"; then
    cd "$REPO_ROOT/system/multitier/backend-typescript"
    run_lint "TypeScript multitier (eslint)" npm run lint
    cd "$REPO_ROOT"
fi

if echo "$STAGED_FILES" | grep -q "^system/monolith/typescript/"; then
    cd "$REPO_ROOT/system/monolith/typescript"
    run_lint "TypeScript monolith (next lint)" npm run lint
    cd "$REPO_ROOT"
fi

# --- Java backends ---

if echo "$STAGED_FILES" | grep -q "^system/multitier/backend-java/"; then
    cd "$REPO_ROOT/system/multitier/backend-java"
    run_lint "Java multitier (checkstyle)" ./gradlew checkstyleMain
    cd "$REPO_ROOT"
fi

if echo "$STAGED_FILES" | grep -q "^system/monolith/java/"; then
    cd "$REPO_ROOT/system/monolith/java"
    run_lint "Java monolith (checkstyle)" ./gradlew checkstyleMain
    cd "$REPO_ROOT"
fi

# --- .NET backends ---

if echo "$STAGED_FILES" | grep -q "^system/multitier/backend-dotnet/"; then
    cd "$REPO_ROOT/system/multitier/backend-dotnet"
    # Find the solution file (slnx or sln)
    SLN_FILE=$(ls *.slnx 2>/dev/null || ls *.sln 2>/dev/null)
    run_lint ".NET multitier (dotnet format)" dotnet format "$SLN_FILE" --verify-no-changes
    cd "$REPO_ROOT"
fi

if echo "$STAGED_FILES" | grep -q "^system/monolith/dotnet/"; then
    cd "$REPO_ROOT/system/monolith/dotnet"
    SLN_FILE=$(ls *.slnx 2>/dev/null || ls *.sln 2>/dev/null)
    run_lint ".NET monolith (dotnet format)" dotnet format "$SLN_FILE" --verify-no-changes
    cd "$REPO_ROOT"
fi

if [ "$FAILED" -ne 0 ]; then
    echo ""
    echo "Pre-commit hook FAILED. Fix lint errors before committing."
    exit 1
fi

echo ""
echo "Pre-commit hook passed."
exit 0
