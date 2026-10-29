# 2026-09-17 14:04:00 UTC — Pinned-SHA acceptance-stage runs must not skip

## TL;DR

**Why:** When the prerelease pipeline dispatches `<prefix>-acceptance-stage.yml` pinned to a commit, the check gate can skip with "No new artifacts since last run AND no test/workflow changes since last RC tag". The gate compares the pinned `sha-<sha>` image date with the last run of the workflow for *any* commit, and compares test changes with the newest RC tag, which can sit on a *later* commit. The stage succeeds without tagging, so the pipeline's `Fail If No RC Tag On SHA` step (`.github/workflows/_prerelease-pipeline.yml:410-418`) fails. This happened in meta run 35212665594 → child run 35229886589. It is deterministic: pinned dispatches 35220930239, 35228933071 and 35229916617 all skipped the same way.
**End result:** A pinned acceptance-stage run either runs the tests and publishes an RC tag on that commit, or skips only because an RC tag already exists on that commit. The prerelease pipeline no longer fails because an unrelated scheduled run tested a newer `latest`.

## Outcomes

- A pinned dispatch whose images are older than the last acceptance run still runs the tests and tags the pinned commit.
- Pinned dispatches always run: re-dispatching an already-tagged commit re-runs the suite and adds a higher rc tag. Accepted deliberately over an idempotency check, in exchange for a smaller change.
- Unpinned (scheduled or `latest`) runs keep today's staleness skip unchanged.
- All six tag-publishing acceptance-stage workflows have structurally identical check jobs.
- The "race-tolerant with the hourly cron" comment at `_prerelease-pipeline.yml:401-403` describes the actual guarantee.

## ▶ Next executable step (resume here)

Commit the workflow changes with `/commit`, scoped to the shop repo, then verify in CI (Step 1 below). The code work is done: in all six `*-acceptance-stage.yml` files the staleness skip condition gained `inputs.commit-sha == ''`, so a pinned dispatch always runs its tests and tags its commit, with a comment above `Evaluate Run Gate` recording why. `_prerelease-pipeline.yml:401-406` documents the new guarantee. actionlint passes and the six check jobs are identical apart from the pre-existing image list. No idempotency check was added: re-runs re-test the commit and add a higher rc tag, which the pipeline resolves. The `-cloud` variants are manual-dispatch only and no pipeline consumes their RC tag, so they were left alone; `-legacy` variants publish no RC tag and keep today's skip.

## Steps

- [ ] Step 1: Commit via `/commit`, then verify in CI. Dispatch `prerelease-pipeline-multitier-dotnet.yml` with `commit-sha` pinned to a commit whose images predate the last acceptance run, and confirm acceptance-stage runs the tests and publishes an RC tag on that commit, and that the pipeline gets past `Fail If No RC Tag On SHA`.
