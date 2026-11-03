# 2026-09-17 19:49:00 UTC — Commit stage should survive a SonarCloud outage

## TL;DR

**Why:** A SonarCloud outage on 2026-09-17 (~19:30-19:45 UTC) failed the `Run Code Analysis` / `End Sonar Analysis` steps in `multitier-frontend-react-commit-stage.yml:149` and `multitier-backend-dotnet-commit-stage.yml:138`, which failed the whole `prerelease-pipeline-multitier-dotnet` run 35264500002 twice — before the acceptance stage even started. The steps already wrap the scanner in `optivem/actions/retry@v1`, and the retry worked as designed: 4 attempts, 5s/15s/45s backoff, then `exhausted 4 attempts`. Every attempt got `Error 500 on https://sonarcloud.io/batch/project.protobuf`. A ~65-second envelope cannot ride out a multi-minute vendor outage, so a green build is blocked on SonarCloud's availability.
**End result:** A SonarCloud outage no longer decides whether a commit stage can pass, by whichever policy is chosen below. The `optivem/actions` `v1` re-point moved to its own plan, `20261103-1042-optivem-actions-retry-release.md`.

## Outcomes

- A multi-minute SonarCloud outage does not fail a commit stage (policy per the decision in Open questions).
- Genuine Sonar findings and quality-gate failures still fail the stage — an outage is distinguished from a verdict.
- The same treatment applies to every Sonar-invoking step, not just the two that happened to fail: 8 commit-stage steps plus the 6 `Run Sonar Analysis` steps in the acceptance stages.

## ▶ Next executable step (resume here)

All decisions are resolved (2026-11-03). Run `/refine-plan` on this file to write concrete per-file edits for Step 2, then `/execute-plan`. All work is in **shop** only: no changes to `optivem/actions` (the `v1` re-point and the retry inputs moved to `20261103-1042-optivem-actions-retry-release.md`). Implement the outage rule in shop's workflows around the existing `optivem/actions/retry@v1` calls, e.g. by detecting `exhausted N attempts` against a 5xx.

## Steps

- [ ] Step 2: (Depends on the policy decision.) Apply the chosen change to every Sonar-invoking step — `Run Code Analysis` / `Begin Sonar Analysis` / `Build for Sonar` / `End Sonar Analysis` in the commit stages, and `Run Sonar Analysis` in the six acceptance stages. Keep the files structurally identical across languages for `workflow-comparator` parity.
- [ ] Step 4: Verify. `actionlint` on the changed workflows; then re-run a real commit stage and confirm the Sonar step behaves as decided.

## Resolved decisions

1. **What a vendor outage does to the build: (b) non-blocking on transient failure only.** The step reports a warning and the stage continues when the scanner exhausts retries against a 5xx, but still fails on a quality-gate verdict or a real scanner error. Chosen over widening the envelope because the 2026-09-17 outage outlasted any reasonable envelope, and the retry layer already distinguishes exhaustion (`exhausted N attempts`, `retry-core.sh:161`) from a verdict. Accepted cost: some commits land without analysis.

2. **Who notices the skipped analysis: a job-summary warning** (decided 2026-11-03).
3. **Acceptance stages: same policy as commit stages** (decided 2026-11-03).
4. **Repo scope: shop only** (decided 2026-11-03). Anything that would need `optivem/actions` goes to `20261103-1042-optivem-actions-retry-release.md`.
