# Rename `verify-*` workflows → `pipeline-*`

## Context

The top-level orchestrator workflows in `shop/.github/workflows/` are named `verify-*.yml`. They chain the full pipeline (local → commit → acceptance → qa → release) across language/architecture variants. "verify" is generic and overloaded (release verification, artifact verification, etc.); `pipeline-*` directly names what they are and reads cleanly alongside the stage workflows (`*-commit-stage`, `*-acceptance-stage`, etc.).

Scope: 7 orchestrator files + 1 reusable template + internal references + README badges + one external reference in `gh-optivem`.

Out of scope: `gh-optivem/.github/workflows/verify-release-chain.yml` — keep its filename (different semantics: it verifies the release chain end-to-end), only update its inner `workflow:` reference.

---

## Phase 1: Rename workflow files

In `shop/.github/workflows/`:

1. `verify-all.yml` → `pipeline-all.yml`
2. `verify-monolith-java.yml` → `pipeline-monolith-java.yml`
3. `verify-monolith-dotnet.yml` → `pipeline-monolith-dotnet.yml`
4. `verify-monolith-typescript.yml` → `pipeline-monolith-typescript.yml`
5. `verify-multitier-java.yml` → `pipeline-multitier-java.yml`
6. `verify-multitier-dotnet.yml` → `pipeline-multitier-dotnet.yml`
7. `verify-multitier-typescript.yml` → `pipeline-multitier-typescript.yml`
8. `_verify-pipeline.yml` → `_pipeline.yml` (reusable template — underscore prefix kept)

Use `git mv` so history is preserved.

---

## Phase 2: Update `name:` field inside each renamed file

Each file has a `name:` at the top that matches the filename. Update to match the new name:

- `name: verify-all` → `name: pipeline-all`
- `name: verify-monolith-java` → `name: pipeline-monolith-java`
- ...and so on for all 7 orchestrators
- `name: _verify-pipeline` → `name: _pipeline`

---

## Phase 3: Update internal references

### In `pipeline-all.yml`

Update all `workflow:` fields that point to the renamed orchestrators (14 occurrences across the `local-*` and sequential `monolith-*` / `multitier-*` jobs):

- `workflow: verify-monolith-java.yml` → `workflow: pipeline-monolith-java.yml`
- `workflow: verify-monolith-dotnet.yml` → `workflow: pipeline-monolith-dotnet.yml`
- `workflow: verify-monolith-typescript.yml` → `workflow: pipeline-monolith-typescript.yml`
- `workflow: verify-multitier-java.yml` → `workflow: pipeline-multitier-java.yml`
- `workflow: verify-multitier-dotnet.yml` → `workflow: pipeline-multitier-dotnet.yml`
- `workflow: verify-multitier-typescript.yml` → `workflow: pipeline-multitier-typescript.yml`

### In each `pipeline-{monolith,multitier}-{java,dotnet,typescript}.yml`

Each of the 6 per-variant orchestrators references the reusable template via `uses:`. Update:

- `uses: ./.github/workflows/_verify-pipeline.yml` → `uses: ./.github/workflows/_pipeline.yml`

---

## Phase 4: Update README badges

In `shop/README.md`, update 7 badge blocks (lines ~7–16):

- `verify-all.yml` → `pipeline-all.yml` (badge URL + link URL + alt text)
- `verify-monolith-java.yml` → `pipeline-monolith-java.yml`
- ...all 6 variants

Also update the prose line: `**verify-all** — runs the full pipeline...` → `**pipeline-all** — runs the full pipeline...`

---

## Phase 5: Update external reference in `gh-optivem`

In `gh-optivem/.github/workflows/verify-release-chain.yml`:

- Line 8 job name: `Stage 1 — shop/verify-all (level=local)` → `Stage 1 — shop/pipeline-all (level=local)`
- Line 15: `workflow: verify-all.yml` → `workflow: pipeline-all.yml`

Keep the filename `verify-release-chain.yml` unchanged.

---

## Phase 6: Update plan/doc references

Search for remaining `verify-{all,monolith,multitier,pipeline}` references in:

- `shop/plans/HELLO-WORLD-GREETER.md`
- `shop/plans/MIGRATION-CLOUD-DEPLOY.md`
- `shop/plans/WORKFLOW-DIFF.md`
- `courses/plans/20260417-174826-gh-optivem-pin-shop-version.md`

Update mentions where they refer to the renamed workflows. Skip references to `verify-release-chain` (not renamed).

---

## Phase 7: Commit + verify

1. Commit all changes via `/commit` (one repo at a time: `shop`, `gh-optivem`).
2. Wait for `pipeline-all.yml` nightly trigger, or trigger manually via `gh workflow run pipeline-all.yml --repo optivem/shop -f level=local`.
3. Confirm the run succeeds and all referenced sub-workflows resolve.
4. Check README badges render (may take a minute after push).
5. Confirm `gh-optivem`'s `verify-release-chain.yml` still dispatches shop's `pipeline-all.yml` correctly.

---

## Verification

1. `gh workflow list --repo optivem/shop` — no `verify-*` orchestrators remain; all appear as `pipeline-*`.
2. `grep -r "verify-all\|verify-monolith\|verify-multitier\|_verify-pipeline" shop/ gh-optivem/` returns no hits (except `verify-release-chain.yml` filename itself).
3. README badges show green and link to correct workflow pages.
4. Most recent `pipeline-all` run is green.
