# 2026-09-17 14:04:00 UTC — Pinned-SHA acceptance-stage runs must not skip

## TL;DR

**Why:** When the prerelease pipeline dispatches `<prefix>-acceptance-stage.yml` pinned to a commit, the check gate can skip with "No new artifacts since last run AND no test/workflow changes since last RC tag". The gate compares the pinned `sha-<sha>` image date with the last run of the workflow for *any* commit, and compares test changes with the newest RC tag, which can sit on a *later* commit. The stage succeeds without tagging, so the pipeline's `Fail If No RC Tag On SHA` step (`.github/workflows/_prerelease-pipeline.yml:410-418`) fails. This happened in meta run 35212665594 → child run 35229886589. It is deterministic: pinned dispatches 35220930239, 35228933071 and 35229916617 all skipped the same way.
**End result:** A pinned acceptance-stage run either runs the tests and publishes an RC tag on that commit, or skips only because an RC tag already exists on that commit. The prerelease pipeline no longer fails because an unrelated scheduled run tested a newer `latest`.

## Outcomes

- A pinned dispatch whose images are older than the last acceptance run still runs the tests and tags the pinned commit.
- Pinned dispatches stay idempotent: re-dispatching an already-tagged commit skips with an "RC tag already exists on pinned SHA" reason, and the pipeline's `resolve-latest-tag-from-sha` finds that tag.
- Unpinned (scheduled or `latest`) runs keep today's staleness skip unchanged.
- All six tag-publishing acceptance-stage workflows have structurally identical check jobs.
- The "race-tolerant with the hourly cron" comment at `_prerelease-pipeline.yml:401-403` describes the actual guarantee.

## ▶ Next executable step (resume here)

Step 1: edit the `check` job in `.github/workflows/multitier-dotnet-acceptance-stage.yml` (gate at lines 131-139). Insert an `Existing RC Tag On Pinned SHA` step (id `pinned-rc-tag`) after `Detect Test Changes Since Last RC`. Give it `if: inputs.commit-sha != ''` and `uses: optivem/actions/resolve-latest-tag-from-sha@v1` with `commit-sha: ${{ env.COMMIT_SHA }}` and `pattern: multitier-dotnet-v${{ steps.read-base-version.outputs.base-version }}-rc.*`. In `Evaluate Run Gate`, AND `inputs.commit-sha == ''` into the "No new artifacts…" condition. Then add `{"when": ${{ inputs.commit-sha != '' && steps.pinned-rc-tag.outputs.tag != '' }}, "reason": "RC tag already exists on pinned SHA."}`. Stop at that file for review; it becomes the template for Step 2.

## Steps

- [ ] Step 1: Fix the gate in `.github/workflows/multitier-dotnet-acceptance-stage.yml` as described above. Check the `resolve-latest-tag-from-sha` action's inputs and outputs, and how it handles an empty result versus an error. Per the check-* rule, an API or auth failure must fail loudly, not return an empty tag.
- [ ] Step 2: Apply the same change to `multitier-{java,typescript}-acceptance-stage.yml` (gate line 138) and `monolith-{dotnet,java,typescript}-acceptance-stage.yml` (gate line 136), changing only the tag prefix.
- [ ] Step 3: Check the `-cloud` variants (`*-acceptance-stage-cloud.yml`, which have `rc-version`). If any pipeline dispatches them pinned and requires a tag on that commit, apply the same fix. Otherwise note why they are out of scope. `-legacy` variants publish no RC tag and are out of scope.
- [ ] Step 4: Update the comment at `.github/workflows/_prerelease-pipeline.yml:401-403` to say that pinned runs never skip unless the commit is already tagged, so tolerance of the cron no longer depends on the cron tagging the same commit.
- [ ] Step 5: Static checks. Run actionlint (or a YAML lint) on the changed workflows, and diff the six `check` jobs to confirm they match apart from the prefix. Optionally run the `workflow-comparator` agent.
- [ ] Step 6: Commit via `/commit`, then verify in CI. Dispatch `prerelease-pipeline-multitier-dotnet.yml` with `commit-sha` pinned to a commit whose images predate the last acceptance run, and confirm acceptance-stage runs the tests and publishes an RC tag on that commit. Dispatch the same commit again and confirm it skips with "RC tag already exists on pinned SHA" and the pipeline still passes.

## Open questions

- Should `-legacy` pinned runs also bypass the staleness skip, so every pinned dispatch actually runs legacy tests? Nothing depends on it today; recommended to leave as is.
