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
- [ ] Replace hardcoded `eshop` / `eshop-tests` with placeholders (`{{.SystemRepo}}` / `{{.TestRepo}}` or similar). Audit results: 5 hits in `acceptance-tests.md`, 3 in `orchestrator.md` (see grep output below).
- [ ] Decide what to do with the `shop/` package convention — it's a generic ATDD term in the prompts, not the repo name. Keep as-is; do NOT templatize.
- [ ] Decide whether to embed the templates (Go `embed.FS`) — consistent with how `gh-optivem/internal/templates/templates.go` already works.

### Phase 2 — `gh optivem atdd install` command

- [ ] Add subcommand `gh optivem atdd install` (in `main.go` / `runner_commands.go`).
- [ ] Flags:
  - `--system-repo <name>` (defaults to current repo name detected via `git remote`).
  - `--test-repo <name>` (optional; only relevant for multi-repo ATDD setups like eshop-tests).
  - `--force` to overwrite existing files.
  - `--dry-run`.
- [ ] Render templates and write into `./.claude/` and `./docs/prompts/`.
- [ ] Idempotent: skip files that already exist unless `--force`.

### Phase 3 — `gh optivem atdd upgrade`

- [ ] Re-render templates and diff against installed copies; warn before overwriting hand-edits.
- [ ] Print a summary of what changed.

### Phase 4 — wire into `gh optivem init`

- [ ] After scaffolding succeeds, call the installer.
- [ ] Add `--no-atdd` to skip; default is on.
- [ ] Remove the `CLAUDE.md` skip in `gh-optivem/internal/files/files.go:205-207` and the `.claude` skip at line 195 — replace with the templated install.

### Phase 5 — retrofit shop and eshop-tests

- [ ] Run `gh optivem atdd install --system-repo shop` inside shop. Commit.
- [ ] Run `gh optivem atdd install --system-repo eshop --test-repo eshop-tests` inside eshop-tests to switch it from owning the source-of-truth to consuming it. (Or leave eshop-tests as-is until next refactor — it's archived.)

## Open questions

1. **Templating scope.** Are there any domain-specific bits beyond the repo names? Re-grep `eshop` in eshop-tests' agent files before extracting.
2. **shop's test repo.** Does shop have a sibling test repo (like eshop-tests) or is testing in-repo? If in-repo, the `--test-repo` flag becomes optional / no-op.
3. **`Run-SystemTests.ps1` references.** acceptance-tests.md hardcodes the script name. Shop uses the same script name (`run-all-system-tests.sh` exists too). Decide: keep hardcoded as a convention students must follow, or templatize.
4. **CLAUDE.md generation.** The existing TODO at files.go:203 wants per-project CLAUDE.md. Out of scope for this plan unless we want to bundle it — recommend deferring.
5. **Versioning.** When we update agents in gh-optivem, how do downstream repos know to re-run `atdd upgrade`? Print a version banner in agent files? Out of scope for v1.

## Non-goals

- Changing the ATDD doctrine itself.
- Updating eshop-tests (archived per memory).
- Building a per-language ATDD variant.
