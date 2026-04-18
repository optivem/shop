# .NET — System Test Alignment Plan

Reference report: [`reports/20260418-1751-compare-tests-both.md`](../reports/20260418-1751-compare-tests-both.md)

.NET matches Java's test and architecture structure closely. One architectural mismatch remains: the shared `ErrorVerification` abstraction that Java (`system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/shared/ErrorVerification.java`) and TypeScript (`system-test/typescript/src/testkit/dsl/core/shared/error-verification.ts`) both expose at the DSL-core shared layer does not exist in .NET. .NET currently exposes only per-domain flavors:

- `system-test/dotnet/Dsl.Core/UseCase/Shop/UseCases/Base/SystemErrorFailureVerification.cs`
- `system-test/dotnet/Dsl.Core/UseCase/External/Clock/UseCases/Base/ClockErrorVerification.cs`
- `system-test/dotnet/Dsl.Core/UseCase/External/Erp/UseCases/Base/ErpErrorVerification.cs`
- `system-test/dotnet/Dsl.Core/UseCase/External/Tax/UseCases/Base/TaxErrorVerification.cs`

## 1. Architecture — Scenario DSL — introduce a shared `ErrorVerification<TError>` type at `Dsl.Core/Shared/`

Add `system-test/dotnet/Dsl.Core/Shared/ErrorVerification.cs`, parallel to Java's `ErrorVerification.java`. Give it a base generic shape that can be reused across `SystemError`, `ClockErrorResponse`, `ErpErrorResponse`, and `TaxErrorResponse`:

- Inherit from `ResponseVerification<TError>` (already present at `Dsl.Core/Shared/ResponseVerification.cs`).
- Expose an `ErrorMessage(string expected)` method (alias expansion via `UseCaseContext` like Java does) — this is what the shop `SystemErrorFailureVerification` currently does inline.
- Optionally expose a `FieldErrorMessage(string field, string expected)` method for `SystemError` (shop) — in Java this lives on `ErrorVerification` itself because `SystemError` has `fields`; in .NET this can live on a `SystemErrorVerification : ErrorVerification<SystemError>` derived class if the generic version cannot express it.

Then refactor the domain-specific files to thin wrappers (or delete them if the shared type is sufficient):

- `SystemErrorFailureVerification.cs` → subclass of `ErrorVerification<SystemError>` exposing `FieldErrorMessage`.
- `ClockErrorVerification.cs` → subclass of `ErrorVerification<ClockErrorResponse>`; delete any body that is now in the base.
- `ErpErrorVerification.cs` → subclass of `ErrorVerification<ErpErrorResponse>`; delete redundant body.
- `TaxErrorVerification.cs` → subclass of `ErrorVerification<TaxErrorResponse>`; delete redundant body.

Keep each subclass only if it contributes type-specific behavior — otherwise remove it in favor of the shared base (mirroring Java's single `ErrorVerification.java`).

Update any call sites (e.g. `BaseThenResultOrder.cs`, `BaseThenResultCoupon.cs`, `ThenFailure*.cs`) that currently reference the per-domain types.

Do **not** touch the Success/Failure split on the Then step, the `*And` variants, `ThenStageBase`, or any member listed under the "Exceptions (known divergences)" section of the report. Those are deliberately .NET-specific for async adaptation.

## Local verification & commit

1. From `system-test/dotnet/`, run the latest and legacy suites via the standard entry point:
   ```powershell
   Run-SystemTests -Architecture monolith
   Run-SystemTests -Architecture monolith -Legacy
   ```
   Do **not** substitute `dotnet test` — `Run-SystemTests.ps1` is the only supported entry point because it manages Docker containers and config selection.

2. Investigate and fix any failures reported by either run before moving on.

3. Commit the .NET changes as a single logical commit with a message describing the alignment (e.g. `Centralize ErrorVerification under Dsl.Core/Shared to match Java/TS`).
