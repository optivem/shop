# Remove dead code from prerelease pipeline

## Context

`meta-prerelease-stage.yml` orchestrates Phase 0 (local), Phase 1 (commit-stage), Phase 2 (pipelines) for six variants. Because of this, the `local` and `commit-stage` jobs inside `_prerelease-pipeline.yml` are always skipped when called via meta:
- Phase 0 triggers the variant pipeline with `{"level": "local", "skip-commit-stage": "true"}` — the pipeline's `local` job runs.
- Phase 1 triggers `*-commit-stage.yml` directly (not through the pipeline).
- Phase 2 triggers the pipeline with `{"skip-local-stage": "true", "skip-commit-stage": "true"}` — both inner jobs skip.

So the `commit-stage` job in `_prerelease-pipeline.yml` is never reached via meta, and the `local` job is only reached via the `level=local` path. The skip inputs and `level=local|commit` options exist only to serve this dead internal branching.

Goal: strip the dead code without losing the six named per-variant entry points (useful for direct diagnosis) or the DRY variant config.

## Items

- [ ] Create `_local-stage.yml` as a reusable workflow (`workflow_call` + `workflow_dispatch`) taking `architecture` and `language` inputs. Move the current `local` job body from `_prerelease-pipeline.yml` into it (runtime setup, compile system, compile system tests, run sample system tests latest + legacy).

- [ ] Update `meta-prerelease-stage.yml` Phase 0 jobs (`local-monolith-java`, `local-monolith-dotnet`, `local-monolith-typescript`, `local-multitier-java`, `local-multitier-dotnet`, `local-multitier-typescript`) to trigger `_local-stage.yml` with `architecture` + `language` inputs instead of triggering `prerelease-pipeline-<variant>.yml` with `level=local`.

- [ ] Strip `_prerelease-pipeline.yml`:
  - Remove the `local` job (moved to `_local-stage.yml`).
  - Remove the `commit-stage` job (meta handles commit stages directly in Phase 1).
  - Remove the `skip-local-stage`, `skip-commit-stage`, and `commit-workflows` inputs.
  - Remove `local` and `commit` from the `level` options (keep `acceptance`, `qa`).
  - Update the `acceptance-stage` job's `needs` and `if` to drop references to `local` and `commit-stage`.

- [ ] Strip `prerelease-pipeline-<variant>.yml` × 6 (`monolith-java`, `monolith-dotnet`, `monolith-typescript`, `multitier-java`, `multitier-dotnet`, `multitier-typescript`):
  - Remove `skip-local-stage` and `skip-commit-stage` inputs and their passthrough to `_prerelease-pipeline.yml`.
  - Remove `local` and `commit` from the `level` options.
  - Remove the `commit-workflows` input passthrough.

- [ ] Update `meta-prerelease-stage.yml` Phase 2 jobs (six `monolith-*` / `multitier-*` jobs) — remove `skip-local-stage` and `skip-commit-stage` from the `inputs` JSON; keep only `level`.

- [ ] Update `meta-prerelease-stage.yml` `workflow_dispatch.inputs.level.options` — remove `local` and `commit` only if meta-level Phase 0 / Phase 1 gating still works with just `acceptance` and `qa`. VJ: keep `local` and `commit` in meta's level dropdown — they control which meta phases run, not pipeline-internal levels.

- [ ] Verify no other workflows reference the removed inputs/options. Search for `skip-local-stage`, `skip-commit-stage`, `commit-workflows`, and `level: local` / `level: commit` across `.github/workflows/`.

- [ ] Run `meta-prerelease-stage` via `workflow_dispatch` with `variant=monolith-java, level=qa` as a dry-run smoke test (no meta-rc tag produced because variant != all). Confirm Phase 0, Phase 1, Phase 2 all run correctly for that variant.
