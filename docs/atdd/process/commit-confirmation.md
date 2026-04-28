# Commit Confirmation Rule

A shared, low-level rule that every committing agent in the ATDD pipeline must follow. Imported directly by leaf agents (`atdd-test`, `atdd-dsl`, `atdd-driver`, `atdd-backend`, `atdd-frontend`, `atdd-task`, `atdd-chore`, `atdd-release`, and any future committing agent).

This rule is intentionally separate from `orchestrator.md`: the orchestrator decides *which* phases run; this file decides *how* the commit step inside any phase is gated. Leaf agents only need this gate, not the routing tables.

## Rule

**No agent may run `git commit` (or `gh issue close`, or any other state-mutating push to GitHub) without first asking the user "Can I commit?" and receiving an explicit "yes" reply in the same turn.**

This rule applies universally — to every COMMIT step in every cycle (AT, CT, System API Task, System UI Task, External API Task, Chore, Legacy Coverage, External System Onboarding, Release).

## Procedure

A COMMIT step proceeds as:

1. Stage the intended changes and show the user the exact commit message and a summary of what will be committed (files touched, commands to be run).
2. Ask: **"Can I commit?"**
3. Wait for an explicit affirmative reply (e.g. "yes", "go ahead", "approved"). Silence, ambiguous replies, or anything other than a clear yes counts as **no** — do not commit.
4. Only after explicit approval, run `git commit`.

## Relationship to STOP

The STOP at the end of a WRITE phase is **not** a substitute for this confirmation. The WRITE-STOP approves the *content*; the commit-confirmation approves the *act of committing*. Both are required. If the user has just approved a WRITE-STOP, still ask "Can I commit?" before running `git commit`.

## Bypass

This rule cannot be bypassed by `--no-verify`, `--amend`, scripts, or wrapping the commit inside another command. If the user wants to grant a blanket approval for a single cycle, they must say so explicitly — and even then, the agent should re-confirm before each commit boundary it can name in advance.
