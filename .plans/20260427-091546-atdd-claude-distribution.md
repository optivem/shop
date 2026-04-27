# Plan: consolidate ATDD Claude assets in shop

🤖 **Picked up by agent** — `Valentina_Desk` at `2026-04-27T07:31:40Z`

> **Scope**: Phase 1 only — make shop the canonical source of truth for ATDD Claude assets. Downstream CLI work (`gh optivem atdd install/upgrade` and `init` wiring) lives in `gh-optivem/plans/20260427-091546-atdd-install-cli.md`.

## Problem

`eshop-tests/.claude/` contains an ATDD multi-agent pipeline (~21 files, ~786 lines). We want it to live in **shop** as canonical source — shop is the new SUT replacing eshop, and the doctrine is exercised against shop's live code.

Source content in `eshop-tests/`:

- `.claude/agents/`: backend, frontend, driver, dsl, manager, release, story, test (8 files).
- `.claude/commands/`: implement-ticket, manage-project (2 files).
- `docs/prompts/atdd/`: orchestrator(+diagram), acceptance-tests, contract-tests (4 files).
- `docs/prompts/architecture/`: driver-adapter, driver-port, dsl-core, dsl-port, test (5 files).
- `docs/prompts/code/language-equivalents.md` (1 file).
- `docs/prompts/glossary.md` (1 file — ATDD glossary; placed under `atdd/` in destination).

Total **21 files**. Most is domain-agnostic; references to `eshop` / `eshop-tests` / `Run-SystemTests.ps1` are rewritten to shop equivalents so the install-time substitution (handled by the gh-optivem CLI) becomes straightforward.

## Install layout in shop

```
.claude/
  agents/atdd/             ← managed: backend.md, frontend.md, driver.md, dsl.md,
                              manager.md, release.md, story.md, test.md
  commands/atdd/           ← managed: implement-ticket.md, manage-project.md
docs/prompts/
  atdd/                    ← managed: orchestrator.md, orchestrator-diagram.md,
                              acceptance-tests.md, contract-tests.md, glossary.md
  architecture/            ← managed: driver-adapter.md, driver-port.md,
                              dsl-core.md, dsl-port.md, test.md
  code/                    ← managed: language-equivalents.md
```

The `atdd/` subdirectories under `.claude/agents/` and `.claude/commands/` exist so shop's pre-existing non-ATDD agents (compare-*, editor, monitor, docs/) stay at the root and are not mixed with ATDD doctrine.

**Ownership rule** (enforced by the downstream CLI): every file inside the managed subdirs is owned by `gh optivem atdd`. Students copy out and rename to customize.

## Tasks

### Phase 1 — consolidate canonical assets in shop

- [ ] Replace `eshop` / `eshop-tests` literals in copied content with `shop`. Audit results: 5 hits in `acceptance-tests.md`, 3 in `orchestrator.md`.
- [ ] Replace `Run-SystemTests.ps1` invocations with `gh optivem test system ...` calls. Verify `--test` flag exists in `gh-optivem/internal/runner_commands.go`; if not, add it as part of this work.
- [ ] Confirm Claude Code reads agents/commands from `atdd/` subdirectories. If not, fall back to flat layout with `atdd-` filename prefix.
- [ ] Decide what to do with the `shop/` package convention referenced in the prompts — it's a generic ATDD term (the SUT subfolder, distinct from `external/`), not the repo name. Keep as-is. Document the distinction in `docs/prompts/atdd/orchestrator.md` if not already clear.

## Open questions

1. **Templating scope.** Beyond `eshop`/`eshop-tests` and `Run-SystemTests.ps1`, are there any other domain-specific bits the gh-optivem CLI would need to substitute? Re-grep during Phase 1.5/1.6.

## Non-goals

- Building the gh-optivem CLI subcommands (separate plan).
- Changing the ATDD doctrine itself.
- Updating eshop-tests (archived per memory).
- Building a per-language ATDD variant.
