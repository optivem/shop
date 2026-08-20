#!/usr/bin/env bash
# Run `gh optivem config validate` and `gh optivem config preflight` against
# every gh-optivem-*.yaml in the repo root.
#
# `config validate` checks the YAML against the schema.
# `config preflight` runs the schema check plus the on-disk layout check
# (every declared repo and tier path resolves to a real directory).
#
# Why both rather than just preflight: validate is cheap and pure-local, so
# running it first means a schema break is reported clearly before any
# filesystem or remote work fires. If validate fails for a yaml, preflight
# is skipped for that yaml (it would just re-surface the same schema error).
#
# Exits non-zero if any yaml fails either check (zero-failures policy).
# Continues past the first failure so a single run reports every broken yaml.
#
# A PASSED row only means "this yaml is valid for the gh-optivem that ran the
# check" — so the run reports which build that was, up front and again beside
# the summary. CI installs the latest *published release*; a dev machine often
# runs a source build of gh-optivem main, which understands config fields no
# release does yet. When those diverge, a green local run predicts nothing
# about CI, and the script says so rather than leaving the reader to assume.
#
# Counterpart to migrate-all-gh-optivem-config.sh — same iteration shape,
# different verbs.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

shopt -s nullglob
configs=(gh-optivem-*.yaml)
shopt -u nullglob

if [ ${#configs[@]} -eq 0 ]; then
  echo "ERROR: no gh-optivem-*.yaml files found in $REPO_ROOT" >&2
  exit 1
fi

# Which gh-optivem is doing the validating. stderr is folded into the captured
# output rather than discarded, so a broken CLI reports itself here instead of
# leaving a blank banner. A dev build stamps its version `dev-<sha>`.
CLI_VERSION="$(gh optivem --version 2>&1)"
IS_DEV_BUILD=false
case "$CLI_VERSION" in
  *dev-*) IS_DEV_BUILD=true ;;
esac

dev_build_warning() {
  cat >&2 <<'EOF'

  ******************************************************************
  *  WARNING: validated with a DEV BUILD — this does NOT predict CI *
  ******************************************************************

  CI installs the latest *published release* of gh-optivem
  (optivem/actions/install-gh-optivem@v1 with `ref` unset, mirroring the
  student flow). A source build of gh-optivem main accepts config fields
  that no release understands yet, so every PASSED row above proves only
  that this build is happy — not that the pipeline will be.

  Before relying on this run as a pre-commit gate, confirm that whatever
  the configs depend on is in a *published release*, not merely committed:

      gh release list --repo optivem/gh-optivem --limit 3

EOF
}

echo "=================================================================="
echo "  gh-optivem build performing this check"
echo "=================================================================="
echo "$CLI_VERSION"
if [ "$IS_DEV_BUILD" = true ]; then
  dev_build_warning
fi

# Result rows: "config|validate_status|preflight_status|duration_seconds"
declare -a RESULTS=()
OVERALL_START=$(date +%s)

for cfg in "${configs[@]}"; do
  echo
  echo "=================================================================="
  echo "  $cfg"
  echo "=================================================================="

  start=$(date +%s)
  validate_status="PASSED"
  preflight_status="SKIPPED"

  echo "--- config validate"
  if ! gh optivem -c "$cfg" config validate; then
    validate_status="FAILED"
  fi

  if [ "$validate_status" = "PASSED" ]; then
    echo "--- config preflight"
    preflight_status="PASSED"
    if ! gh optivem -c "$cfg" config preflight; then
      preflight_status="FAILED"
    fi
  fi

  end=$(date +%s)
  RESULTS+=("$cfg|$validate_status|$preflight_status|$((end - start))")
done

OVERALL_END=$(date +%s)

# Summary
printf "\n==================================================================\n"
printf "  SUMMARY\n"
printf "==================================================================\n\n"
printf "%-44s %-10s %-10s %s\n" "Config" "Validate" "Preflight" "Duration"
printf -- "------------------------------------------------------------------\n"

failures=0
for row in "${RESULTS[@]}"; do
  IFS='|' read -r cfg vstatus pstatus dur <<< "$row"
  printf "%-44s %-10s %-10s %02d:%02d\n" "$cfg" "$vstatus" "$pstatus" $((dur/60)) $((dur%60))
  if [ "$vstatus" = "FAILED" ] || [ "$pstatus" = "FAILED" ]; then
    failures=$((failures+1))
  fi
done

printf -- "------------------------------------------------------------------\n"
total_dur=$((OVERALL_END - OVERALL_START))
printf "Total duration: %02d:%02d\n" $((total_dur/60)) $((total_dur%60))
printf "Validated with: %s\n" "$(printf '%s' "$CLI_VERSION" | head -n 1)"

# Repeated here on purpose: the banner at the top has scrolled well past by
# now, and the caveat is only useful next to the rows it qualifies.
if [ "$IS_DEV_BUILD" = true ]; then
  dev_build_warning
fi

if [ "$failures" -gt 0 ]; then
  printf "\n%d yaml(s) FAILED.\n" "$failures" >&2
  exit 1
fi
printf "\nAll yamls validate and pass preflight.\n"
