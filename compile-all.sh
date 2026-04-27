#!/usr/bin/env bash
# Compile (or typecheck) every system and system-test project in the repo.
#
# This is the preflight check before committing — every language under
# `system/` and `system-test/` must compile cleanly before any commit.
# Cross-project breakage from a SonarCloud rule fix or refactor is the
# motivating use case.
#
# Walks:
#   system/monolith/{dotnet,java,typescript}
#   system/multitier/{backend-dotnet,backend-java,backend-typescript,frontend-react}
#   system-test/{dotnet,java,typescript}
#
# Per-language commands:
#   dotnet      → dotnet build --nologo -v q
#   java        → ./gradlew compileJava --no-daemon -q
#   typescript  → npx tsc --noEmit
#   frontend-*  → npx tsc --noEmit (Vite/React projects use plain tsc for type-check)
#
# Usage:
#   ./compile-all.sh
#
# Exits non-zero on any failure (zero-failures policy). Prints a summary
# table at the end mirroring test-all.sh.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"

# Result rows: "project|lang|status|duration_seconds"
declare -a RESULTS=()
OVERALL_START=$(date +%s)

run_compile() {
  local project="$1"   # short label, e.g. "monolith/java"
  local lang="$2"      # dotnet | java | typescript
  local dir="$3"       # absolute path to the project's compile dir

  echo
  echo "=================================================================="
  echo "  $project ($lang)"
  echo "=================================================================="

  if [ ! -d "$dir" ]; then
    echo "  SKIP: directory does not exist: $dir"
    RESULTS+=("$project|$lang|SKIPPED|0")
    return
  fi

  local start
  start=$(date +%s)
  local status="PASSED"

  case "$lang" in
    dotnet)
      ( cd "$dir" && dotnet build --nologo -v q ) || status="FAILED"
      ;;
    java)
      ( cd "$dir" && ./gradlew compileJava --no-daemon -q ) || status="FAILED"
      ;;
    typescript)
      ( cd "$dir" && npx tsc --noEmit ) || status="FAILED"
      ;;
    *)
      echo "  ERROR: unknown lang '$lang'"
      status="FAILED"
      ;;
  esac

  local end
  end=$(date +%s)
  RESULTS+=("$project|$lang|$status|$((end - start))")
}

# system/monolith/*
run_compile "monolith/dotnet"     "dotnet"     "$REPO_ROOT/system/monolith/dotnet"
run_compile "monolith/java"       "java"       "$REPO_ROOT/system/monolith/java"
run_compile "monolith/typescript" "typescript" "$REPO_ROOT/system/monolith/typescript"

# system/multitier/*
run_compile "multitier/backend-dotnet"     "dotnet"     "$REPO_ROOT/system/multitier/backend-dotnet"
run_compile "multitier/backend-java"       "java"       "$REPO_ROOT/system/multitier/backend-java"
run_compile "multitier/backend-typescript" "typescript" "$REPO_ROOT/system/multitier/backend-typescript"
run_compile "multitier/frontend-react"     "typescript" "$REPO_ROOT/system/multitier/frontend-react"

# system-test/*
run_compile "system-test/dotnet"     "dotnet"     "$REPO_ROOT/system-test/dotnet"
run_compile "system-test/java"       "java"       "$REPO_ROOT/system-test/java"
run_compile "system-test/typescript" "typescript" "$REPO_ROOT/system-test/typescript"

OVERALL_END=$(date +%s)

# Summary
printf "\n==================================================================\n"
printf "  SUMMARY\n"
printf "==================================================================\n\n"
printf "%-30s %-12s %-10s %s\n" "Project" "Language" "Result" "Duration"
printf -- "------------------------------------------------------------------\n"

failures=0
for row in "${RESULTS[@]}"; do
  IFS='|' read -r project lang status dur <<< "$row"
  printf "%-30s %-12s %-10s %02d:%02d\n" "$project" "$lang" "$status" $((dur/60)) $((dur%60))
  if [ "$status" = "FAILED" ]; then failures=$((failures+1)); fi
done

printf -- "------------------------------------------------------------------\n"
total_dur=$((OVERALL_END - OVERALL_START))
printf "Total duration: %02d:%02d\n" $((total_dur/60)) $((total_dur%60))

if [ "$failures" -gt 0 ]; then
  printf "\n%d project(s) FAILED to compile.\n" "$failures" >&2
  exit 1
fi
printf "\nAll projects compiled cleanly.\n"
