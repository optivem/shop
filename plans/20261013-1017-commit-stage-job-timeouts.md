# 2026-10-13 08:17:41 UTC — Commit-stage job timeouts (fail fast on hung test suites)

## TL;DR

**Why:** gh-optivem acceptance run 38288447954 went red after 1h42m because a scaffolded `frontend-commit-stage` run hung for 6h in `Run Component Tests`. Vitest was wedged after a Pact write and its own 30s timeouts never fired. None of shop's 8 commit-stage workflows set `timeout-minutes`, so an intermittent hang burns GitHub's 6h job limit and the full gh-optivem watch+poll budget instead of failing loudly.
**End result:** every commit-stage `run` job (and the equivalent suite-running jobs/steps in `_prerelease-pipeline.yml`) has an explicit `timeout-minutes` with headroom over its green duration. A hang now fails in minutes with a clear "exceeded timeout" cause, and the scaffolded student repos inherit that. The underlying intermittent Pact hang is investigated separately, with no retries that mask it.

## Outcomes

- A hung test suite in any commit stage fails within ~30 min instead of 6h, and gh-optivem reports a real failure instead of "never reported a terminal state".
- All 8 commit-stage workflows (monolith dotnet/java/typescript, multitier backend clean-java/dotnet/java/typescript, multitier frontend-react) have parity on the timeout.
- Scaffolded repos created by gh-optivem carry the timeout (verified, not assumed).
- A written finding on the intermittent pact-ffi hang in `PactBackendStubDriver`: upstream issue or pact-core upgrade, or "no known cause" with the evidence.

## ▶ Next executable step (resume here)

Step 1: measure green durations. Run one `gh run list --repo optivem/shop --workflow <wf> --status success --limit 5 --json databaseId,createdAt,updatedAt` per commit-stage workflow (one call each, sleeping between calls is not needed). Record the max `run` job duration per workflow in this plan, then pick each `timeout-minutes` value (default 30, at least 2x the max green duration). Then do Step 2.

## Steps

- [ ] Step 1: Measure the green `run` job duration for each of the 8 `.github/workflows/*-commit-stage.yml` and pick a per-workflow `timeout-minutes` (default 30, at least 2x the slowest recent green run). Record the numbers here.
- [ ] Step 2: Add `timeout-minutes: <n>` to the `run` job of all 8 commit-stage workflows (e.g. `multitier-frontend-react-commit-stage.yml:52`), each with a one-line comment: a hung suite (e.g. intermittent Pact native hang) must fail fast instead of burning GitHub's 6h job limit.
- [ ] Step 3: Parity sweep. Check `_prerelease-pipeline.yml` (e.g. ~line 325 "Run Component Tests (multitier frontend)") and the acceptance/qa stage workflows for jobs running test suites without `timeout-minutes`, and add them with the same rule.
- [ ] Step 4: Confirm gh-optivem's scaffolding carries the change. Find how gh-optivem copies/renders shop's commit-stage workflows (scaffolded name e.g. `frontend-commit-stage.yml`). Check whether any gh-optivem template-rendering code or golden/snapshot tests pin workflow contents and would strip or need the new key, and update those in gh-optivem if needed.
- [ ] Step 5: Investigate the intermittent hang in `system/multitier/frontend-react/src/test/support/driver/adapter/pact-backend-stub-driver.ts` (PactV3 `executeTest`, held open on a deferred until `finish()`). Search upstream pact-js / pact-js-core issues for executeTest/writePact/mock-server hangs at @pact-foundation/pact 13 / pact-core 15.2.1, and check for a newer pact-core with a fix. If a fix exists, bump it and run `npm run test:component` locally (after `npm ci`) several times. Do not add retries. Record the finding in this plan.
- [ ] Step 6: Verify. Lint the edited workflows (actionlint if available, else a YAML parse), push, and confirm one green `multitier-frontend-react-commit-stage` run in shop.

## Verification

- Operator: re-run gh-optivem `gh-acceptance-stage` and confirm the `Smoke (multitier, multirepo, java)` leg is green.

## Open questions

- Timeout value: 30 min is the default. Step 1's measurements may justify a different value per workflow (inferred; not stated by the user).
- Scope of Step 3: whether acceptance/qa stage workflows already have their own timeouts is unchecked. Include them only if they run suites without one.
