# Plan: distribute ATDD Claude assets to shop and student repos

## Problem

`eshop-tests/.claude/` contains an ATDD multi-agent pipeline that we want available in:

1. **shop** (the new system-under-test ecosystem replacing eshop).
2. **Student repos** scaffolded via `gh optivem init`.

The content lives today in `eshop-tests/`:

- `.claude/agents/`: backend, frontend, driver, dsl, manager, release, story, test (8 files).
- `.claude/commands/`: implement-ticket, manage-project (2 files).
- `docs/prompts/atdd/`: orchestrator(+diagram), acceptance-tests, contract-tests (4 files).
- `docs/prompts/architecture/`: driver-adapter, driver-port, dsl-core, dsl-port, test (5 files).
- `docs/prompts/code/language-equivalents.md`.

Total ~21 files, ~786 lines. About 90% is domain-agnostic ATDD doctrine; the rest is hardcoded references (`eshop`, `eshop-tests`, `Run-SystemTests.ps1`).

## Repo layout assumptions

- **shop**: monorepo. System and tests live in the same repo (`system/`, `system-test/`). No sibling test repo.
- **`gh optivem init` scaffolds**:
  - Monorepo: one repo, everything in it.
  - Multirepo monolith: `<repo>` + `<repo>-system`.
  - Multirepo multitier: `<repo>` + `<repo>-backend` + `<repo>-frontend`.
  - In all cases, the **root repo** (`<repo>`) is the orchestration/docs/tests home.
- **eshop-tests** (legacy, archived): separate test repo (`eshop-tests-{java,dotnet,typescript}`) coordinating against `eshop`. This is a special case the v1 installer does **not** need to support.

**Decision**: install `.claude/` and `docs/prompts/` into the root repo only, regardless of mono vs. multirepo. The agents themselves can still operate across sibling repos (via `--system-repos` flags they already accept), but their definitions live in one place. No `--test-repo` flag in v1.

## Install layout & ownership contract

ATDD assets are installed into **dedicated subdirectories** under `.claude/` and `docs/prompts/`, owned by `gh optivem atdd`:

```
.claude/
  agents/
    atdd/                  ← managed: backend.md, frontend.md, driver.md, dsl.md,
                              manager.md, release.md, story.md, test.md
  commands/
    atdd/                  ← managed: implement-ticket.md, manage-project.md
docs/
  prompts/
    atdd/                  ← managed: orchestrator.md, orchestrator-diagram.md,
                              acceptance-tests.md, contract-tests.md
    architecture/          ← managed: driver-adapter.md, driver-port.md,
                              dsl-core.md, dsl-port.md, test.md
    code/                  ← managed: language-equivalents.md
```

**Ownership rule**: every file inside the managed subdirs is owned by `gh optivem atdd`. Students must NOT edit them in place. To customize:

- Copy the file out of the managed subdir into `.claude/agents/` (root) or another sibling location, give it a new name, and edit the copy.
- The original stays untouched and can still be cleanly upgraded.

**Install / upgrade behavior**: **full replace**. `gh optivem atdd install` (and `upgrade`) wipe the managed subdirs and write fresh content from the templates. No merge logic, no version banners, no "are you sure" prompts for files that haven't been edited (because they're not supposed to be edited).

Pre-flight check: warn (and abort without `--force`) if any managed file has a modified mtime relative to its install record OR a content hash that differs from the template — to catch the case of a student editing in place by mistake. With `--force`, replace anyway.

**Why subdirs**:
- Unambiguous ownership signal (whole directory belongs to the tool).
- Doesn't collide with student-authored agents/commands at the `.claude/agents/` root.
- Trivial wholesale upgrade (rm -rf the managed subdir, write fresh).
- Removes versioning/upgrade-merge complexity entirely.

**Verify in Phase 1**: confirm Claude Code reads agents/commands from subdirectories of `.claude/agents/` and `.claude/commands/`. If not, fall back to flat layout with a `atdd-` filename prefix (e.g., `.claude/agents/atdd-backend.md`).

## Architecture-agnosticism

ATDD doctrine is **architecture-agnostic**. The cycle (RED → GREEN at three layers: TEST, DSL, DRIVER) and the contract-test sub-process don't depend on monolith vs. multitier, monorepo vs. multirepo, or backend language. The agents that orchestrate it (story, test, dsl, driver, backend, frontend, release, manager) reference layers, not architectures.

Practical consequences:

- A single set of templates serves every `gh optivem init` arch+strategy combination. No per-arch ATDD variants.
- The `frontend-agent` is harmless in monolith projects — it simply has nothing to do when no UI changes are needed. (Or omit it from monolith installs if we want a tighter set; recommended: keep it, since monolith projects can still have UI.)
- The `shop/` vs. `external/` package convention referenced in the prompts is an ATDD doctrine concept (SUT vs. external collaborator), not an architecture choice — it applies across all archs.

This locks in the "single source of truth, install everywhere" approach. Per-arch divergence is explicitly a non-goal.

## Options considered

### Option A — copy `.claude/` and `docs/prompts/` directly into shop

- Pro: zero infra, students who clone shop get it for free.
- Con: drifts vs. eshop-tests immediately; doesn't help any repo other than shop; hardcoded `eshop`/`eshop-tests` strings have to be hand-edited.

### Option B — `gh optivem atdd install` distributes from gh-optivem (RECOMMENDED)

- Single source of truth lives in `gh-optivem/internal/templates/atdd/`.
- `gh optivem atdd install` (run inside any repo) writes `.claude/agents/`, `.claude/commands/`, and `docs/prompts/` into the current repo, doing template substitution for repo names.
- `gh optivem init` calls the same installer at the end of scaffolding (gated by a flag, default on).
- Existing TODO at `gh-optivem/internal/files/files.go:203` already anticipates this ("When ATDD support is added, generate project-specific CLAUDE.md files…").
- Pro: domain-agnostic, one place to fix bugs, students self-serve, retrofits any existing repo.
- Con: more upfront work (templating, install command, upgrade UX).

### Option C — hybrid: copy now, migrate to gh-optivem later

Rejected — guarantees we'll do the work twice and have to migrate shop's copy back out.

## Recommendation

**Option B.** The ATDD assets are doctrine, not domain code; they belong in the tooling that distributes doctrine. shop becomes the first non-eshop consumer.

## Tasks

### Phase 1 — extract canonical assets into gh-optivem

- [ ] Copy `eshop-tests/.claude/agents/*.md` → `gh-optivem/internal/templates/atdd/agents/`.
- [ ] Copy `eshop-tests/.claude/commands/*.md` → `gh-optivem/internal/templates/atdd/commands/`.
- [ ] Copy `eshop-tests/docs/prompts/{atdd,architecture,code}/` → `gh-optivem/internal/templates/atdd/prompts/`.
- [ ] Replace hardcoded `eshop` / `eshop-tests` with placeholder `{{.SystemRepo}}`. Audit results: 5 hits in `acceptance-tests.md`, 3 in `orchestrator.md`.
- [ ] Replace `Run-SystemTests.ps1` invocations with `gh optivem run system tests` calls (cross-platform, no .ps1 vs .sh divergence, tied to the CLI students already have installed). Mapping:
  - `.\Run-SystemTests.ps1 -Suite <s> -Test <t>` → `gh optivem run system tests --suite <s> --test <t>` (verify `--test` flag exists; if not, drop it or add it as part of this work).
  - `.\Run-SystemTests.ps1 -Suite <s>` → `gh optivem run system tests --suite <s>`.
  - Full suite → `gh optivem run system tests` (no `--suite`).
- [ ] Decide what to do with the `shop/` package convention — it's a generic ATDD term in the prompts (the SUT subfolder, distinct from `external/`), not the repo name. Keep as-is; do NOT templatize.
- [ ] Decide whether to embed the templates (Go `embed.FS`) — consistent with how `gh-optivem/internal/templates/templates.go` already works.

### Phase 2 — `gh optivem atdd install` command

- [ ] Add subcommand `gh optivem atdd install` (in `main.go` / `runner_commands.go`).
- [ ] Flags:
  - `--system-repo <name>` (defaults to current repo name detected via `git remote`).
  - `--force` to overwrite existing files.
  - `--dry-run`.
- [ ] Render templates and write into `./.claude/` and `./docs/prompts/`.
- [ ] Idempotent: skip files that already exist unless `--force`.
- [ ] No `--test-repo` flag in v1 — tests are always in the same repo as the agents.

### Phase 3 — `gh optivem atdd upgrade`

- [ ] Re-render templates and diff against installed copies; warn before overwriting hand-edits.
- [ ] Print a summary of what changed.

### Phase 4 — wire into `gh optivem init`

- [ ] After scaffolding succeeds, call the installer.
- [ ] Add `--no-atdd` to skip; default is on.
- [ ] Remove the `CLAUDE.md` skip in `gh-optivem/internal/files/files.go:205-207` and the `.claude` skip at line 195 — replace with the templated install.

### Phase 5 — retrofit shop and eshop-tests

- [ ] Run `gh optivem atdd install --system-repo shop` inside shop. Commit.
- [ ] Leave eshop-tests as-is (archived per memory) — it stays as the historical source the templates were extracted from.

## Open questions

1. **Templating scope.** Are there any domain-specific bits beyond the repo names? Re-grep `eshop` in eshop-tests' agent files before extracting. (Resolved for `--test-repo`: not needed in v1.)
2. **CLAUDE.md generation.** The existing TODO at files.go:203 wants per-project CLAUDE.md. Out of scope for this plan unless we want to bundle it — recommend deferring.
3. **Versioning.** When we update agents in gh-optivem, how do downstream repos know to re-run `atdd upgrade`? Print a version banner in agent files? Out of scope for v1.

## Non-goals

- Changing the ATDD doctrine itself.
- Updating eshop-tests (archived per memory).
- Building a per-language ATDD variant.
