# TypeScript — System Test Alignment Plan

Reference report: `reports/20260417-1841-compare-tests-both.md`
Reference implementation: **Java** (align TypeScript to Java unless otherwise noted).

All items executed.

---

## Local verification & commit

From `system-test/typescript/`:

1. Run the latest suite:
   ```
   ./Run-SystemTests.ps1 -Architecture monolith
   ```
2. Run the legacy suite:
   ```
   ./Run-SystemTests.ps1 -Architecture monolith -Legacy
   ```
3. Fix any failures before proceeding. Do not substitute raw `npx playwright test` or `npm test` — `Run-SystemTests.ps1` is the only supported entry point because it manages containers and configuration.
4. Commit the changes as a single logical commit:
   ```
   Align TypeScript testkit to Java + eshop-tests: complete usecase DSL, relocate SystemError, add mod11 tax contract tests, drop time-dependent tag
   ```
