System Test Comparison Report
=============================

Mode: both

Languages compared: Java (reference), .NET, TypeScript

Scope:
- Latest tests under `system-test/{java,dotnet,typescript}/.../latest/`
- Legacy tests mod02..mod11 under `.../legacy/`
- Four-layer architecture (clients, drivers, channels, use-case DSL, scenario DSL, common)

---

## Latest Comparison

### Acceptance Tests

#### Class Coverage
| Class Name                          | Java | .NET | TypeScript |
|-------------------------------------|:----:|:----:|:----------:|
| BrowseCouponsPositiveTest           |  Y   |  Y   |     Y      |
| CancelOrderNegativeIsolatedTest     |  Y   |  Y   |     Y      |
| CancelOrderNegativeTest             |  Y   |  Y   |     Y      |
| CancelOrderPositiveIsolatedTest     |  Y   |  Y   |     Y      |
| CancelOrderPositiveTest             |  Y   |  Y   |     Y      |
| PlaceOrderNegativeIsolatedTest      |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest              |  Y   |  Y   |     Y      |
| PlaceOrderPositiveIsolatedTest      |  Y   |  Y   |     Y      |
| PlaceOrderPositiveTest              |  Y   |  Y   |     Y      |
| PublishCouponNegativeTest           |  Y   |  Y   |     Y      |
| PublishCouponPositiveTest           |  Y   |  Y   |     Y      |
| ViewOrderNegativeTest               |  Y   |  Y   |     Y      |
| ViewOrderPositiveTest               |  Y   |  Y   |     Y      |

All acceptance test classes exist in all three languages.

#### Method Differences — PlaceOrderPositiveTest

| Method (Java)                                                             | .NET | TypeScript |
|---------------------------------------------------------------------------|:----:|:----------:|
| shouldBeAbleToPlaceOrderForValidInput                                     |  Y   |     Y      |
| orderStatusShouldBePlacedAfterPlacingOrder                                |  Y   |     Y      |
| shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity                   |  Y   |     Y      |
| shouldPlaceOrderWithCorrectBasePriceParameterized                         |  Y   |     Y      |
| orderPrefixShouldBeORD                                                    |  Y   |     Y      |
| discountRateShouldBeAppliedForCoupon                                      |  Y   |     Y      |
| discountRateShouldBeNotAppliedWhenThereIsNoCoupon                         |  Y   |     Y      |
| subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon | Y | Y   |
| subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon                          |  Y   |     Y      |
| correctTaxRateShouldBeUsedBasedOnCountry                                  |  Y   |     Y      |
| totalPriceShouldBeSubtotalPricePlusTaxAmount                              |  Y   |     Y      |
| couponUsageCountHasBeenIncrementedAfterItsBeenUsed                        |  Y   |     Y      |
| orderTotalShouldIncludeTax                                                |  Y   |     Y      |
| orderTotalShouldReflectCouponDiscount                                     |  Y   |     Y      |
| orderTotalShouldApplyCouponDiscountAndTax                                 |  Y   |     Y      |

All methods present in all languages.

#### Body Differences — PlaceOrderPositiveTest

- Method: `totalPriceShouldBeSubtotalPricePlusTaxAmount`
  - Java data rows (4 fields): `{country, taxRate, subtotalPrice, expectedTaxAmount, expectedTotalPrice}` — uses the third column as both product unit price AND expected subtotal: `.given().product().withUnitPrice(subtotalPrice)` → `.and().order().hasSubtotalPrice(subtotalPrice)`.
  - .NET: identical — 5 positional columns `UK, 0.09, "50.00", "4.50", "54.50"`; body uses `.With().Product().WithUnitPrice(subtotalPrice)` → `.HasSubtotalPrice(subtotalPrice)`.
  - TypeScript: adds an extra `unitPrice` field to the case object so the case has SIX fields: `{country, taxRate, unitPrice, subtotalPrice, expectedTaxAmount, expectedTotalPrice}`. Body uses `.withUnitPrice(unitPrice)`; `unitPrice` and `subtotalPrice` are set to the same literal in every row. Structurally an extra parameter but test values are equivalent.

(All other method bodies in PlaceOrderPositiveTest match across the three languages.)

#### Body Differences — PlaceOrderNegativeTest

- Empty-value parameterization source:
  - Java: `@ArgumentsSource(EmptyArgumentsProvider.class)` (in `com.optivem.shop.systemtest.commons.providers.EmptyArgumentsProvider`).
  - .NET: `[ChannelClassData(typeof(EmptyArgumentsProvider))]` (in `SystemTests.Commons.Providers.EmptyArgumentsProvider`).
  - TypeScript: hard-coded inline array `['', '   ']` at each `test.eachAlsoFirstRow(...)` call. There is no shared `EmptyArgumentsProvider` constant/function in `system-test/typescript/src/testkit/common/` or `tests/commons/`. Values match Java/.NET, but the abstraction is missing.
- `ShouldRejectOrderWithNegativeQuantity` / `ShouldRejectOrderWithZeroQuantity`:
  - Java: `.withQuantity(-10)` / `.withQuantity(0)` (int).
  - .NET: `.WithQuantity(-10)` / `.WithQuantity(0)` (int).
  - TypeScript: `.withQuantity('-10')` / `.withQuantity('0')` (string). Values equivalent after coercion.

#### Body Differences — PlaceOrderNegativeIsolatedTest

- Method order: Java lists `shouldRejectOrderPlacedAtYearEnd` first, `cannotPlaceOrderWithExpiredCoupon` second. .NET lists `CannotPlaceOrderWithExpiredCoupon` first, `ShouldRejectOrderPlacedAtYearEnd` second. TS matches .NET order. Purely cosmetic.
- `@TimeDependent` annotation: Java uses `@TimeDependent`, .NET uses `[Time]`, TS uses suffix ` @time-dependent` on the test title. All three map to the same pipeline filter; naming divergence is accepted.

#### Body Differences — PublishCouponNegativeTest

- `shouldRejectCouponWithBlankCode`:
  - Java: `@Channel(ChannelType.API)` + `@ArgumentsSource(EmptyArgumentsProvider.class)` (no UI).
  - .NET: `[ChannelData(ChannelType.API)]` + `[ChannelClassData(typeof(EmptyArgumentsProvider))]` (no UI).
  - TypeScript: `forChannels(ChannelType.API)(...)` with `emptyCodes.forEach` inline; no UI. Behavior matches, but the TS test iterates via plain `forEach`, not `test.each`/`test.eachAlsoFirstRow`, so each test is statically named. Minor stylistic divergence.
- `cannotPublishCouponWithZeroOrNegativeUsageLimit`:
  - Java/.NET: data rows use strings `"0", "-1", "-100"`.
  - TypeScript: data rows use numbers `[0, -1, -100]`. Resulting request body differs by type (number vs string). Functionally the same only if the adapter's DTO coercion handles both.

#### Body Differences — CancelOrderPositiveIsolatedTest

- Java/.NET: `@Channel(value = {API}, alsoForFirstRow = UI)` / `[ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]` — 4 API rows + 1 UI row.
- TypeScript: `forChannels(ChannelType.UI, ChannelType.API)(() => { test.eachAlsoFirstRow(timesOutsideBlackout)(...) })` — the outer `forChannels(UI, API)` plus `eachAlsoFirstRow` may produce a different cardinality than Java/.NET depending on how `eachAlsoFirstRow` honors the outer `forChannels`. Per `tests/latest/acceptance/base/fixtures.ts`, `eachAlsoFirstRow` is pre-bound to `[API], [UI]`, making the outer `forChannels(UI, API)` redundant/conflicting. Flag as a TS consistency issue.
- Same pattern applies to `CancelOrderNegativeIsolatedTest`.

#### Exceptions (known divergences)

- `await (result).HasTime("...")` two-step `.Then().Clock()` chain in .NET isolated tests (e.g. `PlaceOrderPositiveIsolatedTest.ShouldRecordPlacementTimestamp`, `CancelOrderPositiveIsolatedTest`) — required by .NET's `ThenSuccess`/`ThenFailure` async split. Not a mismatch.
- TS `ThenFailure.and()` returns a separate `ThenFailureAnd`; TS `ThenSuccess.and()` returns `this`. Accepted asymmetry (documented in spec).

---

### Contract Tests (External Systems)

#### Class Coverage
| Class                                | Java | .NET | TypeScript |
|--------------------------------------|:----:|:----:|:----------:|
| BaseClockContractTest (abstract)     |  Y   |  Y   |     Y (registerClockContractTests helper) |
| ClockRealContractTest                |  Y   |  Y   |     Y      |
| ClockStubContractTest                |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest        |  Y   |  Y   |     Y (standalone, not inheriting Base) |
| BaseErpContractTest (abstract)       |  Y   |  Y   |     Y (registerErpContractTests helper) |
| ErpRealContractTest                  |  Y   |  Y   |     Y      |
| ErpStubContractTest                  |  Y   |  Y   |     Y      |
| BaseTaxContractTest (abstract)       |  Y   |  Y   |     Y (registerTaxContractTests helper) |
| TaxRealContractTest                  |  Y   |  Y   |     Y      |
| TaxStubContractTest                  |  Y   |  Y   |     Y      |

#### Method Differences — BaseClockContractTest
- `shouldBeAbleToGetTime` — present in all three.
- `shouldBeAbleToGetConfiguredTime` lives on `ClockStubContractIsolatedTest` (isolated subclass) in all three. Matches.

#### Method Differences — BaseErpContractTest
- `shouldBeAbleToGetProduct` — present in all three. Body identical (`.withSku("SKU-123").withUnitPrice(12.0)` → `.hasSku("SKU-123").hasPrice(12.0)`).

#### Method Differences — BaseTaxContractTest
- `shouldBeAbleToGetTaxRate` — present in all three. Body identical (`.withCode("US").withTaxRate(0.09)` → `.hasTaxRateIsPositive()`).

#### Body Differences
- TypeScript pattern uses a `registerXxxContractTests(test)` helper function instead of an abstract class + subclasses. The Real/Stub variant files set `process.env.EXTERNAL_SYSTEM_MODE = 'real' | 'stub'` and call the helper. Functionally equivalent to Java/.NET's `getFixedExternalSystemMode()` override. Accepted.

---

### E2E Tests

#### Class Coverage
| Class                  | Java | .NET | TypeScript |
|------------------------|:----:|:----:|:----------:|
| PlaceOrderPositiveTest |  Y   |  Y   |     Y      |

#### Method Differences — PlaceOrderPositiveTest
- `shouldPlaceOrder` — identical body across all three: `scenario.when().placeOrder().then().shouldSucceed()` with `@Channel({UI, API})` / `[ChannelData(UI, API)]` / `forChannels(UI, API)`.

---

### Smoke Tests

#### Class Coverage
| Class          | Location (Java)         | Java | .NET | TypeScript |
|----------------|-------------------------|:----:|:----:|:----------:|
| ClockSmokeTest | latest/smoke/external   |  Y   |  Y   |     Y      |
| ErpSmokeTest   | latest/smoke/external   |  Y   |  Y   |     Y      |
| TaxSmokeTest   | latest/smoke/external   |  Y   |  Y   |     Y      |
| ShopSmokeTest  | latest/smoke/system     |  Y   |  Y   |     Y      |

All method bodies identical (`scenario.assume().<system>().shouldBeRunning()`).

---

## Legacy Comparison

### Architectural Abstraction Summary
| Module | Expected Layer                   | Java  | .NET  | TypeScript | Match? |
|--------|----------------------------------|-------|-------|------------|--------|
| mod02  | Raw (direct HTTP)                | Raw   | Raw   | Raw        | Full   |
| mod03  | Raw (+ UI)                       | Raw   | Raw   | Raw        | Full   |
| mod04  | Client (typed API/UI client)     | Client| Client| Client     | Full   |
| mod05  | Driver (adapter abstraction)     | Driver| Driver| Driver     | Full (see body diffs) |
| mod06  | Channel Driver                   | Channel Driver | Channel Driver | Channel Driver | Full |
| mod07  | Use-Case DSL                     | Use-Case DSL | Use-Case DSL | Use-Case DSL | Full |
| mod08  | Scenario DSL                     | Scenario DSL | Scenario DSL | Scenario DSL | Full |
| mod09  | Scenario DSL + Clock             | Scenario DSL + Clock | Scenario DSL + Clock | Scenario DSL + Clock | Full |
| mod10  | Scenario DSL + Isolated          | Scenario DSL + Isolated | Scenario DSL + Isolated | Scenario DSL + Isolated | Full |
| mod11  | Scenario DSL + Contract          | Scenario DSL + Contract (no Tax) | Scenario DSL + Contract | Scenario DSL + Contract | Partial — Java missing Tax contract tests |

**No architectural abstraction mismatches per module** — all languages use the intended layer for each module. The only gap is coverage (Java mod11 missing Tax contract tests — see Module 11 below).

### Module Progression — Java
| Module | Layer                 | Delta vs Prior Module                                  | Logical? |
|--------|-----------------------|--------------------------------------------------------|----------|
| mod02  | Raw                   | baseline (smoke: raw HTTP health checks to shop/erp/tax) | —        |
| mod03  | Raw                   | adds e2e PlaceOrder Api/Ui using raw HTTP + Playwright  | Yes      |
| mod04  | Client                | introduces `shopApiClient`/`erpClient` (typed) wrapping raw HTTP; same tests rewritten | Yes |
| mod05  | Driver                | introduces `shopDriver`/`erpDriver` (driver port) wrapping mod04 clients; adds abstract `PlaceOrderPositive/NegativeBaseTest` with Api/Ui subclasses | Yes |
| mod06  | Channel Driver        | collapses Api/Ui subclasses into channel-parameterized tests via `@Channel({UI, API})`; single class per use case | Yes |
| mod07  | Use-Case DSL          | introduces `app.shop().placeOrder().execute()` fluent builder; replaces direct driver calls | Yes |
| mod08  | Scenario DSL          | introduces `scenario.given().product()...when()...then()` scenario DSL | Yes |
| mod09  | Scenario DSL + Clock  | adds `ClockSmokeTest` under smoke/external; no new e2e | Yes |
| mod10  | Scenario DSL + Isolated | adds acceptance tests with `@Isolated`, `@TimeDependent`, promotion, clock stubs | Yes |
| mod11  | Scenario DSL + Contract | adds external-system contract tests (Clock, Erp); no Tax folder | Yes (but Tax missing) |

### Module Progression — .NET
| Module | Layer                 | Delta vs Prior Module                                                                    | Logical? |
|--------|-----------------------|------------------------------------------------------------------------------------------|----------|
| mod02  | Raw                   | baseline — raw HTTP smoke                                                                | —        |
| mod03  | Raw                   | adds raw HTTP/Playwright e2e                                                             | Yes      |
| mod04  | Client                | introduces typed `_shopApiClient`/`_erpClient`                                           | Yes      |
| mod05  | Driver                | introduces `_shopDriver`/`_erpDriver`; adds Base/Api/Ui subclass pattern                 | Yes      |
| mod06  | Channel Driver        | consolidates to `[ChannelData(UI, API)]` tests                                           | Yes      |
| mod07  | Use-Case DSL          | introduces `App.Shop().PlaceOrder()`                                                     | Yes      |
| mod08  | Scenario DSL          | introduces `Scenario(channel).Given()...Then()`                                          | Yes      |
| mod09  | Scenario DSL + Clock  | adds `ClockSmokeTest`                                                                    | Yes      |
| mod10  | Scenario DSL + Isolated | adds acceptance tests with `[Collection("Isolated")]`, `[Time]`, promotion, clock stubs| Yes      |
| mod11  | Scenario DSL + Contract | adds Clock/Erp/Tax contract tests (Real, Stub, StubIsolated)                          | Yes      |

### Module Progression — TypeScript
| Module | Layer                 | Delta vs Prior Module                                                                | Logical? |
|--------|-----------------------|--------------------------------------------------------------------------------------|----------|
| mod02  | Raw                   | baseline — raw HTTP smoke (uses `fetch`)                                             | —        |
| mod03  | Raw                   | adds raw fetch e2e. TS lacks a `base/BaseRawTest.ts` sibling to Java's `BaseRawTest.java` — imports `getShopApiBaseUrl`/`getErpBaseUrl` from a helper but does not share a base class | Yes with divergence |
| mod04  | Client                | introduces `shopApiClient`/`erpClient` fixtures                                      | Yes      |
| mod05  | Driver                | introduces `shopDriver`/`erpDriver` fixtures; lacks shared `*BaseTest` abstract (Api and Ui spec files duplicate body) | Yes with divergence |
| mod06  | Channel Driver        | uses `forChannels(UI, API)` + combined tests                                         | Yes      |
| mod07  | Use-Case DSL          | introduces `useCase.shop().placeOrder().execute()`                                   | Yes      |
| mod08  | Scenario DSL          | introduces `scenario.given()...`                                                     | Yes      |
| mod09  | Scenario DSL + Clock  | adds `clock-smoke-test.spec.ts`                                                      | Yes      |
| mod10  | Scenario DSL + Isolated | adds acceptance tests with `test.describe('@isolated')` + `serial` mode            | Yes      |
| mod11  | Scenario DSL + Contract | adds Clock/Erp/Tax contract tests                                                  | Yes      |

No progression regressions or layer skips in any language. All modules introduce only the concept they are supposed to.

---

### mod02

#### Architectural Abstraction
| Module | Expected | Java | .NET | TypeScript | Match? |
|--------|----------|------|------|------------|--------|
| mod02  | Raw      | Raw  | Raw  | Raw        | Full   |

#### Class Coverage
| Class                  | Java | .NET | TypeScript | Match? |
|------------------------|:----:|:----:|:----------:|:------:|
| ErpSmokeTest           |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest           |  Y   |  Y   |     Y      |  Full  |
| ShopApiSmokeTest       |  Y   |  Y   |     Y      |  Full  |
| ShopUiSmokeTest        |  Y   |  Y   |     Y      |  Full  |
| BaseRawTest            |  Y (base/BaseRawTest.java) | Y (Base/BaseRawTest.cs) | Y (base/BaseRawTest.ts — verified: imports exist) | Full |

All tests hit `/health` and assert status 200. Bodies equivalent.

---

### mod03

#### Architectural Abstraction
| Module | Expected | Java | .NET | TypeScript | Match? |
|--------|----------|------|------|------------|--------|
| mod03  | Raw      | Raw  | Raw  | Raw        | Full   |

#### Class Coverage
| Class                           | Java | .NET | TypeScript | Match? |
|---------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveApiTest       |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderPositiveUiTest        |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeApiTest       |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeUiTest        |  Y   |  Y   |     Y      |  Full  |
| BaseE2eTest (e2e/base)          |  Y   |  Y   |     N (no dedicated base class; fixtures.ts takes its place) | Partial |
| BaseRawTest (mod03/base)        |  Y   |  Y   |     N (inlined helpers in fixtures/module) | Partial |

TS uses Playwright `fixtures.ts` instead of an abstract base class. This is an idiomatic choice, but it means there is no single source of truth for raw HTTP client setup in TS mod03 (fetch is inlined per test).

#### Body Differences — PlaceOrderPositiveApiTest
- Java/.NET build JSON bodies using templated strings and use `httpObjectMapper`/`System.Text.Json` to parse. TS uses `fetch` + `JSON.stringify` + `.json()`. Test intent is identical: create product in ERP, place order, view order, assert fields.
- TS omits the `brand`/`category`/`description` assertions on the viewed order (Java/.NET don't assert these either — matches).

---

### mod04

#### Architectural Abstraction
| Module | Expected | Java  | .NET  | TypeScript | Match? |
|--------|----------|-------|-------|------------|--------|
| mod04  | Client   | Client| Client| Client     | Full   |

#### Class Coverage
| Class                           | Java | .NET | TypeScript | Match? |
|---------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveApiTest       |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderPositiveUiTest        |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeApiTest       |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeUiTest        |  Y   |  Y   |     Y      |  Full  |
| ErpSmokeTest                    |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest                    |  Y   |  Y   |     Y      |  Full  |
| ShopApiSmokeTest                |  Y   |  Y   |     Y      |  Full  |
| ShopUiSmokeTest                 |  Y   |  Y   |     Y      |  Full  |
| BaseClientTest                  |  Y   |  Y   |     N      | Partial |
| BaseE2eTest                     |  Y   |  Y   |     N      | Partial |

TS substitutes `fixtures.ts` for both base classes; mirrors mod03.

#### Body Differences
- Java: builds `ExtCreateProductRequest` and calls `erpClient.createProduct(...)`.
- .NET: calls typed ERP client similarly.
- TS: `erpClient.createProduct({ sku, price })` — does not take `title`, `description`, `category`, `brand`. Minimized DTO. The adapter likely fills defaults internally. Functional equivalent.

---

### mod05

#### Architectural Abstraction
| Module | Expected | Java   | .NET   | TypeScript | Match? |
|--------|----------|--------|--------|------------|--------|
| mod05  | Driver   | Driver | Driver | Driver     | Full   |

#### Class Coverage
| Class                              | Java | .NET | TypeScript | Match? |
|------------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveApiTest          |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderPositiveUiTest           |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderPositiveBaseTest (abs.)  |  Y   |  Y   |     N      | Mismatch — TS duplicates body between Api/Ui .spec.ts files instead of sharing |
| PlaceOrderNegativeApiTest          |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeUiTest           |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeBaseTest (abs.)  |  Y   |  Y   |     N      | Mismatch — same duplication pattern |
| ShopApiSmokeTest                   |  Y   |  Y   |     Y      |  Full  |
| ShopUiSmokeTest                    |  Y   |  Y   |     Y      |  Full  |
| ShopBaseSmokeTest (abs.)           |  Y   |  Y   |     Y (ShopBaseSmokeTest.ts present — helper module, not a test class) | Partial |
| ErpSmokeTest                       |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest                       |  Y   |  Y   |     Y      |  Full  |
| BaseDriverTest                     |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseE2eTest                        |  Y   |  Y   |     N (fixtures.ts) | Partial |

#### Body Differences
- Java/.NET test bodies live on `PlaceOrderPositiveBaseTest` with two empty subclasses (`Api`, `Ui`) that select client via `setUpShopApiClient()` or `setUpShopUiClient()`. TS has two full test bodies in `place-order-positive-api-test.spec.ts` and `place-order-positive-ui-test.spec.ts` with near-identical content (only fixture import changes: `apiTest` vs `uiTest`). No shared abstract test.

---

### mod06

#### Architectural Abstraction
| Module | Expected       | Java            | .NET            | TypeScript      | Match? |
|--------|----------------|-----------------|-----------------|-----------------|--------|
| mod06  | Channel Driver | Channel Driver  | Channel Driver  | Channel Driver  | Full   |

#### Class Coverage
| Class                        | Java | .NET | TypeScript | Match? |
|------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveTest       |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeTest       |  Y   |  Y   |     Y      |  Full  |
| ShopSmokeTest                |  Y   |  Y   |     Y      |  Full  |
| ErpSmokeTest                 |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest                 |  Y   |  Y   |     Y      |  Full  |
| BaseChannelDriverTest        |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseE2eTest                  |  Y   |  Y   |     N (fixtures.ts) | Partial |

#### Body Differences
- Java `@TestTemplate @Channel({UI, API})` — .NET `[Theory] [ChannelData(UI, API)]` — TS `forChannels(UI, API)(() => test(...))`. All produce 2 runs per test. Match.

---

### mod07

#### Architectural Abstraction
| Module | Expected     | Java         | .NET         | TypeScript   | Match? |
|--------|--------------|--------------|--------------|--------------|--------|
| mod07  | Use-Case DSL | Use-Case DSL | Use-Case DSL | Use-Case DSL | Full   |

#### Class Coverage
| Class                   | Java | .NET | TypeScript | Match? |
|-------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveTest  |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeTest  |  Y   |  Y   |     Y      |  Full  |
| ShopSmokeTest           |  Y   |  Y   |     Y      |  Full  |
| ErpSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| BaseUseCaseDslTest      |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseE2eTest             |  Y   |  Y   |     N (fixtures.ts) | Partial |

#### Body Differences
- Java uses `app.shop().placeOrder()`. .NET uses `App.Shop().PlaceOrder()`. TS uses `useCase.shop().placeOrder()`.
- Naming divergence: Java/.NET expose the root as `app`/`App`; TS exposes it as `useCase`. Functional parity.

---

### mod08

#### Architectural Abstraction
| Module | Expected     | Java         | .NET         | TypeScript   | Match? |
|--------|--------------|--------------|--------------|--------------|--------|
| mod08  | Scenario DSL | Scenario DSL | Scenario DSL | Scenario DSL | Full   |

#### Class Coverage
| Class                   | Java | .NET | TypeScript | Match? |
|-------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveTest  |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeTest  |  Y   |  Y   |     Y      |  Full  |
| ShopSmokeTest           |  Y   |  Y   |     Y      |  Full  |
| ErpSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| BaseScenarioDslTest     |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseE2eTest             |  Y   |  Y   |     N (fixtures.ts) | Partial |

Bodies equivalent.

---

### mod09

#### Architectural Abstraction
| Module | Expected             | Java                 | .NET                 | TypeScript           | Match? |
|--------|----------------------|----------------------|----------------------|----------------------|--------|
| mod09  | Scenario DSL + Clock | Scenario DSL + Clock | Scenario DSL + Clock | Scenario DSL + Clock | Full   |

#### Class Coverage
| Class                   | Java | .NET | TypeScript | Match? |
|-------------------------|:----:|:----:|:----------:|:------:|
| ShopSmokeTest           |  Y   |  Y   |     Y      |  Full  |
| ClockSmokeTest          |  Y   |  Y   |     Y      |  Full  |
| ErpSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| TaxSmokeTest            |  Y   |  Y   |     Y      |  Full  |
| BaseScenarioDslTest     |  Y   |  Y   |     N (fixtures.ts) | Partial |

---

### mod10

#### Architectural Abstraction
| Module | Expected                | Java                    | .NET                    | TypeScript              | Match? |
|--------|-------------------------|-------------------------|-------------------------|-------------------------|--------|
| mod10  | Scenario DSL + Isolated | Scenario DSL + Isolated | Scenario DSL + Isolated | Scenario DSL + Isolated | Full   |

#### Class Coverage
| Class                              | Java | .NET | TypeScript | Match? |
|------------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveTest             |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeTest             |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderPositiveIsolatedTest     |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderNegativeIsolatedTest     |  Y   |  Y   |     Y      |  Full  |
| BaseAcceptanceTest                 |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseScenarioDslTest                |  Y   |  Y   |     N (fixtures.ts) | Partial |

Bodies equivalent (verified `PlaceOrderPositiveIsolatedTest` — `shouldApplyFullPriceOnWeekday`, `shouldApplyDiscountWhenPromotionIsActive`, `shouldRecordPlacementTimestamp` all present and identical in all three).

---

### mod11

#### Architectural Abstraction
| Module | Expected                | Java                    | .NET                    | TypeScript              | Match? |
|--------|-------------------------|-------------------------|-------------------------|-------------------------|--------|
| mod11  | Scenario DSL + Contract | Scenario DSL + Contract (partial — no Tax) | Scenario DSL + Contract | Scenario DSL + Contract | Partial — Java missing Tax |

#### Class Coverage
| Class                              | Java | .NET | TypeScript | Match? |
|------------------------------------|:----:|:----:|:----------:|:------:|
| PlaceOrderPositiveTest (e2e)       |  Y   |  Y   |     Y      |  Full  |
| BaseE2eTest                        |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseScenarioDslTest                |  Y   |  Y   |     N (fixtures.ts) | Partial |
| BaseExternalSystemContractTest     |  Y   |  Y   |     Y      |  Full  |
| BaseClockContractTest              |  Y   |  Y   |     Y (helper) | Full |
| ClockRealContractTest              |  Y   |  Y   |     Y      |  Full  |
| ClockStubContractTest              |  Y   |  Y   |     Y      |  Full  |
| ClockStubContractIsolatedTest      |  Y   |  Y   |     Y      |  Full  |
| BaseErpContractTest                |  Y   |  Y   |     Y (helper) | Full |
| ErpRealContractTest                |  Y   |  Y   |     Y      |  Full  |
| ErpStubContractTest                |  Y   |  Y   |     Y      |  Full  |
| BaseTaxContractTest                |  **N**   |  Y   |     Y      | **Mismatch — Java missing** |
| TaxRealContractTest                |  **N**   |  Y   |     Y      | **Mismatch — Java missing** |
| TaxStubContractTest                |  **N**   |  Y   |     Y      | **Mismatch — Java missing** |

Java `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod11/contract/` contains only `base/`, `clock/`, and `erp/`. No `tax/` subdirectory.

---

## Architecture Comparison

### Channels Layer
| Class/Constant                                              | Java | .NET | TypeScript | Match? |
|-------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ChannelType (UI, API constants)                             |  Y (String `"UI"`, `"API"`) | Y (const `"UI"`, `"API"`) | Y (lowercase `'ui'`, `'api'`) | Value-case mismatch |

TypeScript uses lowercase `'ui'`/`'api'` (see `system-test/typescript/src/testkit/channel/channel-type.ts`). Java and .NET use uppercase `"UI"`/`"API"`. Used internally only (e.g., env var lowercasing in `withApp.ts`) so no external API impact, but the discrepancy is visible across the stack and reduces searchability.

### Common Layer
| File                                  | Java                | .NET                              | TypeScript                | Match? |
|---------------------------------------|---------------------|-----------------------------------|---------------------------|:------:|
| Converter                             | Converter.java      | Converter.cs                      | converter.ts              | Full   |
| Result                                | Result.java         | Result.cs                         | result.ts                 | Full   |
| ResultAssert(Extensions)              | ResultAssert.java   | ResultAssertExtensions.cs         | result-assert.ts          | Full (naming differs) |
| dtos (barrel export)                  | —                   | —                                 | dtos.ts                   | TS-only barrel; informational. |

#### Exceptions (known divergences)
- `Closer.java` — Java-only (checked-exception adapter). Accepted.
- `VoidValue.cs`, `ResultTaskExtensions.cs` — .NET-only (generic `void` stand-in and async composition). Accepted.

### Driver Ports Layer
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| Shop port: ShopDriver / IShopDriver / shop-driver.ts             |  Y   |  Y   |     Y      | Full   |
| Shop DTOs: PlaceOrderRequest, PlaceOrderResponse, ViewOrderResponse, PublishCouponRequest, BrowseCouponsResponse, OrderStatus | Y | Y | Y | Full |
| Shop error DTO: SystemError                                      |  Y (dtos/error/) | Y (Dtos/Error/) | Y (dtos/errors/) | Full (folder case/plural differs) |
| SystemResults (value-type factories)                             |  Y (in dsl/core/usecase/shop/commons) | Y (in Driver.Port/Shop/) | Y (in dsl/core/usecase/shop/commons) | Exception — see below |
| External: ClockDriver / IClockDriver / clock-driver.ts           |  Y   |  Y   |     Y      | Full   |
| External: ErpDriver / IErpDriver / erp-driver.ts                 |  Y   |  Y   |     Y      | Full   |
| External: TaxDriver / ITaxDriver / tax-driver.ts                 |  Y   |  Y   |     Y      | Full   |
| External DTOs (clock/erp/tax: GetXResponse, ReturnsXRequest, error DTO) | Y | Y | Y | Full (TS puts error DTO at top of dtos/ folder instead of dtos/error/ subfolder) |

#### Exceptions (known divergences)
- `SystemResults` lives in `Driver.Port/Shop/` in .NET (vs `dsl/core/usecase/shop/commons/` in Java/TS). Moving it into `Shop.csproj` (the use-case-dsl project) would create a circular project reference: `Driver.Adapter` consumes `SystemResults` (in `ShopUiDriver.cs`, `BasePage.cs`) and `Shop.csproj` already references `Driver.Adapter`. Java/TS don't hit this because they're single-module. **TODO (later):** resolve by inlining `SystemResults.*` calls in the two `Driver.Adapter` files, then move to `Dsl.Core/UseCase/Shop/Commons/` to match Java/TS.

### Clients Layer (Driver Adapters)
#### Shop (API)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ShopApiDriver                                                    |  Y   |  Y   |     Y      | Full   |
| SystemErrorMapper                                                |  Y   |  Y   |     Y      | Full   |
| ShopApiClient                                                    |  Y   |  Y   |     Y      | Full   |
| Controllers (HealthController, OrderController, CouponController) | Y   |  Y   |     Y      | Full   |
| Dtos/Error/ProblemDetailResponse                                 |  Y   |  Y   |     Y      | Full   |

#### Shop (UI)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ShopUiDriver                                                     |  Y   |  Y   |     Y      | Full   |
| ShopUiClient                                                     |  Y   |  Y   |     Y      | Full   |
| Pages: BasePage, HomePage, NewOrderPage, OrderDetailsPage, OrderHistoryPage, CouponManagementPage | Y | Y | Y | Full |

#### External (Clock / Erp / Tax)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ClockRealDriver, ClockStubDriver                                 |  Y   |  Y   |     Y      | Full   |
| ErpRealDriver, ErpStubDriver                                     |  Y   |  Y   |     Y      | Full   |
| TaxRealDriver, TaxStubDriver                                     |  Y   |  Y   |     Y      | Full   |
| Per-system client subfolders (client/)                           |  Y   |  Y   |     Y      | Full   |

#### Shared infrastructure
| File                                                             | Java                     | .NET                        | TypeScript                | Match? |
|------------------------------------------------------------------|--------------------------|-----------------------------|---------------------------|:------:|
| JSON HTTP client                                                 | shared/client/http/JsonHttpClient.java | Shared/Client/Http/JsonHttpClient.cs | shared/http/http-client.ts | Naming/path differ (TS drops `client/` nesting, drops `Json` prefix) |
| HTTP status constants                                            | HttpStatus.java          | HttpStatus.cs               | HttpStatus.ts             | Full   |
| Playwright page client                                           | shared/client/playwright/PageClient.java | Shared/Client/Playwright/PageClient.cs | shared/playwright/PageClient.ts | Full (path nesting differs) |
| Playwright test fixture helper                                   | (baked into test infra)  | (baked into test infra)     | shared/playwright/withApp.ts | TS-only by necessity (TS Playwright lifecycle). |
| WireMock client                                                  | shared/client/wiremock/JsonWireMockClient.java | Shared/Client/WireMock/JsonWireMockClient.cs | shared/wiremock/wiremock-client.ts | Naming/path differ (drops `Json` prefix, drops `client/` nesting) |

### Use Case DSL Layer
| File                                                  | Java | .NET | TypeScript | Match? |
|-------------------------------------------------------|:----:|:----:|:----------:|:------:|
| UseCaseDsl                                            |  Y   |  Y   |     Y      | Full   |
| Configuration                                         |  Y   |  Y   |     Y (defaults.ts or equivalent) | Full |
| ShopDsl                                               |  Y   |  Y   |     Y      | Full   |
| Shop UseCases: PlaceOrder, CancelOrder, ViewOrder, PublishCoupon, BrowseCoupons, DeliverOrder, GoToShop | Y | Y | Y | Full |
| Shop Verification classes: PlaceOrderVerification, BrowseCouponsVerification, ViewOrderVerification | Y | Y | Y | Full |
| Shop usecases/base/: BaseShopUseCase/BaseShopCommand  |  Y (BaseShopUseCase) | Y (BaseShopCommand) | Y (BaseShopUseCase) | Naming differs |
| Shop usecases/base/: ShopUseCaseResult                |  —   |  Y   |     —      | .NET-only helper type |
| Shop usecases/base/: SystemErrorFailureVerification   |  —   |  Y   |     —      | .NET-only helper type |
| Shop commons/: SystemResults                          |  Y (dsl/core/usecase/shop/commons/SystemResults.java) | — (in Driver.Port/Shop) | Y (dsl/core/usecase/shop/commons/system-results.ts) | Exception — see Driver Ports Layer > Exceptions |
| ClockDsl, ErpDsl, TaxDsl                              |  Y   |  Y   |     Y      | Full   |
| External Clock UseCases: GetTime, GetTimeVerification, GoToClock, ReturnsTime | Y | Y | Y | Full |
| External Erp UseCases: GetProduct, ReturnsProduct, ReturnsPromotion, GoToErp (with verifications) | Y | Y | Y | Full |
| External Tax UseCases: GetTax, ReturnsTaxRate, GoToTax (with verifications) | Y | Y | Y | Full |
| External */UseCases/Base/: XxxErrorVerification, XxxUseCaseResult | Y (partial) | Y | Y (partial) | Full |

### Scenario DSL Layer

#### Port (interfaces)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ScenarioDsl / IScenarioDsl / scenario-dsl.ts (top-level)         |  Y (dsl/port/ScenarioDsl.java) | Y (IScenarioDsl.cs) | Y (dsl/port/scenario-dsl.ts) | Full |
| ChannelMode                                                      |  Y   |  Y   |     Y (channel-mode.ts) | Full |
| ExternalSystemMode                                               |  Y   |  Y   |     Y (external-system-mode.ts) | Full |
| AssumeStage / IAssumeStage                                       |  Y   |  Y   |     Y      | Full   |
| GivenStage / IGivenStage                                         |  Y   |  Y   |     Y      | Full   |
| WhenStage / IWhenStage                                           |  Y   |  Y   |     Y      | Full   |
| ThenStage / IThenStage                                           |  Y   |  Y   |     Y      | Full   |
| ThenResultStage / IThenResultStage                               |  Y   |  Y   |     Y      | Full   |
| Given/Steps: GivenClock, GivenCountry, GivenCoupon, GivenOrder, GivenProduct, GivenPromotion | Y | Y | Y | Full |
| When/Steps: WhenBrowseCoupons, WhenCancelOrder, WhenPlaceOrder, WhenPublishCoupon, WhenViewOrder | Y | Y | Y | Full |
| Then/Steps: ThenClock, ThenCountry, ThenCoupon, ThenOrder, ThenProduct, ThenSuccess, ThenFailure | Y | Y | Y | Full |
| Then/Steps/base/: ThenStep / IThenStep                           |  Y (ThenStep.java) | **N** (absent — exception) | Y (then-step.ts) | Exception (.NET) |
| Then/Steps: IThenSuccessAnd.cs                                   |  —   |  Y   |     —      | Exception (.NET async split) |
| Then/Steps: IThenFailureAnd.cs / then-failure-and.ts             |  —   |  Y   |     Y      | Exception (.NET + TS async split) |

#### Core (implementation)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| ScenarioDslImpl / ScenarioDsl / scenario-dsl.ts                  |  Y (dsl/core/ScenarioDslImpl.java) | Y (Dsl.Core/Scenario/ScenarioDsl.cs) | Y (dsl/scenario-dsl.ts and dsl/core/scenario/scenario-dsl.ts) | Full |
| ExecutionResult                                                  |  Y   |  Y   |     Y (execution-result.ts) | Full |
| ExecutionResultBuilder                                           |  Y   |  Y   |     Y (execution-result-builder.ts) | Full |
| ExecutionResultContext                                           |  Y   |  Y   |     **N** (TS has scenario-context.ts and app-context.ts, not ExecutionResultContext) | Naming/factoring differs |
| ScenarioDefaults / GherkinDefaults / defaults.ts                 |  Y (ScenarioDefaults.java) | Y (GherkinDefaults.cs) | Y (dsl/core/defaults.ts) | Naming differs |
| BaseClause                                                       |  —   |  Y (Scenario/BaseClause.cs) | — | .NET-only helper. Does not parallel anything in Java/TS. |
| AssumeImpl / AssumeStage / assume-stage.ts                       |  Y (AssumeImpl.java) | Y (Assume/AssumeStage.cs) | Y (scenario/assume/assume-stage.ts) | Full |
| GivenImpl / GivenStage / given-stage.ts                          |  Y   |  Y   |     Y      | Full   |
| WhenImpl / WhenStage / when-stage.ts                             |  Y   |  Y   |     Y      | Full   |
| ThenImpl / ThenStage / then-*.ts                                 |  Y (ThenImpl + ThenResultImpl) | Y (ThenStage + ThenStageBase) | Y (split across then-place-order.ts etc.) | TS organizes by use case instead of by Then variant; .NET uses ThenStageBase (exception). |
| Given Steps: GivenClock, GivenCountry, GivenCoupon, GivenOrder, GivenProduct, GivenPromotion | Y (given/steps/GivenXImpl.java) | Y (Given/Steps/GivenX.cs) | Y (scenario/given/given-x.ts) | Full |
| When Steps: WhenBrowseCoupons, WhenCancelOrder, WhenPlaceOrder, WhenPublishCoupon, WhenViewOrder | Y | Y | Y | Full |
| Then Steps: ThenClock, ThenCountry, ThenCoupon, ThenFailure, ThenOrder, ThenProduct, ThenSuccess | Y (then/steps/) | Y (Then/Steps/) | Y (scenario/then/then-place-order.ts contains all; no per-entity files) | Factoring differs |
| Then/Steps BaseThenStep / Base/                                  |  Y (BaseThenStep.java) | Y (Base/) | — (no base file separate) | Minor |
| .NET-only Then splits: ThenSuccessOrder, ThenFailureOrder, BaseThenResultOrder, ThenSuccessCoupon, ThenFailureCoupon, BaseThenResultCoupon, ThenSuccessAnd, ThenFailureAnd | — | Y | — | Exception |

#### Shared (use case framework)
| File                                                             | Java | .NET | TypeScript | Match? |
|------------------------------------------------------------------|:----:|:----:|:----------:|:------:|
| UseCase / IUseCase                                               |  Y (UseCase.java) | Y (IUseCase.cs) | Y (use-case interface in base-use-case.ts) | Full |
| BaseUseCase                                                      |  Y   |  Y   |     Y (base-use-case.ts) | Full |
| UseCaseContext                                                   |  Y   |  Y   |     Y (use-case-context.ts) | Full |
| UseCaseResult                                                    |  Y   |  Y   |     Y (use-case-result.ts) | Full |
| ResponseVerification                                             |  Y   |  Y   |     Y (response-verification.ts) | Full |
| VoidVerification                                                 |  Y   |  Y   |     Y (void-verification.ts) | Full |
| ErrorVerification (shared, shop-focused)                         |  Y (dsl/core/shared/ErrorVerification.java) | **N** (replaced with per-domain Xxx ErrorVerification and per-domain SystemErrorFailureVerification in UseCases/Base/) | Y (dsl/core/shared/error-verification.ts) | Mismatch — .NET uses domain-specific variants |

#### Exceptions (known divergences)
- .NET async adaptation requires `ThenSuccess`/`ThenFailure` split, `ThenSuccessAnd`/`ThenFailureAnd`, and `ThenStageBase` (listed in spec). Accepted.
- TS async adaptation requires `ThenFailureAnd` with no matching `ThenSuccessAnd`. Accepted.
- Missing `IThenStep<TThen>` in .NET is intentional. Accepted.

---

## Summary of Required Changes

Total actionable differences found: 23

### By language
- **Java**: 1 action item (missing Tax contract tests in mod11).
- **.NET**: 1 action item (centralize `ErrorVerification` into `Dsl.Core/Shared/` to match Java/TS shared convention; keep domain-specific `XxxErrorVerification` as thin wrappers if useful).
- **TypeScript**: 21 action items (mostly introducing shared abstract base classes in legacy modules and minor alignment fixes).

### By area
- Architectural mismatches (legacy): 0
- Progression mismatches (legacy): 0
- Test — Acceptance: 4 (TS)
- Test — Contract: 3 (Java — add Tax; TS — none needed, helper pattern accepted)
- Test — E2E: 0
- Test — Smoke: 0
- Architecture — Clients: 3 (TS naming/folder nesting for shared http/wiremock)
- Architecture — Drivers: 1 (TS errors folder pluralization / nesting)
- Architecture — Channels: 1 (TS channel-type value casing)
- Architecture — Use Case DSL: 1 (naming of root variable `useCase` vs `app` in TS)
- Architecture — Scenario DSL: 1 (.NET shared ErrorVerification absent; TS ExecutionResultContext naming)
- Architecture — Common: 0 (all differences are accepted exceptions)
- Legacy infrastructure — missing shared base classes (TS): 8 (mod03, mod04, mod05, mod06, mod07, mod08, mod09, mod10, mod11)

Note: Java's single item is purely test coverage (Tax contract tests). Everything else — Closer.java, package naming conventions, single-file test bodies — is either accepted as an exception or is the reference pattern that the other languages should adopt.
