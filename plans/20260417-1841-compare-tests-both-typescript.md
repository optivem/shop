# TypeScript — System Test Alignment Plan

Reference report: `reports/20260417-1841-compare-tests-both.md`
Reference implementation: **Java** (align TypeScript to Java unless otherwise noted).

Execute the tasks below in order. Each task names the concrete target file(s) and the Java reference file to copy behavior from.

---

## 1. Architecture — Scenario DSL Port: delete dead `then-failure-and.ts`

Target file:
- `system-test/typescript/src/testkit/dsl/port/then/steps/then-failure-and.ts` — **delete**.

Rationale:
- The file exports an interface `ThenFailureAnd` that is not imported anywhere in the codebase (checked via grep in the report).
- TypeScript's actual `ThenFailure.and()` returns `this` (see `src/testkit/dsl/core/scenario/then/then-place-order.ts`), so no `ThenFailureAnd` navigation type is used.
- Java reference does not have an equivalent interface; .NET has `IThenFailureAnd.cs` only because of its async Success/Failure split (listed as a known .NET-only divergence — see report "Exceptions").

Do not add a matching `then-success-and.ts`. TypeScript's current one-step `.and()` pattern is closer to Java's `ThenStep<TThen>.and()` and is preferred.

## 2. Architecture — Driver Port / Shop: relocate `SystemError.ts`

Target file:
- `system-test/typescript/src/testkit/driver/port/shop/dtos/SystemError.ts` → move to `system-test/typescript/src/testkit/driver/port/shop/dtos/error/SystemError.ts`.

Reference:
- Java: `system-test/java/src/main/java/com/optivem/shop/testkit/driver/port/shop/dtos/error/SystemError.java`
- .NET: `system-test/dotnet/Driver.Port/Shop/Dtos/Error/SystemError.cs`

Changes:
1. Create directory `src/testkit/driver/port/shop/dtos/error/`.
2. Move `SystemError.ts` into it.
3. Update every `import ... from '.../dtos/SystemError.js'` to `.../dtos/error/SystemError.js`. Affected files include (run a repo-wide grep for `dtos/SystemError` to confirm):
   - `src/testkit/dsl/core/scenario/then/then-place-order.ts`
   - `src/testkit/common/dtos.ts` (barrel re-export)
   - `src/testkit/driver/port/shop/shop-driver.ts`
   - `src/testkit/dsl/core/usecase/shop/ShopDsl.ts`
   - Any other files importing `SystemError` from `dtos/`.

## 3. Architecture — Use Case DSL: add missing Shop usecases

Target directory: `system-test/typescript/src/testkit/dsl/core/usecase/shop/usecases/`

Add the following 6 files, each ported from the Java reference:

| New TS file                        | Java reference                                                                                         |
|------------------------------------|--------------------------------------------------------------------------------------------------------|
| `CancelOrder.ts`                   | `.../dsl/core/usecase/shop/usecases/CancelOrder.java`                                                   |
| `DeliverOrder.ts`                  | `.../dsl/core/usecase/shop/usecases/DeliverOrder.java`                                                  |
| `BrowseCoupons.ts`                 | `.../dsl/core/usecase/shop/usecases/BrowseCoupons.java`                                                 |
| `BrowseCouponsVerification.ts`     | `.../dsl/core/usecase/shop/usecases/BrowseCouponsVerification.java`                                     |
| `PublishCoupon.ts`                 | `.../dsl/core/usecase/shop/usecases/PublishCoupon.java`                                                 |
| `GoToShop.ts`                      | `.../dsl/core/usecase/shop/usecases/GoToShop.java`                                                      |

After adding the files, extend `src/testkit/dsl/core/usecase/shop/ShopDsl.ts` to expose them:
- `cancelOrder(): CancelOrder`
- `deliverOrder(): DeliverOrder`
- `browseCoupons(): BrowseCoupons`
- `publishCoupon(): PublishCoupon`
- `goToShop(): GoToShop` (may replace the current thin `async goToShop()` on `ShopDsl`; keep parity with Java's `GoToShop` usecase that returns a verification.)

Follow the existing TS idioms — functional `Result<T, E>`, `fetch`-based clients — rather than class-based `Result`. Only port the *structure* (usecase names, builder methods, verification API). Reuse existing `ShopDriver` instead of adding a new one.

## 4. Architecture — Use Case DSL: add missing Clock usecases

Target directory: `system-test/typescript/src/testkit/dsl/core/usecase/external/clock/usecases/`

Add the following 4 files, ported from Java:

| New TS file              | Java reference                                                                                   |
|--------------------------|--------------------------------------------------------------------------------------------------|
| `ReturnsTime.ts`         | `.../dsl/core/usecase/external/clock/usecases/ReturnsTime.java`                                  |
| `GetTime.ts`             | `.../dsl/core/usecase/external/clock/usecases/GetTime.java`                                      |
| `GetTimeVerification.ts` | `.../dsl/core/usecase/external/clock/usecases/GetTimeVerification.java`                          |
| `GoToClock.ts`           | `.../dsl/core/usecase/external/clock/usecases/GoToClock.java`                                    |

Wire them into `src/testkit/dsl/core/usecase/external/clock/ClockDsl.ts` so consumers can call `app.clock().returnsTime(...)`, `app.clock().getTime()`, `app.clock().goToClock()`.

## 5. Architecture — Use Case DSL: add missing ERP usecases

Target directory: `system-test/typescript/src/testkit/dsl/core/usecase/external/erp/usecases/`

Add the following 4 files, ported from Java:

| New TS file                 | Java reference                                                                                    |
|-----------------------------|---------------------------------------------------------------------------------------------------|
| `ReturnsPromotion.ts`       | `.../dsl/core/usecase/external/erp/usecases/ReturnsPromotion.java`                                |
| `GetProduct.ts`             | `.../dsl/core/usecase/external/erp/usecases/GetProduct.java`                                      |
| `GetProductVerification.ts` | `.../dsl/core/usecase/external/erp/usecases/GetProductVerification.java`                          |
| `GoToErp.ts`                | `.../dsl/core/usecase/external/erp/usecases/GoToErp.java`                                         |

Wire them into `src/testkit/dsl/core/usecase/external/erp/ErpDsl.ts`. Keep the existing `ReturnsProduct.ts` (already present and aligned).

## 6. Architecture — Use Case DSL: add missing Tax usecases

Target directory: `system-test/typescript/src/testkit/dsl/core/usecase/external/tax/usecases/`

Add the following 4 files, ported from Java:

| New TS file             | Java reference                                                                                |
|-------------------------|-----------------------------------------------------------------------------------------------|
| `ReturnsTaxRate.ts`     | `.../dsl/core/usecase/external/tax/usecases/ReturnsTaxRate.java`                              |
| `GetTaxRate.ts`         | `.../dsl/core/usecase/external/tax/usecases/GetTaxRate.java`                                  |
| `GetTaxVerification.ts` | `.../dsl/core/usecase/external/tax/usecases/GetTaxVerification.java`                          |
| `GoToTax.ts`            | `.../dsl/core/usecase/external/tax/usecases/GoToTax.java`                                     |

Wire them into `src/testkit/dsl/core/usecase/external/tax/TaxDsl.ts`.

## 7. Architecture — UseCaseDsl: rename entry-point `useCase` → `app`

Targets:
- `system-test/typescript/src/testkit/dsl/core/usecase/UseCaseDsl.ts`: keep the class name `UseCaseDsl` but rename the test-facing fixture/property name to `app` (mirror Java `app.shop()...`, `app.erp()...`).
- Fixture file exposing the UseCaseDsl to Playwright tests (likely `tests/legacy/mod07/e2e/fixtures.ts`): rename the exposed fixture key from `useCase` to `app`.
- Legacy mod07 specs that destructure `{ useCase }` (at minimum):
  - `tests/legacy/mod07/e2e/place-order-positive-test.spec.ts`
  - `tests/legacy/mod07/e2e/place-order-negative-test.spec.ts`

  Rename `{ useCase }` → `{ app }` and `useCase.shop()...` → `app.shop()...`. Do a repo-wide grep for `{ useCase }` to catch any other consumers.

Java reference: `.../systemtest/legacy/mod07/e2e/PlaceOrderPositiveTest.java` uses `app.erp().returnsProduct()...execute()`.

## 8. Tests — Latest Acceptance: remove `@time-dependent` tag from `shouldRejectOrderPlacedAtYearEnd`

Target file:
- `system-test/typescript/tests/latest/acceptance/place-order-negative-isolated-test.spec.ts`

Change the test name from:
```
test('shouldRejectOrderPlacedAtYearEnd @time-dependent', ...)
```
to:
```
test('shouldRejectOrderPlacedAtYearEnd', ...)
```

Rationale: Java (`PlaceOrderNegativeIsolatedTest.java`) does **not** mark this test `@TimeDependent`; .NET (`PlaceOrderNegativeIsolatedTest.cs`) does **not** mark it `[Time]`. Java is the reference.

## 9. Legacy Tests — mod11 Tax contract trio

Target files (new):
- `system-test/typescript/tests/legacy/mod11/contract/tax/BaseTaxContractTest.ts`
- `system-test/typescript/tests/legacy/mod11/contract/tax/tax-real-contract-test.spec.ts`
- `system-test/typescript/tests/legacy/mod11/contract/tax/tax-stub-contract-test.spec.ts`

Reference:
- Java: `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod11/contract/tax/BaseTaxContractTest.java`, `TaxRealContractTest.java`, `TaxStubContractTest.java`
- TypeScript latest equivalents (for shape): `system-test/typescript/tests/latest/contract/tax/BaseTaxContractTest.ts`, `tax-real-contract-test.spec.ts`, `tax-stub-contract-test.spec.ts`

Shape:
- `BaseTaxContractTest.ts`: export a `registerTaxContractTests(test: ContractTest)` function that registers the single `shouldBeAbleToGetTaxRate` test — the same pattern used by `tests/latest/contract/tax/BaseTaxContractTest.ts`. The test body is:
  ```ts
  test('shouldBeAbleToGetTaxRate', async ({ scenario }) => {
    await scenario
      .given()
      .country()
      .withCode('US')
      .withTaxRate(0.09)
      .then()
      .country('US')
      .hasTaxRateIsPositive();
  });
  ```
- `tax-real-contract-test.spec.ts`:
  ```ts
  process.env.EXTERNAL_SYSTEM_MODE = 'real';
  import { test } from '../base/fixtures.js';
  import { registerTaxContractTests } from './BaseTaxContractTest.js';
  registerTaxContractTests(test);
  ```
- `tax-stub-contract-test.spec.ts`: same but with `'stub'`.

Confirm that `tests/legacy/mod11/contract/base/fixtures.ts` exists and exports a `test` compatible with the `register*ContractTests` function signature. If the mod11 legacy `contract/base/` directory does not yet exist, copy `tests/legacy/mod11/contract/fixtures.ts` (already listed in the tree) or create a `base/` subdirectory mirroring the Java/latest structure.

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
   Align TypeScript testkit to Java: complete usecase DSL, relocate SystemError, add mod11 tax contract tests, fixture/tag cleanup
   ```

If the scope feels large for a single commit, split along the sections above — for example (a) port-layer & dead-code cleanup (tasks 1–2), (b) usecase DSL fill-in + rename (tasks 3–7), (c) tests (tasks 8–9).
