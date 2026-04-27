# Contract Tests Process

_This process is only triggered when the AT cycle's DSL phase reports **external system interfaces changed = yes** — i.e. new methods were added to interfaces under `external/` (e.g. `driver-port/.../external/erp`). It is initiated by the orchestrator as defined in `orchestrator.md`._

_If the External System does not even exist yet, make Smoke Tests pass first._

## Commit Message Format

Every commit message follows the pattern: `<Scenario> | <Phase>`.

If a GitHub issue number was provided as input, prefix every commit message with `#<issue-number> | `. Example: `#42 | Register Customer | CT - RED - TEST`.

## CT - RED - TEST - WRITE (STOP)

1. Write External System Contract Tests.
   - If new DSL methods are needed, call them directly as if they exist — compile errors are expected.
2. Verify that they pass when executed against the Real External System:
   ```
   .\Run-SystemTests.ps1 -Suite <suite-contract-real> -Test <TestMethodName>
   ```
   If they don't pass, ask the user for support. STOP. Do NOT continue.
3. Verify that they fail when executed against the Stub External System:
   ```
   .\Run-SystemTests.ps1 -Suite <suite-contract-stub> -Test <TestMethodName>
   ```
4. Mark the tests as disabled with reason `"CT - RED - TEST"` (see `language-equivalents.md` for syntax).
5. STOP. Present the contract tests to the user and ask for approval. Do NOT continue.

## CT - RED - TEST - COMMIT

1. If there were compile-time errors in CT - RED - TEST - WRITE:
   a. Extend the DSL interfaces with the new methods.
   b. Implement the new methods by throwing a "TODO: DSL" not-implemented exception (see `language-equivalents.md`).
   c. Run the tests and verify they fail with a runtime error.
2. COMMIT with message `<Scenario> | CT - RED - TEST`.
3. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## CT - RED - DSL - WRITE (STOP)

1. Enable the tests marked disabled with reason `"CT - RED - TEST"`.
2. Implement the DSL for real — replace the "TODO: DSL" stub with actual logic.
3. Update the Driver interfaces as needed.
4. Check whether any interface changes affect files under an `external/` package. Set a flag: **External System Driver Interface Changed = yes/no**. (No recursive triggering — this flag is used only for the CT cycle's own skip logic.)
5. STOP. Present the DSL implementation, Driver interface changes, and the flag to the user and ask for approval. Do NOT continue.

## CT - RED - DSL - COMMIT

1. Implement the Drivers by throwing a "TODO: Driver" not-implemented exception (see `language-equivalents.md`).
2. Run the tests and verify they fail with a runtime error:
   ```
   .\Run-SystemTests.ps1 -Suite <suite-contract-stub> -Test <TestMethodName>
   ```
3. Mark the tests as disabled with reason `"CT - RED - DSL"` (see `language-equivalents.md` for syntax).
4. COMMIT with message `<Scenario> | CT - RED - DSL`.
5. If a GitHub issue number was provided as input, post a comment on the issue summarising the DSL interface changes made (new methods added, interfaces updated).
6. Automatically proceed to CT - RED - EXTERNAL DRIVER - WRITE (STOP).

## CT - RED - EXTERNAL DRIVER - WRITE (STOP)

1. Enable the tests marked disabled with reason `"CT - RED - DSL"`.
2. Implement the Drivers — replace the "TODO: Driver" stub with actual logic.
   - Only look at files in the `driver-adapter` and `driver-port` directories under `external/`.
3. Run the tests and verify they fail with a runtime error.
4. STOP. Present the Driver implementation to the user and ask for approval. Do NOT continue.

## CT - RED - EXTERNAL DRIVER - COMMIT

1. Mark the tests as disabled with reason `"CT - RED - EXTERNAL DRIVER"` (see `language-equivalents.md` for syntax).
2. COMMIT with message `<Scenario> | CT - RED - EXTERNAL DRIVER`.
3. If a GitHub issue number was provided as input, post a comment on the issue summarising the Driver interface changes made (new methods added, interfaces updated).
4. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.

## CT - GREEN - STUBS - WRITE (STOP)

1. Enable the tests marked disabled with reason `"CT - RED - EXTERNAL DRIVER"`.
2. Implement the External System Stubs.
3. Run the External System Contract Tests:
   ```
   .\Run-SystemTests.ps1 -Suite <suite-contract-stub> -Test <TestMethodName> -Rebuild
   ```
4. Verify that the tests pass. If they fail, ask the user. STOP. Do NOT continue.
5. STOP. Present the stub implementation to the user and ask for approval. Do NOT continue.

## CT - GREEN - STUBS - COMMIT

1. Remove the disabled annotation (reason `"CT - RED - EXTERNAL DRIVER"`) from the tests.
2. Run the tests and verify they pass.
3. COMMIT with message `<Scenario> | CT - GREEN - STUBS`.
4. STOP. Do not proceed further. Phase progression is controlled by the orchestrator, not by this agent.
