# 2026-10-13 08:17:41 UTC — Commit-stage job timeouts (fail fast on hung test suites)
> 🤖 **Picked up by agent** — `Valentina_Desk` at `2026-10-13T08:27:59Z`

## TL;DR

**Why:** gh-optivem acceptance run 38288447954 went red after 1h42m because a scaffolded `frontend-commit-stage` run hung for 6h in `Run Component Tests`. Vitest was wedged after a Pact write and its own 30s timeouts never fired. None of shop's 8 commit-stage workflows set `timeout-minutes`, so an intermittent hang burns GitHub's 6h job limit and the full gh-optivem watch+poll budget instead of failing loudly.
**End result:** every commit-stage `run` job (and the equivalent suite-running jobs/steps in `_prerelease-pipeline.yml`) has an explicit `timeout-minutes` with headroom over its green duration. A hang now fails in minutes with a clear "exceeded timeout" cause, and the scaffolded student repos inherit that. The underlying intermittent Pact hang is investigated separately, with no retries that mask it.

## Outcomes

- A hung test suite in any commit stage fails within ~30 min instead of 6h, and gh-optivem reports a real failure instead of "never reported a terminal state".
- All 8 commit-stage workflows (monolith dotnet/java/typescript, multitier backend clean-java/dotnet/java/typescript, multitier frontend-react) have parity on the timeout.
- Scaffolded repos created by gh-optivem carry the timeout (verified, not assumed).
- A written finding on the intermittent pact-ffi hang in `PactBackendStubDriver`: upstream issue or pact-core upgrade, or "no known cause" with the evidence.

## ▶ Next executable step (resume here)

Step 6: confirm one green `multitier-frontend-react-commit-stage` run in shop (triggered by the Step 5b commit). Then remove this plan; the operator Verification item (gh-optivem `gh-acceptance-stage` re-run) is outside this repo.

## Measurements (Steps 1 + 3, done 2026-10-13)

Max whole-run duration (min) over the last 5 green runs (upper bound on the job), and the chosen job timeout (>= 2x, default 30):

| Workflow | Max green | `timeout-minutes` |
|---|---|---|
| monolith-dotnet-commit-stage | 16 | 40 |
| monolith-java-commit-stage | 21 | 45 |
| monolith-typescript-commit-stage | 9 | 30 |
| multitier-backend-clean-java-commit-stage | 3 | 30 |
| multitier-backend-dotnet-commit-stage | 18 | 40 |
| multitier-backend-java-commit-stage | 20 | 45 |
| multitier-backend-typescript-commit-stage | 15 | 30 |
| multitier-frontend-react-commit-stage | 20 | 45 |
| `*-acceptance-stage.yml` / `-legacy.yml` `run` job (12 files) | 40 | 90 |
| `*-acceptance-stage-cloud.yml` `test-*` jobs (6 files, no green history) | n/a | 30 |
| `_prerelease-pipeline.yml` `local` job | 8 (job-level) | 45 |

QA stages (`*-qa-stage*.yml`) only deploy, run no suites, so they're out of scope.

## Steps

- [ ] Step 6: Confirm one green `multitier-frontend-react-commit-stage` run in shop. (actionlint clean and timeouts pushed on 2026-10-13.)

## Pact hang finding (Step 5, 2026-10-13)

- Versions: `@pact-foundation/pact` 13.2.0, `@pact-foundation/pact-core` 15.2.1. Latest are pact 17.1.4 and pact-core 20.2.0.
- Upstream: no relevant issue or fix found. Searched for PactV3 executeTest / writePact / mock-server / pact_ffi hang or deadlock and vitest-never-exits. The pact-core changelog from 15.2.1 to 20.2.0 has no hang or shutdown fix. pact-js #1220 and pact-core #510 are unrelated. So no bump.
- Likely cause is in our code. `release` is only assigned inside the `executeTest` callback (driver :59-61).
  - If a test times out before the mock server is up, `finish()` calls `this.release?.()` (:82) while `release` is still undefined, then `await this.run` (:83) waits forever.
  - After the 30s `hookTimeout` (`vite.config.ts:17`), `reset()` (`component-harness.ts:39`, driver :101-108) drops `release` without calling it, which leaks a parked run with a live native mock server.
  - Fixed 2026-10-13: deferred now created before `executeTest`, `reset()` releases a parked run. No retries. `npm run test:component` green 3/3 locally.

## Verification

- Operator: re-run gh-optivem `gh-acceptance-stage` and confirm the `Smoke (multitier, multirepo, java)` leg is green.

## Open questions

None. All resolved during Steps 1 and 3 (see Measurements).
