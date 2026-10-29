# 2026-06-24 10:57 UTC — Extract a shared composite action for stage checks

## TL;DR

**Why:** Follow-up to
[20260624-1049-local-stage-superset-of-commit-stage.md](20260624-1049-local-stage-superset-of-commit-stage.md)
(resolved **OQ-3(a)**). That plan ships the `local`-stage lint + unit +
component/Pact checks **inline** in `_prerelease-pipeline.yml` to close the
green-local ⟹ green-commit gap fast. Inline means the `local` commands and the
matching `*-commit-stage.yml` commands are **duplicated** and can drift again —
the very problem the parent plan exists to fix. The same duplication already
exists for the **compile** steps.

**Target state:** compile + lint + unit + component/Pact for each
project/language live in a **single reusable composite action** in the sibling
`optivem/actions` repo. Both the `local` job (`_prerelease-pipeline.yml`) and each
`*-commit-stage.yml` workflow **call that action** instead of inlining the
commands. One source of truth ⇒ zero drift between the two stages.

**What changes for the reader:** editing a project's lint/unit/component command
is a one-line change in `optivem/actions`, not an N-place edit across `local` +
commit-stage workflows. Green-local ⟹ green-commit becomes structurally
guaranteed (identical commands), not just convention-guaranteed (mirrored
commands + a source-pointer comment).

## Scope

- **In:** compile, lint, unit, component/Pact step definitions — the test gates
  the parent plan inlines, **plus** the existing compile-step duplication.
- **Out:** Docker build/push and Sonar stay commit-stage-only (parent plan
  Decision 2 — artifact-producing/scanning, not test gates). `gh optivem system
  start` / `test run --sample` stay inline in `local` (system-test orchestration,
  not a per-project check).

## Open questions

- **OQ-A — One composite action or several?** A single parameterised action
  (`inputs: architecture, language, component`) vs per-concern actions
  (`compile`, `lint`, `unit`, `component`) composed by each caller. Lean
  per-concern: callers pick the gates they need (commit-stage wants all four;
  `local` wants the same four; a future stage might want a subset) without a
  mega-action growing conditional branches.
- **OQ-B — Where do the per-language commands live?** Inside the composite action
  (action owns the `./gradlew` / `dotnet` / `npm` matrix) vs passed in by the
  caller. Lean action-owns-them — that is the whole point of single-source-of-truth;
  the caller passes only `architecture`/`language`/`component` selectors.
- **OQ-C — Versioning / pinning of the `optivem/actions` ref.** Both repos must
  agree on how `shop` pins the action (`@main` vs a tag/SHA). Match whatever
  convention the existing `optivem/actions` consumers in `shop` already use;
  confirm during encoding.

## Steps (sketch — refine before executing)

- [ ] **Step 1 — inventory.** Enumerate the exact compile + lint + unit +
  component commands per project from the 7 `*-commit-stage.yml` workflows and the
  parent plan's mapping table. This is the spec the action must reproduce verbatim.
- [ ] **Step 2 — author the composite action(s)** in `optivem/actions` per OQ-A/OQ-B.
- [ ] **Step 3 — rewire `*-commit-stage.yml`** (7 workflows) to call the action.
  Verify the commit-stage runs are unchanged (same commands, same gates).
- [ ] **Step 4 — rewire the `local` job** in `_prerelease-pipeline.yml` to call the
  same action, replacing the inline blocks the parent plan added. Drop the
  source-pointer comments (no longer needed once the source is shared).
- [ ] **Step 5 — verify.** All `prerelease-pipeline-*.yml` + `*-commit-stage.yml`
  parse; a `level: local` meta dry-run still runs the checks; commit-stage still
  green. **Ask before any system-test / component-test run** (memory rule).

## Risks

- **Cross-repo change.** Touches `optivem/actions` + every `*-commit-stage.yml` +
  `_prerelease-pipeline.yml`; larger blast radius than the inline parent plan —
  which is exactly why it was deferred to this follow-up.
- **Action-ref pinning drift** (OQ-C) — a floating `@main` ref means an
  `optivem/actions` change can break `shop` CI without a `shop` commit.
