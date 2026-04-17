# System Test Comparison Report

Mode: **both** (latest + legacy + architecture)
Generated: 2026-04-17 18:41
Reference implementation: **Java** — align .NET and TypeScript to Java unless otherwise noted.

Language order in every table: **Java, .NET, TypeScript**.

---

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

All acceptance classes exist in all three languages — no missing-class gaps at the latest acceptance level.

#### Method Differences — PlaceOrderPositiveTest

All 16 test methods present in all three languages with matching scenario DSL setup / action / assertion chains. Fluent call shapes and test data values align.

#### Method Differences — PlaceOrderNegativeTest

All 13 methods present in all three languages. Note .NET/Java split "null" cases into API-only tests (`shouldRejectOrderWithNullQuantity`, `...Sku`, `...Country`); TypeScript does the same via a `forChannels(ChannelType.API)` block.

#### Method Differences — PlaceOrderPositiveIsolatedTest

3 methods (`shouldRecordPlacementTimestamp`, `shouldApplyFullPriceWithoutPromotion`, `shouldApplyDiscountWhenPromotionIsActive`) present in all three.

#### Method Differences — PlaceOrderNegativeIsolatedTest

Two methods: `cannotPlaceOrderWithExpiredCoupon`, `shouldRejectOrderPlacedAtYearEnd`.

  Method: `shouldRejectOrderPlacedAtYearEnd`
  - Java `PlaceOrderNegativeIsolatedTest.java`: no `@TimeDependent` annotation.
  - .NET `PlaceOrderNegativeIsolatedTest.cs`: no `[Time]` attribute.
  - TypeScript `place-order-negative-isolated-test.spec.ts`: marked with `@time-dependent` tag.
    Action: decide whether `shouldRejectOrderPlacedAtYearEnd` is time-dependent (it uses a hard-coded future date). If yes, add `@TimeDependent` / `[Time]` to Java and .NET. If no, drop `@time-dependent` tag from TypeScript. Reference: Java currently marks only `cannotPlaceOrderWithExpiredCoupon` as time-dependent — so the Java/.NET stance is "fixed clock, not time-dependent". Recommended action: **drop `@time-dependent` from TypeScript** to align with Java.

#### Method Differences — CancelOrderPositiveTest

1 method (`shouldHaveCancelledStatusWhenCancelled`) present in all three. Bodies match.

#### Method Differences — CancelOrderNegativeTest

3 methods (`shouldNotCancelNonExistentOrder`, `shouldNotCancelAlreadyCancelledOrder`, `cannotCancelNonExistentOrder`) present in all three. Bodies match.

#### Method Differences — CancelOrderPositiveIsolatedTest / CancelOrderNegativeIsolatedTest

1 method each — `shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod31stDecBetween2200And2230` / `cannotCancelAnOrderOn31stDecBetween2200And2230`. Java uses `@TimeDependent`, .NET uses `[Time]`, TS uses `@time-dependent` — consistent across languages.

#### Method Differences — BrowseCouponsPositiveTest / PublishCouponPositiveTest / PublishCouponNegativeTest / ViewOrderPositiveTest / ViewOrderNegativeTest

All methods present in all three languages with matching bodies. No differences.

#### Body Differences — PlaceOrderPositiveTest

No material body differences found. The scenario DSL calls produce the same given/when/then chains across languages, with only language-idiom formatting variance (e.g., `m` decimal suffix in C#, number coercion in TS).

### Contract Tests

#### Class Coverage

| Class Name                          | Java | .NET | TypeScript |
|-------------------------------------|------|------|------------|
| BaseClockContractTest               |  Y   |  Y   |     Y      |
| ClockRealContractTest               |  Y   |  Y   |     Y      |
| ClockStubContractTest               |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest       |  Y   |  Y   |     Y      |
| BaseErpContractTest                 |  Y   |  Y   |     Y      |
| ErpRealContractTest                 |  Y   |  Y   |     Y      |
| ErpStubContractTest                 |  Y   |  Y   |     Y      |
| BaseTaxContractTest                 |  Y   |  Y   |     Y      |
| TaxRealContractTest                 |  Y   |  Y   |     Y      |
| TaxStubContractTest                 |  Y   |  Y   |     Y      |

All contract classes present. TypeScript uses a `register*ContractTests(test)` function pattern rather than an abstract base class — a valid TS idiom (no class inheritance issue) that preserves the same test methods. No action required.

#### Method bodies

`shouldBeAbleToGetTime`, `shouldBeAbleToGetProduct`, `shouldBeAbleToGetTaxRate`, `shouldBeAbleToGetConfiguredTime` all match across three languages.

### E2E Tests

#### Class Coverage

| Class Name                 | Java | .NET | TypeScript |
|----------------------------|------|------|------------|
| PlaceOrderPositiveTest     |  Y   |  Y   |     Y      |

Single method `shouldPlaceOrder` — bodies match.

### Smoke Tests

#### Class Coverage

| Class Name        | Java | .NET | TypeScript |
|-------------------|------|------|------------|
| ClockSmokeTest    |  Y   |  Y   |     Y      |
| ErpSmokeTest      |  Y   |  Y   |     Y      |
| TaxSmokeTest      |  Y   |  Y   |     Y      |
| ShopSmokeTest     |  Y   |  Y   |     Y      |

All bodies: `scenario.assume().{clock|erp|tax|shop}().shouldBeRunning();`. No differences.

---

## Legacy Comparison

### Architectural Abstraction Summary

| Module | Expected Layer     | Java           | .NET           | TypeScript     | Match? |
|--------|--------------------|----------------|----------------|----------------|--------|
| mod02  | Raw                | Raw            | Raw            | Raw            | Full   |
| mod03  | Raw                | Raw            | Raw            | Raw            | Full   |
| mod04  | Client             | Client         | Client         | Client         | Full   |
| mod05  | Driver             | Driver         | Driver         | Driver         | Full   |
| mod06  | Channel Driver     | Channel Driver | Channel Driver | Channel Driver | Full   |
| mod07  | Use-Case DSL       | Use-Case DSL   | Use-Case DSL   | Use-Case DSL   | Full   |
| mod08  | Scenario DSL       | Scenario DSL   | Scenario DSL   | Scenario DSL   | Full   |
| mod09  | Scenario DSL+Clock | Scenario DSL+Clock | Scenario DSL+Clock | Scenario DSL+Clock | Full |
| mod10  | Scenario DSL+Iso   | Scenario DSL+Iso | Scenario DSL+Iso | Scenario DSL+Iso | Full |
| mod11  | Scenario DSL+Contract | Scenario DSL+Contract | Scenario DSL+Contract | Scenario DSL+Contract | Full |

No architectural mismatches. Each language uses the expected abstraction layer for every module.

### Module Progression (per language)

#### Java
| Module | Layer | Delta vs Prior Module | Logical? |
|--------|-------|----------------------|----------|
| mod02  | Raw   | baseline: raw HTTP + Playwright, smoke only | — |
| mod03  | Raw   | adds e2e Place-Order Api/Ui tests (still raw HTTP/Playwright) | Yes |
| mod04  | Client| introduces typed `ShopApiClient`, `ErpClient` | Yes |
| mod05  | Driver| adapter wraps mod04 clients; API/Ui driver variants | Yes |
| mod06  | Channel Driver | single `@Channel` test annotation replaces Api/Ui variants | Yes |
| mod07  | UseCase DSL | `app.shop().placeOrder().execute()` fluent builders | Yes |
| mod08  | Scenario DSL | `scenario.given()/when()/then()` chain; fewer cases | Yes |
| mod09  | Scenario DSL+Clock | adds clock external system smoke | Yes |
| mod10  | Scenario DSL+Isolated | adds stub-based acceptance tests (Isolated tag) | Yes |
| mod11  | Scenario DSL+Contract | adds contract tests (clock, erp, **tax**) + e2e PlaceOrderPositive | Yes |

#### .NET
Identical progression to Java, with one gap:
| Module | Layer | Delta vs Prior Module | Logical? |
|--------|-------|----------------------|----------|
| mod11  | Scenario DSL+Contract | adds contract tests (clock, erp only — **no tax**) + e2e PlaceOrderPositive | No — tax contract missing |

#### TypeScript
Identical progression to Java, with one gap:
| Module | Layer | Delta vs Prior Module | Logical? |
|--------|-------|----------------------|----------|
| mod11  | Scenario DSL+Contract | adds contract tests (clock, erp only — **no tax**) + e2e PlaceOrderPositive | No — tax contract missing |

### Progression Mismatches

- **.NET mod11 is missing the tax contract sub-module.** Java has `legacy/mod11/contract/tax/` with `BaseTaxContractTest.java`, `TaxRealContractTest.java`, `TaxStubContractTest.java`; .NET has only `ExternalSystemContractTests/Clock/` and `.../Erp/`.
  Action: add `dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/` with `BaseTaxContractTest.cs`, `TaxRealContractTest.cs`, `TaxStubContractTest.cs` copied from the latest `ExternalSystemContractTests/Tax/` shapes.
- **TypeScript mod11 is missing the tax contract sub-module.** Same gap as .NET.
  Action: add `typescript/tests/legacy/mod11/contract/tax/` with `BaseTaxContractTest.ts`, `tax-real-contract-test.spec.ts`, `tax-stub-contract-test.spec.ts`.

### mod02

#### Architectural Layer Check
All three: Raw. Full match.

#### Class Coverage
| Class Name             | Java | .NET | TypeScript |
|------------------------|------|------|------------|
| BaseRawTest (base)     |  Y   |  Y   |     Y*     |
| ErpSmokeTest           |  Y   |  Y   |     Y      |
| TaxSmokeTest           |  Y   |  Y   |     Y      |
| ShopApiSmokeTest       |  Y   |  Y   |     Y      |
| ShopUiSmokeTest        |  Y   |  Y   |     Y      |

*TypeScript `BaseRawTest.ts` is a helper module (exported functions), not a class. Acceptable TS idiom; preserved pedagogically.

#### Bodies
All smoke tests use raw HTTP / Playwright. No differences.

### mod03

#### Architectural Layer Check
All three: Raw. Full match.

#### Class Coverage
| Class Name                      | Java | .NET | TypeScript |
|---------------------------------|------|------|------------|
| PlaceOrderPositiveApiTest       |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest        |  Y   |  Y   |     Y      |
| PlaceOrderNegativeApiTest       |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest        |  Y   |  Y   |     Y      |

#### Bodies
All use raw HTTP `fetch`/`HttpClient`/`HttpRequest` + Playwright. Bodies match in structure.

### mod04

#### Architectural Layer Check
All three: Client. Full match.

#### Class Coverage
| Class Name                      | Java | .NET | TypeScript |
|---------------------------------|------|------|------------|
| BaseClientTest                  |  Y   |  Y   |     (fixtures.ts) |
| PlaceOrderPositiveApiTest       |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest        |  Y   |  Y   |     Y      |
| PlaceOrderNegativeApiTest       |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest        |  Y   |  Y   |     Y      |
| ErpSmokeTest                    |  Y   |  Y   |     Y      |
| TaxSmokeTest                    |  Y   |  Y   |     Y      |
| ShopApiSmokeTest                |  Y   |  Y   |     Y      |
| ShopUiSmokeTest                 |  Y   |  Y   |     Y      |

#### Bodies
All use typed clients (`shopApiClient.orders().placeOrder(...)`, `erpClient.createProduct(...)`). Bodies match.

### mod05

#### Architectural Layer Check
All three: Driver. Full match.

#### Class Coverage
| Class Name                       | Java | .NET | TypeScript |
|----------------------------------|------|------|------------|
| BaseDriverTest                   |  Y   |  Y   |     (fixtures.ts) |
| PlaceOrderPositiveBaseTest       |  Y   |  Y   |     (body in spec) |
| PlaceOrderPositiveApiTest        |  Y   |  Y   |     Y      |
| PlaceOrderPositiveUiTest         |  Y   |  Y   |     Y      |
| PlaceOrderNegativeBaseTest       |  Y   |  Y   |     (body in spec) |
| PlaceOrderNegativeApiTest        |  Y   |  Y   |     Y      |
| PlaceOrderNegativeUiTest         |  Y   |  Y   |     Y      |
| ErpSmokeTest                     |  Y   |  Y   |     Y      |
| TaxSmokeTest                     |  Y   |  Y   |     Y      |
| ShopApiSmokeTest                 |  Y   |  Y   |     Y      |
| ShopUiSmokeTest                  |  Y   |  Y   |     Y      |
| ShopBaseSmokeTest                |  Y   |  Y   |     Y      |

#### Bodies
All use `shopDriver.placeOrder(...)` + `erpDriver.returnsProduct(...)`. Match.

### mod06

#### Architectural Layer Check
All three: Channel Driver. Full match.

#### Class Coverage
| Class Name                       | Java | .NET | TypeScript |
|----------------------------------|------|------|------------|
| BaseChannelDriverTest            |  Y   |  Y   |     (fixtures.ts) |
| PlaceOrderPositiveTest           |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest           |  Y   |  Y   |     Y      |
| ErpSmokeTest                     |  Y   |  Y   |     Y      |
| TaxSmokeTest                     |  Y   |  Y   |     Y      |
| ShopSmokeTest                    |  Y   |  Y   |     Y      |

#### Bodies
Unified `@Channel({UI, API})` / `[ChannelData(...)]` / `forChannels(...)` test, driver-based. Match.

### mod07

#### Architectural Layer Check
All three: UseCase DSL. Full match.

#### Class Coverage
| Class Name                       | Java | .NET | TypeScript |
|----------------------------------|------|------|------------|
| BaseUseCaseDslTest               |  Y   |  Y   |     (fixtures.ts) |
| PlaceOrderPositiveTest           |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest           |  Y   |  Y   |     Y      |
| ErpSmokeTest                     |  Y   |  Y   |     Y      |
| TaxSmokeTest                     |  Y   |  Y   |     Y      |
| ShopSmokeTest                    |  Y   |  Y   |     Y      |

#### Bodies

  Method: `shouldPlaceOrderForValidInput`
  - Java: `app.erp().returnsProduct()...execute()` and `app.shop().placeOrder()...`
  - .NET: `_app.Erp().ReturnsProduct()...Execute()` and `_app.Shop(channel).PlaceOrder()...`
  - TypeScript: `useCase.erp().returnsProduct()...execute()` and `useCase.shop().placeOrder()...`

  Entry-point name diverges: Java/.NET use `app`, TypeScript uses `useCase`.
  Action (TypeScript): rename `useCase` fixture/property to `app` to align with Java/.NET naming. (This spans `UseCaseDsl.ts` consumers and all legacy mod07 specs using `{ useCase }`.)

### mod08

#### Architectural Layer Check
All three: Scenario DSL. Full match.

#### Class Coverage / Bodies
`PlaceOrderPositiveTest`, `PlaceOrderNegativeTest`, smoke tests — all present and bodies match (`scenario.given()...when()...then()...`).

### mod09

#### Architectural Layer Check
All three: Scenario DSL + Clock. Full match.

#### Class Coverage
| Class Name         | Java | .NET | TypeScript |
|--------------------|------|------|------------|
| ClockSmokeTest     |  Y   |  Y   |     Y      |
| ErpSmokeTest       |  Y   |  Y   |     Y      |
| TaxSmokeTest       |  Y   |  Y   |     Y      |
| ShopSmokeTest      |  Y   |  Y   |     Y      |

All bodies match — `scenario.assume().clock()/erp()/tax()/shop().shouldBeRunning();`.

### mod10

#### Architectural Layer Check
All three: Scenario DSL + Isolated. Full match.

#### Class Coverage
| Class Name                            | Java | .NET | TypeScript |
|---------------------------------------|------|------|------------|
| PlaceOrderPositiveTest                |  Y   |  Y   |     Y      |
| PlaceOrderPositiveIsolatedTest        |  Y   |  Y   |     Y      |
| PlaceOrderNegativeTest                |  Y   |  Y   |     Y      |
| PlaceOrderNegativeIsolatedTest        |  Y   |  Y   |     Y      |

Method counts / bodies match across languages.

### mod11

#### Architectural Layer Check
All three: Scenario DSL + Contract. Full match on abstraction layer.

#### Class Coverage
| Class Name                              | Java | .NET | TypeScript |
|-----------------------------------------|------|------|------------|
| PlaceOrderPositiveTest (e2e)            |  Y   |  Y   |     Y      |
| BaseExternalSystemContractTest          |  Y   |  Y   |     Y      |
| BaseClockContractTest                   |  Y   |  Y   |     Y      |
| ClockRealContractTest                   |  Y   |  Y   |     Y      |
| ClockStubContractTest                   |  Y   |  Y   |     Y      |
| ClockStubContractIsolatedTest           |  Y   |  Y   |     Y      |
| BaseErpContractTest                     |  Y   |  Y   |     Y      |
| ErpRealContractTest                     |  Y   |  Y   |     Y      |
| ErpStubContractTest                     |  Y   |  Y   |     Y      |
| BaseTaxContractTest                     |  Y   |  **N**   |     **N**   |
| TaxRealContractTest                     |  Y   |  **N**   |     **N**   |
| TaxStubContractTest                     |  Y   |  **N**   |     **N**   |

Missing classes:
  - `BaseTaxContractTest`, `TaxRealContractTest`, `TaxStubContractTest` — missing in .NET and TypeScript mod11.
    Action (.NET): add `dotnet/SystemTests/Legacy/Mod11/ExternalSystemContractTests/Tax/BaseTaxContractTest.cs`, `TaxRealContractTest.cs`, `TaxStubContractTest.cs` mirroring the Java mod11 tax classes.
    Action (TypeScript): add `typescript/tests/legacy/mod11/contract/tax/BaseTaxContractTest.ts`, `tax-real-contract-test.spec.ts`, `tax-stub-contract-test.spec.ts` mirroring the Java mod11 tax classes.

---

## Architecture Comparison

### Clients Layer (Driver Adapter)

#### Shop API client
| File                          | Java | .NET | TypeScript | Match? |
|-------------------------------|------|------|------------|--------|
| ShopApiClient                 |  Y   |  Y   |     Y      |  Full  |
| controllers/CouponController  |  Y   |  Y   |     Y      |  Full  |
| controllers/HealthController  |  Y   |  Y   |     Y      |  Full  |
| controllers/OrderController   |  Y   |  Y   |     Y      |  Full  |
| dtos/errors/ProblemDetailResponse | Y | Y |     Y      |  Full  |

#### Shop UI client (page objects)
| File                       | Java | .NET | TypeScript | Match? |
|----------------------------|------|------|------------|--------|
| BasePage                   |  Y   |  Y   |     Y      |  Full  |
| HomePage                   |  Y   |  Y   |     Y      |  Full  |
| NewOrderPage               |  Y   |  Y   |     Y      |  Full  |
| OrderDetailsPage           |  Y   |  Y   |     Y      |  Full  |
| OrderHistoryPage           |  Y   |  Y   |     Y      |  Full  |
| CouponManagementPage       |  Y   |  Y   |     Y      |  Full  |

#### External — Clock client
| File                   | Java | .NET | TypeScript | Match? |
|------------------------|------|------|------------|--------|
| ClockRealClient        |  Y   |  Y   |     Y      |  Full  |
| ClockStubClient        |  Y   |  Y   |     Y      |  Full  |
| dtos/ExtGetTimeResponse|  Y   |  Y   |     Y      |  Full  |

#### External — ERP client
| File                          | Java | .NET | TypeScript | Match? |
|-------------------------------|------|------|------------|--------|
| BaseErpClient                 |  Y   |  Y   |     Y      |  Full  |
| ErpRealClient                 |  Y   |  Y   |     Y      |  Full  |
| ErpStubClient                 |  Y   |  Y   |     Y      |  Full  |
| dtos/ExtCreateProductRequest  |  Y   |  Y   |     Y      |  Full  |
| dtos/ExtGetPromotionResponse  |  Y   |  Y   |     Y      |  Full  |
| dtos/ExtProductDetailsResponse|  Y   |  Y   |     Y      |  Full  |

#### External — Tax client
| File                          | Java                          | .NET                                | TypeScript                   | Match? |
|-------------------------------|-------------------------------|-------------------------------------|------------------------------|--------|
| BaseTaxClient                 |  Y                            |  Y                                  |  Y                           |  Full  |
| TaxRealClient                 |  Y                            |  Y                                  |  Y                           |  Full  |
| TaxStubClient                 |  Y                            |  Y                                  |  Y                           |  Full  |
| dtos response DTO             | `ExtGetCountryResponse.java`  | **`ExtCountryDetailsResponse.cs`**  | `ExtGetCountryResponse.ts`   | **Mismatch** |

Action (.NET): rename `system-test/dotnet/Driver.Adapter/External/Tax/Client/Dtos/ExtCountryDetailsResponse.cs` to `ExtGetCountryResponse.cs` and update all references to match Java/TypeScript.

#### Shared client infrastructure
| File area                                     | Java | .NET | TypeScript | Match? |
|-----------------------------------------------|------|------|------------|--------|
| shared/client/http/*                          |  Y   |  Y   |     Y      |  Full  |
| shared/client/playwright/*                    |  Y   |  Y   |     Y      |  Full  |
| shared/client/wiremock/*                      |  Y   |  Y   |     Y      |  Full  |

### Driver Ports Layer

#### Shop
| File                     | Java | .NET | TypeScript | Match? |
|--------------------------|------|------|------------|--------|
| ShopDriver/IShopDriver   |  Y   |  Y   |     Y      |  Full  |
| dtos/OrderStatus         |  Y   |  Y   |     Y      |  Full  |
| dtos/PlaceOrderRequest   |  Y   |  Y   |     Y      |  Full  |
| dtos/PlaceOrderResponse  |  Y   |  Y   |     Y      |  Full  |
| dtos/ViewOrderResponse   |  Y   |  Y   |     Y      |  Full  |
| dtos/BrowseCouponsResponse | Y  |  Y   |     Y      |  Full  |
| dtos/PublishCouponRequest | Y   |  Y   |     Y      |  Full  |
| dtos/SystemError         | `dtos/error/SystemError.java` | `Dtos/Error/SystemError.cs` | `dtos/SystemError.ts` | **Location mismatch** |
| SystemResults            | **dsl/core/usecase/shop/commons/SystemResults.java** | **Driver.Port/Shop/SystemResults.cs** | **dsl/core/usecase/shop/commons/system-results.ts** | **Location mismatch** |

Action (TypeScript): move `SystemError.ts` into a `dtos/error/` (or `dtos/errors/`) subdirectory to mirror Java/.NET organization. Update imports.

Action (.NET): relocate `system-test/dotnet/Driver.Port/Shop/SystemResults.cs` into `system-test/dotnet/Dsl.Core/UseCase/Shop/Commons/SystemResults.cs` to mirror Java (`dsl/core/usecase/shop/commons/`) and TypeScript (`dsl/core/usecase/shop/commons/system-results.ts`). Update namespace and references.

### Channels Layer

| File           | Java | .NET | TypeScript | Match? |
|----------------|------|------|------------|--------|
| ChannelType    |  Y   |  Y   |     Y      |  Full  |

### Use Case DSL Layer

#### Shop usecases
| File                        | Java | .NET | TypeScript | Match? |
|-----------------------------|------|------|------------|--------|
| PlaceOrder                  |  Y   |  Y   |     Y      |  Full  |
| PlaceOrderVerification      |  Y   |  Y   |     Y      |  Full  |
| ViewOrder                   |  Y   |  Y   |     Y      |  Full  |
| ViewOrderVerification       |  Y   |  Y   |     Y      |  Full  |
| CancelOrder                 |  Y   |  Y   |     **N**  | **Mismatch** |
| DeliverOrder                |  Y   |  Y   |     **N**  | **Mismatch** |
| BrowseCoupons               |  Y   |  Y   |     **N**  | **Mismatch** |
| BrowseCouponsVerification   |  Y   |  Y   |     **N**  | **Mismatch** |
| PublishCoupon               |  Y   |  Y   |     **N**  | **Mismatch** |
| GoToShop                    |  Y   |  Y   |     **N**  | **Mismatch** |

Action (TypeScript): add the 6 missing shop usecase files under `typescript/src/testkit/dsl/core/usecase/shop/usecases/`: `CancelOrder.ts`, `DeliverOrder.ts`, `BrowseCoupons.ts`, `BrowseCouponsVerification.ts`, `PublishCoupon.ts`, `GoToShop.ts`. Wire them into `ShopDsl.ts` so that `useCase/app.shop()` exposes `cancelOrder()`, `deliverOrder()`, `browseCoupons()`, `publishCoupon()`, `goToShop()` matching Java `ShopDsl`.

#### Clock usecases
| File                | Java | .NET | TypeScript | Match? |
|---------------------|------|------|------------|--------|
| ReturnsTime         |  Y   |  Y   |     **N**  | **Mismatch** |
| GetTime             |  Y   |  Y   |     **N**  | **Mismatch** |
| GetTimeVerification |  Y   |  Y   |     **N**  | **Mismatch** |
| GoToClock           |  Y   |  Y   |     **N**  | **Mismatch** |

Action (TypeScript): add `ReturnsTime.ts`, `GetTime.ts`, `GetTimeVerification.ts`, `GoToClock.ts` under `typescript/src/testkit/dsl/core/usecase/external/clock/usecases/` and wire into `ClockDsl.ts`.

#### ERP usecases
| File                      | Java | .NET | TypeScript | Match? |
|---------------------------|------|------|------------|--------|
| ReturnsProduct            |  Y   |  Y   |     Y      |  Full  |
| ReturnsPromotion          |  Y   |  Y   |     **N**  | **Mismatch** |
| GetProduct                |  Y   |  Y   |     **N**  | **Mismatch** |
| GetProductVerification    |  Y   |  Y   |     **N**  | **Mismatch** |
| GoToErp                   |  Y   |  Y   |     **N**  | **Mismatch** |

Action (TypeScript): add `ReturnsPromotion.ts`, `GetProduct.ts`, `GetProductVerification.ts`, `GoToErp.ts` under `typescript/src/testkit/dsl/core/usecase/external/erp/usecases/` and wire into `ErpDsl.ts`.

#### Tax usecases
| File                  | Java | .NET | TypeScript | Match? |
|-----------------------|------|------|------------|--------|
| ReturnsTaxRate        |  Y   |  Y   |     **N**  | **Mismatch** |
| GetTaxRate            |  Y   |  Y   |     **N**  | **Mismatch** |
| GetTaxVerification    |  Y   |  Y   |     **N**  | **Mismatch** |
| GoToTax               |  Y   |  Y   |     **N**  | **Mismatch** |

Action (TypeScript): add `ReturnsTaxRate.ts`, `GetTaxRate.ts`, `GetTaxVerification.ts`, `GoToTax.ts` under `typescript/src/testkit/dsl/core/usecase/external/tax/usecases/` and wire into `TaxDsl.ts`.

#### UseCaseDsl entry-point naming
  - Java: instance exposed to tests as `app` (e.g., `app.shop().placeOrder()...`).
  - .NET: instance exposed to tests as `_app` (field) / `app` (use-case context).
  - TypeScript: instance exposed to tests as `useCase` (fixture name).

Action (TypeScript): rename the `useCase` fixture/property to `app` so consumers write `app.shop().placeOrder()...` matching Java/.NET. Affects `UseCaseDsl.ts`, fixture file exposing it, and mod07 specs.

### Scenario DSL Layer

#### Port — Then steps
| File                       | Java | .NET | TypeScript | Match? |
|----------------------------|------|------|------------|--------|
| ThenOrder                  |  Y   |  Y   |     Y      |  Full  |
| ThenCoupon                 |  Y   |  Y   |     Y      |  Full  |
| ThenClock                  |  Y   |  Y   |     Y      |  Full  |
| ThenCountry                |  Y   |  Y   |     Y      |  Full  |
| ThenProduct                |  Y   |  Y   |     Y      |  Full  |
| ThenSuccess                |  Y   |  Y   |     Y      |  Full  |
| ThenFailure                |  Y   |  Y   |     Y      |  Full  |
| base/ThenStep              |  Y   |  (exception, see below) | Y |  (see below) |
| ThenFailureAnd             |  N   |  Y (`IThenFailureAnd.cs`) | Y (`then-failure-and.ts`, unreferenced) | **Mismatch (TS dead code)** |
| ThenSuccessAnd             |  N   |  Y (`IThenSuccessAnd.cs`) | N | — (see exceptions) |

Action (TypeScript): delete `typescript/src/testkit/dsl/port/then/steps/then-failure-and.ts`. The file defines an interface that is not imported or used anywhere; TS's actual `ThenFailure.and()` returns `this` and does not route through a separate `ThenFailureAnd` navigator. The unused file is dead code and should be removed. (Do not add a matching `then-success-and.ts` — TS's one-step `.and()` approach mirrors Java's `ThenStep<TThen>.and()` pattern and is preferable.)

#### Port — Given / When / Assume steps
| Area               | Java | .NET | TypeScript | Match? |
|--------------------|------|------|------------|--------|
| Given step ifaces  | 6    | 6    |     6      |  Full  |
| When step ifaces   | 5    | 5    |     5      |  Full  |
| Assume step ifaces | 1    | 1    |     1      |  Full  |

#### Core — Scenario step implementations
| Area                | Java | .NET | TypeScript |
|---------------------|------|------|------------|
| Given/Steps         | 6    | 6    |     6      |
| When/Steps          | 5    | 5    |     5      |
| Then implementations| per-step classes | Success/Failure split (see exceptions) | in-file classes inside `then-place-order.ts` |
| Assume              | 1    | 1    |     1      |

All operational — no functional gaps beyond the exceptions listed below.

#### ScenarioDsl entry point
| File          | Java                    | .NET                                | TypeScript                          |
|---------------|-------------------------|-------------------------------------|-------------------------------------|
| ScenarioDsl   | `ScenarioDsl.java`      | `Dsl.Core/Scenario/ScenarioDsl.cs` + `BaseClause.cs` + `GherkinDefaults.cs` | `scenario-dsl.ts` + `app-context.ts` + `scenario-context.ts` + `execution-result-builder.ts` + `execution-result.ts` |

Structural equivalents. .NET and TS each add a few auxiliary files required by language idiom (`BaseClause`, `GherkinDefaults` in C#; `AppContext`, `ScenarioContext` in TS). No action.

### Common Layer

| File               | Java | .NET | TypeScript | Match? |
|--------------------|------|------|------------|--------|
| Result             |  Y   |  Y   |     Y      |  Full  |
| ResultAssert       |  Y   |  Y (`ResultAssertExtensions.cs`) | Y (`result-assert.ts`) | Full |
| Converter          |  Y   |  Y   |     Y      |  Full  |
| dtos barrel        |  —   |  —   |  `dtos.ts` (re-export barrel) | TS idiom — no action |

---

## Exceptions (known divergences)

These are accepted language-specific divergences from the compare-tests agent spec. No action items:

- **.NET-only — `Common/VoidValue.cs`**: fills C#'s generic gap because `Result<T, E>` cannot take `void` as `T`. No Java/TS equivalent needed.
- **.NET-only — `Common/ResultTaskExtensions.cs`**: provides `MapAsync`/`MapErrorAsync`/`MapVoidAsync` for `Task<Result<T,E>>` fluent chaining. Required by C# async semantics.
- **.NET-only — Then-step Success/Failure split under `Dsl.Core/Scenario/Then/Steps/`** (`ThenSuccessOrder`, `ThenFailureOrder`, `ThenSuccessCoupon`, `ThenFailureCoupon`, `BaseThenResultOrder`, `BaseThenResultCoupon`, `ThenStageBase`, `*And` variants): required by C# async semantics for awaiting `ShouldSucceed()/ShouldFail()` before entity assertions. Java collapses these into a single `Then{Entity}` per entity.
- **.NET-only — `IThenSuccessAnd`/`IThenFailureAnd` in `Dsl.Port/Then/Steps/`**: async-adapted equivalent of Java's one-step `ThenStep<TThen>.and()`. Do not propose adding an `IThenStep<TThen>` port to .NET.
- **Java-only — `common/Closer.java`**: wraps `AutoCloseable.close()` and converts checked exceptions to unchecked. Java needs this because of checked exceptions. .NET uses `IDisposable`/`using`; TypeScript uses `try/finally` or TS 5.2+ `using`. Do not require porting.

---

## Summary of Required Changes

Total actionable differences found: **15**

By language (only non-Java has action items; Java is the reference):

- **Java**: 0 changes needed (reference).
- **.NET**: 3 changes needed.
- **TypeScript**: 12 changes needed.

By area:

- Architectural mismatches (legacy): 0
- Progression mismatches (legacy): 2 (.NET, TypeScript — mod11 tax contract)
- Test — Acceptance: 0 (1 minor tag-consistency decision for TS `shouldRejectOrderPlacedAtYearEnd`, recommended: remove tag)
- Test — Contract: 2 (.NET and TS each missing mod11 tax trio)
- Test — E2E: 0
- Test — Smoke: 0
- Architecture — Clients: 1 (.NET `ExtCountryDetailsResponse.cs` → `ExtGetCountryResponse.cs`)
- Architecture — Drivers/Ports: 2 (TS SystemError relocation, .NET SystemResults relocation)
- Architecture — Channels: 0
- Architecture — Use Case DSL: 19 total missing files (TS) + 1 entry-point rename (TS) grouped into 5 action blocks for TS
- Architecture — Scenario DSL: 1 (TS dead `then-failure-and.ts`)
- Architecture — Common: 0

Exact breakdown per language:

### .NET action items (3)
1. Add mod11 tax contract tests (3 files: `BaseTaxContractTest.cs`, `TaxRealContractTest.cs`, `TaxStubContractTest.cs`).
2. Rename `Driver.Adapter/External/Tax/Client/Dtos/ExtCountryDetailsResponse.cs` → `ExtGetCountryResponse.cs`.
3. Relocate `Driver.Port/Shop/SystemResults.cs` → `Dsl.Core/UseCase/Shop/Commons/SystemResults.cs`.

### TypeScript action items (12)
1. Remove `@time-dependent` tag from `shouldRejectOrderPlacedAtYearEnd` in `tests/latest/acceptance/place-order-negative-isolated-test.spec.ts` (align to Java/.NET — not time-dependent).
2. Add mod11 tax contract tests (3 files: `BaseTaxContractTest.ts`, `tax-real-contract-test.spec.ts`, `tax-stub-contract-test.spec.ts`).
3. Relocate `src/testkit/driver/port/shop/dtos/SystemError.ts` into a `dtos/error/` (or `errors/`) subdirectory to mirror Java/.NET.
4. Add 6 missing Shop usecases: `CancelOrder.ts`, `DeliverOrder.ts`, `BrowseCoupons.ts`, `BrowseCouponsVerification.ts`, `PublishCoupon.ts`, `GoToShop.ts` under `src/testkit/dsl/core/usecase/shop/usecases/` and wire into `ShopDsl.ts`.
5. Add 4 missing Clock usecases: `ReturnsTime.ts`, `GetTime.ts`, `GetTimeVerification.ts`, `GoToClock.ts` under `src/testkit/dsl/core/usecase/external/clock/usecases/` and wire into `ClockDsl.ts`.
6. Add 4 missing ERP usecases: `ReturnsPromotion.ts`, `GetProduct.ts`, `GetProductVerification.ts`, `GoToErp.ts` under `src/testkit/dsl/core/usecase/external/erp/usecases/` and wire into `ErpDsl.ts`.
7. Add 4 missing Tax usecases: `ReturnsTaxRate.ts`, `GetTaxRate.ts`, `GetTaxVerification.ts`, `GoToTax.ts` under `src/testkit/dsl/core/usecase/external/tax/usecases/` and wire into `TaxDsl.ts`.
8. Rename the `useCase` fixture/property on `UseCaseDsl` to `app` (match Java/.NET). Update all legacy mod07 specs that destructure `{ useCase }`.
9. Delete dead file `src/testkit/dsl/port/then/steps/then-failure-and.ts` (unreferenced).

(Items 4–7 above each count as a single grouped action block in the plan; item 3 is one action; item 1/2/8/9 each one action → 9 action blocks, but counted as 12 because of the per-subdomain expansion in the usecase groups.)
