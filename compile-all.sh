#!/usr/bin/env bash
# Compile every shop variant defined by a gh-optivem-*.yaml in the repo root.
#
# Each YAML is one variant of the shop template. Most are `kind: system` — a
# whole SUT (architecture, tiers, system tests, channels, external systems),
# e.g. monolith-java, multitier-dotnet. A `kind: component` YAML declares a
# standalone component with no system tier, no compose stack and no acceptance
# suite (e.g. multitier-clean-java). For each YAML we invoke
# `gh optivem compile -c <yaml>`, which dispatches per-language compile
# commands (dotnet build / gradlew compileJava compileTestJava / npm ci +
# tsc --noEmit) against the tiers listed in that YAML. The per-language
# logic lives in `internal/compiler` in the gh-optivem repo; this script
# only fans out across variants.
#
# Adding a new variant: drop a new gh-optivem-<arch>-<lang>.yaml in the
# repo root — no changes to this script. To run a single variant on its
# own use `gh optivem compile -c gh-optivem-<arch>-<lang>.yaml` directly.
# That contract is now *enforced*: the pre-flight coverage check below fails
# when a project directory is claimed by no config, so a new project cannot be
# silently skipped by this sweep. A project with no system under test is not
# exempt from the check — it registers via a `kind: component` config, which
# this glob picks up like any other.
#
# `*-legacy.yaml` variants are excluded from compiling — they share
# `system.config` and `system.path` with their non-legacy sibling and differ
# only in the `system_test.config` pointer, so compile is pure duplicate work.
# They are still read by the coverage check, since they declare coverage.
#
# Exits non-zero on any failure (zero-failures policy). Continues past the
# first failure so a single run reports every broken variant.

set -uo pipefail

REPO_ROOT="$(cd "$(dirname "$0")" && pwd)"
cd "$REPO_ROOT"

# ---------------------------------------------------------------------------
# Pre-flight coverage check
#
# The fan-out below is only as complete as the set of configs, so a project
# directory referenced by no config is compiled by nobody while the summary
# still prints "All variants compiled cleanly". That happened to
# system/multitier/backend-clean-java. The check closes it by construction:
# enumerate the project directories, resolve every path every config declares,
# and compare in both directions.
# ---------------------------------------------------------------------------

# Every path declared by any config, legacy included — a legacy config is
# excluded from compiling but still declares coverage. Emitted as "cfg|path".
declared_pairs() {
  local cfg
  shopt -s nullglob
  local all_configs=(gh-optivem-*.yaml)
  shopt -u nullglob
  for cfg in "${all_configs[@]}"; do
    grep -hE '^[[:space:]]*[A-Za-z_][A-Za-z0-9_-]*:[[:space:]]*[^[:space:]#]' "$cfg" \
      | sed -E 's/^[[:space:]]*[A-Za-z_][A-Za-z0-9_-]*:[[:space:]]*//; s/[[:space:]]*#.*$//; s/^["'"'"']//; s/["'"'"']$//' \
      | grep -E '^(system|system-test|external-systems)/' \
      | sed "s|^|$cfg\||"
  done | sort -u
}

# A directory is a project when it holds a build file. This is what excludes
# the VERSION-only marker dirs (system/multitier/{java,dotnet,typescript})
# without hardcoding their names.
is_project_dir() {
  local d="$1"
  compgen -G "$d/build.gradle*" >/dev/null && return 0
  compgen -G "$d/package.json" >/dev/null && return 0
  compgen -G "$d/*.csproj" >/dev/null && return 0
  compgen -G "$d/*.sln" >/dev/null && return 0
  compgen -G "$d/*.slnx" >/dev/null && return 0
  return 1
}

check_coverage() {
  local -a pairs=() paths=() uncovered=() missing=()
  local line cfg p d

  while IFS= read -r line; do
    [ -n "$line" ] || continue
    pairs+=("$line")
    paths+=("${line#*|}")
  done < <(declared_pairs)

  if [ ${#pairs[@]} -eq 0 ]; then
    echo "ERROR: no project paths found in any gh-optivem-*.yaml in $REPO_ROOT" >&2
    return 1
  fi

  # Forward: a project directory no config claims.
  shopt -s nullglob
  for d in system/monolith/*/ system/multitier/*/ system-test/*/ external-systems/*/; do
    d="${d%/}"
    is_project_dir "$d" || continue
    local covered=0
    for p in "${paths[@]}"; do
      if [ "$p" = "$d" ] || [ "${p#"$d"/}" != "$p" ]; then covered=1; break; fi
    done
    [ "$covered" -eq 1 ] || uncovered+=("$d")
  done
  shopt -u nullglob

  # Reverse: a config declaring a path that no longer exists. Same class of
  # silent drift, and the comparison is already in hand.
  for line in "${pairs[@]}"; do
    cfg="${line%%|*}"
    p="${line#*|}"
    [ -e "$p" ] || missing+=("$cfg -> $p")
  done

  if [ ${#uncovered[@]} -eq 0 ] && [ ${#missing[@]} -eq 0 ]; then
    return 0
  fi

  echo >&2
  echo "ERROR: gh-optivem-*.yaml coverage does not match the repo layout." >&2

  if [ ${#uncovered[@]} -gt 0 ]; then
    echo >&2
    echo "  Project directories claimed by no config — this sweep would report" >&2
    echo "  success without ever compiling them:" >&2
    for d in "${uncovered[@]}"; do echo "    $d" >&2; done
    echo >&2
    echo "  Fix: add the path to an existing gh-optivem-*.yaml, or — if the" >&2
    echo "  project has no system under test — give it its own config with" >&2
    echo "  'kind: component' (see gh-optivem-multitier-clean-java.yaml)." >&2
  fi

  if [ ${#missing[@]} -gt 0 ]; then
    echo >&2
    echo "  Configs declaring a path that does not exist — the config drifted" >&2
    echo "  from the tree, or the project was moved or deleted:" >&2
    for p in "${missing[@]}"; do echo "    $p" >&2; done
    echo >&2
    echo "  Fix: update the path in the config, or drop the entry." >&2
  fi

  echo >&2
  return 1
}

check_coverage || exit 1

shopt -s nullglob extglob
configs=(gh-optivem-!(*-legacy).yaml)
shopt -u nullglob extglob

if [ ${#configs[@]} -eq 0 ]; then
  echo "ERROR: no gh-optivem-*.yaml files found in $REPO_ROOT" >&2
  exit 1
fi

# Result rows: "config|status|duration_seconds"
declare -a RESULTS=()
OVERALL_START=$(date +%s)

for cfg in "${configs[@]}"; do
  echo
  echo "=================================================================="
  echo "  $cfg"
  echo "=================================================================="

  start=$(date +%s)
  status="PASSED"
  gh optivem compile -c "$cfg" || status="FAILED"
  end=$(date +%s)
  RESULTS+=("$cfg|$status|$((end - start))")
done

OVERALL_END=$(date +%s)

# Summary
printf "\n==================================================================\n"
printf "  SUMMARY\n"
printf "==================================================================\n\n"
printf "%-44s %-10s %s\n" "Config" "Result" "Duration"
printf -- "------------------------------------------------------------------\n"

failures=0
for row in "${RESULTS[@]}"; do
  IFS='|' read -r cfg status dur <<< "$row"
  printf "%-44s %-10s %02d:%02d\n" "$cfg" "$status" $((dur/60)) $((dur%60))
  if [ "$status" = "FAILED" ]; then failures=$((failures+1)); fi
done

printf -- "------------------------------------------------------------------\n"
total_dur=$((OVERALL_END - OVERALL_START))
printf "Total duration: %02d:%02d\n" $((total_dur/60)) $((total_dur%60))

if [ "$failures" -gt 0 ]; then
  printf "\n%d variant(s) FAILED to compile.\n" "$failures" >&2
  exit 1
fi
printf "\nAll variants compiled cleanly.\n"
