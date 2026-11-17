# 2026-08-25 14:26:51 UTC — Acceptance-stage gate: a pinned-SHA dispatch must never silently skip

## TL;DR

**Why:** The acceptance-stage `check` gate asks a cron-mode question ("has anything new appeared since I last looked?") even when the workflow is dispatched with an explicit `commit-sha`. When the hourly cron run starts between the commit stage's image push and the pinned dispatch, the gate judges the pinned image "not newer", skips `run` and `publish-tag`, and no RC tag lands on the pinned SHA — so the caller's `Fail If No RC Tag On SHA` guard fires. That is exactly the silent-green-skip the repo's fail-loud rule prohibits.
**End result:** A dispatched, pinned-SHA acceptance-stage run either produces an RC tag on that exact SHA or fails loudly. The freshness heuristic applies only to cron runs. Cron behaviour is unchanged.

## Outcomes

What we get out of this — the goals and deliverables:

- `meta-prerelease-stage` no longer fails intermittently with "No rc tag matching '<prefix>-v<version>-rc.*' found on <sha>" caused by the cron/dispatch race.
- A pinned-SHA acceptance-stage dispatch has exactly two honest terminations: it runs and tags that SHA, or it fails. It can no longer skip into a false green.
- A pinned dispatch of a SHA that already has an RC tag re-tests it and adds a higher RC tag. That is harmless: the caller resolves the highest RC on the SHA (see below).
- The legacy acceptance stages no longer silently skip verification of a pinned SHA.
- The loud `Fail If No RC Tag On SHA` guard stays as it is; it was correct, the defect was upstream.

## Current state (reviewed 2026-11-17, meta-plan U3)

The single-pipeline move rewrote the files this plan targeted. The 12 per-variant `*-acceptance-stage.yml` / `*-acceptance-stage-legacy.yml` files it named are gone. Today there are 18 thin callers (`<arch>-<lang>-acceptance-stage.yml`, `-acceptance-stage-docker-compose.yml`, `-acceptance-stage-docker-compose-legacy.yml`) of two reusable workflows, each with one `check` job:

- `.github/workflows/_acceptance-stage.yml` (cloud; owns the RC tag): `Evaluate Run Gate` at ~line 216.
- `.github/workflows/_acceptance-stage-docker-compose.yml` (docker compose gate, `suite-set` latest or legacy; creates no RC tag): `Evaluate Run Gate` at ~line 202.

What is already done there:

- **The core fix (old Steps 1–4, item 2, third condition) is in both files.** The freshness skip reads `{"when": ${{ inputs.commit-sha == '' && steps.artifacts-changed.outputs.newer != 'true' && steps.test-changes.outputs.changed != 'true' }}, ...}` (`_acceptance-stage.yml:223`, `_acceptance-stage-docker-compose.yml:209`; added in `b893a9cc` with the reusable workflows). A pinned dispatch can no longer take the cron-style "nothing new" skip. Because the legacy callers use the same `_acceptance-stage-docker-compose.yml` `check` job (with `suite-set: legacy`), they carry the same guard (old Step 3).
- **The loud guard moved.** `Fail If No RC Tag On SHA` is no longer at `_prerelease-pipeline.yml:410-419`. It is now the `verify-rc-tag` job in `_acceptance-stage.yml` (~lines 816–848), and `_prerelease-pipeline.yml`'s `cloud-acceptance-stage` job resolves the RC with `Resolve RC Tag On SHA` (~line 394) after dispatching `<prefix>-acceptance-stage.yml` with `commit-sha`.
- **The race source today:** the hourly `cron: '0 * * * *'` is active only on the six `*-acceptance-stage-docker-compose.yml` callers (the cloud callers have it commented out). `_prerelease-pipeline.yml`'s `acceptance-stage` job dispatches those with `commit-sha`, so the guard above is what protects it.

## Resolved decisions

- **`rc-on-sha` idempotence skip (old Step 1, item 1 + new second skip condition): dropped** (U3 review, 2026-11-17; cheap to reverse). `verify-rc-tag`'s comment in `_acceptance-stage.yml` records the current design: "A re-run re-tests the SHA and adds a higher rc tag; we resolve the highest, so that is harmless." That is honest (it never skips into a false green), so adding a skip would only save CI minutes on a rare re-dispatch. `_acceptance-stage-docker-compose.yml` publishes no RC tag, so the condition has nothing to resolve there.
- **Pinned dispatch and the `Release in progress` skip (condition #1): left unchanged.** `check-version` in `_prerelease-pipeline.yml` (~line 64) already fails the pipeline loudly before the acceptance stage is reached. Revisit only if a real run shows the acceptance stage skipping for that reason under a pinned dispatch.
- **Skipping `Get Last Workflow Run` / `check-timestamp-newer` under a pinned dispatch: not done.** Cosmetic; the values are cheap and the same `check` job serves the cron path.

## ▶ Next executable step (resume here)

Meta-plan unit U4 (Lane W, after U2 is pushed: Conflict 2). No workflow edit is left; only the live check. **Step 1:** dispatch `prerelease-pipeline-monolith-typescript.yml` with a pinned `commit-sha` (main HEAD) a few minutes before the top of the hour, so the hourly `monolith-typescript-acceptance-stage-docker-compose.yml` cron run overlaps it. Confirm that the docker-compose acceptance run for that SHA does not skip at `Evaluate Run Gate`, that the cloud `monolith-typescript-acceptance-stage.yml` run's `verify-rc-tag` finds an RC tag on the SHA, and that the pipeline is green. If it passes, delete this plan.

## Steps

- [ ] Step 1: Verify live, as in the Next executable step above. If the docker-compose or cloud stage still skips or `Fail If No RC Tag On SHA` fires, capture the run ids and reopen the gate analysis with the current `check` job (the notes below describe the original failure).

## Notes — root cause evidence (2026-08-25, pre-single-pipeline; file names are historical)

- Failing run: <https://github.com/optivem/shop/actions/runs/32848640220> → downstream `prerelease-pipeline-monolith-typescript` run 32849970949 → job `verify / acceptance-stage`, step `Fail If No RC Tag On SHA`.
- From the `check` job of the dispatched acceptance-stage run 32850857809: `COMMIT_SHA=ebb7bd16` resolved correctly (image `sha-ebb7bd16…` pulled fine), but `since = 2026-08-25T12:52:23Z` — the start time of the **hourly cron** run 32850026370 — so `artifacts-changed.newer=false` (image pushed ~12:45). `test-changes` baselined on `monolith-typescript-v1.0.201-rc.2802` and found nothing. Gate skipped.
- Meanwhile that 12:52 cron run tested main HEAD `fd4920f3` and tagged `rc.2802` **there**, not on the pinned `ebb7bd16`.
- Not reproducible locally: it is a timing race between the hourly cron and the dispatched run — a CI-config defect, not a code defect.
