# Cross-language parity: system-test (TS / Java / .NET) — 2026-04-21 09:10 UTC

## Principle

**The three shop system-test implementations (TypeScript, Java, .NET) should be structural twins** — same layers, same class names, same DSL shape, same test coverage — differing only where the host language's idioms genuinely require it (casing, assertion library, indent).

A 24-theme comparison of shop vs eshop-tests (TS) and shop-only cross-language audit identified 10 unexpected gaps. This plan closes the 5 biggest ones.

## Out of scope

- **T13 (casing of wire-contract values)**: deferred — unifying `'ui'/'UI'`, `'stub'/'STUB'` across languages may break HTTP/JSON wire format assumptions; needs a separate decision on whether the normalization happens at the wire boundary or at the enum definition.
- **T4 (primitive vs domain DTOs)**: deferred — swapping TS `number` for a `Decimal` wrapper is a cross-cutting DTO change with large blast radius.
- **T6 (HTTP abstraction)**, **T19 (PageClient size)**: style gaps; valuable but low risk, leave for opportunistic cleanup.
- **T23 cross-language comments in PowerShell scripts**: non-testkit files, handled separately.

## Items

### Phase 1 — low-risk cleanups (independent, can land in any order)

- [ ] **Delete cross-language comment in TS `base-use-case.ts`** — Remove the `Mirrors Java's BaseUseCase<TDriver, TSuccessResponse, TSuccessVerification>` comment. Violates global CLAUDE.md rule "Each project must be self-contained". Straight deletion — no logic change.
  - Affects: `shop/system-test/typescript/src/testkit/dsl/core/shared/base-use-case.ts`
  - Consumers to update: 0
  - Category: cleanup

- [ ] **Switch TS test specs to use `result-assert.ts` helper** — The helper at `shop/system-test/typescript/src/testkit/common/result-assert.ts` already exists but isn't used. Test specs currently write `expect(result.success).toBe(true)` which hides failure context. Replace with `resultAssert(result).isSuccess()` style, matching Java's `assertThatResult(...)` and .NET's `result.ShouldBeSuccess()`. Enumerate call sites first before bulk edit.
  - Affects: `shop/system-test/typescript/src/testkit/common/result-assert.ts` (if API needs tweaks), all `*.spec.ts` under `shop/system-test/typescript/tests/` that assert on a `Result`
  - Consumers to update: TBD — run `grep -rn "result\.success" shop/system-test/typescript/tests/` to produce the exact list before starting
  - Category: refactor

- [ ] **Delete 9-line `use-case-context.ts` re-export stub in TS** — Migration tech-debt. The real `UseCaseContext` lives at `shop/system-test/typescript/src/testkit/dsl/core/use-case-context.ts`; the stub at `shop/system-test/typescript/src/testkit/dsl/core/shared/use-case-context.ts` just re-exports `LegacyUseCaseContext`. Either delete the stub and point imports at the real file, or move the real class into the `shared/` location to match Java/.NET. Recommended: **move the real class into `shared/`**, matching the Java/.NET single-canonical-location layout.
  - Affects: `shop/system-test/typescript/src/testkit/dsl/core/use-case-context.ts` (delete), `shop/system-test/typescript/src/testkit/dsl/core/shared/use-case-context.ts` (replace stub with real class)
  - Consumers to update: TBD — grep for `from.*use-case-context` under `shop/system-test/typescript/src/testkit/` and `shop/system-test/typescript/tests/`
  - Category: refactor

### Phase 2 — .NET architectural alignment

- [ ] **Drop .NET `BaseUseCase` from 5 → 3 type params** — Currently `BaseUseCase<TDriver, TSuccessResponse, TFailureResponse, TSuccessVerification, TFailureVerification>` with a `[SuppressMessage("SonarAnalyzer.CSharp", "S2436")]` attribute on the class (the suppression is a code smell). Reduce to `BaseUseCase<TDriver, TSuccessResponse, TSuccessVerification>` matching TS/Java — failure types become a shared `SystemError` error branch on `Result<T, SystemError>`, removing the need for per-use-case failure generics.
  - Affects: `shop/system-test/dotnet/DslCore/Shared/BaseUseCase.cs` (signature change, drop Sonar suppress), every concrete `*UseCase.cs` / `*Command.cs` under `shop/system-test/dotnet/DslCore/UseCase/` that inherits from it
  - Consumers to update: TBD — run `grep -rn "BaseUseCase<" shop/system-test/dotnet/` to produce the exact list before starting
  - Category: refactor

- [ ] **Inject external drivers into .NET DSLs (remove internal mode switch)** — Currently `UseCaseDsl.CreateClockDriver()` (and Erp/Tax equivalents) switches on `ExternalSystemMode` internally, coupling the DSL to configuration. Move the switch into the test-base configuration layer (matching TS's `test-setup.ts` factory and Java's `BaseConfigurableTest`). DSL receives the concrete driver via constructor.
  - Affects: `shop/system-test/dotnet/DslCore/UseCase/UseCaseDsl.cs`, `shop/system-test/dotnet/DslCore/UseCase/External/Clock/ClockDsl.cs`, `.../Erp/ErpDsl.cs`, `.../Tax/TaxDsl.cs`, `shop/system-test/dotnet/SystemTests/BaseSystemTest.cs` (or equivalent test base)
  - Consumers to update: `UseCaseDsl` instantiation sites — TBD via grep
  - Category: refactor

### Phase 3 — AppContext extraction (Java + .NET)

- [ ] **Extract `AppContext` in Java and .NET** — TS splits test-kit state into three classes: `AppContext` (driver factory + close-all lifecycle), `ScenarioContext` (per-scenario data), `UseCaseContext` (alias/param state). Java/.NET have only `UseCaseContext`; driver lifecycle and close-all are tangled into `BaseConfigurableTest` (Java) and `UseCaseDsl`/`BaseSystemTest` (.NET). Extract an `AppContext` class in both languages, matching the TS shape — this is the biggest architectural gap across the three. Start with Java (more explicit lifecycle), then mirror in .NET.
  - Affects:
    - Java: `shop/system-test/java/src/main/java/com/optivem/shop/systemtest/BaseConfigurableTest.java`, new `shop/system-test/java/src/main/java/com/optivem/shop/testkit/dsl/core/scenario/AppContext.java`
    - .NET: `shop/system-test/dotnet/SystemTests/BaseSystemTest.cs`, `shop/system-test/dotnet/DslCore/UseCase/UseCaseDsl.cs`, new `shop/system-test/dotnet/DslCore/Scenario/AppContext.cs`
  - Consumers to update: every test file inheriting from `BaseConfigurableTest` (Java) / `BaseSystemTest` (.NET) — TBD via grep before starting
  - Category: refactor

### Phase 4 — verification

- [ ] **Run the full system-test suite in all three languages after each phase** — Per shop CI conventions, the three languages are tested independently. Confirm no regressions; in particular Phase 2 (BaseUseCase param reduction) and Phase 3 (AppContext extraction) touch every use-case and every test base — run the full matrix before declaring done.
  - Affects: runtime verification only
  - Consumers to update: 0
  - Category: verification
