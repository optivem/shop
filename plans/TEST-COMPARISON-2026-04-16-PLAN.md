# System Test Comparison - Action Plan (2026-04-16)

**Source:** [TEST-COMPARISON-2026-04-16.md](../reports/TEST-COMPARISON-2026-04-16.md)
**Date:** 2026-04-16
**Target repo:** `shop` only. Do NOT modify `eshop-tests`.

---

## Cross-Language Decisions Made

- **DIFF-L004/L005/L006/L007 (`.withQuantity(1)`)**: Keep in TS -- more explicit is better. Eventually add to Java/.NET.
- **DIFF-L001 (`.withCouponCode(null)`)**: TS DSL uses optional param (omit = no coupon), not `null`. Kept TS approach.
- **DIFF-G015 (clock `.withWeekday()`)**: Already matches Java -- no change needed.

---

## Phase 1: Critical - TS Legacy Architectural Mismatches

These are the highest priority items. TS legacy mod03-mod07 all use Scenario DSL via the shared `withApp()` fixture, skipping the intended abstraction progression.

- [ ] **DIFF-G001**: Rewrite TS mod03 e2e tests to use raw `fetch()` and Playwright calls (no DSL)
- [ ] **DIFF-G002**: Split TS mod03 into separate `PlaceOrderPositiveApiTest`/`UiTest` and `PlaceOrderNegativeApiTest`/`UiTest` files
- [ ] **DIFF-G003**: Add full field assertions to TS mod03 positive tests (orderNumber, sku, quantity, unitPrice, basePrice, totalPrice, status)
- [ ] **DIFF-G004**: Rewrite TS mod04 to use typed client abstractions (no DSL)
- [ ] **DIFF-G005**: Split TS mod04 smoke into separate `ShopApiSmokeTest`/`ShopUiSmokeTest`
- [ ] **DIFF-G006**: Change TS mod04 negative test data from `"3.5"` to `"invalid-quantity"`
- [ ] **DIFF-G007**: Rewrite TS mod05 to use Driver-level abstraction
- [ ] **DIFF-G008**: Adopt Base+Api+Ui class hierarchy in TS mod05
- [ ] **DIFF-G009**: Add missing assertions to TS mod05 positive test (quantity, unitPrice, totalPriceGreaterThanZero)
- [ ] **DIFF-G010**: Rewrite TS mod06 to use Channel Driver abstraction
- [ ] **DIFF-G011**: Add missing assertions to TS mod06 positive test
- [ ] **DIFF-G012**: Rewrite TS mod07 to use Use-Case DSL abstraction (`app.shop().placeOrder()...`)
- [ ] **DIFF-G013**: Add missing assertions and explicit test constants to TS mod07

## Phase 2: High - Module Incrementality

- [ ] **DIFF-G014**: Remove extra negative tests from TS mod08 that belong to mod10. Keep only `shouldRejectOrderWithNonIntegerQuantity` with `"3.5"`.

## Phase 3: Medium - Latest Test Alignment

### Channel scope

- [ ] **DIFF-L008**: Update TS `cannotPublishCouponWithZeroOrNegativeDiscount` to use alsoForFirstRow equivalent (UI only for first data row)
- [ ] **DIFF-L009**: Update TS `cannotPublishCouponWithZeroOrNegativeUsageLimit` same channel fix
- [ ] **DIFF-L010**: Add `'ui'` channel to TS `ViewOrderNegativeTest` first row

### Contract test method distribution

- [ ] **DIFF-L011**: Remove `.clock().withTime()` from TS latest `clock-stub-contract-test` `shouldBeAbleToGetTime` given-step
- [ ] **DIFF-L012**: Remove `shouldBeAbleToGetConfiguredTime` from TS latest `clock-stub-contract-test.spec.ts` (keep only in isolated)
- [ ] **DIFF-L013**: Remove `shouldBeAbleToGetTime` from TS latest `clock-stub-contract-isolated-test.spec.ts`

### Parameterized test convergence

- [ ] **DIFF-L003**: Java: add parameterized `shouldRejectOrderWithNonPositiveQuantity`; .NET: remove redundant individual tests (keep parameterized only); TS: already correct

## Phase 4: Legacy Contract Alignment

- [ ] **DIFF-G016**: Add `shouldBeAbleToGetTime` to TS legacy mod11 `clock-stub-contract-test.spec.ts`
- [ ] **DIFF-G017**: Remove `shouldBeAbleToGetTime` from TS legacy mod11 `clock-stub-contract-isolated-test.spec.ts`

---

## Structural Observations (No Action)

- **DIFF-G002/G005/G008**: TS legacy mod03/04/05 uses unified channel-aware files while Java/.NET use separate Api/Ui classes. TS is structurally ahead. Will be resolved when architectural mismatches are fixed (Phase 1).
- TS inlines base contract test methods instead of using inheritance. Structurally equivalent, language-idiomatic.
- Tax contract tests missing from all three languages in legacy mod11 (present in latest). Consistent gap across all languages.

---

## Investigations

- [ ] **DSL: skip `.given()` when no setup needed** -- Investigate whether `scenario.then()` (without `.given()`) is possible across all three DSL implementations. Would simplify contract tests like `shouldBeAbleToGetTime`.

---

## Stats

| Category | Total |
|----------|-------|
| Phase 1 (Critical - Architectural) | 13 items |
| Phase 2 (High - Incrementality) | 1 item |
| Phase 3 (Medium - Latest alignment) | 7 items |
| Phase 4 (Legacy contract) | 2 items |
| **Total actionable** | **23 items** |
| Investigations | 1 |
