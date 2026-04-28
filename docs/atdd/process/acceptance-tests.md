# ATDD Rules

## Phase Progression

Proceed to the next phase automatically **unless** the current phase ends with **STOP**. When a phase ends with STOP, wait for the user to explicitly approve before continuing. If the user says something other than approval after a STOP, ask clarifying questions — do not execute the next phase.

---

## Suite Selection

Each acceptance test is annotated with a channel. Use the matching suite placeholder throughout all phases:
- `<acceptance-api>` — for tests annotated with `@Channel(API)`
- `<acceptance-ui>` — for tests annotated with `@Channel(UI)`

If a test covers both channels, run both suites.

## Commit Message Format

Every commit message follows the pattern: `<Ticket> | <Phase>`.

The unit of work in the AT Cycle is a **ticket**, not an individual scenario — all scenarios for the ticket are batched through each phase together (see AT - RED - TEST below). Commit messages reflect the ticket title.

If a GitHub issue number was provided as input, prefix every commit message with `#<issue-number> | `. Example: `#42 | Register Customer | AT - RED - TEST`.

**Important:** The phase suffix in the message is the phase *prefix only* (e.g. `AT - RED - TEST`). Do **NOT** append `- COMMIT` or `- WRITE` to the phase in the commit message — those suffixes identify the section header only, not the commit message.

## AT - RED - TEST - WRITE (STOP)

**Goal:** every test in the scenario set compiles and fails only with runtime failure, then is marked as known-failing for the next phase.

The unit of work is the **ticket** — all scenarios for the ticket are written together as a batch. There is no per-scenario inner loop in this phase.

1. Write the acceptance tests for **all scenarios in the ticket**, following these rules:
   - Write acceptance tests only — do not implement anything.
   - Each Gherkin scenario maps directly to one test method — one-to-one, no interpretation needed. All scenarios are written as real test methods; no `// TODO:` placeholders.
   - Only specify the minimum data needed — inputs directly relevant to what is being tested, and assertions directly relevant to the expected outcome. Do not add extra fields, extra assertions, or noise. If a field is not relevant to the scenario being tested, omit it entirely and let the DSL use its default value.
   - If the DSL needs to be extended with new methods, call them directly in the test as if they exist — do not add them to the DSL interface yet. Compile errors are expected and intentional.
   - **Scenario ordering within the test class:**
     1. Legacy Coverage scenarios (from the `## Legacy Coverage` section of the ticket, if any)
     2. New feature scenarios that use only existing DSL
     3. New feature scenarios that need new DSL
   - After writing each test, verify it matches the acceptance criteria exactly — Given maps to Given, When maps to When, Then maps to Then. Every precondition stated in the scenario must appear in the test. If anything is unclear, ask before proceeding.
2. STOP. Present the tests to the user for review (the user may revise DSL usage). Do NOT continue.

## AT - RED - TEST - COMMIT

1. **Attempt to compile** the tests.
2. If compilation fails (i.e. the tests reference DSL methods that do not yet exist):
   a. Change the DSL interfaces to add the missing methods.
   b. Implement DSL **prototypes** for the new methods — throw a `"TODO: DSL"` not-implemented exception in each (see `language-equivalents.md`). Do not implement DSL behavior here.
   c. **STOP.** Present the DSL changes and prototype implementations to the user for approval. Do NOT continue until approved.
3. Run the tests and verify they fail with a **runtime** error (not a compile error):
   ```
   gh optivem test system --suite <acceptance-api> --test <TestMethodName>
   gh optivem test system --suite <acceptance-ui> --test <TestMethodName>
   ```
4. Mark **all** the tests as disabled with reason `"AT - RED - TEST"` (see `language-equivalents.md` for syntax).
5. COMMIT with message `<Ticket> | AT - RED - TEST`.
6. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## AT - RED - DSL - WRITE (STOP)

1. Enable the tests marked disabled with reason `"AT - RED - TEST"`.
2. Implement the DSL for real — replace each "TODO: DSL" prototype with actual logic.
3. Update the Driver interfaces as needed.
4. Check whether any interface changes (see `glossary.md`) affect external-system drivers. Set a flag: **External System Driver Interface Changed = yes/no**.
5. Check whether any interface changes affect system drivers. Set a flag: **System Driver Interface Changed = yes/no**.
6. STOP. Present the DSL implementation, Driver interface changes, and both flags to the user and ask for approval. Do NOT continue.

## AT - RED - DSL - COMMIT

1. **If any Driver interface changed** (either flag is `yes`):
   a. Implement Driver **prototypes** for the new/changed Driver methods — throw a `"TODO: Driver"` not-implemented exception in each (see `language-equivalents.md`).
2. Run the tests and verify they fail with a runtime error:
   ```
   gh optivem test system --suite <acceptance-api> --test <TestMethodName>
   gh optivem test system --suite <acceptance-ui> --test <TestMethodName>
   ```
3. Mark the tests as disabled with reason `"AT - RED - DSL"` (see `language-equivalents.md` for syntax).
4. Ensure that there are no test files (accidentally) in the list of changed files.
5. COMMIT with message `<Ticket> | AT - RED - DSL`.
6. STOP. Outputs of this phase: the **External System Driver Interface Changed** flag and the **System Driver Interface Changed** flag — both consumed by the orchestrator to decide whether to enter the Contract Test sub-process and/or AT - RED - SYSTEM DRIVER. Phase progression is controlled by the orchestrator, not by this agent.

## AT - RED - SYSTEM DRIVER - WRITE (STOP)

**Notes:**
- Implement **System Drivers only** — only look at files in the `driver-adapter` and `driver-port` directories under `shop/` (e.g. `shop/api`, `shop/ui`).
- Do NOT implement drivers under `external/` — those are handled by the Contract Test sub-process.
- Do NOT read or search backend/frontend source code. Model new methods on existing driver methods in the same file.

1. Enable the tests marked disabled with reason `"AT - RED - DSL"`.
2. Implement the System Drivers — replace each "TODO: Driver" prototype with actual logic.
3. Run the tests and verify they fail with a runtime error:
   ```
   gh optivem test system --suite <acceptance-api> --test <TestMethodName>
   gh optivem test system --suite <acceptance-ui> --test <TestMethodName>
   ```
4. STOP. Present the Driver implementation to the user and ask for approval. Do NOT continue.

## AT - RED - SYSTEM DRIVER - COMMIT

1. Mark the tests as disabled with reason `"AT - RED - SYSTEM DRIVER"` (see `language-equivalents.md` for syntax).
2. Ensure no test files (accidentally) are in the list of changed files.
3. COMMIT with message `<Ticket> | AT - RED - SYSTEM DRIVER`.
4. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## AT - GREEN - SYSTEM - WRITE (STOP)

**Notes / Assumptions:**
- The agent has access to both backend and frontend code and works across the full stack — no silos like a human team. A single COMMIT therefore covers all implementation changes; the workflow does not split backend and frontend into separate commits.
- When fixing backend or frontend code, do NOT change tests, DSL, or drivers — change only the system implementation.

1. Enable the tests marked disabled with reason `"AT - RED - SYSTEM DRIVER"`. (This is the only "remove disabled annotation" step in this phase — there is no second one at the end.)
2. Implement the backend:
   a. Implement the backend changes.
   b. Run acceptance tests for the API channel:
      ```
      gh optivem test system --rebuild --suite <acceptance-api> --test <TestMethodName>
      ```
   c. If tests fail, fix the backend until the tests pass.
   d. If you have challenges making the tests pass, ask the user.
3. Implement the frontend:
   a. Implement the frontend changes.
   b. Run acceptance tests for the UI channel:
      ```
      gh optivem test system --rebuild --suite <acceptance-ui> --test <TestMethodName>
      ```
   c. If tests fail, fix the frontend until the tests pass.
   d. If you have challenges making the tests pass, ask the user.
4. By now, all acceptance tests should be passing.
5. STOP. Present the implementation to the user and ask for approval. Do NOT continue.

## AT - GREEN - SYSTEM - COMMIT

<!-- TODO(gh-optivem): multirepo support — in monorepo (shop) the COMMIT covers backend, frontend, and (re-enabled) test changes in the same repo. In multirepo scaffolds (`<repo>` + `<repo>-backend` + `<repo>-frontend`, or `<repo>` + `<repo>-system`) the changes span repos; install-time substitution must fan the single logical COMMIT out to one commit per affected repo. v1 install is monorepo-only. -->

1. COMMIT all changes (backend, frontend, and the test re-enabling from AT - GREEN - SYSTEM - WRITE step 1) with message `<Ticket> | AT - GREEN - SYSTEM`.
2. If a GitHub issue was provided as input, tick the checkbox for each acceptance criterion completed by this ticket.
3. If all acceptance criteria in the issue are now ticked, and the issue belongs to a GitHub project, move the issue to the **In Review** status in that project.


# TODO: VJ: Need to add insutrctions regarding handling legacy code...
