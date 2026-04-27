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

Every commit message follows the pattern: `<Scenario> | <Phase>`.

If a GitHub issue number was provided as input, prefix every commit message with `#<issue-number> | `. Example: `#42 | Register Customer | AT - RED - TEST`.

**Important:** The phase suffix in the message is the phase *prefix only* (e.g. `AT - RED - TEST`). Do **NOT** append `- COMMIT` or `- WRITE` to the phase in the commit message — those suffixes identify the section header only, not the commit message.

## AT - RED - TEST - WRITE (STOP)

1. Write the acceptance tests, following these rules:
   - Write acceptance tests only — do not implement anything.
   - Each Gherkin scenario maps directly to one test method — one-to-one, no interpretation needed.
   - Only specify the minimum data needed — inputs directly relevant to what is being tested, and assertions directly relevant to the expected outcome. Do not add extra fields, extra assertions, or noise. If a field is not relevant to the scenario being tested, omit it entirely and let the DSL use its default value.
   - If the DSL needs to be extended with new methods, call them directly in the test as if they exist — do not add them to the DSL interface yet. Compile errors are expected and intentional.
   - **Scenario ordering within the test class:**
     1. Legacy Coverage scenarios (from the `## Legacy Coverage` section of the ticket, if any)
     2. New feature scenarios that use only existing DSL
     3. New feature scenarios that need new DSL
   - **Attempt to compile.** If compilation succeeds, all written tests remain as real methods. If compilation fails, keep exactly **one** real test method total across all test files — the very first scenario in scenario order — and convert every other scenario to a `// TODO: <Scenario Name>` comment with no method body. Compile errors on that first test are expected and intentional; the method is needed to drive the DSL implementation.
   - After writing each test, verify it matches the acceptance criteria exactly — Given maps to Given, When maps to When, Then maps to Then. Every precondition stated in the scenario must appear in the test. If anything is unclear, ask before proceeding.
2. Run the tests and verify they fail (compile error is expected if new DSL methods are needed):
   ```
   .\Run-SystemTests.ps1 -Suite <acceptance-api> -Test <TestMethodName>
   .\Run-SystemTests.ps1 -Suite <acceptance-ui> -Test <TestMethodName>
   ```
3. STOP. Present the tests to the user and ask for approval. Do NOT continue.

## AT - RED - TEST - COMMIT

1. If there were compile-time errors in AT - RED - TEST - WRITE:
   a. Extend the DSL interfaces with the new methods.
   b. Implement the new methods by throwing a "TODO: DSL" not-implemented exception (see `language-equivalents.md`) — do not implement DSL.
   c. Run the tests and verify they fail with a runtime error:
      ```
      .\Run-SystemTests.ps1 -Suite <acceptance-api> -Test <TestMethodName>
      .\Run-SystemTests.ps1 -Suite <acceptance-ui> -Test <TestMethodName>
      ```
2. Mark the tests as disabled with reason `"AT - RED - TEST"` (see `language-equivalents.md` for syntax).
3. COMMIT with message `<Scenario> | AT - RED - TEST`.
4. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## AT - RED - DSL - WRITE (STOP)

1. Enable the tests marked disabled with reason `"AT - RED - TEST"`.
2. Implement the DSL for real — replace the "TODO: DSL" stub with actual logic.
3. Update the Driver interfaces as needed.
4. Check whether any interface changes (see `glossary.md`) affect files under an `external/` package (e.g. `driver-port/.../external/clock`). Set a flag: **External System Driver Interface Changed = yes/no**.
5. Check whether any interface changes affect files under a `shop/` package (e.g. `driver-port/.../shop/api`). Set a flag: **System Driver Interface Changed = yes/no**.
6. STOP. Present the DSL implementation, Driver interface changes, and both flags to the user and ask for approval. Do NOT continue.

## AT - RED - DSL - COMMIT

1. Implement the Drivers by throwing a "TODO: Driver" not-implemented exception (see `language-equivalents.md`).
2. Run the tests and verify they fail with a runtime error:
   ```
   .\Run-SystemTests.ps1 -Suite <acceptance-api> -Test <TestMethodName>
   .\Run-SystemTests.ps1 -Suite <acceptance-ui> -Test <TestMethodName>
   ```
3. Mark the tests as disabled with reason `"AT - RED - DSL"` (see `language-equivalents.md` for syntax).
4. Ensure that there are no test files in the list of changed files.
5. COMMIT with message `<Scenario> | AT - RED - DSL`.
6. If a GitHub issue number was provided as input, post a comment on the issue summarising the DSL interface changes made (new methods added, interfaces updated).
7. Automatically proceed to AT - RED - SYSTEM DRIVER - WRITE (STOP).

## AT - RED - SYSTEM DRIVER - WRITE (STOP)

1. Enable the tests marked disabled with reason `"AT - RED - DSL"`.
2. Implement the Drivers — replace the "TODO: Driver" stub with actual logic.
   - Only look at files in the `driver-adapter` and `driver-port` directories under `shop/` (e.g. `shop/api`, `shop/ui`).
   - Do NOT implement drivers under `external/` — those are handled by the Contract Test sub-process.
   - Do NOT read or search backend/frontend source code. Model the new method on existing driver methods in the same file.
3. Run the tests and verify they fail with a runtime error.
4. STOP. Present the Driver implementation to the user and ask for approval. Do NOT continue.

## AT - RED - SYSTEM DRIVER - COMMIT

1. Mark the tests as disabled with reason `"AT - RED - SYSTEM DRIVER"` (see `language-equivalents.md` for syntax).
2. Ensure no test files are in the list of changed files.
3. COMMIT with message `<Scenario> | AT - RED - SYSTEM DRIVER`.
4. If a GitHub issue number was provided as input, post a comment on the issue summarising the Driver interface changes made (new methods added, interfaces updated).
5. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## AT - GREEN - SYSTEM - WRITE (STOP)

1. Implement the backend:
   a. Implement the backend changes.
   b. Run acceptance tests for the API channel:
      ```
      .\Run-SystemTests.ps1 -Suite <acceptance-api> -Test <TestMethodName> -Rebuild
      ```
   c. If tests fail, fix the backend until the tests pass.
   d. If you have challenges making the tests pass, ask the user.
   e. Do NOT change the tests/dsl/drivers — change only the backend code.
2. Implement the frontend:
   a. Implement the frontend changes.
   b. Run acceptance tests for the UI channel:
      ```
      .\Run-SystemTests.ps1 -Suite <acceptance-ui> -Test <TestMethodName> -Rebuild
      ```
   c. If tests fail, fix the frontend until the tests pass.
   d. If you have challenges making the tests pass, ask the user.
   e. Do NOT change the tests/dsl/drivers — change only the frontend code.
3. By now, all acceptance tests should be passing.
4. STOP. Present the implementation to the user and ask for approval. Do NOT continue.

## AT - GREEN - SYSTEM - COMMIT

1. In the `eshop` repository: COMMIT all backend and frontend changes with message `<Scenario> | AT - GREEN - SYSTEM`.
2. Remove the disabled annotation (reason `"AT - RED - SYSTEM DRIVER"`) from the tests.
3. Run the tests and verify they all pass:
   ```
   .\Run-SystemTests.ps1 -Suite <acceptance-api> -Test <TestMethodName>
   .\Run-SystemTests.ps1 -Suite <acceptance-ui> -Test <TestMethodName>
   ```
4. Ensure that there are no non-test files in the list of changed files in the `eshop-tests` repository.
5. COMMIT in the `eshop-tests` repository with message `<Scenario> | AT - GREEN - SYSTEM`.
6. If a GitHub issue was provided as input, tick the checkbox for the completed acceptance criterion in that issue.
7. If all acceptance criteria in the issue are now ticked, and the issue belongs to a GitHub project, move the issue to the **In Review** status in that project.
8. If there are remaining `// TODO:` scenarios in the test file, return to AT - RED - TEST - WRITE for the next scenario.


# TODO: VJ: Need to add insutrctions regarding handling legacy code...
