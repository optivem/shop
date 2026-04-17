Plan: Align Latest & Legacy System Tests and Architecture Across Languages
==========================================================================

Reference report: `reports/20260417-1436-compare-tests-both.md`

Reference implementation: **Java**. Each task aligns .NET and/or TypeScript to Java unless noted.

Ordering: architectural mismatches first, then architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common → ports), then tests (acceptance → contract → e2e → smoke).

---

## A. Architectural Mismatches (Legacy) — Highest Priority

### A1. TypeScript — mod04 UI: introduce a `ShopUiClient` page-object client
- Files: `system-test/typescript/src/testkit/driver/adapter/shop/ui/` — create `client/ShopUiClient.ts` + page objects `HomePage.ts`, `NewOrderPage.ts`, `OrderHistoryPage.ts`, `OrderDetailsPage.ts`, `CouponManagementPage.ts`, `BasePage.ts`.
- Rewrite `system-test/typescript/tests/legacy/mod04/e2e/place-order-positive-ui-test.spec.ts` and `place-order-negative-ui-test.spec.ts` to use the new UI client.
- Update `system-test/typescript/tests/legacy/mod04/e2e/fixtures.ts` to provide a `shopUiClient` fixture, replacing the raw `shopPage` usage for mod04.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/shop/ui/client/ShopUiClient.java` and page classes.

### A2. TypeScript — mod04 External systems: switch to `ErpRealClient` / `TaxRealClient`
- Edit `system-test/typescript/tests/legacy/mod04/e2e/fixtures.ts` and `system-test/typescript/tests/legacy/mod04/smoke/fixtures.ts` to use `ErpRealClient` and `TaxRealClient` (TS already has both real clients at `src/testkit/driver/adapter/external/erp/client/ErpRealClient.ts` and `.../tax/client/TaxRealClient.ts`).
- Remove the `ErpStubClient`/`TaxStubClient` fixtures from mod04 and the `WireMock`-style `configureProduct`/`configureTaxRate` calls in positive/negative spec files. Use real `createProduct` / equivalents.

### A3. TypeScript — mod05/mod06 External systems: switch to `ErpRealDriver` / `TaxRealDriver`
- Edit `system-test/typescript/tests/legacy/mod05/e2e/fixtures.ts`, `system-test/typescript/tests/legacy/mod05/smoke/fixtures.ts`, `system-test/typescript/tests/legacy/mod06/e2e/fixtures.ts`, `system-test/typescript/tests/legacy/mod06/smoke/fixtures.ts` to use `ErpRealDriver` and `TaxRealDriver` in place of the stub variants.
- Remove the extra `taxDriver.returnsTaxRate({country:'US', taxRate:'0.07'})` calls added in TS positive tests (Java/.NET do not have this step).

### A4. TypeScript — mod07: introduce a fluent use-case DSL with step builders
- File: `system-test/typescript/src/testkit/dsl/core/usecase/shop/ShopDsl.ts` and related DSL files.
- Implement fluent builders for each use case: `shop().placeOrder().sku().quantity().country().execute()`, `shop().viewOrder().orderNumber().execute()`, `erp().returnsProduct().sku().unitPrice().execute()`, etc. Result types expose `shouldSucceed()`, `shouldFail()`, `orderNumber()`, `orderNumberStartsWith()`, `sku()`, `quantity()`, `unitPrice()`, `status()`, `totalPriceGreaterThanZero()`, `errorMessage()`, `fieldErrorMessage()`.
- Rewrite `system-test/typescript/tests/legacy/mod07/e2e/place-order-positive-test.spec.ts` and `place-order-negative-test.spec.ts` to use the builder chain identical to `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod07/e2e/PlaceOrderPositiveTest.java`.
- Remove `useCase.tax().returnsTaxRate(...)` extra step.

### A5. TypeScript — mod02: introduce a `BaseRawTest` equivalent
- File: `system-test/typescript/tests/legacy/mod02/base/BaseRawTest.ts` (new).
- Extract shared `configuration`, `shopApiHttpClient` (raw `fetch` wrapper), Playwright browser setup into a base fixture module similar to Java's `BaseRawTest`. Let each spec import and use it.
- Reference: `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod02/base/BaseRawTest.java`.

### A6. TypeScript — mod11: introduce `BaseExternalSystemContractTest`, `BaseClockContractTest`, `BaseErpContractTest`
- Files (new): `system-test/typescript/tests/legacy/mod11/contract/base/BaseExternalSystemContractTest.ts`, `system-test/typescript/tests/legacy/mod11/contract/clock/BaseClockContractTest.ts`, `system-test/typescript/tests/legacy/mod11/contract/erp/BaseErpContractTest.ts`.
- Extract the `shouldBeAbleToGetTime` / `shouldBeAbleToGetProduct` body into a base contract helper that parameterizes by external-system mode. Make `clock-real-contract-test.spec.ts`, `clock-stub-contract-test.spec.ts`, `erp-real-contract-test.spec.ts`, `erp-stub-contract-test.spec.ts` thin wrappers that set the mode and call the shared test.
- Apply the same pattern to the `latest` contract tests in `system-test/typescript/tests/latest/contract/` for consistency.

### A7. TypeScript — mod03 WireMock stubbing is a layering leak
- Files: `system-test/typescript/tests/legacy/mod03/e2e/place-order-positive-api-test.spec.ts`, `place-order-positive-ui-test.spec.ts`, `place-order-negative-*.spec.ts`.
- Remove inline `fetch(\`${url}/__admin/mappings\`)` calls. Align with Java/.NET mod03: POST a real ERP product through a raw HTTP request, then run the order flow. This requires running mod03 TS against **real** external systems, not WireMock stubs. If the TS test infrastructure cannot currently support real external systems, fix the infrastructure first.

### A8. TypeScript — mod08 negative test: remove premature coverage
- File: `system-test/typescript/tests/legacy/mod08/e2e/place-order-negative-test.spec.ts`.
- Reduce to a single test `shouldRejectOrderWithNonIntegerQuantity` with `'3.5'` matching `system-test/java/.../legacy/mod08/e2e/PlaceOrderNegativeTest.java`.
- The `shouldRejectOrderForNonExistentProduct`, `shouldRejectOrderWithEmptySku`, `shouldRejectOrderWithNonPositiveQuantity`, `shouldRejectOrderWithEmptyQuantity`, `shouldRejectOrderWithNullQuantity` tests belong to mod10 in Java/.NET — keep them there and remove from mod08.

---

## B. Architecture Layers — Clients

### B1. TypeScript — split `ShopApiClient` into per-domain controllers
- Files: `system-test/typescript/src/testkit/driver/adapter/shop/api/client/ShopApiClient.ts` (refactor) + new `controllers/OrderController.ts`, `controllers/CouponController.ts`, `controllers/HealthController.ts`.
- Update callers to use `shopApiClient.orders().placeOrder(...)`, `.coupons().publishCoupon(...)`, `.health().check(...)` as in Java/.NET.

### B2. TypeScript — add explicit ShopUiClient and page objects
- Files: `system-test/typescript/src/testkit/driver/adapter/shop/ui/client/ShopUiClient.ts`, `client/pages/BasePage.ts`, `HomePage.ts`, `NewOrderPage.ts`, `OrderHistoryPage.ts`, `OrderDetailsPage.ts`, `CouponManagementPage.ts`.
- Reference: Java `shop/ui/client/ShopUiClient.java` + `pages/*.java`.

### B3. TypeScript — add `BaseErpClient` and `BaseTaxClient` abstract/shared classes
- Files: `system-test/typescript/src/testkit/driver/adapter/external/erp/client/BaseErpClient.ts`, `.../tax/client/BaseTaxClient.ts`.
- Refactor `ErpRealClient.ts`, `ErpStubClient.ts`, `TaxRealClient.ts`, `TaxStubClient.ts` to extend the base.

### B4. TypeScript — expose external client DTOs
- Files (new): `system-test/typescript/src/testkit/driver/adapter/external/erp/client/dtos/ExtCreateProductRequest.ts`, `ExtProductDetailsResponse.ts`, `ExtGetPromotionResponse.ts`, `error/ExtErpErrorResponse.ts`; `.../clock/client/dtos/ExtGetTimeResponse.ts`, `error/ExtClockErrorResponse.ts`; `.../tax/client/dtos/ExtGetCountryResponse.ts`, `error/ExtTaxErrorResponse.ts`.
- Replace anonymous inline types in the client code with these explicit DTO types.

### B5. TypeScript — add HttpStatus constants
- File (new): `system-test/typescript/src/testkit/driver/adapter/shared/http/HttpStatus.ts`.
- Reference: Java `HttpStatus.java`, .NET `HttpStatus.cs`.

### B6. TypeScript — add PageClient wrapper
- File (new): `system-test/typescript/src/testkit/driver/adapter/shared/playwright/PageClient.ts`.
- Reference: Java `PageClient.java`. Keep `withApp.ts` if it's genuinely better; otherwise converge to a common approach. (**Recommended**: consolidate on `PageClient` to match Java/.NET; revisit whether `withApp` helper stays as a test-only fixture.)

### B7. TypeScript — add `SystemErrorMapper`
- File (new): `system-test/typescript/src/testkit/driver/adapter/shop/api/SystemErrorMapper.ts`.
- Reference: Java `SystemErrorMapper.java`.

### B8. .NET — verify `SystemErrorMapper` equivalent exists
- Check `system-test/dotnet/Driver.Adapter/Shop/Api/` for an equivalent. If absent, add it mirroring Java.

### B9. TypeScript — move `ProblemDetailResponse` from port to adapter
- Current: `system-test/typescript/src/testkit/driver/port/shop/dtos/ProblemDetailResponse.ts`.
- Move to: `system-test/typescript/src/testkit/driver/adapter/shop/api/client/dtos/errors/ProblemDetailResponse.ts` to match Java/.NET placement.

---

## C. Architecture Layers — Drivers

### C1. TypeScript — add `BaseErpDriver` and `BaseTaxDriver`
- Files (new): `system-test/typescript/src/testkit/driver/adapter/external/erp/BaseErpDriver.ts`, `.../tax/BaseTaxDriver.ts`.
- Refactor `ErpRealDriver`/`ErpStubDriver` and `TaxRealDriver`/`TaxStubDriver` to extend the base classes.
- Reference: Java `BaseErpDriver.java`, `BaseTaxDriver.java`.

---

## D. Architecture Layers — Channels

No changes required (aligned across all three languages).

---

## E. Architecture Layers — Use Case DSL

### E1. TypeScript — decompose use-case DSL into per-use-case files
- Create directory trees matching Java: `system-test/typescript/src/testkit/dsl/core/usecase/shop/usecases/` containing `PlaceOrder.ts`, `PlaceOrderVerification.ts`, `CancelOrder.ts`, `ViewOrder.ts`, `ViewOrderVerification.ts`, `BrowseCoupons.ts`, `BrowseCouponsVerification.ts`, `PublishCoupon.ts`, `DeliverOrder.ts`, `GoToShop.ts`, `base/BaseShopUseCase.ts`, `SystemResults.ts`.
- Do the same for `external/clock/usecases/`, `external/erp/usecases/`, `external/tax/usecases/`.
- Refactor `ShopDsl.ts`, `ClockDsl.ts`, `ErpDsl.ts`, `TaxDsl.ts` to wire the new per-use-case classes instead of inlining logic.

### E2. TypeScript — add `DeliverOrder` use case to the DSL
- File (new): `system-test/typescript/src/testkit/dsl/core/usecase/shop/usecases/DeliverOrder.ts`.
- Wire into `ShopDsl.ts`.
- Reference: Java `DeliverOrder.java`, .NET `DeliverOrder.cs`.

### E3. TypeScript — add per-system `Base*UseCase` classes
- Files: `.../shop/usecases/base/BaseShopUseCase.ts`, `.../external/clock/usecases/base/BaseClockUseCase.ts`, `.../external/erp/usecases/base/BaseErpUseCase.ts`, `.../external/tax/usecases/base/BaseTaxUseCase.ts`.

---

## F. Architecture Layers — Scenario DSL

### F1. .NET — remove extra `WhenGoToShop.cs`
- File: `system-test/dotnet/Dsl.Core/Scenario/When/Steps/WhenGoToShop.cs`.
- Decision: Java and TS have no equivalent. Either remove from .NET, or add to Java and TS. **Recommended**: remove from .NET (Java is reference and has no equivalent).

### F2. .NET — reconcile ThenFailureAnd/ThenSuccessAnd/ThenFailureCoupon/ThenFailureOrder/ThenSuccessCoupon/ThenSuccessOrder/BaseThenResultCoupon/BaseThenResultOrder/ThenStageBase
- Files under `system-test/dotnet/Dsl.Core/Scenario/Then/Steps/`.
- Java aggregates by entity (`ThenClock`, `ThenCountry`, `ThenCoupon`, `ThenOrder`, `ThenProduct`). .NET splits by outcome+entity.
- Decision: align .NET to Java — collapse the `Success*` / `Failure*` / `*And` variants into a unified entity-based `Then*` set. **Recommended**: collapse; Java's decomposition is simpler.

### F3. TypeScript — add missing Then* at both port and core
- At the core: add `system-test/typescript/src/testkit/dsl/core/scenario/then/ThenClock.ts`, `ThenCountry.ts`, `ThenProduct.ts`.
- At the port: add `system-test/typescript/src/testkit/dsl/port/then/steps/then-clock.ts`, `then-country.ts`, `then-product.ts` as named (currently TS has `then-given-*` variants which is semantically different).
- Decision on the TS per-use-case files (`then-place-order.ts`, `then-cancel-order.ts`, `then-publish-coupon.ts`, `then-view-order.ts`, `then-browse-coupons.ts`, `then-contract.ts`): align with Java by removing the per-use-case decomposition and relying on entity-level Then steps.

### F4. TypeScript — add `ExecutionResult`, `ExecutionResultBuilder`
- Files (new): `system-test/typescript/src/testkit/dsl/core/scenario/execution-result.ts`, `execution-result-builder.ts`. Keep `scenario-context.ts` as the ExecutionResultContext analogue.
- Reference: Java `ExecutionResult.java`, `ExecutionResultBuilder.java`.

### F5. TypeScript — add `WhenStep` port base and `GivenStep`/`ThenStep` bases
- Files: `system-test/typescript/src/testkit/dsl/port/when/steps/base/when-step.ts`, `.../given/steps/base/given-step.ts`, `.../then/steps/base/then-step.ts`.

### F6. .NET — add `GivenStep`/`ThenStep` base ports
- Files: `system-test/dotnet/Dsl.Port/Given/Steps/Base/IGivenStep.cs` (exists), `Then/Steps/Base/IThenStep.cs` (add).

### F7. TypeScript — add shared DSL verification classes
- Files (new): `system-test/typescript/src/testkit/dsl/core/shared/base-use-case.ts`, `use-case-result.ts`, `error-verification.ts`, `response-verification.ts`, `void-verification.ts`.
- Reference: Java `BaseUseCase.java`, `UseCaseResult.java`, `ErrorVerification.java`, `ResponseVerification.java`, `VoidVerification.java`.

### F8. .NET — remove `ScenarioDslFactory.cs` or add to Java/TS
- Decision: .NET has it; Java does not. **Recommended**: remove from .NET to match Java.

---

## G. Architecture Layers — Common

### G1. TypeScript — add `Closer` utility
- File (new): `system-test/typescript/src/testkit/common/closer.ts`.
- Reference: Java `Closer.java`. Provides a disposal helper. (JS has native dispose semantics now; consider whether this is still needed or replace with pattern.)

### G2. TypeScript — add `Converter`
- File (new): `system-test/typescript/src/testkit/common/converter.ts`.

### G3. TypeScript — add `ResultAssert` / `ResultAssertExtensions`
- File (new): `system-test/typescript/src/testkit/common/result-assert.ts`.
- Reference: Java `ResultAssert.java`, .NET `ResultAssertExtensions.cs`.

### G4. .NET — reconcile `ResultTaskExtensions.cs` and `VoidValue.cs`
- Files: `system-test/dotnet/Common/ResultTaskExtensions.cs`, `VoidValue.cs`.
- Java does not have these. **Recommended**: keep in .NET only if language idiom requires them; document they're language-specific in a comment. Do not add to Java or TS.

### G5. Java — reconcile `Closer.java`
- Referenced by nearly every base test — keep. Ensure .NET has an equivalent pattern (IDisposable + using).

---

## H. Architecture Layers — Driver Ports

### H1. TypeScript — add `GetProductRequest`
- File (new): `system-test/typescript/src/testkit/driver/port/external/erp/dtos/GetProductRequest.ts`.
- Reference: Java `GetProductRequest.java`.

### H2. .NET — add `GetCountryRequest`
- File (new): `system-test/dotnet/Driver.Port/External/Tax/Dtos/GetCountryRequest.cs`.
- Reference: Java `GetCountryRequest.java`.

### H3. TypeScript — add `GetCountryRequest`
- File (new): `system-test/typescript/src/testkit/driver/port/external/tax/dtos/GetCountryRequest.ts`.

### H4. .NET and TypeScript — add `GetPromotionResponse`
- Files: `system-test/dotnet/Driver.Port/External/Erp/Dtos/GetPromotionResponse.cs`, `system-test/typescript/src/testkit/driver/port/external/erp/dtos/GetPromotionResponse.ts`.
- Reference: Java `GetPromotionResponse.java`.

### H5. TypeScript — add `SystemResults`
- File (new): `system-test/typescript/src/testkit/dsl/core/usecase/shop/commons/system-results.ts` (matching Java placement).
- Reference: Java `SystemResults.java`.

---

## I. Latest Tests — Acceptance

### I1. TypeScript — split `shouldRejectOrderWithNonPositiveQuantity` or adjust Java/.NET
- Current TS: parameterized `['-10', '-1', '0']` in one test `shouldRejectOrderWithNonPositiveQuantity`.
- Current Java/.NET: two separate tests `shouldRejectOrderWithNegativeQuantity`(-10) and `shouldRejectOrderWithZeroQuantity`(0).
- **Recommended**: align TS to Java/.NET by splitting into two non-parameterized tests; drop the `-1` case to match. Rationale: Java is the reference.
- Files: `system-test/typescript/tests/latest/acceptance/place-order-negative-test.spec.ts`.

### I2. TypeScript — add `@TimeDependent`/`[Time]` equivalent marker
- Files: `system-test/typescript/tests/latest/acceptance/place-order-negative-isolated-test.spec.ts` (for `cannotPlaceOrderWithExpiredCoupon`), `cancel-order-positive-isolated-test.spec.ts`, `cancel-order-negative-isolated-test.spec.ts`.
- Add a TypeScript-idiomatic equivalent (a tag or hook) and document the mapping in the TS testing helpers package (`@optivem/optivem-testing`).

### I3. TypeScript — ensure ViewOrderNegativeTest exercises UI for first row
- File: `system-test/typescript/tests/latest/acceptance/view-order-negative-test.spec.ts`.
- Change from `forChannels(ChannelType.API)` to `test.eachAlsoFirstRow(nonExistentOrderCases)` or equivalent Channel helper so the first row also runs via UI, matching Java `alsoForFirstRow = ChannelType.UI` and .NET `AlsoForFirstRow = new[] { ChannelType.UI }`.

### I4. TypeScript/Java/.NET — align PublishCouponNegativeTest discount-rate value types
- Decision: Java/.NET pass strings (`"0.0"`, `"-0.01"`); TS passes numbers.
- **Recommended**: convert TS to strings to match Java reference.
- File: `system-test/typescript/tests/latest/acceptance/publish-coupon-negative-test.spec.ts`.

---

## J. Latest Tests — Contract

### J1. TypeScript — remove stray `.withTime()` from `clock-stub-contract-test.spec.ts`
- File: `system-test/typescript/tests/latest/contract/clock/clock-stub-contract-test.spec.ts`.
- Current: `scenario.given().clock().withTime().then().clock().hasTime()`.
- Target: `scenario.given().then().clock().hasTime()` matching Java `BaseClockContractTest.java` and .NET `BaseClockContractTest.cs`.

### J2. TypeScript — align `tax-real/stub-contract-test.spec.ts` taxRate argument type
- Current TS: `.withTaxRate('0.09')`.
- Java: `.withTaxRate(0.09)` (double).
- **Recommended**: convert TS to numeric. Files under `system-test/typescript/tests/latest/contract/tax/`.

---

## K. Latest Tests — E2E

No changes required.

---

## L. Latest Tests — Smoke

No changes required.

---

## M. Legacy Tests — mod02

Covered under A5.

---

## N. Legacy Tests — mod03 (TypeScript)

### N1. Rename TS mod03 test methods to match Java
- Files: `system-test/typescript/tests/legacy/mod03/e2e/place-order-positive-api-test.spec.ts`, `place-order-positive-ui-test.spec.ts`.
- Rename `shouldPlaceOrder` → `shouldPlaceOrderForValidInput`.

### N2. TypeScript — restore full API positive assertions
- File: `system-test/typescript/tests/legacy/mod03/e2e/place-order-positive-api-test.spec.ts`.
- After `placeOrder`, issue a raw `fetch` view-order call and assert `orderNumber`, `sku`, `quantity=5`, `unitPrice=20.00`, `basePrice=100.00`, `totalPrice>0`, `status='PLACED'`.

### N3. TypeScript — restore full UI positive flow
- File: `system-test/typescript/tests/legacy/mod03/e2e/place-order-positive-ui-test.spec.ts`.
- After placing order, navigate to `/order-history`, filter by orderNumber, click View Details, assert orderNumber/sku/quantity/unitPrice/basePrice/totalPrice/status on details page. Generate a unique SKU instead of using `DEFAULT-SKU`.

### N4. TypeScript — fix UI negative test selector assumptions
- File: `system-test/typescript/tests/legacy/mod03/e2e/place-order-negative-ui-test.spec.ts`.
- Align assertions to Java/.NET: a single error alert containing the validation message, "quantity" field, and "Quantity must be an integer". Use a single text match rather than `.error-message` + `.field-error` split, to match the UI that Java/.NET test against.

### N5. Already covered by A7 — remove WireMock setup

---

## O. Legacy Tests — mod04 (TypeScript)

### O1. Rename TS mod04 positive tests to match Java method name
- Files: `system-test/typescript/tests/legacy/mod04/e2e/place-order-positive-api-test.spec.ts`, `place-order-positive-ui-test.spec.ts`.
- Rename `shouldPlaceOrder` → `shouldPlaceOrderForValidInput`.

### O2. TypeScript — restore API positive full assertions via viewOrder
- File: `place-order-positive-api-test.spec.ts`.
- After `shopApiClient.placeOrder(...)`, call `shopApiClient.orders().viewOrder(orderNumber)` (post-B1 refactor), assert orderNumber/sku/quantity=5/unitPrice=20.00/totalPrice>0/status=PLACED.

### O3. TypeScript — restore UI positive full flow via ShopUiClient
- File: `place-order-positive-ui-test.spec.ts`.
- After A1 (ShopUiClient added), rewrite to use `shopUiClient.openHomePage().clickNewOrder().inputSku(sku).inputQuantity("5").inputCountry("US").clickPlaceOrder().getResult()`, then `openHomePage().clickOrderHistory().inputOrderNumber().clickSearch().clickViewOrderDetails(orderNumber)` and assert each field.

### O4. TypeScript — negative tests: replace `'3.5'` with `"invalid-quantity"`
- Files: `place-order-negative-api-test.spec.ts`, `place-order-negative-ui-test.spec.ts`.
- Match Java/.NET data.

---

## P. Legacy Tests — mod05

### P1. .NET — align `PlaceOrderNegativeBaseTest` parameterization with Java
- File: `system-test/dotnet/SystemTests/Legacy/Mod05/E2eTests/PlaceOrderNegativeBaseTest.cs`.
- Current: `[InlineData("3.5"), InlineData("lala")]` — two cases.
- Java: single case `"3.5"`.
- **Recommended**: remove `[InlineData("lala")]` so .NET matches Java. Alternatively, add `"lala"` to Java if both languages should have both cases; but Java is the reference.

### P2. TypeScript — restore full positive assertions in mod05
- Files: `system-test/typescript/tests/legacy/mod05/e2e/place-order-positive-api-test.spec.ts`, `place-order-positive-ui-test.spec.ts`.
- After A3 (external drivers switched to Real + extra `taxDriver.returnsTaxRate` call removed), add assertions on `orderNumber`, `sku`, `unitPrice`, `totalPrice > 0`, `status === 'PLACED'`.
- Use a unique SKU in UI spec (not `DEFAULT-SKU`).

### P3. TypeScript — add `ShopBaseSmokeTest` abstraction (optional)
- Currently TS mod05 duplicates the smoke body across api/ui spec files. Consider extracting a helper. **Recommended** lower priority; only after core mismatches are fixed.

---

## Q. Legacy Tests — mod06

### Q1. TypeScript — restore full positive assertions in mod06
- File: `system-test/typescript/tests/legacy/mod06/e2e/place-order-positive-test.spec.ts`.
- Remove `taxDriver.returnsTaxRate(...)` extra step.
- Add assertions on `orderNumber`, `sku`, `quantity=5`, `unitPrice=20.00`, `totalPrice>0`, `status==='PLACED'`.

---

## R. Legacy Tests — mod07

Covered under A4.

### R1. TypeScript — restore full mod07 positive assertions
- File: `system-test/typescript/tests/legacy/mod07/e2e/place-order-positive-test.spec.ts`.
- After A4 (fluent builder introduced), assert `orderNumber(ORDER_NUMBER)`, `orderNumberStartsWith("ORD-")`, `sku`, `quantity`, `unitPrice(20.00)`, `status(PLACED)`, `totalPriceGreaterThanZero()` exactly like `system-test/java/.../legacy/mod07/e2e/PlaceOrderPositiveTest.java`.
- Remove `useCase.tax().returnsTaxRate(...)` extra step.

---

## S. Legacy Tests — mod08

Covered under A8.

---

## T. Legacy Tests — mod09

No changes required.

---

## U. Legacy Tests — mod10

### U1. .NET — add `ShouldRejectOrderWithNonPositiveQuantity` to mod10 acceptance
- File: `system-test/dotnet/SystemTests/Legacy/Mod10/AcceptanceTests/PlaceOrderNegativeTest.cs`.
- Add method parameterized over `"-10"`, `"-1"`, `"0"` asserting field error `quantity / Quantity must be positive`.
- Reference: `system-test/java/.../legacy/mod10/acceptance/PlaceOrderNegativeTest.java` lines with `@ValueSource(strings = {"-10", "-1", "0"})`.

### U2. TypeScript — add missing `.and().clock().withWeekday()` step
- File: `system-test/typescript/tests/legacy/mod10/acceptance/place-order-positive-isolated-test.spec.ts`.
- In `shouldApplyFullPriceOnWeekday`, insert `.and().clock().withWeekday()` before `.when().placeOrder(...)`. Match `system-test/java/.../legacy/mod10/acceptance/PlaceOrderPositiveIsolatedTest.java`.

---

## V. Legacy Tests — mod11

Covered under A6 (base classes), J1 (clock stub body).

### V1. TypeScript — mod11 contract clock-stub body alignment
- File: `system-test/typescript/tests/legacy/mod11/contract/clock/clock-stub-contract-test.spec.ts`.
- Remove `.clock().withTime()` no-arg call; align to Java's `.given().then().clock().hasTime()`.

---

## W. Summary of priorities

1. **Section A** — resolve architectural mismatches (A1–A8). Without these, the per-module pedagogical layering is broken in TS.
2. **Sections B, C, E, F, G, H** — architecture layers alignment; start with Clients (B), then Drivers (C), then Use Case DSL (E), then Scenario DSL (F), then Common (G), then Driver Ports (H). **Recommended** order: B → C → D (no-op) → E → F → G → H.
3. **Section I / J** — latest test body alignment (acceptance I1–I4, contract J1–J2).
4. **Sections N → V** — legacy test alignment per module, mod03 → mod11.
5. **Sections U / V / P / N / J1 / F1 / F8** — small .NET-specific fixes (add NonPositive test, remove extra inline data, remove WhenGoToShop, remove ScenarioDslFactory).

Java remains unchanged throughout (reference).
