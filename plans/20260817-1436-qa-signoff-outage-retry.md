# 2026-08-17 14:36:00 UTC — Retry `qa-signoff` dispatch on transient GitHub API outages

## TL;DR

**Why:** Run [32030493529](https://github.com/optivem/shop/actions/runs/32030493529) (`meta-prerelease-stage`, 2026-08-17) failed on 4 of 6 per-language pipelines — multitier-java, multitier-typescript, multitier-dotnet, monolith-typescript — all at the same `qa-signoff` job in `.github/workflows/_prerelease-pipeline.yml:451-463`. Root cause is a transient GitHub API outage (~12:44–13:50 UTC) that exhausted the fixed retry budgets inside the external `optivem/actions` composite actions: `create-commit-status@v1`'s `gh` call hit HTTP 502/503 ("No server is currently available") for java/typescript/monolith-typescript (e.g. job `95408405254` in run `32036649748`), and `trigger-and-wait-for-workflow@v1`'s own action-bundle download hit HTTP 429 from codeload.github.com for multitier-dotnet (job `95405451653` in run `32031346421`). This is an external infra flake, not a shop code defect, confirmed by two unrelated GH API surfaces failing together across otherwise-independent pipelines, and it is not reproducible locally. The exhausted retry budgets live in `optivem/actions` (external repo, out of scope here) — the actionable, shop-side mitigation is to retry the whole `qa-signoff` dispatch once so a short blip doesn't fail an entire multi-hour, already-mostly-green pipeline.
**End result:** `_prerelease-pipeline.yml`'s `qa-signoff` job retries its `trigger-and-wait-for-workflow@v1` dispatch once on failure before failing the job, for every language/flavor (the file is shared via `inputs.prefix`), without touching the other, more expensive dispatch sites in the same file.

## Outcomes

- A single transient GitHub API blip during `qa-signoff` (Statuses API 502/503, or codeload 429 downloading the action bundle) no longer fails the whole `qa-signoff` job — it retries the ~1-3 minute `<prefix>-qa-signoff.yml` dispatch once and only fails if the retry also fails.
- The fix applies uniformly to all languages/flavors (Java, .NET, TypeScript × monolith, multitier) since `_prerelease-pipeline.yml` is shared and parameterized by `inputs.prefix` — no per-language duplication.
- The other 4 `trigger-and-wait-for-workflow` call sites in the same file (`acceptance-stage`, `acceptance-stage-legacy`, `qa-stage`) are explicitly left unchanged — they're 20-40 minute stages where blind retry-on-failure risks doubling cost on a blip, and this incident didn't hit them.

## ▶ Next executable step (resume here)

Edit `.github/workflows/_prerelease-pipeline.yml`'s `qa-signoff` job (currently lines 451-463): split the single `uses: optivem/actions/trigger-and-wait-for-workflow@v1` step into two identical steps — first with `id: attempt` + `continue-on-error: true`, second with `if: steps.attempt.outcome == 'failure'` — both dispatching `${{ inputs.prefix }}-qa-signoff.yml` with the same `poll-interval: '90'` and `workflow-inputs`. See Step 1 below for the exact diff.

## Steps

- [ ] Step 1: In `.github/workflows/_prerelease-pipeline.yml`, replace the `qa-signoff` job's single step with:
  ```yaml
      steps:
        - id: attempt
          continue-on-error: true
          uses: optivem/actions/trigger-and-wait-for-workflow@v1
          with:
            workflow: ${{ inputs.prefix }}-qa-signoff.yml
            poll-interval: '90'
            workflow-inputs: '{"version": "${{ needs.acceptance-stage.outputs.version }}", "result": "approved"}'
        - if: steps.attempt.outcome == 'failure'
          uses: optivem/actions/trigger-and-wait-for-workflow@v1
          with:
            workflow: ${{ inputs.prefix }}-qa-signoff.yml
            poll-interval: '90'
            workflow-inputs: '{"version": "${{ needs.acceptance-stage.outputs.version }}", "result": "approved"}'
  ```
  Re-dispatching `<prefix>-qa-signoff.yml` is safe/idempotent — it only re-sets the `qa/signoff` commit status to the same value.
- [ ] Step 2: Verify the YAML is well-formed (e.g. `actionlint` or a GitHub Actions workflow syntax check) and that the job structure is otherwise unchanged. No `compile-all.sh` or system-test run is needed — this change touches only orchestration YAML, not application or test code.
- [ ] Step 3: Note for the user: real confidence only comes from watching the next scheduled/triggered `meta-prerelease-stage` run to confirm `qa-signoff` survives a first-attempt blip (or continues to pass normally when there's no blip at all). This is an observational follow-up, not a blocking gate for this plan.

## Open questions

- None — scope, fix shape, and non-goals were settled during diagnosis in `/fix-bug`.
