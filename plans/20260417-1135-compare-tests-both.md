# System Tests & Architecture Alignment Plan

**Report:** [reports/20260417-1135-compare-tests-both.md](../reports/20260417-1135-compare-tests-both.md)

Reference implementation: **Java**. Tasks below are ordered so prerequisites come first:

1. Architectural mismatches (legacy) — blocking, affects later tasks.
2. Architecture layers (clients → driver ports → channels → use-case DSL → scenario DSL → common).
3. Tests (acceptance → contract → e2e → smoke).

---

## TypeScript

### A. Legacy Abstraction Layers (blocking)

1. **DIFF-LEGACY-1 — Rewrite TS legacy mod03-mod07 to use correct abstraction layers** *(Critical)*
   - mod03: raw HTTP/Playwright calls (no clients, no DSL).
   - mod04: typed clients, no driver abstraction.
   - mod05: driver adapters over clients.
   - mod06: channel-aware driver.
   - mod07: use-case DSL.
   - Reference: `system-test/java/.../legacy/mod03..mod07/` base classes (BaseRawTest, BaseClientTest, BaseDriverTest, BaseChannelDriverTest, BaseUseCaseDslTest).
   - *Blocks tasks 2 and 3.*

2. **DIFF-LEGACY-2 — Split TS mod03-mod05 e2e tests into separate API/UI files** *(High, blocked by #1)*
   - Create `place-order-positive-api-test.spec.ts`, `place-order-positive-ui-test.spec.ts`, `place-order-negative-api-test.spec.ts`, `place-order-negative-ui-test.spec.ts` mirroring Java/.NET.

3. **DIFF-LEGACY-3 — Split TS mod04-mod05 smoke tests into separate API/UI files** *(High, blocked by #1)*
   - mod04: `shop-api-smoke-test.spec.ts` + `shop-ui-smoke-test.spec.ts`.
   - mod05: add `shop-base-smoke-test.spec.ts` as well.

### B. Architecture Layers

4. **DIFF-ARCH-1 — Add separate Clients layer** *(Medium)*
   - Extract HTTP/Playwright wrapping from each `*-driver.ts` into dedicated client classes: `ShopApiClient`, `ShopUiClient`, `ClockRealClient`, `ClockStubClient`, `ErpRealClient`, `ErpStubClient`, `TaxRealClient`, `TaxStubClient`, `PageClient`.
   - Location: `system-test/typescript/src/testkit/driver/adapter/` alongside existing drivers.

5. **DIFF-ARCH-2 — Split `common/dtos.ts` into domain-specific DTO files** *(Medium)*
   - Move DTOs to `driver/port/shop/`, `driver/port/clock/`, `driver/port/erp/`, `driver/port/tax/` to match Java/.NET layout.

6. **DIFF-ARCH-7 — Define `ChannelType` enum/constant instead of string literals** *(Medium)*
   - Replace `'ui'` / `'api'` literals with a typed enum mirroring Java's `ChannelType`.

7. **DIFF-ARCH-3 — Extract separate Use-Case DSL classes per domain** *(Medium)*
   - Add `ShopDsl`, `ClockDsl`, `ErpDsl`, `TaxDsl` mirroring Java; current logic is embedded in the scenario DSL.

8. ~~**DIFF-ARCH-6 — Verify whether TS needs a `Converter` class** *(Low)*~~
   - **Resolved:** Not needed. Java Converter handles String↔BigDecimal/Integer/Instant; TS uses native idioms (Number.parseFloat, String(), new Date()).

### C. Tests — Acceptance

9. **DIFF-TS-3 — Implement `alsoForFirstRow` pattern in TS test harness** *(High)*
   - Affected tests (~12 methods):
     - `cancelOrderNegativeIsolatedTest.cannotCancelAnOrderOn31stDecBetween2200And2230`
     - `cancelOrderPositiveIsolatedTest.shouldBeAbleToCancelOrderOutsideOfBlackoutPeriod...`
     - `placeOrderNegativeTest.shouldRejectOrderWithEmptySku`
     - `placeOrderNegativeTest.shouldRejectOrderWithEmptyQuantity`
     - `placeOrderNegativeTest.shouldRejectOrderWithNonIntegerQuantity`
     - `placeOrderNegativeTest.shouldRejectOrderWithEmptyCountry`
     - `placeOrderPositiveTest.shouldPlaceOrderWithCorrectBasePriceParameterized`
     - `placeOrderPositiveTest.correctTaxRateShouldBeUsedBasedOnCountry`
     - `placeOrderPositiveTest.totalPriceShouldBeSubtotalPricePlusTaxAmount`
     - `publishCouponNegativeTest.cannotPublishCouponWithZeroOrNegativeDiscount`
     - `publishCouponNegativeTest.cannotPublishCouponWithDiscountGreaterThan100percent`
     - `publishCouponNegativeTest.cannotPublishCouponWithZeroOrNegativeUsageLimit`
   - First data row runs on both UI+API, remaining rows API only.

10. **DIFF-TS-8 — Add `alsoForFirstRow=UI` to `ViewOrderNegativeTest.shouldNotBeAbleToViewNonExistentOrder`** *(Medium, blocked by #9)*
    - **Deferred:** TS UI driver can't view non-existent orders (times out clicking in table). Needs UI driver fix first.

11. **DIFF-TS-4 — Remove extra `.withQuantity(1)` calls in `place-order-negative-test.spec.ts`** *(Low)*
    - Tests: `shouldRejectOrderWithNonExistentSku`, `shouldRejectOrderWithEmptySku`, `shouldRejectOrderWithEmptyCountry`, `shouldRejectOrderWithInvalidCountry`, `cannotPlaceOrderWithNonExistentCoupon`.

12. **DIFF-TS-5 — Add `.withCouponCode(null)` to `discountRateShouldBeNotAppliedWhenThereIsNoCoupon`** *(Low)*

13. **DIFF-TS-6 — Change `.withCouponCode(code)` to `.withCouponCode()` (no arg) in subtotal-with-coupon test** *(Low)*

14. **DIFF-TS-7 — Verify `undefined` vs `null` handling for optional fields in `shouldBeAbleToPublishCouponWithEmptyOptionalFields`** *(Low)*
    - If behaviour differs, switch TS to `null`; otherwise document the choice.

### D. Tests — Contract

15. **DIFF-TS-1 — Remove `shouldBeAbleToGetConfiguredTime` from latest `clock-stub-contract-test.spec.ts`** *(Medium)*

16. **DIFF-TS-2 — Remove `shouldBeAbleToGetTime` from latest `clock-stub-contract-isolated-test.spec.ts`** *(Medium)*

17. **DIFF-LEGACY-4 — Fix TS mod11 `clock-stub-contract-test.spec.ts` to use `shouldBeAbleToGetTime`** *(Medium)*
    - Currently has `shouldBeAbleToGetConfiguredTime`; Java inherits `shouldBeAbleToGetTime` from `BaseClockContractTest`.

18. **DIFF-LEGACY-5 — Remove `shouldBeAbleToGetTime` from TS mod11 `clock-stub-contract-isolated-test.spec.ts`** *(Medium)*

---

## .NET

### E. Architecture — Scenario DSL

19. ~~**DIFF-ARCH-4 — Verify `WhenGoToShop` step** *(Low)*~~
    - **Resolved:** `GoToShop` exists in both Java and .NET (drivers, use-case DSL, scenario DSL, smoke tests). Report was incorrect — no action needed.

### F. Tests — Acceptance

20. **DIFF-NET-1 — Remove `ShouldRejectOrderWithNonPositiveQuantity` from `PlaceOrderNegativeTest.cs`** *(Low)*
    - Redundant with existing `ShouldRejectOrderWithNegativeQuantity` and `ShouldRejectOrderWithZeroQuantity`.
VJ: approved
---

## Java

No changes required — Java is the reference implementation.

---

## Severity Totals

| Language | Critical | High | Medium | Low | Total |
|----------|----------|------|--------|-----|-------|
| TypeScript | 1 | 3 | 10 | 6 | 20 |
| .NET | 0 | 0 | 0 | 2 | 2 |
| Java | 0 | 0 | 0 | 0 | 0 |
| **Total** | **1** | **3** | **10** | **8** | **22** |

Note: DIFF-ARCH-5 (extra .NET `Then*` step classes) is classified as an acceptable language-specific implementation detail in the report and is not listed as an action item here.
