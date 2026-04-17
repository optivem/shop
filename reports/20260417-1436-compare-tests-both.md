System Test Comparison Report
=============================

Mode: both

Reference: Java. Default action for any difference is to align .NET and TypeScript to the Java implementation.

---

## Latest Comparison

Test root directories:
- Java: `system-test/java/src/test/java/com/optivem/shop/systemtest/latest/`
- .NET: `system-test/dotnet/SystemTests/Latest/`
- TypeScript: `system-test/typescript/tests/latest/`

### Acceptance Tests

#### Class Coverage
| Class Name                           | Java | .NET | TypeScript |
|--------------------------------------|------|------|------------|
| BrowseCouponsPositiveTest            |  Y   |  Y   |     Y      |
| CancelOrderNegativeIsolatedTest      |  Y   |  Y   |     Y      |
| CancelOrderNegativeTest              |  Y   |  Y   |     Y      |
| CancelOrderPositiveIsolatedTest      |  Y   |  Y   |     Y      |
| CancelOrderPositiveTest              |  Y   |  Y   |     Y      |
| PlaceOrderNegativeIsolatedTest       |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest               |  Y   |  Y   |     Y      |
| PlaceOrderPositiveIsolatedTest       |  Y   |  Y   |     Y      |
| PlaceOrderPositiveTest               |  Y   |  Y   |     Y      |
| PublishCouponNegativeTest            |  Y   |  Y   |     Y      |
| PublishCouponPositiveTest            |  Y   |  Y   |     Y      |
| ViewOrderNegativeTest                |  Y   |  Y   |     Y      |
| ViewOrderPositiveTest                |  Y   |  Y   |     Y      |

No missing acceptance classes.

#### Method Differences — BrowseCouponsPositiveTest
Identical across languages. Method: `shouldBeAbleToBrowseCoupons`, channels UI+API, body `.when().browseCoupons().then().shouldSucceed()`.

#### Method Differences — PlaceOrderPositiveTest

| Method Name (Java)                                                           | .NET | TypeScript | Match? |
|------------------------------------------------------------------------------|------|------------|--------|
| shouldBeAbleToPlaceOrderForValidInput                                        |  Y   |  Y         |  Full  |
| orderStatusShouldBePlacedAfterPlacingOrder                                   |  Y   |  Y         |  Full  |
| shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity                      |  Y   |  Y         |  Full  |
| shouldPlaceOrderWithCorrectBasePriceParameterized                            |  Y   |  Y         |  Full  |
| orderPrefixShouldBeORD                                                       |  Y   |  Y         |  Full  |
| discountRateShouldBeAppliedForCoupon                                         |  Y   |  Y         |  Full  |
| discountRateShouldBeNotAppliedWhenThereIsNoCoupon                            |  Y   |  Y         |  Full  |
| subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon |  Y   |  Y      |  Full  |
| subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon                             |  Y   |  Y         |  Full  |
| correctTaxRateShouldBeUsedBasedOnCountry                                     |  Y   |  Y         |  Full  |
| totalPriceShouldBeSubtotalPricePlusTaxAmount                                 |  Y   |  Y         |  Full  |
| couponUsageCountHasBeenIncrementedAfterItsBeenUsed                           |  Y   |  Y         |  Full  |
| orderTotalShouldIncludeTax                                                   |  Y   |  Y         |  Full  |
| orderTotalShouldReflectCouponDiscount                                        |  Y   |  Y         |  Full  |
| orderTotalShouldApplyCouponDiscountAndTax                                    |  Y   |  Y         |  Full  |

Full alignment.

#### Method Differences — PlaceOrderNegativeTest

| Method Name (Java)                                | .NET | TypeScript | Match? |
|---------------------------------------------------|------|------------|--------|
| shouldRejectOrderWithInvalidQuantity              |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNonExistentSku               |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNegativeQuantity             |  Y   |     N (merged) |  Partial |
| shouldRejectOrderWithZeroQuantity                 |  Y   |     N (merged) |  Partial |
| shouldRejectOrderWithEmptySku                     |  Y   |     Y      |  Full  |
| shouldRejectOrderWithEmptyQuantity                |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNonIntegerQuantity           |  Y   |     Y      |  Full  |
| shouldRejectOrderWithEmptyCountry                 |  Y   |     Y      |  Full  |
| shouldRejectOrderWithInvalidCountry               |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullQuantity                 |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullSku                      |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullCountry                  |  Y   |     Y      |  Full  |
| cannotPlaceOrderWithNonExistentCoupon             |  Y   |     Y      |  Full  |
| cannotPlaceOrderWithCouponThatHasExceededUsageLimit |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNonPositiveQuantity          |  N   |     Y      |  TS extra |

Differences:
- TypeScript merges `shouldRejectOrderWithNegativeQuantity` and `shouldRejectOrderWithZeroQuantity` into a single parameterized test `shouldRejectOrderWithNonPositiveQuantity` with values `['-10', '-1', '0']`. Java/.NET have two separate non-parameterized tests using `-10` and `0`. TypeScript adds the `-1` case.

#### Body Differences — PlaceOrderNegativeTest

Method: shouldRejectOrderWithNegativeQuantity
- Java: single test, `withQuantity(-10)`.
- .NET: single test, `WithQuantity(-10)`.
- TypeScript: merged into `shouldRejectOrderWithNonPositiveQuantity` parameterized over `['-10', '-1', '0']`.

Method: shouldRejectOrderWithZeroQuantity
- Java: single test, `withQuantity(0)`.
- .NET: single test, `WithQuantity(0)`.
- TypeScript: merged into `shouldRejectOrderWithNonPositiveQuantity`.

Method: shouldRejectOrderWithEmptySku / shouldRejectOrderWithEmptyQuantity / shouldRejectOrderWithEmptyCountry
- Java: `@ArgumentsSource(EmptyArgumentsProvider.class)` (empty-set provider).
- .NET: `[ChannelClassData(typeof(EmptyArgumentsProvider))]`.
- TypeScript: inline `['', '   ']` array — but uses the same two values that the Empty providers produce. Bodies equivalent.

Method: shouldRejectOrderWithNonIntegerQuantity
- Java: `@ValueSource(strings = {"3.5", "lala"})` with `alsoForFirstRow = UI`.
- .NET: `[ChannelInlineData("3.5"), ChannelInlineData("lala")]` with `AlsoForFirstRow`.
- TypeScript: `test.eachAlsoFirstRow(['3.5', 'lala'])`. Aligned.

#### Method Differences — PlaceOrderPositiveIsolatedTest

| Method Name                          | Java | .NET | TypeScript | Match? |
|--------------------------------------|------|------|------------|--------|
| shouldRecordPlacementTimestamp       |  Y   |  Y   |     Y      |  Full  |
| shouldApplyFullPriceWithoutPromotion |  Y   |  Y   |     Y      |  Full  |
| shouldApplyDiscountWhenPromotionIsActive |  Y |  Y   |     Y      |  Full  |

#### Method Differences — PlaceOrderNegativeIsolatedTest

| Method Name                          | Java | .NET | TypeScript | Match? |
|--------------------------------------|------|------|------------|--------|
| shouldRejectOrderPlacedAtYearEnd     |  Y   |  Y   |     Y      |  Full  |
| cannotPlaceOrderWithExpiredCoupon    |  Y   |  Y   |     Y      |  Full  |

Note: Java has `@TimeDependent` on `cannotPlaceOrderWithExpiredCoupon`, .NET has `[Time]`, TypeScript has no analogous marker. TypeScript omits the time-dependent metadata.

#### Method Differences — CancelOrderPositiveTest
Identical: `shouldHaveCancelledStatusWhenCancelled`, channels UI+API.

#### Method Differences — CancelOrderNegativeTest
Identical: `shouldNotCancelNonExistentOrder` (parameterized), `shouldNotCancelAlreadyCancelledOrder`, `cannotCancelNonExistentOrder`. All API-only channel.

#### Method Differences — CancelOrderPositiveIsolatedTest
Identical: `shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230` parameterized over 4 times.
- Java has `@TimeDependent`, .NET has `[Time]`, TypeScript has no marker. TypeScript lacks time-dependent metadata.

#### Method Differences — CancelOrderNegativeIsolatedTest
Identical: `cannotCancelAnOrderOn31stDecBetween2200And2230` parameterized over 5 times.
- Java has `@TimeDependent`, .NET has `[Time]`, TypeScript has no marker. TypeScript lacks time-dependent metadata.

#### Method Differences — PublishCouponPositiveTest

| Method Name                                   | Java | .NET | TypeScript | Match? |
|-----------------------------------------------|------|------|------------|--------|
| shouldBeAbleToPublishValidCoupon              |  Y   |  Y   |     Y      |  Full  |
| shouldBeAbleToPublishCouponWithEmptyOptionalFields |  Y |  Y  |     Y      |  Full  |
| shouldBeAbleToCorrectlySaveCoupon             |  Y   |  Y   |     Y      |  Full  |
| shouldPublishCouponSuccessfully               |  Y   |  Y   |     Y      |  Full  |

#### Method Differences — PublishCouponNegativeTest

| Method Name                                          | Java | .NET | TypeScript | Match? |
|------------------------------------------------------|------|------|------------|--------|
| cannotPublishCouponWithZeroOrNegativeDiscount        |  Y   |  Y   |     Y      |  Full  |
| cannotPublishCouponWithDiscountGreaterThan100percent |  Y   |  Y   |     Y      |  Full  |
| cannotPublishCouponWithDuplicateCouponCode           |  Y   |  Y   |     Y      |  Full  |
| cannotPublishCouponWithZeroOrNegativeUsageLimit      |  Y   |  Y   |     Y      |  Full  |
| shouldRejectCouponWithBlankCode                      |  Y   |  Y   |     Y      |  Full  |

Body differences — PublishCouponNegativeTest:
- `shouldRejectCouponWithBlankCode`: Java/.NET wire the channel as `ChannelType.API` alone; TS wraps the code loop in `forChannels(ChannelType.API)` — equivalent.
- `cannotPublishCouponWithZeroOrNegativeDiscount`: Java/.NET pass the parameter as string (`"0.0"`, `"-0.01"`, `"-0.15"`). TypeScript passes numbers (`0`, `-0.01`, `-0.15`). Data values equivalent but type differs.
- `cannotPublishCouponWithZeroOrNegativeUsageLimit`: Java/.NET pass string (`"0"`, `"-1"`, `"-100"`). TypeScript passes numbers (`0`, `-1`, `-100`).

#### Method Differences — ViewOrderPositiveTest
Identical: `shouldBeAbleToViewOrder`.

#### Method Differences — ViewOrderNegativeTest
Identical method `shouldNotBeAbleToViewNonExistentOrder` parameterized over 3 orders.

Body/channel difference:
- Java: `@Channel(value = {ChannelType.API}, alsoForFirstRow = ChannelType.UI)` — first row also exercised via UI.
- .NET: `[ChannelData(ChannelType.API, AlsoForFirstRow = new[] { ChannelType.UI })]` — matches Java.
- TypeScript: `forChannels(ChannelType.API)` plus plain `test.each`. No UI first-row coverage.

### Contract Tests

#### Class Coverage
| Class Name                          | Java | .NET | TypeScript |
|-------------------------------------|------|------|------------|
| BaseExternalSystemContractTest      |  Y   |  Y   |     N (structural) |
| BaseClockContractTest               |  Y   |  Y   |     N (structural) |
| ClockRealContractTest               |  Y   |  Y   |     Y      |
| ClockStubContractTest               |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest       |  Y   |  Y   |     Y      |
| BaseErpContractTest                 |  Y   |  Y   |     N (structural) |
| ErpRealContractTest                 |  Y   |  Y   |     Y      |
| ErpStubContractTest                 |  Y   |  Y   |     Y      |
| BaseTaxContractTest                 |  Y   |  Y   |     N (structural) |
| TaxRealContractTest                 |  Y   |  Y   |     Y      |
| TaxStubContractTest                 |  Y   |  Y   |     Y      |

Structural difference:
- Java/.NET use abstract Base<Clock|Erp|Tax>ContractTest classes with shared test methods. Real/Stub tests are thin subclasses that only override `FixedExternalSystemMode`. TypeScript has no base abstraction — each concrete `*-real-*.spec.ts` / `*-stub-*.spec.ts` file duplicates the body inline and sets `process.env.EXTERNAL_SYSTEM_MODE` at module scope.

#### Method Differences — Clock

| Method                              | Java       | .NET       | TypeScript | Match? |
|-------------------------------------|------------|------------|------------|--------|
| shouldBeAbleToGetTime (ClockReal)   |  Y (inherited) |  Y (inherited) |  Y  |  Body differs (see below) |
| shouldBeAbleToGetTime (ClockStub)   |  Y (inherited) |  Y (inherited) |  Y  |  Body differs |
| shouldBeAbleToGetConfiguredTime (ClockStubIsolated) |  Y |  Y |  Y  |  Full |

Body differences — ClockStubContractTest.shouldBeAbleToGetTime:
- Java: `.given().then().clock().hasTime()` (no time setup).
- .NET: `.Given().Then().Clock().HasTime()` (no time setup).
- TypeScript: `.given().clock().withTime().then().clock().hasTime()` — extra `.withTime()` step before assertion.

#### Method Differences — Erp

| Method                  | Java | .NET | TypeScript | Match? |
|-------------------------|------|------|------------|--------|
| shouldBeAbleToGetProduct | Y (inherited) | Y (inherited) | Y (in each file) | Full |

Both Real and Stub spec files contain identical body.

#### Method Differences — Tax

| Method                         | Java | .NET | TypeScript | Match? |
|--------------------------------|------|------|------------|--------|
| shouldBeAbleToGetTaxRate       | Y (inherited) | Y (inherited) | Y (in each file) | Full |
| shouldBeAbleToGetConfiguredTaxRate (Stub only) | Y | Y | Y | Full |

Body difference — shouldBeAbleToGetTaxRate:
- Java: `.withTaxRate(0.09)` — numeric.
- .NET: `.WithTaxRate(0.09m)` — decimal literal.
- TypeScript: `.withTaxRate('0.09')` — string.
Equivalent values but type differs.

### E2E Tests

#### Class Coverage
| Class Name               | Java | .NET | TypeScript |
|--------------------------|------|------|------------|
| PlaceOrderPositiveTest   |  Y   |  Y   |     Y      |

#### Method Differences — E2E PlaceOrderPositiveTest
Identical: `shouldPlaceOrder` with channels UI+API, body `.when().placeOrder().then().shouldSucceed()`.

### Smoke Tests

#### Class Coverage
| Class Name         | Java | .NET | TypeScript |
|--------------------|------|------|------------|
| ClockSmokeTest     |  Y   |  Y   |     Y      |
| ErpSmokeTest       |  Y   |  Y   |     Y      |
| TaxSmokeTest       |  Y   |  Y   |     Y      |
| ShopSmokeTest      |  Y   |  Y   |     Y      |

#### Method Differences
All four smoke tests have identical single method each (`shouldBeAbleToGoToClock` / `Erp` / `Tax` / `Shop`). Full alignment; bodies match.

---

## Legacy Comparison

### Architectural Abstraction Summary

| Module | Expected Layer    | Java   | .NET   | TypeScript | Match? |
|--------|-------------------|--------|--------|------------|--------|
| mod02  | Raw               | Raw (BaseRawTest + HttpClient + Playwright) | Raw (BaseRawTest + HttpClient + Playwright) | Raw (inline `fetch` + `chromium.launch`, no base class) | Partial (TS lacks base class) |
| mod03  | Raw               | Raw   | Raw    | Raw (+ WireMock `__admin/mappings` inline stubs) | Partial (TS uses stub-mode admin API inline) |
| mod04  | Client            | Client (ShopApiClient, ShopUiClient, ErpRealClient, TaxRealClient) | Client (same) | Client API (ShopApiClient) + **UI uses raw Playwright page object, not ShopUiClient** | MISMATCH (TS UI) |
| mod05  | Driver            | Driver (ShopApiDriver/ShopUiDriver/ErpRealDriver/TaxRealDriver) | Driver (same) | Driver (ShopApiDriver/ShopUiDriver/**ErpStubDriver/TaxStubDriver**) | Partial (TS uses stub external drivers vs real) |
| mod06  | Channel Driver    | Channel-aware ShopDriver via ChannelExtension | Channel-aware with `SetChannelAsync` per-test | Channel-aware with `ChannelContext.get` in fixture (uses stub external drivers) | Partial (TS uses stub external drivers vs real) |
| mod07  | Use-Case DSL      | UseCaseDsl with `app.erp().returnsProduct().sku(...).execute()` fluent builder | Same fluent builder (`Execute()`) | `useCase.shop().placeOrder({...})` plain object, no step-by-step builder | MISMATCH (TS skips builder semantics) |
| mod08  | Scenario DSL      | ScenarioDslImpl (scenario.given()...) | ScenarioDsl (Scenario(channel)...) | scenario-dsl.ts (scenario.given()...) | Full |
| mod09  | Scenario DSL + Clock | Scenario DSL with ClockDsl + ClockRealDriver/ClockStubDriver | Same | Same | Full |
| mod10  | Scenario DSL + Isolated | Scenario DSL plus PlaceOrderPositiveIsolatedTest / PlaceOrderNegativeIsolatedTest | Same | Same | Full |
| mod11  | Scenario DSL + Contract | Scenario DSL plus BaseExternalSystemContractTest hierarchy | Same | No Base contract class; inlined tests per real/stub spec file | Partial (TS lacks base contract class) |

Architectural Mismatches:
- mod04 (UI, TypeScript): UI tests directly drive Playwright page locators instead of calling a `ShopUiClient`. Java/.NET wrap UI in `ShopUiClient.openHomePage().clickNewOrder()` etc. TS has no `ShopUiClient` for mod04.
- mod04 (external systems, TypeScript): uses `ErpStubClient`/`TaxStubClient` whereas Java/.NET use `ErpRealClient`/`TaxRealClient`. Mode diverges.
- mod05/mod06 (external systems, TypeScript): uses `ErpStubDriver`/`TaxStubDriver` where Java/.NET use `ErpRealDriver`/`TaxRealDriver`.
- mod07 (TypeScript): use-case DSL is not a fluent builder. Calls look like function invocations with config object, not `.sku().quantity().execute()`. The pedagogical intent (introducing fluent use-case builder) is lost.
- mod02, mod11 (TypeScript): missing shared base test classes (`BaseRawTest`, `BaseExternalSystemContractTest`, `BaseClockContractTest`, `BaseErpContractTest`).

### Module Progression — Java

| Module | Layer                 | Delta vs Prior Module                                              | Logical? |
|--------|-----------------------|--------------------------------------------------------------------|----------|
| mod02  | Raw                   | (baseline) Smoke tests via raw HttpClient/Playwright               | —        |
| mod03  | Raw                   | Adds e2e PlaceOrder API/UI positive+negative                      | Yes      |
| mod04  | Client                | Introduces typed ShopApiClient, ShopUiClient, ErpRealClient, TaxRealClient; reuses same smoke tests | Yes |
| mod05  | Driver                | Introduces ShopDriver port + ShopApiDriver/ShopUiDriver/ErpRealDriver/TaxRealDriver; separate shop base tests for UI/API | Yes |
| mod06  | Channel Driver        | Single PlaceOrder{Positive,Negative}Test using `@Channel(UI,API)` via ChannelExtension; drops Api/Ui suffix pairs | Yes |
| mod07  | Use-Case DSL          | Replaces raw driver calls with `app.erp().returnsProduct()...execute()` fluent builders | Yes |
| mod08  | Scenario DSL          | Wraps use-case builders in scenario.given/when/then chain         | Yes      |
| mod09  | Scenario DSL + Clock  | Adds ClockSmokeTest; introduces clock external system             | Yes      |
| mod10  | Scenario DSL + Isolated | Adds acceptance tests + PlaceOrderPositiveIsolatedTest / PlaceOrderNegativeIsolatedTest | Yes |
| mod11  | Scenario DSL + Contract | Adds BaseExternalSystemContractTest + clock + erp contract test hierarchy | Yes |

### Module Progression — .NET

| Module | Layer                 | Delta vs Prior Module                                              | Logical? |
|--------|-----------------------|--------------------------------------------------------------------|----------|
| Mod02  | Raw                   | Baseline                                                           | —        |
| Mod03  | Raw                   | Adds e2e PlaceOrder API/UI positive+negative                      | Yes      |
| Mod04  | Client                | Introduces ShopApiClient, ShopUiClient, ErpRealClient, TaxRealClient | Yes    |
| Mod05  | Driver                | Driver abstraction with `IShopDriver`. Introduces parameterized quantities `[InlineData("3.5"), InlineData("lala")]` in PlaceOrderNegativeBaseTest (Java only had `"3.5"`) | Yes but adds data vs Java |
| Mod06  | Channel Driver        | `BaseChannelDriverTest` with `SetChannelAsync`                     | Yes      |
| Mod07  | Use-Case DSL          | `_app.Erp().ReturnsProduct().Sku().UnitPrice().Execute()` fluent   | Yes      |
| Mod08  | Scenario DSL          | `Scenario(channel).Given()...`                                     | Yes      |
| Mod09  | Scenario DSL + Clock  | Adds ClockSmokeTest                                                | Yes      |
| Mod10  | Scenario DSL + Isolated | Acceptance tests + Isolated variants                             | Yes but missing shouldRejectOrderWithNonPositiveQuantity (Java/TS have it) |
| Mod11  | Scenario DSL + Contract | Adds BaseExternalSystemContractTest hierarchy                     | Yes      |

### Module Progression — TypeScript

| Module | Layer                 | Delta vs Prior Module                                              | Logical? |
|--------|-----------------------|--------------------------------------------------------------------|----------|
| mod02  | Raw                   | Baseline; no BaseRawTest                                           | —        |
| mod03  | Raw                   | Adds e2e; introduces WireMock admin stubs inline, uses different test names (`shouldPlaceOrder` vs `shouldPlaceOrderForValidInput`); assertions are minimal | Partial (body thinner than Java/.NET) |
| mod04  | Client (API only)     | Adds `ShopApiClient`, `ErpStubClient`, `TaxStubClient`. UI path still uses raw Playwright page locators — does not introduce a UI client | No — UI regresses/stays raw |
| mod05  | Driver                | Introduces `ShopApiDriver`, `ShopUiDriver`, `ErpStubDriver`, `TaxStubDriver`; skips to `ShopUiDriver` without having motivated it via a mod04 UI client | No — client layer never taught for UI |
| mod06  | Channel Driver        | Channel-aware `shopDriver` fixture                                 | Yes      |
| mod07  | Use-Case DSL          | Introduces `useCase.shop().placeOrder({...})` config-object style; does NOT introduce a fluent step-builder | Partial — does not teach builder semantics |
| mod08  | Scenario DSL          | Introduces `scenario.given()...` chain. Adds extensive extra negative tests (`shouldRejectOrderWithEmptySku`, `shouldRejectOrderWithNonPositiveQuantity`, etc.) not present in Java/.NET mod08 | Yes for layer; extra content |
| mod09  | Scenario DSL + Clock  | Adds ClockSmokeTest                                                | Yes      |
| mod10  | Scenario DSL + Isolated | Acceptance + Isolated tests                                      | Yes      |
| mod11  | Scenario DSL + Contract | Contract tests; no shared BaseClockContractTest                  | Partial — missing base-class refactor |

Progression Mismatches:
- TypeScript mod04 → mod05 for UI: `ShopUiClient` step is skipped; mod05 jumps from raw-Playwright UI straight to `ShopUiDriver`. The "introduce UI client, then wrap it in a driver" pedagogical step does not exist in TypeScript.
- TypeScript mod04 external systems: uses Stub clients when all other languages use Real clients at the client layer. The Real→Stub distinction is blurred.
- TypeScript mod07 introduces a use-case "DSL" that is effectively identical to mod06 driver usage (just grouped via `useCase.shop()` instead of `shopDriver`). The fluent builder motif taught in Java/.NET mod07 is absent; mod07→mod08 therefore has less incremental value in TS.
- TypeScript mod08 adds negative test coverage that belongs to mod10 (acceptance). The mod08 negative test file in TS already enumerates what Java/.NET only introduce at mod10, blurring the mod08→mod10 progression story.

### mod02

#### Architectural Layer
- Java: Raw — `BaseRawTest` with `HttpClient`, `Playwright`, per-test setup.
- .NET: Raw — `BaseRawTest` equivalent.
- TypeScript: Raw — no base class; each spec file imports `test`, `expect` from `@playwright/test` and calls `fetch` or `chromium.launch()` directly.

Structural mismatch: TypeScript lacks a `BaseRawTest` equivalent.

#### Class Coverage
| Class Name         | Java | .NET | TypeScript |
|--------------------|------|------|------------|
| BaseRawTest        |  Y   |  Y   |     N      |
| ShopApiSmokeTest   |  Y   |  Y   |     Y      |
| ShopUiSmokeTest    |  Y   |  Y   |     Y      |
| ErpSmokeTest       |  Y   |  Y   |     Y      |
| TaxSmokeTest       |  Y   |  Y   |     Y      |

#### Method Differences

All four smoke tests share the single method name `shouldBeAbleToGoToShop` / `shouldBeAbleToGoToErp` / `shouldBeAbleToGoToTax` across languages.

#### Body Differences

ShopApiSmokeTest.shouldBeAbleToGoToShop:
- Java: builds HttpRequest, calls `/health`, asserts `statusCode == 200`.
- .NET: builds HttpRequestMessage, calls `/health`, asserts `StatusCode == 200`.
- TypeScript: `fetch(${backendApiUrl}/health)`, asserts `response.status === 200`.
Aligned.

ShopUiSmokeTest.shouldBeAbleToGoToShop:
- Java: navigate, assert 200 + content-type text/html + `<html>` markers.
- .NET: GotoAsync, same assertions.
- TypeScript: page.goto, same assertions; also `await browser.close()` inline per test.
Aligned content.

ErpSmokeTest / TaxSmokeTest `.shouldBeAbleToGoToErp` / `shouldBeAbleToGoToTax`:
- All three: GET `/health`, assert `200`. Aligned.

### mod03

#### Architectural Layer
- Java: Raw. Tests extend `BaseE2eTest` (itself extends `BaseRawTest`). Direct `HttpClient` + `Playwright` usage.
- .NET: Raw. Same structure.
- TypeScript: Raw. Uses `apiTest` / `uiTest` fixtures that wrap `@playwright/test`. But the tests also set up WireMock stubs via `__admin/mappings` inline — which isn't raw against the "real" external system.

#### Class Coverage
| Class Name                    | Java | .NET | TypeScript |
|-------------------------------|------|------|------------|
| BaseRawTest                   |  Y   |  Y   |     N      |
| BaseE2eTest                   |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveApiTest     |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest      |  Y   |  Y   |     Y      |
| PlaceOrderNegativeApiTest     |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest      |  Y   |  Y   |     Y      |

#### Method Differences — PlaceOrderPositive{Api,Ui}Test

| Method (Java)                 | .NET                           | TypeScript          | Match? |
|-------------------------------|--------------------------------|---------------------|--------|
| shouldPlaceOrderForValidInput | ShouldPlaceOrderForValidInput  | shouldPlaceOrder    | Name mismatch (TS) |

#### Body Differences — PlaceOrderPositiveApiTest

- Java: creates ERP product via POST `/api/products`, places order with SKU/quantity/country, views order, asserts orderNumber/sku/quantity=5/unitPrice=20.00/basePrice=100.00/totalPrice>0/status=PLACED.
- .NET: identical flow and assertions.
- TypeScript: uses WireMock `__admin/mappings` to stub `/erp/api/products/${sku}` and `/tax/api/countries/US` (taxRate 0.07), then places order, asserts `response.ok === true` and `orderNumber` is defined. No view-order step. No sku/quantity/unitPrice/basePrice/totalPrice/status assertions.

#### Body Differences — PlaceOrderPositiveUiTest

- Java: creates ERP product (POST), navigates to /new-order, fills SKU/Quantity/Country, clicks Place Order. Extracts orderNumber from success alert. Navigates to order history, filters by orderNumber, clicks View Details. Asserts orderNumber/sku/quantity/unitPrice/basePrice/totalPrice/status on details page.
- .NET: identical.
- TypeScript: uses WireMock stub for product and tax; uses fixed `DEFAULT-SKU`; navigates, fills, clicks Place Order. Asserts only that notification contains `"Order has been created with Order Number"`. No history navigation, no details assertions.

#### Body Differences — PlaceOrderNegativeApiTest

- Java: POST with `quantity: "invalid-quantity"`, asserts 422 + detail + `errors[].field=="quantity"` + `message=="Quantity must be an integer"`.
- .NET: same.
- TypeScript: same field/message expectations via `fetch`.
All aligned except TS uses `shouldRejectOrderWithNonIntegerQuantity` as the only method.

#### Body Differences — PlaceOrderNegativeUiTest

- Java/.NET: navigate /new-order, fill `invalid-quantity`, click. Assert error alert contains validation message + "quantity" + "Quantity must be an integer".
- TypeScript: similar, but uses different selectors (`notification.error`, `.error-message`, `.field-error`). Asserts `quantityError` via `.field-error` text lookup. Semantics match, selectors/assertion style differ.

### mod04

#### Architectural Layer
- Java: Client. `BaseClientTest` wires `ShopApiClient`, `ShopUiClient`, `ErpRealClient`, `TaxRealClient`.
- .NET: Client. `BaseClientTest` equivalent.
- TypeScript: Client for API (`ShopApiClient`) + External systems use **Stub** clients (`ErpStubClient`, `TaxStubClient`) instead of Real. UI tests DO NOT use a ShopUiClient — they drive `shopPage.locator(...)` directly, i.e. stay at raw Playwright.

Architectural Mismatch (from Summary): TS mod04 UI skips the client layer; TS mod04 external uses Stub instead of Real.

#### Class Coverage
| Class Name                    | Java | .NET | TypeScript |
|-------------------------------|------|------|------------|
| BaseClientTest                |  Y   |  Y   |     N (fixture) |
| BaseE2eTest                   |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveApiTest     |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest      |  Y   |  Y   |     Y      |
| PlaceOrderNegativeApiTest     |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest      |  Y   |  Y   |     Y      |
| ShopApiSmokeTest              |  Y   |  Y   |     Y      |
| ShopUiSmokeTest               |  Y   |  Y   |     Y      |
| ErpSmokeTest                  |  Y   |  Y   |     Y      |
| TaxSmokeTest                  |  Y   |  Y   |     Y      |

#### Method Differences — PlaceOrderPositive{Api,Ui}Test
Java/.NET: `shouldPlaceOrderForValidInput`. TypeScript: `shouldPlaceOrder`.

#### Body Differences — PlaceOrderPositiveApiTest
- Java: `erpClient.createProduct(ExtCreateProductRequest.builder().id(sku).price("20.00")...)`, `shopApiClient.orders().placeOrder(...)`, `shopApiClient.orders().viewOrder(...)`, assert orderNumber/sku/quantity/unitPrice 20.00/totalPrice>0/status PLACED.
- .NET: equivalent. Calls `_erpClient.CreateProductAsync`, `_shopApiClient.Orders().PlaceOrderAsync`, `.ViewOrderAsync`. Same assertions.
- TypeScript: `erpClient.configureProduct({ sku, price: '20.00' })`, `taxClient.configureTaxRate({ country: 'US', taxRate: '0.07' })`, `shopApiClient.placeOrder({ sku, quantity: '5', country: 'US' })`. Asserts only `result.success===true` and `result.value.orderNumber` defined. NO view-order step. NO sku/quantity/unitPrice/basePrice/totalPrice/status assertions. Extra `taxClient.configureTaxRate` step.

#### Body Differences — PlaceOrderPositiveUiTest
- Java/.NET: uses `shopUiClient.openHomePage().clickNewOrder()`, `newOrderPage.inputSku/inputQuantity/inputCountry/clickPlaceOrder`, `getResult`, then `openHomePage().clickOrderHistory().inputOrderNumber().clickSearch()`, `clickViewOrderDetails(orderNumber)`, assert orderNumber/sku/quantity/unitPrice/totalPrice/status on details page.
- TypeScript: no UI client; directly uses `shopPage.locator("a[href='/new-order']").click({ timeout })`, fills SKU=`DEFAULT-SKU` (hardcoded), quantity/country, clicks Place Order. Asserts only notification text contains "Order has been created". No order-history or details-page navigation. Extra `erpClient.configureProduct({sku:'DEFAULT-SKU'...})` + `taxClient.configureTaxRate`.

#### Body Differences — PlaceOrderNegativeApiTest
- Java/.NET: `new PlaceOrderRequest().quantity("invalid-quantity")`, `shopApiClient.orders().placeOrder(...)`, assert Failure with message "The request contains one or more validation errors" and field error `quantity / Quantity must be an integer`.
- TypeScript: `shopApiClient.placeOrder({ quantity: '3.5' })` — uses `3.5` instead of `invalid-quantity`. Same assertions otherwise.

#### Body Differences — PlaceOrderNegativeUiTest
- Java/.NET: `shopUiClient.openHomePage().clickNewOrder()`, `newOrderPage.inputSku/inputQuantity("invalid-quantity")/clickPlaceOrder/getResult`, assert Failure with same message & field error.
- TypeScript: no UI client; uses `shopPage.locator(...)` directly with `quantity='3.5'`. Same assertions via `.error-message` / `.field-error` locators.

#### Body Differences — Smoke Tests
Java/.NET smoke tests use `ShopApiClient`/`ShopUiClient`/`ErpRealClient`/`TaxRealClient`. TypeScript: shop-api-smoke uses the ShopApiClient, shop-ui-smoke uses raw `chromium.launch` (no UI client), erp/tax smoke use the Stub clients.

### mod05

#### Architectural Layer
- Java: Driver. `BaseDriverTest` wires `ShopDriver` (ShopApiDriver or ShopUiDriver), `ErpRealDriver`, `TaxRealDriver`.
- .NET: Driver. `BaseDriverTest` equivalent with `IShopDriver`.
- TypeScript: Driver. Fixture wires `ShopApiDriver`/`ShopUiDriver` and `ErpStubDriver`/`TaxStubDriver`.

Architectural partial mismatch: TS uses Stub external drivers vs Java/.NET Real.

#### Class Coverage
| Class Name                     | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| BaseDriverTest                 |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveBaseTest     |  Y   |  Y   |     N      |
| PlaceOrderPositiveApiTest      |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest       |  Y   |  Y   |     Y      |
| PlaceOrderNegativeBaseTest     |  Y   |  Y   |     N      |
| PlaceOrderNegativeApiTest      |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest       |  Y   |  Y   |     Y      |
| ShopApiSmokeTest               |  Y   |  Y   |     Y      |
| ShopUiSmokeTest                |  Y   |  Y   |     Y      |
| ShopBaseSmokeTest              |  Y   |  Y   |     N      |
| ErpSmokeTest                   |  Y   |  Y   |     Y      |
| TaxSmokeTest                   |  Y   |  Y   |     Y      |

Structural difference: TS has no Base abstract test class — each Api/Ui spec inlines the body.

#### Method Differences — PlaceOrderPositiveBaseTest (Java/.NET)

Single method `shouldPlaceOrderForValidInput` / `ShouldPlaceOrderForValidInput`.

#### Body Differences — PlaceOrderPositive tests

- Java: uses real ERP via `erpDriver.returnsProduct(ReturnsProductRequest().sku().price())`, then `shopDriver.placeOrder(PlaceOrderRequest().sku.quantity("5").country)`, `shopDriver.viewOrder(orderNumber)`, asserts OrderNumber/Sku/Quantity=5/UnitPrice=20.00/TotalPrice>0/Status=PLACED.
- .NET: identical.
- TypeScript API spec: uses `erpDriver.returnsProduct({ sku, price: '20.00' })`, then adds `taxDriver.returnsTaxRate({ country: 'US', taxRate: '0.07' })` (not in Java/.NET), then `shopDriver.placeOrder(...)`, then `shopDriver.viewOrder(...)`, asserts only `quantity=5`, `unitPrice=20`, `status='PLACED'`, `totalPrice>0`. Missing `OrderNumber` equality check, no SKU assertion.
- TypeScript UI spec: uses fixed `DEFAULT-SKU` (not unique); same extra `taxDriver.returnsTaxRate` step; same minimal assertions; no `orderNumber===orderNumber` identity check.

#### Method Differences — PlaceOrderNegativeBaseTest

| Method                              | Java                        | .NET                                         | TypeScript | Match? |
|-------------------------------------|-----------------------------|----------------------------------------------|------------|--------|
| shouldRejectOrderWithNonIntegerQuantity | single-case ("3.5")     | parameterized `[InlineData("3.5"), InlineData("lala")]` | single-case ("3.5") | .NET diverges (adds "lala") |

Body is otherwise aligned across all three.

#### Body Differences — ShopBaseSmokeTest
Java/.NET have `ShopBaseSmokeTest` as abstract with method `shouldBeAbleToGoToShop` calling `shopDriver.goToShop()`. TS has no Base spec; each of `shop-api-smoke-test.spec.ts` / `shop-ui-smoke-test.spec.ts` would need the body — but these files don't exist explicitly for mod05 TS smoke system? Re-check: TS mod05 has `shop-api-smoke-test.spec.ts` and `shop-ui-smoke-test.spec.ts`. The ShopBaseSmokeTest class itself is absent (TS duplicates body per channel file).

### mod06

#### Architectural Layer
- Java: Channel Driver. `BaseChannelDriverTest` uses `@ExtendWith(ChannelExtension.class)` and `ChannelContext.get()` to wire `ShopApiDriver` or `ShopUiDriver` per channel.
- .NET: Channel Driver. `BaseChannelDriverTest` with `SetChannelAsync(channel)` mutating the driver.
- TypeScript: Channel Driver. Fixture uses `ChannelContext.get()` then selects `ShopApiDriver` / `ShopUiDriver`. External drivers are still Stubs.

#### Class Coverage
| Class Name                     | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| BaseChannelDriverTest          |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveTest         |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest         |  Y   |  Y   |     Y      |
| ShopSmokeTest                  |  Y   |  Y   |     Y      |
| ErpSmokeTest                   |  Y   |  Y   |     Y      |
| TaxSmokeTest                   |  Y   |  Y   |     Y      |

#### Body Differences — PlaceOrderPositiveTest
- Java/.NET: after `returnsProduct`, place order, view order and assert orderNumber equality, sku equality, quantity=5, unitPrice=20.00, totalPrice>0, status=PLACED.
- TypeScript: adds `taxDriver.returnsTaxRate({country:'US', taxRate:'0.07'})` step; places order; view order; asserts ONLY `status === 'PLACED'`. No sku/quantity/unitPrice/totalPrice/orderNumber-equality assertions.

#### Body Differences — PlaceOrderNegativeTest
Java/.NET/TypeScript: same single method `shouldRejectOrderWithNonIntegerQuantity` with `"3.5"`, same assertion. Aligned.

#### Body Differences — Smoke Tests
Aligned bodies.

### mod07

#### Architectural Layer
- Java: Use-Case DSL. `BaseUseCaseDslTest` wires `UseCaseDsl app`. Tests call fluent `app.erp().returnsProduct().sku(SKU).unitPrice(20.00).execute()`.
- .NET: Use-Case DSL. `_app.Erp().ReturnsProduct().Sku().UnitPrice().Execute()`.
- TypeScript: NOT a fluent step builder. Tests call `useCase.shop().placeOrder({sku, quantity, country})` — plain config object. No `.sku()` / `.quantity()` step chain.

Architectural mismatch: TS's `useCase` layer is functionally equivalent to mod06 driver, just under a `useCase.shop()` namespace.

#### Class Coverage
| Class Name                     | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| BaseUseCaseDslTest             |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveTest         |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest         |  Y   |  Y   |     Y      |
| ShopSmokeTest                  |  Y   |  Y   |     Y      |
| ErpSmokeTest                   |  Y   |  Y   |     Y      |
| TaxSmokeTest                   |  Y   |  Y   |     Y      |

#### Body Differences — PlaceOrderPositiveTest
- Java: `app.erp().returnsProduct().sku(SKU).unitPrice(20.00).execute().shouldSucceed()`, `app.shop().placeOrder().orderNumber(ORDER_NUMBER).sku(SKU).quantity(5).country(COUNTRY).execute().shouldSucceed().orderNumber(ORDER_NUMBER).orderNumberStartsWith("ORD-")`, `app.shop().viewOrder().orderNumber(ORDER_NUMBER).execute().shouldSucceed().orderNumber(ORDER_NUMBER).sku(SKU).quantity(5).unitPrice(20.00).status(OrderStatus.PLACED).totalPriceGreaterThanZero()`.
- .NET: fluent-equivalent chain.
- TypeScript: `useCase.erp().returnsProduct({sku, price:'20.00'})`, `useCase.tax().returnsTaxRate({country:'US', taxRate:'0.07'})` (extra), `useCase.shop().placeOrder({sku, quantity, country})`, asserts `result.value.orderNumber` matches `/^ORD-/`, `useCase.shop().viewOrder(orderNumber)`, asserts only `status==='PLACED'`. No OrderNumber equality verification, no SKU, no quantity, no unitPrice assertion, no totalPriceGreaterThanZero.

#### Body Differences — PlaceOrderNegativeTest
- Java: `app.shop().placeOrder().sku(SKU).quantity("3.5").country(COUNTRY).execute().shouldFail().errorMessage(...).fieldErrorMessage("quantity", "Quantity must be an integer")`.
- .NET: equivalent.
- TypeScript: `useCase.shop().placeOrder({sku:'SOME-SKU', quantity:'3.5', country:'US'})`, then asserts error message contains validation message and `fieldErrors.find(...)` has message "Quantity must be an integer".
Equivalent semantics.

### mod08

#### Architectural Layer
Scenario DSL. Java `ScenarioDslImpl`, .NET `ScenarioDsl`, TypeScript `scenario-dsl.ts`. All use `scenario.given()...when()...then()...and()` chain. Aligned.

#### Class Coverage
| Class Name                     | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| BaseScenarioDslTest            |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveTest         |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest         |  Y   |  Y   |     Y      |
| ShopSmokeTest                  |  Y   |  Y   |     Y      |
| ErpSmokeTest                   |  Y   |  Y   |     Y      |
| TaxSmokeTest                   |  Y   |  Y   |     Y      |

#### Body Differences — PlaceOrderPositiveTest
All three identical: `.given().product().withUnitPrice(20.00).when().placeOrder().withQuantity(5).then().shouldSucceed().and().order().hasOrderNumberPrefix("ORD-").hasQuantity(5).hasUnitPrice(20.00).hasStatus(OrderStatus.PLACED).hasTotalPriceGreaterThanZero()`. Aligned.

#### Method Differences — PlaceOrderNegativeTest

| Method                                  | Java | .NET | TypeScript | Match? |
|-----------------------------------------|------|------|------------|--------|
| shouldRejectOrderWithNonIntegerQuantity | Y (single case) | Y (single case) | Y (parameterized `['3.5','lala','invalid-quantity']`) | TS extra cases |
| shouldRejectOrderForNonExistentProduct  | N    | N    |     Y      | TS extra |
| shouldRejectOrderWithEmptySku (`['', '   ']`) | N | N |     Y      | TS extra |
| shouldRejectOrderWithNonPositiveQuantity (`['-10','-1','0']`) | N | N | Y | TS extra |
| shouldRejectOrderWithEmptyQuantity (`['', '   ']`) | N | N |     Y | TS extra |
| shouldRejectOrderWithNullQuantity (API only) | N | N |     Y      | TS extra |

TypeScript mod08 negative test has 6 distinct test cases (many parameterized); Java/.NET have exactly 1.

### mod09

#### Architectural Layer
Scenario DSL + Clock. All three languages add clock smoke test. Aligned.

#### Class Coverage
| Class Name                     | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| BaseScenarioDslTest            |  Y   |  Y   |     N (fixture) |
| ClockSmokeTest                 |  Y   |  Y   |     Y      |
| ShopSmokeTest                  |  Y   |  Y   |     Y      |
| ErpSmokeTest                   |  Y   |  Y   |     Y      |
| TaxSmokeTest                   |  Y   |  Y   |     Y      |

#### Body Differences
All smoke tests identical: `scenario.assume().clock|shop|erp|tax().shouldBeRunning()`. Aligned.

### mod10

#### Architectural Layer
Scenario DSL + Isolated tests. All three languages add acceptance + isolated variants. Aligned.

#### Class Coverage
| Class Name                          | Java | .NET | TypeScript |
|-------------------------------------|------|------|------------|
| BaseScenarioDslTest                 |  Y   |  Y   |     N (fixture) |
| BaseAcceptanceTest                  |  Y   |  Y   |     N (fixture) |
| PlaceOrderPositiveTest              |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest              |  Y   |  Y   |     Y      |
| PlaceOrderPositiveIsolatedTest      |  Y   |  Y   |     Y      |
| PlaceOrderNegativeIsolatedTest      |  Y   |  Y   |     Y      |

#### Method Differences — PlaceOrderNegativeTest

| Method                                      | Java | .NET | TypeScript | Match? |
|---------------------------------------------|------|------|------------|--------|
| shouldRejectOrderWithNonIntegerQuantity     |  Y   |  Y   |     Y      | Full |
| shouldRejectOrderForNonExistentProduct      |  Y   |  Y   |     Y      | Full |
| shouldRejectOrderWithEmptySku               |  Y   |  Y   |     Y      | Full |
| shouldRejectOrderWithNonPositiveQuantity    |  Y   |  N   |     Y      | .NET missing |
| shouldRejectOrderWithEmptyQuantity          |  Y   |  Y   |     Y      | Full |
| shouldRejectOrderWithNullQuantity           |  Y   |  Y   |     Y      | Full |

.NET is missing `ShouldRejectOrderWithNonPositiveQuantity` in `Mod10/AcceptanceTests/PlaceOrderNegativeTest.cs`.

#### Method Differences — PlaceOrderPositiveIsolatedTest

| Method                               | Java | .NET | TypeScript | Match? |
|--------------------------------------|------|------|------------|--------|
| shouldApplyFullPriceOnWeekday        |  Y   |  Y   |     Y      | Body differs |
| shouldApplyDiscountWhenPromotionIsActive |  Y |  Y  |     Y      | Full |
| shouldRecordPlacementTimestamp       |  Y   |  Y   |     Y      | Full |

Body difference — `shouldApplyFullPriceOnWeekday`:
- Java: `.given().product().withUnitPrice(20.00).and().promotion().withActive(false).and().country().withTaxRate("0.00").and().clock().withWeekday()...`.
- .NET: `.Given().Product().WithUnitPrice(20.00m).And().Promotion().WithActive(false).And().Country().WithTaxRate("0.00").And().Clock().WithWeekday()...`.
- TypeScript: `.given().product().withUnitPrice(20.0).and().promotion().withActive(false).and().country().withTaxRate('0.00').when().placeOrder().withQuantity(5)...` — **missing `.and().clock().withWeekday()` step**.

#### Method Differences — PlaceOrderNegativeIsolatedTest
Identical single method `shouldRejectOrderPlacedAtYearEnd`. Body aligned.

### mod11

#### Architectural Layer
Scenario DSL + Contract. Java and .NET have a `BaseExternalSystemContractTest` → per-system bases → Real/Stub subclasses. TypeScript lacks all Base*ContractTest files; each `*-real-contract-test.spec.ts` / `*-stub-contract-test.spec.ts` has the body inline and sets `process.env.EXTERNAL_SYSTEM_MODE` at module scope.

#### Class Coverage
| Class Name                            | Java | .NET | TypeScript |
|---------------------------------------|------|------|------------|
| BaseScenarioDslTest                   |  Y   |  Y   |     N      |
| BaseE2eTest                           |  Y   |  Y   |     N      |
| PlaceOrderPositiveTest (e2e)          |  Y   |  Y   |     Y      |
| BaseExternalSystemContractTest        |  Y   |  Y   |     N      |
| BaseClockContractTest                 |  Y   |  Y   |     N      |
| ClockRealContractTest                 |  Y   |  Y   |     Y      |
| ClockStubContractTest                 |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest         |  Y   |  Y   |     Y      |
| BaseErpContractTest                   |  Y   |  Y   |     N      |
| ErpRealContractTest                   |  Y   |  Y   |     Y      |
| ErpStubContractTest                   |  Y   |  Y   |     Y      |
| `contract/tax` directory              |  Y (empty) | N (absent) | N (absent) | Java has empty dir |

#### Body Differences — PlaceOrderPositiveTest (e2e)
All three have `shouldPlaceOrder` with `.when().placeOrder().then().shouldSucceed()`. Aligned.

#### Body Differences — ClockRealContractTest.shouldBeAbleToGetTime
- Java: `scenario.given().then().clock().hasTime()`.
- .NET: `Scenario().Given().Then().Clock().HasTime()`.
- TypeScript: `scenario.given().then().clock().hasTime()`.
Aligned.

#### Body Differences — ClockStubContractTest.shouldBeAbleToGetTime
- Java: `scenario.given().then().clock().hasTime()` (no withTime).
- .NET: `Scenario().Given().Then().Clock().HasTime()` (no withTime).
- TypeScript: `scenario.given().clock().withTime().then().clock().hasTime()` — adds a `.withTime()` with no argument, not present in Java/.NET.

#### Body Differences — ClockStubContractIsolatedTest.shouldBeAbleToGetConfiguredTime
All three identical: `.given().clock().withTime("2024-01-02T09:00:00Z").then().clock().hasTime("2024-01-02T09:00:00Z")`. Aligned.

#### Body Differences — BaseErpContractTest.shouldBeAbleToGetProduct
All three identical: `.given().product().withSku("SKU-123").withUnitPrice(12.0).then().product("SKU-123").hasSku("SKU-123").hasPrice(12.0)`. Aligned.

---

## Architecture Comparison

Source directories:
- Java: `system-test/java/src/main/java/com/optivem/shop/testkit/`
- .NET: `system-test/dotnet/{Channel,Common,Driver.Adapter,Driver.Port,Dsl.Core,Dsl.Port}/`
- TypeScript: `system-test/typescript/src/testkit/`

### Clients Layer

#### Shop API Client

| Class / Construct                                  | Java | .NET | TypeScript |
|----------------------------------------------------|------|------|------------|
| ShopApiClient                                      |  Y   |  Y   |     Y      |
| OrderController (sub-controller)                   |  Y   |  Y   |     N      |
| CouponController (sub-controller)                  |  Y   |  Y   |     N      |
| HealthController (sub-controller)                  |  Y   |  Y   |     N      |
| SystemErrorMapper                                  |  Y   |  N (different) |  N |
| Shop DTO ProblemDetailResponse                     |  Y (adapter) | Y (adapter) | Y (port) |

TS `ShopApiClient` is a single monolithic client; Java/.NET decompose into per-domain controllers.

#### Shop UI Client / Pages

| Class                                | Java | .NET | TypeScript |
|--------------------------------------|------|------|------------|
| ShopUiClient                         |  Y   |  Y   |     N      |
| BasePage                             |  Y   |  Y   |     N      |
| HomePage                             |  Y   |  Y   |     N      |
| NewOrderPage                         |  Y   |  Y   |     N      |
| OrderHistoryPage                     |  Y   |  Y   |     N      |
| OrderDetailsPage                     |  Y   |  Y   |     N      |
| CouponManagementPage                 |  Y   |  Y   |     N      |

TS has no UI client or page-object model. UI tests either drive Playwright locators directly (mod03/mod04) or go through `ShopUiDriver` directly (mod05+).

#### External Clients

| Class                                | Java | .NET | TypeScript |
|--------------------------------------|------|------|------------|
| BaseErpClient                        |  Y   |  Y   |     N      |
| ErpRealClient                        |  Y   |  Y   |     Y      |
| ErpStubClient                        |  Y   |  Y   |     Y      |
| BaseTaxClient                        |  Y   |  Y   |     N      |
| TaxRealClient                        |  Y   |  Y   |     Y      |
| TaxStubClient                        |  Y   |  Y   |     Y      |
| ClockRealClient                      |  Y   |  Y   |     Y      |
| ClockStubClient                      |  Y   |  Y   |     Y      |

TS lacks `BaseErpClient` / `BaseTaxClient` intermediate classes.

#### Shared Client Infrastructure

| Class / File                         | Java | .NET | TypeScript |
|--------------------------------------|------|------|------------|
| JsonHttpClient / http-client         |  Y   |  Y   |     Y      |
| HttpStatus                           |  Y   |  Y   |     N      |
| PageClient                           |  Y   |  Y   |     N (withApp instead) |
| JsonWireMockClient / wiremock-client |  Y   |  Y   |     Y      |
| withApp helper                       |  N   |  N   |     Y      |

TS has no `HttpStatus` constants and no `PageClient`; instead has `withApp.ts` fixture helper (TS-unique).

#### Client DTOs

| DTO                                     | Java | .NET | TypeScript |
|-----------------------------------------|------|------|------------|
| ExtCreateProductRequest                 |  Y   |  Y   |     N (inlined into client) |
| ExtProductDetailsResponse               |  Y   |  Y   |     N |
| ExtGetPromotionResponse                 |  Y   |  Y   |     N |
| ExtGetTimeResponse                      |  Y   |  Y   |     N |
| ExtGetCountryResponse / ExtCountryDetailsResponse | Y | Y | N |
| ExtErpErrorResponse                     |  Y   |  Y   |     N |
| ExtClockErrorResponse                   |  Y   |  Y   |     N |
| ExtTaxErrorResponse                     |  Y   |  Y   |     N |
| ProblemDetailResponse                   |  Y (adapter) | Y (adapter) | Y (port) |

TS does not expose explicit external-client DTOs; TypeScript client code uses anonymous inline types.

### Drivers Layer

#### Shop Drivers

| Class                  | Java | .NET | TypeScript |
|------------------------|------|------|------------|
| ShopDriver (port)      |  Y   |  Y   |     Y      |
| ShopApiDriver          |  Y   |  Y   |     Y      |
| ShopUiDriver           |  Y   |  Y   |     Y      |

Aligned.

#### External Drivers

| Class                  | Java | .NET | TypeScript |
|------------------------|------|------|------------|
| ErpDriver (port)       |  Y   |  Y   |     Y      |
| BaseErpDriver          |  Y   |  Y   |     N      |
| ErpRealDriver          |  Y   |  Y   |     Y      |
| ErpStubDriver          |  Y   |  Y   |     Y      |
| TaxDriver (port)       |  Y   |  Y   |     Y      |
| BaseTaxDriver          |  Y   |  Y   |     N      |
| TaxRealDriver          |  Y   |  Y   |     Y      |
| TaxStubDriver          |  Y   |  Y   |     Y      |
| ClockDriver (port)     |  Y   |  Y   |     Y      |
| ClockRealDriver        |  Y   |  Y   |     Y      |
| ClockStubDriver        |  Y   |  Y   |     Y      |

TS lacks `BaseErpDriver` and `BaseTaxDriver`.

### Channels Layer

| Class / File                | Java | .NET | TypeScript |
|-----------------------------|------|------|------------|
| ChannelType                 |  Y   |  Y   |     Y      |

Aligned.

### Use Case DSL Layer

#### Shop

| Use Case               | Java | .NET | TypeScript |
|------------------------|------|------|------------|
| ShopDsl                |  Y   |  Y   |     Y      |
| PlaceOrder             |  Y (own file) | Y (own file) | N (inlined) |
| PlaceOrderVerification |  Y   |  Y   |     N      |
| CancelOrder            |  Y   |  Y   |     N (inlined) |
| ViewOrder              |  Y   |  Y   |     N (inlined) |
| ViewOrderVerification  |  Y   |  Y   |     N      |
| BrowseCoupons          |  Y   |  Y   |     N (inlined) |
| BrowseCouponsVerification |  Y |  Y   |     N      |
| PublishCoupon          |  Y   |  Y   |     N (inlined) |
| DeliverOrder           |  Y   |  Y   |     N (no DSL use case; only driver method exists) |
| GoToShop               |  Y   |  Y   |     N (inlined) |
| BaseShopUseCase        |  Y   |  Y   |     N      |
| SystemResults          |  Y   |  Y   |     N      |

TS implements ShopDsl as a single class with methods rather than one file per use case.

#### External Clock

| Use Case              | Java | .NET | TypeScript |
|-----------------------|------|------|------------|
| ClockDsl              |  Y   |  Y   |     Y      |
| GetTime               |  Y   |  Y   |     N (inlined) |
| GetTimeVerification   |  Y   |  Y   |     N      |
| GoToClock             |  Y   |  Y   |     N (inlined) |
| ReturnsTime           |  Y   |  Y   |     N (inlined) |
| BaseClockUseCase / BaseClockCommand | Y | Y | N |

#### External ERP

| Use Case              | Java | .NET | TypeScript |
|-----------------------|------|------|------------|
| ErpDsl                |  Y   |  Y   |     Y      |
| GetProduct            |  Y   |  Y   |     N (inlined) |
| GetProductVerification|  Y   |  Y   |     N      |
| ReturnsProduct        |  Y   |  Y   |     N (inlined) |
| ReturnsPromotion      |  Y   |  Y   |     N (inlined) |
| GoToErp               |  Y   |  Y   |     N (inlined) |
| BaseErpUseCase / BaseErpCommand | Y | Y | N |

#### External Tax

| Use Case              | Java | .NET | TypeScript |
|-----------------------|------|------|------------|
| TaxDsl                |  Y   |  Y   |     Y      |
| GetTaxRate            |  Y   |  Y   |     N (inlined) |
| GetTaxVerification    |  Y   |  Y   |     N      |
| ReturnsTaxRate        |  Y   |  Y   |     N (inlined) |
| GoToTax               |  Y   |  Y   |     N (inlined) |
| BaseTaxUseCase / BaseTaxCommand | Y | Y | N |

TS collapses the per-use-case files into top-level methods on the DSL classes. Java/.NET decompose into individual use case + verification classes.

### Scenario DSL Layer

#### Top-level

| Class                       | Java | .NET | TypeScript |
|-----------------------------|------|------|------------|
| ScenarioDsl (port)          |  Y   |  Y   |     Y      |
| ScenarioDslImpl / ScenarioDsl (core) | Y | Y |     Y      |
| ScenarioDslFactory          |  N   |  Y   |     N      |
| ExecutionResult             |  Y   |  Y   |     N      |
| ExecutionResultBuilder      |  Y   |  Y   |     N      |
| ExecutionResultContext      |  Y   |  Y   |     Y (scenario-context.ts) |
| ScenarioDefaults / GherkinDefaults | Y | Y | Y (defaults.ts) |

TS lacks explicit `ExecutionResult` / `ExecutionResultBuilder` classes.

#### Given Steps

| Step              | Java | .NET | TypeScript |
|-------------------|------|------|------------|
| GivenStage        |  Y   |  Y   |     Y      |
| GivenClock        |  Y   |  Y   |     Y      |
| GivenCountry      |  Y   |  Y   |     Y      |
| GivenCoupon       |  Y   |  Y   |     Y      |
| GivenOrder        |  Y   |  Y   |     Y      |
| GivenProduct      |  Y   |  Y   |     Y      |
| GivenPromotion    |  Y   |  Y   |     Y      |

Aligned.

#### When Steps

| Step                     | Java | .NET | TypeScript |
|--------------------------|------|------|------------|
| WhenStage                |  Y   |  Y   |     Y      |
| WhenBrowseCoupons        |  Y   |  Y   |     Y      |
| WhenCancelOrder          |  Y   |  Y   |     Y      |
| WhenPlaceOrder           |  Y   |  Y   |     Y      |
| WhenPublishCoupon        |  Y   |  Y   |     Y      |
| WhenViewOrder            |  Y   |  Y   |     Y      |
| WhenGoToShop             |  N   |  Y   |     N      |

.NET adds `WhenGoToShop.cs` which is not present in Java or TS.

#### Then Steps

| Step                     | Java      | .NET                                                  | TypeScript |
|--------------------------|-----------|-------------------------------------------------------|------------|
| ThenStage                |  Y        |  Y                                                    |  Y         |
| ThenResultStage          |  Y        |  Y                                                    |  Y (then-result-stage.ts) |
| ThenSuccess              |  Y        |  Y                                                    |  Y         |
| ThenFailure              |  Y        |  Y                                                    |  Y         |
| ThenFailureAnd           |  N        |  Y                                                    |  Y         |
| ThenSuccessAnd           |  N        |  Y                                                    |  N         |
| ThenClock                |  Y        |  Y                                                    |  N (only then-given-clock port) |
| ThenCountry              |  Y        |  Y                                                    |  N (only then-given-country port) |
| ThenCoupon               |  Y        |  Y                                                    |  Y         |
| ThenOrder                |  Y        |  Y                                                    |  Y         |
| ThenProduct              |  Y        |  Y                                                    |  N (only then-given-product port) |
| ThenFailureCoupon        |  N        |  Y                                                    |  N         |
| ThenFailureOrder         |  N        |  Y                                                    |  N         |
| ThenSuccessCoupon        |  N        |  Y                                                    |  N         |
| ThenSuccessOrder         |  N        |  Y                                                    |  N         |
| BaseThenResultCoupon     |  N        |  Y                                                    |  N         |
| BaseThenResultOrder      |  N        |  Y                                                    |  N         |
| ThenStageBase            |  N        |  Y                                                    |  N         |
| then-browse-coupons      |  N        |  N                                                    |  Y         |
| then-cancel-order        |  N        |  N                                                    |  Y         |
| then-contract            |  N        |  N                                                    |  Y         |
| then-place-order         |  N        |  N                                                    |  Y         |
| then-publish-coupon      |  N        |  N                                                    |  Y         |
| then-view-order          |  N        |  N                                                    |  Y         |

Three-way decomposition mismatch. Java groups by entity (ThenOrder), .NET duplicates per-outcome (ThenFailureOrder, ThenSuccessOrder, ThenSuccessAnd, ThenFailureAnd), TS groups per-use-case (then-place-order, then-cancel-order, etc.). Different decompositions.

#### Assume Steps

| Step              | Java | .NET | TypeScript |
|-------------------|------|------|------------|
| AssumeStage       |  Y   |  Y   |     Y      |
| AssumeRunning     |  Y   |  Y   |     Y      |

Aligned.

#### Scenario Shared / Shared Utilities

| Class                       | Java | .NET | TypeScript |
|-----------------------------|------|------|------------|
| BaseUseCase                 |  Y   |  Y   |     N      |
| UseCase (interface)         |  Y   |  N   |     N      |
| IUseCase (interface)        |  N   |  Y   |     N      |
| UseCaseContext              |  Y   |  Y   |     Y (use-case-context.ts) |
| UseCaseResult               |  Y   |  Y   |     N (Result<T,E> in common) |
| ErrorVerification           |  Y   |  N   |     N      |
| ResponseVerification        |  Y   |  Y   |     N      |
| VoidVerification            |  Y   |  Y   |     N      |
| BaseClause                  |  N   |  Y   |     N      |

### Common Layer

| Class / File                | Java | .NET | TypeScript |
|-----------------------------|------|------|------------|
| Closer                      |  Y   |  N   |     N      |
| Converter                   |  Y   |  Y   |     N      |
| Result                      |  Y   |  Y   |     Y (result.ts) |
| ResultAssert / ResultAssertExtensions | Y | Y | N |
| ResultTaskExtensions        |  N   |  Y   |     N      |
| VoidValue                   |  N   |  Y   |     N      |
| dtos.ts (OrderStatus re-export) | N | N |     Y      |

TS `common/` exposes only `result.ts` and a `dtos.ts` barrel. No `Closer`, no `Converter`, no assertion helpers.

### DSL Ports Layer

| File / Interface             | Java | .NET | TypeScript |
|------------------------------|------|------|------------|
| ScenarioDsl / IScenarioDsl   |  Y   |  Y   |     Y      |
| ChannelMode                  |  Y   |  Y   |     Y      |
| ExternalSystemMode           |  Y   |  Y   |     Y      |
| port/assume/*                |  Y   |  Y   |     Y      |
| port/given/*                 |  Y   |  Y   |     Y (partial — see then-given-*) |
| port/when/*                  |  Y   |  Y   |     Y      |
| port/then/ThenClock          |  Y   |  Y   |     N (only then-given-clock) |
| port/then/ThenCountry        |  Y   |  Y   |     N (only then-given-country) |
| port/then/ThenProduct        |  Y   |  Y   |     N (only then-given-product) |
| port/then/ThenCoupon         |  Y   |  Y   |     Y      |
| port/then/ThenOrder          |  Y   |  Y   |     Y      |
| port/then/ThenFailure        |  Y   |  Y   |     Y      |
| port/then/ThenSuccess        |  Y   |  Y   |     Y      |
| port/when/base/WhenStep      |  Y   |  Y   |     N      |
| port/given/steps/base/GivenStep |  Y | N (not present) | N |
| port/then/steps/base/ThenStep |  Y  | N (not present) | N |

### Driver Ports Layer

| File / Interface               | Java | .NET | TypeScript |
|--------------------------------|------|------|------------|
| ShopDriver / IShopDriver       |  Y   |  Y   |     Y      |
| ErpDriver / IErpDriver         |  Y   |  Y   |     Y      |
| TaxDriver / ITaxDriver         |  Y   |  Y   |     Y      |
| ClockDriver / IClockDriver     |  Y   |  Y   |     Y      |
| PlaceOrderRequest              |  Y   |  Y   |     Y      |
| PlaceOrderResponse             |  Y   |  Y   |     Y      |
| ViewOrderResponse              |  Y   |  Y   |     Y      |
| BrowseCouponsResponse          |  Y   |  Y   |     Y      |
| PublishCouponRequest           |  Y   |  Y   |     Y      |
| OrderStatus                    |  Y   |  Y   |     Y      |
| SystemError                    |  Y   |  Y   |     Y      |
| GetProductRequest              |  Y   |  Y   |     N      |
| GetProductResponse             |  Y   |  Y   |     Y      |
| GetPromotionResponse           |  Y   |  N   |     N      |
| ReturnsProductRequest          |  Y   |  Y   |     Y      |
| ReturnsPromotionRequest        |  Y   |  Y   |     Y      |
| ErpErrorResponse               |  Y   |  Y   |     Y      |
| GetCountryRequest              |  Y   |  N   |     N      |
| GetTaxResponse                 |  Y   |  Y   |     Y      |
| ReturnsTaxRateRequest          |  Y   |  Y   |     Y      |
| TaxErrorResponse               |  Y   |  Y   |     Y      |
| GetTimeResponse                |  Y   |  Y   |     Y      |
| ReturnsTimeRequest             |  Y   |  Y   |     Y      |
| ClockErrorResponse             |  Y   |  Y   |     Y      |
| ProblemDetailResponse          |  N (in adapter) | N (in adapter) | Y |
| SystemResults (static helpers) |  Y (in core/usecase/shop/commons) | Y (in port/Shop) | N |

Driver-port DTO mismatches:
- `GetProductRequest`: Java and .NET have it; TS is missing.
- `GetCountryRequest`: Java only; .NET and TS missing.
- `GetPromotionResponse`: Java only; .NET and TS missing.
- `ProblemDetailResponse`: Java and .NET keep it in adapter; TS promotes it to port.
- `SystemResults`: Java places in core use-case commons; .NET places in port; TS missing.

---

## Summary of Required Changes

Total differences found: ~95

By language:
- Java: ~5 changes (leaves in place — reference) + keep-or-accept decisions for items .NET/TS have but Java doesn't
- .NET: ~18 changes (many related to Then* decomposition, missing mod10 test, extra mod05 test case, missing ports)
- TypeScript: ~75 changes (majority — most legacy body and architectural gaps)

By area:
- Architectural mismatches (legacy): 5 (mod02 TS no BaseRawTest, mod04 TS UI not using client, mod04 TS external using Stub, mod07 TS use-case not fluent, mod11 TS no Base contract classes)
- Progression mismatches (legacy): 4 (TS mod04→mod05 UI skip, TS mod04 external Stub choice, TS mod07 identical-to-mod06, TS mod08 pre-empts mod10 content)
- Test — Acceptance: 8 (negative test split/merge in latest; mod10 .NET missing non-positive; mod10 TS missing withWeekday; time-dependent markers missing in TS)
- Test — Contract: 1 (TS clock-stub withTime extra step in latest + mod11)
- Test — E2E: 0 (latest aligned)
- Test — Smoke: 0 (latest aligned)
- Legacy Test — mod02: 1 (TS no BaseRawTest)
- Legacy Test — mod03: 4 (TS method name + thinner assertions + different mocking + UI path)
- Legacy Test — mod04: 5 (TS method name + thinner assertions + no UI client + extra tax step + Stub external)
- Legacy Test — mod05: 3 (.NET extra "lala" case + TS thinner assertions + TS no Base spec)
- Legacy Test — mod06: 1 (TS thinner assertions + extra tax step)
- Legacy Test — mod07: 2 (TS not a fluent builder + thinner assertions + extra tax step)
- Legacy Test — mod08: 5 (TS adds extra negative coverage prematurely)
- Legacy Test — mod10: 2 (.NET missing NonPositive test + TS missing withWeekday step)
- Legacy Test — mod11: 2 (TS clock-stub withTime extra + TS no Base contract classes)
- Architecture — Clients: 18 (TS no UI pages, no sub-controllers, no Base*Client, no external DTOs, no HttpStatus, no PageClient, no SystemErrorMapper)
- Architecture — Drivers: 2 (TS no BaseErpDriver / BaseTaxDriver)
- Architecture — Channels: 0 (aligned)
- Architecture — Use Case DSL: ~20 (TS collapses per-use-case files; TS missing DeliverOrder DSL; TS no Base*UseCase)
- Architecture — Scenario DSL: ~18 (Three-way decomposition of Then* steps; .NET has ThenFailureAnd/SuccessAnd duplicates; TS has per-use-case then files; TS no ExecutionResult/Builder; .NET has extra WhenGoToShop)
- Architecture — Common: 5 (TS no Closer, Converter, ResultAssert; .NET extra ResultTaskExtensions/VoidValue; Java extra Closer)
- Architecture — DSL Ports: 4 (TS missing ThenClock/ThenCountry/ThenProduct ports, WhenStep base; only then-given-* variants)
- Architecture — Driver Ports: 5 (TS missing GetProductRequest; .NET missing GetCountryRequest/GetPromotionResponse; TS relocates ProblemDetailResponse; TS missing SystemResults)
