# 2026-08-25 14:26:51 UTC — Acceptance-stage gate: a pinned-SHA dispatch must never silently skip

## TL;DR

**Why:** The acceptance-stage `check` gate asks a cron-mode question ("has anything new appeared since I last looked?") even when the workflow is dispatched with an explicit `commit-sha`. When the hourly cron run starts between the commit stage's image push and the pinned dispatch, the gate judges the pinned image "not newer", skips `run` and `publish-tag`, and no RC tag lands on the pinned SHA — so the caller's `Fail If No RC Tag On SHA` guard fires. That is exactly the silent-green-skip the repo's fail-loud rule prohibits.
**End result:** A dispatched, pinned-SHA acceptance-stage run either produces an RC tag on that exact SHA or fails loudly. The freshness heuristic applies only to cron runs. Cron behaviour is unchanged.

## Outcomes

What we get out of this — the goals and deliverables:

- `meta-prerelease-stage` no longer fails intermittently with "No rc tag matching '<prefix>-v<version>-rc.*' found on <sha>" caused by the cron/dispatch race.
- A pinned-SHA acceptance-stage dispatch has exactly two honest terminations: it runs and tags that SHA, or it fails. It can no longer skip into a false green.
- A pinned-SHA dispatch is idempotent: if an RC tag already exists on that SHA, it skips *for the right reason*, and the caller resolves the existing tag and passes.
- The 6 `-legacy` acceptance stages no longer silently skip verification of a pinned SHA (they publish no tag, so today the skip is invisible to the caller).
- The existing loud guard at `_prerelease-pipeline.yml:410-419` stays untouched — it was correct; the defect was upstream.

## ▶ Next executable step (resume here)

Edit `.github/workflows/monolith-typescript-acceptance-stage.yml` (the variant that actually failed) as the reference implementation:

1. In the `check` job, insert a new step immediately **before** `- name: Evaluate Run Gate` (currently at line 129):
   ```yaml
   - name: Resolve Existing RC Tag On SHA
     id: rc-on-sha
     uses: optivem/actions/resolve-latest-tag-from-sha@v1
     with:
       commit-sha: ${{ env.COMMIT_SHA }}
       pattern: monolith-typescript-v${{ steps.read-base-version.outputs.base-version }}-rc.*
   ```
   Requires `contents: read` on the `check` job — already present.
2. Rewrite the `skip-conditions` array so it reads:
   ```yaml
   skip-conditions: |
     [
       {"when": ${{ steps.check-tag.outputs.exists == 'true' }}, "reason": "Release in progress — version already tagged, awaiting post-release VERSION bump."},
       {"when": ${{ inputs.commit-sha != '' && steps.rc-on-sha.outputs.tag != '' }}, "reason": "RC tag already published for this commit — nothing to re-verify."},
       {"when": ${{ !inputs.commit-sha && steps.artifacts-changed.outputs.newer != 'true' && steps.test-changes.outputs.changed != 'true' }}, "reason": "No new artifacts since last run AND no test/workflow changes since last RC tag." }
     ]
   ```
   The only change to the third condition is the `!inputs.commit-sha &&` prefix. Leave the first condition alone.

Gate: stop after this one file, confirm the YAML parses and the diff is exactly the two edits above, then replicate to the other 11 files (Steps 2–3). This unblocks the whole plan — the remaining files are mechanical copies of the same shape.

## Steps

- [ ] Step 1: Apply the reference fix to `.github/workflows/monolith-typescript-acceptance-stage.yml` (new `rc-on-sha` step + rewritten `skip-conditions`), per the Next executable step above.
- [ ] Step 2: Replicate to the other 5 `*-acceptance-stage.yml` files — `monolith-dotnet`, `monolith-java`, `multitier-dotnet`, `multitier-java`, `multitier-typescript`. Substitute the per-variant tag prefix in the `pattern:` (`<arch>-<lang>`, e.g. `multitier-java-v...-rc.*`). Note the gate block sits at ~line 136 in the monolith files and ~line 138 in the multitier files; each file contains the skip-condition string exactly once, and each already has a `read-base-version` step id in the `check` job.
- [ ] Step 3: Apply **only** the `!inputs.commit-sha &&` guard (Step 1's item 2, third condition) to the 6 `*-acceptance-stage-legacy.yml` files. Do **not** add the `rc-on-sha` step there — those workflows publish no RC tag (no `compose-prerelease-version` / `publish-tag` job), so the idempotence condition has nothing to resolve.
- [ ] Step 4: Verify statically — run `actionlint` (or the repo's workflow lint) over all 12 changed files; then grep-confirm that all 12 contain `!inputs.commit-sha &&` on the freshness condition, that exactly 6 contain the `rc-on-sha` step, and that none of the 6 legacy files gained one.
- [ ] Step 5: Verify live — dispatch `prerelease-pipeline-monolith-typescript.yml` (or the full `meta-prerelease-stage`) with a pinned `commit-sha`, and confirm the acceptance stage actually runs for that SHA and publishes an RC tag on it, i.e. `Fail If No RC Tag On SHA` does not trigger. Deliberately overlapping the hourly cron makes this a real regression test for the race.
- [ ] Step 6: Commit the 12 workflow files via `/commit`.

## Notes — root cause evidence (for the executor)

- Failing run: <https://github.com/optivem/shop/actions/runs/32848640220> → downstream `prerelease-pipeline-monolith-typescript` run 32849970949 → job `verify / acceptance-stage`, step `Fail If No RC Tag On SHA`.
- From the `check` job of the dispatched acceptance-stage run 32850857809: `COMMIT_SHA=ebb7bd16` resolved correctly (image `sha-ebb7bd16…` pulled fine), but `since = 2026-08-25T12:52:23Z` — the start time of the **hourly cron** run 32850026370 — so `artifacts-changed.newer=false` (image pushed ~12:45). `test-changes` baselined on `monolith-typescript-v1.0.201-rc.2802` and found nothing. Gate skipped.
- Meanwhile that 12:52 cron run tested main HEAD `fd4920f3` and tagged `rc.2802` **there**, not on the pinned `ebb7bd16`.
- Not reproducible locally: it is a timing race between the hourly cron and the dispatched run — a CI-config defect, not a code defect.
- The `Release in progress` skip condition stays as-is: that case is already caught loudly upstream by the `check-version` job at `_prerelease-pipeline.yml:80-91`.

## Open questions

- Should a pinned-SHA dispatch *also* bypass the `Release in progress` skip (condition #1)? Left unchanged here on the reasoning that `check-version` already fails the pipeline loudly before acceptance-stage is reached. Revisit only if a real run shows the acceptance stage skipping for that reason under a pinned dispatch.
- Should the `check` job's `Get Last Workflow Run` / `check-timestamp-newer` steps be skipped entirely under a pinned dispatch (they compute values no longer consumed)? Cosmetic and token-cheap to leave in — proposed as a follow-up cleanup, not part of this fix.
