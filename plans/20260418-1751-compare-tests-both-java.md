# Java — System Test Alignment Plan

Reference report: [`reports/20260418-1751-compare-tests-both.md`](../reports/20260418-1751-compare-tests-both.md)

Java is the cross-language reference implementation. This plan exists only because one coverage gap was found in legacy mod11.

## 1. Legacy — Contract Tests (mod11) — add missing Tax contract tests

`system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod11/contract/` currently contains `base/`, `clock/`, and `erp/` only. Both .NET (`system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/`) and TypeScript (`system-test/typescript/tests/legacy/mod11/contract/tax/`) include a `tax/` sibling with three files. Add the matching Java trio under `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod11/contract/tax/`:

1. **`BaseTaxContractTest.java`** — abstract class extending `com.optivem.shop.systemtest.legacy.mod11.contract.base.BaseExternalSystemContractTest`. Port the single test method from `system-test/dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/BaseTaxContractTest.cs` and `system-test/typescript/tests/legacy/mod11/contract/tax/BaseTaxContractTest.ts`:

   ```java
   @Test
   void shouldBeAbleToGetTaxRate() {
       scenario
               .given().country().withCode("US").withTaxRate(0.09)
               .then().country("US").hasTaxRateIsPositive();
   }
   ```

   Match the structural pattern used by the existing `BaseErpContractTest.java` and `BaseClockContractTest.java` in the same module.

2. **`TaxRealContractTest.java`** — concrete subclass overriding `getFixedExternalSystemMode()` to return `ExternalSystemMode.REAL`. Mirror the structure of `mod11/contract/erp/ErpRealContractTest.java`.

3. **`TaxStubContractTest.java`** — concrete subclass overriding `getFixedExternalSystemMode()` to return `ExternalSystemMode.STUB`. Mirror `mod11/contract/erp/ErpStubContractTest.java`.

Note: .NET and TS do not include a `TaxStubContractIsolatedTest` for mod11 (only Clock has the isolated-stub variant). Do not add one for Java — match the other two languages by including Real and Stub only.

## Local verification & commit

1. From `system-test/java/`, run the latest and legacy suites via the standard entry point:
   ```powershell
   Run-SystemTests -Architecture monolith
   Run-SystemTests -Architecture monolith -Legacy
   ```
   Do **not** substitute `./gradlew test`, `mvn test`, or any raw toolchain invocation — `Run-SystemTests.ps1` is the only supported entry point because it manages Docker containers and config selection.

2. Investigate and fix any failures reported by either run before moving on.

3. Commit the Java changes as a single logical commit with a message describing the alignment (e.g. `Add mod11 Tax contract tests to match .NET and TypeScript`).
