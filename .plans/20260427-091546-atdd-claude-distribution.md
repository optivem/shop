# Plan: distribute ATDD Claude assets to shop and student repos

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-27T07:31:40Z`

## Problem

`eshop-tests/.claude/` contains an ATDD multi-agent pipeline that we want available in:

1. **shop** (the new system-under-test ecosystem replacing eshop).
2. **Student repos** scaffolded via `gh optivem init`.

The content lives today in `eshop-tests/`:

- `.claude/agents/`: backend, frontend, driver, dsl, manager, release, story, test (8 files).
- `.claude/commands/`: implement-ticket, manage-project (2 files).
- `docs/prompts/atdd/`: orchestrator(+diagram), acceptance-tests, contract-tests (4 files).
- `docs/prompts/architecture/`: driver-adapter, driver-port, dsl-core, dsl-port, test (5 files).
- `docs/prompts/code/language-equivalents.md` (1 file).
- `docs/prompts/glossary.md` (1 file — ATDD glossary, defines "interface change" etc.; placed under `atdd/` in the destination).

Total 21 files. About 90% is domain-agnostic ATDD doctrine; the rest is hardcoded references to the SUT (`eshop`, `eshop-tests`) and to legacy test-runner commands.

## Repo layout assumptions

- **shop**: monorepo. System and tests live in the same repo (`system/`, `system-test/`). No sibling test repo.
- **`gh optivem init` scaffolds**:
  - Monorepo: one repo, everything in it.
  - Multirepo monolith: `<repo>` + `<repo>-system`.
  - Multirepo multitier: `<repo>` + `<repo>-backend` + `<repo>-frontend`.
  - In all cases, the **root repo** (`<repo>`) is the orchestration/docs/tests home.
- **eshop-tests** (legacy, archived): separate test repo (`eshop-tests-{java,dotnet,typescript}`) coordinating against `eshop`. This is a special case the v1 installer does **not** need to support.

**Decision**: install `.claude/` and `docs/prompts/` into the root repo only, regardless of mono vs. multirepo. The agents themselves can still operate across sibling repos (via `--system-repos` flags they already accept), but their definitions live in one place. No `--test-repo` flag in v1.

## Source of truth: shop, not gh-optivem

`gh optivem init` already treats **shop** as the canonical source for everything it distributes — workflows, system code, externals, system-tests, docs. The CLI does not embed templates: at release time `goreleaser` bakes a specific shop SHA into the binary via ldflags (`internal/version.ShopRef`), and at runtime `--shop-ref <tag|sha|branch>` pins which shop checkout to copy from. `ApplyTemplate` walks `cfg.ShopPath` and copies files, applying string-replacement maps (`monolithContentReplacements`, `FixupWorkflowContent`, etc.) at copy time.

**ATDD assets follow the same pattern**: live in `shop/.claude/` and `shop/docs/prompts/`, copied by `gh optivem atdd install` from `cfg.ShopPath`. No `embed.FS`, no per-asset versioning, no template extraction step. Shop is the SUT the doctrine is exercised against, so authoring lives where it is dogfooded.

This rules out the earlier "extract into `gh-optivem/internal/templates/atdd/`" approach: it would duplicate the source-of-truth that `init` already establishes and require its own release/versioning UX.

## Install layout & ownership contract

ATDD assets are installed into **dedicated subdirectories** under `.claude/` and `docs/prompts/`, owned by `gh optivem atdd`. The same subdir layout is used in **shop itself** (the source) so the copy is a 1:1 directory mapping:

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
                              acceptance-tests.md, contract-tests.md, glossary.md
    architecture/          ← managed: driver-adapter.md, driver-port.md,
                              dsl-core.md, dsl-port.md, test.md
    code/                  ← managed: language-equivalents.md
```

**Ownership rule**: every file inside the managed subdirs is owned by `gh optivem atdd`. Students must NOT edit them in place. To customize:

- Copy the file out of the managed subdir into `.claude/agents/` (root) or another sibling location, give it a new name, and edit the copy.
- The original stays untouched and can still be cleanly upgraded.

**Install / upgrade behavior**: **full replace**. `gh optivem atdd install` (and `upgrade`) wipe the managed subdirs and write fresh content from shop. No merge logic, no version banners, no "are you sure" prompts for files that haven't been edited (because they're not supposed to be edited).

Pre-flight check: warn (and abort without `--force`) if any managed file's content hash differs from what shop has at `cfg.ShopRef` — to catch the case of a student editing in place by mistake. With `--force`, replace anyway.

**Why subdirs**:
- Unambiguous ownership signal (whole directory belongs to the tool).
- Doesn't collide with student-authored agents/commands at the `.claude/agents/` root, including in shop itself.
- Trivial wholesale upgrade (rm -rf the managed subdir, copy fresh).
- Removes versioning/upgrade-merge complexity entirely.

**Verify in Phase 1**: confirm Claude Code reads agents/commands from subdirectories of `.claude/agents/` and `.claude/commands/`. If not, fall back to flat layout with an `atdd-` filename prefix (e.g., `.claude/agents/atdd-backend.md`).

## Architecture-agnosticism

ATDD doctrine is **architecture-agnostic**. The cycle (RED → GREEN at three layers: TEST, DSL, DRIVER) and the contract-test sub-process don't depend on monolith vs. multitier, monorepo vs. multirepo, or backend language. The agents that orchestrate it (story, test, dsl, driver, backend, frontend, release, manager) reference layers, not architectures.

Practical consequences:

- A single set of templates serves every `gh optivem init` arch+strategy combination. No per-arch ATDD variants.
- The `frontend-agent` is harmless in monolith projects — it simply has nothing to do when no UI changes are needed. (Or omit it from monolith installs if we want a tighter set; recommended: keep it, since monolith projects can still have UI.)
- The `shop/` vs. `external/` package convention referenced in the prompts is an ATDD doctrine concept (SUT vs. external collaborator), not an architecture choice — it applies across all archs.

This locks in the "single source of truth, install everywhere" approach. Per-arch divergence is explicitly a non-goal.

## Templating

Follow the existing `ApplyTemplate` pattern: shop's files contain literal strings (e.g., `shop` for the SUT name); `gh optivem atdd install` applies a replacement map at copy time:

- `shop` → `cfg.SystemRepo` (the student's repo name, defaults to current `git remote` repo name).
- `Run-SystemTests.ps1` invocations → `gh optivem test system ...` (cross-platform, no .ps1 vs .sh divergence). Mapping:
  - `.\Run-SystemTests.ps1 -Suite <s> -Test <t>` → `gh optivem test system --suite <s> --test <t>`
  - `.\Run-SystemTests.ps1 -Suite <s>` → `gh optivem test system --suite <s>`
  - Full suite → `gh optivem test system`

A post-condition validator (mirroring `ValidateNoLeftoverTemplateRefs` in `apply_template.go`) fails if any forbidden literal (`shop` SUT references, `Run-SystemTests.ps1`) survives in the installed assets. Note: the generic ATDD-doctrine `shop/` package convention is distinct from the SUT name and must NOT be in the replacement map — Phase 1 audit needs to confirm the two uses are textually distinguishable (e.g., `shop` as SUT name appears bare or in URLs; `shop/` as package appears with a trailing slash in code paths).

## Options considered

### Option A — copy `.claude/` and `docs/prompts/` directly into shop, no installer

- Pro: zero infra, students who clone shop get it for free.
- Con: doesn't help any repo other than shop; hardcoded SUT names have to be hand-edited per consumer; no story for `gh optivem init` scaffolds.

### Option B — extract into `gh-optivem/internal/templates/atdd/`, distribute via embed.FS

- Pro: self-contained binary, doctrine versioned with the CLI.
- Con: duplicates the source-of-truth that `init` already establishes (shop); requires its own release/versioning UX; templates rot away from the live SUT they're meant to exercise.
- Rejected.

### Option C — shop hosts canonical assets, `gh optivem atdd install` copies from `cfg.ShopPath` (RECOMMENDED)

- Single source of truth lives in `shop/.claude/agents/atdd/`, `shop/.claude/commands/atdd/`, `shop/docs/prompts/`.
- `gh optivem atdd install` (run inside any repo) copies from `cfg.ShopPath` into the current repo, applying the templating replacement map.
- `gh optivem init` calls the same installer at the end of scaffolding (gated by `--no-atdd`, default on).
- Versioning is free: `--shop-ref` (and the baked-in SHA in released binaries) already pins which shop revision is the canonical source.
- Existing TODO at `gh-optivem/internal/files/files.go:203` already anticipates this ("When ATDD support is added, generate project-specific CLAUDE.md files…").
- Pro: domain-agnostic, single place to author, students self-serve, retrofits any existing repo, dogfooded against shop's live code, no new versioning surface.
- Con: ATDD assets become part of shop's release surface — a doctrine fix is gated on a shop `meta-v*` release.

## Recommendation

**Option C.** Mirrors how `init` already distributes everything else from shop; reuses `--shop-ref`, `cfg.ShopPath`, and the replacement-map idiom; no new infra.

## Tasks

### Phase 1 — consolidate canonical assets in shop

- [ ] Copy `eshop-tests/.claude/agents/*.md` → `shop/.claude/agents/atdd/`.
- [ ] Copy `eshop-tests/.claude/commands/*.md` → `shop/.claude/commands/atdd/`.
- [ ] Copy `eshop-tests/docs/prompts/{atdd,architecture,code}/` → `shop/docs/prompts/{atdd,architecture,code}/`, and `eshop-tests/docs/prompts/glossary.md` → `shop/docs/prompts/atdd/glossary.md`.
- [ ] Replace `eshop` / `eshop-tests` in copied content with `shop` so the literal substitution at install time is straightforward. Audit results: 5 hits in `acceptance-tests.md`, 3 in `orchestrator.md`.
- [ ] Replace `Run-SystemTests.ps1` invocations with `gh optivem test system ...` calls. Verify `--test` flag exists in `runner_commands.go`; if not, add it as part of this work.
- [ ] Confirm Claude Code reads agents/commands from `atdd/` subdirectories. If not, fall back to flat layout with `atdd-` filename prefix.
- [ ] Decide what to do with the `shop/` package convention referenced in the prompts — it's a generic ATDD term (the SUT subfolder, distinct from `external/`), not the repo name. Keep as-is; do NOT include in the replacement map. Document the distinction in `docs/prompts/atdd/orchestrator.md` if not already clear.

### Phase 2 — `gh optivem atdd install` command

- [ ] Add subcommand `gh optivem atdd install` (in `main.go` / `runner_commands.go`).
- [ ] Resolve `cfg.ShopPath` and `cfg.ShopRef` via the same `resolveShopRef` mechanism `init` uses.
- [ ] Flags:
  - `--system-repo <name>` (defaults to current repo name detected via `git remote`).
  - `--shop-ref <ref>` (inherits `init`'s default of baked-in SHA → latest `meta-v*`).
  - `--force` to overwrite existing files / pre-flight failures.
  - `--dry-run`.
- [ ] Copy `shop/.claude/agents/atdd/` → `./.claude/agents/atdd/` (full replace).
- [ ] Copy `shop/.claude/commands/atdd/` → `./.claude/commands/atdd/` (full replace).
- [ ] Copy `shop/docs/prompts/{atdd,architecture,code}/` → `./docs/prompts/{atdd,architecture,code}/` (full replace; `glossary.md` lives under `atdd/`).
- [ ] Apply replacement map (`shop` → `cfg.SystemRepo`) using the existing `templates.FixupAllTextFiles` helper.
- [ ] Pre-flight: hash-check installed copies against shop at `cfg.ShopRef`; abort without `--force` if any managed file diverges.
- [ ] Post-condition: validator fails if forbidden literals survive in the installed assets — mirror `ValidateNoLeftoverTemplateRefs`.
- [ ] No `--test-repo` flag in v1 — tests are always in the same repo as the agents.

### Phase 3 — `gh optivem atdd upgrade`

- [ ] Re-run `install` with the latest `cfg.ShopRef`.
- [ ] Print a summary of what changed (file count, hash diffs).
- [ ] If the pre-flight detects hand-edits without `--force`, list them and exit non-zero.

### Phase 4 — wire into `gh optivem init`

- [ ] After `ApplyTemplate` succeeds, call `atdd install` with the same `cfg.ShopPath` / `cfg.ShopRef` / `cfg.SystemRepo`.
- [ ] Add `--no-atdd` to skip; default is on.
- [ ] Remove the `CLAUDE.md` skip in `gh-optivem/internal/files/files.go:205-207` and the `.claude` skip at line 195 — the new install path supersedes them.

### Phase 5 — retrofit existing repos

- [ ] Run `gh optivem atdd install --system-repo shop` inside shop itself to verify idempotence (for shop, `cfg.SystemRepo == shop`, so the substitution is a no-op and the install should be a content-identical copy).
- [ ] Leave eshop-tests as-is (archived per memory) — it stays as the historical source the templates were extracted from.

## Open questions

1. **Templating scope.** Beyond `eshop`/`eshop-tests` and `Run-SystemTests.ps1`, are there any domain-specific bits? Re-grep the eshop-tests agent files during Phase 1 audit.
2. **CLAUDE.md generation.** The existing TODO at `files.go:203` wants per-project CLAUDE.md. Out of scope for this plan unless we want to bundle it — recommend deferring.
3. **Release coupling.** Once ATDD assets ship as part of shop, a doctrine fix is gated on a shop `meta-v*` release. Acceptable cost; flagging here for visibility.

## Non-goals

- Changing the ATDD doctrine itself.
- Updating eshop-tests (archived per memory).
- Building a per-language ATDD variant.
- Embedding ATDD templates in the `gh-optivem` binary (rejected — see Option B).
