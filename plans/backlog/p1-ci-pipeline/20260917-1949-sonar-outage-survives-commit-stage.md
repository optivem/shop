# 2026-09-17 19:49:00 UTC — Commit stage should survive a SonarCloud outage

## TL;DR

**Why:** A SonarCloud outage on 2026-09-17 (~19:30-19:45 UTC) failed the `Run Code Analysis` / `End Sonar Analysis` steps in `multitier-frontend-react-commit-stage.yml:149` and `multitier-backend-dotnet-commit-stage.yml:138`, which failed the whole `prerelease-pipeline-multitier-dotnet` run 35264500002 twice — before the acceptance stage even started. The steps already wrap the scanner in `optivem/actions/retry@v1`, and the retry worked as designed: 4 attempts, 5s/15s/45s backoff, then `exhausted 4 attempts`. Every attempt got `Error 500 on https://sonarcloud.io/batch/project.protobuf`. A ~65-second envelope cannot ride out a multi-minute vendor outage, so a green build is blocked on SonarCloud's availability.
**End result:** A SonarCloud outage no longer decides whether a commit stage can pass, by whichever policy is chosen below. The `optivem/actions` `v1` re-point moved to its own plan, `20261103-1042-optivem-actions-retry-release.md`.

## Outcomes

- A multi-minute SonarCloud outage does not fail a commit stage (policy per the decision in Open questions).
- Genuine Sonar findings and quality-gate failures still fail the stage — an outage is distinguished from a verdict.
- The same treatment applies to every Sonar-invoking step, not just the two that happened to fail: 8 commit-stage steps plus the 6 `Run Sonar Analysis` steps in the acceptance stages.

## ▶ Next executable step (resume here)

The commit stages are done (2026-11-04, local commit, not pushed during the GitHub incident): the shop-local action `.github/actions/sonar-scan` wraps `optivem/actions/retry@v1` and passes with a warning annotation plus a job-summary warning only when the retry wrapper gave up after all its attempts and the last one failed on an HTTP 5xx. Every other failure still fails the step. All 7 commit stages use it; in the two .NET ones, `Build for Sonar` and `End Sonar Analysis` are skipped when `Begin Sonar Analysis` was skipped. Next: the acceptance stages (Step 2 below), then the CI proof (Step 4).

Note: none of shop's scanners waits for the quality gate (no `sonar.qualitygate.wait`), so a "quality-gate failure" is not a step failure today. Any scanner failure that is not a 5xx outage (4xx/auth, bad project key, build error) still fails.

## Steps

- [ ] Step 2: Replace `optivem/actions/retry@v1` with `./.github/actions/sonar-scan` (`label: Run Sonar Analysis`) in the `Run Sonar Analysis` step of the acceptance stages: `_acceptance-stage.yml`, `_acceptance-stage-docker-compose.yml`, and `{monolith-dotnet,monolith-java,monolith-typescript,multitier-dotnet,multitier-typescript}-acceptance-stage.yml`. Keep the files structurally identical across languages for `workflow-comparator` parity.
- [ ] Step 4: Verify in CI after the push. `actionlint` is green locally, and the action's logic was simulated locally against the real `retry@v1` scripts (5xx outage → skipped with a warning; 401, build error, timeouts only, and 500-then-401 → fail; 500-then-success → pass). In CI: confirm that one Java, one .NET and one TypeScript commit stage still run analysis green (`skipped=false`, no warning), and one acceptance stage too. The outage branch can only be proven by a real SonarCloud outage, or by a throwaway dispatch whose `sonar.host.url` points at a host that always answers 503.

## Resolved decisions

1. **What a vendor outage does to the build: (b) non-blocking on transient failure only.** The step reports a warning and the stage continues when the scanner exhausts retries against a 5xx, but still fails on a quality-gate verdict or a real scanner error. Chosen over widening the envelope because the 2026-09-17 outage outlasted any reasonable envelope, and the retry layer already distinguishes exhaustion (`exhausted N attempts`, `retry-core.sh:161`) from a verdict. Accepted cost: some commits land without analysis.

2. **Who notices the skipped analysis: a job-summary warning** (decided 2026-11-03).
3. **Acceptance stages: same policy as commit stages** (decided 2026-11-03).
4. **Repo scope: shop only** (decided 2026-11-03). Anything that would need `optivem/actions` goes to `20261103-1042-optivem-actions-retry-release.md`.
