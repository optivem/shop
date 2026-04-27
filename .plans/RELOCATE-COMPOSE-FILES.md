# Plan: Relocate docker-compose files out of `system-test/`

## Status: DRAFT — awaiting go-ahead

## Goal

Move SUT-deployment docker-compose files from `system-test/<lang>/<topology>/` to `system/<topology>/<lang>/`, co-locating each compose file with the Dockerfile it builds.

Today the compose files describe how to build and run a language-specific SUT image plus its dependencies (postgres, external-real-sim or external-stub/WireMock). They live under `system-test/`, but they are not consumed by test code — only by `docker compose` invocations in workflows and scripts. Their primary subject is the system, not the tests.

After this plan:

```
shop/
  system/
    monolith/
      java/
        Dockerfile
        docker-compose.local.real.yml      ← moved here
        docker-compose.local.stub.yml
        docker-compose.pipeline.real.yml
        docker-compose.pipeline.stub.yml
      dotnet/        … (same)
      typescript/    … (same)
    multitier/
      java/          … (same 4 files)
      dotnet/        … (same)
      typescript/    … (same)
    external-real-sim/
    external-stub/
  system-test/
    java/
      monolith/
        system.json     ← stays (test infra, language-specific URLs/ports)
      multitier/
        system.json     ← stays
    dotnet/    …
    typescript/ …
```

### Why this layout (recommended)

- Compose `context:` collapses from `../../../system/monolith/java` to `.` — file lives next to what it builds.
- External stub/sim mounts collapse from `../../../system/external-stub` to `../../external-stub` — fewer `..` traversals.
- The compose file's primary subject (the SUT image) and its descriptor sit together.
- `system.json` stays under `system-test/` because it encodes test-side concerns (per-language port partitioning 3111/3211/3311, health-check URLs); only its `composeFile` field changes.

### Alternatives considered

| Option | Path | Verdict |
|---|---|---|
| **A — co-locate with SUT** ✅ | `system/<topology>/<lang>/docker-compose.*.yml` | Recommended |
| B — central deploy dir | `shop/docker-compose/<topology>/<lang>/` | Same number of `..` traversals; new dir without justification |
| C — keep as-is | `system-test/<lang>/<topology>/docker-compose.*.yml` | Status quo; co-located with consumer instead of subject |

---

## Inventory

### Files to move (24 total)

Per language `lang ∈ {java, dotnet, typescript}` and topology `top ∈ {monolith, multitier}`:

```
system-test/<lang>/<top>/docker-compose.local.real.yml      → system/<top>/<lang>/docker-compose.local.real.yml
system-test/<lang>/<top>/docker-compose.local.stub.yml      → system/<top>/<lang>/docker-compose.local.stub.yml
system-test/<lang>/<top>/docker-compose.pipeline.real.yml   → system/<top>/<lang>/docker-compose.pipeline.real.yml
system-test/<lang>/<top>/docker-compose.pipeline.stub.yml   → system/<top>/<lang>/docker-compose.pipeline.stub.yml
```

### Files to update

**Inside each moved compose file (24 files):**
- `context: ../../../system/<top>/<lang>` → `context: .`
- `volumes: - ../../../system/external-real-sim:/app` → `- ../../external-real-sim:/app`
- `volumes: - ../../../system/external-stub:/home/wiremock` → `- ../../external-stub:/home/wiremock`

**`system.json` files (6 total — `system-test/<lang>/<top>/system.json`):**
- Field `composeFile`: `docker-compose.local.real.yml` → relative path to new location, e.g. `../../../system/monolith/java/docker-compose.local.real.yml`. Verify path resolution semantics against the consumer in `gh-optivem/internal/runner/` (`config.go`, `system.go`).

**Workflows (~30 files under `shop/.github/workflows/*-{commit,acceptance,acceptance-legacy,qa,prod}-stage.yml`):**
- Inputs `compose-file: <topology>/docker-compose.<env>.<mode>.yml` (with `working-directory: system-test/<lang>`) need to point at the new location. Two approaches:
  - **Approach A:** change `working-directory` to repo root and `compose-file` to full path `system/<topology>/<lang>/docker-compose.<env>.<mode>.yml`.
  - **Approach B:** keep `working-directory: system-test/<lang>` and switch to `compose-file: ../../system/<topology>/<lang>/docker-compose.<env>.<mode>.yml`.
  - **Recommendation:** Approach A — clearer, doesn't rely on relative traversal. Decide before executing.

Affected workflow files (per grep):
- `monolith-{java,dotnet,typescript}-{commit,qa,prod}-stage.yml`
- `monolith-{java,dotnet,typescript}-acceptance-stage{,-legacy}.yml`
- `multitier-{java,dotnet,typescript}-{qa,prod}-stage.yml`
- `multitier-{java,dotnet,typescript}-acceptance-stage{,-legacy}.yml`
- `_prerelease-pipeline.yml`

**Scripts:**
- `shop/scripts/pre-commit-hook.sh` (any compose path mentions)
- `shop/run-all-system-tests.sh` (verify)

**Docs:**
- `shop/README.md`
- `shop/system-test/{java,dotnet,typescript}/README.md` (if they describe the layout)

**Existing plan files** (reference paths in inventory tables):
- `shop/.plans/GENERIC-SYSTEM-SLOT.md`
- `shop/.plans/PLACEHOLDER-RENAME.md`
- `shop/.plans/HELLO-WORLD-GREETER.md`

---

## Steps

1. **Confirm target layout.** User signs off on Option A (co-locate under `system/`) and workflow Approach A (full path from repo root).
2. **Move 24 compose files** with `git mv` to preserve history.
3. **Edit moved compose files:** rewrite `context:` and external-sim/stub volume mounts.
4. **Update `system.json` × 6:** rewrite each `composeFile` field; verify `gh-optivem/internal/runner/config.go` path resolution accepts the new form.
5. **Update workflows:** rewrite every `compose-file:` and adjust `working-directory:` per Approach A.
6. **Update scripts:** any compose path references.
7. **Update docs and plan files:** README + cross-references.
8. **Run validation** (see below).
9. **Commit and push** via `/commit`.

---

## Validation

- **Local — monolith × 3 langs × {real, stub}:** `docker compose -f system/monolith/<lang>/docker-compose.local.<mode>.yml up --build` succeeds. Hit health endpoints from `system.json`.
- **Local — multitier × 3 langs × {real, stub}:** same.
- **Run system tests locally** for at least one language using `gh optivem test system` (or equivalent) to confirm `system.json` resolves the new `composeFile` path correctly.
- **CI:** push and watch one commit-stage + one acceptance-stage workflow per language to confirm the new paths work in pipeline.

---

## Risks

- **`gh-optivem` path resolution.** `composeFile` in `system.json` is consumed by Go code in `gh-optivem/internal/runner/`. If that code assumes the file is in the same directory as `system.json`, a relative `../../../system/...` path may not work. Mitigation: read `config.go`/`system.go` resolution logic before step 4; if it's strict, either (a) update the resolver or (b) keep compose files where they are and abandon this plan.
- **Workflow path drift across many files.** Mechanical search/replace risk — easier to miss one. Mitigation: grep `docker-compose\.(local|pipeline)\.(real|stub)\.yml` before and after; counts must match.
- **Pipeline compose vs local compose.** Pipeline variants may build from a pre-published image rather than a local context — verify before assuming `context: .` is universally correct.

---

## Rollback

`git revert` the move commit. No schema, infra, or external system changes — fully reversible.

---

## Open questions

1. Confirm Option A (recommended) vs Option B vs status quo.
2. Confirm workflow Approach A (full path from repo root) vs Approach B (relative from `system-test/<lang>`).
3. Should `system.json` also move, or stay in `system-test/<lang>/<topology>/`? (Recommended: **stay** — it's test-language-specific.)
