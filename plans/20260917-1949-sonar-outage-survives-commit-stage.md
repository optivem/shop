# 2026-09-17 19:49:00 UTC — Commit stage should survive a SonarCloud outage

## TL;DR

**Why:** A SonarCloud outage on 2026-09-17 (~19:30-19:45 UTC) failed the `Run Code Analysis` / `End Sonar Analysis` steps in `multitier-frontend-react-commit-stage.yml:149` and `multitier-backend-dotnet-commit-stage.yml:138`, which failed the whole `prerelease-pipeline-multitier-dotnet` run 35264500002 twice — before the acceptance stage even started. The steps already wrap the scanner in `optivem/actions/retry@v1`, and the retry worked as designed: 4 attempts, 5s/15s/45s backoff, then `exhausted 4 attempts`. Every attempt got `Error 500 on https://sonarcloud.io/batch/project.protobuf`. A ~65-second envelope cannot ride out a multi-minute vendor outage, so a green build is blocked on SonarCloud's availability.
**End result:** A SonarCloud outage no longer decides whether a commit stage can pass, by whichever policy is chosen below. Separately, `optivem/actions` `v1` is re-pointed so consumers pick up the retry classification fix that is currently unreleased.

## Outcomes

- A multi-minute SonarCloud outage does not fail a commit stage (policy per the decision in Open questions).
- Genuine Sonar findings and quality-gate failures still fail the stage — an outage is distinguished from a verdict.
- `optivem/actions` `v1` includes `cd0432c` "Classify retry failures on messages, not stack frames", so every consumer's retry envelope classifies SonarCloud HTTP failures on the message instead of on a stack frame.
- The same treatment applies to every Sonar-invoking step, not just the two that happened to fail: 8 commit-stage steps plus the 6 `Run Sonar Analysis` steps in the acceptance stages.

## ▶ Next executable step (resume here)

Blocked on decisions — do not start editing. The Open questions below choose between widening the retry envelope, making Sonar non-blocking, and leaving it as is; the steps depend on which. Once decided, come back with `/refine-plan plans/20260917-1949-sonar-outage-survives-commit-stage.md` to write the concrete steps, then `/execute-plan`.

Note on repo scope: most of the work lands in the sibling `optivem/actions` checkout (`shared/retry*.sh`, `retry/action.yml`, the `v1` tag), not in shop. This file lives in shop because that is where the failure surfaced; move it to `actions/plans/` if you prefer it next to the code it changes.

## Steps

- [ ] Step 1: Re-point `optivem/actions` `v1` to include `cd0432c` (currently `v1` = `d02d32b`, exactly one commit behind `origin/main`). Use the repo's own `update-v1.yml` workflow rather than moving the tag by hand. This is independent of the policy decision and valuable on its own: without it, any SonarCloud HTTP failure whose trace passes through `DefaultScannerWsClient.call` is classified by stack frame and can be treated as a hard fail. Verify afterwards that a consumer run resolves `optivem/actions@v1` to the new SHA.
- [ ] Step 2: (Depends on the policy decision.) Apply the chosen change to every Sonar-invoking step — `Run Code Analysis` / `Begin Sonar Analysis` / `Build for Sonar` / `End Sonar Analysis` in the commit stages, and `Run Sonar Analysis` in the six acceptance stages. Keep the files structurally identical across languages for `workflow-comparator` parity.
- [ ] Step 3: If the decision needs per-call tuning, add `attempts` and `delays` inputs to `optivem/actions/retry@v1` (`retry/action.yml` exposes neither today; `shared/retry.sh:43` hardcodes `_RETRY_DELAYS=(5 15 45)` and `retry-core.sh:49` `_RETRY_CORE_ATTEMPTS=4`). Cover the new inputs in `shared/_test-retry.sh`, then release via `update-v1.yml`.
- [ ] Step 4: Verify. `actionlint` on the changed workflows; run the actions repo's `commit-stage.yml` shell tests; then re-run a real commit stage and confirm the Sonar step behaves as decided.

## Open questions

Decide before execution — each option is a different policy, not an implementation detail:

1. **What should a vendor outage do to the build?**
   - **(a) Widen the retry envelope** — e.g. 6 attempts with 30/60/120/240s backoff, roughly 8 minutes of patience instead of 65 seconds. Keeps Sonar blocking and green means analyzed. Costs runner minutes while a real outage is in progress, and still fails on an outage longer than the envelope. Today's outage lasted at least 15 minutes, so this alone would not have saved run 35264500002.
   - **(b) Make Sonar non-blocking on transient failure only** — the step reports a warning and the stage continues when the scanner exhausts retries against a 5xx, but still fails on a quality-gate verdict or a real scanner error. Survives any outage length. Needs the exhaustion case to be distinguishable from a verdict, which the retry envelope already knows (`exhausted N attempts` versus a clean non-zero verdict), and means some commits land without analysis.
   - **(c) Status quo** — outages are rare, re-run by hand. Zero work beyond Step 1.
2. **If (b): who notices the skipped analysis?** Options: a job summary warning only, a follow-up issue, or a scheduled re-scan of `main`. Unresolved.
3. **Should the acceptance stages' `Run Sonar Analysis` follow the same policy as the commit stages, or stay blocking?** They gate RC tagging, so the answer may differ.
