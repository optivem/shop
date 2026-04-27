Implement the following ticket using the multi-agent ATDD workflow.

Input: $ARGUMENTS

The input is a GitHub issue number (e.g. `#42`), optionally followed by flags. A ticket is always required — free-text stories are not accepted.

**Autonomous mode:** if `--autonomous` is present in the input, skip all STOP/human-approval steps — agents self-approve and the pipeline runs end-to-end without waiting for the user.

**Repositories:** optionally specify which repositories the pipeline operates on:
- `--test-repos <repo1>,<repo2>,...` — the test repositories to implement in (e.g. `eshop-tests-java`, `eshop-tests-dotnet`, `eshop-tests-typescript`)
- `--system-repos <repo1>,<repo2>,...` — the system (backend/frontend) repositories (e.g. `eshop`)

If not specified, infer the appropriate repositories from the GitHub issue context (labels, title, existing tests, etc.) and confirm with the user before proceeding.

## Orchestration

Follow the decision flow defined in `docs/prompts/atdd/orchestrator.md`. That document defines:
- The AT cycle decision tree (TEST → DSL → SYSTEM DRIVER → GREEN, with skip logic)
- The Contract Test sub-process (triggered when external driver interfaces change)
- The scenario loop (repeat until all scenarios are GREEN)
- Phase-to-agent mapping
- STOP behaviour (normal vs autonomous mode)
- Resume detection via `@Disabled` markers

### Pre-flight

#### Status Validation

Check the issue's status on the GitHub project board:
- **Ready** → move it to **In Progress** and proceed.
- **In Progress** → proceed (resume case).
- **Any other status** → STOP. Tell the user the issue is in status `<status>` and ask whether to proceed.

#### Resume Detection

Before doing anything else, scan the test repository for `@Disabled` annotations with known phase markers (see orchestrator.md resume detection table). If found, skip to the indicated phase. If not found, proceed from step 1.

### Step 1: Story

Launch **story-agent** with the issue number. It will read the GitHub issue and produce Gherkin scenarios.
- **Normal mode:** Present the Gherkin scenarios and wait for human approval.
- **Autonomous mode:** Auto-approve and proceed immediately.

After approval, update the issue body with the approved Gherkin scenarios (use `gh issue edit` preserving the user story preamble).

### Step 2: Per-Scenario Loop

For each scenario, follow the AT cycle decision tree from `orchestrator.md`:

1. **AT - RED - TEST:** Launch test-agent (WRITE → STOP → COMMIT).
2. **Decision:** DSL Interface Changed? If no → skip to GREEN.
3. **AT - RED - DSL:** Launch dsl-agent (WRITE → STOP → COMMIT).
4. **Decision:** External System Driver Interface Changed? If yes → run Contract Test Sub-Process.
5. **Decision:** System Driver Interface Changed? If no → skip to GREEN.
6. **AT - RED - SYSTEM DRIVER:** Launch driver-agent (WRITE → STOP → COMMIT).
7. **AT - GREEN - SYSTEM:** Launch backend-agent → frontend-agent → release-agent.
8. If remaining `// TODO:` scenarios exist, loop back to step 1.

### Escalation

If any agent reports it cannot proceed, STOP and present the blocker to the user — **even in autonomous mode**.
