# Plan — Cross-Language System Verification Workflow

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-27T18:38:02Z`

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

## Phase 2 — switch to pre-built images (depends on [fix-deploy-sha-pinning](20260427-130000-fix-deploy-sha-pinning.md))

Once `deploy-docker-compose` honors the resolved digest and the pipeline compose files accept `${SYSTEM_IMAGE:-…}`, the cross-lang workflow should refactor to **pull pre-built `sha-<sha>` images** instead of building from source. This:

- ✅ Saves ~5–15 min per matrix entry (no rebuild)
- ✅ Tests the actual artifact that will be released, not a parallel source build
- ✅ Reuses the now-correct SHA-pinning machinery

**Refactor steps:**

- [ ] **Replace `gh optivem build system` + `gh optivem run system` with `optivem/actions/resolve-docker-image-digests@v1` + `optivem/actions/deploy-docker-compose@v1`** — same pattern the per-lang acceptance stages use today. Point at `docker-compose.pipeline.real.yml` (post-fix) instead of the `.local.` variant.
- [ ] **Add a preflight step** — call `check-ghcr-packages-exist` for the matrix entry's system-lang × arch image set. Skip the combo if images don't exist yet (e.g. for a freshly-scaffolded system that has not yet run commit-stage). Use `continue-on-error` or a per-combo gate, not a workflow-level abort.
- [ ] **Add GHCR / Docker Hub login** — copy the `Wandalen/wretry.action@v3` block from [monolith-java-acceptance-stage.yml](../.github/workflows/monolith-java-acceptance-stage.yml#L78-L97).
- [ ] **Keep `gh optivem test system` for the test-execution step only** — do not let it own SUT lifecycle (compose up/down) when we're using `deploy-docker-compose`. Verify the runner can be invoked in test-only mode (skim [gh-optivem/internal/runner](../../gh-optivem/internal/runner/) to confirm — currently `test system` reads `system.json` only for setup metadata, not for compose lifecycle).

**Risk specific to Phase 2:**
- Cross-lang testing pre-built images means failures could indicate (a) genuine cross-lang behavior drift OR (b) drift between source HEAD and the published image. Minor confusion, manageable via good error messaging in the test summary.

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
