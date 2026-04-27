#!/usr/bin/env bash
# Run latest + legacy system-test suites for every language in the repo.
#
# Brings up each lang's system stack, runs latest then legacy tests, and prints
# a Lang / Phase / Result / Duration summary at the end. Exits non-zero on any
# failure (zero-failures policy).
#
# Usage:
#   ./test-all.sh -a monolith
#   ./test-all.sh -a multitier -l dotnet,java
set -uo pipefail

ARCH=""
LANGS_CSV="dotnet,java,typescript"

usage() {
  cat <<EOF
Usage: $0 -a <architecture> [-l <langs>]
  -a, --architecture   monolith | multitier  (required)
  -l, --languages      Comma-separated subset of: dotnet, java, typescript
                       (default: all three)
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -a|--architecture) ARCH="${2:-}"; shift 2 ;;
    -l|--languages)    LANGS_CSV="${2:-}"; shift 2 ;;
    -h|--help)         usage; exit 0 ;;
    *) echo "Unknown arg: $1" >&2; usage; exit 2 ;;
  esac
done

if [ "$ARCH" != "monolith" ] && [ "$ARCH" != "multitier" ]; then
  echo "ERROR: -a/--architecture must be 'monolith' or 'multitier'." >&2
  usage
  exit 2
fi

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"

IFS=',' read -ra LANGS <<< "$LANGS_CSV"

# Result rows: "lang|phase|status|duration_seconds"
declare -a RESULTS=()
OVERALL_START=$(date +%s)

run_phase() {
  local lang="$1"
  local phase="$2"
  local tests_file="$3"
  local system_json="$REPO_ROOT/docker/$lang/$ARCH/system.json"
  local tests_json="$REPO_ROOT/system-test/$lang/$tests_file"

  echo
  echo "=================================================================="
  echo "  $lang / $phase"
  echo "=================================================================="
  local start
  start=$(date +%s)
  local status="PASSED"
  (
    cd "$REPO_ROOT" \
      && gh optivem run system --system "$system_json" \
      && gh optivem test system --system "$system_json" --tests "$tests_json"
  ) || status="FAILED"
  local end
  end=$(date +%s)
  RESULTS+=("$lang|$phase|$status|$((end - start))")
}

for lang in "${LANGS[@]}"; do
  run_phase "$lang" "Latest" "tests-latest.json"
  run_phase "$lang" "Legacy" "tests-legacy.json"
done

OVERALL_END=$(date +%s)

# Summary
printf "\n==================================================================\n"
printf "  SUMMARY\n"
printf "==================================================================\n\n"
printf "%-12s %-8s %-10s %s\n" "Language" "Phase" "Result" "Duration"
printf -- "------------------------------------------------------------------\n"

failures=0
for row in "${RESULTS[@]}"; do
  IFS='|' read -r lang phase status dur <<< "$row"
  printf "%-12s %-8s %-10s %02d:%02d\n" "$lang" "$phase" "$status" $((dur/60)) $((dur%60))
  if [ "$status" = "FAILED" ]; then failures=$((failures+1)); fi
done

printf -- "------------------------------------------------------------------\n"
total_dur=$((OVERALL_END - OVERALL_START))
printf "Total duration: %02d:%02d\n" $((total_dur/60)) $((total_dur%60))

if [ "$failures" -gt 0 ]; then
  printf "\n%d run(s) FAILED.\n" "$failures" >&2
  exit 1
fi
printf "\nAll runs passed.\n"
