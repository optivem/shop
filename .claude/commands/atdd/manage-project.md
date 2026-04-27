Pick the top ticket from the GitHub project board and implement it using the multi-agent ATDD workflow defined in `docs/prompts/atdd/workflow.md`.

Input: $ARGUMENTS

**Autonomous mode:** if `--autonomous` is present, pass it through to `/implement-ticket` so all human approval touchpoints are skipped.

**GitHub project:** optionally specify `--project <org/number-or-url>` to identify which GitHub project board to use (e.g. `--project optivem/3`). If not specified, the manager-agent will attempt to discover it from the git remote of the current repository.

**Repositories:** optionally specify `--test-repos` and `--system-repos` to control which repositories the pipeline operates on:
- `--test-repos <repo1>,<repo2>,...` — the test repositories to implement in (e.g. `eshop-tests-java`, `eshop-tests-dotnet`, `eshop-tests-typescript`)
- `--system-repos <repo1>,<repo2>,...` — the system (backend/frontend) repositories (e.g. `eshop`)

If not specified, pass them through to the manager-agent and let it determine the appropriate repositories from the GitHub issue context (labels, title, existing code, etc.).

## Steps

1. Launch **manager-agent** with any `--project`, `--test-repos`, and `--system-repos` values (or without them if not provided). It will:
   - Resolve the GitHub project (from the argument, or by discovering it from the git remote)
   - Read the GitHub project board
   - Pick the top card in the Ready column
   - Move it to In Progress
   - If repos were not specified, determine the appropriate test and system repositories from the issue context
   - Return the issue number and the resolved repository lists

2. Pass the issue number, resolved repository lists, and `--autonomous` (if provided) to `/implement-ticket` and run the full pipeline to completion.

## Rules

- If the Ready column is empty, stop and report it to the user.
- If the pipeline is blocked at any point, stop and present the blocker to the user before continuing — even in autonomous mode.
