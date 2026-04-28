# Plan — Cross-Language System Verification Workflow

**Date:** 2026-04-27
**Status:** Phase 1 partially complete (workflow file written, not yet run/verified)
**Owner:** unassigned

## Goal

A single workflow that verifies behavior parity across system languages: each test-lang's suite is run against every other system-lang's SUT (Java tests vs .NET system, .NET tests vs TypeScript system, etc.). Same-lang-vs-same-system combos are skipped — those are already covered by the per-(arch, lang) acceptance stages.

This is purely a **regression check**. It does NOT tag images, publish git tags, or trigger downstream prerelease pipelines. Failures surface backend-behavior drift (the "identical backend behavior" contract from the project rules) but do not block any release.

## Why it goes here, not in gh-optivem or per-lang stages

- **Not in `gh-optivem/_gh-acceptance-pipeline.yml`** — that matrix tests the gh-optivem CLI's scaffolding ability, not shop's runtime behavior parity. Different concern.
- **Not in per-(arch, lang) acceptance stages** — those produce per-artifact RC tags. Adding cross-lang verification to each stage would (a) get template-copy-pasted across 6 workflows, (b) couple "Java RC release" to "all 3 backends green" — wrong, since a flaky .NET image should not block a Java release.
- **One standalone workflow** — runs on its own cadence, fails loud, blocks nothing.

## Out of scope (for Phase 1 / Phase 2)

- `tests-legacy.json` cross-lang verification. **Not dropped — deferred to Phase 3** as a standalone workflow rather than co-mounted in this matrix. Rationale: legacy tests pin to specific historical module versions, so the failure modes (version-pin drift vs cross-lang behavior drift) are different in kind from latest. Mixing them in one matrix would muddy the signal and double wall time before we even know if the latest matrix is stable. See Phase 3 below.
- Fixing the SHA-pinning gap in pipeline compose files. Tracked separately in [20260427-130000-fix-deploy-sha-pinning.md](20260427-130000-fix-deploy-sha-pinning.md). Phase 2 of this plan depends on that being done.
- Test code base-URL parameterization. The cross-lang matrix maps each system-lang to its own port set (java=31xx, dotnet=32xx, ts=33xx). If `system-test/<test-lang>/` test code hardcodes one port set in its config, those tests will hit "wrong" ports when pointed at a different system-lang. Likely needs base-URL injection via env var. **Defer until Phase 1 dry run reveals whether this is actually broken** — possible the test runner already reads URLs from `system.json` via the gh-optivem runner.

## Phase 1 — ship build-from-source workflow (mostly done)

**File:** [shop/.github/workflows/cross-lang-system-verification.yml](../.github/workflows/cross-lang-system-verification.yml)

**What it does:**
- Matrix: `arch × test-lang × system-lang` (2 × 3 × 3 = 18) minus 3 same-lang exclude rules (each spans both archs, so 6 entries) = **12 combos**
- Daily cron at 06:00 UTC, off-peak from per-lang stages
- `workflow_dispatch` with optional `commit-sha` (atomically pins source + compose + tests + system.json)
- Builds SUT from source via `gh optivem build system` against `docker/<system-lang>/<arch>/system.json` (which references `docker-compose.local.real.yml` — has `build:` directives, no GHCR pull)
- Runs full `tests-latest.json` from the test-lang's `system-test/` directory via `gh optivem test system`
- Stops SUT in `if: always()` finalizer

**Tradeoff vs pre-built images:**
- ✅ No GHCR coupling, no auth, no preflight, no digest resolution — clean isolation
- ✅ Atomically SHA-pinned via `checkout @ <sha>` (stricter than per-lang stages today)
- ❌ Rebuilds 3 systems (Java/Maven, .NET/dotnet, Node/npm) from scratch on every run, ~5–15 min per matrix entry overhead
- ❌ Tests fresh source build, not the artifact that will actually be released

**Items remaining for Phase 1:** _(none — Phase 1 complete pending verification CI run)_

## Phase 2 — switch to pre-built images

**Status (2026-04-28):** unblocked. The SHA-pinning prerequisite landed in `b638b6a5` — `deploy-docker-compose` honors the resolved digest and pipeline compose files accept `${SYSTEM_IMAGE:-…}`. Phase 2 can begin.

The cross-lang workflow should refactor to **pull pre-built `sha-<sha>` images** instead of building from source. This:

- ✅ Saves ~5–15 min per matrix entry (no rebuild)
- ✅ Tests the actual artifact that will be released, not a parallel source build
- ✅ Reuses the now-correct SHA-pinning machinery

**Refactor steps:** _(none — Phase 2 complete pending verification CI run)_

**Decisions made during Phase 2 implementation:**
- **Preflight `check-ghcr-packages-exist` deliberately omitted.** Cross-lang is intended to be invoked from `meta-prerelease-stage.yml` after per-(arch, lang) commit + acceptance stages have already produced the images. Missing images are a real error, not a "skip" condition — failing hard surfaces orchestration bugs immediately. Aligns with the project rule "GitHub Actions — `check-*` actions must NOT swallow errors": a `false` `exists` output would conflate "absent" with "couldn't tell" and bury misconfiguration.
- **Test execution uses `gh optivem test system --no-build --no-start`.** SUT lifecycle is owned by `deploy-docker-compose`; the runner just executes `setupCommands` + suites from `tests-latest.json`. Confirmed safe in [tests.go prepareSystem](../../gh-optivem/internal/runner/tests.go) — when `--no-start` is set, the runner probes `system.json` URLs to verify the system is up, then runs tests.

**Risk specific to Phase 2:**
- Cross-lang testing pre-built images means failures could indicate (a) genuine cross-lang behavior drift OR (b) drift between source HEAD and the published image. Minor confusion, manageable via good error messaging in the test summary.

**Meta-prerelease integration (done with Phase 2):** cross-lang is now invoked from `meta-prerelease-stage.yml` as a sibling job to `run`, gated by the same `check` step. Both run in parallel — cross-lang is a regression check and does NOT gate the meta-rc tag (release gating stays with the per-flavor acceptance stages inside `run`). Triggered with `commit-sha: ${{ github.sha }}` so it pulls `sha-<sha>` images for the same commit meta-prerelease is processing.

## Phase 3 — legacy cross-lang verification (open question, separate workflow)

**Status:** open question — do we want this at all?

If Phases 1–2 prove the cross-lang signal is valuable, consider extending coverage to `tests-legacy.json`. Two design rules:

1. **Separate workflow file** — `cross-lang-system-verification-legacy.yml`, not a `tests-config` matrix dimension on the existing workflow. Keeps the latest signal independent (a flaky legacy run must not bury a clean latest result).
2. **Separate cron** — distinct schedule from latest (e.g. weekly, or shifted off the daily latest run) to avoid runner contention and keep wall time bounded.

**Open questions to answer before starting Phase 3:**

- [ ] **Is legacy cross-lang parity a meaningful signal?** Legacy tests pin to specific historical module versions. A failure could mean (a) genuine cross-lang behavior drift OR (b) version-pin artifacts that have nothing to do with cross-lang. If it's mostly (b), the workflow is noise.
- [ ] **What's the right cadence?** Weekly is the strawman — legacy moves slowly, daily is overkill. Confirm after a few sample runs.
- [ ] **Does build-from-source even apply to legacy?** Phase 1's "build SUT from current SHA" semantics make less sense when the *test* config pins to historical module versions. May need to pull pre-built images for the pinned SHAs from GHCR (only viable post-Phase 2).

**Items remaining for Phase 3:**

- [ ] **Decide go/no-go after Phase 1 dry run** — if latest cross-lang fails for boring reasons (port mismatches, infra flake), there's no point extending to legacy until the basic mechanism is proven.
- [ ] **If go: copy `cross-lang-system-verification.yml` to `*-legacy.yml`**, swap `tests-latest.json` → `tests-legacy.json`, give it a separate cron, and revisit the build-vs-pull decision.

## Cron cadence

Phase 1 schedule: **daily at 06:00 UTC**. Rationale:
- Per-lang acceptance is hourly. Cross-lang is more expensive (15 combos vs 1) and lower-stakes (regression detection, not release gating).
- Daily is enough to catch drift within ~24h of introduction.
- Off-peak avoids contention with the hourly per-lang fleet.

If Phase 2 reduces wall time significantly (no rebuild), revisit cadence — could go to every-N-hours.

## Verification

End state: **one full workflow run completes with all 15 combos either green or with a clearly-categorized failure** (genuine drift / port mismatch / infra flake). Until that happens, neither phase is "done" — a never-run workflow is dead code.
