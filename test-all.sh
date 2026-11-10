#!/usr/bin/env bash
# Run latest + legacy system-test suites for every language in the repo.
#
# Brings up each lang's system stack, runs latest then legacy tests, stops the
# stack after each phase (also on Ctrl+C / SIGTERM, via a trap), and prints a
# Lang / Phase / Result / Duration summary at the end. Exits non-zero on any
# failure (zero-failures policy).
#
# Usage:
#   ./test-all.sh -a monolith
#   ./test-all.sh -a multitier -l dotnet,java
#   ./test-all.sh -a monolith --restart
set -uo pipefail

ARCH=""
LANGS_CSV="dotnet,java,typescript"
START_ARGS=()

usage() {
  cat <<EOF
Usage: $0 -a <architecture> [-l <langs>] [--restart]
  -a, --architecture   monolith | multitier  (required)
  -l, --languages      Comma-separated subset of: dotnet, java, typescript
                       (default: all three)
      --restart        Pass --restart to 'gh optivem system start' so changed
                       services are rebuilt/recreated instead of reusing
                       possibly stale images (default: plain start)

Each phase's stack is stopped with 'gh optivem system stop' when the phase
ends, and also if the run is interrupted (Ctrl+C / SIGTERM).
EOF
}

while [ $# -gt 0 ]; do
  case "$1" in
    -a|--architecture) ARCH="${2:-}"; shift 2 ;;
    -l|--languages)    LANGS_CSV="${2:-}"; shift 2 ;;
    --restart)         START_ARGS+=(--restart); shift ;;
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

# Config of the phase whose stack may currently be up; empty when none is.
# The trap below stops it if the script is cut off mid-phase.
CURRENT_CONFIG=""

stop_stack() {
  local config="$1"
  echo
  echo "--- Stopping stack ($config) ---"
  ( cd "$REPO_ROOT" && GH_OPTIVEM_CONFIG="$config" gh optivem system stop ) \
    || echo "WARNING: 'gh optivem system stop' failed for $config; containers may still be running." >&2
}

# Runs on EXIT, and on INT/TERM (which then exit with 130/143). Tears down a
# stack left up by an interrupted phase, then preserves the exit code.
cleanup() {
  local rc=$?
  if [ $# -gt 0 ]; then rc="$1"; fi
  trap - EXIT INT TERM
  if [ -n "$CURRENT_CONFIG" ]; then
    local config="$CURRENT_CONFIG"
    CURRENT_CONFIG=""
    echo "Run interrupted; tearing down $config." >&2
    stop_stack "$config"
  fi
  exit "$rc"
}
trap 'cleanup' EXIT
trap 'cleanup 130' INT
trap 'cleanup 143' TERM

run_phase() {
  local lang="$1"
  local phase="$2"
  local variant_suffix="$3"
  local config="gh-optivem-$ARCH-$lang$variant_suffix.yaml"

  echo
  echo "=================================================================="
  echo "  $lang / $phase"
  echo "=================================================================="
  local start
  start=$(date +%s)
  local status="PASSED"
  CURRENT_CONFIG="$config"
  (
    cd "$REPO_ROOT" \
      && GH_OPTIVEM_CONFIG="$config" gh optivem system start ${START_ARGS[@]+"${START_ARGS[@]}"} \
      && GH_OPTIVEM_CONFIG="$config" gh optivem system-test setup \
      && GH_OPTIVEM_CONFIG="$config" gh optivem system-test run
  ) || status="FAILED"
  # Always stop, even after a failed start (it may have left a partial stack).
  # Clear CURRENT_CONFIG first so an interrupt during stop doesn't stop twice.
  CURRENT_CONFIG=""
  stop_stack "$config"
  local end
  end=$(date +%s)
  RESULTS+=("$lang|$phase|$status|$((end - start))")
}

for lang in "${LANGS[@]}"; do
  run_phase "$lang" "Latest" ""
  run_phase "$lang" "Legacy" "-legacy"
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
