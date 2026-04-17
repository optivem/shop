# TypeScript — System Test Alignment Plan

Reference report: `reports/20260417-1436-compare-tests-both.md`

Reference implementation: **Java**. Each task aligns TypeScript to Java unless noted.

Ordering: architectural mismatches first, then architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common → ports), then tests (acceptance → contract → e2e → smoke).

**Porting legend** (based on audit of `eshop-tests/typescript/`):
- ✅ **Port from eshop-tests** — target file/folder already exists in eshop-tests with the target layered architecture; copy over with minimal adjustment.
- 🟡 **Partial** — some pieces exist in eshop-tests, others need to be created or relocated.
- ✏️ **Net-new** — not present in eshop-tests; typically legacy-mod progression work or starter-specific test-body tweaks that eshop-tests doesn't cover.

---

## A. Architectural Mismatches (Legacy) — Highest Priority

### A1. TypeScript — mod04 UI: introduce a `ShopUiClient` page-object client
- Files: `system-test/typescript/src/testkit/driver/adapter/shop/ui/` — create `client/ShopUiClient.ts` + page objects `HomePage.ts`, `NewOrderPage.ts`, `OrderHistoryPage.ts`, `OrderDetailsPage.ts`, `CouponManagementPage.ts`, `BasePage.ts`.
- Rewrite `system-test/typescript/tests/legacy/mod04/e2e/place-order-positive-ui-test.spec.ts` and `place-order-negative-ui-test.spec.ts` to use the new UI client.
- Update `system-test/typescript/tests/legacy/mod04/e2e/fixtures.ts` to provide a `shopUiClient` fixture, replacing the raw `shopPage` usage for mod04.
- Reference: `system-test/java/src/main/java/com/optivem/shop/testkit/driver/adapter/shop/ui/client/ShopUiClient.java` and page classes.
- **Source:** 🟡 Partial — ShopUiClient + all page objects (`BasePage`, `HomePage`, `NewOrderPage`, `OrderHistoryPage`, `OrderDetailsPage`, `CouponManagementPage`) already exist at `eshop-tests/typescript/driver-adapter/shop/ui/client/` and `.../client/pages/` (✅ port the client + pages); mod04 `place-order-positive-ui-test.spec.ts` / `place-order-negative-ui-test.spec.ts` rewrites and `fixtures.ts` update are starter-specific (✏️ net-new).

### A2. ✅ DONE (commit e2b660e): TypeScript — mod04 External systems: switch to `ErpRealClient` / `TaxRealClient`
- mod04 e2e fixtures now use `ErpRealClient` (tax client dropped — mod04 positive spec no longer configures tax, matching Java/.NET).
- mod04 smoke fixtures use `ErpRealClient` + `TaxRealClient`.
- Positive spec calls `erpClient.createProduct(...)`; `configureProduct` / `configureTaxRate` stub-style calls removed.
- `EXTERNAL_SYSTEM_MODE` default flipped to `'real'`.

### A4. 🟡 PARTIAL (commit 42ced1d): TypeScript — mod07: introduce a fluent use-case DSL with step builders
- **Remaining:** Fixtures now use Real drivers (infrastructure done), but the fluent use-case DSL files still need to be created and the mod07 e2e spec bodies still use the imperative `useCase.erp().returnsProduct(...)` / `useCase.shop().placeOrder(...)` style and still contain the `useCase.tax().returnsTaxRate(...)` extra step — not yet converted to the `shop().placeOrder().sku().quantity().country().execute()` builder chain.
- File: `system-test/typescript/src/testkit/dsl/core/usecase/shop/ShopDsl.ts` and related DSL files.
- Implement fluent builders for each use case: `shop().placeOrder().sku().quantity().country().execute()`, `shop().viewOrder().orderNumber().execute()`, `erp().returnsProduct().sku().unitPrice().execute()`, etc. Result types expose `shouldSucceed()`, `shouldFail()`, `orderNumber()`, `orderNumberStartsWith()`, `sku()`, `quantity()`, `unitPrice()`, `status()`, `totalPriceGreaterThanZero()`, `errorMessage()`, `fieldErrorMessage()`.
- Rewrite `system-test/typescript/tests/legacy/mod07/e2e/place-order-positive-test.spec.ts` and `place-order-negative-test.spec.ts` to use the builder chain identical to `system-test/java/src/test/java/com/optivem/shop/systemtest/legacy/mod07/e2e/PlaceOrderPositiveTest.java`.
- Remove `useCase.tax().returnsTaxRate(...)` extra step.
- **Source:** 🟡 Partial — the fluent use-case DSL (`ShopDsl.ts`, `ClockDsl.ts`, `ErpDsl.ts`, `TaxDsl.ts` + per-use-case files `PlaceOrder.ts`, `ViewOrder.ts`, `ReturnsProduct.ts`, etc.) already exists at `eshop-tests/typescript/dsl-core/usecase/` (✅ port the DSL); the starter's legacy mod07 test rewrites are net-new (✏️ eshop-tests has mod07 tests but with different naming and no starter-specific `returnsTaxRate` regression).

### A7. ✅ DONE (commit d57c5b3): TypeScript — mod03 WireMock stubbing is a layering leak
- mod03 positive api/ui specs now POST to real ERP `/api/products` instead of `/__admin/mappings`.
- mod03 fixtures default `EXTERNAL_SYSTEM_MODE` to `'real'`.
- Negative specs untouched (they don't contain WireMock admin calls — verify separately if new regressions appear).

---

## B. Architecture Layers — Clients

### B4. TypeScript — expose external client DTOs
- Files (new): `system-test/typescript/src/testkit/driver/adapter/external/erp/client/dtos/ExtCreateProductRequest.ts`, `ExtProductDetailsResponse.ts`, `ExtGetPromotionResponse.ts`, `error/ExtErpErrorResponse.ts`; `.../clock/client/dtos/ExtGetTimeResponse.ts`, `error/ExtClockErrorResponse.ts`; `.../tax/client/dtos/ExtGetCountryResponse.ts`, `error/ExtTaxErrorResponse.ts`.
- Replace anonymous inline types in the client code with these explicit DTO types.
- **Source:** 🟡 Partial — `ExtCreateProductRequest.ts`, `ExtProductDetailsResponse.ts`, `ExtErpErrorResponse.ts`, `ExtGetTimeResponse.ts`, `ExtClockErrorResponse.ts`, `ExtTaxErrorResponse.ts` all exist at `eshop-tests/typescript/driver-adapter/external/{erp,clock,tax}/client/dtos/...` (✅ port); `ExtGetPromotionResponse.ts` is missing (✏️ net-new) and tax has `ExtCountryDetailsResponse.ts` instead of `ExtGetCountryResponse.ts` (rename + adapt).

### B7. TypeScript — add `SystemErrorMapper`
- File (new): `system-test/typescript/src/testkit/driver/adapter/shop/api/SystemErrorMapper.ts`.
- Reference: Java `SystemErrorMapper.java`.
- **Source:** ✏️ Net-new — `SystemErrorMapper` not present anywhere in `eshop-tests/typescript/` (grep finds zero hits).

---

## D. Architecture Layers — Channels

No changes required (aligned across all three languages).

---

## E. Architecture Layers — Use Case DSL

### E1. TypeScript — decompose use-case DSL into per-use-case files
- Create directory trees matching Java: `system-test/typescript/src/testkit/dsl/core/usecase/shop/usecases/` containing `PlaceOrder.ts`, `PlaceOrderVerification.ts`, `CancelOrder.ts`, `ViewOrder.ts`, `ViewOrderVerification.ts`, `BrowseCoupons.ts`, `BrowseCouponsVerification.ts`, `PublishCoupon.ts`, `DeliverOrder.ts`, `GoToShop.ts`, `base/BaseShopUseCase.ts`, `SystemResults.ts`.
- Do the same for `external/clock/usecases/`, `external/erp/usecases/`, `external/tax/usecases/`.
- Refactor `ShopDsl.ts`, `ClockDsl.ts`, `ErpDsl.ts`, `TaxDsl.ts` to wire the new per-use-case classes instead of inlining logic.
- **Source:** 🟡 Partial — `ShopDsl.ts`, `ClockDsl.ts`, `ErpDsl.ts`, `TaxDsl.ts` and `shop/usecases/{PlaceOrder, PlaceOrderVerification, CancelOrder, ViewOrder, ViewOrderVerification, BrowseCoupons, BrowseCouponsVerification, PublishCoupon, GoToShop}.ts` and the external usecases (`GetTime`, `ReturnsTime`, `GetProduct`, `ReturnsProduct`, `GoToErp`, `GetTaxRate`, `ReturnsTaxRate`, `GoToTax`, `GoToClock`) all exist at `eshop-tests/typescript/dsl-core/usecase/` (✅ port). Missing/different: `DeliverOrder.ts` (see E2, ✏️ net-new), `SystemResults.ts` (absent — see H5, ✏️ net-new), `base/BaseShopUseCase.ts` per-system base naming (eshop-tests uses `BaseShopCommand.ts` / `BaseClockCommand.ts` etc., see E3 — rename needed).

### E2. TypeScript — add `DeliverOrder` use case to the DSL
- File (new): `system-test/typescript/src/testkit/dsl/core/usecase/shop/usecases/DeliverOrder.ts`.
- Wire into `ShopDsl.ts`.
- Reference: Java `DeliverOrder.java`, .NET `DeliverOrder.cs`.
- **Source:** ✏️ Net-new — `DeliverOrder` does not exist anywhere in `eshop-tests/typescript/` (grep finds zero hits).

### E3. TypeScript — add per-system `Base*UseCase` classes
- Files: `.../shop/usecases/base/BaseShopUseCase.ts`, `.../external/clock/usecases/base/BaseClockUseCase.ts`, `.../external/erp/usecases/base/BaseErpUseCase.ts`, `.../external/tax/usecases/base/BaseTaxUseCase.ts`.
- **Source:** 🟡 Partial — analogous classes exist in eshop-tests but under different names: `BaseShopCommand.ts`, `BaseClockCommand.ts`, `BaseErpCommand.ts`, `BaseTaxCommand.ts` (all under `dsl-core/usecase/*/usecases/base/`). Port the bodies; rename `*Command` → `*UseCase` to match Java/.NET.

---

## F. Architecture Layers — Scenario DSL

### F3. TypeScript — add missing Then* at both port and core
- At the core: add `system-test/typescript/src/testkit/dsl/core/scenario/then/ThenClock.ts`, `ThenCountry.ts`, `ThenProduct.ts`.
- At the port: add `system-test/typescript/src/testkit/dsl/port/then/steps/then-clock.ts`, `then-country.ts`, `then-product.ts` as named (currently TS has `then-given-*` variants which is semantically different).
- Decision on the TS per-use-case files (`then-place-order.ts`, `then-cancel-order.ts`, `then-publish-coupon.ts`, `then-view-order.ts`, `then-browse-coupons.ts`, `then-contract.ts`): align with Java by removing the per-use-case decomposition and relying on entity-level Then steps.
- **Source:** ✏️ Net-new — eshop-tests' `dsl-core/scenario/then/` has `ThenGivenClock.ts`, `ThenGivenCountry.ts`, `ThenGivenProduct.ts` (the same "wrong" given-prefixed naming as starter, not the target entity-only naming) plus `ThenFailure*`/`ThenSuccess*` variants that the plan wants removed. No `ThenClock.ts`, `ThenCountry.ts`, `ThenProduct.ts` exist there; eshop-tests' port layer also lacks `ThenStep` bases.

### F5. TypeScript — add `WhenStep` port base and `GivenStep`/`ThenStep` bases
- Files: `system-test/typescript/src/testkit/dsl/port/when/steps/base/when-step.ts`, `.../given/steps/base/given-step.ts`, `.../then/steps/base/then-step.ts`.
- **Source:** ✏️ Net-new — eshop-tests' `dsl-port/scenario/` has only `*StagePort`/`*ResultPort` files; no `WhenStep`/`GivenStep`/`ThenStep` port bases.

### F7. TypeScript — add shared DSL verification classes
- Files (new): `system-test/typescript/src/testkit/dsl/core/shared/base-use-case.ts`, `use-case-result.ts`, `error-verification.ts`, `response-verification.ts`, `void-verification.ts`.
- Reference: Java `BaseUseCase.java`, `UseCaseResult.java`, `ErrorVerification.java`, `ResponseVerification.java`, `VoidVerification.java`.
- **Source:** 🟡 Partial — `eshop-tests/typescript/dsl-core/shared/` has `BaseUseCase.ts`, `UseCaseResult.ts`, `ResponseVerification.ts`, `VoidVerification.ts` (✅ port). Missing: `error-verification.ts` — eshop-tests instead has per-system error verifications (`ClockErrorVerification.ts`, `ErpErrorVerification.ts`, `TaxErrorVerification.ts`) under each `*/usecases/base/` (✏️ consolidate into a shared one).

---

## G. Architecture Layers — Common

### G1. TypeScript — add `Closer` utility — ❌ EXCEPTION (TS-specific)
- **Decision:** Not ported. Java wraps `AutoCloseable`; TS has no equivalent abstraction. Only `ErpStubClient`/`TaxStubClient` have a `close()` method and callers invoke it directly — no utility needed. The plan itself acknowledged: "JS has native dispose semantics now; consider whether this is still needed."

---

## H. Architecture Layers — Driver Ports

### H3. TypeScript — add `GetCountryRequest`
- File (new): `system-test/typescript/src/testkit/driver/port/external/tax/dtos/GetCountryRequest.ts`.
- **Source:** ✏️ Net-new — `GetCountryRequest` not present in `eshop-tests/typescript/driver-port/external/tax/dtos/` (only `GetTaxResponse.ts`, `ReturnsTaxRateRequest.ts`).

### H5. TypeScript — add `SystemResults`
- File (new): `system-test/typescript/src/testkit/dsl/core/usecase/shop/commons/system-results.ts` (matching Java placement).
- Reference: Java `SystemResults.java`.
- **Source:** 🟡 Partial — `SystemResults.ts` exists in eshop-tests under `driver-adapter/shop/commons/SystemResults.ts` (adapter-layer, not dsl/core commons). Content can be ported; needs relocation to match Java placement.

---

## I. Latest Tests — Acceptance

### I1. TypeScript — split `shouldRejectOrderWithNonPositiveQuantity` or adjust Java/.NET
- Current TS: parameterized `['-10', '-1', '0']` in one test `shouldRejectOrderWithNonPositiveQuantity`.
- Current Java/.NET: two separate tests `shouldRejectOrderWithNegativeQuantity`(-10) and `shouldRejectOrderWithZeroQuantity`(0).
- **Recommended**: align TS to Java/.NET by splitting into two non-parameterized tests; drop the `-1` case to match. Rationale: Java is the reference.
- Files: `system-test/typescript/tests/latest/acceptance/place-order-negative-test.spec.ts`.
- **Source:** ✏️ Net-new — starter-specific test-body tweak; eshop-tests' `latest/acceptance/PlaceOrderNegative.spec.ts` uses different naming and doesn't share the starter's parameterization regression.

### I2. TypeScript — add `@TimeDependent`/`[Time]` equivalent marker
- Files: `system-test/typescript/tests/latest/acceptance/place-order-negative-isolated-test.spec.ts` (for `cannotPlaceOrderWithExpiredCoupon`), `cancel-order-positive-isolated-test.spec.ts`, `cancel-order-negative-isolated-test.spec.ts`.
- Add a TypeScript-idiomatic equivalent (a tag or hook) and document the mapping in the TS testing helpers package (`@optivem/optivem-testing`).
- **Source:** ✏️ Net-new — no `TimeDependent`/`[Time]` marker equivalent exists in `eshop-tests/typescript/` (grep finds zero hits).

### I3. TypeScript — ensure ViewOrderNegativeTest exercises UI for first row
- File: `system-test/typescript/tests/latest/acceptance/view-order-negative-test.spec.ts`.
- Change from `forChannels(ChannelType.API)` to `test.eachAlsoFirstRow(nonExistentOrderCases)` or equivalent Channel helper so the first row also runs via UI, matching Java `alsoForFirstRow = ChannelType.UI` and .NET `AlsoForFirstRow = new[] { ChannelType.UI }`.
- **Source:** ✏️ Net-new — `alsoForFirstRow`/`eachAlsoFirstRow` pattern does not exist anywhere in `eshop-tests/typescript/` (grep finds zero hits); requires extending the TS channel helper.

### I4. TypeScript/Java/.NET — align PublishCouponNegativeTest discount-rate value types — ⏳ Deferred
- Java/.NET pass strings (`"0.0"`, `"-0.01"`); TS passes numbers.
- **Deferred to Phase 3c:** the core `WhenPublishCoupon.withDiscountRate` signature is `number` (and the `PublishCouponRequest.discountRate` DTO is `number`). Aligning requires extending the DSL + DTO to accept `number | string` and parsing at the serialization boundary — bundled with other DSL work in Phase 3c.

---

## J. Latest Tests — Contract

(All J items completed in Phase 2 + Phase 3a.)

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

All tasks resolved (N1, N2, N3 rem, N4 in Phase 3a; N5 covered by A7).

---

## O. Legacy Tests — mod04 (TypeScript)

### O2. TypeScript — restore API positive full assertions via viewOrder
- File: `place-order-positive-api-test.spec.ts`.
- After `shopApiClient.placeOrder(...)`, call `shopApiClient.orders().viewOrder(orderNumber)` (post-B1 refactor), assert orderNumber/sku/quantity=5/unitPrice=20.00/totalPrice>0/status=PLACED.
- **Source:** ✏️ Net-new — starter-specific legacy-mod04 test-body restoration (depends on B1 port).

### O3. TypeScript — restore UI positive full flow via ShopUiClient
- File: `place-order-positive-ui-test.spec.ts`.
- After A1 (ShopUiClient added), rewrite to use `shopUiClient.openHomePage().clickNewOrder().inputSku(sku).inputQuantity("5").inputCountry("US").clickPlaceOrder().getResult()`, then `openHomePage().clickOrderHistory().inputOrderNumber().clickSearch().clickViewOrderDetails(orderNumber)` and assert each field.
- **Source:** ✏️ Net-new — starter-specific legacy-mod04 UI test rewrite (depends on A1/B2 ShopUiClient port).

(O1, O4 completed in Phase 3a.)

---

## P. Legacy Tests — mod05

All tasks resolved (P2 in Phase 3a — sku assertion added; A3 remaining tax step removed in Phase 2).

---

## Q. Legacy Tests — mod06

All tasks resolved (Q1 in Phase 3a — full assertions added; A3 remaining tax step removed in Phase 2).

---

## R. Legacy Tests — mod07

Covered under A4.

### R1. 🟡 PARTIAL (commit 42ced1d): TypeScript — restore full mod07 positive assertions
- **Remaining:** Fixture now injects Real drivers (prerequisite infrastructure done), but the test body still uses imperative style, still contains the `useCase.tax().returnsTaxRate(...)` extra step, and lacks the full assertions (`orderNumber(ORDER_NUMBER)`, `orderNumberStartsWith("ORD-")`, `unitPrice(20.00)`, `totalPriceGreaterThanZero()`, etc.) required by A4/R1.
- File: `system-test/typescript/tests/legacy/mod07/e2e/place-order-positive-test.spec.ts`.
- After A4 (fluent builder introduced), assert `orderNumber(ORDER_NUMBER)`, `orderNumberStartsWith("ORD-")`, `sku`, `quantity`, `unitPrice(20.00)`, `status(PLACED)`, `totalPriceGreaterThanZero()` exactly like `system-test/java/.../legacy/mod07/e2e/PlaceOrderPositiveTest.java`.
- Remove `useCase.tax().returnsTaxRate(...)` extra step.
- **Source:** ✏️ Net-new — starter-specific legacy-mod07 test-body restoration (depends on A4/E1 DSL port).

---

## S. Legacy Tests — mod08

Covered under A8.

---

## T. Legacy Tests — mod09

No changes required.

---

## U. Legacy Tests — mod10

### U2. TypeScript — add missing `.and().clock().withWeekday()` step
- File: `system-test/typescript/tests/legacy/mod10/acceptance/place-order-positive-isolated-test.spec.ts`.
- In `shouldApplyFullPriceOnWeekday`, insert `.and().clock().withWeekday()` before `.when().placeOrder(...)`. Match `system-test/java/.../legacy/mod10/acceptance/PlaceOrderPositiveIsolatedTest.java`.
- **Source:** ✏️ Net-new — `withWeekday`/`shouldApplyFullPriceOnWeekday` not present in `eshop-tests/typescript/` (grep finds zero hits); also requires extending `GivenClock` DSL step.

---

## V. Legacy Tests — mod11

All tasks resolved in Phase 2 (commit dc85f61).

---

## Local verification & commit

- From `system-test/typescript/`, run `Run-SystemTests -Architecture monolith` (latest suite) and `Run-SystemTests -Architecture monolith -Legacy` (legacy suite). Do not substitute `npm test`, `npx playwright test`, or bare `docker compose` — `Run-SystemTests.ps1` is the only supported entry point because it manages containers and config.
- Fix any failures before moving on.
- Commit TS changes as one logical commit (or a small series split along the natural boundaries: A. architectural mismatches → B/C/E/F/G/H. architecture-layer ports → I/J. latest test alignment → N–V. legacy-mod test-body restoration).

---

## W. Summary of priorities (TypeScript-relevant)

1. **Section A** — resolve architectural mismatches (A1–A8). Without these, the per-module pedagogical layering is broken in TS.
2. **Sections B, C, E, F, G, H** — architecture layers alignment; start with Clients (B), then Drivers (C), then Use Case DSL (E), then Scenario DSL (F), then Common (G), then Driver Ports (H). **Recommended** order: B → C → D (no-op) → E → F → G → H.
3. **Section I / J** — latest test body alignment (acceptance I1–I4, contract J1–J2).
4. **Sections N → V** — legacy test alignment per module, mod03 → mod11.

Java remains unchanged throughout (reference).

---

## X. TypeScript Porting Summary (from eshop-tests audit)

- **Total TS tasks audited:** 51
- ✅ **Fully port from eshop-tests:** 13 — B1, B2, B3, B5, B6, B9, C1, F4, G1, G2, G3, H1, P3
- 🟡 **Partial (port + adapt):** 8 — A1, A4, A5, B4, E1, E3, F7, H5
- ✏️ **Net-new:** 30 — A2, A3, A6, A7, A8, B7, E2, F3, F5, H3, H4, I1, I2, I3, I4, J1, J2, N1, N2, N3, N4, O1, O2, O3, O4, P2, Q1, R1, U2, V1

**Implication:** 21 of 51 TS tasks (13 ✅ + 8 🟡) can be fully or largely satisfied by a port from `eshop-tests/typescript/` — consider a single bulk port commit for the layered architecture (sections B, C, E, F, G, H) before tackling the 30 net-new items (mostly legacy-mod test-body tweaks in sections A2–A8, N–V, and a handful of latest test-body fixes in I/J).

---

## Y. Interim commit reconciliation (TypeScript)

Four TypeScript commits landed after this plan was generated. Their impact on plan tasks:

- **f5ef50c** — "Fix TS system tests: mod03 type casts, mod04-07 smoke/external rewrites"
  - mod03 positive/negative API specs: only added TypeScript type casts on `response.json()` return values (dev-ergonomics fix). Does NOT address **A7** (WireMock leak removal) at this point.
  - mod04 smoke `fixtures.ts` + `erp-smoke-test.spec.ts` / `tax-smoke-test.spec.ts`: added `erpClient`/`taxClient` fixtures using `ErpStubClient`/`TaxStubClient` and rewrote the smoke specs to call `checkHealth()`. Does NOT address **A2** (which requires `ErpRealClient`/`TaxRealClient`) at this point.
  - mod05 / mod06 smoke `fixtures.ts` + smoke external specs: added `erpDriver`/`taxDriver` fixtures using `ErpStubDriver`/`TaxStubDriver` and rewrote the smoke specs to call `goToErp()`/`goToTax()`. Does NOT address **A3** at this point.
  - mod07 smoke external specs: rewritten to use `useCase.erp().goToErp()` / `useCase.tax().goToTax()` (unrelated to the e2e fluent DSL of A4).
  - No plan task fully resolved by f5ef50c. (Later superseded for mod04 smoke by e2b660e which switched those fixtures to Real clients.)

- **42ced1d** — "Fix mod07 fixtures: use Real drivers (stubs not introduced until mod09)"
  - mod07 e2e/smoke `fixtures.ts`: swapped `ErpStubDriver`/`TaxStubDriver`/`ClockStubDriver` for `ErpRealDriver`/`TaxRealDriver`/`ClockRealDriver` and removed the `EXTERNAL_SYSTEM_MODE=stub` default.
  - Partially resolves **A4** and **R1** (infrastructure/fixture-level prerequisite for Real external systems is now in place). Test-body rewrite, fluent builder DSL, and `returnsTaxRate` removal still pending.

- **d57c5b3** — "Fix legacy/mod03/e2e TS tests: run against real ERP (not WireMock admin)"
  - mod03 e2e `fixtures.ts`: default `EXTERNAL_SYSTEM_MODE` flipped to `'real'`.
  - mod03 positive api/ui specs: `/__admin/mappings` calls replaced with raw POST to `/api/products` on the real ERP; UI spec now uses a random UUID SKU instead of `DEFAULT-SKU`.
  - **Fully resolves A7** (WireMock layering leak removed).
  - **Partially resolves N3** (unique SKU done; order-history view-details flow still pending).
  - **N2** unchanged — positive api spec still lacks the raw `fetch` view-order call and full-field assertions.

- **e2b660e** — "Align TS mod04-07 legacy e2e/smoke with Java/.NET: switch fixtures to Real drivers/clients"
  - mod04 e2e + smoke `fixtures.ts`: `ErpStubClient`/`TaxStubClient` → `ErpRealClient` (tax client dropped in e2e; both Real in smoke); default `EXTERNAL_SYSTEM_MODE='real'`; positive specs call `erpClient.createProduct(...)`.
  - mod05 e2e + smoke `fixtures.ts`: `ErpStubDriver`/`TaxStubDriver` → `ErpRealDriver`/`TaxRealDriver`; UI positive spec now uses unique SKU.
  - mod06 e2e + smoke `fixtures.ts`: `ErpStubDriver`/`TaxStubDriver` → `ErpRealDriver`/`TaxRealDriver`.
  - **Fully resolves A2** (mod04 Real clients + stub-style calls removed).
  - **Partially resolves A3** (mod05/06 fixtures switched to Real drivers; the `taxDriver.returnsTaxRate(...)` extra step is still present in mod05 e2e api/ui and mod06 e2e positive specs and must be removed).
  - **Partially resolves P2** (mod05 api + ui positive specs already have full viewOrder assertions: `orderNumber~/^ORD-/`, `quantity=5`, `unitPrice=20`, `status='PLACED'`, `totalPrice>0`; unique SKU in both; blocked on A3 remaining work to remove the tax step).
  - **Partially resolves Q1** (mod06 e2e positive spec has `orderNumber~/^ORD-/` + `status='PLACED'` via viewOrder; still missing `sku`/`quantity`/`unitPrice`/`totalPrice` assertions and the tax step removal).
  - **O2, O3, O4** unchanged — mod04 api still lacks viewOrder assertions; mod04 UI still uses raw `shopPage.locator(...)` (no `ShopUiClient`); mod04 negative specs not touched.

- **a14a51b** — "Port architecture layers from eshop-tests to TS system-test (Phase 1)"
  - Structural alignment of TypeScript testkit to Java reference, keeping starter's idioms (raw fetch, functional `Result<T,E>`, no axios/class-Result/decimal.js stack from eshop-tests).
  - **Fully resolves B1, B2, B3, B5, B6, B9, C1, F4, G2, G3, H1, P3** (all ✅ entries from section X).
  - **G1 marked as EXCEPTION** — TS has no `AutoCloseable` equivalent; only stub clients have `close()` and callers invoke directly.
  - Local verification: full latest + full legacy suite run on monolith, both green.

- **dc85f61** — "Align TS legacy tests with Java: mod02 BaseRawTest, mod05/06/08 test body, mod11+latest contract base classes (Phase 2)"
  - **A3:** removed stray `taxDriver.returnsTaxRate(...)` calls from mod05 e2e api/ui + mod06 e2e positive specs.
  - **A5:** added `tests/legacy/mod02/base/BaseRawTest.ts` helper module (config getters, `createUniqueSku`, browser setup/teardown); rewired all 4 mod02 smoke specs.
  - **A6:** added `BaseExternalSystemContractTest.ts` + `BaseClockContractTest.ts` + `BaseErpContractTest.ts` (and `BaseTaxContractTest.ts` for latest); thin-wrappered all 10 contract spec files (mod11 clock-real/stub, erp-real/stub + latest clock-real/stub, erp-real/stub, tax-real/stub).
  - **A8:** pruned mod08 negative spec to single `shouldRejectOrderWithNonIntegerQuantity('3.5')` test.
  - **J1, V1:** stray `.clock().withTime()` removed from clock-stub bodies (resolved as side-effect of A6).
  - Local verification: full latest + full legacy suite run on monolith, both green.

- **c8229de** — "Align TS test bodies with Java (Phase 3a)"
  - **N1, O1:** renamed `shouldPlaceOrder` → `shouldPlaceOrderForValidInput` in mod03 + mod04 positive specs (api + ui).
  - **N2:** mod03 api positive spec now asserts full order details via raw fetch viewOrder (orderNumber, sku, quantity=5, unitPrice=20, basePrice=100, totalPrice>0, status=PLACED).
  - **N3 rem:** mod03 ui positive spec now navigates to order-history, filters by order number, clicks View Details, asserts all fields.
  - **N4:** mod03 ui negative spec switched to single text match on `[role='alert'][data-notification-id]`, matching Java.
  - **O4:** mod04 negative quantity data `'3.5'` → `'invalid-quantity'`.
  - **I1:** latest acceptance parameterized `shouldRejectOrderWithNonPositiveQuantity` split into `shouldRejectOrderWithNegativeQuantity('-10')` + `shouldRejectOrderWithZeroQuantity('0')` (matches Java).
  - **J2:** `BaseTaxContractTest.withTaxRate('0.09')` → `0.09` (numeric, matches Java double).
  - **P2 rem:** mod05 e2e api + ui positive specs now also assert `sku`.
  - **Q1 rem:** mod06 e2e positive spec now also asserts `sku`, `quantity`, `unitPrice`, `totalPrice`.
  - Local verification: full latest + full legacy suite on monolith, both green.

**Net result so far:**
- ✅ DONE: **A2** (e2b660e), **A3 A5 A6 A8 J1 V1** (dc85f61), **A7** (d57c5b3), **B1 B2 B3 B5 B6 B9 C1 F4 G2 G3 H1 P3** (a14a51b), **I1 J2 N1 N2 N3 N4 O1 O4 P2 Q1** (c8229de).
- ❌ EXCEPTION: **G1** (a14a51b — not ported; TS-specific).
- ⏳ DEFERRED: **I4** (DSL signature change needed), **U2** (GivenClock `withWeekday` step needed).
- 🟡 PARTIAL: **A4** (42ced1d), **R1** (42ced1d).
- ⏸ NOT STARTED: **A1, B4, B7, E1, E2, E3, F3, F5, F7, H3, H4, H5, I2, I3, O2, O3.**
