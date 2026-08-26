# 2026-08-26 17:05:43 UTC — Scope `drift` concurrency so a runner shortage costs one red run, not two

## TL;DR

**Why:** Scheduled run [32983873455](https://github.com/optivem/shop/actions/runs/32983873455) went red because GitHub could not allocate a hosted runner (`The job was not acquired by Runner of type hosted even after multiple attempts`) — a platform capacity blip, not a shop defect. But `drift.yml`'s `cancel-in-progress: false` turned that single shortage into **two** red runs: the follow-on run [32984349351](https://github.com/optivem/shop/actions/runs/32984349351) sat blocked on the concurrency group, then queued into the same shortage and was dropped too.
**End result:** Scheduled `drift` runs collapse their own backlog, so a superseded hourly canary is cancelled (grey) instead of executed-and-failed (red). Pipeline-invoked `drift` runs are unaffected — they keep `cancel-in-progress: false` and live in their own per-SHA concurrency group.

## Outcomes

What we get out of this — the goals and deliverables:

- A hosted-runner shortage (or any queue delay) produces **at most one** red `drift` run, never a cascade of backlogged duplicates.
- A backlogged scheduled `drift` run — testing `:latest` images that a newer run will test anyway — is cancelled rather than burning a 45-minute job slot for zero information.
- A `workflow_call` invocation of `drift` from `_meta-prerelease-pipeline.yml` **can never** be cancelled by a concurrent scheduled run, because pinned and unpinned runs resolve to different concurrency groups.
- The scheduled-vs-pinned concurrency intent is documented in the workflow header, so a future edit doesn't silently undo it.
- On record: the run that triggered this work was a **GitHub platform flake**, not a shop bug — and auto-rerun machinery was considered and deliberately rejected.

## Diagnosis (for the record — no action required)

The originating failure is **not** a shop defect. Evidence:

- `gh run view 32983873455 --log-failed` returns **empty** — zero steps executed.
- Both jobs: `startedAt 15:02:12` → `completedAt 15:17:14`, job conclusion `cancelled`. That is exactly GitHub's ~15-minute hosted-runner allocation window.
- `.github/workflows/drift.yml` unchanged since `6f324522` (2026-07-01); green roughly hourly before and after.
- The next run, `32989039764` (16:33 UTC), succeeded in **4m23s** on the identical file.
- Repo is `PUBLIC` → hosted minutes are free, no spending-limit gating; only 2 jobs were queued, far below the concurrency cap.

**Classification: environment/infra flake (GitHub-side capacity starvation, ~15:00–15:35 UTC on 2026-08-26).** No `file:line` in shop can be pinned as the cause because no repo code ran, and the failure is not locally reproducible by construction.

**Languages affected: none.** `drift.yml` is a single workflow covering both monolith and multitier in one file, and the .NET third leg is explicitly deferred (header comment, lines 27–28). The repo's usual "fix all three languages" rule does not apply.

## Non-goals

- **Auto-rerun machinery** that watches for the `not acquired by Runner` annotation and calls `gh run rerun --failed`. Rejected: it needs a second workflow plus an `actions: write` permission escalation, to mask a platform blip that self-healed within one hour on an hourly canary. Recorded here so it is not re-litigated.
- Changing `runs-on`, the `45`-minute job timeout, or the `15 * * * *` cron. None of them contributed.
- Touching `cross-lang-system-verification.yml`'s `cancel-in-progress: false` — that workflow has no `schedule` trigger, so it has no scheduled backlog to collapse.

## ▶ Next executable step (resume here)

**Observation only — no edits remain.** The workflow change is committed. What is left is
confirming it behaves as intended in CI; there is nothing to edit, so do not go hunting
for code changes.

Run `gh run list --repo optivem/shop --workflow drift.yml --limit 5` and confirm the two
scheduled `drift` runs following the commit completed green. Separately, the next time
`_meta-prerelease-pipeline.yml` invokes `drift` via `workflow_call`, confirm that run was
**not** cancelled by a concurrent scheduled run — that is the one behaviour the group
separation (`drift-<ref>-<sha>` vs `drift-<ref>-latest`) exists to protect.

If both hold, delete this plan file. If a pipeline-invoked run *was* cancelled, the
`required: true` guarantee on `workflow_call`'s `commit-sha` has been broken — reopen
and revisit the concurrency expression.

## Steps

- [ ] **Step 7: Post-merge observation.** ⏳ Deferred: cannot run in the authoring session — requires scheduled `drift` runs that fire *after* the commit lands. Watch the next two scheduled `drift` runs complete green, and confirm the next `workflow_call` invocation from `_meta-prerelease-pipeline.yml` is not cancelled by a concurrent scheduled run.

## Notes — precedent already in the repo

- `.github/workflows/compose-drift.yml:24-25` — sibling drift workflow, already uses `cancel-in-progress: true`.
- `.github/workflows/monolith-dotnet-commit-stage.yml:30-31` — already uses the event-scoped pattern `cancel-in-progress: ${{ github.event_name != 'workflow_dispatch' }}` with a group keyed partly on `inputs.commit-sha`. Step 1 is the same idea, keyed on the input rather than the event name (correct here, because a `workflow_call` from a scheduled pipeline would report `github.event_name == 'schedule'`).
