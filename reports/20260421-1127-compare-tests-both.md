# 20260421-1127 — System Test Comparison Report (both)

Mode: both (latest + legacy + architecture)

Generated: 2026-04-21 11:27 (local)

Reference implementation: **Java** (align .NET and TypeScript to Java unless otherwise flagged).

Language order throughout: **Java, .NET, TypeScript**.

## Latest Comparison

### Acceptance Tests

#### Class Coverage

| Class Name                         | Java | .NET | TypeScript |
|------------------------------------|------|------|------------|
| BrowseCouponsPositiveTest          |  Y   |  Y   |     Y      |
| CancelOrderNegativeIsolatedTest    |  Y   |  Y   |     Y      |
| CancelOrderNegativeTest            |  Y   |  Y   |     Y      |
| CancelOrderPositiveIsolatedTest    |  Y   |  Y   |     Y      |
| CancelOrderPositiveTest            |  Y   |  Y   |     Y      |
| PlaceOrderNegativeIsolatedTest     |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest             |  Y   |  Y   |     Y      |
| PlaceOrderPositiveIsolatedTest     |  Y   |  Y   |     Y      |
| PlaceOrderPositiveTest             |  Y   |  Y   |     Y      |
| PublishCouponNegativeTest          |  Y   |  Y   |     Y      |
| PublishCouponPositiveTest          |  Y   |  Y   |     Y      |
| ViewOrderNegativeTest              |  Y   |  Y   |     Y      |
| ViewOrderPositiveTest              |  Y   |  Y   |     Y      |

No missing acceptance test classes across languages.

#### Method Differences — PlaceOrderPositiveTest

| Method (Java)                                                                      | .NET | TypeScript | Match? |
|------------------------------------------------------------------------------------|------|------------|--------|
| shouldBeAbleToPlaceOrderForValidInput                                              |  Y   |     Y      |  Full  |
| orderStatusShouldBePlacedAfterPlacingOrder                                         |  Y   |     Y      |  Full  |
| shouldCalculateBasePriceAsProductOfUnitPriceAndQuantity                            |  Y   |     Y      |  Full  |
| shouldPlaceOrderWithCorrectBasePriceParameterized                                  |  Y   |     Y      |  Full  |
| orderPrefixShouldBeORD                                                             |  Y   |     Y      |  Full  |
| discountRateShouldBeAppliedForCoupon                                               |  Y   |     Y      |  Full  |
| discountRateShouldBeNotAppliedWhenThereIsNoCoupon                                  |  Y   |     Y      |  Full  |
| subtotalPriceShouldBeCalculatedAsTheBasePriceMinusDiscountAmountWhenWeHaveCoupon   |  Y   |     Y      |  Full  |
| subtotalPriceShouldBeSameAsBasePriceWhenNoCoupon                                   |  Y   |     Y      |  Full  |
| correctTaxRateShouldBeUsedBasedOnCountry                                           |  Y   |     Y      |  Full  |
| totalPriceShouldBeSubtotalPricePlusTaxAmount                                       |  Y   |     Y      |  Full  |
| couponUsageCountHasBeenIncrementedAfterItsBeenUsed                                 |  Y   |     Y      |  Full  |
| orderTotalShouldIncludeTax                                                         |  Y   |     Y      |  Full  |
| orderTotalShouldReflectCouponDiscount                                              |  Y   |     Y      |  Full  |
| orderTotalShouldApplyCouponDiscountAndTax                                          |  Y   |     Y      |  Full  |

No method-name differences in PlaceOrderPositiveTest.

#### Method Differences — PlaceOrderNegativeTest

| Method (Java)                                            | .NET | TypeScript | Match? |
|----------------------------------------------------------|------|------------|--------|
| shouldRejectOrderWithInvalidQuantity                     |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNonExistentSku                      |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNegativeQuantity                    |  Y   |     Y      |  Full  |
| shouldRejectOrderWithZeroQuantity                        |  Y   |     Y      |  Full  |
| shouldRejectOrderWithEmptySku                            |  Y   |     Y      |  Full  |
| shouldRejectOrderWithEmptyQuantity                       |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNonIntegerQuantity                  |  Y   |     Y      |  Full  |
| shouldRejectOrderWithEmptyCountry                        |  Y   |     Y      |  Full  |
| shouldRejectOrderWithInvalidCountry                      |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullQuantity                        |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullSku                             |  Y   |     Y      |  Full  |
| shouldRejectOrderWithNullCountry                         |  Y   |     Y      |  Full  |
| cannotPlaceOrderWithNonExistentCoupon                    |  Y   |     Y      |  Full  |
| cannotPlaceOrderWithCouponThatHasExceededUsageLimit      |  Y   |     Y      |  Full  |

No method-name differences in PlaceOrderNegativeTest.

#### Method Differences — Other Acceptance Tests

For the remaining latest acceptance classes, method names match across all three languages (Java/.NET/TS). Specifically:

- BrowseCouponsPositiveTest: `shouldBeAbleToBrowseCoupons` in all 3.
- CancelOrderNegativeIsolatedTest: `cannotCancelAnOrderOn31stDecBetween2200And2230` (parameterized with 5 datasets) in all 3.
- CancelOrderNegativeTest: `shouldNotCancelNonExistentOrder` (parameterized), `shouldNotCancelAlreadyCancelledOrder`, `cannotCancelNonExistentOrder` in all 3.
- CancelOrderPositiveIsolatedTest: `shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230` in all 3.
- CancelOrderPositiveTest: `shouldHaveCancelledStatusWhenCancelled` in all 3.
- PlaceOrderNegativeIsolatedTest: `cannotPlaceOrderWithExpiredCoupon`, `shouldRejectOrderPlacedAtYearEnd` in all 3.
- PlaceOrderPositiveIsolatedTest: `shouldRecordPlacementTimestamp`, `shouldApplyFullPriceWithoutPromotion`, `shouldApplyDiscountWhenPromotionIsActive` in all 3.
- PublishCouponNegativeTest: `cannotPublishCouponWithZeroOrNegativeDiscount` (Java/.NET) vs `cannotPublishCouponWithZeroOrNegativeDiscount` (TS), `cannotPublishCouponWithDiscountGreaterThan100percent` (Java) / `CannotPublishCouponWithDiscountGreaterThan100Percent` (.NET) / TS name matches Java — **Java uses lowercase `percent`, .NET uses PascalCase `Percent`**; this is a casing-only mismatch per language idiom.
- PublishCouponPositiveTest: `shouldBeAbleToPublishValidCoupon`, `shouldBeAbleToPublishCouponWithEmptyOptionalFields`, `shouldBeAbleToCorrectlySaveCoupon`, `shouldPublishCouponSuccessfully` in all 3.
- ViewOrderNegativeTest: `shouldNotBeAbleToViewNonExistentOrder` in all 3.
- ViewOrderPositiveTest: `shouldBeAbleToViewOrder` in all 3.

#### Body Differences — Acceptance Tests

No significant body differences detected in the sampled tests (PlaceOrderPositiveTest, PlaceOrderNegativeTest, CancelOrderNegativeIsolatedTest). All three languages use the same scenario DSL chain, same SKUs (`ABC`), same unit prices (20.00), same countries, same error messages, same coupon codes, and the same channel annotations.

### Contract Tests

#### Class Coverage

| Class Name                        | Java | .NET | TypeScript |
|-----------------------------------|------|------|------------|
| BaseClockContractTest             |  Y   |  Y   |     Y      |
| ClockRealContractTest             |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest     |  Y   |  Y   |     Y      |
| ClockStubContractTest             |  Y   |  Y   |     Y      |
| BaseErpContractTest               |  Y   |  Y   |     Y      |
| ErpRealContractTest               |  Y   |  Y   |     Y      |
| ErpStubContractTest               |  Y   |  Y   |     Y      |
| BaseTaxContractTest               |  Y   |  Y   |     Y      |
| TaxRealContractTest               |  Y   |  Y   |     Y      |
| TaxStubContractTest               |  Y   |  Y   |     Y      |

No missing contract test classes across languages. TS uses `registerXxxContractTests(test)` function helpers instead of abstract-class inheritance — this is covered under the Exceptions (known divergences) list.

#### Method Differences — Contract Tests

| Method                                                    | Java | .NET | TypeScript | Match? |
|-----------------------------------------------------------|------|------|------------|--------|
| Clock BaseClockContractTest.shouldBeAbleToGetTime         |  Y   |  Y   |     Y      |  Full  |
| Clock ClockStubContractIsolatedTest.shouldBeAbleToGetConfiguredTime | Y | Y |  Y      |  Full  |
| Erp BaseErpContractTest.shouldBeAbleToGetProduct          |  Y   |  Y   |     Y      |  Full  |
| Tax BaseTaxContractTest.shouldBeAbleToGetTaxRate          |  Y   |  Y   |     Y      |  Full  |
| Tax TaxStubContractTest.shouldBeAbleToGetConfiguredTaxRate |  Y  |  Y   |     Y      |  Full  |

#### Body Differences — Contract Tests

All contract test bodies align structurally across languages. Common pattern: `scenario.given().then().{externalSystem}().hasTime()/hasProduct()/etc.`

### E2E Tests

#### Class Coverage

| Class Name                | Java | .NET | TypeScript |
|---------------------------|------|------|------------|
| PlaceOrderPositiveTest    |  Y   |  Y   |     Y      |

#### Method Differences — E2E

| Method                    | Java | .NET | TypeScript | Match? |
|---------------------------|------|------|------------|--------|
| shouldPlaceOrder          |  Y   |  Y   |     Y      |  Full  |

#### Body Differences — E2E

All bodies match — `scenario.when().placeOrder().then().shouldSucceed()`.

### Smoke Tests

#### Class Coverage

| Class Name        | Java | .NET | TypeScript |
|-------------------|------|------|------------|
| ClockSmokeTest    |  Y   |  Y   |     Y      |
| ErpSmokeTest      |  Y   |  Y   |     Y      |
| TaxSmokeTest      |  Y   |  Y   |     Y      |
| ShopSmokeTest     |  Y   |  Y   |     Y      |

#### Method Differences — Smoke

| Method                 | Java | .NET | TypeScript | Match? |
|------------------------|------|------|------------|--------|
| shouldBeAbleToGoToClock|  Y   |  Y   |     Y      |  Full  |
| shouldBeAbleToGoToErp  |  Y   |  Y   |     Y      |  Full  |
| shouldBeAbleToGoToTax  |  Y   |  Y   |     Y      |  Full  |
| shouldBeAbleToGoToShop |  Y   |  Y   |     Y      |  Full  |

#### Body Differences — Smoke

All smoke tests use `scenario.assume().{externalSystem}().shouldBeRunning()` or equivalent across all three.

## Legacy Comparison

### Architectural Abstraction Summary

| Module | Expected Layer            | Java            | .NET            | TypeScript      | Match? |
|--------|---------------------------|-----------------|-----------------|-----------------|--------|
| mod02  | Raw                       | Raw             | Raw             | Raw             | Full   |
| mod03  | Raw                       | Raw             | Raw             | Raw             | Full   |
| mod04  | Client                    | Client          | Client          | Client          | Full   |
| mod05  | Driver                    | Driver          | Driver          | Driver          | Full   |
| mod06  | Channel Driver            | Channel Driver  | Channel Driver  | Channel Driver  | Full   |
| mod07  | Use-Case DSL              | Use-Case DSL    | Use-Case DSL    | Use-Case DSL    | Full   |
| mod08  | Scenario DSL              | Scenario DSL    | Scenario DSL    | Scenario DSL    | Full   |
| mod09  | Scenario DSL + Clock      | Scenario DSL + Clock | Scenario DSL + Clock | Scenario DSL + Clock | Full |
| mod10  | Scenario DSL + Isolated   | Scenario DSL + Isolated | Scenario DSL + Isolated | Scenario DSL + Isolated | Full |
| mod11  | Scenario DSL + Contract   | Scenario DSL + Contract | Scenario DSL + Contract | Scenario DSL + Contract | Full |

**Architectural Mismatches**: None. All three languages follow the same abstraction progression per module.

### Module Progression (per language)

| Module | Java Delta                                | .NET Delta                                | TypeScript Delta                          | Logical? |
|--------|-------------------------------------------|-------------------------------------------|-------------------------------------------|----------|
| mod02  | baseline (raw HTTP + Playwright)          | baseline (raw HTTP + Playwright)          | baseline (raw fetch + Playwright)         | Yes (all) |
| mod03  | adds e2e raw HTTP tests                   | adds e2e raw HTTP tests                   | adds e2e raw fetch tests                  | Yes (all) |
| mod04  | introduces typed API/UI clients + ErpClient | introduces typed API/UI clients + ErpClient | introduces typed API/UI clients + ErpClient | Yes (all) |
| mod05  | introduces driver adapters over clients   | introduces driver adapters over clients   | introduces driver adapters over clients   | Yes (all) |
| mod06  | unifies channel Api/Ui into single channel-driver tests | unifies channel Api/Ui into single channel-driver tests | unifies channel Api/Ui into single channel-driver tests | Yes (all) |
| mod07  | introduces use-case DSL (app.shop().placeOrder().execute()) | introduces use-case DSL | introduces use-case DSL | Yes (all) |
| mod08  | introduces scenario DSL (given/when/then) | introduces scenario DSL | introduces scenario DSL | Yes (all) |
| mod09  | adds clock external system assumptions    | adds clock external system assumptions    | adds clock external system assumptions    | Yes (all) |
| mod10  | adds stub-based isolated acceptance tests | adds stub-based isolated acceptance tests | adds stub-based isolated acceptance tests | Yes (all) |
| mod11  | adds external-system contract tests       | adds external-system contract tests       | adds external-system contract tests       | Yes (all) |

**Progression Mismatches**: None. Each language walks mod02 → mod11 logically.

### mod02

#### Class Coverage

| Class Name          | Java | .NET | TypeScript |
|---------------------|------|------|------------|
| BaseRawTest         |  Y   |  Y   |     Y      |
| ErpSmokeTest        |  Y   |  Y   |     Y      |
| TaxSmokeTest        |  Y   |  Y   |     Y      |
| ShopApiSmokeTest    |  Y   |  Y   |     Y      |
| ShopUiSmokeTest     |  Y   |  Y   |     Y      |

#### Method Differences

All smoke tests use `shouldBeAbleToGoTo{Clock|Erp|Tax|Shop}` across all three languages.

#### Body Differences

All three use raw HTTP calls to `/health` endpoint. Identical structure. Java uses `java.net.http.HttpClient`; .NET uses `HttpClient`/`HttpRequestMessage`; TS uses global `fetch`.

### mod03

#### Class Coverage

| Class Name                        | Java | .NET | TypeScript |
|-----------------------------------|------|------|------------|
| BaseRawTest                       |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest              |  Y   |  Y   |     Y      |
| e2e/PlaceOrderNegativeApiTest     |  Y   |  Y   |     Y      |
| e2e/PlaceOrderNegativeUiTest      |  Y   |  Y   |     Y      |
| e2e/PlaceOrderPositiveApiTest     |  Y   |  Y   |     Y      |
| e2e/PlaceOrderPositiveUiTest      |  Y   |  Y   |     Y      |

#### Method Differences

All use `shouldPlaceOrderForValidInput` (positive) and negative-case methods consistently across languages.

#### Body Differences

All three use raw HTTP fetches with hand-rolled JSON. Identical pattern. TS uses fetch with `e2e/fixtures.ts` wrapping the Playwright test runner. All bodies align on status codes (201/200) and field assertions.

### mod04

#### Class Coverage

| Class Name                        | Java | .NET | TypeScript |
|-----------------------------------|------|------|------------|
| BaseClientTest                    |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest              |  Y   |  Y   |     Y      |
| e2e/PlaceOrder{Pos,Neg}{Api,Ui}Test|  Y  |  Y   |     Y      |
| smoke/external/ErpSmokeTest       |  Y   |  Y   |     Y      |
| smoke/external/TaxSmokeTest       |  Y   |  Y   |     Y      |
| smoke/system/ShopApiSmokeTest     |  Y   |  Y   |     Y      |
| smoke/system/ShopUiSmokeTest      |  Y   |  Y   |     Y      |

#### Method Differences / Body Differences

All three languages introduce typed `ShopApiClient`, `ShopUiClient`, `ErpRealClient`. Java/.NET use `.builder()/DTO` request objects; TS uses object literals (e.g. `{ sku, quantity }`). Bodies align structurally.

### mod05

#### Class Coverage

| Class Name                               | Java | .NET | TypeScript |
|------------------------------------------|------|------|------------|
| BaseDriverTest                           |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest                     |  Y   |  Y   |     Y      |
| e2e/PlaceOrder{Pos,Neg}ApiTest           |  Y   |  Y   |     Y      |
| e2e/PlaceOrder{Pos,Neg}BaseTest          |  Y   |  Y   |     Y      |
| e2e/PlaceOrder{Pos,Neg}UiTest            |  Y   |  Y   |     Y      |
| smoke/external/{Erp,Tax}SmokeTest        |  Y   |  Y   |     Y      |
| smoke/system/ShopApiSmokeTest            |  Y   |  Y   |     Y      |
| smoke/system/ShopBaseSmokeTest           |  Y   |  Y   |     Y      |
| smoke/system/ShopUiSmokeTest             |  Y   |  Y   |     Y      |

#### Body Differences

All three use driver abstractions: Java `shopDriver.placeOrder(...)`, .NET `_shopDriver.PlaceOrderAsync(...)`, TS `shopDriver.placeOrder(...)`. TS `Base*Test.ts` files are module helpers per the exceptions list (function/fixture-based vs class-based).

### mod06

#### Class Coverage

| Class Name                              | Java | .NET | TypeScript |
|-----------------------------------------|------|------|------------|
| BaseChannelDriverTest                   |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest                    |  Y   |  Y   |     Y      |
| e2e/PlaceOrderNegativeTest              |  Y   |  Y   |     Y      |
| e2e/PlaceOrderPositiveTest              |  Y   |  Y   |     Y      |
| smoke/external/{Erp,Tax}SmokeTest       |  Y   |  Y   |     Y      |
| smoke/system/ShopSmokeTest              |  Y   |  Y   |     Y      |

#### Body Differences

All use channel annotations + single unified tests. Java `@Channel({UI, API})`, .NET `[ChannelData(ChannelType.UI, ChannelType.API)]`, TS `forChannels(ChannelType.UI, ChannelType.API)(...)`. Bodies align.

### mod07

#### Class Coverage

| Class Name                              | Java | .NET | TypeScript |
|-----------------------------------------|------|------|------------|
| BaseUseCaseDslTest                      |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest                    |  Y   |  Y   |     Y      |
| e2e/PlaceOrderNegativeTest              |  Y   |  Y   |     Y      |
| e2e/PlaceOrderPositiveTest              |  Y   |  Y   |     Y      |
| smoke/external/{Erp,Tax}SmokeTest       |  Y   |  Y   |     Y      |
| smoke/system/ShopSmokeTest              |  Y   |  Y   |     Y      |

#### Body Differences

All three use `app.shop().placeOrder().sku(...).quantity(...)...execute().shouldSucceed()`. Aligned.

### mod08

Same class coverage as mod07 but base class is `BaseScenarioDslTest`. All bodies use `scenario.given()...when()...then()...and().order().has...()`. Aligned across Java/.NET/TS.

### mod09

#### Class Coverage

| Class Name                              | Java | .NET | TypeScript |
|-----------------------------------------|------|------|------------|
| BaseScenarioDslTest                     |  Y   |  Y   |     Y      |
| smoke/external/ClockSmokeTest           |  Y   |  Y   |     Y      |
| smoke/external/{Erp,Tax}SmokeTest       |  Y   |  Y   |     Y      |
| smoke/system/ShopSmokeTest              |  Y   |  Y   |     Y      |

#### Body Differences

All use `scenario.assume().{clock|erp|tax|shop}().shouldBeRunning()`. Aligned.

### mod10

#### Class Coverage

| Class Name                               | Java | .NET | TypeScript |
|------------------------------------------|------|------|------------|
| BaseScenarioDslTest                      |  Y   |  Y   |     Y      |
| acceptance/base/BaseAcceptanceTest       |  Y   |  Y   |     Y      |
| acceptance/PlaceOrderNegativeIsolatedTest|  Y   |  Y   |     Y      |
| acceptance/PlaceOrderNegativeTest        |  Y   |  Y   |     Y      |
| acceptance/PlaceOrderPositiveIsolatedTest|  Y   |  Y   |     Y      |
| acceptance/PlaceOrderPositiveTest        |  Y   |  Y   |     Y      |

#### Method Differences

All three use identical method names including: `shouldRejectOrderPlacedAtYearEnd`, `shouldRejectOrderWithNonIntegerQuantity`, `shouldRejectOrderForNonExistentProduct`, `shouldRejectOrderWithEmptySku`, `shouldRejectOrderWithNonPositiveQuantity`, `shouldRejectOrderWithEmptyQuantity`, `shouldRejectOrderWithNullQuantity`, `shouldApplyFullPriceOnWeekday`, `shouldApplyDiscountWhenPromotionIsActive`, `shouldRecordPlacementTimestamp`, `orderNumberShouldStartWithORD`, `orderStatusShouldBePlacedAfterPlacingOrder`.

#### Body Differences

Aligned.

### mod11

#### Class Coverage

| Class Name                                        | Java | .NET | TypeScript |
|---------------------------------------------------|------|------|------------|
| BaseScenarioDslTest                               |  Y   |  Y   |     Y      |
| contract/base/BaseExternalSystemContractTest      |  Y   |  Y   |     Y      |
| contract/clock/BaseClockContractTest              |  Y   |  Y   |     Y      |
| contract/clock/ClockRealContractTest              |  Y   |  Y   |     Y      |
| contract/clock/ClockStubContractIsolatedTest      |  Y   |  Y   |     Y      |
| contract/clock/ClockStubContractTest              |  Y   |  Y   |     Y      |
| contract/erp/BaseErpContractTest                  |  Y   |  Y   |     Y      |
| contract/erp/ErpRealContractTest                  |  Y   |  Y   |     Y      |
| contract/erp/ErpStubContractTest                  |  Y   |  Y   |     Y      |
| contract/tax/BaseTaxContractTest                  |  Y   |  Y   |     Y      |
| contract/tax/TaxRealContractTest                  |  Y   |  Y   |     Y      |
| contract/tax/TaxStubContractTest                  |  Y   |  Y   |     Y      |
| e2e/base/BaseE2eTest                              |  Y   |  Y   |     Y      |
| e2e/PlaceOrderPositiveTest                        |  Y   |  Y   |     Y      |

#### Body Differences

Aligned across languages; all use scenario DSL in e2e and the same contract-test approach per external system.

## Architecture Comparison

### Clients Layer (Driver Adapters)

#### File Coverage

| Class / Interface                                             | Java | .NET | TypeScript |
|---------------------------------------------------------------|------|------|------------|
| external/clock/ClockRealDriver                                |  Y   |  Y   |     Y      |
| external/clock/ClockStubDriver                                |  Y   |  Y   |     Y      |
| external/clock/client/ClockRealClient                         |  Y   |  Y   |     Y      |
| external/clock/client/ClockStubClient                         |  Y   |  Y   |     Y      |
| external/clock/client/dtos/ExtGetTimeResponse                 |  Y   |  Y   |     Y      |
| external/clock/client/dtos/error/ExtClockErrorResponse        |  Y   |  Y   |     Y      |
| external/erp/BaseErpDriver                                    |  Y   |  Y   |     Y      |
| external/erp/ErpRealDriver                                    |  Y   |  Y   |     Y      |
| external/erp/ErpStubDriver                                    |  Y   |  Y   |     Y      |
| external/erp/client/BaseErpClient                             |  Y   |  Y   |     Y      |
| external/erp/client/ErpRealClient                             |  Y   |  Y   |     Y      |
| external/erp/client/ErpStubClient                             |  Y   |  Y   |     Y      |
| external/erp/client/dtos/ExtCreateProductRequest              |  Y   |  Y   |     Y      |
| external/erp/client/dtos/ExtGetPromotionResponse              |  Y   |  Y   |     Y      |
| external/erp/client/dtos/ExtProductDetailsResponse            |  Y   |  Y   |     Y      |
| external/erp/client/dtos/error/ExtErpErrorResponse            |  Y   |  Y   |     Y      |
| external/tax/BaseTaxDriver                                    |  Y   |  Y   |     Y      |
| external/tax/TaxRealDriver                                    |  Y   |  Y   |     Y      |
| external/tax/TaxStubDriver                                    |  Y   |  Y   |     Y      |
| external/tax/client/BaseTaxClient                             |  Y   |  Y   |     Y      |
| external/tax/client/TaxRealClient                             |  Y   |  Y   |     Y      |
| external/tax/client/TaxStubClient                             |  Y   |  Y   |     Y      |
| external/tax/client/dtos/ExtGetCountryResponse                |  Y   |  Y   |     Y      |
| external/tax/client/dtos/error/ExtTaxErrorResponse            |  Y   |  Y   |     Y      |
| shared/client/http/HttpStatus                                 |  Y   |  Y   |     Y      |
| shared/client/http/JsonHttpClient (TS: json-http-client.ts)   |  Y   |  Y   |     Y      |
| shared/client/playwright/PageClient                           |  Y   |  Y   |     Y      |
| shared/client/playwright/withApp                              |  N   |  N   |     Y      |
| shared/client/wiremock/JsonWireMockClient (TS: json-wiremock-client.ts) |  Y   |  Y   |  Y    |
| shop/api/ShopApiDriver (TS: shop-api-driver.ts)               |  Y   |  Y   |     Y      |
| shop/api/SystemErrorMapper                                    |  Y   |  Y   |     Y      |
| shop/api/client/ShopApiClient                                 |  Y   |  Y   |     Y      |
| shop/api/client/controllers/CouponController                  |  Y   |  Y   |     Y      |
| shop/api/client/controllers/HealthController                  |  Y   |  Y   |     Y      |
| shop/api/client/controllers/OrderController                   |  Y   |  Y   |     Y      |
| shop/api/client/dtos/errors/ProblemDetailResponse             |  Y   |  Y   |     Y      |
| shop/ui/ShopUiDriver (TS: shop-ui-driver.ts)                  |  Y   |  Y   |     Y      |
| shop/ui/client/ShopUiClient                                   |  Y   |  Y   |     Y      |
| shop/ui/client/pages/BasePage                                 |  Y   |  Y   |     Y      |
| shop/ui/client/pages/CouponManagementPage                     |  Y   |  Y   |     Y      |
| shop/ui/client/pages/HomePage                                 |  Y   |  Y   |     Y      |
| shop/ui/client/pages/NewOrderPage                             |  Y   |  Y   |     Y      |
| shop/ui/client/pages/OrderDetailsPage                         |  Y   |  Y   |     Y      |
| shop/ui/client/pages/OrderHistoryPage                         |  Y   |  Y   |     Y      |

**Observations:**

- TS-only extra file `shared/client/playwright/withApp.ts` — Java/.NET have no equivalent. Likely a TS-specific Playwright-fixture helper. Needs verification of necessity; may be dead code or genuine helper.
- TS error-DTO folder naming is **inconsistent within TS**: adapter layer uses `dtos/error/` (singular) — e.g. `adapter/external/clock/client/dtos/error/ExtClockErrorResponse.ts` — while TS driver-port DTOs use `dtos/errors/` (plural) — e.g. `port/external/clock/dtos/errors/ClockErrorResponse.ts`. Per the exceptions list, plural-vs-singular across Java/.NET/TS is accepted, **but cross-adapter/port divergence within TS is not** — this is an actionable action item.

### Driver Ports Layer

#### File Coverage

| Class / Interface                              | Java | .NET | TypeScript |
|------------------------------------------------|------|------|------------|
| external/clock/ClockDriver (port)              |  Y   |  Y (IClockDriver) | Y (clock-driver.ts) |
| external/clock/dtos/GetTimeResponse            |  Y   |  Y   |     Y      |
| external/clock/dtos/ReturnsTimeRequest         |  Y   |  Y   |     Y      |
| external/clock/dtos/{error}/ClockErrorResponse |  Y   |  Y   |     Y (errors/) |
| external/erp/ErpDriver (port)                  |  Y   |  Y (IErpDriver) |     Y (erp-driver.ts) |
| external/erp/dtos/GetProductRequest            |  Y   |  Y   |     Y      |
| external/erp/dtos/GetProductResponse           |  Y   |  Y   |     Y      |
| external/erp/dtos/GetPromotionResponse         |  Y   |  Y   |     **N**  |
| external/erp/dtos/ReturnsProductRequest        |  Y   |  Y   |     Y      |
| external/erp/dtos/ReturnsPromotionRequest      |  Y   |  Y   |     Y      |
| external/erp/dtos/{error}/ErpErrorResponse     |  Y   |  Y   |     Y (errors/) |
| external/tax/TaxDriver (port)                  |  Y   |  Y (ITaxDriver) |     Y (tax-driver.ts) |
| external/tax/dtos/GetCountryRequest            |  Y   |  Y   |     Y      |
| external/tax/dtos/GetTaxResponse               |  Y   |  Y   |     Y      |
| external/tax/dtos/ReturnsTaxRateRequest        |  Y   |  Y   |     Y      |
| external/tax/dtos/{error}/TaxErrorResponse     |  Y   |  Y   |     Y (errors/) |
| shop/ShopDriver (port)                         |  Y   |  Y (IShopDriver) | Y (shop-driver.ts) |
| shop/dtos/BrowseCouponsResponse                |  Y   |  Y   |     Y      |
| shop/dtos/OrderStatus                          |  Y   |  Y   |     Y      |
| shop/dtos/PlaceOrderRequest                    |  Y   |  Y   |     Y      |
| shop/dtos/PlaceOrderResponse                   |  Y   |  Y   |     Y      |
| shop/dtos/PublishCouponRequest                 |  Y   |  Y   |     Y      |
| shop/dtos/ViewOrderResponse                    |  Y   |  Y   |     Y      |
| shop/dtos/{error}/SystemError                  |  Y   |  Y   |     Y (errors/) |
| shop/SystemResults                             |  N   |  Y   |     N      |

**Observations:**

- **TS missing `driver/port/external/erp/dtos/GetPromotionResponse.ts`** — Java and .NET both expose a port-level DTO; TS only has the adapter-level `ExtGetPromotionResponse.ts`. Needs adding in TS to maintain port/adapter separation consistency.
- **.NET has an extra `Driver.Port/Shop/SystemResults.cs` helper** that Java/TS do not have. This helper supports the C# `Result<T, E>` / `Result<VoidValue, E>` idiom and is closely tied to the `VoidValue.cs` exception noted in the Known Divergences list. Treat as a consequent of that exception (not actionable).

### Channels Layer

#### File Coverage

| Class / Interface          | Java          | .NET         | TypeScript           |
|----------------------------|---------------|--------------|----------------------|
| ChannelType                |  Y            |  Y           | Y (channel-type.ts)  |

All three define `UI` and `API` channel constants. Java uses a final class with `public static final String` fields; .NET uses `public const string`; TS uses `export const ChannelType = { ... } as const` with a derived `ChannelTypeValue` type. Functionally equivalent.

### Use Case DSL Layer

#### File Coverage

| Class / Interface                                      | Java | .NET | TypeScript |
|--------------------------------------------------------|------|------|------------|
| Configuration                                          |  Y   |  Y   |     Y      |
| UseCaseDsl                                             |  Y   |  Y   |     Y      |
| external/clock/ClockDsl                                |  Y   |  Y   |     Y      |
| external/clock/usecases/GetTime                        |  Y   |  Y   |     Y      |
| external/clock/usecases/GetTimeVerification            |  Y   |  Y   |     Y      |
| external/clock/usecases/GoToClock                      |  Y   |  Y   |     Y      |
| external/clock/usecases/ReturnsTime                    |  Y   |  Y   |     Y      |
| external/clock/usecases/base/BaseClockUseCase          |  Y   |  Y (BaseClockCommand) |   Y |
| external/clock/usecases/base/ClockUseCaseResult        |  N   |  Y   |     N      |
| external/erp/ErpDsl                                    |  Y   |  Y   |     Y      |
| external/erp/usecases/GetProduct                       |  Y   |  Y   |     Y      |
| external/erp/usecases/GetProductVerification           |  Y   |  Y   |     Y      |
| external/erp/usecases/GoToErp                          |  Y   |  Y   |     Y      |
| external/erp/usecases/ReturnsProduct                   |  Y   |  Y   |     Y      |
| external/erp/usecases/ReturnsPromotion                 |  Y   |  Y   |     Y      |
| external/erp/usecases/base/BaseErpUseCase              |  Y   |  Y (BaseErpCommand) |   Y  |
| external/erp/usecases/base/ErpUseCaseResult            |  N   |  Y   |     N      |
| external/tax/TaxDsl                                    |  Y   |  Y   |     Y      |
| external/tax/usecases/GetTaxRate                       |  Y   |  Y   |     Y      |
| external/tax/usecases/GetTaxVerification               |  Y   |  Y   |     Y      |
| external/tax/usecases/GoToTax                          |  Y   |  Y   |     Y      |
| external/tax/usecases/ReturnsTaxRate                   |  Y   |  Y   |     Y      |
| external/tax/usecases/base/BaseTaxUseCase              |  Y   |  Y (BaseTaxCommand) |   Y  |
| external/tax/usecases/base/TaxUseCaseResult            |  N   |  Y   |     N      |
| shop/ShopDsl                                           |  Y   |  Y   |     Y      |
| shop/commons/SystemResults                             |  Y   |  N   |     Y (system-results.ts) |
| shop/usecases/BrowseCoupons                            |  Y   |  Y   |     Y      |
| shop/usecases/BrowseCouponsVerification                |  Y   |  Y   |     Y      |
| shop/usecases/CancelOrder                              |  Y   |  Y   |     Y      |
| shop/usecases/DeliverOrder                             |  Y   |  Y   |     Y      |
| shop/usecases/GoToShop                                 |  Y   |  Y   |     Y      |
| shop/usecases/PlaceOrder                               |  Y   |  Y   |     Y      |
| shop/usecases/PlaceOrderVerification                   |  Y   |  Y   |     Y      |
| shop/usecases/PublishCoupon                            |  Y   |  Y   |     Y      |
| shop/usecases/ViewOrder                                |  Y   |  Y   |     Y      |
| shop/usecases/ViewOrderVerification                    |  Y   |  Y   |     Y      |
| shop/usecases/base/BaseShopUseCase                     |  Y   |  Y (BaseShopCommand) |   Y  |
| shop/usecases/base/ShopUseCaseResult                   |  N   |  Y   |     N      |
| shop/usecases/base/SystemErrorFailureVerification      |  N   |  Y   |     N      |

**Observations:**

- **.NET naming divergence — "UseCase" vs "Command":** .NET renames every Java `Base{X}UseCase` to `Base{X}Command` (BaseShopCommand, BaseErpCommand, BaseTaxCommand, BaseClockCommand). TS uses Java's naming (`BaseShopUseCase`, etc.). Suggestion: align .NET to Java's `Base{X}UseCase` naming for cross-language consistency. This is a **naming-only** difference, behavior is equivalent; flag as a suggestion, not a hard mismatch.
- **.NET-only helpers** `ClockUseCaseResult`, `ErpUseCaseResult`, `TaxUseCaseResult`, `ShopUseCaseResult`, `SystemErrorFailureVerification` — these are C#-specific stand-ins for the async `Result<T, Err>` composition pattern, same family as the `VoidValue.cs` / `ResultTaskExtensions.cs` exceptions. Treat as consequences of the accepted exceptions (not actionable).
- **.NET missing `shop/commons/SystemResults`** (present in Java and TS). Java uses `shop/commons/SystemResults.java`; TS uses `shop/commons/system-results.ts`. .NET has a `Driver.Port/Shop/SystemResults.cs` in the **port** layer instead — this is a **placement divergence**. Suggest adding `Dsl.Core/UseCase/Shop/Commons/SystemResults.cs` (mirroring Java) OR removing the Java/TS variants if .NET's placement is preferred. Recommended: add .NET version at use-case layer to mirror Java (Java is the reference).
- TS includes an orphan `core/use-case-context.ts` at the DSL root (outside `core/scenario/`), in addition to `core/shared/use-case-context.ts`. One is likely a duplicate/barrel. Need verification.

### Scenario DSL Layer

#### File Coverage

| Class / Interface                                      | Java | .NET | TypeScript |
|--------------------------------------------------------|------|------|------------|
| ScenarioDslImpl / ScenarioDsl                          |  Y   |  Y   |     Y      |
| scenario/ExecutionResult                               |  Y   |  Y   |     Y      |
| scenario/ExecutionResultBuilder                        |  Y   |  Y   |     Y      |
| scenario/ExecutionResultContext                        |  Y   |  Y   |     Y (scenario-context.ts + app-context.ts split) |
| scenario/ScenarioDefaults / GherkinDefaults / DEFAULTS |  Y (ScenarioDefaults) | Y (GherkinDefaults) | Y (DEFAULTS in defaults.ts) |
| scenario/assume/AssumeImpl / AssumeStage               |  Y   |  Y   |     Y      |
| scenario/given/GivenImpl / GivenStage                  |  Y   |  Y   |     Y (given-stage.ts) |
| scenario/given/steps/BaseGivenStep                     |  Y   |  Y   |     N      |
| scenario/given/steps/GivenClockImpl                    |  Y   |  Y   |     Y      |
| scenario/given/steps/GivenCountryImpl                  |  Y   |  Y   |     Y      |
| scenario/given/steps/GivenCouponImpl                   |  Y   |  Y   |     Y      |
| scenario/given/steps/GivenOrderImpl                    |  Y   |  Y   |     Y      |
| scenario/given/steps/GivenProductImpl                  |  Y   |  Y   |     Y      |
| scenario/given/steps/GivenPromotionImpl                |  Y   |  Y   |     Y      |
| scenario/then/ThenImpl / ThenStage                     |  Y   |  Y   |     varies |
| scenario/then/ThenResultImpl / ThenStageBase           |  Y   |  Y   |     varies |
| scenario/then/steps/BaseThenStep                       |  Y   |  Y   |     N      |
| scenario/then/steps/ThenClockImpl                      |  Y   |  Y   |     indirectly (then-contract.ts inline) |
| scenario/then/steps/ThenCountryImpl                    |  Y   |  Y   |     indirectly |
| scenario/then/steps/ThenCouponImpl                     |  Y   |  Y   |     indirectly |
| scenario/then/steps/ThenFailureImpl / ThenFailure      |  Y   |  Y   |     Y (in then-place-order.ts) |
| scenario/then/steps/ThenOrderImpl                      |  Y   |  Y   |     indirectly (ThenOrder inline in then-place-order.ts) |
| scenario/then/steps/ThenProductImpl                    |  Y   |  Y   |     indirectly (in then-contract.ts) |
| scenario/then/steps/ThenSuccessImpl / ThenSuccess      |  Y   |  Y   |     Y      |
| scenario/when/WhenImpl / WhenStage                     |  Y   |  Y   |     Y (when-stage.ts) |
| scenario/when/steps/BaseWhenStep                       |  Y   |  Y   |     N      |
| scenario/when/steps/WhenBrowseCoupons                  |  Y   |  Y   |     Y      |
| scenario/when/steps/WhenCancelOrder                    |  Y   |  Y   |     Y      |
| scenario/when/steps/WhenPlaceOrder                     |  Y   |  Y   |     Y      |
| scenario/when/steps/WhenPublishCoupon                  |  Y   |  Y   |     Y      |
| scenario/when/steps/WhenViewOrder                      |  Y   |  Y   |     Y      |

#### Observations

- **TS scenario/then/ structure is substantially different**: Java has `steps/ThenOrderImpl.java`, `steps/ThenCouponImpl.java`, etc. organized by entity. TS has per-use-case files (`then-place-order.ts`, `then-cancel-order.ts`, `then-browse-coupons.ts`, etc.) that each bundle `ThenResultStage` + `ThenSuccess` + `ThenOrder` + `ThenCoupon` + `ThenClock` + `ThenFailure` + `ThenFailureAnd` inline. This is a major architectural restructuring that is **not covered by the exceptions list**. The result: TS has no `ThenOrderImpl` / `ThenCouponImpl` / `ThenClockImpl` / `ThenProductImpl` / `ThenCountryImpl` per-entity classes — each Then{Entity} is duplicated inline inside each use-case-specific Then* file. Actionable for TS: consolidate duplicated Then{Entity} classes into single per-entity files (then-order.ts, then-coupon.ts, etc.) matching Java's `ThenOrderImpl` / `ThenCouponImpl` / `ThenClockImpl` pattern.
- **TS missing `BaseGivenStep`, `BaseThenStep`, `BaseWhenStep`** — Java and .NET both have these base classes in `steps/base/`. TS has none under `given/` or `when/` or `then/`. Actionable for TS.
- **ScenarioDsl port has `close()` in TS only** — Java/.NET do not expose a `close()` on `ScenarioDsl`; they handle teardown in test-base classes. TS uses Playwright's `closeAll()` pattern. Functional equivalent, accept as TS Playwright idiom, but note for symmetry.
- **Java/.NET have `markAsExecuted()` safeguard in ScenarioDslImpl / ScenarioDsl** that prevents re-running `.given()` or `.when()` after a scenario has executed (throws `IllegalStateException` / equivalent). TS does **not** have this safeguard. Actionable for TS: add equivalent guard on the `ScenarioDsl.given()` / `when()` methods.
- **Defaults naming divergence:** Java `ScenarioDefaults`, .NET `GherkinDefaults` (+ `Dsl.Core.Gherkin` namespace), TS `DEFAULTS` (in `defaults.ts`). Java is the reference.
  - .NET should rename `GherkinDefaults` → `ScenarioDefaults` and move namespace `Dsl.Core.Gherkin` → `Dsl.Core.Scenario` to match Java.
  - TS `DEFAULTS` key set is a subset of Java's: missing `DEFAULT_ORDER_STATUS`, `DEFAULT_VALID_FROM`, `DEFAULT_VALID_TO`, `DEFAULT_USAGE_LIMIT`, `EMPTY`. Actionable for TS.
- **TS has two different `use-case-context.ts` files** (`core/use-case-context.ts` and `core/shared/use-case-context.ts`) — likely a duplication. Actionable: consolidate to one (match Java, which has only `shared/UseCaseContext`).

### DSL Ports Layer

#### File Coverage

| Class / Interface                                         | Java | .NET | TypeScript |
|-----------------------------------------------------------|------|------|------------|
| ChannelMode                                               |  Y   |  Y   |     Y      |
| ExternalSystemMode                                        |  Y   |  Y   |     Y      |
| ScenarioDsl / IScenarioDsl                                |  Y   |  Y   |     Y      |
| assume/AssumeStage / IAssumeStage                         |  Y   |  Y   |     Y      |
| assume/steps/AssumeRunning / IAssumeRunning               |  Y   |  Y   |     Y      |
| given/GivenStage / IGivenStage                            |  Y   |  Y   |     Y      |
| given/steps/base/GivenStep / IGivenStep                   |  Y   |  Y   |     Y      |
| given/steps/GivenClock / IGivenClock                      |  Y   |  Y   |     Y      |
| given/steps/GivenCountry / IGivenCountry                  |  Y   |  Y   |     Y      |
| given/steps/GivenCoupon / IGivenCoupon                    |  Y   |  Y   |     Y      |
| given/steps/GivenOrder / IGivenOrder                      |  Y   |  Y   |     Y      |
| given/steps/GivenProduct / IGivenProduct                  |  Y   |  Y   |     Y      |
| given/steps/GivenPromotion / IGivenPromotion              |  Y   |  Y   |     Y      |
| then/ThenResultStage / IThenResultStage                   |  Y   |  N (replaced by ThenStageBase) | Y (then-result-stage.ts) |
| then/ThenStage / IThenStage                               |  Y   |  Y   |     Y      |
| then/steps/base/ThenStep                                  |  Y   |  N   |     Y (then-step.ts) |
| then/steps/ThenClock / IThenClock                         |  Y   |  Y   |     Y      |
| then/steps/ThenCountry / IThenCountry                     |  Y   |  Y   |     Y      |
| then/steps/ThenCoupon / IThenCoupon                       |  Y   |  Y   |     Y      |
| then/steps/ThenFailure / IThenFailure                     |  Y   |  Y   |     Y      |
| then/steps/ThenFailureAnd / IThenFailureAnd               |  N   |  Y   |     Y (then-failure-and.ts) |
| then/steps/ThenOrder / IThenOrder                         |  Y   |  Y   |     Y      |
| then/steps/ThenProduct / IThenProduct                     |  Y   |  Y   |     Y      |
| then/steps/ThenSuccess / IThenSuccess                     |  Y   |  Y   |     Y      |
| then/steps/ThenSuccessAnd / IThenSuccessAnd               |  N   |  Y   |     N      |
| when/WhenStage / IWhenStage                               |  Y   |  Y   |     Y      |
| when/steps/base/WhenStep / IWhenStep                      |  Y   |  Y   |     Y      |
| when/steps/WhenBrowseCoupons / IBrowseCoupons             |  Y   |  Y   |     Y      |
| when/steps/WhenCancelOrder / ICancelOrder                 |  Y   |  Y   |     Y      |
| when/steps/WhenPlaceOrder / IPlaceOrder                   |  Y   |  Y   |     Y      |
| when/steps/WhenPublishCoupon / IPublishCoupon             |  Y   |  Y   |     Y      |
| when/steps/WhenViewOrder / IViewOrder                     |  Y   |  Y   |     Y      |

**Observations:**

- `IThenFailureAnd` / `then-failure-and.ts` present in .NET and TS, not in Java — this is the accepted exception (.NET async semantics / TS async semantics). Java is synchronous and doesn't need it.
- `IThenSuccessAnd` present in .NET only — accepted exception (.NET async-split Then design).
- `IThenResultStage` in Java vs `ThenStageBase` (.NET) vs `then-result-stage.ts` (TS) — .NET uses a different name/structure (abstract class `ThenStageBase` instead of interface `IThenResultStage`). This is tied to the accepted async-Then-split exception for .NET.
- .NET port `When/Steps` uses `IBrowseCoupons`, `ICancelOrder`, `IPlaceOrder`, `IPublishCoupon`, `IViewOrder` (dropping the `When` prefix) — Java uses `WhenBrowseCoupons`, `WhenCancelOrder`, `WhenPlaceOrder`, `WhenPublishCoupon`, `WhenViewOrder`. TS uses `WhenBrowseCoupons`, etc. (matches Java). Actionable: rename .NET `IBrowseCoupons` → `IWhenBrowseCoupons`, `ICancelOrder` → `IWhenCancelOrder`, `IPlaceOrder` → `IWhenPlaceOrder`, `IPublishCoupon` → `IWhenPublishCoupon`, `IViewOrder` → `IWhenViewOrder` to match Java reference.

### Common Layer

#### File Coverage

| Class / Interface                  | Java | .NET | TypeScript |
|------------------------------------|------|------|------------|
| Closer                             |  Y   |  N   |     N      |
| Converter                          |  Y   |  Y   |     Y (converter.ts) |
| Result                             |  Y   |  Y   |     Y (result.ts) |
| ResultAssert / ResultAssertExtensions |  Y |  Y   |     Y (result-assert.ts) |
| ResultTaskExtensions               |  N   |  Y   |     N      |
| VoidValue                          |  N   |  Y   |     N      |
| dtos (barrel)                      |  N   |  N   |     Y      |

**Observations:**

- `Closer.java` — Java-only (accepted exception).
- `ResultTaskExtensions.cs`, `VoidValue.cs` — .NET-only (accepted exceptions).
- **TS-only `dtos.ts` barrel file** in `common/` — re-exports driver-port DTOs for back-compat. Not in Java/.NET. Arguably harmless; not actionable unless decided otherwise.

## Exceptions (known divergences) — Tracked but no action

### .NET-only accepted divergences (from exceptions list)

- `system-test/dotnet/Common/VoidValue.cs` — fills C# generic gap for `Result<VoidValue, E>`. Java/TS not needed.
- `system-test/dotnet/Common/ResultTaskExtensions.cs` — `MapAsync` / `MapErrorAsync` / `MapVoidAsync`. Required for C# async composition.
- Scenario Then Success/Failure split (`ThenSuccessOrder.cs`, `ThenFailureOrder.cs`, `ThenSuccessCoupon.cs`, `ThenFailureCoupon.cs`, `BaseThenResultOrder.cs`, `BaseThenResultCoupon.cs`, `ThenStageBase.cs`, `*And` variants) under `Dsl.Core/Scenario/Then/Steps/`. Required by C# async semantics.
- Two-step `.And().{Entity}()` navigation via `IThenSuccess` + `IThenSuccessAnd` and `IThenFailure` + `IThenFailureAnd` port split (`system-test/dotnet/Dsl.Port/Then/Steps/`). Do not try to collapse to Java's single-step `ThenStep<TThen>` shape.
- `.NET has a `Driver.Port/Shop/SystemResults.cs` helper** tied to the `VoidValue.cs` idiom. Treat as a consequence of the accepted exception.
- `.NET Use-Case DSL `*UseCaseResult.cs` helper classes** (`ClockUseCaseResult.cs`, `ErpUseCaseResult.cs`, `TaxUseCaseResult.cs`, `ShopUseCaseResult.cs`, `SystemErrorFailureVerification.cs`) — related to the C# async `Result<T, E>` pattern. Not actionable.

### Java-only accepted divergences

- `system-test/java/src/main/java/com/optivem/shop/testkit/common/Closer.java` — required for Java's checked-exception handling. .NET uses `using`, TS uses `try/finally`.

### TypeScript-only accepted divergences

- Plural `errors/` folder (TS) vs singular `error/` (Java/.NET) for driver-port error DTO folders — accepted, do not propose renaming to match Java/.NET.
- kebab-case filenames for client/service modules (TS) vs PascalCase (Java/.NET). Do not propose renaming.
- Two-file scenario context split (`scenario-context.ts` + `app-context.ts`) vs Java/.NET single `ExecutionResultContext`. Accepted if it reflects a deliberate SRP boundary.
- Module-exported fixtures/helper functions (TS) vs abstract base classes (Java/.NET) for shared test scaffolding. Files like `legacy/mod02/base/BaseRawTest.ts`, `legacy/mod05/smoke/system/ShopBaseSmokeTest.ts` are helper modules not inheritance bases — accepted.
- Contract tests via `registerXxxContractTests(test)` helper pattern instead of abstract-class + real/stub subclasses (TS vs Java/.NET).
- `then-failure-and.ts` (and the matching `ThenFailureAnd` class inline in `then-place-order.ts`) — TS async semantics parallel with .NET. Accepted.
- Absence of `then-success-and.ts` / `ThenSuccessAnd` in TS — intentional (async `and` only on Failure; Success `and()` returns `this` one-step). Accepted.
- `ScenarioDsl.close()` method in TS only — Playwright-specific teardown. Accepted.

## Summary of Required Changes

Total differences flagged: **21**

By language:
- Java: **0** changes needed (reference implementation — no plan file).
- .NET: **7** changes needed.
- TypeScript: **14** changes needed.

By area:
- Architectural mismatches (legacy): **0**
- Progression mismatches (legacy): **0**
- Test — Acceptance: **1** (PublishCouponNegativeTest `100percent`-vs-`100Percent` casing — cosmetic naming, no behavioral mismatch; flagged low-priority)
- Test — Contract: **0**
- Test — E2E: **0**
- Test — Smoke: **0**
- Architecture — Clients: **2** (TS `withApp.ts` orphan; TS adapter-vs-port `error/` vs `errors/` folder naming inconsistency)
- Architecture — Drivers (ports): **1** (TS missing `driver/port/external/erp/dtos/GetPromotionResponse.ts`)
- Architecture — Channels: **0**
- Architecture — Use Case DSL: **3** (.NET `Base{X}Command` vs Java `Base{X}UseCase` naming; .NET missing `Dsl.Core/UseCase/Shop/Commons/SystemResults.cs` at use-case layer; TS has duplicate `core/use-case-context.ts` vs `core/shared/use-case-context.ts`)
- Architecture — Scenario DSL: **8** (TS restructuring of `scenario/then/` into per-use-case files instead of per-entity Then{Impl}; TS missing `BaseGivenStep`/`BaseThenStep`/`BaseWhenStep`; TS missing `markAsExecuted()` safeguard; .NET `GherkinDefaults` → `ScenarioDefaults` rename + namespace move; TS `DEFAULTS` incomplete vs Java `ScenarioDefaults`; .NET `When/Steps` drops `When` prefix in 5 interfaces)
- Architecture — Common: **0** (all language-idiomatic files are on the exceptions list)

Plan files:
- `plans/20260421-1127-compare-tests-both-dotnet.md`
- `plans/20260421-1127-compare-tests-both-typescript.md`
- No Java plan file (reference implementation, 0 action items).
