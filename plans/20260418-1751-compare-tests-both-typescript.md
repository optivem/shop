# TypeScript — System Test Alignment Plan

Reference report: [`reports/20260418-1751-compare-tests-both.md`](../reports/20260418-1751-compare-tests-both.md)

TypeScript has full cross-language parity at the **abstraction-layer** level for legacy modules (no layer is wrong in any module) and no progression regressions. The remaining action items are a mix of naming, folder structure, and missing shared-base-class abstractions in legacy modules. Ordered as per the spec: architectural layers → architecture layers → tests.

## 21. Legacy — Contract — restructure Contract tests to match Java/.NET abstract-class pattern (optional) — ⏳ Deferred: suggestion-only per plan; not implemented unless explicitly approved.

TypeScript currently uses `registerXxxContractTests(test)` helper functions instead of abstract base classes with real/stub subclasses (as Java and .NET do). Functionally equivalent. If the user wants strict parity, refactor to an abstract-class pattern — but this is stylistic and the current approach is a reasonable TS idiom. Flag as a **suggestion only** and do not execute unless explicitly approved.
VJ: is it possible with TYpeScirpt?what's the best way in TypeScript? Or already done?



## Local verification & commit

1. From `system-test/typescript/`, run the latest and legacy suites via the standard entry point:
   ```powershell
   Run-SystemTests -Architecture monolith
   Run-SystemTests -Architecture monolith -Legacy
   ```
   Do **not** substitute `npm test`, `npx playwright test`, `npx vitest`, or any raw toolchain invocation — `Run-SystemTests.ps1` is the only supported entry point because it manages Docker containers and config selection.

2. Investigate and fix any failures reported by either run before moving on.

3. Commit the TypeScript changes as a single logical commit (or a small set of commits split by theme: channel type, folder structure, acceptance fixes, legacy bases) with messages describing the alignment (e.g. `Introduce per-module BaseTest abstractions for legacy mod03–mod11 to match Java/.NET`).
