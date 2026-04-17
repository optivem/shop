# TypeScript — System Test Alignment Plan

Reference report: `reports/20260417-1436-compare-tests-both.md`

Reference implementation: **Java**. Each task aligns TypeScript to Java unless noted.

Ordering: architectural mismatches first, then architecture layers (clients → drivers → channels → use-case DSL → scenario DSL → common → ports), then tests (acceptance → contract → e2e → smoke).

**Porting legend** (based on audit of `eshop-tests/typescript/`):
- ✅ **Port from eshop-tests** — target file/folder already exists in eshop-tests with the target layered architecture; copy over with minimal adjustment.
- 🟡 **Partial** — some pieces exist in eshop-tests, others need to be created or relocated.
- ✏️ **Net-new** — not present in eshop-tests; typically legacy-mod progression work or shop-specific test-body tweaks that eshop-tests doesn't cover.

---

## A. Architectural Mismatches (Legacy) — Highest Priority

### A2. ✅ DONE (commit e2b660e): TypeScript — mod04 External systems: switch to `ErpRealClient` / `TaxRealClient`
- mod04 e2e fixtures now use `ErpRealClient` (tax client dropped — mod04 positive spec no longer configures tax, matching Java/.NET).
- mod04 smoke fixtures use `ErpRealClient` + `TaxRealClient`.
- Positive spec calls `erpClient.createProduct(...)`; `configureProduct` / `configureTaxRate` stub-style calls removed.
- `EXTERNAL_SYSTEM_MODE` default flipped to `'real'`.

### A7. ✅ DONE (commit d57c5b3): TypeScript — mod03 WireMock stubbing is a layering leak
- mod03 positive api/ui specs now POST to real ERP `/api/products` instead of `/__admin/mappings`.
- mod03 fixtures default `EXTERNAL_SYSTEM_MODE` to `'real'`.
- Negative specs untouched (they don't contain WireMock admin calls — verify separately if new regressions appear).

---

## B. Architecture Layers — Clients

(All B items completed across Phases 1/3b.)

---

## D. Architecture Layers — Channels

No changes required (aligned across all three languages).

---

## E. Architecture Layers — Use Case DSL

(All E items completed in Phase 3c.)

---

## F. Architecture Layers — Scenario DSL

(All F items completed across Phases 1/3b/3c.)

---

## G. Architecture Layers — Common

### G1. TypeScript — add `Closer` utility — ❌ EXCEPTION (TS-specific)
- **Decision:** Not ported. Java wraps `AutoCloseable`; TS has no equivalent abstraction. Only `ErpStubClient`/`TaxStubClient` have a `close()` method and callers invoke it directly — no utility needed. The plan itself acknowledged: "JS has native dispose semantics now; consider whether this is still needed."

---

## H. Architecture Layers — Driver Ports

(All H items completed across Phases 1/3b.)

---

## I. Latest Tests — Acceptance

(All I items completed in Phase 3b.)

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

All tasks resolved (O1, O4 in Phase 3a; O2, O3 in Phase 3b).

---

## P. Legacy Tests — mod05

All tasks resolved (P2 in Phase 3a — sku assertion added; A3 remaining tax step removed in Phase 2).

---

## Q. Legacy Tests — mod06

All tasks resolved (Q1 in Phase 3a — full assertions added; A3 remaining tax step removed in Phase 2).

---

## R. Legacy Tests — mod07

All tasks resolved (A4 + R1 in Phase 3c — fluent DSL built, mod07 positive+negative specs rewritten).

---

## S. Legacy Tests — mod08

Covered under A8.

---

## T. Legacy Tests — mod09

No changes required.

---

## U. Legacy Tests — mod10

(U2 completed in Phase 3b.)

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
  - Structural alignment of TypeScript testkit to Java reference, keeping shop's idioms (raw fetch, functional `Result<T,E>`, no axios/class-Result/decimal.js stack from eshop-tests).
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

- **0f88bcc** — "Align TS testkit with Java/.NET: DTOs, verification classes, UI driver not-found handling (Phase 3b)"
  - **H3:** `GetCountryRequest` DTO; `TaxDriver.getTaxRate` now accepts request object.
  - **H5:** `SystemResults` utility (`success`/`failure` factories for `Result<T, SystemError>`).
  - **B7:** `SystemErrorMapper.from(problemDetail)` replaces `problem-detail-mapper.ts`; all controllers use it.
  - **F5:** port-layer `WhenStep`/`GivenStep`/`ThenStep` marker base types for structural parity with Java.
  - **F7:** shared DSL verification classes at `dsl/core/shared/`: `BaseUseCase`, `UseCaseResult`, `ErrorVerification`, `ResponseVerification`, `VoidVerification`, `UseCaseContext` (re-export).
  - **B4:** 8 external-client DTOs (`Ext*`) under `driver/adapter/external/{erp,clock,tax}/client/dtos/` replace the anonymous inline types in the clients.
  - **O2:** mod04 api positive spec now asserts full `viewOrder` details (orderNumber/sku/quantity=5/unitPrice=20/totalPrice>0/status=PLACED).
  - **A1, O3:** mod04 UI positive + negative specs rewritten to use `ShopUiClient` page objects via a new `shopUiClient` fixture; page method names renamed `fill*` → `input*` and `clickCreateCoupon` → `clickPublishCoupon` (matching Java).
  - **I4:** `WhenPublishCoupon.withDiscountRate` and `PublishCouponRequest.discountRate` now accept `number | string`; publish-coupon-negative test values switched to strings (`'0.0'`, `'-0.01'`, `'-0.15'`, `'1.01'`, `'2.00'`) matching Java.
  - **U2:** mod10 `shouldApplyFullPriceOnWeekday` now includes `.and().clock().withWeekday()` before `.when().placeOrder(...)`.
  - **I2:** `@time-dependent` tag appended to test titles in `place-order-negative-isolated`, `cancel-order-positive-isolated`, `cancel-order-negative-isolated` (Playwright-idiomatic equivalent of Java's `@TimeDependent`).
  - **I3:** `OrderHistoryPage.isOrderListed(orderNumber)` (30 s `waitFor` matching Java/.NET `PageClient.isVisible`); `shop-ui-driver` `viewOrder`/`cancelOrder`/`deliverOrder` now return `failure('Order X does not exist.')` when the row never appears; `view-order-negative` switched to `eachAlsoFirstRow` (first row now runs via UI too).
  - Local verification: full latest + full legacy suite on monolith, both green.

- **b621222** — "Decompose TS use-case DSL into fluent builders matching Java/.NET/eshop-tests (Phase 3c)"
  - **E3:** per-system base classes `BaseShopUseCase`, `BaseErpUseCase`, `BaseTaxUseCase`, `BaseClockUseCase` (each extends shared `BaseUseCase` with a fixed driver type).
  - **E1:** per-use-case builder + verification classes under `dsl/core/usecase/shop/usecases/` (`PlaceOrder`, `PlaceOrderVerification`, `ViewOrder`, `ViewOrderVerification`) and `dsl/core/usecase/external/erp/usecases/` (`ReturnsProduct`). Builder pattern matches Java/.NET/eshop-tests exactly: sync setters, async `execute()` → `Promise<UseCaseResult>`, sync `.shouldSucceed()/.shouldFail()` returning verification with sync assertion chain.
  - **E2:** `DeliverOrder` covered by the existing `ShopDsl.deliverOrder` flat method; dedicated fluent builder not added since no current test requires it.
  - **A4:** `ShopDsl`/`ErpDsl`/`TaxDsl`/`ClockDsl` now receive a shared `UseCaseContext` from `UseCaseDsl`; `shop.placeOrder()`/`viewOrder()` and `erp.returnsProduct()` return builders; flat `goToShop`/`goToErp`/`goToTax`/`goToClock` retained for smoke tests.
  - **R1:** mod07 positive + negative e2e specs rewritten in fluent chain matching Java reference. Stray `useCase.tax().returnsTaxRate(...)` step removed.
  - **F3:** port Then* interfaces renamed — `then-given-{clock,country,product}.ts` → `then-{clock,country,product}.ts`; `ThenGiven*` types → `Then*`.
  - **Alias resolution aligned to Java/.NET/eshop-tests:** `sku(alias)` → `context.getParamValue(alias)` (generate-on-miss + cache); `country(alias)` → `context.getParamValueOrLiteral(alias)`; `couponCode(alias)` → `context.getParamValue(alias)`; `orderNumber(alias)` on PlaceOrder stores result via `context.setResultEntry` after execute; `orderNumber/sku/country(alias)` on verifications look up stored values. mod07 positive test now uses `SKU='sku'`, `ORDER_NUMBER='order-number'`, `COUNTRY='US'` constants matching Java `Defaults.java`.
  - Local verification: full latest (11 suites) + full legacy (26 suites) on monolith, all green.

**Final result — all tasks resolved:**
- ✅ DONE: **A1 A2 A3 A4 A5 A6 A7 A8** (section A), **B1 B2 B3 B4 B5 B6 B7 B9** (section B), **C1** (section C), **E1 E2 E3** (section E), **F3 F4 F5 F7** (section F), **G2 G3** (section G), **H1 H3 H5** (section H), **I1 I2 I3 I4** (section I), **J1 J2** (section J), **N1 N2 N3 N4** (section N), **O1 O2 O3 O4** (section O), **P2 P3** (section P), **Q1** (section Q), **R1** (section R), **U2** (section U), **V1** (section V).
- ❌ EXCEPTION: **G1** (TS has no `AutoCloseable` equivalent — intentionally not ported).
