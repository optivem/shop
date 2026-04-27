# Remove dead code from prerelease pipeline

## Context

`_meta-prerelease-pipeline.yml` (the reusable workflow called by `meta-prerelease-stage.yml` and `meta-prerelease-dry-run.yml`) orchestrates Phase 0 (local), Phase 1 (commit-stage), Phase 2 (pipelines) for six variants via matrix jobs. Because of this, the `local` and `commit-stage` jobs inside `_prerelease-pipeline.yml` are always skipped when called via meta:
- Phase 0 (`local` matrix job) triggers the variant pipeline with `{"level": "local", "skip-commit-stage": "true"}` — the pipeline's `local` job runs.
- Phase 1 (`commit` matrix job) triggers `*-commit-stage.yml` directly (not through the pipeline).
- Phase 2 (`pipeline` matrix job) triggers the pipeline with `{"skip-local-stage": "true", "skip-commit-stage": "true"}` — both inner jobs skip.

So the `commit-stage` job in `_prerelease-pipeline.yml` is never reached via meta, and the `local` job is only reached via the `level=local` path. The skip inputs and `level=local|commit` options exist only to serve this dead internal branching.

Goal: strip the dead code without losing the six named per-variant entry points (useful for direct diagnosis) or the DRY variant config.

## Items

- [ ] Create `_local-stage.yml` as a reusable workflow (`workflow_call` + `workflow_dispatch`) taking `architecture` and `language` inputs. Move the current `local` job body from `_prerelease-pipeline.yml` into it (runtime setup, compile system, compile system tests, run sample system tests latest + legacy).

- [ ] Update `_meta-prerelease-pipeline.yml` Phase 0 (`local` matrix job, lines ~200-214) to trigger `_local-stage.yml` with `architecture` + `language` derived from `matrix.variant` (split the variant name on `-`), instead of triggering `prerelease-pipeline-<variant>.yml` with `level=local`. Preserve `skip-acceptance-legacy` passthrough.

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

- [ ] Update `_meta-prerelease-pipeline.yml` Phase 2 (`pipeline` matrix job, lines ~257-271) — remove `skip-local-stage` and `skip-commit-stage` from the workflow-inputs JSON; keep `level`, `skip-acceptance-legacy`, and `commit-sha`.

- [ ] No-op (documented for clarity): keep `meta-prerelease-dry-run.yml`'s `level` choice dropdown (`local`, `commit`, `acceptance`, `qa`) and `_meta-prerelease-pipeline.yml`'s `level` input intact. They gate which meta phases run (Phase 0 / Phase 1 / Phase 2), not pipeline-internal levels — so removing `local`/`commit` here would break the dry-run knob. Per VJ.

- [ ] Verify no other workflows reference the removed inputs/options. Search for `skip-local-stage`, `skip-commit-stage`, `commit-workflows`, and `level: local` / `level: commit` across `.github/workflows/`.

- [ ] Run `meta-prerelease-dry-run` via `workflow_dispatch` with `variant=monolith-java, level=qa, skip-local=false, skip-commit=false, auto-trigger-stage=false` as a smoke test (dry-run never tags meta-rc — `is-release-run` is not passed, so `_meta-prerelease-pipeline.yml` defaults it to false). Confirm Phase 0 (now via `_local-stage.yml`), Phase 1, Phase 2 all run correctly for that variant.
